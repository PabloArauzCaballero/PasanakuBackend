# Reporte — Firmar el reglamento siempre deja a quien firma con un cupo

> **AVANCE: 4 / 4 — 100 %.**

- Fecha: 2026-10-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/fix-firma-ocupa-cupo-20261020`
- Peldaño de evidencia: `TESTED` (pruebas dirigidas sobre PostgreSQL real + regresión completa del servicio `grupos`). Falta verlo en el teléfono contra TEST después del despliegue.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | El organizador sin cupo que firma ocupa el primer cupo libre, figura «Firmó» y el contador sube | `integrationTest --tests CU69FirmaYSorteoTest` | PASS — [evidencia/pruebas.txt](./evidencia/pruebas.txt) |
| H1.S1.M2 | Quien ya firmó y quedó ACTIVO sin cupo vuelve a ver «Firmar»; al firmar recupera un cupo y no se duplica nada | misma corrida | PASS |
| H1.S1.M3 | Sin cupos libres, firmar sin cupo se rechaza con AP-CU69-01 y nada cambia | misma corrida | PASS |
| H1.S1.M4 | Regresión de `grupos` | `spotlessCheck test webTest integrationTest` | PASS — 72 + 103 + 258 tests, 0 fallos |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
Ver [evidencia/pruebas.txt](./evidencia/pruebas.txt).

## No cubierto
- No se probó en el teléfono contra TEST (requiere desplegar `grupos`). Después del despliegue, el organizador del grupo afectado abre el grupo, ve «Leer y firmar el reglamento», firma y queda en el primer cupo libre.
- No se consultó la base de TEST: el diagnóstico sale del código, del historial (`4e842cd2`) y de la captura del dueño.

## Desvíos del plan
- La prueba «sin cupo libre» se armó primero con 1 y 2 cupos; la base exige `num_periodos >= 3` (`ck_grupo_num_periodos`): se rehízo con 3 cupos ocupados. Era un error de la prueba, no del producto.

## Riesgos residuales
- Otros grupos creados antes del 2026-10-20 12:29 tienen al organizador sin cupo; se reparan solos cuando su organizador firma (o vuelve a firmar).

## Decisiones y ambigüedades
- El organizador toma el **primer cupo libre** (el menor número), no necesariamente el 1: el 1 puede estar ocupado. Coherente con `CU69Enlace` y `OrganizadorDecideIngreso`, que asignan el primer LIBRE.
- Seguridad: no cambia quién puede firmar (sigue exigiendo ser integrante con firma pendiente o ACTIVO, AP-CU69-13 para el ajeno, test `quienNoEsIntegranteNiFirmaNiVeElDetalle`); la concurrencia la cubre el `FOR UPDATE` de la fila del grupo y del cupo.
