plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":core"))

    implementation("org.javatuples:javatuples:1.2")
    implementation("org.json:json:20240303")

    implementation("org.apache.commons:commons-csv:1.12.0")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}