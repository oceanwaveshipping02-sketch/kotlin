/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.statistics

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.UNKNOWN_BUILD_ID
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFinishBuildService
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertTrue

class BuildFinishBuildServiceTest {

    @Rule
    @JvmField
    var tmpFolder = TemporaryFolder()

    @Test
    fun testFusMetricAggregation() {
        val fusDir = tmpFolder.newFolder("fus-dir")
        fusDir.mkdirs()

        val buildId = "build-id"
        val logger = TestLogger(LogLevel.DEBUG)

        fusDir.resolve("$buildId-akjsldjb.plugin-profile").writeText(
            """
                unknown-metric=1

                ${NumericalMetrics.COMPILATION_DURATION}=10
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("$buildId-kjbsjofhbkb.plugin-profile").writeText(
            """
                unknown-metric=1
                wrong format
                ${NumericalMetrics.COMPILATION_DURATION}=10
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("another-id.plugin-profile").writeText(
            """
                ${BooleanMetrics.TESTS_EXECUTED}=true
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("$buildId.kajfsjfh.kotlin-profile").writeText(
            """
                ${BooleanMetrics.BUILD_SCAN_BUILD_REPORT}=true
                BUILD FINISHED
            """.trimIndent()
        )


        val errorMessages = BuildFinishBuildService.collectAllFusReportsIntoOne(buildId, fusDir, "test version", logger)
        assertTrue("No error messages expected") { errorMessages.isEmpty() }

        assertTrue("finish-profile file should be created after build finish") {
            fusDir.resolve("$buildId.finish-profile").exists()
        }

        val fusProfileFile = fusDir.resolve("$buildId.profile")
        assertTrue("old profile file should be created after build finish") {
            fusProfileFile.exists()
        }

        val profileContent = fusProfileFile.readText()
        assertTrue("Profile file should contain valid metrics") {
            profileContent.contains("${NumericalMetrics.COMPILATION_DURATION}=20")
        }
        assertTrue("Profile file should not contain metrics from another build") {
            !profileContent.contains(BooleanMetrics.TESTS_EXECUTED.name)
        }

    }

    @Test
    fun testFusMetricAggregationForUnknownBuildId() {
        val fusDir = tmpFolder.newFolder("fus-dir")
        fusDir.mkdirs()

        val logger = TestLogger(LogLevel.DEBUG)
        val kotlinVersion = "test-version"

        val listOfSuffixes = listOf("random-suffix", "another-random-suffix")

        listOfSuffixes.forEach { suffix ->
            fusDir.resolve("$UNKNOWN_BUILD_ID.$suffix.kotlin-profile").also {
                it.writeText(
                    """
                ${NumericalMetrics.COMPILATION_DURATION}=10
                BUILD FINISHED
            """.trimIndent()
                )
            }
        }

        fusDir.resolve("${UNKNOWN_BUILD_ID}.with-already-created-profile-file.plugin-profile").also {
            it.writeText(
                """
                ${NumericalMetrics.COMPILATION_DURATION}=10
                BUILD FINISHED
            """.trimIndent()
            )
        }

        fusDir.resolve("${UNKNOWN_BUILD_ID}.with-already-created-profile-file.profile").also {
            it.writeText("should not be overwritten")
        }

        val errors = BuildFinishBuildService.collectAllFusReportsIntoOne(UNKNOWN_BUILD_ID, fusDir, kotlinVersion, logger)
        assertTrue("No error messages expected") { errors.isEmpty() }

        listOfSuffixes.forEach { suffix ->
            assertTrue("Finish-profile file should be created for $suffix") {
                fusDir.resolve("$UNKNOWN_BUILD_ID.$suffix.finish-profile").exists()
            }
            val profileFile = fusDir.resolve("$UNKNOWN_BUILD_ID.$suffix.profile")
            assertTrue("Profile file should be created for $suffix") { profileFile.exists() }
            assertTrue("Profile must contain flushed metrics from source") {
                profileFile.readText().contains("${NumericalMetrics.COMPILATION_DURATION}=10")
            }
        }

    }
}
