> **AVANCE: 10 / 14 — 71,4 %.** Todo lo corregido está verificado en local; **falta la CI real**: H2.S2.M1 (`sagaTest` y cobertura, que no corren sin Docker) y H2.S3.M2 (confirmar que Trivy ya no marca los 4 hallazgos) siguen abiertas, y las dos entregas por PR (H3) no están hechas.

# Reporte — Dejar en verde los rojos que quedaron en `dev`

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/modest-bell-jz1x9e` (desde `origin/dev` ae38c444)
- Peldaño de evidencia alcanzado: **TESTED** en local para cada cambio (comandos y salidas en `evidencia/`); el CSS de `ui` además tiene **prueba visual real** (capturas por viewport y tema, miradas en dos pasadas). No es `REGRESSION_VERIFIED`: la CI de la PR todavía no corrió, y `sagaTest`, la imagen y Trivy solo se ejercitan allí.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `dart format` deja de cambiar archivos (3 de pruebas de la app móvil) | `dart format --output=none --set-exit-if-changed lib test` (Dart 3.12.2) | «620 files (0 changed)», exit 0 · `evidencia/dart-format.txt` |
| H1.S2.M1 | Reproducido el rojo de `f5` en local con los mismos 11 controles que la CI | `yarn workspace @aportaya/web exec playwright test pruebas/e2e/catalogo.spec.ts` | FAIL reproducido · `evidencia/f5-catalogo-44px.txt` |
| H1.S2.M2 | Migas, pestañas segmentadas, nota de vista y los botones de ayuda miden 44 px reales sin mover el diseño | `yarn workspace @aportaya/web test:e2e` | 36 passed; lint raíz 10/10; `ui` 196+9, backoffice 1321+94, web 74+8 · `evidencia/visual/` |
| H1.S3.M1–M3 | 14 pruebas e2e del backoffice atrasadas, actualizadas al comportamiento actual | `playwright test` de las 3 specs | admisiones+sistemas 28 passed; roles 10 passed · `evidencia/f6-e2e-backoffice.txt` |
| H2.S1.M1–M2 | `15 · contractTest` en verde | `./gradlew contractTest` | FAILED → BUILD SUCCESSFUL en todos los módulos |
| H2.S3.M1 | Hallazgos de Trivy listados | log del paso 17b2 | 4 HIGH con parche · `evidencia/trivy-y-locks.txt` |
| H2.S4.M1 | Criterios de CU-58 sin divergencias | `python3 scripts/verificar_criterios.py` | 5 FALLAS → «Sin divergencias entre la boveda y el codigo.» |

## A medias
### H2.S3.M2 — Trivy
- Qué anda: `guava` 33.7.2-jre y `snappy-java` 1.1.10.9 en las restricciones de `aportaya.servicio.gradle.kts`; los 14 lockfiles cambian solo en esas dependencias y las transitivas de guava; `identidad` compila y pasan sus pruebas de unidad y web; `notificaciones` y `gateway` compilan. `libbrotli1` nombrado en `despliegue/Dockerfile`.
- Qué no anda / no se probó: no se construyó la imagen ni se corrió Trivy (no hay Docker en la sesión). No sé si la imagen resultante trae `libbrotli1 1.1.0-2ubuntu0.1`.
- Qué falta exactamente: que el paso `17b2` de la CI de la PR pase. Si `libbrotli1` sigue marcado, el digest base necesita otra solución (p. ej. `apt-get upgrade` o un digest nuevo).
- Dónde quedó: rama `claude/modest-bell-jz1x9e`; compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S2.M1 | TODO | CI: `16 · sagaTest` y `16b` cobertura corren recién cuando `15` pasa; si hay rojos debajo, entran como microtareas |
| H3.S1.M1 / M2 | TODO | abrir las dos PR y verificar `mergeable` y los checks reales |

## Evidencia
Ver `evidencia/`: `dart-format.txt`, `f5-catalogo-44px.txt`, `f6-e2e-backoffice.txt`, `trivy-y-locks.txt`, `java-y-boveda.txt` y `visual/` (escritorio y teléfono, claro y oscuro, antes | después; más foco y hover de los botones de ayuda). Extracto del cambio de `f5`:
```text
ANTES : 11 controles < 44 px (a «Inicio», a «Sistemas», button «Notas de la vista», 5 «Ayuda sobre…», «Vista general», «Columnas 18», «Gobierno»)
DESPUES: yarn workspace @aportaya/web test:e2e → 36 passed (34.3s)
Solape del botón de ayuda (44 px) sobre el control de abajo: elementFromPoint → select / textarea (reciben su clic)
```

## No cubierto
- La corrida **completa** de `f6` con las tres correcciones juntas (corrí cada spec por separado; la completa la hace la CI) y `f7` (se saltea sin backend vivo).
- `integrationTest`, `sagaTest`, el build de la imagen y Trivy: no hay Docker en la sesión.
- `flutter analyze` y `flutter test` (no hay SDK de Flutter; solo se bajó `dart` para el formato). Las goldens y los simuladores iOS son de otros jobs.
- Estados de hover/foco del resto de componentes tocados (solo se capturaron los de la ayuda de la lista y de la barra de filtros); no se probó en un dispositivo táctil real.
- El aviso de presupuesto del bundle del backoffice ya existía (502,05 kB sin mis cambios, 502,20 kB con ellos); no lo toqué.

## Desvíos del plan
Ver «Desvíos del plan» en [PLAN.md](./PLAN.md): se agregaron H1.S3 y H2.S4, y el README de `identidad` no declara un CU-07 «parcial».

## Riesgos residuales
- Los botones de ayuda ahora tienen 44×44 px con margen negativo: un clic en los ~10 px que rodean el icono abre el tooltip en vez de caer en un vecino que no sea el control de debajo (ese lo cubrí con `position: relative`). Otros vecinos interactivos pegados a una ayuda podrían quedar parcialmente tapados; no los busqué.
- Subir `guava` y `snappy-java` en todos los servicios es un cambio de ejecución: las pruebas de integración y de saga de los 15 módulos solo corren en la CI.
- `CU07FotoDePerfil` seguirá chocando con el CU-07 de la bóveda hasta que su autor decida el nombre.

## Decisiones y ambigüedades
1. **CU-07 / `identidad`** — a confirmar con el autor del commit 509dad69: ¿se renombra la clase o se asigna otro CU a la foto de perfil? Mientras tanto el README dice qué es y avisa la discrepancia.
2. **Escenarios de CU-58** — a confirmar con quien hizo #287/#290: redacté los cinco escenarios solo con lo que ya verifica cada prueba.
3. **44 px en el backoffice** — a confirmar con diseño: se cumplió el contrato del catálogo (44 px) en los componentes que el backoffice tenía compactos; si el backoffice quiere densidad de escritorio, lo correcto es una variante explícita (hoy no existe), no relajar la prueba.
4. **Pruebas e2e atrasadas** — los cambios de producto que las dejaron atrás eran intencionales (4f873212, 5d0f8d99, 3d4344e3); se actualizó la prueba, no el producto.
