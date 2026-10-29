# Plan — Poner en vigencia un tarifario desde el backoffice (CU-34)

- Fecha: 2026-10-29 · Repos afectados: monorepo Pasanaku (apps/backoffice) · Predecesor: backlog `2026-10-29-backoffice-decisiones` ítem 4
- Rama: `claude/tarifario-vigencia-20261029` (desde `origin/test`)
- Resultado observable: un operador con `TARIFARIO_PUBLICAR` ve «Poner en vigencia» en cada versión `EN_PREAVISO`,
  confirma, y la versión pasa a VIGENTE (la anterior queda SUSTITUIDA); si el preaviso no se cumplió, el servidor lo
  rechaza (R-TAR-08) y la pantalla lo explica.
- Kill-test: una versión EN_PREAVISO sin botón, o un error crudo del servidor → no está hecho.

## Hallazgos
- `POST /tarifas/tarifarios/{id}/vigencia` (`ponerTarifarioVigente`, `tarifas.yaml:277`), `@Permiso("TARIFARIO_PUBLICAR")`
  (`TarifasController.java:151`); ADMIN_PLATAFORMA lo tiene. Sin cuerpo y sin clave de idempotencia en el contrato.
- La pantalla (`rutas/configuracion/tarifario/pantalla-de-tarifario.ts`) es de solo lectura; `recargar()` en
  `contabilidad/listado/listado-por-cursor.ts:95`.

## Alcance
- IN: botón por fila EN_PREAVISO (si la sesión tiene TARIFARIO_PUBLICAR), confirmación, llamada, mensaje y recarga.
- OUT: **publicar una versión nueva** (`publicarTarifario`): exige acta de comité, aprobador, URL y hash del documento y
  no trae precios en el contrato; es un acto regulado que toca comisiones → **DECISION_REQUIRED** (Finanzas/Legal: de
  dónde salen los precios de la versión nueva y quién aprueba). No se calcula en el cliente si el preaviso venció:
  lo decide el servidor.

## H1
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Dominio `crearPonerVigente()` | POST a la URL del contrato, sin cuerpo | spec del dominio | HECHO |
| H1.S1.M2 | Botón + confirmación + resultado | correcto (200 → mensaje y recarga) / límite (sin permiso o no EN_PREAVISO: sin botón) / inválido (422 R-TAR-08 traducido, conserva la pantalla) | spec de la pantalla | HECHO |
| H1.S1.M3 | Regresión completa + typecheck | sin rojos | suites | HECHO |
