/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.plugin.StatisticsBuildFlowManager

abstract class ConfigurationMetricParameterFlowActionBuildFusService() : BuildFusService<ConfigurationMetricsBuildFusParameters>() {

    companion object {
        internal fun registerIfAbsentImpl(
            project: Project,
            buildUidService: Provider<BuildUidService>,
            generalConfigurationMetricsProvider: Provider<MetricContainer>,
            kotlinVersion: String
        ): Provider<ConfigurationMetricParameterFlowActionBuildFusService> {
            return project.gradle.sharedServices.registerIfAbsent(
                serviceName,
                ConfigurationMetricParameterFlowActionBuildFusService::class.java
            ) { spec ->
                spec.parameters.setBuildFusServiceCommonParameters(project, buildUidService, generalConfigurationMetricsProvider, kotlinVersion)
                //init value to avoid `java.lang.IllegalStateException: GradleScopeServices has been closed` exception on close
                spec.parameters.configurationMetrics.add(MetricContainer())
            }.also {
                StatisticsBuildFlowManager.getInstance(project).subscribeForBuildResult()
            }
        }
    }
}