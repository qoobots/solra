plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Solra SAF Service - Content Safety & Moderation"

dependencies {
    implementation(project(":services:common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // gRPC Server
    implementation("net.devh:grpc-server-spring-boot-starter:2.6.0.RELEASE")
    // gRPC Client (to AI safety model service)
    implementation("net.devh:grpc-client-spring-boot-starter:2.6.0.RELEASE")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.kafka:spring-kafka")

    // Observability
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
}
