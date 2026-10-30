# Plan — Dejar en verde los rojos que quedaron en `dev`

- Fecha: 2026-10-30 · Repos afectados: PasanakuBackend · Predecesor: `2026-10-29-cerrar-boveda-dev`
- Resultado observable: la CI de un push a `dev` (workflow «CI», todos los jobs que corren con el cambio) termina sin rojos; `dev` y `test` quedan iguales en lo que toca este trabajo.
- Kill-test: el último push a `dev` (ae38c444, run 40974687042) tiene el job «Frontend — Flutter y Angular» en rojo en el paso `f1`. Si tras el cambio `f1` sigue rojo, o aparece otro paso rojo sin explicar, esto no está hecho.

## Alcance
- IN: los rojos conocidos de `dev`, en el orden en que la CI los destapa: `f1` (formato Dart), `f5` (44 px), los de `f6` que ese `f5` escondía, `15 · contractTest`, los criterios de CU-58 y Trivy `17b2`.
- OUT: cambiar lo que hacen las funciones (solo formato, tamaño de objetivos táctiles, pruebas atrasadas, documentación y versiones de dependencias con parche publicado); tocar la política de la CI; suprimir un check; debilitar o saltar un test; renombrar clases de otra sesión.
- Ambigüedades registradas:
  - **CU-07 en `identidad`** (a confirmar con el autor de 509dad69): el código llama `CU07FotoDePerfil` a la foto de perfil, pero en la bóveda CU-07 es «Ejercer derechos sobre datos personales», que ya implementa `auditoria` (`CU07EjercerDerechos`). Supuesto tomado: el README de `identidad` nombra el CU diciendo qué es y avisando la discrepancia; no se renombra la clase.
  - **Escenarios nuevos de CU-58** (a confirmar con quien hizo #287/#290): se redactaron solo con lo que cada prueba ya verifica.
  - **Altura táctil de 44 px** en componentes que el backoffice había hecho compactos a propósito: se respetó la prueba (contrato del catálogo) en lugar de relajarla.

## H1 — El job Frontend queda verde
**CA:** Dado `dev` con `f1` en rojo, cuando se formatean los 3 archivos, se corrigen los controles de < 44 px y se actualizan las pruebas atrasadas, entonces `f1` a `f7` pasan en la CI.
**DoD:** comandos locales de cada microtarea · CI de la PR con el job Frontend en verde · mismo cambio en `test`.
**Estado:** A MEDIAS (todo verificado en local; falta la CI)

### H1.S1 — Formato Dart
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `dart format` sobre `ruleta_del_sorteo_test.dart`, `pantalla_mfa_test.dart`, `pantalla_mi_puntaje_test.dart` | el formateador no cambia ningún archivo de `lib` ni `test` | `dart format --output=none --set-exit-if-changed lib test` → «620 files (0 changed)», exit 0 (Dart 3.12.2) · `evidencia/dart-format.txt` | HECHO |

### H1.S2 — Objetivos táctiles del catálogo (`f5`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Reproducir `catalogo.spec.ts:48` en local y listar los controles < 44 px | la prueba falla en local con la misma lista que la CI | Playwright, un worker: 11 elementos, idénticos a la CI · `evidencia/f5-catalogo-44px.txt` | HECHO |
| H1.S2.M2 | Llevar esos controles a 44 px reales con tokens (sin literales) y sin mover el diseño | la prueba pasa; el e2e del sitio sigue verde; dos pasadas visuales sin hallazgos abiertos | `yarn workspace @aportaya/web test:e2e` → 36 passed · lint/typecheck/pruebas de `ui`, `backoffice` y `web` en verde · `evidencia/visual/` | HECHO |

### H1.S3 — Pruebas e2e del backoffice atrasadas (`f6`, escondidas por `f5`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | `admisiones.e2e.ts` (2 pruebas): texto del vacío y fin de las pestañas (4f873212) | las 2 pasan sin quitar ninguna verificación | Playwright → pasan · `evidencia/f6-e2e-backoffice.txt` | HECHO |
| H1.S3.M2 | `f2-posiciones-y-roles.e2e.ts` (4 pruebas): sin «Inicio» en el menú (5d0f8d99) y `ADMIN_PLATAFORMA` ve «Sistemas» (3d4344e3) | las 4 pasan y ahora exigen que «Inicio» **no** esté en el menú y que el logo lleve a `/tablero` | Playwright → 10 passed | HECHO |
| H1.S3.M3 | `sistemas-sin-contrato.e2e.ts` (8 pruebas): siete rutas con fuente real exigen tabla o error accionable; dos sin fuente exigen el aviso neutro actual | las 8 pasan; nunca aparece «99.95» | Playwright → pasan | HECHO |

## H2 — Los jobs Java quedan verdes
**CA:** Dado un push que toca Java, cuando corre «Los cinco corredores» y «bootJar y docker build», entonces `13` a `16b` y `17b2` pasan.
**DoD:** comandos locales · CI de la PR con ambos jobs en verde.
**Estado:** A MEDIAS

### H2.S1 — `identidad` documenta CU-07 (`15 · contractTest`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Reproducir `DocumentacionVivaContratoTest` en local | falla con «identidad: implementa [07]…» | `./gradlew :plataforma:comun-pruebas:contractTest` → FAILED, 20 pruebas, 1 falla | HECHO |
| H2.S1.M2 | Nombrar CU-07 en `servicios/identidad/README.md` diciendo qué es y avisando la discrepancia con la bóveda | la prueba pasa | `./gradlew contractTest` (todos los módulos) → BUILD SUCCESSFUL · `evidencia/java-y-boveda.txt` | HECHO |

### H2.S2 — Lo que `15` ocultaba
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S2.M1 | Ver si `16 · sagaTest` y `16b` cobertura pasan | pasan en la CI de la PR, o se agrega una microtarea por cada rojo | CI (`sagaTest` necesita Docker: no corre en local) · «Los cinco corredores» de #316 (run 40991387140, head 70f836b4): success, con `16` y `16b`. En #315 el job cortó antes, en `14` (ver H2.S8) | HECHO |

### H2.S3 — Trivy (`17b2`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S3.M1 | Leer el informe: qué CVE, qué paquete, si hay versión corregida | lista exacta | log del paso `17b2` · `evidencia/trivy-y-locks.txt` | HECHO |
| H2.S3.M2 | Subir `guava` y `snappy-java` (restricciones + lockfiles) y nombrar `libbrotli1` en el Dockerfile | los 4 hallazgos desaparecen de Trivy | compila y pasan las pruebas de `identidad` · lockfiles: solo cambian guava, snappy y las transitivas de guava · CI de #315 (head c6757463): «bootJar y docker build» success y check «Trivy» success; igual en #316 | HECHO |

### H2.S4 — Criterios de CU-58 (`verificar_criterios.py`, escondido por el impacto del cambio)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S4.M1 | 5 escenarios nuevos en la bóveda y `@DisplayName` de las 5 pruebas alineado (sin tocar aserciones) | el verificador no encuentra divergencias | `python3 scripts/verificar_criterios.py` → «Sin divergencias entre la boveda y el codigo.» · spotless/compile/testBarrido de `auditoria` OK | HECHO |

### H2.S5 — Criterios de CU-64 (rojo nuevo: llegó con #311 y #313 mientras se trabajaba)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S5.M1 | Un escenario nuevo para el camino feliz de `CU64TitularYAcuerdoTest` y los 5 nombres de rechazo con el prefijo «rechaza» que la convención de `verificar_criterios.py` exime (sin tocar aserciones) | el verificador no encuentra divergencias | `python3 scripts/verificar_criterios.py` → «Sin divergencias…» · spotless/compileTestJava/testBarrido/contractTest → BUILD SUCCESSFUL | HECHO |

### H2.S6 — Postman desactualizado (rojo nuevo: «Contratos y clientes generados», llegó con #311 y #313)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S6.M1 | Regenerar las colecciones Postman de `grupos` (falta `listarAcuerdos`; cambia la descripción de `traspasarCupo`) | el generador no deja diff | `python3 scripts/generar_postman.py && git diff --exit-code -- postman/` → exit 0 | HECHO |

### H2.S7 — OSV-Scanner (rojo latente: el job solo corre cuando se tocan dependencias, y esta PR toca lockfiles)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S7.M1 | `opentelemetry` 1.62.0 → 1.67.0 (GHSA-ccrj-gw8r-854m, en los 15 lockfiles de Gradle) | compila y pasan las pruebas de unidad de `gateway`, `identidad` y `auditoria` | `./gradlew … test webTest testBarrido` → BUILD SUCCESSFUL · lockfiles: solo cambia la familia OpenTelemetry y sus `okhttp`/`okio` · CI de #315 (head c6757463): «Dependencias vulnerables (OSV-Scanner) / osv-scan» success; igual en #316 | HECHO |
| H2.S7.M2 | 28 paquetes npm de `yarn.lock` (undici, vite, zod, hono, qs, fast-uri, body-parser, @hono/node-server, @modelcontextprotocol/server, @vitest/mocker) a su versión corregida, con `resolutions` porque la cadena de Angular los fija exactos | `yarn npm audit` sin vulnerabilidades; lint, tipos, pruebas, builds y e2e del sitio igual que antes | `yarn lint` 10/10 · `ui` 196+9, `web` 74+8, `backoffice` 1321+94 · builds OK · e2e del sitio 36/36 · CI de #315 y #316: osv-scan success | HECHO |

### H2.S8 — `GruposDeBackofficeRepositorioTest.listado` intermitente (`14 · integrationTest`; falló en la CI de #315 y pasó en #316 con el mismo código)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S8.M1 | Demostrar la causa: `listar(null, null)` ordena por `fecha_inicio DESC NULLS FIRST, codigo_publico, id` con `LIMIT 200`; la fixture pone `fecha_inicio = hoy` y un `codigo_publico` aleatorio en una base compartida sin limpieza | con más de 200 grupos de hoy, algunos quedan fuera del listado | SQL contra PostgreSQL local, en transacción con `ROLLBACK`: de 300 grupos, 100 fuera del listado de 200 | HECHO |
| H2.S8.M2 | La prueba fija la `fecha_inicio` de su grupo en el futuro (`CURRENT_DATE + 3650`) antes de listar, sin tocar ninguna aserción | el grupo de la prueba siempre cae dentro de los primeros 200 | SQL: el peor `codigo_publico` entra primero · spotless/compileTestJava de `grupos` → exit 0 · `evidencia/listado-grupos-backoffice.txt` · **falta** `integrationTest` (solo corre en la CI; no hay Docker acá) | A MEDIAS |

## H3 — Entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | PR a `dev`, checks sin fallos, `mergeable` | PR mergeable | `mergeable` + checks en el estado real | TODO |
| H3.S1.M2 | Mismo cambio a `test` (rama propia desde `test`, cherry-pick) | PR mergeable | ídem | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| No hay Docker: `integrationTest`, `sagaTest` y la imagen solo corren en la CI | cada iteración de Java cuesta ~25 min | agrupar cambios; usar lo que sí corre en local |
| Más rojos ocultos tras `f6` (`f7`) o tras `15` (`16`, `16b`) | trabajo no previsto | cada uno entra como microtarea nueva |
| Concurrencia con otras sesiones que escriben en `dev` | conflictos o rojos nuevos ajenos | consultar el estado antes de abrir y de mergear |

## Desvíos del plan
- Se agregaron H1.S3 (14 pruebas de `f6`), H2.S4 (5 criterios de CU-58) y H2.S5 (6 de CU-64, que llegaron con el merge de `dev` del mismo día), H2.S6 (Postman) y H2.S7 (OSV): los dos últimos aparecieron en la CI de la propia PR: no estaban en el plan, salieron al correr localmente lo que la CI no ejecutaba (`f6`, `verificar_criterios.py`). Se clasificaron como pruebas atrasadas por cambios intencionales de otras sesiones, con evidencia en cada caso.
- H2.S1 no usa «Parcial»: el CU-07 de la bóveda ya está implementado en `auditoria`; documentar el prefijo de `identidad` como un CU-07 «parcial» habría escrito el error de etiqueta como si fuera hecho.
- Mi corrida local del e2e del backoffice regeneró cientos de capturas versionadas (`docs/trabajo/**/capturas`, `evidencia/`, `apps/web/capturas`); se restauraron y no forman parte del cambio.
