/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.DeclarationIdTableReader

/**
 * Some information obtained from library's IR.
 *
 * @property meaningfulInlineFunctionNumber The number of meaningful (non-private, non-local, with body, etc.) inline functions.
 */
internal class KlibIrInfo(
    val meaningfulInlineFunctionNumber: Int
)

internal class KlibIrInfoLoader(private val library: KotlinLibrary) {
    fun loadIrInfo(): KlibIrInfo? {
        if (!library.hasIr) return null

        val declarationsReader = DeclarationIdTableReader(library.declarationsOfInlineableFuns())
        val meaningfulInlineFunctionNumber = declarationsReader.entryCount()

        return KlibIrInfo(
            meaningfulInlineFunctionNumber = meaningfulInlineFunctionNumber,
        )
    }
}
