---
name: idempotencia-reintentos
description: "Hacer que reintentar sea seguro en AportaYa: claves de idempotencia, webhooks duplicados y fuera de orden, reintentos con retroceso, trabajos que corren dos veces y respuestas tardías del proveedor. Úsala en todo endpoint con efecto, en todo consumidor de webhook y en todo trabajo del worker. La red duplica; el diseño tiene que absorberlo."
---

# Idempotencia y reintentos

Todo lo que cruza la red se entrega **cero, una o muchas veces**. El usuario toca
dos veces el botón, la pasarela reenvía el webhook, el worker se reinicia a mitad
de un trabajo. Ninguno de esos casos puede producir un segundo cobro.

## Dónde vive la garantía

En la base, como restricción única. No en un `if` que consulta antes de escribir:
entre el `SELECT` y el `INSERT` cabe otra ejecución.

```sql
ALTER TABLE transaccion_billetera
  ADD CONSTRAINT uq_transaccion_clave_idempotencia UNIQUE (clave_idempotencia);
```

El flujo correcto es **intentar escribir y manejar el conflicto**, no preguntar y
después escribir.

## Quién genera la clave

| Origen | Clave | Regla |
| --- | --- | --- |
| Cliente (app, backoffice) | UUID que el cliente genera y **reenvía igual** en el reintento | Va en el contrato (`claveIdempotencia`), obligatoria en toda operación con efecto |
| Webhook de proveedor | Identificador del evento del proveedor + su tipo | Se guarda el cuerpo crudo en `webhook_pasarela` antes de procesarlo |
| Trabajo del worker | Identificador del evento de dominio | Un evento, un efecto, aunque el trabajo corra dos veces |
| Proceso periódico | Clave natural del período (`grupo_id + periodo`) | Correr el cierre dos veces no duplica obligaciones |

Una clave generada por el servidor en cada request **no sirve para nada**: cada
reintento traería una distinta.

## Qué devuelve un reintento

**La respuesta original, íntegra, con `200`.** No un error, no un `409`, no una
respuesta vacía. El cliente que reintenta por timeout no sabe si la primera llegó;
si le devolvemos error, va a intentar de nuevo o —peor— a mostrarle al usuario que
falló algo que sí ocurrió.

Eso implica **guardar la respuesta** junto con la clave, no solo la clave.

## Webhooks: las cuatro fallas reales

| Falla | Qué debe pasar |
| --- | --- |
| **Duplicado** | El segundo no produce efecto nuevo. Se responde `200` igual: si respondemos error, el proveedor lo reenvía para siempre |
| **Fuera de orden** | Una confirmación que llega después de un reverso **no revive** la operación. Se compara contra el estado actual, no se aplica a ciegas |
| **Tardío** | Llega cuando la orden ya expiró: se registra y se resuelve por conciliación, no se ignora en silencio |
| **Desconocido** | Referencia que no existe en el sistema: se guarda igual en `webhook_pasarela` y se alerta. Puede ser un ataque o un error del proveedor |

Y siempre: **verificar la firma antes de procesar**. Un webhook sin firma válida
se guarda y se descarta, nunca se ejecuta.

## Máquina de estados, no banderas sueltas

Las transiciones se validan contra el estado actual, en la base:

```
pendiente → confirmado → reversado
     ↓
  expirado
```

`confirmado → confirmado` no hace nada. `reversado → confirmado` se rechaza. Con
banderas booleanas sueltas ese rechazo no existe y el orden de llegada decide el
resultado.

## Reintentos del lado nuestro

| Situación | Política |
| --- | --- |
| Timeout o `5xx` del proveedor | Reintento con retroceso exponencial y tope; el trabajo queda encolado, no en un bucle |
| `4xx` del proveedor | **No se reintenta**: es un error nuestro. Se marca fallido con evidencia |
| Agotados los reintentos | `cola_muerta` con el motivo, visible en el backoffice. Nunca silencio |
| Operación de dinero | El reintento usa **la misma** clave de idempotencia; si genera una nueva, cobra dos veces |

## El outbox también necesita esto

El trabajo se encola **dentro** de la transacción (`evento_dominio`). Si la
transacción se revierte, el trabajo no existe: nunca se notifica algo que no pasó.
Y si el worker procesa el mismo evento dos veces —porque se reinició después de
actuar y antes de marcar—, el efecto debe ser uno solo. Ver `trabajos-outbox`.

## Pruebas obligatorias

- [ ] Misma operación, misma clave, dos veces ⇒ misma respuesta y **cero filas
      nuevas** (se cuenta antes y después).
- [ ] Misma operación, misma clave, **en paralelo** ⇒ una gana, la otra recibe la
      respuesta original; el saldo queda correcto.
- [ ] Webhook duplicado ⇒ un efecto, `200` las dos veces.
- [ ] Webhook fuera de orden ⇒ no revive lo revertido.
- [ ] Transacción revertida ⇒ el trabajo **no** quedó encolado.
- [ ] Dos réplicas del worker ⇒ el trabajo con fecha corre una vez.

## Antipatrones

| Antipatrón | Qué rompe |
| --- | --- |
| `SELECT` para ver si existe y después `INSERT` | Carrera: dos requests simultáneos pasan los dos |
| Clave generada por el servidor en cada request | No hay dos requests con la misma clave: no hay idempotencia |
| Devolver `409` al reintento | El cliente cree que falló algo que sí pasó |
| Responder error a un webhook duplicado | El proveedor reenvía indefinidamente |
| `sleep` y volver a intentar en el mismo request | Ocupa la conexión y no sobrevive a un reinicio |

## Ver también

`contabilidad-partida-doble` · `trabajos-outbox` · `qr-pagos` · `errores-api` ·
`pruebas-cu` · `docs/Arquitectura/Flujo de una transacción.md`
