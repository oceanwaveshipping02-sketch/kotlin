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

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

fun Test.setUpJsBoxTests(tag: String) {
    with(d8KotlinBuild) {
        setupV8()
    }
    dependsOn(":dist")
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    useJUnitPlatform { includeTags(tag) }
    workingDir = rootDir
}

data class CustomCompilerVersion(val rawVersion: String) {
    val sanitizedVersion = rawVersion.replace('.', '_').replace('-', '_')
    override fun toString() = sanitizedVersion
}

fun Project.customCompilerTest(
    version: CustomCompilerVersion,
    taskName: String,
    tag: String,
): TaskProvider<Test> {
    val configurationName = "customCompilerArtifacts_$version"

    val configuration = configurations.findByName(configurationName)
        ?: configurations.create(configurationName).also {
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-compiler-embeddable:${version.rawVersion}")
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-stdlib-js:${version.rawVersion}") {
                attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
            }
            project.dependencies.add(configurationName, "org.jetbrains.kotlin:kotlin-test-js:${version.rawVersion}") {
                attributes { attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir) }
            }
        }

    val downloadTask = getOrCreateTask<Sync>("downloadCustomCompilerArtifacts_$version") {
        from(configuration)
        into(layout.buildDirectory.dir("customCompiler_$version"))
    }

    return projectTest(taskName, jUnitMode = JUnitMode.JUnit5) {
        setUpJsBoxTests(tag)

        dependsOn(downloadTask)
        systemProperty("kotlin.internal.js.test.compat.customCompilerArtifactsDir", downloadTask.get().outputs.files.first().absolutePath)
        systemProperty("kotlin.internal.js.test.compat.customCompilerVersion", version.rawVersion)
    }
}

fun Project.customFirstPhaseTest(rawVersion: String): TaskProvider<Test> {
    val version = CustomCompilerVersion(rawVersion)

    return customCompilerTest(
        version = version,
        taskName = "testCustomFirstPhase_$version",
        tag = "custom-first-phase"
    )
}

fun Project.customSecondPhaseTest(rawVersion: String): TaskProvider<Test> = customCompilerTest(
    version = CustomCompilerVersion(rawVersion),
    taskName = "testCustomSecondPhase",
    tag = "custom-second-phase"
)

/* Custom-first-phase test tasks for different compiler versions. */
customFirstPhaseTest("1.9.20")
customFirstPhaseTest("2.0.0")
customFirstPhaseTest("2.1.0")
customFirstPhaseTest("2.2.0")
// TODO: Add a new task for the "custom-first-phase" test here.

/* Custom-second-phase test task for the latest compiler version. */
// TODO: Update the compiler version to the latest one.
customSecondPhaseTest("2.2.0")

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
