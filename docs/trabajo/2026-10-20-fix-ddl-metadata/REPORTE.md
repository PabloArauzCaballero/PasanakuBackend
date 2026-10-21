# Reporte — Las semillas regeneradas no borran los comentarios de metadata

> **AVANCE: 2 / 2 — 100 %.**

- Fecha: 2026-10-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/fix-ddl-metadata-20261020`
- Peldaño de evidencia: `TESTED` (pasos del job de CI reproducidos en local; falta la corrida en GitHub Actions).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Regenerar semillas reescribe `24-metadata-comentarios.sql` y su `\ir` | `python scripts/generar_ddl.py && python scripts/generar_semillas.py && git diff --quiet -- sql/` | PASS — «sql/ SIN DIVERGENCIA» |
| H1.S1.M2 | Segunda corrida idempotente | `python scripts/generar_semillas.py && git diff --quiet -- sql/ scripts/metadata_enriquecida.json` | PASS |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
verificar_seguridad exit=0
verificar_boveda exit=0
verificar_carriles exit=0
inventario exit=0
contratos exit=0
k8s exit=0
ddl exit=0
semillas exit=0
sql/ SIN DIVERGENCIA
2da corrida idempotente
```

## No cubierto
- Los pasos del job que aplican el esquema en un PostgreSQL limpio (no hay base local en esta sesión); los cubre el CI.
- Los otros checks que fallan en `dev` («Formato, reglas propias y compilacion», «Frontend — Flutter y Angular») no se tocaron.

## Desvíos del plan
- Primero se probó llamar a `inyectar_metadata` desde `generar_ddl.py`; se descartó porque el CI corre `generar_semillas.py` después y volvía a borrar el archivo. La llamada quedó en `generar_semillas.py`, que es quien vacía la carpeta.

## Riesgos residuales
ninguno nuevo.

## Decisiones y ambigüedades
- Se reutiliza `inyectar_metadata.generar()` tal cual; no se cambió su contenido.
