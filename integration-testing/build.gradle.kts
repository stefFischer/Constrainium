plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))

    implementation(project(":generator:random"))

    implementation(project(":driver:rest"))

    implementation("org.javatuples:javatuples:1.2")

    implementation(platform("org.junit:junit-bom:5.10.0"))
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.assertj:assertj-core:3.26.0")

    testImplementation(project(":test-systems:TestSystemRunner"))
}

tasks.test {
    useJUnitPlatform()
}