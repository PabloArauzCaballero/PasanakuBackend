plugins {
    `kotlin-dsl`
}

// Los plugins de convencion viven aca para que un servicio nuevo herede todo sin
// copiar nada: catorce servicios iguales, no catorce servicios parecidos.
dependencies {
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.spring.dep.mgmt))
    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.openapi.generator))
    // Version literal, NO por el catalogo: gradle/libs.versions.toml es
    // "MICRO-PR, nunca una rama de carril" (paso 19a de ci.yml lo hace cumplir),
    // y el micro-PR con la entrada real ya esta abierto y sin mergear
    // (PR #2, pablo/troncal/cyclonedx). Hasta que se mergee y esta rama
    // rebase, la version vive aca literal — MISMA version (3.4.1) que el
    // catalogo del micro-PR, para que el rebase no cambie nada mas que esta
    // linea por "implementation(plugin(libs.plugins.cyclonedx))".
    implementation("org.cyclonedx.bom:org.cyclonedx.bom.gradle.plugin:3.4.1")
    implementation(libs.codegen.jooq)
    implementation(libs.driver.postgresql)
}

fun plugin(dependencia: Provider<PluginDependency>): Provider<String> =
    dependencia.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
