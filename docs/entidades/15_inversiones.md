# Módulo 15 — Inversiones

> **Pregunta de negocio que responde este módulo:**
> *¿Puede una persona poner su saldo libre en un depósito a plazo fijo o en un fondo de un
> aliado, saber exactamente qué aceptó, y recibir lo que le corresponde una sola vez, sin
> que la plataforma prometa una rentabilidad que no controla?*

Ficha escrita para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Cada
afirmación sale de los casos de uso CU-120 a CU-129, de las reglas `R-INV-01` a `R-INV-10` de
`docs/Restricciones.md` y de las notas de `15_inversiones.puml`. No agrega reglas.

> [!warning] Dos límites que valen para todo el módulo (CU-120)
> 1. **La inversión de dinero de grupos está DESACTIVADA.** Solo se invierte saldo propio y libre del
>    titular; un saldo afectado a un aporte o a un pozo nunca se invierte (CU-121).
> 2. **Todo parámetro comercial o fiscal es SINTÉTICO** (tasas, plazos, mínimos, costos, retención,
>    comisión de éxito) hasta que exista una decisión de negocio con fuente. Un dato sintético sale
>    marcado `origen_datos = SINTETICO` y `apto_produccion = false` y nunca se ofrece en producción
>    (`R-INV-01`).

La cadena del dinero es:

```
ProductoInversion → VersionCondiciones → ConsentimientoInversion → OrdenInversion
  → InstruccionLibro (retener) → PosicionInversion + ComprobanteInversion (suscripción)
  → DevengoDpf / ValorCuota → RescateInversion → ConciliacionInteres + DistribucionInteres
  + ComisionExito → ComprobanteInversion (liquidación) → InstruccionLibro (acreditar)
```

---

## Paquete: Catálogo y condiciones

### `ProductoInversion` / `producto_inversion`

**Qué es.** Un producto que ofrece un aliado: un depósito a plazo fijo (`DPF`) o un fondo (`FONDO`), con su emisor, su moneda y su nivel de riesgo.

**Para qué sirve (negocio).** Que la persona vea qué se le ofrece y de quién, sin ninguna promesa de rentabilidad (CU-120).

**Por qué debe existir.** El producto es estable; sus condiciones cambian con cada cotización. Separarlos permite actualizar las condiciones sin perder a qué producto se refería lo que alguien ya aceptó.

**A nivel de sistema.** `codigo` único; `tipo` ∈ DPF/FONDO decide qué condiciones son obligatorias (`R-INV-01`). `estado` ∈ ACTIVO/SUSPENDIDO. Lo sincroniza Operaciones desde el aliado (CU-120).

### `VersionCondiciones` / `version_condiciones`

**Qué es.** Cada cotización de las condiciones de un producto: plazo, tasa, mínimo, costos, retención, comisión de éxito y el texto que la persona acepta.

**Para qué sirve (negocio).** Que lo que la persona aceptó quede fijo aunque el aliado cambie las condiciones al día siguiente: la orden apunta a la versión exacta.

**Por qué debe existir.** Si las condiciones se pisaran, nadie podría demostrar qué tasa o qué penalización se le ofreció a quien invirtió el mes pasado.

**A nivel de sistema.** Append-only: cada cotización es una fila nueva, numerada por producto. `texto_hash` identifica el texto aceptado. `R-INV-01`: `SINTETICO` implica `apto_produccion = false`; un DPF exige plazo, base de días, tasa y retención, y un fondo exige días de rescate y hora de corte, sin plazo ni tasa fija. `fuente` y `fecha_cotizacion` dicen de cuándo es el dato.

### `ConsentimientoInversion` / `consentimiento_inversion`

**Qué es.** La aceptación, por una persona, de una versión de condiciones concreta.

**Para qué sirve (negocio).** Que la persona invierta exactamente bajo las condiciones que vio, y que eso se pueda demostrar (CU-121).

**Por qué debe existir.** Sin un consentimiento separado de la orden, una orden podría apuntar a condiciones que la persona nunca aceptó.

**A nivel de sistema.** Append-only. Guarda el `texto_hash` aceptado. Idempotente por usuario y `clave_idempotencia`; `hash_solicitud` detecta la misma clave con otro pedido. `R-INV-02`: la orden usa el consentimiento del mismo titular y de la misma versión.

