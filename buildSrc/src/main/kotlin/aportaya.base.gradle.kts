// Convenciones comunes a todo modulo Java del monorepo: plataforma y servicios.
plugins {
    `java-library`
    jacoco
    id("com.diffplug.spotless")
}

group = "bo.aportaya"
version = "1.0.0"

java {
    // Toolchain 21, no "el JDK que tenga la maquina". Si no esta, Gradle lo baja.
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -Werror: una advertencia que nadie mira es una advertencia que no existe.
    options.compilerArgs.addAll(
        listOf("-Xlint:all,-processing,-serial,-this-escape", "-Werror", "-parameters"),
    )
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Testcontainers necesita saber donde esta el socket de Docker. En Linux lo
// encuentra solo; en macOS con Docker Desktop y en WSL2 no siempre, y el sintoma es
// "Could not find a valid Docker environment" en la maquina de otro. Se propaga lo
// que el entorno ya diga, sin cablear ninguna ruta en el repositorio.
val entornoDocker = listOf("DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_API_VERSION", "TESTCONTAINERS_RYUK_DISABLED")
    .associateWith { providers.environmentVariable(it) }

// docker-java —el cliente que trae Testcontainers— pide por omision la API 1.32,
// y Docker Engine 29 exige 1.40 como minimo: la peticion vuelve con 400 y, a traves
// del proxy de Docker Desktop, con un cuerpo que no dice nada. 1.41 la soporta
// cualquier motor desde 2020 y satisface el minimo de los actuales.
val apiDeDocker = "1.41"

val saltada = Regex("""<testcase name="([^"]*)"[^>]*>\s*<skipped""")

tasks.withType<Test>().configureEach {
    entornoDocker.forEach { (clave, valor) -> valor.orNull?.let { environment(clave, it) } }
    systemProperty("api.version", apiDeDocker)
    environment("DOCKER_API_VERSION", apiDeDocker)

    // Ninguna prueba saltada. No es celo: jqwik reporta una propiedad como SALTADA
    // cuando no puede correrla, y el build queda verde con mil casos de cuadre que
    // nunca se ejecutaron. Una prueba saltada miente mejor que una que falta.
    val resultados = reports.junitXml.outputLocation
    val corredor = name
    doLast {
        val carpeta = resultados.get().asFile
        if (!carpeta.isDirectory) return@doLast
        val saltadas = carpeta.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .flatMap { archivo -> saltada.findAll(archivo.readText()).map { it.groupValues[1] } }
            .toList()
        require(saltadas.isEmpty()) {
            "$corredor dejo ${saltadas.size} prueba(s) saltada(s); ninguna @Disabled:\n" +
                saltadas.joinToString("\n") { "  $it" }
        }
    }
}

// --------------------------------------------------------------- contenedores --
//
// Cada modulo corre en su propia JVM, asi que cada uno arranca su PostgreSQL de
// Testcontainers y le aplica las 304 tablas de `sql/aplicar.sql`. Con
// `org.gradle.parallel=true` y seis modulos con pruebas de integracion, eso son
// seis contenedores compitiendo por CPU y disco: las pruebas empiezan a fallar por
// tiempo sin que nada este mal, que es la peor clase de fallo — enseña a
// desconfiar del gate.
//
// El limite se pone aca y no subiendo los timeouts: el gate que dice «ninguna
// prueba de caso de uso tarda mas de 120s» sigue intacto, que es el que importa.
// Lo que se acota es cuantas arrancan a la vez.
abstract class LimiteDeContenedores : BuildService<BuildServiceParameters.None>

val limiteDeContenedores =
    gradle.sharedServices.registerIfAbsent("limiteDeContenedores", LimiteDeContenedores::class) {
        maxParallelUsages.set(2)
    }

// Los cinco corredores. Uno solo con todo adentro es un corredor que nadie corre
// en local porque tarda cinco minutos.
tasks.named<Test>("test") {
    usesService(limiteDeContenedores)
    // Sin filtro de motores: corren Jupiter, jqwik y ArchUnit. Nombrar dos deja
    // fuera al tercero, y un servicio entero se queda sin pruebas de arquitectura
    // sin que nadie lo note.
    useJUnitPlatform()
    exclude(
        "**/CU*Test.class",
        "**/*RepositorioTest.class",
        "**/Aislamiento*Test.class",
        "**/Arranque*Test.class",
        "**/*ContratoTest.class",
        "**/*SagaTest.class",
        "**/*E2ETest.class",
        "**/*BarridoTest.class",
        // La capa web tiene su corredor: no porque sea lenta —no levanta contenedor—
        // sino porque cargar el contexto MVC no entra en los 5s de un atomo, y el
        // sintoma seria «timeout» en la primera clase de cada modulo.
        "**/*WebTest.class",
        // troncal(corredores): nombres reservados del carril PR4-seguridad que
        // tambien necesitan Testcontainers — ver el comentario en `integrationTest`.
        "**/Libro*Test.class",
        "**/AppendOnlyTest.class",
        "**/AuditoriaCriticaTest.class",
        "**/SegundoFactor*Test.class",
        "**/Reconciliacion*Test.class",
        "**/*ResilienciaTest.class",
        "**/AutorizacionNegativaTest.class",
    )
    systemProperty("junit.jupiter.execution.timeout.default", "5s")
    testLogging {
            events("failed")
            // Con solo `events("failed")`, el CI imprime el NOMBRE de la prueba caida y
            // nada mas: ni el mensaje ni la causa. Cada rojo obligaba a reproducirlo en
            // otra maquina para enterarse de que decia — y cuando el rojo solo aparece en
            // Linux, eso es media hora por intento. El motivo va en el log.
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showCauses = true
            showStackTraces = true
        }
}

// El source set se toma FUERA del bloque de configuracion: dentro, `the<...>()`
// resuelve contra la tarea y no contra el proyecto, y falla recien cuando alguien
// realiza la tarea — es decir, en la maquina de otro.
val pruebas = the<SourceSetContainer>()["test"]


fun corredor(
    nombre: String,
    descripcion: String,
    patrones: List<String>,
    tiempo: String,
    // Los corredores que levantan PostgreSQL comparten el limite; los que no, no
    // tienen por que hacer cola detras de ellos. La capa web es el caso: sin esto,
    // el corredor mas rapido del repositorio esperaria a los mas lentos.
    conContenedor: Boolean = true,
) =
    tasks.register<Test>(nombre) {
        group = "verification"
        description = descripcion
        testClassesDirs = pruebas.output.classesDirs
        classpath = pruebas.runtimeClasspath
        useJUnitPlatform()
        patrones.forEach { include(it) }
        // Un servicio sin sagas todavia no es un servicio roto.
        failOnNoDiscoveredTests = false
        systemProperty("junit.jupiter.execution.timeout.default", tiempo)
        // El arranque del contenedor NO es una prueba, y por eso tiene su propio
        // presupuesto. Cada modulo corre en su JVM, asi que con `org.gradle.parallel`
        // arrancan tantos PostgreSQL como servicios haya, y cada uno aplica las 304
        // tablas de `sql/aplicar.sql`. Con tres servicios entraba en 120s; con cinco
        // ya no, y las pruebas fallaban por «timeout» sin que nada estuviera mal.
        //
        // Se separa en vez de subir el limite de las pruebas: el gate que dice
        // «ningun caso de uso tarda mas de 120s» sigue intacto, que es el que importa.
        systemProperty("junit.jupiter.execution.timeout.beforeall.method.default", "600s")
        if (conContenedor) usesService(limiteDeContenedores)
        testLogging {
            events("failed")
            // Con solo `events("failed")`, el CI imprime el NOMBRE de la prueba caida y
            // nada mas: ni el mensaje ni la causa. Cada rojo obligaba a reproducirlo en
            // otra maquina para enterarse de que decia — y cuando el rojo solo aparece en
            // Linux, eso es media hora por intento. El motivo va en el log.
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showCauses = true
            showStackTraces = true
        }
    }

corredor(
    "integrationTest",
    "Casos de uso y repositorios contra PostgreSQL real (Testcontainers)",
    // `Arranque*Test` levanta el contexto de Spring entero: es lo unico que comprueba
    // que el proceso ARRANCA —con su guardia, sus beans y su decodificador de token— y
    // no solo que las piezas compilan. Sin el, un servicio puede estar verde y no
    // levantar en el primer despliegue.
    //
    // troncal(corredores): los cuatro patrones de abajo son los nombres de test
    // RESERVADOS del carril PR4-seguridad (Marcelo, Q-05 del encargo:
    // `LibroInvariantesTest`, `LibroBenchmarkTest`, `AppendOnlyTest`,
    // `AuditoriaCriticaTest` — `Aislamiento*Test` ya estaba). Sin esto ninguno corria
    // bajo Testcontainers: `AuditoriaCriticaTest` caia en el corredor `test` (5s de
    // timeout, sin contenedor) y fallaba por "timeout" sin que nada estuviera mal —
    // exactamente el sintoma que este archivo ya documenta para el resto de los
    // corredores. Cambio puramente aditivo: ningun patron existente se toca.
    listOf(
        "**/CU*Test.class",
        "**/*RepositorioTest.class",
        "**/Aislamiento*Test.class",
        "**/Arranque*Test.class",
        "**/Libro*Test.class",
        "**/AppendOnlyTest.class",
        "**/AuditoriaCriticaTest.class",
        // H2 (carril PR2, nucleo-financiero): SegundoFactorStepUpTest extiende
        // BaseDeBilletera (Testcontainers) pero no es un CU/Repositorio/Arranque —
        // sin este patron cae en `test`, cuyo timeout de 5s por metodo (linea ~119)
        // es para pruebas puras y no le alcanza a un @BeforeAll que arma un
        // contenedor. Micro-PR de una linea, generalizable a cualquier
        // SegundoFactor*Test futuro.
        "**/SegundoFactor*Test.class",
        // H4.S2.M3 (carril PR2, nucleo-financiero): mismo motivo exacto que
        // SegundoFactor*Test arriba — ReconciliacionDeRetirosTest tambien extiende
        // BaseDeBilletera y tampoco calza con CU/Repositorio/Arranque.
        "**/Reconciliacion*Test.class",
        // H4.S2.M4 (carril PR2, nucleo-financiero): pruebas de resiliencia
        // (@Retry/@CircuitBreaker) que arrancan el contexto completo — mismo motivo,
        // generalizable a cualquier *ResilienciaTest futuro de cualquier servicio.
        "**/*ResilienciaTest.class",
        // H4.S2.M5 (carril PR2, nucleo-financiero): IDOR contra PostgreSQL real
        // (RLS) — mismo motivo, generalizable a cualquier AutorizacionNegativaTest
        // futuro de cualquier servicio.
        "**/AutorizacionNegativaTest.class",
    ),
    "120s",
)
// `LibroBenchmarkTest` (H3.S2 del carril PR4-seguridad) mide, no verifica: 200
// transferencias concurrentes en 3 corridas no tiene lugar en un gate que corre en
// cada guardado. Se compila con el resto de `integrationTest` (para que un cambio
// que lo rompa se note) pero se EXCLUYE de la ejecucion automatica; se corre a mano
// con `./gradlew :servicios:nucleo-financiero:integrationTest --tests
// '*LibroBenchmarkTest*'`.
tasks.named<Test>("integrationTest") {
    // `-PcorrerBenchmarks` es la unica forma de que la etiqueta "benchmark" corra:
    // sin la propiedad, queda excluida (el gate normal); con ella, un humano pidio
    // explicitamente medir. `./gradlew :servicios:nucleo-financiero:integrationTest
    // --tests '*LibroBenchmarkTest*' -PcorrerBenchmarks`.
    if (!project.hasProperty("correrBenchmarks")) {
        useJUnitPlatform { excludeTags("benchmark") }
    } else {
        useJUnitPlatform()
    }
}
// La capa web (ADR-043): el corte MVC con dobles del caso de uso. Sin contenedor y
// sin base, asi que corre en la maquina de cualquiera y en cada guardado. Es donde se
// prueban el estado HTTP, el JSON, la validacion del contrato, el manejador de errores
// y —lo que ninguna prueba de integracion ve— que la guardia niegue con 403 al
// autenticado sin permiso y con 401 al que no trae sesion.
corredor(
    "webTest",
    "Contrato HTTP de los controladores · MockMvc, sin contenedor",
    listOf("**/*WebTest.class"),
    // 30s y no 5s: lo que tarda es cargar el contexto MVC la primera vez, no la
    // prueba. Bajarlo convierte el arranque de Spring en un fallo de la primera
    // clase de cada modulo, que es el peor mensaje de error posible.
    "30s",
    conContenedor = false,
)

// La capa web entra en `check`: es la suite rapida, y una suite rapida que hay que
// acordarse de correr no la corre nadie.
tasks.named("check") { dependsOn("webTest") }

corredor("contractTest", "Contratos entre pares de servicios", listOf("**/*ContratoTest.class"), "60s")
corredor("sagaTest", "Sagas con dobles de los servicios participantes", listOf("**/*SagaTest.class"), "120s")
corredor("e2eTest", "Punta a punta sobre compose --profile todo", listOf("**/*E2ETest.class"), "300s")

// Cobertura como PISO, no como meta. No se excluye codigo dificil para subir el
// numero: la pregunta de ADR-026 es «que del dinero no esta probado», y un porcentaje
// alto conseguido excluyendo lo dificil la contesta al reves.
//
// ADR-026 fija tres pisos distintos, y por eso hay tres propiedades y no una:
//
//   extra["pisoDeCobertura"] = 0.80   // lineas del modulo entero
//   extra["pisoDeRamas"]     = 0.70   // ramas del modulo entero
//   extra["pisoDelDominio"]  = 0.95   // lineas Y ramas de dominio/, en los de dinero
//
// Sin declaracion no hay piso todavia, y la regla no corre. Una regla que no corre es
// preferible a una que corre sobre nada: la segunda deja el build verde diciendo que
// midio.
fun piso(clave: String): Double = (project.findProperty(clave) as? Number)?.toDouble() ?: 0.0

// -------------------------------------------------------- datos de ejecucion --
//
// El informe se arma con lo que ejecutaron TODOS los corredores, no solo `test`.
// Con el cableado por omision del plugin, la cobertura se mide contra los atomos
// solamente: los casos de uso corren en `integrationTest` y el contrato HTTP en
// `webTest`, asi que un servicio con veinte pruebas de caso de uso aparecia con la
// cobertura de sus tres atomos. Ese numero no medía lo que la suite prueba, medía
// que corredor se habia elegido — y con un piso encima, habria obligado a bajar el
// piso hasta volverlo inutil.
//
// `mustRunAfter` y no `dependsOn`: quien corre `cobertura` decide que suites correr.
// Atarlo a `integrationTest` obligaria a tener Docker para medir la cobertura de un
// modulo que no toca la base.
val corredoresDeCobertura = listOf("test", "webTest", "integrationTest", "contractTest", "sagaTest")

val ejecuciones = files(corredoresDeCobertura.map { layout.buildDirectory.file("jacoco/$it.exec") })

// Lo GENERADO no cuenta, ni a favor ni en contra. jOOQ produce ~110 clases por
// servicio y el generador de OpenAPI otras tantas; con ellas adentro, la cobertura de
// `aportes` daba 12 % teniendo su dominio al 84 %. Un numero asi no mide nada: mide
// cuantas tablas tiene el esquema.
//
// Es la unica exclusion, y es la que ADR-026 nombra. NO se excluye codigo dificil para
// subir el porcentaje: services, controllers, validadores, calculo financiero y manejo
// de errores cuentan enteros.
val soloEscritoAMano: (org.gradle.api.file.FileTree) -> org.gradle.api.file.FileTree = { arbol ->
    arbol.matching { exclude("**/generado/**", "**/generated/**") }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")
    // Nunca leer un .exec que un corredor todavia esta escribiendo. Sin esto, correr
    // `verificar` —que lanza los corredores en paralelo— hacia que JaCoCo midiera un
    // archivo a medio escribir y el piso fallara por una carrera y no por cobertura.
    // `mustRunAfter` y no `dependsOn`: quien mide decide que suites correr.
    mustRunAfter(corredoresDeCobertura)
    executionData.setFrom(ejecuciones.filter { it.exists() })
    classDirectories.setFrom(files(classDirectories.files.map { soloEscritoAMano(fileTree(it)) }))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Se configura la tarea que el plugin YA cablea contra el .exec de `test`. Una
// JacocoCoverageVerification registrada a mano no tiene datos de ejecucion y se
// saltea sola — verde, y sin haber medido nada.
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("test")
    mustRunAfter(corredoresDeCobertura)
    executionData.setFrom(ejecuciones.filter { it.exists() })
    classDirectories.setFrom(files(classDirectories.files.map { soloEscritoAMano(fileTree(it)) }))
    onlyIf { piso("pisoDeCobertura") > 0.0 || piso("pisoDelDominio") > 0.0 }
    violationRules {
        if (piso("pisoDeCobertura") > 0.0) {
            rule {
                limit {
                    counter = "LINE"
                    minimum = piso("pisoDeCobertura").toBigDecimal()
                }
            }
        }
        if (piso("pisoDeRamas") > 0.0) {
            rule {
                limit {
                    counter = "BRANCH"
                    minimum = piso("pisoDeRamas").toBigDecimal()
                }
            }
        }
        // El piso alto va sobre `dominio/` y no sobre el modulo: ahi viven el calculo
        // del dinero, los plazos y las transiciones de estado, que es lo que ADR-026
        // manda cubrir al 95 %. Exigirle lo mismo al modulo entero obligaria a probar
        // el mapeo de un DTO con el mismo celo que una comision, y el resultado seria
        // aflojar el numero para todos.
        if (piso("pisoDelDominio") > 0.0) {
            rule {
                element = "PACKAGE"
                // Solo `dominio`, sin sus subpaquetes. La regla de JaCoCo se aplica
                // paquete por paquete, y `dominio.puertos` son INTERFACES: cero lineas
                // ejecutables y por lo tanto cero por ciento, siempre. Exigirles el piso
                // del calculo del dinero seria exigirle cobertura a una declaracion.
                includes = listOf("*.dominio")
                limit {
                    counter = "LINE"
                    minimum = piso("pisoDelDominio").toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    minimum = piso("pisoDelDominio").toBigDecimal()
                }
            }
        }
    }
}

val cobertura = tasks.register("cobertura") {
    group = "verification"
    description = "El piso de cobertura del ambito de este modulo"
    dependsOn("jacocoTestCoverageVerification")
}

tasks.named("check") { dependsOn(cobertura) }

// Barrido: las reglas propias que no son de arquitectura viven en
// plataforma/comun-pruebas como pruebas, no como convencion de revision — y cada
// servicio las aplica sobre sus propias fuentes (planes/00 §6).
corredor("testBarrido", "Las reglas propias sobre las fuentes de este modulo", listOf("**/*BarridoTest.class"), "60s")

tasks.named("check") { dependsOn("testBarrido") }
