/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.swc.KotlinSwc
import org.jetbrains.kotlin.gradle.targets.js.swc.KotlinSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinNodeJsIr.Companion.SWC_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.withType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class SwcConfigurator(private val subTarget: KotlinJsIrSubTarget) : SubTargetConfigurator<KotlinSwc, KotlinSwc> {
    private val project = subTarget.project
    private val propertiesProvider = PropertiesProvider(project)

    private val nodeJsRoot = subTarget.target.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    internal val isWasm: Boolean = subTarget.target.webTargetVariant(
        jsVariant = false,
        wasmVariant = true,
    )

    override fun setupBuild(compilation: KotlinJsIrCompilation) {
        if (isWasm || !propertiesProvider.delegateTranspilationToExternalTool) return

        compilation.binaries
            .withType<Executable>()
            .configureEach { configureSwcTask(it, compilation) }

        compilation.binaries
            .withType<Library>()
            .configureEach { configureSwcTask(it, compilation) }

        // Add @swc/helpers and core-js dependency to consume polyfills
        compilation.dependencies {
            with(nodeJsRoot.versions) {
                implementation(npm(coreJs.name, coreJs.version))
                implementation(npm(swcHelpers.name, swcHelpers.version))
            }
        }
    }

    private fun configureSwcTask(binary: JsIrBinary, compilation: KotlinJsIrCompilation) {
        val mode = binary.mode
        val linkTask = binary.linkTask
        val linkSyncTask = binary.linkSyncTask

        val name = when (binary) {
            is Executable -> binary.executeTaskBaseName
            is Library -> binary.name
            else -> error("Unsupported binary type: ${binary::class.simpleName}")
        }

        val swcTask = subTarget.registerSubTargetTask<KotlinSwc>(
            subTarget.disambiguateCamelCased(name, SWC_TASK_NAME),
            listOf(compilation)
        ) { task ->
            val inputFilesDirectory = linkTask.flatMap { it.destinationDirectory }
            val envTargets = compilation.target._targetPlatforms
            val outputDirectory = binary.distribution.distributionName.flatMap {
                project.layout.buildDirectory.dir("kotlin-swc/${compilation.target.name}/$it")
            }

            task.description = "transpile compiler output with Swc [${mode.name.toLowerCaseAsciiOnly()}]"

            task.versions.value(nodeJsRoot.versions).disallowChanges()
            task.inputFilesDirectory.value(inputFilesDirectory).disallowChanges()
            task.outputDirectory.value(outputDirectory).disallowChanges()
            task.npmToolingEnvDir.value(compilation.npmProject.dir).disallowChanges()

            task.mode = when (mode) {
                KotlinJsBinaryMode.DEVELOPMENT -> Mode.DEVELOPMENT
                KotlinJsBinaryMode.PRODUCTION -> Mode.PRODUCTION
            }

            task.config.convention(binary.linkTask.flatMap { linkTask ->
                val options = linkTask.compilerOptions
                options.target.zip(options.sourceMap) { esTarget, sourceMaps ->
                    KotlinSwcConfig(
                        esTarget = esTarget,
                        sourceMaps = sourceMaps,
                        moduleKind = options.moduleKind.orNull?.kind,
                        envTargets = envTargets.orNull,
                        coreJsVersion = nodeJsRoot.versions.coreJs.version,
                        isNodeJs = true
                    )
                }
            }).finalizeValueOnRead()

            task.dependsOn(linkTask)
        }

        linkSyncTask.configure { task ->
            task.from.from(swcTask.flatMap { it.outputDirectory })
        }
    }

    override fun configureBuild(body: Action<KotlinSwc>) {
    }

    override fun setupRun(compilation: KotlinJsIrCompilation) {
    }

    override fun configureRun(body: Action<KotlinSwc>) {
    }
}
