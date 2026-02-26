plugins {
    java
    id("org.springframework.boot") version "2.5.15"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.evomaster"
version = "3.4.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

val springfoxVersion = "3.0.0"
val swaggerVersion = "1.6.14"

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("io.springfox:springfox-spring-web:$springfoxVersion")

    implementation("io.springfox:springfox-swagger2:$springfoxVersion") {
        exclude(group = "io.swagger", module = "swagger-annotations")
        exclude(group = "io.swagger", module = "swagger-models")
    }

    implementation("io.springfox:springfox-swagger-ui:$springfoxVersion")

    implementation("io.swagger:swagger-annotations:$swaggerVersion")
    implementation("io.swagger:swagger-models:$swaggerVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveFileName.set("rest-ncs-sut.jar")
}