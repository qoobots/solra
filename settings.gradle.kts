rootProject.name = "solra"

// services/ 下所有子模块（扁平结构：services/<service-name>/）
file("services").listFiles()?.filter {
    it.isDirectory && !it.name.startsWith(".") && it.name != "01docs"
}?.forEach { service ->
    if (file("services/${service.name}/build.gradle.kts").exists()) {
        include("services:${service.name}")
        project(":services:${service.name}").projectDir = file("services/${service.name}")
    }
}

// clients/android
include("clients:android:app")
