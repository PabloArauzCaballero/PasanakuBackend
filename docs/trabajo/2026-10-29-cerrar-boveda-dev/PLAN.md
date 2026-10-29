# Plan — Poner en verde la verificación de la bóveda en `dev`/`test`

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend · Predecesor: `2026-10-29-rediseno-bancario-al-repo-desplegado`, `2026-10-21-rediseno-bancario`
- Resultado observable: el workflow «Bóveda, esquema, semillas y despliegue» deja de fallar en el paso «Verificar la bóveda» en `dev` y `test`.
- Kill-test: `python3 scripts/verificar_boveda.py` sigue imprimiendo `FALLA` (hoy: 4 FALLAS sobre `origin/dev` b1e18ec6).

## Alcance
- IN: documentación de la bóveda y de las skills: ADR-053, índice de ADR, índice de skills, cifra de ADR del Plan maestro.
- OUT: código, esquema, contratos, tokens, tests. Cualquier otra falla que aparezca en los pasos que hoy no corren (carriles, contratos, derivados) se registra, no se arregla en este trabajo salvo que sea documental y trivial.
- Ambigüedades registradas:
  - «Motivo» y «Cómo se verifica» de ADR-053 se redactan después de la decisión, solo con lo que el propio ADR y los reportes del rediseño ya dicen. Supuesto: no cambian la decisión. A confirmar por quien aceptó el ADR (dueño).
  - Procedencia de las 10 skills de marketing: el commit 579ddeac solo registra `coreyhaines31/marketingskills (MIT)`, sin SHA. No se inventa SHA.

## H1 — La bóveda verifica en verde
**CA:** Dado `origin/dev` con 4 fallas de `verificar_boveda.py`, cuando se corrigen los 4 documentos, entonces el verificador termina sin `FALLA` y los pasos siguientes del workflow se ejecutan.
**DoD:** `python3 scripts/verificar_boveda.py` sin FALLA · `verificar_carriles.py` y demás pasos del workflow corridos y su resultado registrado · CI de la PR en verde · mismo cambio en `test`.
**Estado:** TODO

### H1.S1 — Corregir los cuatro documentos
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | ADR-053: secciones «Motivo» y «Cómo se verifica» | las 6 secciones existen | `verificar_boveda.py` ya no lista ADR-053 en «6 secciones» | HECHO |
| H1.S1.M2 | ADR-053 en `_Arquitectura.md` | fila 053 en el índice | `verificar_boveda.py` ya no lista ADR-053 en «índice» | HECHO |
| H1.S1.M3 | 10 skills de marketing en `.claude/skills/README.md` | todas listadas con procedencia | «índice de skills completo (291 skills)» OK | HECHO |
| H1.S1.M4 | «50 ADR» → «51 ADR» en el Plan maestro | cifra = vigente | «ninguna cifra desfasada» OK | HECHO |
| H1.S1.M5 | *(agregada)* Asignar a `F1-M` las 13 skills huérfanas en `planes/19` (aparecieron al correr `verificar_carriles.py`, antes oculto tras el fallo de la bóveda) | «289/291 asignadas» → todas | `verificar_carriles.py` sin FALLA | HECHO |
| H1.S1.M6 | *(agregada)* `verificar_carriles.py`: nombre de skill de 3+ letras (`aso`, `cro`) | script reconoce skills de 3 letras sin falso positivo en «skill nombrada que no exista» | `verificar_carriles.py` → TODO OK | HECHO |
| H1.S1.M7 | *(agregada)* CU-68: dos criterios de backoffice en la bóveda y 2.ª prueba con nombre de criterio (`verificar_criterios.py` fallaba en CI de la PR) | bóveda y pruebas coinciden | `verificar_criterios.py` → «Sin divergencias» | HECHO |
| H1.S1.M8 | *(agregada)* `generar_compose.py` declara `SALUD_OBJETIVO_SLO` (default 99.5, el de `application.yml`) y se regenera `docker-compose.coolify.yml` (paso 6h de la CI fallaba) | el generador termina sin «variables que ningún valor cubre» y no deja diff | `generar_compose.py --coolify` + `verificar_limites_compose.py` + `git diff --exit-code` → exit 0 | HECHO |
| H1.S1.M9 | *(agregada)* `auditoria`: sin cifras SLO en el código (el 99,5 queda solo en `application.yml`) y `IndicadoresController` < 300 líneas con los mapeos en `MapeoDeTablero`/`MapeoDeSistemas` (paso 3 de la CI fallaba: `testBarrido`) | `testBarrido` sin hallazgos; mismo comportamiento | `./gradlew :servicios:auditoria:testBarrido` · `check -x test` · `test` · `webTest` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M10 | *(agregada)* `GruposBandejaWebTest.sinParametros` verifica el default del contrato (`limite` = 200) en vez de `null` (la CI del job Gradle fallaba en `grupos:webTest`) | las 114 pruebas web de `grupos` pasan; el job Gradle completo pasa en local | `./gradlew check -x test -x jacocoTestCoverageVerification -x jacocoTestReport --continue` · `testBarrido compileJava erroresCatalogo` · `spotlessCheck` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M12 | *(agregada)* `BandejaDeAdmisionesRepositorioTest`: el fixture manda `fecha_resolucion` como texto a una columna `timestamptz` (6 pruebas rojas en `14 · integrationTest`, que no se había ejecutado nunca con este código); se castea `?::timestamptz`, como el resto del repo | el INSERT del fixture resuelve el tipo; las 6 pruebas pasan en CI | reproducción del error y del cast en PostgreSQL local (`evidencia/bandeja-fecha-resolucion-psql.txt`) + `spotlessCheck compileTestJava` + CI `14 · integrationTest` | A MEDIAS |
| H1.S1.M11 | *(agregada y luego descartada)* Backoffice: lint verde y archivos bajo 200 líneas | — | — | DESCARTADO |

