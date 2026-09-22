// cumplimiento — generado por scripts/nuevo_servicio.py
// Las versiones salen del catalogo (gradle/libs.versions.toml): una dependencia
// nueva es un micro-PR al troncal, nunca un cambio en una rama de carril.
plugins {
    id("aportaya.servicio")          // convencion: toolchain 21, spotless, test, docker
    id("aportaya.jooq")              // genera SOLO el esquema de este servicio
    id("aportaya.openapi")           // interfaz de servidor + clientes
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.89
extra["pisoDeRamas"] = 0.73
extra["pisoDelDominio"] = 0.70   // lineas Y ramas de dominio/

aportaya {
    esquema.set("cumplimiento")
    rol.set("svc_cumplimiento")
}

dependencies {
    implementation(project(":plataforma:comun-dominio"))
    implementation(project(":plataforma:comun-datos"))
    implementation(project(":plataforma:comun-web"))
    implementation(project(":plataforma:comun-mensajeria"))

    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)
    implementation(libs.jooq)
    implementation(libs.kafka)
    implementation(libs.shedlock)
    implementation(libs.resilience4j)
    implementation(libs.micrometer)

    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.spring.boot.jdbc)
    testImplementation(libs.bundles.pruebas)   // JUnit 5, AssertJ, Testcontainers, ArchUnit
}

// JPA esta PROHIBIDO (ADR-016): compite con sql/ por la propiedad del esquema y
// su dirty checking es incompatible con append-only. La convencion falla el build
// si alguien lo agrega, pero dejarlo escrito acá ahorra la discusion.
