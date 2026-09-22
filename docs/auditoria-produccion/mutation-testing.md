# Mutation testing (PIT) — H6.S1 del carril PR4-seguridad

> **Estado: HECHO.** El plugin `info.solidsoft.pitest` (versión 1.19.0) quedó
> instalado como convención OPCIONAL de `buildSrc` (`aportaya.mutacion`), aplicado
> a `servicios/aportes` sobre el paquete `dominio`, y **corrido de verdad** contra
> Gradle 9.7.1 con evidencia literal más abajo. Este micro-PR vive en una rama
> separada (`marcelo/chore/pitest-plugin`, PR aparte) porque toca
> `buildSrc/`+`gradle/libs.versions.toml`, y ese archivo se declara "MICRO-PR, nunca
> una rama de carril" en su propio encabezado.

## Qué se instaló

- `gradle/libs.versions.toml`: versión `pitest = "1.19.0"` (plugin de Gradle) y
  `pitest-junit5 = "1.2.3"` (soporte de JUnit 5), ambas verificadas contra la
  documentación publicada el 2026-09-22
  (`github.com/szpak/gradle-pitest-plugin`, `github.com/pitest/pitest-junit5-plugin`)
  antes de aplicarlas.
- `buildSrc/src/main/kotlin/aportaya.mutacion.gradle.kts` (nuevo): convención
  OPCIONAL — ningún módulo la hereda por omisión (a diferencia de
  `aportaya.base`/`aportaya.servicio`), porque instrumentar bytecode y correr la
  suite completa varias veces es caro y no pertenece al build normal (mismo
  criterio que `pisoDeCobertura` en `aportaya.base.gradle.kts`).
- `servicios/aportes/build.gradle.kts`: aplica `aportaya.mutacion` y declara
  `targetClasses`/`targetTests` sobre `bo.aportaya.aportes.dominio.*` — el primer
  módulo en usar la convención.

## Hallazgo real durante la instalación (documentado, no oculto)

La doc publicada de `gradle-pitest-plugin` **no confirma compatibilidad con
Gradle 9.x** (el smoke test oficial solo llega a Gradle 7.4.2). Se aplicó de
todos modos, como convención opcional, y se corrió de verdad contra Gradle
9.7.1 para confirmar si funcionaba en la práctica — no se asumió que "la doc no
lo prueba" significa "no funciona", ni tampoco que "es un plugin conocido"
significa que iba a andar sin problemas.

**Dos problemas reales encontrados y corregidos, ambos con la causa exacta:**

1. **El accessor tipado `pitest { }` no se genera dentro de un plugin de
   convención de `buildSrc`** (a diferencia de aplicarlo directo en el
   `build.gradle.kts` de un servicio): Gradle 9.7.1 resuelve `pitest` contra
   `DependencyHandler.pitest(...)` en su lugar, y falla la compilación de Kotlin
   con "Unresolved reference". Corregido usando el tipo explícito:
   `configure<info.solidsoft.gradle.pitest.PitestPluginExtension> { ... }` en vez
   del bloque `pitest { }` implícito.
2. **PIT 1.19.0 declara una dependencia ESTRICTA (`strictly`) sobre
   `junit-platform-launcher:1.12.2`**, que choca con la versión `1.13.1` que trae
   el BOM de Spring Boot 3.5.6 (la misma que usa el resto del proyecto). La tarea
   `pitest` reutiliza `testRuntimeClasspath` directamente — no crea su propia
   configuración (confirmado con `./gradlew :servicios:aportes:configurations`) —
   así que no hay forma de acotar un fix solo a PIT. Una vez confirmado que
   ninguna sustitución/fuerza HACIA 1.13.1 satisface un `strictly` (Gradle
   rechaza el conflicto sin importar cómo se llegue a la versión distinta), se
   optó por lo contrario: bajar `junit-platform-launcher` a exactamente `1.12.2`
   para todo `testRuntimeClasspath` de `aportes`, vía
   `resolutionStrategy.dependencySubstitution`. **Verificado que no rompe nada
   real**: `./gradlew :servicios:aportes:test :servicios:aportes:webTest
   :servicios:aportes:integrationTest` — los tres, `BUILD SUCCESSFUL` con el
   override puesto.

## Evidencia literal de la corrida real

```text
$ ./gradlew :servicios:aportes:pitest
...
================================================================================
- Statistics
================================================================================
>> Line Coverage (for mutated classes only): 14/60 (23%)
>> 11 tests examined
>> Generated 42 mutations Killed 12 (29%)
>> Mutations with no coverage 30. Test strength 100%
>> Ran 15 tests (0.36 tests per mutation)

BUILD SUCCESSFUL in 1m 40s
```

Desglose por clase (`servicios/aportes/build/reports/pitest/mutations.xml`):

| Clase (`bo.aportaya.aportes.dominio.*`) | Mutantes matados | Total |
|---|---:|---:|
| `VerificadorDeFirmaWebhook` | **12** | **12 (100%)** |
| `EstadoDePagos` | 0 | 1 |
| `MotivoDeReembolso` | 0 | 3 |
| `RecargoDeMora` | 0 | 11 |
| `SaldoDeLaObligacion` | 0 | 7 |
| `SaldoDeLaObligacion$Estado` | 0 | 5 |
| `TipoDeDisputa` | 0 | 3 |

**Lectura, sin inflar ni minimizar**: `VerificadorDeFirmaWebhook` (HMAC-SHA256,
comparación en tiempo constante, la pieza de seguridad crítica de H1.S2) tiene
**100% de mutantes matados** — sus 8 tests unitarios (`VerificadorDeFirmaWebhookTest`)
no solo ejecutan las líneas, las verifican de verdad. Las otras seis clases de
`dominio` en `aportes` muestran 0 mutantes matados de los suyos — **no
necesariamente significa que no tengan pruebas**: `targetTests` se limitó a
`bo.aportaya.aportes.dominio.*`, así que un test que las ejercite desde
`aplicacion`/`web` (paquetes distintos) no cuenta para PIT en esta corrida. Es un
hallazgo real de COBERTURA DE MUTACIÓN AL NIVEL UNITARIO, no una afirmación de
"código sin probar" — queda declarado así, sin conflar ambas cosas.

## Siguiente paso concreto para quien retome

1. Ampliar `targetTests` de `servicios/aportes/build.gradle.kts` a
   `bo.aportaya.aportes.*` completo (no solo `dominio`) para que los tests de
   `aplicacion`/`web` cuenten al medir mutantes de las clases de dominio que
   consumen — probablemente sube el score de `RecargoDeMora`/`SaldoDeLaObligacion`
   sin escribir un test nuevo, solo con el alcance correcto.
2. Extender `aportaya.mutacion` a los demás 13 servicios cuando cada dueño de
   carril lo pida — la convención ya queda lista, cada uno solo agrega
   `id("aportaya.mutacion")` + su propio `pitest { targetClasses.set(...) }`.
3. El override de `junit-platform-launcher` a `1.12.2` es un parche puntual para
   PIT 1.19.0. Si una versión futura del plugin deja de declarar ese `strictly`,
   este override se puede borrar sin más (queda comentado con el motivo exacto en
   `servicios/aportes/build.gradle.kts` para que no se vuelva "código que nadie
   entiende por qué está").
