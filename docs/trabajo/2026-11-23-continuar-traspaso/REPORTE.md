# Reporte — Continuar traspaso Pasanaku (PR #503–#505 → dev → test)

> **AVANCE: 1 / 11 — 9,1 %.**

- Fecha: 2026-11-23 · Plan: [PLAN.md](./PLAN.md) · Ramas: `codex/f3s8-m4-parametros-backoffice` (#503), `codex/f3-l2-turnos-asignados` (#504), `codex/l4l6-resume` (#505), `claude/modest-newton-4ckcid` (este reporte)
- Peldaño de evidencia alcanzado: `WRITTEN` en #503 y #504 (CI en curso); #505 sin cambios míos.
- Estado provisional: escrito mientras corre la CI de los tres heads. Se actualiza con su resultado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S2.M1 | Causa de "El turno … no tiene titular" (10 tests rojos de #504) demostrada | log del job 138026360778 + lectura de `FixturaDeGrupos.cuposOcupados` y de los caminos que escriben OCUPADO | PASS: la fixture creaba cupos OCUPADO sin `participante_id`; producción siempre lo fija (`CU69Enlace.java:144`, `CU69FirmarReglamento.java:96,177`) |

## A medias
### H1.S1.M1 — TS4111 en spec de parámetros (#503)
- Qué anda: el spec usa acceso por corchetes; `yarn typecheck` local da 0 errores en ese archivo.
- Qué no anda: el typecheck local completo no es representativo (161 errores ajenos: faltan los clientes generados que CI descarga como artefacto).
- Qué falta exactamente: CI "Frontend — Flutter y Angular (ADR-044)" en success sobre `31234da`.
- Dónde quedó: commit `31234da` en `codex/f3s8-m4-parametros-backoffice`, empujado.

### H1.S2.M2 — Fixture con titular (#504)
- Qué anda: `cuposOcupados` crea participante ACTIVO + cupo con `participante_id`, mismo patrón que `participantesConCupo`; no se tocó ninguna aserción ni el código de producto.
- Qué no anda: sin verificar; local BLOCKED (no hay daemon Docker para Testcontainers).
- Qué falta exactamente: "Los cinco corredores" en success sobre `319e7b3`.
- Dónde quedó: commit `319e7b3` en `codex/f3-l2-turnos-asignados`, empujado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M2 | TODO | CI de `31234da` (re-ejecuta también "Formato", que falló por 429 de Maven Central) |
| H1.S1.M3 | TODO | Control encontrado (`CU132RegistroDeParametros.java:168`, `@Permiso("TARIFARIO_ADMINISTRAR")` en `TarifasController.java:245-265`, test `CU132Test` "mismaPersona"); falta ver esos tests pasar en CI |
| H1.S3.M1 | TODO | "Los cinco corredores" de #505 en curso |
| H1.S4.M1 | TODO | PR verdes |
| H2.S1.M1–M3 | TODO | H1 completo; M3 requiere que Pablo ejecute `estado` en su host (SSH bloqueado desde aquí) |

## Evidencia
```text
#503 job 138033735520: Could not GET '.../kotlin-gradle-plugins-bom-2.4.0.pom'. Received status code 429 from server: Too Many Requests
#503 job 138035184830: pantalla-de-parametros-financieros.spec.ts(98,42): error TS4111: Property 'valor' comes from an index signature ...
#504 job 138026360778: 337 tests completed, 10 failed
    java.lang.IllegalStateException: El turno <uuid> no tiene titular
        at bo.aportaya.grupos.infraestructura.TurnosAsignadosRepositorio.lambda$delGrupo$0(TurnosAsignadosRepositorio.java:30)
local typecheck backoffice (tras fix): total errores 161 · en el spec corregido 0
```

## No cubierto
- Ninguna ejecución local de `integrationTest` (sin Docker) ni del typecheck completo con clientes generados.
- Checkpoints M08 y M6: no se tocaron.
- Frontend `c28f69bb`: sin corridas de CI ni PR (consultado; sigue igual).

## Desvíos del plan
- Ninguno de ejecución. Diagnóstico distinto del traspaso: #504 no era el lock de JaCoCo sino datos de prueba; #503 "spotlessCheck" era un 429 de Maven Central.

## Riesgos residuales
- Si existiera algún camino (fuera de `grupos`, o datos migrados) que deje un cupo OCUPADO sin participante, la revelación del sorteo fallaría con `IllegalStateException` en lugar de un error de negocio. No se encontró ninguno en el código.

## Decisiones y ambigüedades
- Se mantiene la invariante "OCUPADO ⇒ titular" del productor porque el pacto `grupos-entregas-turnos-asignados.json` exige `titularParticipanteId`. Confirmar con Pablo que no hay datos legados con OCUPADO sin participante en TEST.
- M08 TTL (24 vs ~25 meses): DECISION_REQUIRED, pendiente de Pablo.
