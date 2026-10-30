# Plan — Traspaso de cupo que funciona de punta a punta

- Fecha: 2026-10-30 · Repos afectados: PasanakuBackend (servicios/grupos, clientes/dart/grupos, apps/movil) · Predecesor: rediseño del traspaso estilo Binance (sin precio)
- Resultado observable: el titular de un cupo pide la votación al grupo desde la app y, con la votación aprobada sobre ESE cupo, traspasa el cupo.
- Kill-test: con una votación aprobada de OTRO cupo, o sin ser titular, el servidor rechaza (AP-CU64-05 / AP-CU64-06).

## Alcance
- IN: CU-64 (titular, votación por cupo, uso único), listado de acuerdos del grupo (CU-63), app: pedir/esperar/usar la votación.
- OUT: backoffice; vincular la votación a la persona entrante (no hay campo en el modelo).
- Decisiones del usuario (2026-10-30): ejecuta el dueño con voto aprobado; el traspaso SIEMPRE se vota.

## H1 — Traspaso votado
**CA:** Dado un titular con la votación aprobada de su cupo, cuando traspasa, entonces el cupo cambia de titular y el acuerdo queda EJECUTADO.
**DoD:** integración grupos + web + suite app en verde.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | CU-64: titular, grupo de la ruta, acuerdo del cupo, uso único | 6 casos de `CU64TitularYAcuerdoTest` | `gradle :servicios:grupos:test` | HECHO |
| H1.S1.M2 | GET /acuerdos del grupo (participantes) | 3 casos de `AcuerdosListadoWebTest` | `gradle :servicios:grupos:test` | HECHO |
| H1.S2.M1 | App: pedir votación / esperar / traspasar con el acuerdo aprobado | `pantalla_traspaso_test` 14/14 | `flutter test test/pasanaku/pantalla_traspaso_test.dart` | HECHO |
| H1.S2.M2 | Regresión app + gate de frontend | suite completa verde | `flutter test ...` + `verificar_frontend.py movil` | HECHO |
