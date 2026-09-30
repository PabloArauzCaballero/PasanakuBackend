# Reporte — seguimiento H1: reintentos concurrentes

- Fecha: 2026-09-22
- Rama: `justin/fix/idempotencia-concurrente-pr2`
- Base: PR #7, `justin/feature/carril-PR2-nucleo-financiero`
- Estado: `REGRESSION_VERIFIED` (módulo financiero)

## Cambio

El único `UNIQUE` no devuelve el resultado original si dos solicitudes con la misma clave pasan el `SELECT` antes del primer `INSERT`. Se toma un bloqueo consultivo transaccional antes del lookup en recarga, retiro y transferencia. El lock usa el mismo scope que el índice respectivo y se libera en commit o rollback.

## Evidencia

La prueba de 50 reintentos coordinados fallaba antes con `uq_tx_idem`; después del cambio todos obtienen el mismo id, hay una sola transacción y el saldo pasa de 1000 a 800. La regresión del módulo se ejecutó con:

```text
./gradlew --no-daemon :servicios:nucleo-financiero:webTest :servicios:nucleo-financiero:integrationTest :servicios:nucleo-financiero:spotlessCheck
BUILD SUCCESSFUL in 4m 1s
```

El `spotlessCheck` global de raíz sigue fallando fuera de este alcance por formato preexistente en `plataforma/comun-archivos`.
