import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

// ADR-020: el contrato se escribe primero y el servidor se genera de el. Lo que el
// controlador implementa es una interfaz generada, no una firma escrita a mano: es
// lo que hace que "el codigo se aparto del contrato" no pueda pasar en silencio.
plugins {
    id("aportaya.base")
    id("org.openapi.generator")
}

val servicio = project.name

// El paquete Java del servicio NO es su nombre de proyecto: `nucleo-financiero` vive
// en `bo.aportaya.nucleofinanciero`. Si el generador arma su paquete desde el nombre
// del proyecto, el guion se convierte en guion bajo y el codigo generado cae en
// `bo.aportaya.nucleo_financiero` — otro arbol de paquetes. El controlador que lo
// importa deja de cumplir `ArquitecturaTest > ningunImportCruzado`, porque para
// ArchUnit es otro servicio.
val paquete = servicio.replace("-", "")
val contrato = layout.projectDirectory.file("src/main/resources/openapi/$servicio.yaml")

// El generador interpreta `inputSpec` como URI. Una ruta absoluta de Windows
// (`C:\...`) no lo es: falla con «Illegal character in opaque part at index 2» y
// tumba `compileJava` en las tres maquinas Windows del parque. La forma `file:` es
// URI en las cinco.
val rutaDelContrato = contrato.asFile.toURI().toString()
val servidor = layout.buildDirectory.dir("generated/openapi")
val clienteTs = rootProject.layout.projectDirectory.dir("clientes/typescript/$servicio")

// Un contrato sin operaciones todavia no genera nada, y eso no es un error: es la
// Fase 0, donde los borradores existen y estan vacios a proposito.
fun tieneOperaciones(): Boolean {
    val f = contrato.asFile
    return f.isFile && !f.readText().contains(Regex("""(?m)^paths:\s*\{\s*\}\s*$"""))
}

val generarServidor = tasks.register<GenerateTask>("generarServidorOpenApi") {
    group = "build"
    description = "Interfaz de servidor de $servicio desde su OpenAPI"
    generatorName.set("spring")
    inputSpec.set(rutaDelContrato)
    outputDir.set(servidor.map { it.asFile.absolutePath })
    apiPackage.set("bo.aportaya.$paquete.web.generado")
    modelPackage.set("bo.aportaya.$paquete.web.generado.modelo")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "openApiNullable" to "false",
            "useJakartaEe" to "true",
        ),
    )
    onlyIf { tieneOperaciones() }
}

// clientes/typescript/ es GENERADO y no se edita a mano: el CI regenera y falla si
// hay diff. Un tipo escrito ahi es una divergencia esperando a ocurrir.
// Dos clientes desde el mismo contrato (ADR-044): uno para Angular (backoffice y sitio)
// y otro para Flutter (la app). Los importes quedan como `String` en los dos porque el
// contrato los declara `type: string, format: decimal`; la prueba `importes-son-cadena`
// del CI lo verifica en cada regeneracion.
val clienteAngular = rootProject.layout.projectDirectory.dir("clientes/angular/$servicio")
val clienteDart = rootProject.layout.projectDirectory.dir("clientes/dart/$servicio")

val generarClienteAngular = tasks.register<GenerateTask>("generarClienteAngular") {
    group = "build"
    description = "Cliente Angular de $servicio para apps/backoffice y apps/web"
    generatorName.set("typescript-angular")
    inputSpec.set(rutaDelContrato)
    outputDir.set(clienteAngular.asFile.absolutePath)
    configOptions.set(
        mapOf(
            "providedInRoot" to "true",
            "stringEnums" to "true",
            "withInterfaces" to "true",
            "modelPropertyNaming" to "original",
            "supportsES6" to "true",
            "ngVersion" to "20.0.0",
        ),
    )
    onlyIf { tieneOperaciones() }
}

val generarClienteDart = tasks.register<GenerateTask>("generarClienteDart") {
    group = "build"
    description = "Cliente Dart (dio) de $servicio para apps/movil"
    generatorName.set("dart-dio")
    inputSpec.set(rutaDelContrato)
    outputDir.set(clienteDart.asFile.absolutePath)
    additionalProperties.set(
        mapOf(
            "pubName" to "aportaya_cliente_${paquete}",
            "pubLibrary" to "aportaya_cliente_${paquete}",
        ),
    )
    configOptions.set(
        mapOf(
            "serializationLibrary" to "json_serializable",
            "useEnumExtension" to "true",
        ),
    )
    onlyIf { tieneOperaciones() }
    // El generador escribe `sdk: '>=3.5.0'`, pero json_serializable 6.9 emite elementos
    // null-aware (`?instance.x`), que son Dart 3.8. Sin esto build_runner no compila
    // el `.g.dart` de ningun modelo con campos opcionales.
    doLast {
        val pubspec = clienteDart.file("pubspec.yaml").asFile
        if (pubspec.isFile) {
            pubspec.writeText(pubspec.readText().replace("sdk: '>=3.5.0 <4.0.0'", "sdk: '>=3.8.0 <4.0.0'"))
        }
    }
}

tasks.register("generarClientes") {
    group = "build"
    description = "Los dos clientes de $servicio: Angular y Dart"
    dependsOn(generarClienteAngular, generarClienteDart)
}

sourceSets["main"].java.srcDir(servidor.map { it.dir("src/main/java") })

tasks.named<JavaCompile>("compileJava") { dependsOn(generarServidor) }
