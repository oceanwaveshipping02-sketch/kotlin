/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

abstract class BaseSymbolsImpl(protected val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder

    // TODO KT-79436 unify backend specific functions and remove the old ones
    protected fun findSharedVariableBoxClass(primitiveType: PrimitiveType?): FrontendKlibSymbols.SharedVariableBoxClassInfo {
        val suffix = primitiveType?.typeName?.asString() ?: ""
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = classId.classSymbol()
        return FrontendKlibSymbols.SharedVariableBoxClassInfo(boxClass)
    }

    // Native
    protected fun ClassId.classSymbol() = symbolFinder.findClass(this) ?: error("Class $this is not found")
    protected fun CallableId.propertySymbols() = symbolFinder.findProperties(this).toList()
    protected fun CallableId.functionSymbols() = symbolFinder.findFunctions(this).toList()
    protected fun ClassId.primaryConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.primaryConstructor ?: error("Class ${this} has no primary constructor")).symbol }
    }

    protected fun ClassId.noParametersConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.constructors.singleOrNull { it.parameters.isEmpty() } ?: error("Class ${this} has no constructor without parameters")).symbol }
    }

    protected fun CallableId.functionSymbol(): IrSimpleFunctionSymbol {
        val elements = functionSymbols()
        require(elements.isNotEmpty()) { "No function $this found" }
        require(elements.size == 1) { "Several functions $this found:\n${elements.joinToString("\n")}" }
        return elements.single()
    }

    protected inline fun CallableId.functionSymbol(crossinline condition: (IrSimpleFunction) -> Boolean): Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = functionSymbols()
        return lazy {
            val elements = unfilteredElements.filter { condition(it.owner) }
            require(elements.isNotEmpty()) { "No function $this found corresponding given condition" }
            require(elements.size == 1) { "Several functions $this found corresponding given condition:\n${elements.joinToString("\n")}" }
            elements.single()
        }
    }

    protected inline fun <K> CallableId.functionSymbolAssociatedBy(crossinline getKey: (IrSimpleFunction) -> K): Lazy<Map<K, IrSimpleFunctionSymbol>> {
        val unfilteredElements = functionSymbols()
        return lazy {
            unfilteredElements.associateBy { getKey(it.owner) }
        }
    }

    protected fun CallableId.getterSymbol(): Lazy<IrSimpleFunctionSymbol> {
        val elements = propertySymbols()
        require(elements.isNotEmpty()) { "No properties $this found" }
        require(elements.size == 1) { "Several properties $this found:\n${elements.joinToString("\n")}" }
        return lazy {
            elements.single().owner.getter!!.symbol
        }
    }

    protected fun CallableId.getterSymbol(extensionReceiverClass: IrClassSymbol?): Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = propertySymbols()
        require(unfilteredElements.isNotEmpty()) { "No properties $this found" }
        return lazy {
            val elements = unfilteredElements.filter { it.owner.getter?.extensionReceiverClass == extensionReceiverClass }
            require(elements.isNotEmpty()) { "No properties $this found with ${extensionReceiverClass} receiver" }
            require(elements.size == 1) { "Several properties $this found with ${extensionReceiverClass} receiver:\n${elements.joinToString("\n")}" }
            elements.single().owner.getter!!.symbol
        }
    }

    protected val IrFunction.extensionReceiverType get() = parameters.singleOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
    protected val IrFunction.extensionReceiverClass get() = extensionReceiverType?.classOrNull
}

interface FrontendSymbols {
    val asserts: Iterable<IrSimpleFunctionSymbol>
    val arrays: List<IrClassSymbol>

    val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
    val throwUnsupportedOperationException: IrSimpleFunctionSymbol

    val defaultConstructorMarker: IrClassSymbol
    val coroutineContextGetter: IrSimpleFunctionSymbol
    val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
    val coroutineGetContext: IrSimpleFunctionSymbol

    companion object {
        private val String.reflectId get() = CallableId(KOTLIN_REFLECT_FQ_NAME, Name.identifier(this))
        private val typeOf = "typeOf".reflectId

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean {
            return if (symbol.isBound) {
                symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                    function.isTopLevelInPackage(typeOf.callableName.asString(), typeOf.packageName) && function.hasShape()
                }
            } else {
                symbol.hasTopLevelEqualFqName(typeOf.packageName.asString(), typeOf.callableName.asString())
            }
        }
    }

    abstract class Impl(irBuiltIns: IrBuiltIns) : FrontendSymbols, BaseSymbolsImpl(irBuiltIns) {
        override val asserts: Iterable<IrSimpleFunctionSymbol> = CallableIds.asserts.functionSymbols()

        override val arrays: List<IrClassSymbol>
            get() = irBuiltIns.primitiveTypesToPrimitiveArrays.values + irBuiltIns.unsignedTypesToUnsignedArrays.values + irBuiltIns.arrayClass

        override val defaultConstructorMarker: IrClassSymbol
            get() = error("Should not be called on the first stage")

        companion object {
            private object CallableIds {
                private val String.baseId get() = CallableId(BASE_KOTLIN_PACKAGE, Name.identifier(this))
                val asserts = "assert".baseId
            }
        }
    }
}

