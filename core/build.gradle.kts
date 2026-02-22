plugins {
    id("java-library")
}

group = "at.sfischer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")
    implementation("org.javatuples:javatuples:1.2")

    implementation("org.reflections:reflections:0.10.2")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    // Show detailed test results in the console
    testLogging {
        events("passed", "skipped", "failed") // Log passed, skipped, and failed tests
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL // Show full stack traces for failures
        showStandardStreams = true // Log standard output and error streams of tests
    }

    // Customize the summary displayed after tests run
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) { // This ensures the summary is only printed once at the end
                println("""
                    ----------------------------------------
                    Test results:
                    Executed: ${result.testCount}
                    Successful: ${result.successfulTestCount}
                    Failed: ${result.failedTestCount}
                    Skipped: ${result.skippedTestCount}
                    ----------------------------------------
                """.trimIndent())
            }
        }

        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
    })
}