/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.Serializable

sealed interface TargetPlatformsDescription : Serializable {
    fun toJson(): String
}

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
    @SerializedName("react-native") var reactNative: String? = null,
    @SerializedName("opera-mobile") var operaMobile: String? = null,
    @SerializedName("opera-android") var operaAndroid: String? = null,
    @SerializedName("chrome-android") var chromeAndroid: String? = null,
    @SerializedName("firefox-android") var firefoxAndroid: String? = null,
) : Serializable, TargetPlatformsDescription {
    override fun toJson(): String =
        GsonBuilder().create().toJson(this)
}

@JvmInline
internal value class BrowsersListQuery(val queries: Array<out String>) : TargetPlatformsDescription, Serializable {
    override fun toJson(): String =
        GsonBuilder().create().toJson(queries)
}

/**
 * Restrict platforms (browsers, operating systems, runtimes) by listing them with the minimal versions to support
 */
fun minimalVersions(configuration: MinimalPlatformVersions.() -> Unit): TargetPlatformsDescription =
    MinimalPlatformVersions().apply(configuration)

/**
 * Restrict platforms by a declarative [browserslist](https://browsersl.ist/) query
 * You can test the query before use here: https://browsersl.ist/, and it will show you the platforms that will be used
 */
fun browserslist(vararg queries: String): TargetPlatformsDescription = BrowsersListQuery(queries)
