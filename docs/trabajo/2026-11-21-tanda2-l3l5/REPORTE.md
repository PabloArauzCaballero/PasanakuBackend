# Reporte — Tanda 2, lote L3L5

> **AVANCE: 1 / 24 — 4,2 %.**

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `correcciones/tanda2-l3l5-test-admision`
- Peldaño de evidencia alcanzado: T2.M0 `TESTED` (reproducción bajo carga antes/después + suites del servicio).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M0 | El arranque único de Mockito ya no cae dentro de `armar()`; con la CPU repartida la clase pasa 3/3 (antes `TimeoutException` a 7,3 s) sin cambiar ningún límite | `taskset -c 0 ./gradlew --no-daemon :servicios:grupos:test --tests '*AdmisionDelGrupoSustitucionTest' --rerun` ×3 con 3 procesos ocupados en el mismo núcleo | PASS · [evidencia](./evidencia/T2.M0-mockito-arranque.txt) |

## A medias
### T2.M0b — El mismo arreglo para todos los servicios
- Qué anda: en `grupos`, Mockito entra como `-javaagent` y su arranque se paga en `MockitoAlAbrirLaSesion`.
- Qué no anda: los otros servicios siguen con el auto-attach de Mockito dentro de la primera prueba que mockea.
- Qué falta exactamente: mover el bloque `tasks.withType<Test>` de `servicios/grupos/build.gradle.kts` a
  `buildSrc/src/main/kotlin/aportaya.base.gradle.kts`, y `MockitoAlAbrirLaSesion` + su archivo
  `META-INF/services` a `plataforma/comun-pruebas` (con `junit-platform-launcher` en su `api`); después borrar la
  copia de `grupos`. Lo hace la sesión dueña de `buildSrc`/`plataforma`.
- Dónde quedó: rama `correcciones/tanda2-l3l5-test-admision`, compila, suites de `grupos` en verde.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Resto de H1.S1, H1.S2 y H2 | TODO | Se ejecutan en orden en esta sesión |

## Evidencia
- [`evidencia/T2.M0-mockito-arranque.txt`](./evidencia/T2.M0-mockito-arranque.txt)

## No cubierto
- Corredores con Testcontainers de `grupos` (`integrationTest`): no corren sin Docker.

## Desvíos del plan
- El `PLAN.md` se escribió después de la primera edición de `servicios/grupos/build.gradle.kts` (la reproducción
  y el diagnóstico se hicieron antes). Se registra como desvío de la regla 20.

## Riesgos residuales
- Bajo la carga extrema simulada el primer caso tarda 3,3 s de 5 s: el margen restante es trabajo propio de la
  clase (mockear cuatro tipos del proyecto).

## Decisiones y ambigüedades
- Se declaró `junit-platform-launcher` como `testImplementation` de `grupos` (ya estaba en el catálogo y en el
  runtime de pruebas; solo cambia `testCompileClasspath` en el lockfile). A confirmar con Pablo al hacer T2.M0b.
