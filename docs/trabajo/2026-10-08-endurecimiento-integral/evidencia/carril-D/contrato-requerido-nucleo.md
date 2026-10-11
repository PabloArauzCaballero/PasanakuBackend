# Contrato que `inversiones` le pide a `nucleo-financiero` (DR-INV-05, dueño: carril A / núcleo)

Estado: PEDIDO. `inversiones` no edita `servicios/nucleo-financiero`. Lo de abajo es lo que el
consumidor (`LibroPorHttp`, puerto `LibroDelTitular`) ya invoca; hoy solo está probado contra el
consumidor (doble de tres niveles `LibroDoble` + `LibroPorHttpContratoTest`), NO contra el productor.

| Operación del puerto | Ruta que invoca el consumidor | Existe en el productor | Qué falta |
| --- | --- | --- | --- |
| `saldo(cuentaId)` | `GET /billetera/{id}/saldo` | Sí, pero sin `titularId` ni `comprometidoEnPozos` | Agregar ambos campos. Sin ellos el consumidor falla cerrado (no invierte «por las dudas»). |
| `retener` | `POST /billetera/retenciones` | Sí (contrato de retención) | Motivo `INVERSION_VOLUNTARIA` y `referenciaTipo = ORDEN_INVERSION` en el catálogo de motivos. |
| `liberar` | `POST /billetera/retenciones/{id}/cierre` con `{"desenlace":"LIBERADA"}` | Sí | Nada, salvo confirmar la idempotencia por `Idempotency-Key`. |
| `debitarInversion` | `POST /billetera/inversiones/debitos` | NO | Operación nueva: convierte la retención en un movimiento de débito hacia la custodia de inversión (sale del disponible y del retenido en un solo asiento balanceado). `cerrarRetencion(EJECUTADA)` NO alcanza: el saldo se deriva de `movimiento_billetera` menos retenciones vigentes, así que sin un movimiento el importe volvería a estar disponible. |
| `acreditarRescate` | `POST /billetera/inversiones/acreditaciones` | NO | Operación nueva: crédito desde la custodia de inversión al titular (rescate, vencimiento, interés), idempotente. |

Reglas que el productor debe cumplir (consumidor ya las asume):

1. `Idempotency-Key` es un UUID derivado de la clave de la instrucción; repetir devuelve la misma respuesta sin mover plata.
2. Saldo insuficiente = HTTP 422 con `codigo = AP-CU13-01`; cualquier otro no-2xx el consumidor lo trata como «no se sabe» y reintenta con la misma clave.
3. Partida doble: cada operación nueva genera asientos con suma cero y cuenta de custodia de inversión propia, separada del fondo de garantía.
4. Mientras el productor no exista, `inversiones` NO se puede activar contra un núcleo real; el adaptador de ese contrato queda sin prueba del lado productor.

Contrato nuevo para el carril A: definir estas dos rutas en su OpenAPI y publicar la prueba de contrato del lado productor.
