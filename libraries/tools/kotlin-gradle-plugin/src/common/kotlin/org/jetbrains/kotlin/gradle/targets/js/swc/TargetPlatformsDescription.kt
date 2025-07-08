/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import com.google.gson.GsonBuilder
import java.io.Serializable

sealed interface TargetPlatformsDescription : Serializable {
    fun toJson(): String
}

class MinimalPlatformVersions internal constructor(
    var android: String? = null,
    var chrome: String? = null,
    var deno: String? = null,
    var edge: String? = null,
    var electron: String? = null,
    var firefox: String? = null,
    var ie: String? = null,
    var ios: String? = null,
    var node: String? = null,
    var opera: String? = null,
    var rhino: String? = null,
    var safari: String? = null,
    var samsung: String? = null
) : Serializable, TargetPlatformsDescription {
    override fun toJson(): String =
        GsonBuilder().create().toJson(this)
}

@JvmInline
internal value class BrowserListQuery(val value: String) : TargetPlatformsDescription, Serializable {
    override fun toJson(): String = "\"$value\""
}

fun minimalVersions(configuration: MinimalPlatformVersions.() -> Unit): TargetPlatformsDescription =
    MinimalPlatformVersions().apply(configuration)

fun browserListQuery(query: String): TargetPlatformsDescription = BrowserListQuery(query)
