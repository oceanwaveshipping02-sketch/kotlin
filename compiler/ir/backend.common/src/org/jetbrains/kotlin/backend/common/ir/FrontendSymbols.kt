/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@OptIn(InternalSymbolFinderAPI::class)
abstract class BaseSymbolsImpl(val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder

    protected fun findSharedVariableBoxClass(primitiveType: PrimitiveType?): FrontendKlibSymbols.SharedVariableBoxClassInfo {
        val suffix = primitiveType?.typeName?.asString() ?: ""
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = symbolFinder.findClass(classId)
            ?: error("Could not find class $classId")
        return FrontendKlibSymbols.SharedVariableBoxClassInfo(boxClass)
    }
}

interface FrontendSymbols {

}

abstract class FrontendSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendSymbols, BaseSymbolsImpl(irBuiltIns) {

}

interface FrontendKlibSymbols : FrontendSymbols {
    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    val genericSharedVariableBox: SharedVariableBoxClassInfo
}

@OptIn(InternalSymbolFinderAPI::class)
abstract class FrontendKlibSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendKlibSymbols, FrontendSymbolsImpl(irBuiltIns) {
    // The SharedVariableBox family of classes exists only in non-JVM stdlib variants, hence the nullability of the properties below.
    override val genericSharedVariableBox: FrontendKlibSymbols.SharedVariableBoxClassInfo = findSharedVariableBoxClass(null)
}

interface FrontendWebSymbols : FrontendKlibSymbols {

}

abstract class FrontendWebSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWebSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {

}

interface FrontendJsSymbols : FrontendWebSymbols {}

open class FrontendJsSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendJsSymbols, FrontendWebSymbolsImpl(irBuiltIns) {

}

interface FrontendWasmSymbols : FrontendWebSymbols {}

open class FrontendWasmSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWasmSymbols, FrontendWebSymbolsImpl(irBuiltIns) {

}

interface FrontendNativeSymbols : FrontendKlibSymbols {}

open class FrontendNativeSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendNativeSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {

}
