/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import kotlin.RequiresOptIn.Level.WARNING
import kotlin.annotation.AnnotationTarget.PROPERTY
import  org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

/**
 * API marker for using the declarative target platforms DSL.
 *
 * It may be dropped or changed at any time.
 *
 * @see KotlinJsIrTarget.targetPlatforms
 */
@RequiresOptIn(level = WARNING)
@Target(PROPERTY)
annotation class ExperimentalDeclarativePlatformRestrictionDsl
