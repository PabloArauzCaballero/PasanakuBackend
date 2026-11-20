# Plan — Auditoría de solo lectura: aportes + núcleo financiero (lote L1)

- Fecha: 2026-11-20 · Repos afectados: PasanakuBackend (solo esta carpeta) · Predecesor: metaprompt `docs/trabajo/2026-11-20-metaprompt-auditoria/METAPROMPT_AUDITORIA_PASANAKU.md` (rama `claude/nice-fermat-essf9e`)
- Rama: `auditoria/aportes-nucleo-financiero-2026-11-20` desde `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`
- Resultado observable: Pablo abre `AUDITORIA-aportes-nucleo-financiero.md` y encuentra cada defecto del módulo con ubicación, evidencia y severidad, y los inventarios de aprobaciones y respaldo contra R-NUEVA-1/2.
- Kill-test: un hallazgo S1/S2 cuya `ruta:línea` no dice lo que el informe afirma, o una clase de la sección 6 sin fila de cobertura.

## Alcance
- IN: `servicios/aportes`, `servicios/nucleo-financiero` (main + test), tablas de `sql/10_tablas/03_aportes_pagos_qr`, `sql/10_tablas/10_billetera_custodia` y las que el módulo usa, sus claves/índices/append-only/reglas/semillas, pactos de `contratos/pactos/` donde participa, clientes generados, `packages/simulado`, pantallas de cobro/billetera de `apps/movil`, `apps/backoffice`, `apps/web`. Conexiones directas.
- OUT: cualquier modificación de código, SQL, contratos, pruebas o documentación fuera de esta carpeta. Interior de otros módulos (solo puntos de contacto).
- Ambigüedades registradas: A-1..A-4 del metaprompt (se registran, no se resuelven; responsable Pablo / Cumplimiento).

## H1 — Informe de auditoría L1 entregado en la rama
**CA:** Dado el commit auditado, cuando se abre el informe, entonces tiene las 12 secciones de la §9 del metaprompt, sin datos personales ni secretos, y cada S1/S2 resistió la refutación.
**DoD:** `git status --short` muestra solo esta carpeta · commit y push a `auditoria/aportes-nucleo-financiero-2026-11-20`.
**Estado:** EN CURSO (falta solo la entrega H1.S1.M9)

### H1.S1 — Fases del método (§8)
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fase 0 preparación | Commit registrado, historial completo | `git rev-parse HEAD; git rev-parse --is-shallow-repository` → `false` | HECHO |
| H1.S1.M2 | Fase 1 huella y mapa | Sección 3 escrita | `grep -c '^## 3' AUDITORIA-*.md` → 1 | HECHO |
| H1.S1.M3 | Fase 2 lectura con cobertura | Anexo 12 con una fila por archivo | `grep -c '^| `servicios' AUDITORIA-*.md` > 0 | HECHO |
| H1.S1.M4 | Fase 3 inventarios | Secciones 4a/4b/4c escritas | `grep -c 'APROB-\|RESP-' AUDITORIA-*.md` > 0 | HECHO |
| H1.S1.M5 | Fase 4 vista de conjunto | Hallazgos DUP/REESCRITURA/PISADA evaluados | fila en «Cobertura por clase» | HECHO |
| H1.S1.M6 | Fase 5 conexiones, contratos, verificadores | Matriz de contratos + salidas de verificadores | sección 6 y 7 con salida pegada o NO COMPROBADO | HECHO |
| H1.S1.M7 | Fase 6 estándares | Cada hallazgo con ISO 25010 | `grep -c 'ISO/IEC 25010'` = nº hallazgos | HECHO |
| H1.S1.M8 | Fase 7 refutación | S1/S2 re-abiertos y confirmados | sección «Refutación» en el informe | HECHO |
| H1.S1.M9 | Entrega | Commit + push | `git push` exit 0 | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Docker/DB/Gradle offline en la nube | Evidencia 91.6/98.8 NO COMPROBADA | Se declara NO COMPROBADO con el error literal |
| Volumen (~22k líneas main + ~30k test) | Cobertura parcial | Lectura repartida en 4 subagentes de solo lectura (autorizado en CLAUDE.md) y cobertura declarada honestamente |

## Desvíos registrados
- 2026-11-20: por indicación de Pablo (retransmitida por la sesión coordinadora), los hallazgos se catalogan y se suben a la rama en tandas, no solo al final.
- H1.S1.M6: la parte de Gradle, yarn y Flutter quedó `BLOCKED` por el entorno (429 de Maven Central, sin Docker, yarn 1 frente a 4, sin Flutter); se cierra con los verificadores Python ejecutados y lo demás declarado NO COMPROBADO, como manda el metaprompt.