## Paquete: Orden, posición y libro

### `OrdenInversion` / `orden_inversion`

**Qué es.** El pedido de invertir un importe del saldo libre en un producto, con sus estados hasta que el aliado confirma o rechaza.

**Para qué sirve (negocio).** Que el importe quede reservado antes de que nada salga hacia el aliado, y que la persona vea en qué paso está su inversión (CU-121, CU-122).

**Por qué debe existir.** Entre pedir y tener la posición pasan pasos que pueden cortarse. Sin la orden con estado, un corte dejaría plata retenida sin explicación o una posición sin débito.

**A nivel de sistema.** `estado` ∈ CREADA/RETENIDA/ENVIADA/INCIERTA/CONFIRMADA/RECHAZADA/CANCELADA. `INCIERTA` es el aliado que no contestó: se reconsulta, no se adivina. Una orden por consentimiento. `R-INV-02` lo comprueba un trigger. Idempotente por usuario y clave.

### `PosicionInversion` / `posicion_inversion`

**Qué es.** Lo que la persona tiene invertido en un producto: un depósito con su vencimiento o una cantidad de cuotas de un fondo.

**Para qué sirve (negocio).** Mostrarle a la persona qué tiene, desde cuándo y, en un fondo, a qué valor de cuota entró.

**Por qué debe existir.** La orden es el pedido; la posición es el resultado confirmado por el aliado. Sin separarlas, una orden rechazada parecería una inversión.

**A nivel de sistema.** Una por orden; `posicion_externa` única es la referencia del aliado. `R-INV-04`: un DPF tiene vencimiento y no tiene cuotas; un fondo tiene cuotas, valor de entrada y marca máxima. El saldo **no se guarda**: se deriva de los comprobantes (costo base creado por la suscripción menos el liberado por cada liquidación).

### `InstruccionLibro` / `instruccion_libro`

**Qué es.** La intención persistida de un efecto sobre la billetera del titular: retener, liberar, debitar o acreditar.

**Para qué sirve (negocio).** Que el dinero se mueva una sola vez aunque el sistema se corte en el medio: lo peor que puede pasar es «retenido y sin posición todavía», visible y reintentable (CU-122).

**Por qué debe existir.** El libro vive en `nucleo-financiero`. Sin una intención guardada antes de pedirle el movimiento, un corte haría perder el pedido o repetirlo.

**A nivel de sistema.** `R-INV-06`: única por `clave_idempotencia`, y no se da por `APLICADA` sin `referencia_libro`. `intentos` y `ultimo_error` los usa el reintento programado (`ReintentoDeInstrucciones`). Su origen es una orden o un rescate.

### `ComprobanteInversion` / `comprobante_inversion`

**Qué es.** El comprobante de una suscripción o de una liquidación, con el dinero descompuesto en principal, interés, impuesto, comisión y pérdida realizada.

**Para qué sirve (negocio).** Que la persona vea de dónde sale cada boliviano que recibe o que le descontaron, y que sirva para su reporte fiscal (CU-128).

**Por qué debe existir.** Sin la descomposición, el neto sería un número que nadie puede explicar ni reconciliar con los impuestos retenidos.

**A nivel de sistema.** Append-only; uno por origen. `R-INV-03`: `neto = principal + interes - impuesto - comision`, sin residuo. `costo_base = principal + perdida_realizada`: es lo que permite derivar el saldo de una posición sin guardarlo.

## Paquete: Valoración y rendimiento

### `ValorCuota` / `valor_cuota`

**Qué es.** El valor de la cuota de un fondo en una fecha, según el aliado.

**Para qué sirve (negocio).** Valorar las posiciones en fondos y calcular cuánto vale un rescate (CU-123).

**Por qué debe existir.** El valor de un fondo cambia todos los días. Sin la serie guardada, no se puede reconstruir a qué valor se rescató ni cobrar la comisión de éxito sobre la ganancia real.

**A nivel de sistema.** Append-only; uno por producto y fecha. Lleva `fuente`, `origen_datos`, cuándo lo publicó el aliado y cuándo se recibió.

### `DevengoDpf` / `devengo_dpf`

**Qué es.** El interés acumulado de un depósito a plazo fijo, día por día, calculado por la plataforma.

