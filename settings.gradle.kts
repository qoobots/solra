rootProject.name = "solra"

// services/ 下所有子模块
file("services").listFiles()?.filter { it.isDirectory }?.forEach { serviceGroup ->
    file(serviceGroup).listFiles()?.filter { it.isDirectory }?.forEach { service ->
        include("services:${serviceGroup.name}:${service.name}")
        project(":services:${serviceGroup.name}:${service.name}").projectDir =
            file("services/${serviceGroup.name}/${service.name}")
    }
}

// clients/android
include("clients:android:app")
