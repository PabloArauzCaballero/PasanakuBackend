// Testcontainers, fixtures y los barridos. Se expone con `api` a proposito: un
// servicio no vuelve a declarar como se levanta una PostgreSQL de prueba.
plugins { id("aportaya.libreria") }

dependencies {
    api(project(":plataforma:comun-dominio"))
    api(libs.bundles.pruebas)
    api(libs.testcontainers.kafka)
    api(libs.jqwik)
    api(libs.junit.jupiter)
    api(libs.snakeyaml)
    implementation(libs.spring.boot.jdbc)
    runtimeOnly(libs.postgresql)

    testRuntimeOnly(libs.junit.platform.launcher)
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.64
extra["pisoDeRamas"] = 0.64
