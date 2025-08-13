/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.fus.internal.detectedCiProperty
import org.jetbrains.kotlin.gradle.fus.internal.isCiBuild
import org.jetbrains.kotlin.gradle.internal.isInIdeaSync
import org.jetbrains.kotlin.gradle.logging.Errors
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.reportToIde
import org.jetbrains.kotlin.gradle.plugin.BuildEventsListenerRegistryHolder
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.isConfigurationCacheRequested
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationEnabled
import org.jetbrains.kotlin.gradle.plugin.internal.isProjectIsolationRequested
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.reportingSettings
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.kotlinErrorsDir
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.isNotEmpty


internal interface UsesBuildFusService : Task {
    @get:Internal
    val buildFusService: Property<BuildFusService<out BuildFusService.Parameters>?>
}

abstract class BuildFusService<T : BuildFusService.Parameters> :
    BuildService<T>,
    AutoCloseable, OperationCompletionListener {
    protected var buildFailed: Boolean = false
    internal val log = Logging.getLogger(this.javaClass)
    protected val buildId = parameters.buildId.get()
    private val errorWasReported = AtomicBoolean(false)

    init {
        log.kotlinDebug("Initialize ${this.javaClass.simpleName}")
        log.info("Build service ${serviceName} init for build $buildId")
        KotlinBuildStatsBeanService.recordBuildStart(buildId)
    }

    interface Parameters : BuildServiceParameters {
        val generalMetricsFinalized: Property<Boolean>
        val generalConfigurationMetrics: Property<MetricContainer>
        val buildStatisticsConfiguration: Property<KotlinBuildStatsConfiguration>
        val buildId: Property<String>
        val kotlinVersion: Property<String>
        val errorDirs: ListProperty<File>

        val fusReportDirectory: Property<File>
    }

    private val fusMetricsConsumer = SynchronizedMetricsContainer()

    internal fun getFusMetricsConsumer(): StatisticsValuesConsumer = fusMetricsConsumer

    /**
     * Collects metrics using the provided function into a temporary, non-thread-safe instance
     * of [StatisticsValuesConsumer], and then synchronizes the results into the primary [fusMetricsConsumer].
     */
    internal fun reportFusMetrics(reportAction: (StatisticsValuesConsumer) -> Unit) {
        val metricConsumer = NonSynchronizedMetricsContainer()
        reportAction(metricConsumer)
        fusMetricsConsumer.readFromMetricConsumer(metricConsumer)
    }

    private val projectEvaluatedTime: Long = System.currentTimeMillis()

    companion object {
        internal val serviceName = "${BuildFusService::class.simpleName}_${BuildFusService::class.java.classLoader.hashCode()}"
        private var buildStartTime: Long = System.currentTimeMillis()

        internal fun getBuildFusService(project: Project) =
            if (project.buildServiceShouldBeCreated) {
                project.gradle.sharedServices.registrations.findByName(serviceName).also {
                    if (it == null) {
                        project.logger.info("BuildFusService was not registered")
                    }
                }
            } else {
                null
            }


        fun registerIfAbsent(project: Project, pluginVersion: String, buildUidService: Provider<BuildUidService>) =
            if (project.buildServiceShouldBeCreated) {
                registerIfAbsentImpl(project, pluginVersion, buildUidService).also { serviceProvider ->
                    SingleActionPerProject.run(project, UsesBuildFusService::class.java.name) {
                        project.tasks.withType<UsesBuildFusService>().configureEach { task ->
                            task.buildFusService.value(serviceProvider).disallowChanges()
                            task.usesService(serviceProvider)
                        }
                    }
                }
            } else {
                val reason = when {
                    project.isInIdeaSync.get() -> "Idea sync is in progress"
                    !project.kotlinPropertiesProvider.enableFusMetricsCollection -> "Fus was disabled for the build"
                    !project.isCustomLoggerRootPathIsProvided && isCiBuild() -> "CI build is detected via environment variable ${detectedCiProperty()}"
                    else -> "BuildFusService should not be created."
                }
                project.logger.debug("Fus metrics won't be collected: $reason.")
                null
            }

        private fun registerIfAbsentImpl(
            project: Project,
            kotlinPluginVersion: String,
            buildUidService: Provider<BuildUidService>,
        ): Provider<out BuildFusService<out Parameters>> {

            val isProjectIsolationEnabled = project.isProjectIsolationEnabled
            val isConfigurationCacheRequested = project.isConfigurationCacheRequested
            val isProjectIsolationRequested = project.isProjectIsolationRequested

            project.gradle.sharedServices.registrations.findByName(serviceName)?.let {
                @Suppress("UNCHECKED_CAST")
                return (it.service as Provider<out BuildFusService<out Parameters>>)
            }

            //init buildStatsService
            KotlinBuildStatsBeanService.initStatsService(project)

            val buildReportOutputs = reportingSettings(project).buildReportOutputs
            val gradle = project.gradle
            val generalConfigurationMetricsProvider = project.provider {
                //isProjectIsolationEnabled isConfigurationCacheRequested and isProjectIsolationRequested should be calculated beforehand
                // because since Gradle 8.0 provider's calculation is made in BuildFinishFlowAction
                // and VariantImplementationFactories is not initialized at that moment
                collectGeneralConfigurationTimeMetrics(
                    project,
                    gradle,
                    buildReportOutputs,
                    kotlinPluginVersion,
                    isProjectIsolationEnabled,
                    isProjectIsolationRequested,
                    isConfigurationCacheRequested
                )
            }

            //Workaround for known issues for Gradle 8+: https://github.com/gradle/gradle/issues/24887:
            // when this OperationCompletionListener is called services can be already closed for Gradle 8,
            // so there is a change that no VariantImplementationFactory will be found
            val fusService = if (GradleVersion.current().baseVersion >= GradleVersion.version("8.9")) {
                FlowActionBuildFusService.registerIfAbsentImpl(
                    project,
                    buildUidService,
                    generalConfigurationMetricsProvider,
                    kotlinPluginVersion
                ).also {
                    BuildFusFlowProviderManager.getInstance(project).subscribeWithFlowActionBuildFusService()
                }
            } else if (GradleVersion.current().baseVersion >= GradleVersion.version("8.1")) {
                ConfigurationMetricParameterFlowActionBuildFusService.registerIfAbsentImpl(
                    project,
                    buildUidService,
                    generalConfigurationMetricsProvider,
                    kotlinPluginVersion
                ).also {
                    BuildFusFlowProviderManager.getInstance(project).subscribeWithConfigurationMetricParameterFlowActionBuildFusService()
                }
            } else {
                CloseActionBuildFusService.registerIfAbsentImpl(
                    project,
                    buildUidService,
                    generalConfigurationMetricsProvider,
                    kotlinPluginVersion
                )
            }
            //DO NOT call buildService.get() before all parameters.configurationMetrics are set.
            // buildService.get() call will cause parameters calculation and configuration cache storage.

            BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(fusService)

            return fusService
        }

        internal fun collectAllFusReportsIntoOne(
            buildUid: String,
            fusReportDirectory: File,
            kotlinVersion: String,
            log: Logger,
        ): Errors {
            log.info("Build service ${serviceName} internal collectAllFusReportsIntoOne for build $buildUid")
            try {
                val metricContainer = MetricsContainer()

                fusReportDirectory.listFiles()
                    .filter { it.name.startsWith(buildUid) && (it.name.endsWith("plugin-profile") || it.name.endsWith("kotlin-profile")) }
                    .forEach {
                        MetricsContainer.readFromFile(it) {
                            metricContainer.populateFromMetricsContainer(it)
                        }
                    }

                val fusFile = fusReportDirectory.resolve("$buildUid.profile")
                fusFile.writer().buffered().use {
                    it.appendLine("Build: $buildUid")
                    it.appendLine("Kotlin version: $kotlinVersion")
                    metricContainer.flush(it)
                }

                if (!fusReportDirectory.resolve("$buildUid.finish-profile").createNewFile()) {
                    log.debug("File $fusReportDirectory/$buildUid.finish-profile already exists")
                    return listOf("File $fusReportDirectory/$buildUid.finish-profile already exists")
                }

            } catch (e: Exception) {
                log.debug("Unable to collect finish file for build $buildUid: ${e.message}")
                return listOf("Error while creating finish file: ${e.message}" + e.stackTrace.joinToString("\n"))
            }
            log.debug("Single fus file was created for build $buildUid ")
            return emptyList()
        }
    }

    @Synchronized //access fusMetricsConsumer requires synchronisation as long as tasks are executed in parallel
    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            if (event.result is TaskFailureResult) {
                buildFailed = true
            }

            val taskExecutionResult = TaskExecutionResults[event.descriptor.taskPath]

            taskExecutionResult?.also { executionResult ->
                reportFusMetrics {
                    KotlinTaskExecutionMetrics.collectMetrics(executionResult, event, it)
                }
            }
        }
        reportFusMetrics {
            ExecutedTaskMetrics.collectMetrics(event, it)
        }
    }

    override fun close() {
        KotlinBuildStatsBeanService.closeServices()
        log.kotlinDebug("Close ${this.javaClass.simpleName}")
        log.info("Build service ${serviceName} close for build $buildId")
    }

    internal fun recordBuildFinished(buildFailed: Boolean, configurationMetrics: List<MetricContainer>) {
        log.info("Build service ${serviceName} recordBuildFinished for build $buildId")
        BuildFinishMetrics.collectMetrics(log, buildFailed, buildStartTime, projectEvaluatedTime, fusMetricsConsumer)
        configurationMetrics.forEach { it.addToConsumer(fusMetricsConsumer) }
        parameters.generalConfigurationMetrics.orNull?.addToConsumer(fusMetricsConsumer)
        parameters.buildStatisticsConfiguration.orNull?.also {
            val loggerService = KotlinBuildStatsLoggerService(it)
            loggerService.initSessionLogger(buildId)
            loggerService.reportBuildFinished(fusMetricsConsumer)
        }
    }

    internal fun collectAllFusReportsIntoOne() {
        log.info("Build service ${serviceName} collectAllFusReportsIntoOne for build $buildId")
        val errorMessages = collectAllFusReportsIntoOne(
            buildId,
            parameters.fusReportDirectory.get(),
            parameters.kotlinVersion.get(),
            log
        )

        //KT-79408 skip reporting to IDE if there is already a reported fus related error file with the same buildId
        if (errorMessages.isNotEmpty()) {
            if (errorWasReported.compareAndSet(false, true)) {
                errorMessages.reportToIde(
                    parameters.errorDirs.get().map { it.errorFile() }, parameters.kotlinVersion.get(), buildId,
                    GradleKotlinLogger(log)
                )
            }
        }
    }

    private fun File.errorFile() = resolve("errors-$buildId-${System.currentTimeMillis()}.log")
}

