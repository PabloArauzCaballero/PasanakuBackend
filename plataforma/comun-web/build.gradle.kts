// Manejador global de errores, idempotencia, guardia por omision y traza. Lo que
// hace que un endpoint nuevo nazca cerrado y devuelva AP-CU<NN>-<nn>, no un stack.
// `java-test-fixtures` y no un modulo nuevo: el arnes de la capa web necesita las
// clases de ESTE modulo (la guardia, el manejador de errores, la cadena de
// seguridad), y un modulo aparte que dependiera de comun-web no podria a su vez ser
// usado por las pruebas de comun-web sin cerrar un ciclo. Los fixtures viven del lado
// correcto de esa flecha.
plugins {
    id("aportaya.libreria")
    `java-test-fixtures`
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.57
extra["pisoDeRamas"] = 0.47

dependencies {
    api(project(":plataforma:comun-dominio"))
    api(project(":plataforma:comun-datos"))
    api(project(":plataforma:comun-mensajeria"))  // el outbox se cablea aca: es donde vive el cableado
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.jdbc)   // DataAccessException: la traduccion de restricciones
    implementation(libs.spring.boot.validation)
    api(libs.spring.boot.security)
    implementation(libs.spring.boot.oauth2)
    implementation(libs.micrometer.tracing)

    // El arnes de la capa web: se publica para los catorce servicios, asi que sus
    // dependencias son `api` — un servicio no vuelve a declarar como se arma un corte
    // MVC ni con que se fabrica una sesion.
    // Estos dos son `implementation` en el modulo, asi que no llegan solos: el arnes
    // expone `RequestMappingHandlerMapping` y `Jwt` en su propia firma, y por eso los
    // declara como `api` y no como detalle interno.
    testFixturesApi(libs.spring.boot.web)
    testFixturesApi(libs.spring.boot.oauth2)
    testFixturesApi(libs.spring.boot.test)
    testFixturesApi(libs.spring.security.test)
    testFixturesApi(libs.junit.jupiter)
    testFixturesApi(libs.assertj)
    testFixturesApi(libs.mockito)
    testFixturesApi(libs.mockito.junit)

    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.bundles.pruebas)
    testImplementation(testFixtures(project(":plataforma:comun-web")))
    testRuntimeOnly(libs.junit.platform.launcher)
}

// erroresCatalogo — nombre de restriccion -> R-XXX-nn, generado desde sql/.
// Se genera en build/ y NO se versiona: el gate es que se pueda regenerar, no un
// diff que alguien tiene que acordarse de actualizar.
val catalogoDeErrores = layout.buildDirectory.dir("generated/recursos")

val erroresCatalogo = tasks.register<Exec>("erroresCatalogo") {
    group = "generadores"
    description = "Catalogo de errores: constraint_name -> R-XXX-nn, desde sql/"
    workingDir = rootDir
    executable = "python3"
    val salida = catalogoDeErrores.map { it.file("errores-restricciones.properties") }
    outputs.file(salida)
    inputs.files(
        rootProject.layout.projectDirectory.file("sql/50_verificacion/prueba_humo.sql"),
        rootProject.layout.projectDirectory.file("sql/40_reglas/restricciones.sql"),
    )
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf("scripts/generar_errores.py", salida.get().asFile.absolutePath)
        },
    )
}

sourceSets["main"].resources.srcDir(catalogoDeErrores)
tasks.named("processResources") { dependsOn(erroresCatalogo) }
