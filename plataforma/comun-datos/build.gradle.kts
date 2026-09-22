// conContexto(): SET LOCAL dentro de la transaccion, la fabrica de DSLContext y el
// pool. Toda consulta del proyecto pasa por aca (invariante 3).
plugins { id("aportaya.libreria") }

dependencies {
    api(project(":plataforma:comun-dominio"))
    api(libs.jooq)
    implementation(libs.spring.boot.jdbc)
    compileOnly(libs.jakarta.xml.bind)
    runtimeOnly(libs.postgresql)

    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.bundles.pruebas)
    testImplementation(libs.spring.boot.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.38
extra["pisoDeRamas"] = 0.31