class MetricContainer : Serializable {
    private val numericalMetrics = HashMap<NumericalMetrics, Long>()
    private val booleanMetrics = HashMap<BooleanMetrics, Boolean>()
    private val stringMetrics = HashMap<StringMetrics, String>()

    fun addToConsumer(metricsConsumer: StatisticsValuesConsumer) {
        for ((key, value) in numericalMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in booleanMetrics) {
            metricsConsumer.report(key, value)
        }
        for ((key, value) in stringMetrics) {
            metricsConsumer.report(key, value)
        }
    }

    fun put(metric: StringMetrics, value: String) = stringMetrics.put(metric, value)
    fun put(metric: BooleanMetrics, value: Boolean) = booleanMetrics.put(metric, value)
    fun put(metric: NumericalMetrics, value: Long) = numericalMetrics.put(metric, value)
}

internal val Project.buildServiceShouldBeCreated
    get() = !isInIdeaSync.get() && kotlinPropertiesProvider.enableFusMetricsCollection && (isCustomLoggerRootPathIsProvided || !isCiBuild())

internal fun BuildFusService.Parameters.finalizeGeneralConfigurationMetrics() {
    if (generalMetricsFinalized.get()) return
    synchronized(this) {
        if (generalMetricsFinalized.get()) return
        generalMetricsFinalized.set(true)
        generalConfigurationMetrics.finalizeValue()
    }
}

private fun BuildFusService.Parameters.setErrorDirs(project: Project) {
    errorDirs.add(project.kotlinErrorsDir)
    if (!project.kotlinPropertiesProvider.kotlinProjectPersistentDirGradleDisableWrite) {
        errorDirs.add(project.rootDir.resolve(".gradle/kotlin/errors/"))
    }
    errorDirs.disallowChanges()
}

internal fun BuildFusService.Parameters.setBuildFusServiceCommonParameters(
    project: Project,
    buildUidService: Provider<BuildUidService>,
    generalConfigurationMetricsProvider: Provider<MetricContainer>,
    kotlinPluginVersion: String,
) {
    generalConfigurationMetrics.set(generalConfigurationMetricsProvider)
    generalMetricsFinalized.set(false)
    buildStatisticsConfiguration.set(KotlinBuildStatsConfiguration(project))
    buildId.value(buildUidService.map { it.buildId }).disallowChanges()
    kotlinVersion.value(kotlinPluginVersion).disallowChanges()
    setErrorDirs(project)
    fusReportDirectory.value(project.getFusDirectoryFromPropertyService()).disallowChanges()
}