plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "Solra SOC Service - Social & Share Engine"
group = "com.solra.soc"
version = "0.1.0"

dependencies {
    implementation(project(":services:common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // gRPC Server + Client
    implementation("net.devh:grpc-server-spring-boot-starter:2.6.0.RELEASE")
    implementation("net.devh:grpc-client-spring-boot-starter:2.6.0.RELEASE")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.data:spring-data-redis")
    implementation("org.springframework.kafka:spring-kafka")

    // JSON processing for domain↔entity mapping
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
}

application {
    mainClass.set("com.solra.soc.SocServiceApplication")
}

docker {
    name = "solra/soc-service:${version}"
}
