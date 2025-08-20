// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidImplicitTypeAnnotationWithMissingDependency
// ISSUE: KT-80247
// MODULE: a
// FILE: a.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

// MODULE: b(a)
// FILE: b.kt
fun f(): @Anno String = ""

// MODULE: c(b)
// FILE: c.kt
fun g() = <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>f<!>()

fun local() {
    val x = f()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, stringLiteral */
