/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*

@MppGradlePluginTests
class WasmSingleModuleIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED)

    @GradleTest
    fun `test wasm singlemodule`(version: GradleVersion) {
        project("wasm-d8-singlemodule", version) {
            // Compile only kotlin stdlib
            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":wasmJsDevelopmentExecutableCompileSync")
            }

            // Saving the output to the separate directory
            build("stdlibCopy") {
                assertTasksExecuted(":stdlibCopy")
            }

            // Switch stdlib module to user module
            buildGradleKts.modify {
                it.replace("NOW_COMPILE_STDLIB_AS_SINGLE_MODULE", "")
            }

            // Compile project in slave mode
            build("wasmJsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":wasmJsDevelopmentExecutableCompileSync")
            }

            // Backing up stdlib module compilation
            build("stdlibCopyBack") {
                assertTasksExecuted(":stdlibCopyBack")
            }

            // Run the multimodule project
            build("wasmJsD8DevelopmentRun") {
                assertTasksExecuted(":wasmJsD8DevelopmentRun")
                assertOutputContains("SingleModule is FINE!")
            }
        }
    }
}
