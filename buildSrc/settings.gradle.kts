pluginManagement {
    repositories {
        // El classpath del propio buildSrc (kotlin-dsl y sus dependencias) se resuelve
        // con ESTOS repositorios, no con los del settings raiz.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // Espejo de Maven Central en Google: los runners de CI reciben 429 de repo.maven.apache.org.
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        // El mismo catalogo que el build principal: los plugins de convencion no
        // pueden quedar en otra version que los servicios que configuran.
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
