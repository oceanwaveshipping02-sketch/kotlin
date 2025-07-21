@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        binaries.executable()
        d8 {
            runTask {
                val directory = this.inputFileProperty.get().asFile.parentFile
                inputFileProperty.set(File(directory, "_wasm-d8-multimodule_.mjs"))
            }
        }
        val multiModuleMode = if ("MULTIMODULE_MODE_STDLIB_TO_COMPILE".isEmpty()) "wasm-d8-multimodule" else "kotlin"
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-single-module=$multiModuleMode")
        }
    }
}

tasks.register<Copy>("stdlibCopy") {
    from(File(project.buildDir, "wasm"))
    into(File(project.buildDir, "stdlib"))
}

tasks.register<Copy>("stdlibCopyBack") {
    from(File(project.buildDir, "stdlib"))
    into(File(project.buildDir, "wasm"))
}