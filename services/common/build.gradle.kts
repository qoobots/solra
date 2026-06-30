plugins {
    id("java-library")
}

description = "Solra Common Shared Library"

dependencies {
    // Spring Boot starters (shared across all services)
    api(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-validation")

    // Observability
    api("io.micrometer:micrometer-tracing-bridge-otel")
    api("io.micrometer:micrometer-registry-prometheus")
    api("io.opentelemetry:opentelemetry-exporter-otlp")

    // gRPC (shared client/server)
    // Use 3.1.0 for Spring Boot 3.x compatibility (avoids Hoxton.RC1 dependency issue)
    api("net.devh:grpc-spring-boot-starter:3.1.0.RELEASE")

    // Kafka (event publishing/consuming)
    api("org.springframework.kafka:spring-kafka")

    // Utilities
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.google.guava:guava:33.2.0-jre")

    // Security
    api("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
