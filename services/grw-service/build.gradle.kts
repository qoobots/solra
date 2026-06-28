plugins {
    id("solra.java-service")
}

group = "com.solra.grw"
version = "0.1.0"

dependencies {
    implementation(project(":services:common"))
}

application {
    mainClass.set("com.solra.grw.GrwServiceApplication")
}

docker {
    name = "solra/grw-service:${version}"
}
