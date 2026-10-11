# H11.S1.M3 · Distribuir intereses confirmados (R-INV-09)

Carril I2. Especificación: `docs/CasosDeUso/CU-126 Distribuir los intereses al titular y conciliarlos.md`.

## Qué se modeló y por qué así

Fuente única: `docs/entidades/15_inversiones.puml` + `docs/Restricciones.md` (R-INV-09) + `scripts/modelo.py`
(`distribucion_interes` en el conjunto append-only) → `scripts/generar_ddl.py` → `sql/` (**generado, no editado**).

- **`distribucion_interes`** (nueva, append-only): por rescate de DPF, un titular, un período
  (`periodo_desde` = constitución de la posición, `periodo_hasta` = corte), el devengo propio al corte,
  la penalización, el interés esperado, el interés pagado por el emisor, la retención y el interés neto;
  `estado` `CONCILIADA` | `DISCREPANCIA` con `motivo_discrepancia`.
- **`movimiento_posicion` NO se crea** (el ADR del carril D lo mencionaba). El saldo de una posición se deriva de
  `comprobante_inversion` (append-only, ya registra la constitución y cada liquidación); un segundo libro de
  movimientos de la posición diverge del primero sin aportar nada. Decisión registrada en CU-126.
- **`corrida_trabajo`** (nueva): ver `H10-S1-trabajos-idempotencia.md`.

## Qué rechaza la base (R-INV-09), con prueba

| Regla | Mecanismo | Prueba (`CU126Test`) |
| --- | --- | --- |
| El interés se reparte al titular de la posición | trigger `tg_distribucion_interes_coherente` | `baseProtegeLaDistribucion` |
| El período arranca en la constitución de la posición | ídem | ídem |
| Interés y retención son los del comprobante de liquidación | ídem | ídem |
| `esperado = devengado − penalización`; `neto = pagado − retención` | CHECK `ck_distribucion_interes_esperado` / `_neto` | `baseProtegeLaConciliacionDelReparto` |
| `CONCILIADA` solo si `esperado = pagado` (y `DISCREPANCIA` con motivo si no) | CHECK `ck_distribucion_interes_estado` | ídem |
| Ni `UPDATE` ni `DELETE` (R-AUD-01) | trigger append-only + `REVOKE` | `baseProtegeLaDistribucion` |

## Conciliación contra devengo y contra la liquidación externa

- **Contra la liquidación externa:** ya existía (`conciliacion_interes`, R-INV-08): el interés y la retención que informa el
  emisor se comparan con el cálculo propio y, si difieren, `AP-CU125-01` y no se acredita ni se reparte nada
  (`CU126Test.sinRepartoSiElEmisorDiscrepa`).
- **Contra el devengo:** el devengo diario (`devengo_dpf`) hasta el corte, menos la penalización del contrato en la
  cancelación anticipada, tiene que ser el interés pagado. Si no hay devengo hasta el corte la distribución queda
  `DISCREPANCIA` con `SIN_DEVENGO_AL_CORTE` y **el titular cobra igual** (el pago ya pasó el control fuerte): se corrige
  corriendo el devengo (`CU126Test.sinDevengoQuedaComoDiscrepancia`). El reporte de CU-128 cuenta esas discrepancias.

## Decisiones abiertas (no se cierran acá)

| ID | Qué | A quién |
| --- | --- | --- |
| DR-INV-03 | La retención es una tasa **sintética** de las condiciones; el tratamiento fiscal real | Fiscal |
| DR-INV-01 | Si el emisor paga intereses periódicos antes del vencimiento (hoy solo se reparte el de la liquidación) | Aliado + producto |

## Esquema cambiado

Cambió el esquema (dos tablas, un trigger, CHECKs y un trigger de marca máxima): **hay que regenerar `sql/` y el cliente
jOOQ** (`python scripts/generar_ddl.py`; borrar `servicios/*/build/generated/jooq`). En este carril se regeneró y se aplicó
sobre un Postgres desechable (puerto 5438).
