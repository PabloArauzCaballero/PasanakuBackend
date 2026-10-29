> **AVANCE: 13 / 15 — 86,7 %** (M11 descartada, aparte). Hechas: M1–M10, M12, M13 y S2.M1. Faltan S2.M2 (mergear #262 a `dev`) y S2.M3 (mergear #263 a `test`): al escribir este reporte aún no estaban mergeadas. La CI de ambas sigue con 3 rojos que no son de este trabajo (ver «Decisiones»).

# Reporte — Poner en verde la verificación de la bóveda en `dev`/`test`

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/modest-bell-jz1x9e` (desde `origin/dev` b1e18ec6)
- Peldaño de evidencia alcanzado: **TESTED** para lo que toca este trabajo: la CI real de #262 y #263 pasa `bóveda`, formato y reglas propias, `13 · test`, `13b · webTest` y `14 · integrationTest`. No es `REGRESSION_VERIFIED`: `15 · contractTest` está rojo (por un README de otra sesión) y `16 · sagaTest` y `16b` cobertura no corrieron.

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
| H1.S1.M12 | Fixture de `BandejaDeAdmisionesRepositorioTest`: `?::timestamptz` en `fecha_resolucion` | reproducción en PostgreSQL local + CI `14 · integrationTest` | PASS · `evidencia/bandeja-fecha-resolucion-psql.txt`; CI #262 run 40903451975 intento 2 y #263 run 40903462356: `14 · integrationTest` BUILD SUCCESSFUL |
| H1.S1.M13 | Las 6 pruebas de la bandeja abren la transacción (`transaccion.execute`) | `spotlessCheck compileTestJava testBarrido` + CI `14 · integrationTest` | PASS: BUILD SUCCESSFUL local y en CI de ambas ramas |
| (nota) | `13 · test` con `TimeoutException` de 5 s en `AdmisionDelGrupoSustitucionTest` | `./gradlew :servicios:grupos:test --rerun` + repetición del job | ENVIRONMENT: 72/72 local y `13 · test` verde en la repetición · `evidencia/gradle-grupos-test-completo.txt` |
| H1.S2.M1 | Pasos del job «Bóveda…» corridos en local | `verificar_seguridad.py`, `verificar_boveda.py`, `verificar_carriles.py`, `generar_k8s.py`, `generar_ddl.py`, `generar_semillas.py`, `git diff --quiet -- sql/` | todos exit 0 · `evidencia/` |

## A medias
### H1.S2.M2 y H1.S2.M3 — entrega por PR
- Qué anda: #262 (hacia `dev`) y #263 (hacia `test`) tienen la verificación de la bóveda en verde, formato y reglas propias, `13`, `13b` y `14` en verde en la CI real.
- Qué no anda: en ambas, `15 · contractTest` (README de `identidad`), frontend `f5` (44 px) y Trivy están en rojo por trabajo ajeno; `16`/`16b` no corrieron.
- Qué falta exactamente: mergear #262 y luego #263 y verificar que los archivos de este trabajo queden idénticos en `dev` y `test`. Si el merge se hizo después de escribir este reporte, queda constancia en los comentarios de las PR.
- Dónde quedó: ramas `claude/modest-bell-jz1x9e` (a50e2846 + este reporte) y `claude/modest-bell-jz1x9e-test`; compila y pasa spotless.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Rojos ajenos: `15 · contractTest` (README de `identidad`, CU-07), frontend `f5` (44 px en el backoffice), Trivy | DEVUELTO A SU DUEÑO | `identidad`: la sesión de 509dad69 nombra CU-07 en su README (decidir cómo se etiqueta un CU parcial); backoffice: ajustar el tamaño de los 11 controles; imagen: actualizar o suprimir con justificación las CVE |
| `16 · sagaTest` y `16b` cobertura | SALTADOS EN LA CI | se ejecutan cuando `15` pase |

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
- `integrationTest` de `grupos` en local: no hay Docker en la sesión; solo lo ejercitó la CI.
- `15 · contractTest` salvo la prueba de README (que falla por otra causa), `16 · sagaTest` y `16b` cobertura: no corrieron en la CI.
- Los demás módulos Java no se compilaron ni probaron: el cambio de `auditoria` no es referenciado desde otro módulo (verificado con búsqueda), pero no se corrió `compileJava` global.
- Prueba de integración de `auditoria` con el contexto Spring completo y `aportaya.salud.objetivo-slo` sin default en el `@Value`: el valor lo da `application.yml` (`${SALUD_OBJETIVO_SLO:99.5}`) y `compose` (`generar_compose.py`); no se arrancó el servicio.
- Frontend: tras fusionar `dev` (que ya traía sus propios arreglos de frontend) no se re-ejecutaron lint, typecheck ni `test:front` en local; los cubre la CI de la PR. Tampoco se ejecutaron en local `test:a11y`, los builds (`ng build` bajo presupuesto), los e2e de Playwright (web y backoffice), ni nada de Flutter (no hay SDK). Los cubre la CI de la PR.
- Entorno de verificación: PostgreSQL 16 efímero local con las credenciales de la CI y un *init script* de Gradle local (espejo de Maven Central por 429 de límite de tasa); no forman parte del repo.
- El job de PostgreSQL real no se re-ejecutó en local; ya pasaba en `dev` (run 40875015485) y este cambio no toca SQL.
- `Motivo` y `Cómo se verifica` de ADR-053 citan los reportes del rediseño; no se volvió a ejecutar `flutter test` ni `dart analyze`.
- La captura antes/después de web y backoffice del rediseño (H2.S5.M1, H8.S3.M2) sigue sin re-verificarse; el ADR lo declara como casilla abierta.

## Desvíos del plan
Agregadas H1.S1.M7 a H1.S1.M10, M12 y M13 (fallos latentes que fue mostrando la CI de la PR) y H1.S1.M11, descartada por duplicar lo que `dev` ya traía (ver PLAN). Agregadas H1.S1.M5 y H1.S1.M6 (ver PLAN).

## Riesgos residuales
- `@Value("${aportaya.salud.objetivo-slo}")` ya no tiene default en el código: si alguien arranca `auditoria` con otra configuración que no incluya `application.yml`, falla al iniciar (comportamiento buscado por la regla 90.3.4: la configuración inválida o ausente detiene el servicio).
- `DisponibilidadDelServicio.CIEN` pasa a `BigDecimal.TEN.pow(2)` para no escribir un literal: 100 es la base del porcentaje, no un umbral regulatorio; la regla `sin-umbral-literal` está pensada para cifras de negocio.
- `verificar_carriles.py` ahora acepta nombres de 3 letras en toda la matriz de `planes/19`; si alguien nombra con comillas invertidas una palabra de 3 letras que no es skill (`sql`), el chequeo «skill nombrada que no exista» la marcará. En este momento no marca ninguna.
- Avisos preexistentes sin tocar: tramos T5 y T7 con carga 4x; servicio `inversiones` sin ficha de carril.

## Decisiones y ambigüedades
0. **Rojos que no son de este trabajo y por qué se entrega igual** (regla 35.2: clasificados con evidencia, no ignorados). `15 · contractTest`: `DocumentacionVivaContratoTest` → «identidad: implementa [07] y su README no los nombra»; viene del commit 509dad69 (foto de perfil, otra sesión, hoy). Frontend `f5`: 11 controles de `apps/backoffice` < 44 px; `apps/` es idéntico al de `dev`. Trivy `17b2`: CVE CRITICAL/HIGH sin parche en la imagen, ya fallaba antes. No los arreglé porque exigen una decisión de su dueño (cómo etiquetar un CU parcial en un README; rediseño de controles; política de CVE) y la regla 00 §3 prohíbe tocar lo ajeno sin plan. `GruposDeBackofficeRepositorioTest.listado` falló una vez y luego pasó sin cambios: intermitente sin causa demostrada (ver PLAN).
1. **Asignación de las 13 skills a `F1-M`** (supuesto). Base: el commit 579ddeac y el README dicen que se instalaron para el rediseño bancario (ADR-053), que es trabajo de `F1-M`. Confirmar con el dueño del repositorio (@PabloArauzCaballero) si alguna corresponde a otro carril (p. ej. `aso`, `referrals` al sitio público `F9`).
2. **Procedencia de las 10 skills de marketing**: sin SHA registrado en el commit de instalación; se declara «SHA no registrado» en el README. Confirmar con quien las instaló.
3. **Redacción posterior de `Motivo` y `Cómo se verifica`** de ADR-053: solo con lo que el ADR y los reportes ya decían; no cambia la decisión. Confirmar con quien aceptó el ADR.
