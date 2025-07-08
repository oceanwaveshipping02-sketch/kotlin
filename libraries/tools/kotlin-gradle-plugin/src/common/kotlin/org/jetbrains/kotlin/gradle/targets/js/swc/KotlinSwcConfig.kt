/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import java.io.File
import java.io.Serializable

data class KotlinSwcConfig(
    val esTarget: String,
    val sourceMaps: Boolean,
    val coreJsVersion: String,
    val moduleKind: String? = null,
    val envTargets: TargetPlatformsDescription? = null,
    val parseMap: Boolean? = null,
    val isNodeJs: Boolean = true,
) : Serializable {
    fun save(configFile: File) {
        configFile.writer().use { it.emitConfigJson() }
    }

    internal val fileExtension: String
        get() = if (moduleSystemToUse == "es") "mjs" else "js"

    internal val moduleSystemToUse: String?
        get() = when {
            moduleKind != null -> moduleKind
            esTarget == ES_2015 -> "es"
            isNodeJs -> "commonjs"
            envTargets != null -> null
            else -> "umd"
        }

    fun Appendable.emitConfigJson(indent: String = "") {
        val targets = envTargets?.toJson() ?: when (esTarget) {
            "es5" -> "{}"
            else -> "{ \"chrome\": \"51\", \"edge\": \"15\" }"
        }

        // language=JSON
        appendLine("""
           {
               "${'$'}schema": "https://swc.rs/schema.json",
               "jsc": {
                   "parser": {
                       "syntax": "ecmascript",
                       "dynamicImport": true,
                       "functionBind": true,
                       "importMeta": true
                   },
                   "loose": true,
                   "externalHelpers": true
               },${moduleSystemToUse?.let {"""
               "module": {
                  "resolveFully": true,
                  "type": "${if (it == "es") "nodenext" else it}",
                  "outFileExtension": "$fileExtension"
               },"""}.orEmpty()}
               "env": {
                 "mode": "usage",
                 "targets": $targets,
                 "coreJs": "$coreJsVersion"
               },
               ${if (parseMap != null) "\"parseMap\": ${!parseMap}," else ""}
               "sourceMaps": $sourceMaps
           }
        """.trimIndent().prependIndent(indent))
    }
}