// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@MustUseReturnValue
expect interface A {
    fun foo(): String
    fun bar(): String
}

interface B : A {
    override fun foo(): String
}

fun testCommon(b: B) {
    b.foo() // (1) warning or not?
    b.bar() // (2) warning or not?
}


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

//@MustUseReturnValue
actual interface A {
    actual fun foo(): String
    actual fun bar(): String
}

fun testPlatform(b: B) {
    b.foo() // No warning because status for B.foo computed only once in common session?
    b.bar() // (4) warning or not?
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
