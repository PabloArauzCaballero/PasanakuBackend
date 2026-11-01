# Plan — La votación del traspaso dice a quién entra · contraseña una sola vez

- Fecha: 2026-10-30 · Repos: PasanakuBackend (modelo, sql, servicios/grupos, clientes/dart/grupos, apps/movil) · Predecesor: `2026-10-30-traspaso-servidor`
- Resultado observable: (1) el titular solo puede traspasar a la persona que el grupo votó; (2) el registro pide la contraseña una vez, con el botón para verla.
- Kill-test: con una votación aprobada para OTRA persona, el traspaso se rechaza (AP-CU64-05).
- Decisiones del usuario (2026-10-30): «lo más recomendado» en ambos puntos.

## Alcance
- IN: columna `acuerdo.entrante_participante_id` (opcional) desde el `.puml`; convergencia en bases existentes; CU-63 exige quién entra en `TRASPASO_CUPO`; CU-64 lo compara; listado lo devuelve; app; quitar «Confirmar contraseña».
- OUT: la entidad `disponibilidad_servicio_dia` que la bóveda no tiene (deriva previa, no se toca).

## H1 — Votación con persona entrante
**Estado:** HECHO

| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H1.M1 | Columna en el modelo + `ADD COLUMN IF NOT EXISTS` generado | `generar_ddl.py` + `aplicar.sql` dos veces sobre base existente y una sobre base nueva | HECHO |
| H1.M2 | CU-63 exige y guarda quién entra (AP-CU63-06) | `webTest` | HECHO |
| H1.M3 | CU-64 exige que sea la persona votada | `integrationTest` + mutante | HECHO |
| H1.M4 | Listado devuelve `usuarioEntranteId` | `integrationTest` | HECHO |
| H1.M5 | App: pide la votación con quién entra; la aprobada solo vale para esa persona | `flutter test test/pasanaku/pantalla_traspaso_test.dart` | HECHO |

## H2 — Contraseña una sola vez
**Estado:** HECHO

| ID | Microtarea | DoD | Estado |
|---|---|---|---|
| H2.M1 | Quitar el segundo campo; queda el botón para verla | `flutter test test/identidad` + goldens mirados | HECHO |
| H2.M2 | Regresión app + gates | suite completa + `verificar_frontend.py movil` | HECHO |
