plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
}

group = "at.sfischer.payroll"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("at.sfischer.spring.payroll.PayrollApplication")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.3.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.3.3")
    implementation("org.springframework.boot:spring-boot-starter-hateoas:3.3.3")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    implementation("com.h2database:h2:2.3.232")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "at.sfischer.spring.payroll.PayrollApplication"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}