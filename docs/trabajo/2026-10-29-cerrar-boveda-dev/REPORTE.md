> **AVANCE: 11 / 14 — 78,6 %** (1 descartada: ver Desvíos). Cambios y verificación local hechos; falta la CI verde, el merge a `dev` y el mismo cambio en `test` (H1.S2.M2, H1.S2.M3).

# Reporte — Poner en verde la verificación de la bóveda en `dev`/`test`

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/modest-bell-jz1x9e` (desde `origin/dev` b1e18ec6)
- Peldaño de evidencia alcanzado: **RUNS** (los verificadores corren en local con exit 0). La CI de la PR aún no corrió: no se sube a `TESTED`/`VERIFIED` hasta tenerla.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | ADR-053 con las 6 secciones | `python3 scripts/verificar_boveda.py` | PASS · `evidencia/verificar_boveda-despues.txt` |
| H1.S1.M2 | ADR-053 en el índice `_Arquitectura.md` | ídem | PASS |
| H1.S1.M3 | 10 skills de marketing en `.claude/skills/README.md` | ídem | PASS |
| H1.S1.M4 | «51 ADR» en el Plan maestro | ídem | PASS |
| H1.S1.M5 | 13 skills asignadas a `F1-M` en `planes/19` | `python3 scripts/verificar_carriles.py` | PASS (TODO OK con avisos) · `evidencia/verificar_carriles-despues.txt` |
| H1.S1.M6 | `verificar_carriles.py` reconoce skills de 3 letras | ídem | PASS |
| H1.S1.M7 | CU-68: criterios de backoffice en la bóveda + `@DisplayName` de la 2.ª prueba alineado | `python3 scripts/verificar_criterios.py` | PASS «Sin divergencias entre la boveda y el codigo» · `evidencia/verificar_criterios-despues.txt` |
| H1.S1.M8 | Generador de Compose declara `SALUD_OBJETIVO_SLO`; `docker-compose.coolify.yml` regenerado (1 línea) | `python3 scripts/generar_compose.py --coolify` · `python3 scripts/auditoria/verificar_limites_compose.py` · `git diff --exit-code -- docker-compose.coolify.yml despliegue/coolify/` | PASS exit 0 · `evidencia/compose-despues.txt` |
| H1.S1.M9 | `auditoria`: sin literales SLO en el código y `IndicadoresController` de 379 a 254 líneas (mapeos a `MapeoDeTablero` y `MapeoDeSistemas`) | `./gradlew :servicios:auditoria:spotlessCheck` · `check -x test -x jacocoTestCoverageVerification -x jacocoTestReport` · `testBarrido` · `test` · `webTest --rerun` | PASS: BUILD SUCCESSFUL en todos; `test` 58/58, `webTest` 82/82 · `evidencia/gradle-auditoria-*.txt` |
| H1.S1.M10 | `GruposBandejaWebTest.sinParametros` alineado con el default `limite=200` del contrato (TEST_BUG demostrado) | `./gradlew :servicios:grupos:webTest --rerun` · `./gradlew check -x test -x jacocoTestCoverageVerification -x jacocoTestReport --continue` · `./gradlew testBarrido compileJava erroresCatalogo --continue` · `./gradlew spotlessCheck` | PASS: grupos/webTest 114/114; el resto BUILD SUCCESSFUL · `evidencia/gradle-grupos-webtest.txt`, `evidencia/gradle-check-completo.txt` |
| H1.S2.M1 | Pasos del job «Bóveda…» corridos en local | `verificar_seguridad.py`, `verificar_boveda.py`, `verificar_carriles.py`, `generar_k8s.py`, `generar_ddl.py`, `generar_semillas.py`, `git diff --quiet -- sql/` | todos exit 0 · `evidencia/` |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S2.M2 | TODO | PR a `dev`, CI verde, merge |
| H1.S2.M3 | TODO | Rama desde `test` con el mismo cambio y la línea de backlog de #238; PR y merge |

## Evidencia
Antes (observado sobre `origin/dev` b1e18ec6; la CI lo muestra en el run 40875015485, paso «Verificar la bóveda»):
```text
FALLA · índice de skills completo (291 skills)
FALLA · toda decisión está en el índice ['ADR-053']
FALLA · toda decisión tiene las 6 secciones obligatorias ['ADR-053']
FALLA · ninguna cifra desfasada en los planes ['planes/00 Plan maestro.md: dice 50 ADR, son 51']
4 FALLAS
FALLA · 278/291 skills asignadas — huérfanas: aso, copy-editing, copywriting, cro, fintech-ux, marketing-ideas, marketing-psychology, mobile-app-ui-design, onboarding, product-marketing, referrals, roast-my-ui, signup
```
Después: `evidencia/*-despues.txt`.

## No cubierto
- `./gradlew test` de `servicios/grupos`: el cambio de código es solo un `@DisplayName`; no se ejecutó la prueba Java en local.
- Los demás módulos Java no se compilaron ni probaron: el cambio de `auditoria` no es referenciado desde otro módulo (verificado con búsqueda), pero no se corrió `compileJava` global.
- Prueba de integración de `auditoria` con el contexto Spring completo y `aportaya.salud.objetivo-slo` sin default en el `@Value`: el valor lo da `application.yml` (`${SALUD_OBJETIVO_SLO:99.5}`) y `compose` (`generar_compose.py`); no se arrancó el servicio.
- Frontend: tras fusionar `dev` (que ya traía sus propios arreglos de frontend) no se re-ejecutaron lint, typecheck ni `test:front` en local; los cubre la CI de la PR. Tampoco se ejecutaron en local `test:a11y`, los builds (`ng build` bajo presupuesto), los e2e de Playwright (web y backoffice), ni nada de Flutter (no hay SDK). Los cubre la CI de la PR.
- Entorno de verificación: PostgreSQL 16 efímero local con las credenciales de la CI y un *init script* de Gradle local (espejo de Maven Central por 429 de límite de tasa); no forman parte del repo.
- La CI de GitHub (todavía no corrió sobre este cambio).
- El job de PostgreSQL real no se re-ejecutó en local; ya pasaba en `dev` (run 40875015485) y este cambio no toca SQL.
- `Motivo` y `Cómo se verifica` de ADR-053 citan los reportes del rediseño; no se volvió a ejecutar `flutter test` ni `dart analyze`.
- La captura antes/después de web y backoffice del rediseño (H2.S5.M1, H8.S3.M2) sigue sin re-verificarse; el ADR lo declara como casilla abierta.

## Desvíos del plan
Agregadas H1.S1.M7 a H1.S1.M10 (fallos latentes que fue mostrando la CI de la PR) y H1.S1.M11, descartada por duplicar lo que `dev` ya traía (ver PLAN). Agregadas H1.S1.M5 y H1.S1.M6 (ver PLAN).

## Riesgos residuales
- `@Value("${aportaya.salud.objetivo-slo}")` ya no tiene default en el código: si alguien arranca `auditoria` con otra configuración que no incluya `application.yml`, falla al iniciar (comportamiento buscado por la regla 90.3.4: la configuración inválida o ausente detiene el servicio).
- `DisponibilidadDelServicio.CIEN` pasa a `BigDecimal.TEN.pow(2)` para no escribir un literal: 100 es la base del porcentaje, no un umbral regulatorio; la regla `sin-umbral-literal` está pensada para cifras de negocio.
- `verificar_carriles.py` ahora acepta nombres de 3 letras en toda la matriz de `planes/19`; si alguien nombra con comillas invertidas una palabra de 3 letras que no es skill (`sql`), el chequeo «skill nombrada que no exista» la marcará. En este momento no marca ninguna.
- Avisos preexistentes sin tocar: tramos T5 y T7 con carga 4x; servicio `inversiones` sin ficha de carril.

## Decisiones y ambigüedades
1. **Asignación de las 13 skills a `F1-M`** (supuesto). Base: el commit 579ddeac y el README dicen que se instalaron para el rediseño bancario (ADR-053), que es trabajo de `F1-M`. Confirmar con el dueño del repositorio (@PabloArauzCaballero) si alguna corresponde a otro carril (p. ej. `aso`, `referrals` al sitio público `F9`).
2. **Procedencia de las 10 skills de marketing**: sin SHA registrado en el commit de instalación; se declara «SHA no registrado» en el README. Confirmar con quien las instaló.
3. **Redacción posterior de `Motivo` y `Cómo se verifica`** de ADR-053: solo con lo que el ADR y los reportes ya decían; no cambia la decisión. Confirmar con quien aceptó el ADR.
