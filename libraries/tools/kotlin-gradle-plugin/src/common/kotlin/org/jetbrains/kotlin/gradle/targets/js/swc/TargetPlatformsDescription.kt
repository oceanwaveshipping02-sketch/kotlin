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

/**
 *     #[serde(default, rename = "chrome-android")]
 *     pub chrome_android: T,
 *     #[serde(default, rename = "firefox-android")]
 *     pub firerfox_android: T,
 *     #[serde(default, rename = "opera-android")]
 *     pub opera_android: T,
 *     #[serde(default, rename = "react-native")]
 *     pub react_native: T,
 *     #[serde(default)]
 *     pub and_chr: T,
 *     #[serde(default)]
 *     pub and_ff: T,
 *     #[serde(default)]
 *     pub op_mob: T,
 *     #[serde(default)]
 *     pub opera_mobile: T,
 */

class MinimalPlatformVersions internal constructor(
    var android: String? = null,
    var chrome: String? = null,
    var edge: String? = null,
    var firefox: String? = null,
    var ie: String? = null,
    var ios: String? = null,
    var opera: String? = null,
    var rhino: String? = null,
    var safari: String? = null,
    var samsung: String? = null,
    var electron: String? = null,
    var node: String? = null,
    var deno: String? = null,
    var bun: String? = null,
    var hermes: String? = null,
    var oculus: String? = null,
    var quest: String? = null,
    var phantom: String? = null,

) : Serializable, TargetPlatformsDescription {
    override fun toJson(): String =
        GsonBuilder().create().toJson(this)
}

@JvmInline
internal value class BrowsersListQuery(val queries: Array<out String>) : TargetPlatformsDescription, Serializable {
    override fun toJson(): String = "[${queries.joinToString(", ") { "\"$it\"" }}]"
}

fun minimalVersions(configuration: MinimalPlatformVersions.() -> Unit): TargetPlatformsDescription =
    MinimalPlatformVersions().apply(configuration)

fun browserslist(vararg queries: String): TargetPlatformsDescription = BrowsersListQuery(queries)
