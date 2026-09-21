# Auditoría de producción — `dev` → production ready

Carpeta de trabajo del plan de endurecimiento de `dev` (SHA inicial `19a621e666afdea5bdc40aced326d3f212a116f4`, 2026-09-21).

| Qué | Dónde |
|---|---|
| El plan madre (13 hitos, 210 microtareas, hechos con ruta:línea, decisiones) | [`PLAN.md`](PLAN.md) |
| El reparto en cinco carriles (encargos, dailies, reservas, ritual de entrega) | `PasanakuPromptManager/repartos/2026-09-21/PromptNoche/` |
| Contratos entre carriles, fijados antes de codificar | [`contratos/step-up-jwt.md`](contratos/step-up-jwt.md) · [`contratos/evento-kafka.md`](contratos/evento-kafka.md) |
| Estado vivo de cada carril (lo escribe su dueño) | `carriles/PR<n>-<carril>.md` |
| Salidas literales de cada DoD | `evidencia/<ID>-<slug>.txt` |
| Baseline global y por módulo | `baseline.md`, `baseline-PR<n>-*.md` |
| Cierre | `promotion-gate.md`, `FINAL_REPORT.md` |

Reglas de la casa: plan antes de código, `HECHO` solo con la salida del DoD pegada, nada de datos de personas en ninguna salida. Ver `PasanakuPromptManager/AGENTS.md`.
