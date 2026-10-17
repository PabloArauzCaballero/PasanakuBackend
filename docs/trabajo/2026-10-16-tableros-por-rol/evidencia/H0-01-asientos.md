# H0.S1.M1 — Qué operaciones generan asientos en el mayor

Comando (worktree `../Pasanaku-tableros`, `origin/dev` `d63e4fef`):

```text
grep -rln -iE "asiento|registrarAsiento|contabiliz" servicios/*/src/main/java
```

## Orígenes de asiento existentes

`servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/OrigenAsiento.java`:
`PAGO, ENTREGA, COBERTURA, COMISION, BILLETERA, AJUSTE, COBRO_CXC, DEPRECIACION_ACTIVO, FACTURA_PROVEEDOR`.

## Operación → quién asienta

| Operación (KPI) | Dónde se asienta | Origen |
|---|---|---|
| Saldo cargado (recarga) | `nucleo-financiero/aplicacion/CU10RecargarSaldo.java` | BILLETERA |
| Saldo retirado | `nucleo-financiero/aplicacion/ResolucionDeRetiro.java` | BILLETERA |
| Aporte cobrado | `nucleo-financiero/aplicacion/ConsumoDeAportes.java` (consume `aportes.aporte_cobrado`) | PAGO: billetera del participante → billetera del grupo |
| Impago cubierto | `garantia/aplicacion/CU23CubrirConRespaldo.java` → `AsientoPropuesto` | COBERTURA |
| Recuperación de lo cubierto | `garantia/aplicacion/CU23RecuperarRespaldo.java` | COBERTURA |
| Reversa de cobertura | `garantia/aplicacion/CU23ReversarCobertura.java` | COBERTURA |
| Entrega / desembolso | `entregas/aplicacion/CU28EmitirDesembolso.java`, `entregas/dominio/AsientoDeEntrega.java` | ENTREGA |
| Comisión devengada | `tarifas/aplicacion/CU31DevengarComision.java` | COMISION |
| Inversión | `inversiones/dominio/puertos/LibroDelTitular.java` (puerto; contra un doble, ver el README del servicio) | — |
| **Recargo por mora cobrado** | **Sin asiento propio.** Ver el hallazgo | — |

## HALLAZGO H0-01 (dinero) — el tipo de obligación no llega al mayor

1. `aportes.obligacion_aporte.tipo` admite `APORTE_PERIODICO, APORTE_FONDO_GARANTIA, RECARGO_MORA,
   REPOSICION_COBERTURA, COMISION_PLATAFORMA, AJUSTE_MANUAL` (`sql/10_tablas/03_aportes_pagos_qr/obligacion_aporte.sql:29`).
2. El evento `aportes.aporte_cobrado` (`CU21CobrarAporte.java:136-148`) publica `pagoId, grupoId,
   participanteId, monto, estado`: **no publica el `tipo`**.
3. `ConsumoDeAportes.AporteCobrado` (líneas 94-102) no tiene `tipo`, y siempre asienta billetera del
   participante → billetera del grupo (líneas 173-176).

**Consecuencia:** hoy, un `RECARGO_MORA` pagado terminaría en el pozo del grupo, no en el fondo de garantía (D1).
Lo mismo pasaría con `APORTE_FONDO_GARANTIA`, `REPOSICION_COBERTURA` y `COMISION_PLATAFORMA`, si se cobran por
esta vía. No se reprodujo en runtime: es una lectura del código y queda en peldaño `DISCOVERED`.

**Tratamiento:**
- `RECARGO_MORA` → dentro de este trabajo (H1.S2, microtareas nuevas): se agrega el campo `tipo` al evento
  (cambio aditivo y compatible) y el consumidor asienta `RECARGO_MORA` contra `RESERVA_RESPALDO`.
- Los otros tres tipos → fuera de alcance (regla 00 §3). Se registran como riesgo en el reporte para decidirlos
  aparte. El mismo campo `tipo` los habilita.
