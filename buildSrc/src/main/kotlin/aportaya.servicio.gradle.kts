import bo.aportaya.gradle.AportayaExtension

// Un servicio desplegable: Spring Boot, el BOM que sincroniza las versiones, y la
// guarda de JPA. Nada de esto se copia catorce veces.
plugins {
    id("aportaya.base")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.cyclonedx.bom")
}

extensions.create<AportayaExtension>("aportaya")

// H2.S2 (regla 90/98): el escaneo de dependencias real (OSV-Scanner) necesita un
// lockfile por modulo desplegable para saber exactamente que version resolvio el
// build, no solo el rango declarado en el catalogo. Se regenera con
// `./gradlew dependencies --write-locks` y se commitea (docs.gradle.org/current/
// userguide/dependency_locking.html, verificado contra Gradle 9.7 instalado).
dependencyLocking {
    lockAllConfigurations()
}

// H2.S3: SBOM CycloneDX por modulo desplegable, como artifact del CI. DSL
// verificada contra el README de CycloneDX/cyclonedx-gradle-plugin ("Custom
// task output", 2026-09-21): `jsonOutput` es un RegularFileProperty, no un
// nombre — hay que darle la ruta completa. Sin esto el default es
// build/reports/sbom/bom.json igual en los quince modulos, y el paso de CI que
// junta los artifacts pisaria uno con otro.
tasks.cyclonedxBom {
    jsonOutput = layout.buildDirectory.file("reports/sbom/${project.name}-bom.json")
    xmlOutput.convention(null as org.gradle.api.file.RegularFile?)
}

val catalogo = extensions.getByType<VersionCatalogsExtension>().named("libs")
// El generador y la libreria de jOOQ tienen que ser la MISMA version: el codigo
// generado por 3.20 llama a metodos que el runtime del BOM (3.19) no tiene, y el
// error aparece recien al compilar 300 clases generadas. Se sobrescribe la
// propiedad del BOM de Spring Boot, que es donde vive la decision.
extra["jooq.version"] = catalogo.findVersion("jooq").get().requiredVersion

// Parches de seguridad posteriores al ultimo BOM OSS de Boot 3.5. Se mantienen
// juntas las familias que Boot administra para no mezclar versiones incompatibles.
extra["jackson-bom.version"] = "2.21.7"
extra["tomcat.version"] = "10.1.60"
extra["netty.version"] = "4.1.138.Final"
extra["log4j2.version"] = "2.25.5"
extra["postgresql.version"] = "42.7.12"
extra["commons-lang3.version"] = "3.18.0"
// GHSA-rcgg-9c38-7xpx (opentelemetry-api 1.49.0, el que trae el BOM de Boot 3.5.16): corregido en 1.62.0.
// GHSA-ccrj-gw8r-854m (opentelemetry-api 1.62.0, OSV-Scanner 2026-10-30): corregido en 1.67.0.
// La API de OpenTelemetry es estable y compatible hacia atrás; el bridge de Micrometer la usa sin cambios.
extra["opentelemetry.version"] = "1.67.0"

// Estas transitivas no las fija el BOM. Las restricciones no agregan paquetes a
// modulos que no los usan; actualizan solamente los que ya llegan por otra via.
dependencies {
    constraints {
        listOf("implementation", "testImplementation").forEach { alcance ->
            add(alcance, "org.apache.commons:commons-compress:1.26.0")
            add(alcance, "at.yawk.lz4:lz4-java:1.11.4")
            add(alcance, "org.bouncycastle:bcprov-jdk18on:1.85.2")
            add(alcance, "org.bouncycastle:bcpkix-jdk18on:1.85.2")
            add(alcance, "org.bouncycastle:bcutil-jdk18on:1.85.2")
            // Trivy (job «bootJar y docker build», paso 17b2) sobre la imagen de identidad, 2026-10-30: HIGH con parche publicado.
            // guava 33.4.8-jre llega por io.minio:minio 8.6.0 (CVE-2026-102554, corregido en 33.7.2-jre) y snappy-java 1.1.10.7 por
            // kafka-clients (CVE-2026-108106 y CVE-2026-93451, corregidos en 1.1.10.9). Los demas servicios traian 1.1.10.5.
            add(alcance, "com.google.guava:guava:33.7.2-jre")
            add(alcance, "org.xerial.snappy:snappy-java:1.1.10.9")
        }
    }
}

// El arnes de la capa web llega por la convencion y no por catorce lineas iguales en
// catorce build.gradle.kts: ese es el archivo que un carril edita y que despues choca
// con los otros cuatro. Un servicio nuevo tiene MockMvc, la sabana de seguridad y la
// fabrica de sesiones sin escribir nada.
dependencies {
    "testImplementation"(testFixtures(project(":plataforma:comun-web")))
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // Capas de Spring Boot: un cambio de codigo no empuja 200 MB de dependencias.
    layered { enabled.set(true) }
}

// ADR-016: JPA compite con sql/ por la propiedad del esquema, y su dirty checking
// es incompatible con append-only. La regla se implementa en la Fase 0 porque la
// tentacion aparece el primer dia.
val sinJpa = tasks.register("sinJpa") {
    group = "verification"
    description = "Falla si alguien declara JPA o configura spring.jpa"
    val declaradas = provider {
        configurations
            .filter { it.isCanBeDeclared }
            .flatMap { it.dependencies }
            .map { "${it.group}:${it.name}" }
    }
    val configuracion = layout.projectDirectory.file("src/main/resources/application.yml").asFile
    val donde = path
    doLast {
        val prohibidas = declaradas.get().filter {
            it.contains("data-jpa") || it.contains("hibernate") || it.contains("jakarta.persistence")
        }
        require(prohibidas.isEmpty()) { "$donde declara JPA, y JPA esta prohibido (ADR-016): $prohibidas" }
        if (configuracion.isFile) {
            val lineas = configuracion.readLines().withIndex().filter { (_, l) -> l.trim().startsWith("jpa:") }
            require(lineas.isEmpty()) {
                "$donde tiene spring.jpa en application.yml (linea ${lineas.first().index + 1}): prohibido (ADR-016)"
            }
        }
    }
}

tasks.named("check") { dependsOn(sinJpa) }
