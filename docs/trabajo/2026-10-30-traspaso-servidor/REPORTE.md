# Reporte — Traspaso de cupo que funciona de punta a punta

> **AVANCE: 5 / 5 — 100 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/traspaso-servidor`
- Peldaño de evidencia alcanzado: `TESTED` (integración contra Postgres real con Testcontainers + widget tests de la app contra un backend simulado). No se ejercitó en el servidor TEST desplegado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Traspasa solo el titular (AP-CU64-06); el cupo tiene que ser del grupo de la ruta (AP-CU64-01); el acuerdo tiene que ser de ESE cupo y estar APROBADO; al usarse queda EJECUTADO | `./gradlew :servicios:grupos:test` | PASS — integración 281/281, web 126/126, unitarios en verde |
| H1.S1.M2 | `GET /acuerdos?grupoId&tipo&estado&referenciaAfectadaId` para participantes (AP-CU63-01 a quien no participa, 403 sin permiso) | idem | PASS |
| H1.S2.M1 | La app lee la votación del cupo: sin votación ofrece «Pedir la votación al grupo» (POST /acuerdos TRASPASO_CUPO sobre el cupo, con clave de idempotencia); abierta muestra «Esperando la votación»; aprobada traspasa enviando su `acuerdoId` | `flutter test test/pasanaku/pantalla_traspaso_test.dart` | PASS 14/14 |
| H1.S2.M2 | Regresión de la app y gate | `flutter test test/unidad … test/navegacion --timeout 90s` · `python3 scripts/verificar_frontend.py movil` · `dart analyze --fatal-infos lib test` | PASS — 1146 pruebas, gate OK, sin issues |

| H1.S1.M3 | Carrera: dos pedidos simultáneos con el MISMO acuerdo (y distinto entrante) traspasaban los dos. Ahora el acuerdo se gasta primero con un UPDATE condicionado a APROBADO; el segundo espera el bloqueo, no encuentra la fila y recibe AP-CU64-05 | `./gradlew :servicios:grupos:integrationTest` | PASS — integración 282/282, web 126/126, unitarios 72/72; mutante (ignorar el resultado del UPDATE) hace fallar la prueba de concurrencia |

Mutantes: quitar la comprobación del titular o el filtro por cupo hace fallar la suite (exit=1 en ambos).

## A medias
Ninguna.

## Pendiente
Ninguna dentro del alcance.

## Evidencia
```text
$ flutter test test/pasanaku/pantalla_traspaso_test.dart
00:09 +14: All tests passed!
$ flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku test/a11y test/goldens test/billetera test/navegacion --timeout 90s
04:29 +1146: All tests passed!
```

## No cubierto
- Flujo real en el servidor TEST (votar con otros participantes hasta aprobar y traspasar).
- Capturas nuevas de los estados «Pedir la votación» / «Esperando la votación».

## Desvíos del plan
- La especificación de CU-64 decía que ejecuta `GRUPO_ADMINISTRAR`; por decisión del usuario ejecuta el titular con la votación aprobada. Queda escrito en la descripción del contrato (`grupos.yaml`).

## Riesgos residuales
- La votación se ata al cupo, no a la persona entrante: el modelo de `acuerdo` no tiene ese campo. La persona entrante solo queda en el texto de la votación. Un titular podría usar una votación aprobada para traspasar a otra persona del grupo.

## Decisiones y ambigüedades
- Ejecuta el titular con voto aprobado; el traspaso siempre se vota (usuario, 2026-10-30).
- Atar la votación a la persona entrante exige un cambio de modelo: confirmar con producto.
