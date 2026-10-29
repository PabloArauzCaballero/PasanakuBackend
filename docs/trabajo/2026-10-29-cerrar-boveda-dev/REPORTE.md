> **AVANCE: 8 / 10 — 80,0 %.** Los 7 cambios y la verificación local están hechos; falta que la CI de las PR quede verde, el merge a `dev` y el mismo cambio en `test` (H1.S2.M2, H1.S2.M3).

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
- La CI de GitHub (todavía no corrió sobre este cambio).
- El job de PostgreSQL real no se re-ejecutó en local; ya pasaba en `dev` (run 40875015485) y este cambio no toca SQL.
- `Motivo` y `Cómo se verifica` de ADR-053 citan los reportes del rediseño; no se volvió a ejecutar `flutter test` ni `dart analyze`.
- La captura antes/después de web y backoffice del rediseño (H2.S5.M1, H8.S3.M2) sigue sin re-verificarse; el ADR lo declara como casilla abierta.

## Desvíos del plan
Agregadas H1.S1.M5 y H1.S1.M6 (ver PLAN).

## Riesgos residuales
- `verificar_carriles.py` ahora acepta nombres de 3 letras en toda la matriz de `planes/19`; si alguien nombra con comillas invertidas una palabra de 3 letras que no es skill (`sql`), el chequeo «skill nombrada que no exista» la marcará. En este momento no marca ninguna.
- Avisos preexistentes sin tocar: tramos T5 y T7 con carga 4x; servicio `inversiones` sin ficha de carril.

## Decisiones y ambigüedades
1. **Asignación de las 13 skills a `F1-M`** (supuesto). Base: el commit 579ddeac y el README dicen que se instalaron para el rediseño bancario (ADR-053), que es trabajo de `F1-M`. Confirmar con el dueño del repositorio (@PabloArauzCaballero) si alguna corresponde a otro carril (p. ej. `aso`, `referrals` al sitio público `F9`).
2. **Procedencia de las 10 skills de marketing**: sin SHA registrado en el commit de instalación; se declara «SHA no registrado» en el README. Confirmar con quien las instaló.
3. **Redacción posterior de `Motivo` y `Cómo se verifica`** de ADR-053: solo con lo que el ADR y los reportes ya decían; no cambia la decisión. Confirmar con quien aceptó el ADR.
