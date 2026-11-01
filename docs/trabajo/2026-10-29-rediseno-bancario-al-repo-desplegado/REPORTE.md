# Reporte — Rediseño bancario de la app móvil en el repo que despliega

> **AVANCE: 4 / 6 — 66,7 %.** Merge, análisis, pruebas y goldens en verde; faltan los PR (en esta misma sesión) y las capturas de la app compilada.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/rediseno-bancario-a-backend`
- Peldaño: **TESTED** (Flutter 3.44.8, la del CI). No VERIFIED en dispositivo.

## Causa
El rediseño «Banca Verde Profunda» (ADR-053) quedó en `PasanakuFrontend` rama `claude/rediseno-bancario` y nunca se mergeó: el APK y la app web salen de PasanakuBackend.

## Completado
| ID | Qué | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Merge, 102 conflictos (93 goldens) | `git diff --diff-filter=U` | 0 |
| H1.M2 | Análisis | `dart analyze --fatal-infos` | movil y diseno: No issues found |
| H1.M3 | Pruebas | `flutter test` | diseno +128 · movil +828 (incluye a11y) · All tests passed |
| H1.M4 | Goldens regenerados con 3.44.8 y mirados | `flutter test test/goldens` | diseno +18 · movil +274 · All tests passed · `evidencia/goldens-billetera-portada.png` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.M5 capturas de la app compilada | TODO | — |
| H1.M6 PR a dev/test y APK | TODO | — |
| Resto del plan del rediseño (85 microtareas) | TODO | Siguiente tanda |

## No cubierto
- Dispositivo real. El golden de la billetera muestra «Tus pasanakus» sin conexión porque el arnés no simula los grupos.

## Decisiones
- Conflictos: función de dev + diseño del rediseño (detalle en el commit del merge `da95ec07`).
