plugins {
    id("solra.java-service")
}

group = "com.solra.not"
version = "0.1.0"

dependencies {
    implementation(project(":services:common"))
    // TODO: APNs / FCM push SDK integration
}

application {
    mainClass.set("com.solra.not.NotServiceApplication")
}

docker {
    name = "solra/not-service:${version}"
}
