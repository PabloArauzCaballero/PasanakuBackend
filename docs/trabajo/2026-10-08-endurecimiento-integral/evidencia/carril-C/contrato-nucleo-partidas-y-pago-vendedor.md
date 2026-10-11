# Contrato que `garantia` y `entregas` esperan de `nucleo-financiero` (canal de coordinacion)

Escrito por el carril O2 el 2026-10-09. **Es el unico canal con quien implementa el consumo en `nucleo-financiero`**:
este carril no toca ese servicio. Todo lo de abajo esta ejercitado contra DOBLES en pruebas que se citan; nada de esto
esta verificado contra el productor/consumidor real. Cuando `nucleo-financiero` lo publique, las pruebas citadas se
repiten contra el (se indica cual).

## A. Consumo de eventos con partidas (CU-24 `registrar asiento`)

Cada evento de abajo viaja por el outbox de su servicio (`<esquema>.evento_dominio`) y trae `payload.asiento`:

```json
{ "origenTipo": "COBERTURA|AJUSTE|ENTREGA",
  "origenId":   "<uuid del agregado que origina el asiento>",
  "partidas": [ { "cuenta": "<CUENTA LOGICA>", "debe": "6000.00", "haber": "0.00", "moneda": "BOB" },
                { "cuenta": "<CUENTA LOGICA>", "debe": "0.00", "haber": "6000.00", "moneda": "BOB" } ] }
```

Reglas del contrato (el consumidor las tiene que hacer cumplir y las pruebas citadas las verifican en el doble):

1. **Cuadre**: `sum(debe) = sum(haber)` por asiento y mayor a cero; importes como CADENA decimal de dos decimales, moneda al lado.
2. **Idempotencia doble**: el mismo evento (`evento_dominio.id`) entregado dos veces registra UN asiento; y el mismo
   `(origenTipo, origenId)` bajo OTRO evento tampoco registra un segundo asiento.
3. **Append-only**: una correccion es un contra-asiento (`origenTipo` igual, partidas invertidas), nunca un cambio.
4. **Cuentas LOGICAS**: los nombres de cuenta son logicos. El mapeo al plan de cuentas real (`cuenta_contable`) lo
   decide contabilidad (ambiguedad A5): este contrato NO inventa codigos contables.

| Evento | Servicio | `origenTipo` / `origenId` | Debe | Haber | Prueba que lo ejercita (doble) |
|---|---|---|---|---|---|
| `garantia.respaldo_reservado`, `garantia.respaldo_ampliado` | garantia | `AJUSTE` / reserva | `RESERVA_RESPALDO` | `CAJA_EMPRESA` | `CU131CoberturaTest`, `AsientoPropuestoTest` |
| `garantia.respaldo_liberado` | garantia | `AJUSTE` / reserva | `CAJA_EMPRESA` | `RESERVA_RESPALDO` | `CU131RespaldoTest` |
| `garantia.cobertura_empresarial_aplicada` | garantia | `COBERTURA` / cobertura | `POZO_GRUPO` | `RESERVA_RESPALDO` | `CU131CoberturaTest` (MayorDeDoble de garantia) |
| reversa de cobertura | garantia | `COBERTURA` / cobertura | `RESERVA_RESPALDO` | `POZO_GRUPO` | `CU131CoberturaTest` |
| recuperacion por aporte tardio | garantia | `COBERTURA` / recuperacion | `CAJA_EMPRESA` | `POZO_GRUPO` | `CU131RecuperacionTest` |
| **`entregas.desembolso_acreditado`** (nuevo, H8.S1.M4) | entregas | `ENTREGA` / entrega | `POZO_GRUPO` | `DESEMBOLSO_CONFIRMADO` | `CU133EntregaConsistenteTest` (`MayorDeDoble` de entregas) |

Notas: el desembolso sale por el **principal completo** (bruto = neto = pozo, sin deducciones; A6). `origen_tipo` `COBERTURA`,
`AJUSTE` y `ENTREGA` ya los admite `asiento_contable` (`sql/10_tablas/03_aportes_pagos_qr/asiento_contable.sql`).

Eventos de **escalamiento** (sin partidas, para notificaciones/backoffice, no para el mayor): `garantia.respaldo_insuficiente`,
`garantia.contingencia_de_caja_activada` (responsables, roles, obligaciones, comunicacion) y `garantia.contingencia_de_caja_resuelta`.

## B. Operacion "pagar al vendedor con retencion de un tercero" (`FondosDelComprador`)

Hoy `nucleo-financiero` tiene `retenerSaldo` y `cerrarRetencion(LIBERADA|EJECUTADA)` pero **no** ejecutar una retencion A FAVOR
de un tercero. El puerto esta en `servicios/entregas/.../dominio/puertos/FondosDelComprador.java`; el adaptador por omision
(`FondosNoDisponibles`) deniega todo (`NO_DISPONIBLE`) y la compra queda detenida y reanudable.

| Operacion | Entrada | Salida | Idempotencia |
|---|---|---|---|
| `retener(compradorUsuarioId, monto, clave)` | monto positivo | `OK(referencia)` / `SALDO_INSUFICIENTE` / `NO_DISPONIBLE` | por `clave` (`cesion:<id>:retener`) |
| `pagarAlVendedor(retencionRef, vendedorUsuarioId, paraElVendedor, cargos, clave)` | `paraElVendedor + cargos` = monto retenido, exacto | `OK(referencia)` / `NO_DISPONIBLE` | por `clave` (`cesion:<id>:pagar`) |
| `liberar(retencionRef, clave)` | retencion viva | `OK` / `NO_DISPONIBLE` | por `clave` (`cesion:<id>:liberar`) |

Semantica obligatoria (la verifica `ContratoDeFondosDelComprador`, 9 pruebas en tres niveles, hoy contra el doble en
`DobleDeFondosCumpleElContratoTest`):

- pagar mueve `paraElVendedor` al vendedor y `cargos` a la plataforma en el MISMO asiento; el total del sistema no cambia;
- **lo ejecutado no se libera y lo liberado no se paga** (`NO_DISPONIBLE`): lo irreversible no se deshace a ciegas;
- un importe que no sea exactamente la retencion no mueve nada;
- misma `clave` => misma referencia y ningun efecto nuevo; con nucleo caido responde `NO_DISPONIBLE` sin retener ni pagar.

**Para quien implemente el lado real**: agregar una subclase de `ContratoDeFondosDelComprador` que arme el adaptador HTTP
contra un nucleo levantado y debe pasar las mismas 9 pruebas; sin eso H9.S1.M5 queda cerrada solo contra doble.

## C. Que quedo contra doble y que no

| ID | Contra doble (hecho) | Contra el real (pendiente de `nucleo-financiero`) |
|---|---|---|
| H8.S1.M3 cobertura empresarial | partidas del evento, cuadre, idempotencia, reversa | consumo real del evento y asiento en `asiento_contable` |
| H8.S1.M4 entrega del pozo | asiento `ENTREGA` cuadrado, confirmacion y comprobante coinciden, una sola vez | consumo real de `entregas.desembolso_acreditado` |
| H9.S1.M5 liquidacion de la cesion | saga completa, reinicio en dos puntos, contrato de `FondosDelComprador` | operacion `pagarAlVendedor` real |
| H9.S1.M6 cancelacion vs compra | entrelazados deterministas + carrera con hilos (12 rondas) | la misma carrera con `liberar`/`pagar` reales (la barrera depende de que nucleo rechace liberar lo ejecutado) |
