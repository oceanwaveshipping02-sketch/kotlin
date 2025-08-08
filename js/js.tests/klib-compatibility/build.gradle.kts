import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    if (cacheRedirectorEnabled) {
        distBaseUrl.set("https://cache-redirector.jetbrains.com/nodejs.org/dist")
    }
}

dependencies {
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(testFixtures(project(":js:js.tests")))
}

/* Configurations for custom compiler versions. */
val customCompilerArtifacts1920: Configuration by configurations.creating
val customCompilerArtifacts200: Configuration by configurations.creating
val customCompilerArtifacts210: Configuration by configurations.creating
// Step 1: Add a new configuration here.

/* Dependencies for custom compiler versions. */
dependencies {
    /* 1.9.20 */
    customCompilerArtifacts1920("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.20")
    customCompilerArtifacts1920("org.jetbrains.kotlin:kotlin-stdlib-js:1.9.20") {
        attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
    }
    customCompilerArtifacts1920("org.jetbrains.kotlin:kotlin-test-js:1.9.20") {
        attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
    }

    /* 2.0.0 */
    customCompilerArtifacts200("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.0")
    customCompilerArtifacts200("org.jetbrains.kotlin:kotlin-stdlib-js:2.0.0")
    customCompilerArtifacts200("org.jetbrains.kotlin:kotlin-test-js:2.0.0")

    /* 2.1.0 */
    customCompilerArtifacts210("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    customCompilerArtifacts210("org.jetbrains.kotlin:kotlin-stdlib-js:2.1.0")
    customCompilerArtifacts210("org.jetbrains.kotlin:kotlin-test-js:2.1.0")

    // Step 2: Add the dependencies for the new configuration here.
}

/* Directories with custom compiler artifacts. */
val customCompilerArtifactsDir1920: Provider<Directory> = layout.buildDirectory.dir("customCompiler_1920")
val customCompilerArtifactsDir200: Provider<Directory> = layout.buildDirectory.dir("customCompiler_200")
val customCompilerArtifactsDir210: Provider<Directory> = layout.buildDirectory.dir("customCompiler_210")
// Step 3: Add a new directory here.

/* Download tasks for custom compiler artifacts. */
val downloadCustomCompilerArtifacts1920: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(customCompilerArtifacts1920)
    into(customCompilerArtifactsDir1920)
}
val downloadCustomCompilerArtifacts200: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(customCompilerArtifacts200)
    into(customCompilerArtifactsDir200)
}
val downloadCustomCompilerArtifacts210: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(customCompilerArtifacts210)
    into(customCompilerArtifactsDir210)
}
// Step 4: Add a new download task here.

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

fun Test.setUpJsBoxTests() {
    with(d8KotlinBuild) {
        setupV8()
    }
    dependsOn(":dist")
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    useJUnitPlatform { includeTags("custom-first-phase") }
    workingDir = rootDir
}

fun Test.setUpCustomCompiler(
    customCompilerVersion: String,
    downloadTask: TaskProvider<Sync>,
    artifactsDir: Provider<Directory>,
) {
    dependsOn(downloadTask)
    systemProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir", artifactsDir.get().asFile.absolutePath)
    systemProperty("kotlin.internal.js.test.compat.customCompilerVersion", customCompilerVersion)
}

/* Custom-first-phase test tasks for different compiler versions. */
projectTest("testCustomFirstPhase1920", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests()
    setUpCustomCompiler("1.9.20", downloadCustomCompilerArtifacts1920, customCompilerArtifactsDir1920)
}
projectTest("testCustomFirstPhase200", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests()
    setUpCustomCompiler("2.0.0", downloadCustomCompilerArtifacts200, customCompilerArtifactsDir200)
}
projectTest("testCustomFirstPhase210", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests()
    setUpCustomCompiler("2.1.0", downloadCustomCompilerArtifacts210, customCompilerArtifactsDir210)
}
// Step 5: Add a new test task here.

@Suppress("unused")
val test by tasks.getting(Test::class) {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

@Suppress("unused")
val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsKlibCompatibilityTestsKt") {
    dependsOn(":compiler:generateTestData")
}
