# Plan — Continuar traspaso Pasanaku (PR #503–#505 → dev → test)

- Fecha: 2026-11-23 · Repos afectados: PasanakuBackend (PasanakuFrontend solo lectura de estado) · Predecesor: traspaso del 2026-11-23 (ramas `codex/claude-handoff-*`)
- Resultado observable: las PR #503, #504 y #505 quedan con CI completo en verde e integradas a `dev`; `test` recibe solo ese conjunto validado.
- Kill-test: cualquier check requerido en rojo sobre el head de una PR, o `integrationTest` de `servicios/grupos` en rojo en #504.

## Alcance
- IN: corregir los fallos reales de CI de #503 y #504, vigilar #505, merge a `dev` de PR verdes, promoción `dev → test` si todo el conjunto está verde.
- OUT: `main` (sin autorización de Pablo); checkpoints M08 y M6 (incompletos, no se integran); PR a `main` #468/#495–#497; Frontend `c28f69bb`.
- Ambigüedades registradas:
  - M08 TTL: ¿24 meses estricto o "casi 25" por partición mensual? Supuesto: no se decide en este trabajo (DECISION_REQUIRED, confirmar con Pablo).
  - El traspaso atribuía el rojo de #504 a un lock de JaCoCo; el log de CI muestra otra causa (ver hallazgos). Se sigue la evidencia del log.

## Hallazgos (descubrimiento, con evidencia en `evidencia/`)
- #503 "Formato, reglas propias y compilación" (job 138033735520): Maven Central respondió **429** al resolver `kotlin-stdlib:2.4.0` → clase `ENVIRONMENT`, no `spotlessCheck`.
- #503 "Frontend ADR-044" (job 138035184830): `yarn typecheck` del backoffice falla con TS4111 en `pantalla-de-parametros-financieros.spec.ts` líneas 98, 99, 145, 146 → `TEST_BUG`.
- #504 "Los cinco corredores" (job 138026360778): `servicios:grupos:integrationTest` 337 tests, 10 fallan con `IllegalStateException: El turno … no tiene titular` en `TurnosAsignadosRepositorio.delGrupo:30` → `PRODUCT_BUG`.
- Corrección: #504 se clasifica `TEST_BUG`/`DATA` (dato de prueba irreal), no `PRODUCT_BUG`.
- #505: head actual `ed9bb51`; "Formato" ya en verde; "Los cinco corredores" en curso al abrir este plan.

## H1 — PR #503–#505 en verde e integradas a dev
**CA:** Dado el head de cada PR, cuando CI termina, entonces todos los checks no omitidos están en `success` y la PR se integra a `dev`.
**DoD:** check-runs del head en success (salida de `gh api …/check-runs` pegada) · merge registrado.
**Estado:** EN CURSO

### H1.S1 — #503 CU-132
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Corregir TS4111 en el spec (acceso con corchetes) | typecheck del backoffice sin errores TS4111 | `yarn typecheck` en apps/backoffice → exit 0 | EN CURSO — commit 31234da; local: 0 errores en el spec (161 ajenos por clientes generados ausentes); falta CI |
| H1.S1.M2 | Re-ejecutar CI de #503 (429 era de entorno) | Formato y Frontend en success | check-runs del head nuevo | TODO |
| H1.S1.M3 | Confirmar doble aprobación por persona distinta con `TARIFARIO_ADMINISTRAR` | existe test/control que rechaza aprobación del mismo proponente | `grep` del control + test pasando en CI | TODO |

### H1.S2 — #504 turnos asignados
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Reproducir y localizar causa de "turno sin titular" | causa enunciada explica los 10 fallos | lectura de código + reproducción | HECHO — fixture `cuposOcupados` creaba OCUPADO sin participante; producción siempre fija participante (CU69Enlace:144, CU69FirmarReglamento:96/177); los 10 tests usan la fixture |
| H1.S2.M2 | Corregir la causa en producto | los 10 tests pasan sin debilitar aserciones | `./gradlew :servicios:grupos:integrationTest` local o CI verde | EN CURSO — commit 319e7b3 (fixture); local BLOCKED sin daemon Docker; espera CI |

### H1.S3 — #505 CU-44
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Esperar "Los cinco corredores" y triagear si falla | todos los checks en success | check-runs del head | TODO |

### H1.S4 — Integración
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Merge a `dev` de cada PR verde | `origin/dev` contiene los heads | `git branch -r --contains <head>` | TODO |

## H2 — Promoción dev → test
**CA:** Dado `dev` con #503–#505 y CI verde, cuando se promueve, entonces `test` apunta a ese contenido y el health responde.
**DoD:** CI de `dev` verde · promoción por la ruta acordada · `estado` ejecutado por Pablo en su host (fuera de este entorno: SSH bloqueado).
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Revisar diff `test..dev` y CI de `dev` | sin cambios inesperados, CI verde | `git diff --stat origin/test origin/dev` + check-runs | TODO |
| H2.S1.M2 | Promover `dev → test` | `origin/test` contiene `dev` | `git branch -r --contains` | TODO |
| H2.S1.M3 | Health en TEST | `estado` responde OK | salida provista por Pablo | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Rate-limit 429 de Maven Central en CI | rojo espurio | re-ejecutar el job; si persiste, registrar como ENVIRONMENT |
| Sin Java/Gradle/Postgres local | no se reproduce local | usar CI como verificación, declarado |
| SSH a TEST bloqueado | no se observa el health | Pablo ejecuta `estado` (acción en su host, no simulable) |
