plugins {
    id("solra.java-service")
}

group = "com.solra.crt"
version = "0.1.0"

dependencies {
    implementation(project(":services:common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

application {
    mainClass.set("com.solra.crt.CrtServiceApplication")
}

docker {
    name = "solra/crt-service:${version}"
}
