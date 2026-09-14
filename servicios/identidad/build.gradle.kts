// identidad — generado por scripts/nuevo_servicio.py
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
extra["pisoDeCobertura"] = 0.79
extra["pisoDeRamas"] = 0.63

aportaya {
    esquema.set("identidad")
    rol.set("svc_identidad")
}

dependencies {
    implementation(project(":plataforma:comun-dominio"))
    implementation(project(":plataforma:comun-datos"))
    // El puerto de archivos: la cedula y la selfie van al servidor de archivos,
    // no al disco del contenedor (ADR-034).
    implementation(project(":plataforma:comun-archivos"))
    implementation(project(":plataforma:comun-web"))
    implementation(libs.spring.boot.oauth2)  // identidad FIRMA: necesita nimbus-jose, no solo verificar
    implementation(project(":plataforma:comun-mensajeria"))

    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.validation)
    implementation(libs.jooq)
    implementation(libs.kafka)
    implementation(libs.shedlock)
    implementation(libs.resilience4j)
    implementation(libs.micrometer)
    implementation(libs.argon2)          // credenciales: hash lento con pimienta
    implementation(libs.spring.boot.security)

    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.spring.boot.jdbc)
    testImplementation(libs.bundles.pruebas)   // JUnit 5, AssertJ, Testcontainers, ArchUnit
}

// JPA esta PROHIBIDO (ADR-016): compite con sql/ por la propiedad del esquema y
// su dirty checking es incompatible con append-only. La convencion falla el build
// si alguien lo agrega, pero dejarlo escrito acá ahorra la discusion.
