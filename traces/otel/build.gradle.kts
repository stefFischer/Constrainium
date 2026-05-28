plugins {
    id("java")
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.65.1"

dependencies {
    implementation(project(":core"))

    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation("org.json:json:20251224")

    implementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")

    implementation("com.github.jsqlparser:jsqlparser:5.3")

    implementation("org.javatuples:javatuples:1.2")

    implementation("io.opentelemetry.proto:opentelemetry-proto:1.3.2-alpha")

    testImplementation(project(":test-systems:TestSystemRunner"))

    testImplementation("org.apache.commons:commons-lang3:3.18.0")

    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.mockito:mockito-core:5.21.0")
}

tasks.test {
    useJUnitPlatform()
}