**Para qué sirve (negocio).** Mostrar cuánto lleva ganado un depósito y tener un número propio contra el cual controlar lo que paga el emisor (CU-123, CU-126).

**Por qué debe existir.** Si el único número fuera el que informa el aliado, no habría forma de detectar que pagó de menos.

**A nivel de sistema.** Append-only; uno por posición y fecha. `monto <= interes_acumulado`. Lo calcula el trabajo diario `DevengoDiario`.

### `ComisionExito` / `comision_exito`

**Qué es.** La comisión por resultado cobrada en el rescate de un fondo, con la marca máxima antes y después.

**Para qué sirve (negocio).** Cobrar solo sobre la ganancia que supera la marca máxima previa: una pérdida no se cobra, una recuperación sin pasar la marca no se cobra y el mismo tramo de ganancia no se cobra dos veces (CU-127).

**Por qué debe existir.** Sin la marca máxima registrada por rescate, la comisión podría cobrarse dos veces sobre la misma ganancia.

**A nivel de sistema.** Append-only; una por rescate. `R-INV-07`: la marca nueva nunca es menor que la previa, y sin base elegible la comisión es cero.

## Paquete: Rescate y conciliación

### `RescateInversion` / `rescate_inversion`

**Qué es.** El pedido de sacar dinero de una posición: parcial, total, al vencimiento o anticipado, con sus estados hasta liquidarse.

**Para qué sirve (negocio).** Que la persona recupere su dinero sabiendo en qué paso está, y que lo que pidió no se ofrezca dos veces (CU-124).

**Por qué debe existir.** El rescate pasa por el aliado y puede quedar incierto. Sin el registro, un reintento podría rescatar dos veces las mismas cuotas.

**A nivel de sistema.** `estado` ∈ SOLICITADO/PENDIENTE/INCIERTO/POR_ACREDITAR/LIQUIDADO/RECHAZADO. `R-INV-05`: las cuotas o el depósito comprometidos en un rescate en curso no se pueden volver a pedir (trigger `tg_rescate_inversion_doble_disponibilidad`). Idempotente por usuario y clave.

### `ConciliacionInteres` / `conciliacion_interes`

**Qué es.** La comparación, por rescate, entre el interés y la retención que calculó la plataforma y los que informó el aliado.

**Para qué sirve (negocio).** Que nada de lo que informa el aliado se acepte porque venga firmado: el interés se recalcula y se concilia (CU-125).

**Por qué debe existir.** Sin ella, una diferencia entre lo devengado y lo pagado pasaría sin que nadie la vea.

**A nivel de sistema.** Append-only; una por rescate. `R-INV-08`: o coincide (`CONCILIADA`) o queda como `DISCREPANCIA`.

### `DistribucionInteres` / `distribucion_interes`

**Qué es.** El reparto al titular del interés pagado por el emisor, por el período de su posición y con la retención del comprobante.

**Para qué sirve (negocio).** Que la persona reciba el interés que le corresponde, neto de retención, y que una diferencia con lo esperado quede a la vista (CU-126).

**Por qué debe existir.** Sin el reparto registrado, el interés acreditado no se podría explicar por período ni conciliar con el devengo propio.

**A nivel de sistema.** Append-only; una por rescate. `R-INV-09`: coincide con la posición, el período y la retención del comprobante, y se concilia contra el devengo propio; una discrepancia se registra con su motivo y no se corrige en silencio.

## Paquete: Operación

### `CorridaTrabajo` / `corrida_trabajo`

**Qué es.** Cada corrida de una operación de inversiones: sincronizar el catálogo o los valores, devengar o reintentar instrucciones, disparada por API o por el programador.

**Para qué sirve (negocio).** Que Operaciones vea cuándo corrió cada trabajo, con qué resultado y si se cortó, y que repetir el pedido no repita el efecto.

**Por qué debe existir.** Sin ella, un trabajo programado sería invisible y un reintento de la API volvería a sincronizar o a devengar.

**A nivel de sistema.** `R-INV-10`: única por operación y clave de idempotencia; nace `EN_CURSO` y solo termina con fecha de fin y, si falló, su error. Repetir la clave con el mismo contenido devuelve el resultado guardado; con otro contenido se rechaza.
