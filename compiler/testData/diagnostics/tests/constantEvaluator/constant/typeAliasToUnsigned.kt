// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// Evaluation of operations on unsigned constants isn't implementented on K1
// WITH_STDLIB

typealias UI = UInt

const val a: UI = 1u
const val b: UI = a
const val c = a == b

/* GENERATED_FIR_TAGS: const, equalityExpression, propertyDeclaration, typeAliasDeclaration, unsignedLiteral */
