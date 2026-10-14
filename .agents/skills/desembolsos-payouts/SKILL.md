---
name: desembolsos-payouts
description: "Sacar dinero de AportaYa hacia afuera: cuenta bancaria de destino verificada y cifrada, orden de desembolso idempotente, intentos con clasificación de error, historial de estados, incidencias con SLA y conciliación. Úsala al implementar retiro, entrega de fondo, devolución o cualquier salida de dinero, al integrar un proveedor de payout, o cuando un beneficiario diga que no le llegó."
---

# Sacar dinero: desembolsos

`qr-pagos` cubre el dinero que **entra**. Esta skill cubre el que **sale**, que es
el que duele cuando se hace dos veces.

```
entrega/retiro autorizado → retención → orden_desembolso (1 por entrega, idempotente)
   → intento_desembolso (N, cada uno con su error) → acuse → asiento + conciliación
```

## La regla que ordena todo

**El dinero no se debita hasta el acuse de acreditación.** Antes de eso está
*retenido* ([[retencion_saldo]]), no gastado. Un desembolso que falla tiene que
dejar el saldo disponible otra vez, sin intervención humana.

## Antes de ordenar

| Requisito | Restricción |
| --- | --- |
| Entrega o retiro **autorizado** | `R-SEG-04` — quien autoriza no ejecuta |
| Cuenta destino `VERIFICADA` y fuera de enfriamiento | `R-DES-02` — trigger en la base |
| Monto **retenido** | `R-BIL-07` |
| Proveedor activo que cubra entidad y moneda | `R-DES-01` |

## La cuenta de destino

```
numero_cuenta_cifrado  ← cifrado, nunca en claro
hash_numero_cuenta     ← para detectar duplicados sin descifrar
numero_enmascarado     ← lo único que se muestra y lo único que viaja en un mensaje
```

`R-SEG-01`: el número completo no se persiste, no se registra en bitácora y no sale
en ninguna notificación. Verificar la titularidad es obligatorio y el documento
tiene que coincidir con el del titular de la billetera: **no se transfiere a cuentas
de terceros desde una billetera personal**.

## Clasificar el error del proveedor

La tabla es explícita y es lo que decide si hay reintento:

| Clase | Ejemplos | Qué se hace |
| --- | --- | --- |
| **Definitivo** | cuenta inexistente, cerrada, titular no coincide, moneda no soportada | `RECHAZADA`, **sin reintento**, saldo liberado, se pide otra cuenta |
| **Transitorio** | timeout, 5xx, límite de ritmo, mantenimiento | reintento con espera creciente y jitter, hasta el tope |
| **Incierto** | sin respuesta y el proveedor no tiene consulta de estado | `EN_VERIFICACION`: **ni se acredita ni se cancela** hasta conciliar |

Reintentar un rechazo definitivo gasta intentos y confunde al usuario. Cancelar un
incierto puede pagar dos veces. La tercera fila es la peligrosa y por eso
`soporta_consulta_estado = false` es un dato de alta prioridad al elegir proveedor.

## Idempotencia

```ts
claveIdempotencia = uuidv5(`desembolso:${entregaId}`, NAMESPACE_APORTAYA)
```

Derivada de la entrega, **no del proveedor**: conmutar de proveedor no puede volver
a pagar. `uq_orden_desembolso_clave` y `uq_orden_desembolso_entrega_viva` lo
sostienen en la base (`R-DES-01`).

## Estados y su historia

Cada transición escribe [[historial_estado_entrega]] con estado anterior, nuevo,
motivo y quién. **Esa tabla es lo que ve el beneficiario**, sin traducciones
piadosas: "enviada al banco" no se muestra como "acreditada".

```
CREADA → ENVIADA → ACREDITADA
              ↘ RECHAZADA (definitivo)
              ↘ FALLIDA   (agotados los reintentos)
```

## Incidencias

Cuando algo se traba se abre [[incidencia_entrega]] con `sla_horas` y
**`fecha_limite_sla` calculada y guardada**, asignada a alguien con nombre. Sin
responsable, la incidencia es una queja archivada.

Casos que abren incidencia sí o sí: acreditado por monto distinto, acreditado a
cuenta equivocada, acuse contradictorio, y "el beneficiario dice que no llegó y el
proveedor dice que sí".

## Conciliación

Toda orden se cruza contra el [[extracto_bancario]]. Lo que no cruza es
[[excepcion_conciliacion]] y **bloquea el cierre diario** (`R-BIL-12`). No hay
excepción a esto: un desembolso sin respaldo bancario es un descuadre de custodia,
aunque el proveedor haya dicho que salió.

## Qué no hacer

- No reasignar una orden viva a otro proveedor: se cierra y se emite otra.
- No debitar antes del acuse.
- No dejar que un fallo de desembolso deje el saldo bloqueado indefinidamente.
- No poner el número de cuenta en un mensaje, un log o una traza.
- No mostrar "acreditado" mientras el estado real sea `ENVIADA`.
- No reintentar sin espera creciente: un proveedor caído no se levanta a fuerza de
  peticiones (skill `resiliencia-rendimiento`).

## Ver también

- [[CU-18 Registrar y verificar una cuenta bancaria de destino]] ·
  [[CU-28 Emitir la orden de desembolso y ejecutar el intento]] ·
  [[CU-11 Retirar saldo]] · [[CU-22 Liquidar y entregar el fondo]]
- `R-DES-01` · `R-DES-02` · `R-BIL-09` · `R-BIL-17` en [[Restricciones]]
- Skills: `qr-pagos`, `idempotencia-reintentos`, `contabilidad-partida-doble`,
  `resiliencia-rendimiento`, `reembolsos-disputas`
