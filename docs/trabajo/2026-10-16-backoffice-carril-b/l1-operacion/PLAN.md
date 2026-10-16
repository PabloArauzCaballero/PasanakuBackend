# Plan — L1 Operación y Tesorería (H1.S1 del carril B)

- Fecha: 2026-10-16 · Repo: Pasanaku (worktree `bo-l1-operacion`, rama `codex/backoffice-ui-l1-operacion`) · Predecesor: `../PLAN.md` (H1.S0.M1 cursor)
- Resultado observable: un operador con el permiso del contrato abre en `/operacion/...` las bandejas reales de retiros, reclamos, conciliaciones, cierres diarios, reversos y desembolsos, y ejecuta las acciones que el contrato publica (aprobar retiro, responder reclamo, conciliar custodia, cerrar día).
- Kill-test: un spec con `HttpTestingController` que espere la URL/parámetros/cuerpo del YAML y no la reciba.

## Alcance
- IN: `apps/backoffice/src/app/rutas/operacion/**`, este directorio.
- OUT: `nucleo/**`, `layout/**`, `app.routes.ts`, `packages/**`, `servicios/**`, otras `rutas/*` (incluida `rutas/cumplimiento/reclamos`, que es otra pantalla de CU-52). Lo necesario fuera se lista en el REPORTE.
- Fuente de verdad: YAML en `servicios/<s>/src/main/resources/openapi/<s>.yaml`.
- Ambigüedades: ver REPORTE (A-7 importes sin moneda; permisos por rol `TESORERIA`/`CONTABILIDAD` en `@Permiso`).

## H1.S1 — Operación y tesorería
**CA:** Dado un operador con el permiso del contrato, cuando abre cada bandeja, entonces ve la página del servidor (cursor), el vacío explicado, el error traducido y el 403 como «sin acceso»; las acciones envían el cuerpo exacto con `Idempotency-Key` y no anuncian éxito sin 2xx.
**DoD:** specs dirigidos verdes + typecheck 0 + `verificar_frontend.py backoffice`.
**Estado:** A MEDIAS — microtareas en `TESTED` (UI contra HTTP simulado); falta prueba visual, E2E contra backend real y navegación a las rutas nuevas (ver REPORTE).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Retiros: bandeja `listarRetirosEnRevision` + ficha `retiros/:ordenId` | cursor `despuesDeFecha/Id`; `puedeDecidir=false` sin acción | spec retiros verde | HECHO |
| H1.S1.M2 | Reclamos: bandeja cursor + ficha + `responderReclamo` | cuerpo `{resultado,respuesta,devolucionId?}`; sin `sinEnviar` | spec reclamos verde | HECHO |
| H1.S1.M3 | Conciliación de custodia: listado + `conciliarCustodia` | cuerpo del contrato, recibo con id | spec custodia verde | HECHO |
| H1.S1.M4 | Cierres diarios: listado + `ejecutarCierreDiario` | cuerpo `{fecha}`, comprobante | spec cierres verde | HECHO |
| H1.S1.M5 | Reversos: listado | `estados` CSV + cursor `antesDe/Id` | spec reversos verde | HECHO |
| H1.S1.M6 | Desembolsos: bandeja de excepciones | sin cuenta destino; `estados` explode | spec desembolsos verde | HECHO |
