# Plan — QR_INTEROP activo solo en la semilla de dev/TEST + REPORTE 7/7 del trabajo anterior

- Fecha: 2026-11-27 · Repos afectados: PasanakuBackend (`seeders/dev`, `sql/61_dev` generado, `despliegue/ensayos`, `scripts/generar_semillas.py`) · Predecesor: `docs/trabajo/2026-11-26-pasarela-simulada-cu21-endpoint` (#547)
- Resultado observable: en una base dev/TEST sembrada, `proveedor_pago.QR_INTEROP.activo = TRUE`; en una base de produccion (solo `minimos/`) sigue `FALSE`. Segunda corrida: 0 filas cambiadas.
- Kill-test: sembrar `minimos` y comprobar `activo = FALSE`; sembrar dev DOS veces y comprobar `activo = TRUE` y que la segunda corrida da `UPDATE 0`.
- DESVIO declarado: el seeder y el guion se escribieron ANTES de este PLAN (se redacto al terminar el descubrimiento); no hay mas codigo pendiente que el de las microtareas de abajo.

## Descubrimiento (hechos)
- `QR_INTEROP` se siembra con `activo = FALSE` en `sql/60_semillas/14-proveedores-externos.sql:6` (generado desde `seeders/minimos/14`).
  `ProveedorPagoRepositorio.activoConWebhook` exige `activo` ⇒ el aviso simulado (#547) responde `AP-CU100-12` mientras siga inactivo.
- Las semillas de dev viven en `seeders/dev/*.json` (guarda `app.entorno = 'dev'`, `sql/61_dev/sembrar_dev.sql:17-26`); la CI las aplica dos veces
  (`.github/workflows/ci.yml` paso 9) y comprueba que produccion las rechaza (paso 10). TEST NO recibe `sembrar_dev.sql` entero: recibe guiones
  por ssh con el SQL generado (`SIEMBRAS_DE_TEST` en `scripts/generar_semillas.py`), p. ej. `sandbox_licencia_test.sh`.
- El generador admite bloques `sql` sueltos (re-ejecutables a mano), sin la guarda `app.dev_sembrado` (`scripts/generar_semillas.py:193-201`).

## Alcance
- IN: seeder `seeders/dev/19-…json` (UPDATE idempotente), manifiesto, `SIEMBRAS_DE_TEST`, guion `proveedor_qr_test.sh` con guardas, SQL generado, evidencia
  de la segunda corrida, REPORTE 7/7 del trabajo anterior.
- OUT: la semilla minima de produccion (sigue `activo = FALSE`), url/credenciales/comisiones del proveedor, cualquier edicion de la base a mano,
  regenerar catalogos de operaciones (PR aparte).
- Ambigüedad: ninguna nueva; la decision de activar solo en dev/TEST es de Pablo (relay 2026-11-27).

## H1 — QR_INTEROP activo solo en dev/TEST
**CA:** Dado una base con `minimos`, QR_INTEROP esta inactivo; dado el mismo estado con las semillas de dev, esta activo; sembrar dev otra vez no cambia nada.
**DoD:** salida pegada de los pasos 8/9/10 de la CI reproducidos en PostgreSQL local · `python3 scripts/generar_semillas.py` sin diff · `bash -n` del guion. **Estado:** A MEDIAS

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Seeder dev/19 + manifiesto + SQL generado | UPDATE solo sobre QR_INTEROP inactivo | generador sin diff posterior | HECHO |
| H1.M2 | Segunda corrida idempotente (minimos ×2 → inactivo; dev ×2 → activo; produccion lo rechaza) | `UPDATE 1` luego `UPDATE 0` | salida pegada en `evidencia/` | HECHO |
| H1.M3 | Guion `proveedor_qr_test.sh` (guardas de perfil y entorno, `--estado`, `--retirar`) | sintaxis valida, mismo SQL que el seeder | `bash -n`; ejecucion real BLOQUEADA (sin Docker) | A MEDIAS |
| H1.M4 | REPORTE del trabajo anterior a 7/7 con CI de `b9e4b424` | reporte honesto | lectura | HECHO |
| H1.M5 | spotless/verificaciones, un push, PR contra `dev`, CI verde, aviso a `@parent` | PR verde fuera de borrador | checks del PR | A MEDIAS |

## Riesgos
| Riesgo | Impacto | Mitigacion |
|---|---|---|
| Alguien activa QR_INTEROP en un entorno no dev | cobros hacia un proveedor sin contrato | solo `seeders/dev` (guarda `app.entorno='dev'`) y guion con guarda de perfil `local` y de entorno |
| El guion no se puede ejecutar en el sandbox | sin prueba del guion real | se declara BLOQUEADO; se prueba su SQL y su sintaxis |
