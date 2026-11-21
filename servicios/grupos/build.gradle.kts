// grupos — generado por scripts/nuevo_servicio.py
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
extra["pisoDeCobertura"] = 0.76
extra["pisoDeRamas"] = 0.61

aportaya {
    esquema.set("grupos")
    rol.set("svc_grupos")
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
    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.spring.boot.jdbc)
    testImplementation(libs.bundles.pruebas)   // JUnit 5, AssertJ, Testcontainers, ArchUnit
    // API de `LauncherSessionListener` (MockitoAlAbrirLaSesion): ya esta en el catalogo y
    // Gradle la carga igual al correr las pruebas; aca solo se compila contra ella.
    testImplementation(libs.junit.platform.launcher)
}

// JPA esta PROHIBIDO (ADR-016): compite con sql/ por la propiedad del esquema y
// su dirty checking es incompatible con append-only. La convencion falla el build
// si alguien lo agrega, pero dejarlo escrito acá ahorra la discusion.

// Mockito entra como agente al ARRANCAR la JVM de prueba, no dentro de la primera
// prueba que mockea. Sin esto, Mockito se auto-adjunta (attach dinamico de
// byte-buddy-agent) en el primer `mock()`, y ese costo cae dentro del `@BeforeEach`
// de esa clase, que tiene el limite de 5s de los atomos. Con la CPU del runner
// compartida, `AdmisionDelGrupoSustitucionTest.armar()` pasaba de 1,5s a mas de 5s
// y fallaba de forma intermitente (causa demostrada en
// docs/trabajo/2026-11-21-tanda2-l3l5/evidencia/). Es la configuracion que pide la
// documentacion de Mockito 5 para JDK 21 (Mockito#0.3). No cambia ningun limite.
// El resto del arranque (ByteBuddy, Objenesis) lo paga MockitoAlAbrirLaSesion.
tasks.withType<Test>().configureEach {
    val clases = configurations.named("testRuntimeClasspath")
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            val agente = clases.get().files.single { it.name.startsWith("mockito-core-") }
            listOf("-javaagent:${agente.absolutePath}")
        },
    )
}
