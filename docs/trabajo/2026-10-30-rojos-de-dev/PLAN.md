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
| H2.S2.M1 | Ver si `16 · sagaTest` y `16b` cobertura pasan | pasan en la CI de la PR, o se agrega una microtarea por cada rojo | CI (`sagaTest` necesita Docker: no corre en local) | TODO |

### H2.S3 — Trivy (`17b2`)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S3.M1 | Leer el informe: qué CVE, qué paquete, si hay versión corregida | lista exacta | log del paso `17b2` · `evidencia/trivy-y-locks.txt` | HECHO |
| H2.S3.M2 | Subir `guava` y `snappy-java` (restricciones + lockfiles) y nombrar `libbrotli1` en el Dockerfile | los 4 hallazgos desaparecen de Trivy | compila y pasan las pruebas de `identidad` · lockfiles: solo cambian guava, snappy y las transitivas de guava · **falta** construir la imagen y correr Trivy (CI) | A MEDIAS |

### H2.S4 — Criterios de CU-58 (`verificar_criterios.py`, escondido por el impacto del cambio)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S4.M1 | 5 escenarios nuevos en la bóveda y `@DisplayName` de las 5 pruebas alineado (sin tocar aserciones) | el verificador no encuentra divergencias | `python3 scripts/verificar_criterios.py` → «Sin divergencias entre la boveda y el codigo.» · spotless/compile/testBarrido de `auditoria` OK | HECHO |

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
- Se agregaron H1.S3 (14 pruebas de `f6`) y H2.S4 (5 criterios de CU-58): no estaban en el plan, salieron al correr localmente lo que la CI no ejecutaba (`f6`, `verificar_criterios.py`). Se clasificaron como pruebas atrasadas por cambios intencionales de otras sesiones, con evidencia en cada caso.
- H2.S1 no usa «Parcial»: el CU-07 de la bóveda ya está implementado en `auditoria`; documentar el prefijo de `identidad` como un CU-07 «parcial» habría escrito el error de etiqueta como si fuera hecho.
- Mi corrida local del e2e del backoffice regeneró cientos de capturas versionadas (`docs/trabajo/**/capturas`, `evidencia/`, `apps/web/capturas`); se restauraron y no forman parte del cambio.
