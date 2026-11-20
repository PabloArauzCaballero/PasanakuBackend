# Plan — Auditoría de solo lectura del módulo garantia-entregas

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo `docs/trabajo/2026-11-20-auditoria-garantia-entregas/`) · Predecesor: metaprompt `docs/trabajo/2026-11-20-metaprompt-auditoria/` (rama `claude/nice-fermat-essf9e`)
- Resultado observable: Pablo lee `AUDITORIA-garantia-entregas.md` en la rama `auditoria/garantia-entregas-2026-11-20` y encuentra cada hallazgo del módulo con ubicación, evidencia y severidad, más los inventarios de aprobación y respaldo contra R-NUEVA-1/2.
- Kill-test: `git status` muestra archivos fuera de esta carpeta, o el informe tiene un hallazgo S1/S2 cuya `ruta:línea` no dice lo que el informe afirma.

## Alcance
- IN: `servicios/garantia`, `servicios/entregas` (main y test), `sql/10_tablas/08_garantia_incumplimiento` y sus claves/índices/reglas/semillas, pactos de contratos que los tocan, clientes generados, `packages/simulado`, pantallas de `apps/movil`, `apps/backoffice`, `apps/web` que los consumen; conexiones con otros servicios (solo el punto de contacto).
- OUT: modificar cualquier archivo fuera de la carpeta de este trabajo; auditar el interior de otros módulos; ejecutar migraciones, seeds o cualquier cosa contra TEST/producción.
- Ambigüedades registradas: A-1..A-4 del metaprompt (se registran, no se resuelven; a confirmar con Pablo / Cumplimiento).

## H1 — Informe de auditoría del módulo garantia-entregas
**CA:** Dado el commit de `origin/test`, cuando una persona abre el informe, entonces encuentra las 12 secciones de la §9 del metaprompt, cada hallazgo con ficha completa, y ningún dato personal ni secreto.
**DoD:** `grep -c '^### garantia-entregas-' AUDITORIA-garantia-entregas.md` coincide con la tabla del resumen · `git status --short` solo muestra esta carpeta · commit pusheado a `auditoria/garantia-entregas-2026-11-20`.
**Estado:** EN CURSO (falta M9)

### H1.S1 — Fases 0 a 7 del método
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fase 0: preparación (rama, commit, historial, lectura de reglas/ADR/auditorías previas) | commit y estado de historial anotados | `git rev-parse HEAD` + `git rev-parse --is-shallow-repository` pegados en evidencia | HECHO — `evidencia/00-preparacion.txt`, `evidencia/09-dod.txt` |
| H1.S1.M2 | Fase 1: huella y mapa de conexiones | sección 3 del informe escrita | `grep -n '^## 3' AUDITORIA-*.md` | HECHO — §3 en línea 83 (`evidencia/09-dod.txt`) |
| H1.S1.M3 | Fase 2: lectura completa con registro de cobertura | anexo con una fila por archivo | `grep -c '^| `' ` sobre el anexo ≥ nº de archivos de la huella | HECHO — anexo §12 con 275 filas de huella + 12.b (`grep -c` → 325 filas con ruta) |
| H1.S1.M4 | Fase 3: inventarios de aprobación y respaldo | tablas 4a/4b/4c presentes | `grep -n '^### 4' AUDITORIA-*.md` | HECHO — §4.a–4.d (`evidencia/09-dod.txt`) |
| H1.S1.M5 | Fase 4: vista de conjunto | hallazgos de DUP/REESCRITURA/FRAGIL registrados o "ninguno" con búsqueda | lectura de la sección 8 | HECHO — §8 con fila por clase; DUP-X/REESCRITURA/FRAGIL/PISADA registrados |
| H1.S1.M6 | Fase 5: conexiones, contratos y verificadores | matriz de contratos + salida de verificadores en `evidencia/` | `ls evidencia/` | HECHO — §6 + `evidencia/05*.txt` (Gradle BLOCKED por entorno: `05c`) |
| H1.S1.M7 | Fase 6: estándares y normativa | cada hallazgo con ISO 25010 y regla | `grep -c 'Estándares y normas' AUDITORIA-*.md` = nº hallazgos | HECHO — 68/68 (`evidencia/09-dod.txt`, re-ejecución) |
| H1.S1.M8 | Fase 7: refutación de S1/S2 | cada S1/S2 re-abierto y confirmado o bajado | sección de refutación en evidencia | HECHO — `evidencia/07-refutacion.md` |
| H1.S1.M9 | Entrega: REPORTE.md, commit y push | rama remota con el commit | `git log origin/auditoria/garantia-entregas-2026-11-20 -1` | EN CURSO — falta push y verificación del remoto |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle/Flutter/Node con dependencias en la nube | verificadores sin correr | se prueba una vez, se pega el error y se marca NO COMPROBADO |
| Volumen (~31 k líneas en la huella) | cobertura parcial | subagentes de solo lectura con lotes disjuntos; verificación de S1/S2 por el agente principal; cobertura declarada |

## Desvíos registrados
- Indicación de Pablo retransmitida por la sesión coordinadora (2026-11-20): catalogar cada hallazgo al identificarlo y hacer commit + push por tanda. Se aplicó (4 tandas).
- Los hallazgos se escribieron por tandas y se reordenaron por clase/severidad al armar el documento final.