interface FrontendKlibSymbols : FrontendSymbols {
    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    val genericSharedVariableBox: SharedVariableBoxClassInfo

    abstract class Impl(irBuiltIns: IrBuiltIns) : FrontendKlibSymbols, FrontendSymbols.Impl(irBuiltIns) {
        // The SharedVariableBox family of classes exists only in non-JVM stdlib variants, hence the nullability of the properties below.
        override val genericSharedVariableBox: FrontendKlibSymbols.SharedVariableBoxClassInfo = findSharedVariableBoxClass(null)
    }

    companion object {
        const val THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME = "throwUninitializedPropertyAccessException"
        const val THROW_UNSUPPORTED_OPERATION_NAME = "throwUnsupportedOperationException"
        const val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
        const val COROUTINE_CONTEXT_NAME = "coroutineContext"
    }
}

interface FrontendWebSymbols : FrontendKlibSymbols {
    abstract class Impl(irBuiltIns: IrBuiltIns) : FrontendWebSymbols, FrontendKlibSymbols.Impl(irBuiltIns)
}

interface FrontendJsSymbols : FrontendWebSymbols {
    val dynamicType: IrDynamicType
        get() = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)

    val jsCode: IrSimpleFunctionSymbol
    val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : FrontendJsSymbols, FrontendWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException = CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException = CallableIds.throwUnsupportedOperationException.functionSymbol()

        override val coroutineContextGetter by CallableIds.coroutineContextGetter.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn = CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext = CallableIds.coroutineGetContext.functionSymbol()

        override val jsCode: IrSimpleFunctionSymbol = CallableIds.jsCall.functionSymbol()
        override val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = ClassIds.JsOutlinedFunction.classSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"

            private object CallableIds {
                private val String.rootCallableId get() = CallableId(kotlinPackageFqn, Name.identifier(this))
                val throwUninitializedPropertyAccessException = FrontendKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.rootCallableId
                val throwUnsupportedOperationException = FrontendKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.rootCallableId

                private val String.baseJsCallableId get() = CallableId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val suspendCoroutineUninterceptedOrReturn = COROUTINE_SUSPEND_OR_RETURN_JS_NAME.baseJsCallableId
                val coroutineGetContext = FrontendKlibSymbols.GET_COROUTINE_CONTEXT_NAME.baseJsCallableId
                val jsCall = "js".baseJsCallableId

                val coroutineContextGetter = CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(FrontendKlibSymbols.COROUTINE_CONTEXT_NAME))
            }

            private object ClassIds {
                private val String.baseJsClassId get() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val JsOutlinedFunction = "JsOutlinedFunction".baseJsClassId
            }
        }
    }
}

interface FrontendWasmSymbols : FrontendWebSymbols {
    companion object {
        val wasmInternalFqName = FqName.fromSegments(listOf("kotlin", "wasm", "internal"))
    }

    open class Impl(irBuiltIns: IrBuiltIns) : FrontendWasmSymbols, FrontendWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException = CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException = CallableIds.throwUnsupportedOperationException.functionSymbol()

        override val coroutineContextGetter by CallableIds.coroutineContextGetter.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn = CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext = CallableIds.coroutineGetContext.functionSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"

            private object CallableIds {
                private val String.internalCallableId get() = CallableId(wasmInternalFqName, Name.identifier(this))
                val throwUninitializedPropertyAccessException = FrontendKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.internalCallableId
                val throwUnsupportedOperationException = FrontendKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.internalCallableId

                val suspendCoroutineUninterceptedOrReturn = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val coroutineGetContext = FrontendKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId
                val coroutineContextGetter = CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(FrontendKlibSymbols.COROUTINE_CONTEXT_NAME))
            }
        }
    }
}

interface FrontendNativeSymbols : FrontendKlibSymbols {
    val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : FrontendNativeSymbols, FrontendKlibSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException = CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException = CallableIds.throwUnsupportedOperationException.functionSymbol()
        override val isAssertionArgumentEvaluationEnabled = CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

        override val coroutineContextGetter by CallableIds.coroutineContext.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn = CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext = CallableIds.getCoroutineContext.functionSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"

            private object RuntimeNames {
                val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
            }

            private object CallableIds {
                // Internal functions
                private val String.internalCallableId get() = CallableId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
                val throwUninitializedPropertyAccessException = FrontendKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.capitalizeAsciiOnly().internalCallableId
                val throwUnsupportedOperationException = FrontendKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.capitalizeAsciiOnly().internalCallableId
                val suspendCoroutineUninterceptedOrReturn = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val getCoroutineContext = FrontendKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId

                // Special stdlib public functions
                val coroutineContext = CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(FrontendKlibSymbols.COROUTINE_CONTEXT_NAME))

                // Built-ins functions
                private val String.builtInsCallableId get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
                val isAssertionArgumentEvaluationEnabled = "isAssertionArgumentEvaluationEnabled".builtInsCallableId
            }
        }
    }
}