### H1.S2 — Verificar y entregar
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Correr todos los pasos del workflow en local | verificador en 0 FALLA; resto registrado | salida en `evidencia/` | HECHO |
| H1.S2.M2 | PR a `dev`, CI verde, merge | PR mergeada | `mergeable` + checks | TODO |
| H1.S2.M3 | Mismo cambio a `test` (rama desde `test`) + línea de backlog de #238 | `dev` y `test` iguales en árbol | `git diff origin/test origin/dev` vacío | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pasos hoy omitidos (carriles, derivados) fallen también | el workflow sigue rojo | registrar cada uno; arreglar solo lo documental |
| Concurrencia con otra sesión que escribe en `dev` | conflicto | volver a consultar el estado antes de abrir/mergear |

## Desvíos del plan
- **CI de la PR #262, `13 · test` (run 40888288799, head 64e1046d)**: cayó `AdmisionDelGrupoSustitucionTest.entranteHabilitadoDelegaYDevuelveElRegistro` con `TimeoutException: armar() timed out after 5 seconds` (límite de 5 s por método de `aportaya.base.gradle.kts`). Clasificación `ENVIRONMENT` (arranque en frío de Mockito en el primer método de la JVM, con otros módulos haciendo codegen en paralelo): la prueba no la toqué, es un mock puro, y en local con el mismo head `grupos:test` da 72 pruebas, 0 fallos (`evidencia/gradle-grupos-test-completo.txt`). Se repite el job fallido una sola vez (regla 80.4); no se sube ningún timeout.
- **H1.S1.M11 DESCARTADA** (decisión mía al fusionar `dev`, 2026-10-29): mientras la CI de esta PR corría, la otra sesión aterrizó en `dev` los mismos arreglos (`81c0e902` import sin uso en la bandeja de postulaciones, `3cbf7ced` `menu.ts` < 200 líneas, y recortes en `pantalla-de-admision.spec.ts`). Para no duplicar ni pisar su trabajo, en la fusión se tomó la versión de `dev` de `menu.ts` y del spec, y se retiraron mis archivos auxiliares (`menu.tipos.ts`, `pruebas/pantalla-de-admision.apoyo.ts`, `pantalla-de-admision.decision-directa.spec.ts`) y su evidencia. Mi versión había pasado lint, typecheck y `test:front` (183 archivos / 1285 pruebas) antes de retirarse.
- Se agregó H1.S1.M11 (histórico): sexto fallo latente (CI de la PR #262, job Frontend, paso f1). El lint se detenía en el primer error y escondía el barrido de tamaño. Las 4 pruebas de decisión directa (#235) se movieron a su propio spec con los textos idénticos; ninguna prueba se borró ni se debilitó (183 archivos / 1285 pruebas en verde).
- Se agregó H1.S1.M10: quinto fallo latente (CI de la PR #262, job Gradle, `grupos:webTest`). Clasificación `TEST_BUG` (regla 80.4) con reproducción local: el contrato `grupos.yaml` declara `limite` con `default: 200` (commit f8f5b3af, 2026-10-25) y el generador 7.14.0 lo aplica en la interfaz; el test escrito en el mismo commit esperaba `null`. El comportamiento de producción (tope 200) es el documentado; se corrige el test, sin tocar el contrato.
- Se agregó H1.S1.M9: cuarto fallo latente (CI de la PR #262, paso 3, `testBarrido`): `sin-umbral-literal` y `tamano-archivo` sobre código del SLO de servicios (#233). Cambio de código acotado a `servicios/auditoria`; sin cambio de contrato ni de comportamiento (82 pruebas web y 58 de módulo en verde).
- Se agregó H1.S1.M8: tercer fallo latente que mostró la CI de la PR #262 (paso 6h): la variable `SALUD_OBJETIVO_SLO` de `auditoria` (SLO de servicios, #233) no estaba en el generador del Compose.
- Se agregó H1.S1.M7: la CI de la PR #262 mostró que `verificar_criterios.py` fallaba en CU-68 (pruebas de #235 sin criterio en la bóveda); estaba latente porque ese job solo corre si el cambio lo activa. Cambio de código: solo el `@DisplayName` de una prueba (`CU68AceptarIngresoTest`), sin tocar aserciones.
- Se agregaron H1.S1.M5 y H1.S1.M6: el paso «Verificar los carriles» no corría mientras fallaba la bóveda y tenía su propia falla (13 skills sin carril; el script no veía nombres de 3 letras).
