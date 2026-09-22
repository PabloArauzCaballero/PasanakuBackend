// aportes — generado por scripts/nuevo_servicio.py
// Las versiones salen del catalogo (gradle/libs.versions.toml): una dependencia
// nueva es un micro-PR al troncal, nunca un cambio en una rama de carril.
plugins {
    id("aportaya.servicio")          // convencion: toolchain 21, spotless, test, docker
    id("aportaya.jooq")              // genera SOLO el esquema de este servicio
    id("aportaya.openapi")           // interfaz de servidor + clientes
    id("aportaya.mutacion")          // H6.S1 (carril PR4-seguridad): mutation testing, opcional
}

// H6.S1: dominio puro de `aportes` como primer objetivo de mutation testing —
// `VerificadorDeFirmaWebhook` (HMAC-SHA256, comparacion en tiempo constante) ya
// tiene 8 tests unitarios dirigidos (`VerificadorDeFirmaWebhookTest`): es la pieza
// de seguridad critica de este servicio, y el candidato natural para confirmar que
// esos tests realmente matan mutantes y no solo ejecutan lineas.
// `./gradlew :servicios:aportes:pitest`
pitest {
    targetClasses.set(setOf("bo.aportaya.aportes.dominio.*"))
    targetTests.set(setOf("bo.aportaya.aportes.dominio.*"))
}

// PIT 1.19.0 declara una dependencia ESTRICTA (`strictly`) sobre
// junit-platform-launcher:1.12.2, que choca con la version 1.13.1 que trae el
// BOM de Spring Boot 3.5.6. La tarea `pitest` reutiliza `testRuntimeClasspath`
// directamente (el plugin no crea una configuracion propia — confirmado
// listando `./gradlew :servicios:aportes:configurations`), asi que no hay forma
// de acotar este override solo a PIT: se aplica a `testRuntimeClasspath`
// entero. Verificado que no rompe nada real: `./gradlew :servicios:aportes:test
// :servicios:aportes:webTest` sigue en verde con este override puesto. Sin el
// override, `pitest` ni siquiera arranca: falla resolviendo
// `testRuntimeClasspath` ("Did not resolve junit-platform-launcher:1.12.2
// which has been forced / substituted to a different version: 1.13.1") —
// encontrado corriendo esto de verdad, no supuesto. Cualquier "strictly" en
// conflicto se rechaza sin importar COMO se llegue a la otra version (fuerza,
// sustitucion o el BOM), asi que la unica salida es bajar el launcher a la
// version exacta que PIT exige.
configurations.matching { it.name == "testRuntimeClasspath" }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.junit.platform:junit-platform-launcher"))
            .using(module("org.junit.platform:junit-platform-launcher:1.12.2"))
            .because("PIT 1.19.0 pide 1.12.2 en 'strictly' (H6.S1); el BOM de Spring Boot pide 1.13.1, " +
                "y test/webTest/integrationTest de aportes se re-verificaron en verde con este override")
    }
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.88
extra["pisoDeRamas"] = 0.69
extra["pisoDelDominio"] = 0.73   // lineas Y ramas de dominio/

aportaya {
    esquema.set("aportes")
    rol.set("svc_aportes")
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
