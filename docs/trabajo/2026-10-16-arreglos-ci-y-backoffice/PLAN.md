# Plan — Arreglar el backoffice de TEST y los rojos de CI en `test` y `dev`

- Fecha: 2026-10-16 · Repos: Pasanaku (backend, backoffice, host TEST) · Predecesor: `2026-10-16-codigo-acceso-por-correo`
- Resultado observable: el backoffice de TEST se despliega con la versión actual (muestra el correo en
  expedientes KYC); en `test` y `dev` compilan los tests de identidad y pasan spotless, el barrido y el formato de Dart.
- Kill-test: `docker images aportaya/backoffice:test` sigue con fecha anterior a hoy, o
  `./gradlew :servicios:identidad:compileTestJava` falla en `test`.
- Pedido: "Corregilo por favor de una vez" (Pablo), sobre la lista de problemas ajenos del reporte anterior.

## Hallazgos
1. **Backoffice:** la build falla por `TS2339 'correo' … 'ExpedienteEnRevision'`. El OpenAPI de `test` tiene el campo
   (`1a086b93`), pero el cliente generado en el host (`/opt/aportaya/repo/clientes/angular/identidad/model/expedienteEnRevision.ts`)
   es del 2026-10-15 19:22 y no lo trae. `clientes/angular` no está versionado, y `reconstruir-si-cambio.sh` (solo vive en el host)
   no regenera clientes cuando cambia un OpenAPI.
2. **`test`:** `VerificacionesControllerWebTest.java:55-66` construye `ExpedienteDeIdentidad` con 11 argumentos sin `telefono`,
   y no existe ese constructor (TEST_BUG de `1a086b93`). Además, `ExpedienteDeIdentidad.java` no pasa spotless.
3. **`dev`:** spotless falla en `RevisionRepositorio.java` y `CU02RevisionTest.java`; el barrido, en `RevisionRepositorio.java`
   (≥300 líneas) y `UsuariosControllerWebTest.java` (300); el formato Dart, en 5 archivos de `packages/diseno_flutter` y
   `apps/movil/lib/pantallas/identidad/pantalla_portada.dart` (`553bd786`).
4. En `test`, el barrido de identidad también marca `RevisionRepositorio.java` (313) y `UsuariosControllerWebTest.java` (300).

## Alcance
- IN: los puntos 1–4, con el cambio mínimo. Para el tamaño, se separa sin cambiar comportamiento.
- OUT: borrar usuarios de TEST (bloqueado por permiso, ver el reporte anterior); cualquier cambio de comportamiento.
- Ambigüedad: "corregilo" se interpreta como "los problemas ajenos listados". Supuesto informado a Pablo en el chat.
- Desvío registrado: el arreglo de `VerificacionesControllerWebTest` se escribió antes que este plan (incumple la regla 20).

## H1 — `test` verde y backoffice desplegado
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `VerificacionesControllerWebTest`: constructor completo con `telefono = null` | Los tests de identidad compilan y `webTest` pasa | `./gradlew :servicios:identidad:webTest` | HECHO (evidencia/01) |
| H1.S1.M2 | spotless en `ExpedienteDeIdentidad.java` | `spotlessJavaCheck` pasa | `./gradlew :servicios:identidad:spotlessJavaCheck` | HECHO (evidencia/01) |
| H1.S1.M3 | Barrido: separar `RevisionRepositorio` y `UsuariosControllerWebTest` por debajo de 300 líneas, sin cambiar comportamiento | `testBarrido` pasa y la regresión de identidad sigue igual | `./gradlew :servicios:identidad:check :servicios:identidad:integrationTest` | HECHO (evidencia/01) |
| H1.S2.M1 | Host: regenerar el cliente Angular de identidad, reconstruir el backoffice y desplegar | La imagen `aportaya/backoffice:test` es de hoy y el contenedor está en marcha | `docker images` + `docker ps` + `grep correo` en el cliente | TODO |
| H1.S2.M2 | Host: `reconstruir-si-cambio.sh` regenera los clientes Angular cuando cambia un OpenAPI | Un cambio en `servicios/*/src/main/resources/openapi/` dispara la regeneración | `bash -n` + diff del guion (copia `.bak`) | TODO |

## H2 — `dev` verde
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | spotless en `RevisionRepositorio`/`CU02RevisionTest` y separación por el barrido (como H1.S1.M3) | `check` de identidad sin rojos | `./gradlew :servicios:identidad:check` | TODO |
| H2.S1.M2 | `dart format` en los 6 archivos de `553bd786` | El lint de `diseno-flutter` y `movil` no cambia nada | `dart format --set-exit-if-changed` | TODO |

## H3 — Publicación
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | PRs a `test` y `dev`, CI y merge | MERGED | `gh pr view` | TODO |
| H3.S1.M2 | Redespliegue en Contabo verificado | Imágenes nuevas y contenedores `healthy` | `docker ps` | TODO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Host saturado (carga ~30) por el autodespliegue | Builds fallan por recursos | No lanzar nada en el host mientras el temporizador construye (regla 70.1.4) |
| El guion del host no está versionado | Un cambio sin respaldo | Copia `.bak` con fecha, y el diff va en la evidencia |
