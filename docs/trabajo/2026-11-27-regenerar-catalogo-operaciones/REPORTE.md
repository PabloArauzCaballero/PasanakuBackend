> **AVANCE: 2 / 3 — 66,7 %.**

# Reporte — Regenerar catalogo-operaciones.json y operaciones.json

- Fecha: 2026-11-27 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/regenerar-catalogo-operaciones` (desde `origin/dev`)
- Peldaño de evidencia alcanzado (regla 30): **TESTED** para la regeneración (determinista, copias iguales, análisis del diff); sin CI todavía. No hay recorrido en el backoffice.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Causa del diff grande demostrada: desfase real, no ruido | script de análisis (salida en [evidencia](./evidencia/analisis-del-diff.txt)) | 349 → 396 operaciones: 49 nuevas, 2 retiradas, 56 con cambios reales; todas verificadas contra los OpenAPI |
| H1.M2b | `auditoria` (que sirve el catálogo) sigue verde con el catálogo nuevo | `./gradlew :servicios:auditoria:test :servicios:auditoria:webTest` (PostgreSQL 16 local) | `test` 65/65 · `webTest` 82/82 |
| H1.M2 | Catálogos regenerados con el generador | `python3 scripts/generar_catalogo_operaciones.py` ×2 + `cmp` | mismos bytes entre corridas y entre las dos copias; `flujos.json` sin cambios |

## A medias
### H1.M3 — Push, PR, CI verde (frontend incluido), aviso
- Qué anda: archivos regenerados y analizados.
- Qué no anda / falta: un push, PR contra `dev` fuera de borrador, CI verde (esta rama toca `apps/backoffice`, así que corre el job de frontend) y aviso a `@parent`.
- Dónde quedó: rama local con los cambios.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Paso de CI que compare el catálogo con el OpenAPI (como el de Postman) | propuesta, no hecha | Decisión de Pablo: sin ese paso el catálogo vuelve a desfasarse |

## Evidencia
[evidencia/analisis-del-diff.txt](./evidencia/analisis-del-diff.txt).

## No cubierto
- Que el backoffice muestre bien las 47 operaciones netas nuevas (no se levantó la app; lo cubre el job de frontend de la CI solo si hay pruebas sobre ese JSON).
- El catálogo trae 396 entradas y el OpenAPI 395 `operationId` únicos: la diferencia (un `operationId` repetido entre servicios) no se investigó.

## Desvíos del plan
Ninguno.

## Riesgos residuales
- El diff es grande (18 000 líneas insertadas) y difícil de revisar a ojo: por eso va aparte y con el análisis.

## Decisiones y ambigüedades
- Se aceptó todo el cambio porque cada diferencia corresponde a una operación o texto vigente en los OpenAPI; no se descartó ningún bloque.
