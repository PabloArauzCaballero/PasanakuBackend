> **AVANCE: 17 / 23 — 73,9 %.** #315 y #316 las mergeó PabloArauzCaballero a las 02:33 UTC con dos rojos sin resolver: `14 · integrationTest` (una prueba intermitente de `grupos`: causa demostrada y corregida en este seguimiento, falta la CI) y los dos jobs de iOS (H2.S9: BLOQUEADO, no reproducible sin macOS). Frontend, Postman, OSV-Scanner, la imagen y Trivy pasaron en la CI de esas PR.

# Reporte — Dejar en verde los rojos que quedaron en `dev`

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/modest-bell-jz1x9e` (desde `origin/dev` ae38c444)
- Peldaño de evidencia alcanzado: **TESTED** en local para cada cambio (comandos y salidas en `evidencia/`) y **observado en la CI** para el job Frontend, Postman, OSV-Scanner, la imagen, Trivy y (en #316) los cinco corredores, medidos sobre la cabeza c6757463; el CSS de `ui` además tiene **prueba visual real** (capturas por viewport y tema, miradas en dos pasadas). No es `REGRESSION_VERIFIED`: la corrección de H2.S8 todavía no corrió en la CI y esta cabeza reinicia los checks.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `dart format` deja de cambiar archivos (3 de pruebas de la app móvil) | `dart format --output=none --set-exit-if-changed lib test` (Dart 3.12.2) | «620 files (0 changed)», exit 0 · `evidencia/dart-format.txt` |
| H1.S2.M1 | Reproducido el rojo de `f5` en local con los mismos 11 controles que la CI | `yarn workspace @aportaya/web exec playwright test pruebas/e2e/catalogo.spec.ts` | FAIL reproducido · `evidencia/f5-catalogo-44px.txt` |
| H1.S2.M2 | Migas, pestañas segmentadas, nota de vista y los botones de ayuda miden 44 px reales sin mover el diseño | `yarn workspace @aportaya/web test:e2e` | 36 passed; lint raíz 10/10; `ui` 196+9, backoffice 1321+94, web 74+8 · `evidencia/visual/` |
| H1.S3.M1–M3 | 14 pruebas e2e del backoffice atrasadas, actualizadas al comportamiento actual | `playwright test` de las 3 specs | admisiones+sistemas 28 passed; roles 10 passed · `evidencia/f6-e2e-backoffice.txt` |
| H2.S1.M1–M2 | `15 · contractTest` en verde | `./gradlew contractTest` | FAILED → BUILD SUCCESSFUL en todos los módulos |
| H2.S3.M1 | Hallazgos de Trivy listados | log del paso 17b2 | 4 HIGH con parche · `evidencia/trivy-y-locks.txt` |
| H2.S6.M1 | Postman regenerado: la CI de la PR falló en «Contratos y clientes generados» por la operación `listarAcuerdos` de #311/#313 | `python3 scripts/generar_postman.py` | 2 colecciones actualizadas, solo cambios aditivos |
| H2.S5.M1 | Criterios de CU-64 sin divergencias (rojo nuevo que llegó con el merge de `dev`) | `python3 scripts/verificar_criterios.py` | 6 FALLAS → «Sin divergencias…»; spotless/compile/barrido/contractTest OK |
| H2.S4.M1 | Criterios de CU-58 sin divergencias | `python3 scripts/verificar_criterios.py` | 5 FALLAS → «Sin divergencias entre la boveda y el codigo.» |
| H2.S2.M1 | `16 · sagaTest` y `16b` cobertura pasan | CI de #316 (run 40991387140, head 70f836b4), «Los cinco corredores» | success (en #315 el job cortó antes, en `14`: H2.S8) |
| H2.S3.M2 | Los 4 hallazgos de Trivy desaparecen (guava, snappy-java, libbrotli1) | CI de #315 y #316: «bootJar y docker build» y check «Trivy» | success en ambas, cabeza c6757463 |
| H2.S7.M1–M2 | OpenTelemetry 1.67.0 y los 28 paquetes npm sin avisos de OSV | CI de #315 y #316: «Dependencias vulnerables (OSV-Scanner) / osv-scan» | success en ambas, cabeza c6757463 |
| H2.S8.M1 | Causa de la prueba intermitente demostrada | SQL contra PostgreSQL local en transacción con `ROLLBACK` | 100 de 300 grupos de hoy quedan fuera del `LIMIT 200` · `evidencia/listado-grupos-backoffice.txt` |

## A medias
### H2.S8.M2 — `GruposDeBackofficeRepositorioTest.listado` intermitente (`14 · integrationTest`)
- Qué anda: la causa está demostrada (el listado ordena por `fecha_inicio DESC NULLS FIRST, codigo_publico, id` con `LIMIT 200`; la fixture pone `fecha_inicio = hoy` y un código aleatorio en una base compartida sin limpieza, así que con más de 200 grupos el de la prueba entra o no al azar). La prueba ahora fija la `fecha_inicio` de su grupo en el futuro antes de listar; las aserciones no cambiaron. `spotlessCheck` y `compileTestJava` de `grupos`: exit 0. Falló en #315 y pasó en #316 con el mismo código (clasificación `TEST_BUG`, regla 80.4).
- Qué no anda / no se probó: no corrí `integrationTest` (necesita Docker, que la sesión no tiene), así que la corrección no se observó en ejecución.
- Qué falta exactamente: que `14 · integrationTest` pase en la CI de las dos PR con esta cabeza.
- Dónde quedó: rama `claude/modest-bell-jz1x9e`; solo `servicios/grupos/src/test/.../GruposDeBackofficeRepositorioTest.java`, que compila.

### H3.S1.M1 / M2 — entrega de #315 (`dev`) y #316 (`test`)
- Qué anda: las dos PR se abrieron, quedaron sin conflictos y las mergeó PabloArauzCaballero a las 02:33 UTC. Con ese merge `dev` y `test` ganan el arreglo de `f1`, `f5`, `f6`, `contractTest`, los criterios de CU-58/CU-64, Postman, OSV y Trivy (todos observados en verde en la CI de las PR).
- Qué no anda: se mergearon con `mergeable_state: unstable`: `14 · integrationTest` (H2.S8) y los jobs de iOS (H2.S9) estaban en rojo. El CA «checks sin fallos» (regla 35.2) no se cumplió.
- Qué falta exactamente: H3.S2 (la corrección de H2.S8 en `dev` y `test`) y H2.S9.
- Dónde quedó: `dev` y `test` ya contienen las PR; la corrección de H2.S8 y las actualizaciones de plan y reporte están en la rama `claude/modest-bell-jz1x9e` (+ merge de `dev`), sin mergear.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S9.M1 | BLOQUEADO | Jobs de iOS: `ld: framework 'Pods_Runner' not found` al compilar con `patrol test` (Xcode 26.3, macos-15). Hace falta reproducirlo en un Mac con Xcode (`pod install` + `xcodebuild build-for-testing` de `apps/movil/ios`); lo destraba el dueño de la app móvil. En el run base de `dev` estaban `skipped`, no verdes |
| H3.S2.M1 / M2 | TODO | abrir el PR de seguimiento a `dev` y su gemelo a `test`, y que sus checks reales queden verdes antes de mergear |

## Evidencia
Ver `evidencia/`: `dart-format.txt`, `f5-catalogo-44px.txt`, `f6-e2e-backoffice.txt`, `trivy-y-locks.txt`, `java-y-boveda.txt`, `listado-grupos-backoffice.txt` y `visual/` (escritorio y teléfono, claro y oscuro, antes | después; más foco y hover de los botones de ayuda). Extracto del cambio de `f5`:
```text
ANTES : 11 controles < 44 px (a «Inicio», a «Sistemas», button «Notas de la vista», 5 «Ayuda sobre…», «Vista general», «Columnas 18», «Gobierno»)
DESPUES: yarn workspace @aportaya/web test:e2e → 36 passed (34.3s)
Solape del botón de ayuda (44 px) sobre el control de abajo: elementFromPoint → select / textarea (reciben su clic)
```

## No cubierto
- `f7` (se saltea sin backend vivo); el job Frontend completo terminó en success en la CI, pero no leí paso por paso su salida.
- `integrationTest`, `sagaTest`, el build de la imagen y Trivy no corren en la sesión (no hay Docker): solo los vi en la CI. La corrección de H2.S8 no tiene aún ninguna corrida.
- `GruposDeBackofficeRepositorioTest.listado` no es la única prueba que cuenta con una base compartida sin limpieza: no revisé las demás.
- `flutter analyze` y `flutter test` (no hay SDK de Flutter; solo se bajó `dart` para el formato). Las goldens y los simuladores iOS son de otros jobs.
- Estados de hover/foco del resto de componentes tocados (solo se capturaron los de la ayuda de la lista y de la barra de filtros); no se probó en un dispositivo táctil real.
- El aviso de presupuesto del bundle del backoffice ya existía (502,05 kB sin mis cambios, 502,20 kB con ellos); no lo toqué.

## Desvíos del plan
- Las PR #315 y #316 se mergearon antes de que llegara la corrección de H2.S8; por eso H2.S8 y las actualizaciones de plan y reporte viajan en un PR de seguimiento (H3.S2) y no en las PR originales.
Ver «Desvíos del plan» en [PLAN.md](./PLAN.md): se agregaron H1.S3, H2.S4 a H2.S8 (todos nacieron al ejecutar lo que la CI mostraba o escondía), y el README de `identidad` no declara un CU-07 «parcial».

## Riesgos residuales
- Los botones de ayuda ahora tienen 44×44 px con margen negativo: un clic en los ~10 px que rodean el icono abre el tooltip en vez de caer en un vecino que no sea el control de debajo (ese lo cubrí con `position: relative`). Otros vecinos interactivos pegados a una ayuda podrían quedar parcialmente tapados; no los busqué.
- Subir `guava`, `snappy-java` y OpenTelemetry en todos los servicios es un cambio de ejecución: sus pruebas de integración y de saga solo corren en la CI (en #316 pasaron; en #315 `14` cortó por H2.S8, ajeno a las versiones).
- La base de integración compartida sin limpieza hace que cualquier prueba que afirme pertenecer a un listado global con tope se vuelva intermitente a medida que se agregan pruebas.
- `CU07FotoDePerfil` seguirá chocando con el CU-07 de la bóveda hasta que su autor decida el nombre.

## Decisiones y ambigüedades
1. **CU-07 / `identidad`** — a confirmar con el autor del commit 509dad69: ¿se renombra la clase o se asigna otro CU a la foto de perfil? Mientras tanto el README dice qué es y avisa la discrepancia.
2. **Escenarios de CU-58 y CU-64** — a confirmar con quien hizo #287/#290 y #311/#313: redacté el escenario nuevo de CU-64 y los cinco de CU-58 solo con lo que ya verifica cada prueba; en CU-64 los cinco nombres de rechazo pasaron a empezar con «rechaza», como en `CU64Test`.
3. **44 px en el backoffice** — a confirmar con diseño: se cumplió el contrato del catálogo (44 px) en los componentes que el backoffice tenía compactos; si el backoffice quiere densidad de escritorio, lo correcto es una variante explícita (hoy no existe), no relajar la prueba.
4. **Pruebas e2e atrasadas** — los cambios de producto que las dejaron atrás eran intencionales (4f873212, 5d0f8d99, 3d4344e3); se actualizó la prueba, no el producto.
