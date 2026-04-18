plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.mockito:mockito-core:5.21.0")
}

tasks.test {
    useJUnitPlatform()
}