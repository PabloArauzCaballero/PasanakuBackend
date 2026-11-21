# Reporte — Consolidado de las auditorías L1–L6

> **AVANCE: 4 / 4 — 100 %.** Consolidado documental (peldaño `DISCOVERED`); no re-verifica los hallazgos en el código.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/nice-fermat-essf9e`
- Entregable: [CONSOLIDADO.md](./CONSOLIDADO.md)

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Totales por lote y severidad | `sed -n '/^## 2/,/^## 3/p' AUDITORIA-*.md \| grep '**Total**'` | PASS: 45, 68, 55, 176, 88, 42 = 474 (S1 32) |
| H1.S1.M2 | Lista de los 32 S1 | `awk` sobre las fichas con «Severidad» S1, contrastado con las tablas | PASS: 32 filas; L1 HUECO-006 quedó en S3 y L6 RESPALDO-002 en S2, según sus fichas |
| H1.S1.M3 | Inventarios y A-1..A-4 unificados | lectura de la §4 de los seis informes | PASS |
| H1.S1.M4 | 20 preguntas para el plan | — | PASS (ninguna respondida por supuesto) |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Plan de correcciones | TODO | Respuestas de Pablo a P-01..P-20 (DECISION_REQUIRED) |

## Evidencia
```text
L1 | **Total** | 8 | 20 | 16 | 1 | 45 |
L2 | **Total** | 13 | 25 | 24 | 6 | 68 |
L3 | **Total** | **2** | **25** | **25** | **3** | **55** |
L4 | **Total** | **3** | **61** | **81** | **31** | **176** |
L5 | **Total** | **5** | **34** | **41** | **8** | **88** |
L6 | **Total** | **1** | **14** | **24** | **3** |   (42)
```

## No cubierto
- No se volvió a verificar ningún hallazgo en el código. No se ejecutó nada contra la base, Gradle ni Flutter.
- No se buscaron duplicados entre lotes hallazgo por hallazgo; solo se agruparon por tema (§4).

## Desvíos del plan
ninguno

## Riesgos residuales
- Los 474 incluyen duplicados entre lotes, así que el número de problemas distintos es menor.
- `blocker_gate` traba el cierre de sesiones en este repo (T9).

## Decisiones y ambigüedades
A-1..A-4 y P-01..P-20 siguen abiertas. Las responde Pablo, y Cumplimiento en lo regulatorio.
