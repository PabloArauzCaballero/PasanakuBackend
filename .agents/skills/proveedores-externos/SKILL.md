---
name: proveedores-externos
description: "Integrar y enrutar proveedores en AportaYa —pasarelas, bancos, mensajería, facturación— sin quedar atado a ninguno: adaptador con interfaz común que declara qué soporta, credenciales fuera de la base, salud medida con ventana móvil, conmutación automática pero nunca silenciosa, costo real medido contra el contratado y baja que conserva la consulta. Úsala al dar de alta un proveedor, cuando uno se degrade, o al elegir entre dos."
---

# Proveedores externos

Una pasarela, un proveedor de SMS y un servicio de facturación tienen el mismo
problema de fondo: **no controlamos si funcionan**. La arquitectura tiene que
absorberlo.

```
operación → enrutador (cobertura → prioridad → salud → costo) → adaptador → proveedor
                ↑                                                    ↓
          registro de salud  ←──────────────  acuses y errores
```

## Antes de integrar

| Requisito | Restricción |
| --- | --- |
| [[evaluacion_tercero]] y [[contrato_tercero]] firmado, con cláusulas de datos y continuidad | `SIN_CONTRATO_TERCERO` |
| Juego de casos ejecutado en [[entorno_prueba_regulado]]: cobro, reembolso, **webhook duplicado** y **timeout** | `PRUEBAS_INCOMPLETAS` |
| Credenciales en el gestor de secretos; en la tabla solo `referencia_credenciales` | `CREDENCIAL_EN_TABLA` (`R-SEG-01`) |

Los dos casos que casi nunca se prueban —webhook duplicado y timeout— son los dos
que rompen en producción.

## El adaptador declara qué soporta

Misma interfaz para todos; el adaptador dice qué operaciones implementa y el
enrutador no le pide el resto.

```ts
interface AdaptadorPago {
  crearOrden(...): Promise<Resultado>
  consultarEstado?(...): Promise<Estado>   // opcional, y eso cambia todo
  reembolsar?(...): Promise<Resultado>
  desembolsar?(...): Promise<Resultado>
}
```

> **`soporta_consulta_estado = false` es el dato más importante de un proveedor.**
> Sin consulta de estado, un timeout deja la operación en incertidumbre y solo se
> resuelve por conciliación. Ese proveedor entra con prioridad baja y no se usa para
> montos altos.

## La salud se mide, no se declara

Ventana móvil de operaciones acreditadas sobre iniciadas, por proveedor y operación.
Por debajo del umbral, el proveedor se **degrada** y el tráfico pasa al siguiente.

- La conmutación es **automática, pero nunca silenciosa**: se avisa a operaciones y
  queda registrada.
- Un proveedor nuevo entra con prioridad baja y una porción del tráfico. **Sube según
  su salud medida, no según lo que promete.**
- Existe el interruptor manual para degradar sin esperar a la métrica.

## El costo real casi nunca es el contratado

Se acumula en [[costo_proveedor_operacion]] por operación. Si lo medido supera lo
contratado, se abre la revisión comercial **con el número medido**. El dato pesa más
que el discurso, y sin medirlo la conversación no se puede tener.

## Idempotencia al conmutar

> **La clave de idempotencia es de la operación, no del proveedor.**

Si la clave dependiera del proveedor, conmutar volvería a cobrar o a mandar el mismo
mensaje. Vale igual para pagos (`uq_orden_desembolso_clave`) y para mensajería
(`R-NOT-01`).

## Webhooks entrantes

1. **Validar firma.** Firma inválida → se descarta, se registra el intento, y puede
   abrir [[incidente_seguridad]].
2. **Guardar crudo** en [[webhook_pasarela]] antes de procesar.
3. **Idempotente** por el identificador del proveedor.
4. **Fuera de orden**: la acreditación puede llegar antes que la creación. Se guarda y
   se reprocesa cuando llega el faltante. La red no garantiza orden y el diseño lo
   absorbe (skill `idempotencia-reintentos`).
5. **Manda el último acuse**, no el primero: éxito seguido de fallo se resuelve como
   fallo.

## Dar de baja

Se desactiva **para operaciones nuevas**, pero sigue disponible para consultar y
conciliar lo viejo. **Nunca se borra**: hay órdenes de hace un año cuya referencia
solo ese proveedor puede explicar.

## Cuándo no integrar

Si el proveedor exige guardar datos que el modelo no persiste —número de tarjeta o de
cuenta en claro— **no se integra** (`R-SEG-01`). No hay excepción comercial para eso.

## Qué no hacer

- No poner una credencial en la base ni en una variable de entorno versionada.
- No enrutar por costo antes que por salud: barato y caído sale caro.
- No reintentar contra un proveedor degradado sin espera creciente y jitter.
- No dar por acreditada una operación por respuesta HTTP 200: manda el acuse.
- No borrar un proveedor dado de baja.
- No acoplar la lógica del caso de uso al SDK del proveedor: eso vive en el adaptador.

## Ver también

- [[CU-99 Dar de alta un proveedor de pago y enrutar el cobro]] ·
  [[CU-83 Enrutar el envío por proveedor de mensajería]] ·
  [[CU-28 Emitir la orden de desembolso y ejecutar el intento]] · [[CU-32 Emitir factura electrónica]]
- `R-SEG-01` · `R-BIL-06` · `R-BIL-10` · `R-NOT-01` · `R-RIS-03` en [[Restricciones]]
- Skills: `qr-pagos`, `desembolsos-payouts`, `resiliencia-rendimiento`,
  `idempotencia-reintentos`, `notificaciones-consentimiento`, `decisiones-adr`
