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
**Estado:** EN CURSO

### H1.S1 — Fases del método (sección 8 del metaprompt)
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fase 0 — preparación: rama, commit, historial, lectura de reglas y auditorías previas | Commit y estado del historial anotados en la Ficha | `git rev-parse HEAD; git rev-parse --is-shallow-repository` → hash + `false` | HECHO — `07fc6971…` test; dev `850e0f51` sin diferencias en el módulo; `--unshallow` aplicado → `false` |
| H1.S1.M2 | Fase 1 — huella del módulo y mapa de conexiones | Sección 3 del informe con huella y conexiones | `grep -c '^| ' AUDITORIA-*.md` sección 3 > 0 | TODO |
| H1.S1.M3 | Fase 2 — lectura completa con registro de cobertura | Anexo 12 con una fila por archivo de la huella | conteo de filas del anexo vs `find` de la huella | TODO |
| H1.S1.M4 | Fase 3 — inventarios de aprobaciones y respaldo | Secciones 4a/4b/4c llenas o "ninguno encontrado" con búsqueda | `grep -n 'APR-\|RSP-' AUDITORIA-*.md` | TODO |
| H1.S1.M5 | Fase 4 — vista de conjunto | Hallazgos DUP/REESCRITURA/FRAGIL evaluados | sección 8 con filas de esas clases | TODO |
| H1.S1.M6 | Fase 5 — conexiones, contratos y verificadores | Matriz de contratos y salida de verificadores en `evidencia/` | `ls evidencia/` con salidas | A MEDIAS — qué anda: verificadores Python corridos (`evidencia/`); qué no anda: Gradle (429 de Maven Central) y yarn (1.22 vs 4.18) no corren por ENTORNO, no por dependencia de otro; qué falta: matriz de contratos en el informe |
| H1.S1.M7 | Fase 6 — estándares y normativa | Cada hallazgo con ISO 25010 y regla | `grep -c 'Estándares y normas' AUDITORIA-*.md` = nº hallazgos | TODO |
| H1.S1.M8 | Fase 7 — refutación de S1/S2 | Cada S1/S2 re-abierto y confirmado o bajado | registro de refutación en el informe | TODO |
| H1.S1.M9 | Entrega — commit y push solo de esta carpeta | Rama remota con el commit | `git push -u origin auditoria/grupos-organizador-2026-11-20` exit 0 | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle/Flutter/Node con dependencias en la nube | Verificadores no corren | Se prueba una vez, se pega el error y se marca NO COMPROBADO |
| Volumen (≈44k líneas Java + 15k front + SQL) | Cobertura parcial | Subagentes de solo lectura en lotes disjuntos; cobertura declarada honestamente |
