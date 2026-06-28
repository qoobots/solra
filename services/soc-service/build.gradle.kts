plugins {
    id("solra.java-service")
}

group = "com.solra.soc"
version = "0.1.0"

dependencies {
    implementation(project(":services:common"))
    // TODO: WebRTC signaling support
}

application {
    mainClass.set("com.solra.soc.SocServiceApplication")
}

docker {
    name = "solra/soc-service:${version}"
}
