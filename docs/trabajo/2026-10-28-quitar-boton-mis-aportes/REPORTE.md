# Reporte — Quitar «Ver el estado de mis aportes» de la pantalla del grupo

> **AVANCE: 2 / 2 — 100 %.**

- Fecha: 2026-10-28 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18` (sin commit)
- Peldaño de evidencia alcanzado: `RUNS`. El analyzer está limpio. No se corrió la app en un dispositivo ni se tomó captura.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Se borraron el botón «Ver el estado de mis aportes» y la constante `_misAportes` de `pantalla_del_grupo.dart` | `grep -rn "estado de mis aportes" apps/movil/lib` | PASS: sin salida, exit 1 |
| H1.S1.M2 | Se quitaron los imports que quedaron sin uso (`boton`, `boton_variante`, `go_router`); el archivo analiza limpio | `flutter analyze lib/pantallas/pasanaku/pantalla_del_grupo.dart` | PASS: `No issues found!` |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Prueba visual en un dispositivo | TODO | Correr la app y abrir un grupo (regla 95.7) |
| Commit y push | TODO | Confirmación de Pablo |

## Evidencia
```text
$ grep -rn "estado de mis aportes" apps/movil/lib
exit=1
$ flutter analyze lib/pantallas/pasanaku/pantalla_del_grupo.dart
Analyzing pantalla_del_grupo.dart...
No issues found! (ran in 2.3s)
```

## No cubierto
- No se tomó captura en runtime. Ningún test de widget cubre `PantallaDelGrupo`: se buscó en `apps/movil/test` y `apps/movil/integration_test` y no hay ninguno.

## Desvíos del plan
ninguno

## Riesgos residuales
- Desde la pantalla del grupo ya no hay un acceso directo a «mi estado». Se sigue llegando por las otras entradas a `/pasanaku/mi-estado`: `pantalla_mis_grupos.dart`, las vistas de unirse y el tutorial.
- `participanteId` y `usuarioId` quedaron en el constructor sin uso, porque los sigue pasando `rutas.dart`. Limpiarlos queda fuera de alcance.

## Decisiones y ambigüedades
- La ruta y la pantalla `mi-estado` se mantienen porque tienen otros consumidores. Se lo confirmo a Pablo.
