// RUN_PIPELINE_TILL: BACKEND
// IGNORE_BACKEND_K1: ANY
// Evaluation of operations on unsigned constants isn't implementented on K
const val c = 1u + 2u

fun box() = when {
    c != 3u -> "fail"
    else -> "OK"
}

/* GENERATED_FIR_TAGS: const, equalityExpression, functionDeclaration, propertyDeclaration, stringLiteral,
unsignedLiteral, whenExpression */
