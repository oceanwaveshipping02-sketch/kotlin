// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@file:MustUseReturnValue

expect class Foo() {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
}

fun commonMain() {
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    <!RETURN_VALUE_NOT_USED!>Foo().x()<!>
    Foo().ign()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: BaseFoo.kt

@MustUseReturnValue
open class BaseFoo {
    @IgnorableReturnValue fun x(): String = ""
    fun ign(): String = ""
}

// FILE: jvm.kt

// Foo.<init> is Unspecified, BaseFoo methods are inverted
// What to do?
actual class Foo : BaseFoo() {
}

fun main() {
    Foo()
    Foo().x()
    <!RETURN_VALUE_NOT_USED!>Foo().ign()<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
