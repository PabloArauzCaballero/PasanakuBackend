// Mutation testing (PIT) — H6.S1 del carril PR4-seguridad, micro-PR troncal.
//
// Convencion OPCIONAL: a diferencia de `aportaya.base`/`aportaya.servicio`, ningun
// modulo la aplica por omision. Instrumentar bytecode y correr la suite completa
// varias veces es caro (minutos por clase objetivo) — cablearlo en el build normal
// convertiria cada `./gradlew check` en un build de PIT, y eso es exactamente lo que
// ADR-026 ya evita para la cobertura de linea (ver el comentario de `pisoDeCobertura`
// en `aportaya.base.gradle.kts`). Un modulo que quiera medir mutantes aplica este
// plugin el mismo (`apply(plugin = "aportaya.mutacion")` en su `build.gradle.kts`) y
// declara sus propias `targetClasses`.
//
// Version verificada contra la doc publicada de gradle-pitest-plugin el 2026-09-22
// (`gradle/libs.versions.toml` tiene el detalle): el smoke test oficial del plugin
// solo confirma Gradle hasta 7.4.2, sin mencion de Gradle 9.x. Por eso esta
// convencion se corrio de verdad contra Gradle 9.7.1 antes de mergear este cambio
// (ver `docs/auditoria-produccion/mutation-testing.md` para el resultado real, no
// supuesto).
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest")
}

// Se usa `configure<PitestPluginExtension>` con el tipo explicito, no el
// accessor implicito `pitest { }`: en un plugin de convencion de `buildSrc`
// (a diferencia de aplicarlo directo en el `build.gradle.kts` de un servicio)
// Gradle 9.7.1 no genera el accessor tipado para la extension y `pitest`
// resuelve contra `DependencyHandler.pitest(...)` en su lugar — encontrado
// corriendo esto de verdad, no supuesto.
configure<PitestPluginExtension> {
    // pitest-junit5-plugin: la version la fija gradle/libs.versions.toml
    // (`pitest-junit5`), coordinada con el JUnit Jupiter que trae el BOM de Spring
    // Boot 3.5.6 (5.11+): pitest-junit5-plugin 1.0.0+ es lo que la propia doc de
    // gradle-pitest-plugin recomienda para esa version de JUnit Platform.
    junit5PluginVersion.set(
        providers.gradleProperty("pitestJunit5Version").orElse("1.2.3"),
    )
    // Sin `targetClasses` por omision: cada modulo que aplique esta convencion
    // declara las suyas. Sin esto, PIT intentaria mutar el modulo entero —
    // incluido el codigo GENERADO por jOOQ/OpenAPI, que `aportaya.base.gradle.kts`
    // ya excluye explicitamente de la cobertura de linea por la misma razon.
    outputFormats.set(setOf("HTML", "XML"))
}
