# Plan — Auditoría de solo lectura del módulo grupos-organizador (lote L3)

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo esta carpeta) · Predecesor: metaprompt `docs/trabajo/2026-11-20-metaprompt-auditoria/METAPROMPT_AUDITORIA_PASANAKU.md` (rama `claude/nice-fermat-essf9e`)
- Resultado observable: Pablo (dueño del producto) lee `AUDITORIA-grupos-organizador.md` en la rama `auditoria/grupos-organizador-2026-11-20` y encuentra, con ruta y línea, todo lo que está mal en `servicios/grupos`, `servicios/organizador` (alta, admisión, inicio, sorteo, postulaciones) y sus conexiones, más la distancia a R-NUEVA-1 y R-NUEVA-2.
- Kill-test: un hallazgo S1/S2 cuyo `ruta:línea` no dice lo que el informe afirma; o `git status` mostrando archivos fuera de esta carpeta.

## Alcance
- IN: `servicios/grupos`, `servicios/organizador` (main + test), `sql/10_tablas/02_grupos_turnos`, `sql/10_tablas/07_organizador_automatizacion` y sus claves/índices/reglas/semillas, pactos donde grupos produce o consume, `clientes/dart/grupos`, pantallas de grupos/admisión/inicio/sorteo/postulaciones/organizador en `apps/movil`, `apps/backoffice`, `apps/web`; conexiones directas.
- OUT: modificar cualquier archivo fuera de `docs/trabajo/2026-11-20-auditoria-grupos-organizador/`; auditar el interior de aportes, garantía, entregas, identidad, núcleo financiero (L1, L2, L4); ejecutar migraciones, seeds o algo contra TEST/producción.
- Ambigüedades registradas: A-1 a A-4 del metaprompt, sin resolver; se escalan a Pablo (y a Cumplimiento para lo regulatorio).

## H1 — Informe de auditoría L3 entregado en su rama
**CA:** Dado el commit de `origin/test`, cuando Pablo abre el informe, entonces cada hallazgo tiene ficha completa con ubicación y evidencia, y las 12 secciones del metaprompt existen.
**DoD:** `git status --short` muestra solo esta carpeta · commit pusheado a `auditoria/grupos-organizador-2026-11-20` · REPORTE.md con avance en la primera línea.
**Estado:** HECHO

### H1.S1 — Fases del método (sección 8 del metaprompt)
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fase 0 — preparación: rama, commit, historial, lectura de reglas y auditorías previas | Commit y estado del historial anotados en la Ficha | `git rev-parse HEAD; git rev-parse --is-shallow-repository` → hash + `false` | HECHO — `07fc6971…` test; dev `850e0f51` sin diferencias en el módulo; `--unshallow` aplicado → `false` |
| H1.S1.M2 | Fase 1 — huella del módulo y mapa de conexiones | Sección 3 del informe con huella y conexiones | `grep -c '^| ' AUDITORIA-*.md` sección 3 > 0 | HECHO — sección 3 con huella (10 capas) y mapa de 23 conexiones |
| H1.S1.M3 | Fase 2 — lectura completa con registro de cobertura | Anexo 12 con una fila por archivo de la huella | conteo de filas del anexo vs `find` de la huella | HECHO — anexo con 521 filas (472 en alcance); 84 clases de prueba de `grupos` declaradas no leídas línea a línea, con motivo |
| H1.S1.M4 | Fase 3 — inventarios de aprobaciones y respaldo | Secciones 4a/4b/4c llenas o "ninguno encontrado" con búsqueda | `grep -n 'APR-\|RSP-' AUDITORIA-*.md` | HECHO — APR-01..APR-06, RSP-01..RSP-06, tabla A-1..A-4 |
| H1.S1.M5 | Fase 4 — vista de conjunto | Hallazgos DUP/REESCRITURA/FRAGIL evaluados | sección 8 con filas de esas clases | HECHO — DUP-INT-001, DUP-X-001, REESCRITURA-001, FRAGIL-001..004 y fila PISADA en sección 8 |
| H1.S1.M6 | Fase 5 — conexiones, contratos y verificadores | Matriz de contratos y salida de verificadores en `evidencia/` | `ls evidencia/` con salidas | HECHO — matriz en sección 6; 16 salidas en `evidencia/`. Gradle (429) y yarn (1.22 vs 4.18) no corrieron por ENTORNO: declarado NO COMPROBADO en «Límites» |
| H1.S1.M7 | Fase 6 — estándares y normativa | Cada hallazgo con ISO 25010 y regla | `grep -c 'Estándares y normas' AUDITORIA-*.md` = nº hallazgos | HECHO — 55 fichas, 55 líneas «Estándares y normas» (`grep -c`) |
| H1.S1.M8 | Fase 7 — refutación de S1/S2 | Cada S1/S2 re-abierto y confirmado o bajado | registro de refutación en el informe | HECHO — tabla de refutación en sección 11; CONTRATO-001 bajado de S1 a S2, SEG-001 a «probable» |
| H1.S1.M9 | Entrega — commit y push solo de esta carpeta | Rama remota con el commit | `git push -u origin auditoria/grupos-organizador-2026-11-20` exit 0 | HECHO — `git push` exit 0 a `auditoria/grupos-organizador-2026-11-20`; `git diff --name-only origin/test..HEAD` fuera de la carpeta = 0 |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle/Flutter/Node con dependencias en la nube | Verificadores no corren | Se prueba una vez, se pega el error y se marca NO COMPROBADO |
| Volumen (≈44k líneas Java + 15k front + SQL) | Cobertura parcial | Subagentes de solo lectura en lotes disjuntos; cobertura declarada honestamente |
