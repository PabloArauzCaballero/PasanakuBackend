# Plan — Auditoría L5 `transversal-y-apoyo` (solo lectura)

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo `docs/trabajo/2026-11-20-auditoria-transversal-y-apoyo/`) · Predecesor: metaprompt `docs/trabajo/2026-11-20-metaprompt-auditoria/METAPROMPT_AUDITORIA_PASANAKU.md` (rama `claude/nice-fermat-essf9e`)
- Resultado observable: Pablo lee `AUDITORIA-transversal-y-apoyo.md` en la rama `auditoria/transversal-y-apoyo-2026-11-20` y encuentra cada defecto del código transversal y de los servicios de apoyo con ubicación y evidencia, más la distancia a R-NUEVA-1/2.
- Kill-test: `git status` muestra algo fuera de esta carpeta, o un hallazgo S1/S2 cuyo `ruta:línea` no dice lo que el informe afirma.

## Alcance
- IN: `plataforma/*`, `servicios/{auditoria,transparencia,notificaciones,tarifas,publicidad,inversiones,erp}`, `contratos/`, `sql/` como fuente del esquema; conexiones directas.
- OUT: modificar cualquier archivo fuera de esta carpeta; servicios de los lotes L1–L4 y frontends de L6 (solo puntos de contacto).
- Ambigüedades registradas: A-1 a A-4 del metaprompt (se registran, no se resuelven; a Pablo / Cumplimiento).

## H1 — Informe de auditoría L5 entregado
**CA:** Dado el commit auditado de `origin/test`, cuando una persona abre el informe, entonces encuentra las 12 secciones del metaprompt §9 con hallazgos citados por `ruta:línea`.
**DoD:** `git status --short` solo muestra esta carpeta · commit pusheado a `auditoria/transversal-y-apoyo-2026-11-20`.
**Estado:** EN CURSO (pendiente solo M9 hasta el push)

| ID | Microtarea (fase §8) | CA (binario) | DoD (verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fase 0 — Preparación | Commit de `origin/test` y `origin/dev` anotados, historial completo | `git rev-parse origin/test origin/dev; git rev-parse --is-shallow-repository` → `false` | HECHO — `07fc6971…`/`850e0f51…`, `false` tras `--unshallow` |
| H1.S1.M2 | Fase 1 — Huella y mapa de conexiones | Sección 3 del informe escrita | sección presente en el informe | HECHO — §3 |
| H1.S1.M3 | Fase 2 — Lectura con registro de cobertura | Anexo de cobertura con cada archivo de la huella | anexo presente | HECHO — §12, 1 282 filas |
| H1.S1.M4 | Fase 3 — Inventarios del flujo nuevo | Tablas 4a/4b/4c presentes | sección 4 presente | HECHO — 12 + 11 filas, A-1..A-4 |
| H1.S1.M5 | Fase 4 — Vista de conjunto | Hallazgos DUP/REESCRITURA/FRAGIL registrados | sección 5 | HECHO — 88 hallazgos |
| H1.S1.M6 | Fase 5 — Conexiones, contratos y verificadores | Matriz de contratos + salidas de verificadores en `evidencia/` | archivos en `evidencia/` | HECHO — §6 (130 operaciones), `evidencia/v-*`, `g-*`, `db-*` |
| H1.S1.M7 | Fase 6 — Estándares y normativa | Cada hallazgo con ISO 25010 y regla | grep de fichas | HECHO — 88/88 con línea de estándares |
| H1.S1.M8 | Fase 7 — Refutación | S1/S2 reabiertos y confirmados | registro en sección 11 | HECHO — §11 «Refutación» |
| H1.S1.M9 | Entrega | Commit y push en la rama | `git push` exit 0 | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Gradle/Docker/Node en la nube | Verificadores no corren | Probar una vez, pegar el error, marcar NO COMPROBADO |
| Huella de ~147k líneas de código + 132k de JSON de catálogo | Cobertura parcial | Subagentes de solo lectura en lotes disjuntos; JSON generado se inventaría, no se lee |

## Desvíos registrados
- Se levantó un PostgreSQL 16 **local y efímero** (`/tmp/pg-l5`, puerto 5433) para aplicar `sql/` y para que `generateJooq` permitiera correr las suites de Gradle. Se apagó y borró al terminar. No se tocó TEST ni producción.
- Las pruebas de integración no corrieron: no hay Docker (Testcontainers). No se intentó arreglar el entorno.
