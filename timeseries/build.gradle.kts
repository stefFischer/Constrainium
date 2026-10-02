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

    implementation(project(":driver:rest"))

    implementation(project(":extensions:metamorphic-testing"))

    implementation("org.javatuples:javatuples:1.2")

    implementation("org.apache.hadoop:hadoop-common:3.4.2")
    implementation("org.apache.hadoop:hadoop-mapreduce-client-core:3.4.2")
    implementation("org.apache.parquet:parquet-avro:1.18.0")

    implementation("org.apache.commons:commons-csv:1.14.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
}

tasks.test {
    useJUnitPlatform()
}