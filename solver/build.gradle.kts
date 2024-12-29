plugins {
    id("java-library")
}

group = "at.sfischer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))

    // TODO Try to use Z3 Java API directly since "org.sosy-lab:java-smt" does not work.
//    implementation("com.microsoft.z3:javaAPI:4.8.8")
    implementation("tools.aqua:z3-turnkey:4.13.0.1")

    // FIXME Cannot get Z3 to run with this solver library.
//    implementation("org.sosy-lab:java-smt:5.0.1")
//    implementation("org.sosy-lab:javasmt-solver-mathsat5:5.6.11")
//    implementation("org.sosy-lab:javasmt-solver-z3:4.13.3")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
}

tasks.test {
    useJUnitPlatform()
}