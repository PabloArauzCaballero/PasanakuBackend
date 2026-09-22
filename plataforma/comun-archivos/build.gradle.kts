// El puerto de archivos y su adaptador S3 (MinIO). Trece tablas del modelo guardan
// un archivo (ADR-034) y ninguna guarda una URL publica: guardan una clave de objeto.
plugins { id("aportaya.libreria") }

dependencies {
    api(project(":plataforma:comun-dominio"))
    implementation(libs.minio)
    compileOnly(libs.spotbugs.anotaciones)
    // Spring solo para `@Component` y `@Value`: el adaptador se registra como bean.
    // El puerto y la clave de objeto no lo tocan.
    implementation(libs.spring.boot.web)

    testImplementation(libs.bundles.pruebas)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Piso de cobertura. Lo que se prueba sin servidor es la clave de objeto y el ambito,
// que es donde se decide que entra y que no; hablar con MinIO se prueba con el stack
// levantado, no en unitarias.
extra["pisoDeCobertura"] = 0.0
extra["pisoDeRamas"] = 0.0
