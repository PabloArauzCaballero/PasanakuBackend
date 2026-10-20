# Catálogo de datos

> Generado por `scripts/catalogo_de_datos.py --escribir`. No se edita a mano: la narrativa de negocio
> se corrige en `docs/entidades/*.md` y la estructura en los `.puml`.

## Esquema `aportes`

### `comprobante_manual`

**Qué es.** La foto del comprobante que sube el participante cuando pagó por fuera del circuito automático.

**Para qué sirve.** Cubre el caso real más frecuente de fricción: alguien transfirió desde su banco sin usar el QR, o pagó en efectivo, y ahora reclama que le acrediten. `estadoRevision`, `revisadoPor` y **`segundaRevisionPor`** implementan doble control: acreditar un pago que no existe es la forma más simple de robar en esta plataforma, y por eso un solo par de ojos no alcanza para montos relevantes. `hashArchivo` detecta el mismo comprobante subido dos veces —para dos aportes distintos—, que es el fraude más común y más fácil de intentar.

**Por qué existe.** Sin flujo de comprobante manual, la gente que paga por fuera queda marcada como morosa injustamente; con flujo manual sin doble revisión ni hash, el sistema se vuelve trivial de defraudar. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `pago`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `archivo_url` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `estado_revision` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `revisado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `segunda_revision_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo_rechazo` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `fecha_revision` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `conciliacion`

**Qué es.** El cruce entre un pago del sistema y un movimiento del banco.

**Para qué sirve.** **Es la entidad que hace verdadero el "pagado".** Mientras no exista una conciliación en estado `CONCILIADO_*`, el aporte no está realmente saldado por más que la pasarela haya dicho "aprobado". `metodo` guarda cómo se logró el cruce (referencia exacta, monto+fecha, manual). Es un indicador de calidad operativa: si el 40% de la conciliación es manual, la generación de referencias tiene un problema. `diferenciaMonto` captura los centavos y las comisiones no previstas. `forzarManual(usuarioId, justificacion)` existe porque siempre hay casos que el algoritmo no resuelve — pero exige justificación y deja autor, porque forzar una conciliación es afirmar que entró plata.

**Por qué existe.** Sin conciliación explícita, la única fuente de verdad es la pasarela, y las pasarelas se equivocan, reintentan y reversan. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `movimiento_bancario`, `pago`. La referencian `excepcion_conciliacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `movimiento_bancario_id` | UUID | — | UUID, opcional, referencia a `movimiento_bancario`. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `metodo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `diferencia_monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `conciliado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_conciliacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `constancia_pago`

**Qué es.** El comprobante que la plataforma emite, con código de verificación público.

**Para qué sirve.** Es lo que el participante muestra cuando alguien pone en duda que pagó. `codigoVerificacion` es público y verificable por cualquiera —incluido alguien que no tiene cuenta— contra el módulo 6. Eso convierte el comprobante en algo que no se puede falsificar con un editor de imágenes.

**Por qué existe.** En el pasanaku de cuaderno, la prueba de pago es la palabra del organizador. Acá es un documento verificable de forma independiente. Es una de las razones por las que alguien elegiría la plataforma sobre el cuaderno. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `pago`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `codigo_verificacion` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `hash_contenido` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `url_publica` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `url_pdf` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `fecha_generacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `disputa_pago`

**Qué es.** El reclamo formal sobre un pago: contracargo, desconocimiento del cargo, monto incorrecto.

**Para qué sirve.** Los contracargos son una realidad de cualquier sistema con tarjetas, y tienen **plazo legal de respuesta** (`fechaLimiteRespuesta`). Perder ese plazo significa perder la plata automáticamente, sin importar quién tenga razón. Esta entidad existe en buena medida para que ese reloj sea visible y accionable. `evidencias` acumula lo que se va a presentar: la constancia, el registro de escaneo del QR, la confirmación de recepción.

**Por qué existe.** Sin gestión de disputas, cada contracargo es una pérdida silenciosa y una potencial descuadratura de la bolsa de un grupo. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `pago`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `monto_disputado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `evidencias` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `abierta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_limite_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `enlace_pago_rapido`

**Qué es.** El enlace corto y firmado que lleva directo al pago de una obligación concreta.

**Para qué sirve.** Es la mitad técnica del **"pago en un toque"** (RF-16). El participante recibe por WhatsApp "Tu aporte vence mañana — [Pagar]", toca, y llega a su QR sin login, sin buscar el grupo, sin recordar cuánto era. Cada paso que se le quita a ese flujo se traduce directamente en tasa de pago puntual. `clicks` mide la conversión del recordatorio.

**Por qué existe.** Sin enlace firmado y de un solo uso, un enlace de pago por WhatsApp expondría el identificador de la obligación y permitiría que cualquiera viera —o pagara, o manipulara— la deuda de otro.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.token_verificacion`, `orden_cobro`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `orden_cobro_id` | UUID | — | UUID, obligatoria, referencia a `orden_cobro`. | NINGUNA |
| `token_id` | UUID | — | UUID, obligatoria, referencia a `token_verificacion`. | SECRETO |
| `url_corta` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `clicks` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `excepcion_conciliacion`

**Qué es.** Cada descuadre detectado, tipificado y asignado a alguien para resolver.

**Para qué sirve.** Los siete tipos son el catálogo de todo lo que sale mal con el dinero en la práctica: monto distinto, referencia ausente, pago duplicado, pago sin obligación, obligación sin pago, fuera de plazo, moneda distinta. Tipificarlos permite medir cuál predomina y atacar la causa. `asignadaA` convierte el descuadre en trabajo de alguien, con dueño. Sin asignación, las excepciones se acumulan y nadie las mira hasta que son cien.

**Por qué existe.** Y sobre todo: **toda excepción abierta bloquea el cierre diario.** Esa es la regla que impide que los descuadres se arrastren. Sin la entidad no hay qué bloquear. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `conciliacion`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `conciliacion_id` | UUID | — | UUID, obligatoria, referencia a `conciliacion`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_diferencia` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `asignada_a` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `resolucion` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `abierta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `extracto_bancario`

**Qué es.** El estado de cuenta de un período, importado desde el banco.

**Para qué sirve.** Es la **fuente de verdad externa**. Todo lo que el sistema afirma sobre el dinero se contrasta contra esto. `cuadrar()` verifica que saldo inicial + movimientos = saldo final: si no cuadra, el archivo está incompleto y conciliar contra él daría un falso "todo bien". `importadoPor` deja constancia de quién cargó el extracto, porque un extracto adulterado invalidaría toda la conciliación de ese día.

**Por qué existe.** Sin extracto no hay contra qué conciliar y la plataforma solo puede confiar en lo que la pasarela le informa. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `proveedor_pago`. La referencian `movimiento_bancario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_pago`. | NINGUNA |
| `cuenta` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `fecha_desde` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `saldo_inicial` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `saldo_final` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `archivo_url` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `importado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `importado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |

### `intento_pago`

**Qué es.** Cada vez que se trata de ejecutar el cobro de una orden, con su resultado.

**Para qué sirve.** Registra los fracasos, no solo los éxitos. Cuando un participante dice "intenté pagar tres veces y no me dejó", esta tabla lo confirma o lo desmiente, con el código de error y el mensaje que devolvió el proveedor. Es la diferencia entre creerle o no a alguien que pide que no le cobren mora. Operativamente permite `reintentarConRespaldo()`: el intento 1 falló con el proveedor A, el intento 2 va por el B, y **la trazabilidad del primero no se pierde**.

**Por qué existe.** Sin intentos materializados, un pago fallido no deja rastro y el soporte no puede diagnosticar nada. También es la base para medir la tasa de éxito por proveedor, que es lo que justifica cambiar de pasarela.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `orden_cobro`. La referencian `pago`. Reglas nombradas: `uq_intento_pago_idem`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `orden_cobro_id` | UUID | — | UUID, obligatoria, referencia a `orden_cobro`. | NINGUNA |
| `numero_intento` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `canal` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `iniciado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `finalizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `codigo_error` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `mensaje_proveedor` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |

### `movimiento_bancario`

**Qué es.** Cada línea del extracto: fecha, monto, glosa, referencia del banco.

**Para qué sirve.** Es lo que se cruza con los pagos. `buscarCoincidencias()` intenta la conciliación automática por referencia exacta y, si falla, por monto+fecha. La `glosa` va con índice full-text porque la conciliación real, cuando la referencia se pierde, se hace buscando el nombre o el número que el pagador escribió a mano.

**Por qué existe.** El caso que justifica esta tabla: **entró plata que nadie identificó.** Alguien transfirió sin referencia. Sin `MovimientoBancario` como entidad propia, ese dinero es invisible; con ella, queda como movimiento no conciliado, visible, con `conciliado = FALSE`, esperando que alguien lo reclame.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `extracto_bancario`. La referencian `conciliacion`, `nucleo_financiero.movimiento_custodia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `extracto_id` | UUID | — | UUID, obligatoria, referencia a `extracto_bancario`. | NINGUNA |
| `fecha_movimiento` | DATE | — | DATE, obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `glosa` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `referencia_banco` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `cuenta_origen` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `conciliado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `obligacion_aporte`

**Qué es.** Lo que un cupo debe, en un período, por un concepto. Es la deuda formal que la plataforma le reconoce a un participante.

**Para qué sirve.** Es **el eje de todo el sistema financiero**. La cubre el fondo de garantía (M8), la deduce la entrega (M4), la puntúa la reputación (M6) y la persigue la cobranza (M8). Si hubiera que elegir una sola tabla como corazón del modelo, es esta. Los cinco campos de monto no son redundancia, son la historia económica completa de esa obligación: - `montoEsperado`: lo que se pactó. **Nunca cambia.** - `montoPagado`: lo que efectivamente entró y se concilió. - `montoRecargo`: la mora acumulada. - `montoCondonado`: lo que el grupo decidió perdonar (por acuerdo, M2). - `montoCubiertoGarantia`: lo que puso el fondo para que el grupo no se detenga (M8). **Ojo: cubierto no es pagado.** El participante sigue debiendo esa plata, pero ahora se la debe al fondo, no al grupo. `saldoPendiente` es columna generada de los cinco anteriores: no se puede desincronizar porque no se escribe a mano. `tipo` distingue seis conceptos distintos que un solo "aporte" confundiría: el aporte del período, el recargo por mora, el aporte al fondo de garantía, la reposición de una cobertura consumida y el ajuste manual. Cada uno se cobra distinto, se contabiliza en cuenta distinta y tiene distinto tratamiento si el grupo se disuelve. Hay un sexto tipo, `COMISION_PLATAFORMA`, que solo se usa cuando el tarifario define que la comisión del servicio se prorratea entre los participantes en vez de deducirse de la entrega (M11). **Ninguno de los tipos representa un ingreso del organizador: administrar sigue sin cobrarse (RN-18).** `obligacionOrigenId` implementa una decisión de diseño importante: **un recargo por mora es OTRA obligación**, que apunta a la original. Así el `montoEsperado` del aporte base nunca se "ensucia". Si mañana se condona el recargo pero no el capital, se condona una fila y la otra queda intacta. Si se mezclaran, esa distinción sería imposible. `estado` con trece valores parece excesivo hasta que se enumeran los casos reales: programado (todavía no vence), pendiente, reportado por el usuario (dice que pagó, falta verificar), en verificación, pagado parcial, pagado, vencido, en mora, cubierto por garantía, exonerado, condonado, reprogramado, anulado. Cada uno implica una acción distinta del sistema y un mensaje distinto al usuario.

**Por qué existe.** Sin obligación explícita, "quién debe cuánto" sería un cálculo derivado del calendario, y no habría dónde registrar recargos, condonaciones, coberturas ni exoneraciones.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.cupo`, `grupos.grupo`, `grupos.participante`, `grupos.periodo`, `obligacion_aporte`, `plan_regularizacion`, `politica_mora`. La referencian `garantia.cobertura_incumplimiento`, `garantia.cobertura_respaldo_linea`, `garantia.registro_incumplimiento`, `nucleo_financiero.transferencia_p2p`, `obligacion_aporte`, `orden_cobro`, `pago`, `tarifas.cargo_comision`. Reglas nombradas: `uq_obligacion_periodo_cupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, obligatoria, referencia a `periodo`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `politica_mora_id` | UUID | — | UUID, opcional, referencia a `politica_mora`. | NINGUNA |
| `obligacion_origen_id` | UUID | — | UUID, opcional, referencia a `obligacion_aporte`. | NINGUNA |
| `plan_regularizacion_id` | UUID | — | UUID, opcional, referencia a `plan_regularizacion`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_esperado` | DECIMAL(14,2) | lo que se pactó. **Nunca cambia.** | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_pagado` | DECIMAL(14,2) | lo que efectivamente entró y se concilió. | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_recargo` | DECIMAL(14,2) | la mora acumulada. | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_condonado` | DECIMAL(14,2) | lo que el grupo decidió perdonar (por acuerdo, M2). | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cubierto_garantia` | DECIMAL(14,2) | lo que puso el fondo para que el grupo no se detenga (M8). **Ojo: cubierto no es pagado.** El participante sigue debiendo esa plata, pero ahora se la debe al fondo, no al grupo. | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `saldo_pendiente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria, calculada por la base, no se escribe. | FINANCIERO |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `fecha_vencimiento` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin_gracia` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_pago_efectivo` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `dias_mora` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `orden_cobro`

**Qué es.** La instrucción concreta de cobro para una obligación: monto exacto, proveedor, referencia única, vigencia.

**Para qué sirve.** Es la traducción de "debés Bs 500" a "acá está el QR para pagar esos Bs 500". Separarla de la obligación resuelve varios problemas prácticos: - Una obligación puede tener **varias órdenes**: la primera expiró, se generó otra; o la primera era por el banco A y falló, se generó otra por el banco B. - `referenciaUnica` es la **glosa conciliable**: el texto que viaja con la transferencia y que después permite cruzarla automáticamente con el extracto bancario. Sin ella, la conciliación es manual, que es exactamente lo que RF-15 quiere eliminar. - `permiteMontoAbierto` cubre el pago parcial: hay grupos que aceptan que alguien ponga la mitad ahora y la mitad después.

**Por qué existe.** Sin orden de cobro no hay forma de saber a qué obligación corresponde una transferencia que llegó al banco. Ese cruce es precisamente lo que automatiza el módulo.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `obligacion_aporte`, `proveedor_pago`. La referencian `enlace_pago_rapido`, `intento_pago`, `notificaciones.enlace_pago_notificado`, `qr_cobro`. Reglas nombradas: `uq_orden_cobro_idem`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, obligatoria, referencia a `obligacion_aporte`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_pago`. | NINGUNA |
| `monto_exacto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `permite_monto_abierto` | BOOLEAN | — | BOOLEAN, obligatoria. | FINANCIERO |
| `referencia_unica` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `emitida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |

### `pago`

**Qué es.** Plata que efectivamente se movió. El hecho económico.

**Para qué sirve.** Es el registro que respalda que alguien cumplió. Varios campos merecen explicación: - `montoComisionProveedor` y `montoNetoAcreditado`: **lo que el participante paga no es lo que llega a la bolsa.** La pasarela se queda con su comisión. Si esa diferencia no se modela explícitamente, la bolsa nunca cuadra y nadie entiende por qué faltan Bs 3,50. - `pagadorNombre` y `pagadorDocumento`: quién pagó realmente. Muchas veces paga el esposo, la madre o el jefe por el participante. Registrarlo es necesario para cumplimiento (detectar que una sola persona está pagando por seis participantes distintos, patrón de participantes ficticios) y para resolver reclamos. - `esManual` + `registradoPor`: distingue el pago que entró solo por webhook del que un operador cargó a mano contra un comprobante. Los segundos son intrínsecamente más riesgosos y deben poder auditarse aparte. - `canal = EFECTIVO_AL_ORGANIZADOR`: reconoce que en la realidad boliviana una parte del dinero se paga en efectivo. Negarlo no lo elimina, solo lo saca del sistema y lo vuelve invisible. Mejor registrarlo, marcarlo como manual y someterlo a doble revisión.

**Por qué existe.** Es el hecho económico. Sin él no hay contabilidad, ni constancia, ni conciliación, ni reputación basada en cumplimiento real.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `intento_pago`, `obligacion_aporte`, `proveedor_pago`. La referencian `comprobante_manual`, `conciliacion`, `constancia_pago`, `disputa_pago`, `garantia.abono_recuperacion`, `garantia.ejecucion_aval`, `garantia.recuperacion_respaldo`, `nucleo_financiero.orden_recarga`, `reembolso`, `webhook_pasarela`. Reglas nombradas: `uq_pago_idem`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, obligatoria, referencia a `obligacion_aporte`. | NINGUNA |
| `intento_pago_id` | UUID | — | UUID, opcional, referencia a `intento_pago`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, opcional, referencia a `proveedor_pago`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_comision_proveedor` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | FINANCIERO |
| `monto_neto_acreditado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `canal` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_hora_pago` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_hora_acreditacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `referencia_proveedor` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `pagador_nombre` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `pagador_documento` | VARCHAR(30) | — | VARCHAR(30), opcional. | PERSONAL |
| `cuenta_origen_enmascarada` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `registrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `es_manual` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |

### `plan_regularizacion`

**Qué es.** El acuerdo de pago en cuotas para alguien que se atrasó.

**Para qué sirve.** Es la salida para el participante que quiere pagar pero no puede de golpe. Sin esta figura, el sistema solo ofrece dos caminos —pagar todo o ser expulsado— y la experiencia de cobranza real dice que la mayoría de la gente sí quiere pagar, solo que no puede en el plazo original. `generarCuotas()` crea nuevas obligaciones de aporte que reemplazan a las originales. Eso es importante: el plan no es una anotación al margen, se materializa en obligaciones cobrables con sus propias fechas. `registrarIncumplimiento()` cierra el círculo: **si incumplís el plan de pago, eso también es un incumplimiento** (`INCUMPLIMIENTO_PLAN_REGULARIZACION` en M8), y normalmente más grave que el original, porque ya se te dio una oportunidad.

**Por qué existe.** Es lo que convierte una cartera morosa en una cartera recuperable. Sin plan, la única gestión posible es la presión, y la tasa de recuperación cae. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.participante`, `identidad.usuario`. La referencian `grupos.solicitud_retiro`, `obligacion_aporte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `monto_total` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `num_cuotas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `politica_mora`

**Qué es.** Las reglas de recargo por atraso: cuántos días de gracia, qué tipo de recargo, cuánto, con qué tope, y a partir de cuántos días se considera mora grave o incumplimiento.

**Para qué sirve.** El recargo por mora es un instrumento delicado: **si es muy bajo no cambia el comportamiento; si es muy alto ahoga al que ya está ahogado y garantiza que nunca pague.** Que sea configurable por grupo permite calibrarlo: un pasanaku familiar puede tener cero recargo y solo presión social; uno formal necesita que atrasarse cueste. `topeRecargo` es una protección al deudor y a la plataforma: sin tope, un recargo diario compuesto convierte una deuda de Bs 500 en una impagable en pocos meses, lo que es a la vez injusto y contraproducente (nadie paga una deuda que no puede alcanzar). `diasParaMoraGrave` y `diasParaIncumplimiento` son los umbrales que disparan el módulo 8: dejan de ser un juicio de valor y pasan a ser un parámetro explícito.

**Por qué existe.** Sin política parametrizable, la fórmula de mora está hardcodeada y todos los grupos comparten la misma tolerancia — lo que hace imposible atender segmentos distintos.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`. La referencian `grupos.configuracion_grupo`, `obligacion_aporte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `dias_gracia` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `tipo_recargo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `valor_recargo` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `tope_recargo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `dias_para_mora_grave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `dias_para_incumplimiento` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `aplica_automatico` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `proveedor_pago`

**Qué es.** Cada pasarela, banco o billetera con la que la plataforma opera.

**Para qué sirve.** Tres cosas que importan comercialmente: 1. **Multi-proveedor con respaldo.** `prioridad` y `activo` permiten que si el QR del banco A falla, el `IntentoPago` reintente con el proveedor B. En un país donde los servicios bancarios se caen con cierta regularidad, esto es la diferencia entre "no pude pagar" y "pagué". Y el día de vencimiento es justamente cuando más carga hay. 2. **Costo real por transacción.** `comisionFija` y `comisionPorcentual` permiten calcular `costoDe(monto)` y decidir por dónde enrutar. En aportes chicos —que son la mayoría— la comisión fija puede ser un porcentaje enorme del aporte. 3. **Capacidades declaradas.** `soportaWebhook` y `soportaConsultaEstado` cambian la estrategia: con webhook la acreditación es en segundos; sin él hay que hacer polling, con la latencia y el costo que eso implica. `referenciaCredenciales` es explícitamente **un puntero a la bóveda de secretos, nunca la llave**. Las credenciales de una pasarela de pagos no viven en la base de datos de la aplicación.

**Por qué existe.** Sin abstracción de proveedor, integrar un segundo banco significa reescribir el flujo de cobro. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `entregas.orden_desembolso`, `extracto_bancario`, `nucleo_financiero.orden_recarga`, `nucleo_financiero.orden_retiro`, `orden_cobro`, `pago`, `tarifas.costo_proveedor_operacion`, `webhook_pasarela`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `url_base` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `referencia_credenciales` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `comision_fija` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `comision_porcentual` | DECIMAL(5,3) | — | DECIMAL(5,3), obligatoria. | NINGUNA |
| `soporta_webhook` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `soporta_consulta_estado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `qr_cobro`

**Qué es.** El código QR interoperable propiamente dicho: el payload EMVCo, la imagen, el CRC, el banco emisor.

**Para qué sirve.** Es la interfaz de pago que el mercado boliviano efectivamente usa. El QR interoperable permite que alguien pague desde la app de **cualquier** banco, no solo del banco de la plataforma. Ese detalle define si el producto es usable o no: obligar a los participantes a abrir cuenta en un banco específico mataría la adopción. `escaneos` y `registrarEscaneo()` permiten medir el embudo real: cuánta gente ve el QR, cuánta lo escanea, cuánta termina pagando. Si hay muchos escaneos y pocos pagos, hay un problema en la app del banco, no en el recordatorio. `esReutilizable` distingue dos casos: el QR de un aporte concreto (uso único, monto fijo) y el QR permanente del grupo (reutilizable, monto abierto).

**Por qué existe.** Separado de la orden porque el payload EMV tiene su propio formato, su propio CRC validable y su propia representación gráfica, y porque un mismo cobro puede necesitar QR en un canal y enlace en otro.

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `orden_cobro`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `orden_cobro_id` | UUID | — | UUID, obligatoria, referencia a `orden_cobro`. | NINGUNA |
| `payload_emv` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `url_imagen` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `crc` | VARCHAR(8) | — | VARCHAR(8), obligatoria. | NINGUNA |
| `banco_emisor` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `cuenta_abono` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `es_reutilizable` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `escaneos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `reembolso`

**Qué es.** La devolución de un pago, total o parcial.

**Para qué sirve.** Cuatro motivos, todos reales: pago duplicado (la persona pagó dos veces), error de monto, grupo cancelado antes de arrancar, o resolución de disputa. Es la única manera legítima de sacar plata que ya entró. El flujo `solicitado → aprobado → ejecutado`, con `solicitadoPor` y `aprobadoPor` distintos, es control interno básico: **quien pide una devolución no puede ser quien la aprueba.**

**Por qué existe.** Sin reembolso modelado, "devolver plata" se haría con un ajuste manual sobre el saldo, sin rastro, sin aprobación y sin asiento contable. Es el hueco por donde se va la plata en sistemas mal diseñados. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `pago`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `referencia_proveedor` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `solicitado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_solicitud` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_ejecucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `webhook_pasarela`

**Qué es.** Cada notificación que la pasarela envía al sistema, guardada en crudo antes de procesarla.

**Para qué sirve.** Es el punto de entrada del dinero automático, y por eso es también el punto de mayor riesgo. Dos protecciones críticas: 1. **`claveIdempotencia` es `UNIQUE`.** Las pasarelas reenvían el mismo evento —lo hacen sistemáticamente, por diseño, cuando no reciben confirmación a tiempo. Sin idempotencia, **un reintento del proveedor acredita el aporte dos veces**. Ese es el bug clásico que descuadra la bolsa y hace que alguien cobre de más. 2. **`firmaValida` se verifica ANTES de confiar en el payload.** Un webhook es un endpoint público: cualquiera puede enviarle un JSON que diga "pago aprobado por Bs 5.000". La firma es lo único que distingue a la pasarela real de un atacante. `payloadCrudo` se guarda completo y sin interpretar. Cuando hay una disputa con el proveedor, lo que vale es lo que él mandó, no lo que el sistema entendió.

**Por qué existe.** Sin persistir los webhooks, un fallo de procesamiento pierde el evento para siempre y el pago nunca se acredita. Con la tabla, se reintenta (`intentosProcesamiento`) o se manda a cola muerta para revisión manual. ---

**Sistema.** Vive en el esquema `aportes`; la escribe y la lee solo el servicio `aportes` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `pago`, `proveedor_pago`. Reglas nombradas: `uq_webhook_idem`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_pago`. | NINGUNA |
| `evento` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `payload_crudo` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `firma` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `firma_valida` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `recibido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `procesado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `intentos_procesamiento` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(120) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(120), obligatoria. | NINGUNA |
| `error_procesamiento` | TEXT | — | TEXT, opcional. | NINGUNA |
| `pago_id` | UUID | — | UUID, opcional, referencia a `pago`. | NINGUNA |

## Esquema `auditoria`

### `alerta_cumplimiento`

**Qué es.** Cada detección concreta, con su evidencia y su análisis.

**Para qué sirve.** Es la unidad de trabajo del oficial de cumplimiento. El flujo `ABIERTA → EN_ANALISIS → DESCARTADA | ESCALADA | REPORTADA` con `conclusion` obligatoria hace que **cada alerta se cierre con una decisión fundamentada**. Eso importa por dos razones: la mayoría de las alertas son falsos positivos (y descartarlas sin fundamento sería negligencia), y ante una inspección hay que poder mostrar el criterio con que se descartaron. `detalleDeteccion` guarda por qué saltó: qué operaciones, qué montos, qué ventana. Las tipologías relevantes en un pasanaku digital, según la nota del modelo: fraccionamiento de aportes para evitar umbrales, grupos con participantes ficticios, **una misma cuenta destino para varios usuarios** (detectado en M4 con `hash_numero_cuenta`), y retiro inmediato tras cobrar el turno.

**Por qué existe.** Sin alertas gestionadas, la detección genera ruido que nadie procesa — que a efectos regulatorios es igual que no detectar. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `identidad.usuario`, `regla_cumplimiento`, `reporte_operacion_sospechosa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `regla_id` | UUID | — | UUID, obligatoria, referencia a `regla_cumplimiento`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `analista_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `reporte_sospechoso_id` | UUID | — | UUID, opcional, referencia a `reporte_operacion_sospechosa`. | NINGUNA |
| `operacion_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `operacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `monto_involucrado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `detalle_deteccion` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `conclusion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `detectada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `coincidencia_lista`

**Qué es.** Las listas de personas restringidas (PEP, OFAC, ONU, locales) y las coincidencias detectadas contra usuarios.

**Para qué sirve.** El chequeo contra listas es requisito normativo básico al dar de alta a un usuario y periódicamente después. `puntajeSimilitud` y el estado `FALSO_POSITIVO` reconocen el problema real de estas listas: **los nombres coinciden con frecuencia y la mayoría son falsos positivos.** "Juan Pérez" va a coincidir con alguien. Sin gestión de falsos positivos, o se bloquea a gente inocente o se ignoran todas las coincidencias — ambos extremos son inaceptables. `revisadaPor` deja constancia de quién decidió que era falso positivo, que es la decisión que un inspector revisará. `version` de la lista importa: hay que poder demostrar contra qué versión se verificó a un usuario en su fecha de alta.

**Por qué existe.** Es requisito regulatorio, y sin gestión de coincidencias el requisito se cumple mal en cualquiera de las dos direcciones. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `lista_restrictiva_externa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `lista_id` | UUID | — | UUID, obligatoria, referencia a `lista_restrictiva_externa`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `revisada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `nombre_coincidente` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `puntaje_similitud` | DECIMAL(5,4) | — | DECIMAL(5,4), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `revisada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `definicion_indicador`

**Qué es.** Qué mide cada indicador, cómo se calcula, de dónde sale el dato, quién responde por él y para qué lado se cumple su meta. Versionada y con vigencia.

**Para qué sirve.** Resuelve el problema que no es técnico: **que dos personas lleguen a una reunión con números distintos de la misma cosa**. Un indicador es una definición, no una consulta; si dos lugares lo recalculan, ya hay dos indicadores. - `sentido_meta` decide de qué lado se cumple. Sin él, «morosidad 7 % con meta 5 %» se leería como que cumple, porque 7 es mayor que 5. La mitad de los indicadores de riesgo y de cumplimiento son de los que **cuanto menos, mejor**, y el semáforo se pinta al revés de lo que sugiere la aritmética ingenua. - `dueno_familia` es quien escribe la explicación cuando su indicador está en rojo. Un indicador sin dueño es un número que nadie defiende y que, la tercera vez que aparece en rojo, deja de mirarse. - `minimo_casos` es el piso de muestra para publicar sin identificar personas. - `formula` y `fuente` son lo que se responde cuando alguien pregunta «¿y esto de dónde salió?» — que es la discusión que se lleva la reunión entera.

**Por qué existe.** Por la **reproducibilidad**. `indicador_kpi` apunta a la versión de definición con la que se calculó, y por eso un número de hace un año vuelve a salir igual. Cuando la fórmula cambia, se cierra la vigencia de una versión y se abre otra: la serie vieja conserva la suya, y **el corte se puede señalar en el gráfico** en vez de aparecer como una mejora del 40 % que nadie explica. Sin esta tabla la definición vive en el código, y entonces se pierde en cuanto alguien despliega: el número queda sin forma de auditarse hacia atrás. > La columna se llama `definicion_indicador_id` y no `definicion_id` a propósito: > `definicion_id` ya está tomado por `definicion_reporte`, en este mismo módulo, y la > clave foránea habría resuelto en silencio a la tabla equivocada. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `indicador_kpi`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `familia` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `dueno_familia` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `sentido_meta` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `formula` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `fuente` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `minimo_casos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |

### `definicion_reporte`

**Qué es.** El catálogo de reportes disponibles, con su consulta, parámetros, permiso requerido y si contiene datos sensibles.

**Para qué sirve.** Los diez tipos cubren a todos los públicos de la plataforma: el participante (estado de cuenta), el organizador (estado del grupo, desempeño), la operación (cartera en mora, conciliación diaria), la dirección (KPIs) y el regulador (operaciones sospechosas). `permisoRequerido` y `contieneDatosSensibles` son control de acceso: **el reporte de cartera en mora contiene nombres y montos de deudas de personas reales**, y no puede estar disponible para cualquiera con sesión iniciada. `estimarCosto()` protege la base de datos: un reporte sin filtros sobre tres años de pagos puede tumbar la producción en el peor momento.

**Por qué existe.** Sin catálogo, cada reporte es una consulta escrita a mano por alguien, sin control de acceso, sin caché y sin registro de quién la ejecutó. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `ejecucion_reporte`, `programacion_reporte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `consulta_base` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `parametros_esperados` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `columnas` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `permiso_requerido` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `contiene_datos_sensibles` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `cache_minutos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `ejecucion_reporte`

**Qué es.** Cada corrida de un reporte, con parámetros, resultado y quién la pidió.

**Para qué sirve.** Deja rastro de **quién extrajo qué información y cuándo**. `hashResultado` permite verificar después que un reporte presentado en una reunión o ante un tercero es el que realmente salió del sistema, y no una hoja de cálculo editada. `duracionMs` y `filasGeneradas` son las métricas que permiten detectar reportes que se degradan a medida que crece la base.

**Por qué existe.** Sin registro de ejecuciones, la extracción de datos es invisible. Y la extracción invisible es exactamente cómo salen los datos de una organización. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `definicion_reporte`, `grupos.grupo`, `identidad.usuario`. La referencian `exportacion_reporte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `definicion_id` | UUID | — | UUID, obligatoria, referencia a `definicion_reporte`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `solicitado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `parametros` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `filas_generadas` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `duracion_ms` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `hash_resultado` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `mensaje_error` | TEXT | — | TEXT, opcional. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `finalizada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `exportacion_reporte`

**Qué es.** El archivo generado a partir de una ejecución: PDF, XLSX, CSV o JSON.

**Para qué sirve.** El archivo es el objeto que efectivamente sale de la plataforma, y por eso tiene sus propios controles: - `estaCifrado`: si contiene datos sensibles, se cifra. - `expiraEn`: el enlace de descarga caduca. Un CSV con datos de 500 usuarios no puede quedar accesible indefinidamente en una URL. - `descargas` + `registrarDescarga()`: quién lo bajó y cuántas veces. - `revocarAcceso()`: cortar la descarga si se detecta que el enlace circuló.

**Por qué existe.** Es el punto donde los datos dejan el perímetro de la plataforma. Sin controles en ese punto, todo lo demás es teatro.

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `ejecucion_reporte`. Reglas nombradas: `ck_exportacion_version_llave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `ejecucion_id` | UUID | — | UUID, obligatoria, referencia a `ejecucion_reporte`. | NINGUNA |
| `formato` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `url_archivo` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `tamano_bytes` | BIGINT | — | BIGINT, obligatoria. | NINGUNA |
| `esta_cifrado` | BOOLEAN | si contiene datos sensibles, se cifra. | BOOLEAN, obligatoria. | NINGUNA |
| `version_llave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `descargas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | el enlace de descarga caduca. Un CSV con datos de 500 usuarios no puede quedar accesible indefinidamente en una URL. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `generada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `ficha_gobierno_tabla`

**Qué es.** La ficha de gobierno de cada tabla del modelo: si está en uso (estado), si alguien ya revisó su descripción (revisión), quién responde por ella y qué política de retención le aplica.

**Para qué sirve.** El catálogo de datos dice qué es cada tabla, pero no si sigue en uso ni quién la cuida. Con esta ficha el equipo de sistemas y auditoría ve de un vistazo qué tablas están activas, cuáles se proponen retirar y cuáles nadie revisó todavía, igual que en el portal interno de Atlas. Una tabla sin ficha se muestra **activa y detectada**: está en el modelo y nadie la revisó. La sensibilidad no se guarda acá: se deriva de las columnas del modelo, para no tener dos fuentes de verdad.

**Por qué existe.** Sin ella, retirar una tabla o confirmar que una tabla con datos personales tiene dueño y retención depende de la memoria de alguien. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `politica_retencion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `esquema` | VARCHAR(63) | — | VARCHAR(63), obligatoria. | NINGUNA |
| `tabla` | VARCHAR(63) | — | VARCHAR(63), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `estado_revision` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `responsable` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `politica_retencion_id` | UUID | — | UUID, opcional, referencia a `politica_retencion`. | NINGUNA |
| `critica_para_auditoria` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `nota_revision` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `revisado_por` | UUID | — | UUID, opcional. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `incidente_operativo`

**Qué es.** Un fallo del sistema con impacto en usuarios o en dinero.

**Para qué sirve.** `impactoUsuarios` e `impactoMonetario` son los campos que distinguen un incidente técnico de un incidente de negocio. Un servicio caído dos horas un domingo es una cosa; el mismo servicio caído el día de vencimiento de cuarenta grupos es otra completamente distinta, y hay que poder cuantificarla — entre otras cosas para decidir si corresponde condonar la mora de ese día. `causaRaiz` y `accionesCorrectivas` con post mortem convierten el incidente en aprendizaje en vez de en un susto que se repite.

**Por qué existe.** Sin registro de incidentes no se puede correlacionar un reclamo masivo de usuarios con la caída que lo causó, ni decidir con fundamento sobre las consecuencias (perdonar mora, reprogramar entregas, compensar). ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `cumplimiento.evento_riesgo_operativo`, `cumplimiento.incidente_seguridad`, `nucleo_financiero.descuadre_custodia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `titulo` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(6) | — | VARCHAR(6), obligatoria. | NINGUNA |
| `sistema_afectado` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `impacto_usuarios` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `impacto_monetario` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `detectado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `causa_raiz` | TEXT | — | TEXT, opcional. | NINGUNA |
| `acciones_correctivas` | TEXT | — | TEXT, opcional. | NINGUNA |

### `indicador_kpi`

**Qué es.** Los indicadores de negocio de la plataforma, por dimensión y período.

**Para qué sirve.** Los cuatro códigos de ejemplo son exactamente los que definen la salud del negocio: - `TASA_MOROSIDAD_GLOBAL`: **el indicador más importante.** Si sube sostenidamente, el modelo no funciona. - `GRUPOS_ACTIVOS` y `MONTO_ADMINISTRADO`: escala. - `TASA_CONCILIACION_AUTOMATICA`: eficiencia operativa. Si baja, hay más trabajo manual y más riesgo. `meta` y `variacionPeriodoAnterior` convierten el dato en gestión: no importa el valor absoluto, importa si va hacia donde tiene que ir. `dimension` (global / por grupo / por organizador) permite el mismo indicador a tres niveles, que es como se detecta que la morosidad global es aceptable pero está concentrada en los grupos de un organizador específico. `provisorio` acompaña al número y **no se deduce al consultarlo**. Un indicador calculado sobre un período cuyos cierres todavía no cuadraron es una opinión, y quien lo calculó es el único que sabe si cuadraron. Deducirlo al publicar obligaría a auditoría a mirar el esquema de `nucleo-financiero`, que no puede: por eso el dato viaja con la fila. `casos` es el tamaño de la muestra con la que se calculó. Sin él no se puede aplicar el mínimo de privacidad y el valor se publicaría identificando a las personas que lo componen: un promedio de tres personas identifica a las tres.

**Por qué existe.** Sin KPIs persistidos no hay serie temporal, y sin serie temporal no hay tendencia — solo fotos sueltas. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `definicion_indicador`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `definicion_indicador_id` | UUID | — | UUID, obligatoria, referencia a `definicion_indicador`. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `valor` | DECIMAL(16,4) | — | DECIMAL(16,4), obligatoria. | NINGUNA |
| `unidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `dimension` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `dimension_id` | UUID | — | UUID, opcional. | NINGUNA |
| `periodo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `meta` | DECIMAL(16,4) | — | DECIMAL(16,4), opcional. | NINGUNA |
| `variacion_periodo_anterior` | DECIMAL(8,4) | — | DECIMAL(8,4), opcional. | NINGUNA |
| `provisorio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `casos` | INTEGER | — | INTEGER, opcional. | NINGUNA |
| `calculado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `lista_restrictiva_externa`

**Qué es.** Las listas de personas restringidas (PEP, OFAC, ONU, locales) y las coincidencias detectadas contra usuarios.

**Para qué sirve.** El chequeo contra listas es requisito normativo básico al dar de alta a un usuario y periódicamente después. `puntajeSimilitud` y el estado `FALSO_POSITIVO` reconocen el problema real de estas listas: **los nombres coinciden con frecuencia y la mayoría son falsos positivos.** "Juan Pérez" va a coincidir con alguien. Sin gestión de falsos positivos, o se bloquea a gente inocente o se ignoran todas las coincidencias — ambos extremos son inaceptables. `revisadaPor` deja constancia de quién decidió que era falso positivo, que es la decisión que un inspector revisará. `version` de la lista importa: hay que poder demostrar contra qué versión se verificó a un usuario en su fecha de alta.

**Por qué existe.** Es requisito regulatorio, y sin gestión de coincidencias el requisito se cumple mal en cualquiera de las dos direcciones. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `coincidencia_lista`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `nombre_lista` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fecha_actualizacion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `registros` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `politica_retencion`

**Qué es.** Cuánto tiempo se conserva cada tipo de dato y qué se hace al vencer: archivar, anonimizar o eliminar.

**Para qué sirve.** Concilia dos obligaciones que se contradicen: **conservar** (la normativa financiera exige guardar registros contables por años) y **borrar** (la normativa de datos personales exige no guardar más de lo necesario). `baseLegal` documenta bajo qué norma se retiene cada cosa. Cuando un usuario ejerce su derecho de cancelación y no se le puede borrar todo, este campo es la justificación. `ARCHIVAR | ANONIMIZAR | ELIMINAR` son tres respuestas distintas: los asientos contables se archivan (se necesitan íntegros), los logs de acceso se anonimizan (sirve el patrón, no la identidad), los borradores se eliminan.

**Por qué existe.** Sin política de retención, o se guarda todo para siempre (riesgo regulatorio y costo creciente) o se borra sin criterio (y se pierde evidencia que la ley obliga a tener). ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `ficha_gobierno_tabla`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entidad` | VARCHAR(50) | — | VARCHAR(50), obligatoria. | NINGUNA |
| `meses_retencion_activa` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `meses_retencion_historica` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `accion_al_vencer` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `base_legal` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |

### `proceso_anonimizacion`

**Qué es.** La ejecución técnica del borrado o seudonimización, con lo que se afecta y lo que se retiene por ley.

**Para qué sirve.** Borrar a un usuario de un sistema financiero no es un `DELETE`. Sus pagos aparecen en asientos contables que deben conservarse; sus aportes son parte de la historia de un grupo con otras once personas cuyos derechos también hay que respetar. `SEUDONIMIZACION` es habitualmente la respuesta correcta: **se rompe el vínculo con la persona pero se conserva la integridad de los registros.** El asiento contable sigue cuadrando; ya no dice quién. `datosRetenidosPorLey` documenta explícitamente qué no se borró y con qué fundamento. Es lo que se le entrega al usuario como respuesta y lo que se le muestra a un inspector. `simular()` antes de ejecutar es indispensable: una anonimización mal ejecutada es irreversible y puede romper la contabilidad de un grupo entero.

**Por qué existe.** Sin proceso formal, el borrado es un script ad-hoc que alguien corre una vez, sin registro de qué se afectó ni posibilidad de explicarlo después. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `solicitud_datos_personales`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `solicitud_id` | UUID | — | UUID, opcional, referencia a `solicitud_datos_personales`. | NINGUNA |
| `estrategia` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `entidades_afectadas` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `datos_retenidos_por_ley` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ejecutado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `programacion_reporte`

**Qué es.** Reportes que se generan y envían solos según un cron.

**Para qué sirve.** Los reportes que hay que pedir no se piden. La conciliación diaria, el resumen semanal del organizador, el estado mensual del participante: automatizarlos es lo que hace que la información llegue a quien tiene que actuar sobre ella, y no que se descubra un problema en el cierre trimestral.

**Por qué existe.** Sin programación, los controles periódicos dependen de la disciplina de una persona. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `definicion_reporte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `definicion_id` | UUID | — | UUID, obligatoria, referencia a `definicion_reporte`. | NINGUNA |
| `expresion_cron` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `parametros_fijos` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `destinatarios` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `canal_entrega` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `formato` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ultima_ejecucion_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `proxima_ejecucion_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `regla_cumplimiento`

**Qué es.** Las reglas de detección de operaciones sospechosas, con su umbral, ventana y acción automática.

**Para qué sirve.** Las cinco categorías corresponden a tipologías reales: - `UMBRAL_MONTO`: operaciones sobre el límite reportable. - `FRACCIONAMIENTO`: **la más importante.** Alguien divide un monto grande en varios chicos para no gatillar el umbral. Se detecta con `ventanaHoras` + suma acumulada, nunca operación por operación. - `VELOCIDAD`: mucho movimiento en poco tiempo. - `RED_SOSPECHOSA`: varias cuentas conectadas entre sí (misma cuenta bancaria destino, mismo dispositivo, mismo pagador). - `LISTA_RESTRICTIVA`: coincidencia con listas de PEP, OFAC, ONU. `accionAutomatica` gradúa la respuesta: `ALERTAR` (revisar después), `RETENER_OPERACION` (frenar hasta revisar), `BLOQUEAR_USUARIO` (casos graves). Retener una operación tiene costo para un usuario legítimo, así que la proporcionalidad importa.

**Por qué existe.** Con reglas hardcodeadas no se pueden ajustar umbrales cuando cambia la normativa ni agregar tipologías nuevas cuando aparece un patrón de abuso. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `alerta_cumplimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `categoria` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `expresion` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `umbral` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | NINGUNA |
| `ventana_horas` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `accion_automatica` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `reporte_operacion_sospechosa`

**Qué es.** El reporte formal a la autoridad competente (UIF), consolidando varias alertas.

**Para qué sirve.** Es una **obligación legal**, no una opción. Y la consolidación importa: no se reporta cada alerta por separado, se reporta un caso —un usuario, un período, una tipología, una narrativa que explica el patrón—. `narrativa` es el corazón del reporte: hay que **contar la historia** de por qué esas operaciones son sospechosas. Los datos crudos no constituyen un reporte. `aprobadoPor` (oficial de cumplimiento) y `numeroRadicado` cierran el circuito formal.

**Por qué existe.** No reportar cuando corresponde tiene consecuencias legales directas para la plataforma y sus responsables. Y el reporte tiene que quedar registrado, con fecha y radicado, para poder probar que se cumplió. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `alerta_cumplimiento`, `cumplimiento.caso_investigacion_lft`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipologia` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `periodo_analizado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `narrativa` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `numero_radicado` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `enviado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `revision_columna`

**Qué es.** El veredicto de una persona sobre lo que el modelo dice de una columna: aprobada, rechazada o pendiente, con qué confianza y por qué.

**Para qué sirve.** El catálogo describe cada columna desde el modelo (tipo, sensibilidad, nota de negocio). Esa descripción la escribió alguien una vez; que otra persona la revise y lo deje dicho es lo que convierte una ficha en un dato en el que se puede confiar, igual que en el catálogo del portal interno de Atlas. Un rechazo sin motivo no sirve a quien tiene que corregir: por eso el motivo es obligatorio al rechazar.

**Por qué existe.** Sin ella, «esta columna ya se revisó» vive en la cabeza de quien la revisó, y una columna con datos personales mal clasificada pasa años sin que nadie lo note. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `esquema` | VARCHAR(63) | — | VARCHAR(63), obligatoria. | NINGUNA |
| `tabla` | VARCHAR(63) | — | VARCHAR(63), obligatoria. | NINGUNA |
| `columna` | VARCHAR(63) | — | VARCHAR(63), obligatoria. | NINGUNA |
| `decision` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `confianza` | VARCHAR(10) | — | VARCHAR(10), opcional. | NINGUNA |
| `motivo` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `revisado_por` | UUID | — | UUID, obligatoria. | NINGUNA |
| `revisado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `solicitud_datos_personales`

**Qué es.** El ejercicio de los derechos ARCO: acceso, rectificación, cancelación, oposición, portabilidad.

**Para qué sirve.** Es un derecho del usuario y una obligación de la plataforma, **con plazo legal** (`fechaLimiteLegal`). Incumplir el plazo es sancionable con independencia de si después se atiende. El método `rechazarPorObligacionesVigentes(motivo)` es la pieza de negocio más delicada y merece explicación: **el derecho de cancelación no borra registros con obligación legal o financiera vigente.** Si el usuario tiene una deuda abierta en M8 o hay asientos contables suyos en M3, esos datos se conservan y se documenta la base legal. Es exactamente la situación que hay que resolver bien: alguien que debe plata pide que borren sus datos para desaparecer. La respuesta no puede ser "no" a secas ni "sí" sin más: es una atención parcial, fundamentada, con lo que se conserva y por qué.

**Por qué existe.** Sin gestión formal, las solicitudes llegan por correo, se pierden y se incumplen los plazos. ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `proceso_anonimizacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `atendida_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_limite_legal` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `respuesta` | TEXT | — | TEXT, opcional. | NINGUNA |
| `recibida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `atendida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `ticket_soporte`

**Qué es.** El canal formal de atención al usuario.

**Para qué sirve.** En una plataforma de dinero, el soporte no es opcional: **cada problema no resuelto es plata que alguien siente que perdió.** `referenciaEntidad` + `referenciaId` conectan el ticket con el objeto del problema (la entrega que no llegó, el pago que no se acreditó), lo que le da al agente el contexto sin tener que pedírselo al usuario. `slaHoras` establece el compromiso de tiempo. Un reclamo por una entrega no recibida no puede tener el mismo SLA que una consulta sobre cómo cambiar el avatar.

**Por qué existe.** Y hay una razón adicional: `ticketSoporteId` es la justificación que exige `RegistroAccesoDatos` para consultar datos sensibles. **Sin tickets, no hay forma de validar que un acceso a datos personales tenía una razón legítima.** ---

**Sistema.** Vive en el esquema `auditoria`; la escribe y la lee solo el servicio `auditoria` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `cumplimiento.reclamo_cliente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `asignado_a` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `categoria` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `asunto` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `prioridad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `referencia_entidad` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `referencia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `sla_horas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `abierto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

## Esquema `catalogo`

### `dia_no_habil`

**Qué es.** El calendario de feriados nacionales, departamentales o propios del grupo.

**Para qué sirve.** Si el día de cobro cae feriado, el banco no procesa y la transferencia no se acredita. Cobrar mora a alguien porque el 6 de agosto no había atención bancaria es injusto y genera reclamos legítimos. `siguienteHabil()` corre las fechas automáticamente. El alcance departamental importa en Bolivia: cada departamento tiene su efeméride, y un grupo con gente de La Paz y Santa Cruz no comparte todos los feriados.

**Por qué existe.** Es la diferencia entre un sistema que entiende el contexto y uno que castiga a la gente por el calendario. ---

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; se carga con las semillas. Depende de `grupos.grupo`. Reglas nombradas: `ck_dia_no_habil_ambito`, `uq_dia_no_habil`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `alcance` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |

### `impuesto`

**Qué es.** Las alícuotas vigentes y su aplicación a cada devengo.

**Para qué sirve.** Que el impuesto sea una fila con `vigente_desde` y `base_legal`, y que su cálculo quede guardado por devengo. Cuando cambia una alícuota, el histórico conserva la que se aplicó, que es lo que exige cualquier revisión tributaria. `precio_incluye_impuesto` en el concepto decide si el precio publicado es final o si el impuesto se suma encima. Para el consumidor final, el precio publicado tiene que ser el final: la comisión de Bs 18 es Bs 18, impuestos incluidos. ---

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `tarifas` la escribe. Depende de `nucleo_financiero.cuenta_contable`. La referencian `tarifas.calculo_impuesto`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_contable_id` | UUID | — | UUID, opcional, referencia a `cuenta_contable`. | NINGUNA |
| `codigo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `alicuota` | DECIMAL(6,4) | — | DECIMAL(6,4), obligatoria. | NINGUNA |
| `tipo_calculo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `base_legal` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |

### `licencia_regulatoria`

**Qué es.** El estado de habilitación de la empresa ante el regulador.

**Para qué sirve.** Operar sin licencia es el riesgo número uno de este producto, por encima de cualquier bug. El régimen vigente para empresas de tecnología financiera exige primero un **certificado de adecuación** y después una **licencia de funcionamiento**, con categorías de actividad definidas —pagos y plataformas de pago es la que corresponde a este producto— y con plazo de adecuación para quien ya está operando. `alcance_autorizado` en JSON es la parte accionable: **la aplicación consulta esta tabla antes de habilitar un servicio**, en vez de confiar en que alguien recuerde qué quedó autorizado. Lanzar una función fuera del alcance deja de ser posible por descuido. ---

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `cumplimiento` la escribe. Depende de `identidad.usuario`. La referencian `cumplimiento.entorno_prueba_regulado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `organismo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `categoria_actividad` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `numero_resolucion` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_solicitud` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_otorgamiento` | DATE | — | DATE, opcional. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `alcance_autorizado` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `garantia_seriedad` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | NINGUNA |
| `documento_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `limite_operativo_billetera`

**Qué es.** Los techos de operación por nivel de debida diligencia y ventana de tiempo.

**Para qué sirve.** Es la traducción operativa del principio "a menos conocimiento del cliente, menos capacidad de operación". Una persona verificada solo con celular puede tener saldo bajo y mover poco; una con documento validado y domicilio verificado puede más. Eso es lo que permite ofrecer apertura de cuenta en dos minutos **sin** que la plataforma se convierta en un canal cómodo para mover plata anónima.

**Por qué existe.** Porque los techos cambian por norma y por decisión de riesgo, y cambiar un techo no puede requerir un despliegue. `base_normativa` guarda la referencia al artículo que obliga cada techo: cuando el supervisor pregunta "¿de dónde sacaron este límite?", la respuesta está en la fila.

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `cumplimiento` la escribe. La referencian `nucleo_financiero.consumo_limite`. Reglas nombradas: `ex_limite_vigencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `concepto` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `nivel_debida_diligencia` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ventana` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `monto_maximo` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `cantidad_maxima` | INTEGER | — | INTEGER, opcional. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `base_normativa` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `tarifario`

**Qué es.** Una versión completa y publicada de la política de precios.

**Para qué sirve.** Tres cosas a la vez: 1. **Congelar el precio histórico.** Cada devengo apunta al tarifario que rigió. 2. **Publicar.** `url_publicacion` y `hash_documento` guardan el documento que se puso a disposición del público. La transparencia de tarifas no es opcional para un proveedor de servicios de pago: es exigible y verificable. 3. **Ordenar el cambio.** Un tarifario nuevo no entra de golpe: pasa por `EN_PREAVISO` durante `dias_preaviso` antes de volverse `VIGENTE`.

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `tarifas` la escribe. Depende de `identidad.usuario`, `tarifario`. La referencian `tarifario`, `tarifas.asignacion_tarifario`, `tarifas.cambio_tarifario`, `tarifas.concepto_tarifa`, `tarifas.cotizacion_comision`, `tarifas.devengo_comision`, `tarifas.simulacion_tarifa`, `tarifas.tarifa_congelada_grupo`. Reglas nombradas: `ex_tarifario_vigente`, `ck_tarifario_publicado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `moneda_base` | CHAR(3) | — | CHAR(3), obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `dias_preaviso` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `publicado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `url_publicacion` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `tarifario_anterior_id` | UUID | — | UUID, opcional, referencia a `tarifario`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `acta_aprobacion` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |

### `tipo_cambio`

**Qué es.** El tipo de cambio por moneda y fecha, con su fuente.

**Para qué sirve.** Los umbrales de reporte están expresados en dólares y las operaciones ocurren en bolivianos. Sin el tipo de cambio del día guardado, la pregunta "¿esta operación superaba el umbral?" no tiene respuesta reproducible dos años después: cada recálculo daría un número distinto.

**Por qué existe.** Porque el registro de la operación relevante ([[registro_operacion_relevante]], M12) **copia** el `tipo_cambio_aplicado` al momento del hecho. Esta tabla es de dónde sale ese número, y `fuente` dice si vino del banco central, de un proveedor o de una carga manual.

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; se carga con las semillas.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `moneda_origen` | CHAR(3) | — | CHAR(3), obligatoria. | NINGUNA |
| `moneda_destino` | CHAR(3) | — | CHAR(3), obligatoria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `tipo_cambio` | DECIMAL(12,6) | — | DECIMAL(12,6), obligatoria. | NINGUNA |
| `fuente` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `cargado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `umbral_operativo`

**Qué es.** Cuánto puede mover un usuario según su nivel de KYC.

**Para qué sirve.** Implementa el principio de **KYC escalonado**, que es lo que permite tener a la vez baja fricción y cumplimiento: - Sin verificación: puede participar en grupos chicos. - KYC básico: montos medianos. - KYC avanzado: sin restricción práctica. Es lo que evita el falso dilema entre "pedir cédula a todos" (que expulsa a la mayoría del público objetivo en el registro) y "no pedir nada" (que es incumplimiento normativo). La persona escala su verificación cuando quiere operar más. `ACUMULADO_MENSUAL` como concepto es importante: el control no puede ser solo por operación individual, porque entonces el fraccionamiento lo esquiva trivialmente.

**Por qué existe.** Sin umbrales por nivel de KYC, la única política posible es la misma exigencia para todos — y cualquiera de las dos opciones mata el producto o lo pone fuera de la ley. ---

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `cumplimiento` la escribe.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `concepto` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nivel_kyc_requerido` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `monto_maximo` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |

### `umbral_reporte_uif`

**Qué es.** Cada inciso del instructivo de la unidad de inteligencia financiera, convertido en una fila.

**Para qué sirve.** El régimen de reporte por umbral no es un número: es una tabla de casos, cada uno con su formulario, su concepto de operación, si es por operación individual o acumulada, su monto en dólares, su ventana en días calendario y si exige declarar origen y destino de los fondos. Modelarlo como dato tiene tres consecuencias prácticas: 1. **Cuando la autoridad cambia un umbral, es un `INSERT` con vigencia.** Y los cambios ocurren: el régimen vigente incorporó un umbral específico para billeteras móviles —carga y retiro acumulados desde USD 1.000 en ventanas de 1 a 3 días calendario— que antes no existía. 2. **Se puede demostrar qué umbral regía el día de una operación**, porque la fila vieja conserva su vigencia en lugar de ser sobrescrita. 3. `base_normativa` guarda el artículo e inciso exactos, que es lo que se cita en una inspección. ---

**Sistema.** Vive en el esquema `catalogo`: todos los servicios la consultan; solo `cumplimiento` la escribe. La referencian `cumplimiento.registro_operacion_relevante`. Reglas nombradas: `ck_umbral_base_normativa`, `ck_umbral_ventana`, `ex_umbral_vigencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `formulario` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `inciso` | VARCHAR(4) | — | VARCHAR(4), obligatoria. | NINGUNA |
| `concepto_operacion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `es_acumulado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `umbral_usd` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `ventana_dias_calendario` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `exige_declaracion_origen_destino` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `reinicia_tras_superar` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `base_normativa` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

## Esquema `comun`

### `bitacora_evento`

**Qué es.** El registro insert-only de cada cambio sobre cualquier dato del sistema, con valor anterior, valor nuevo y autor.

**Para qué sirve.** Implementa RN-17 y responde la pregunta que aparece en cada conflicto serio: ***¿quién cambió esto y cuándo?*** Casos concretos que solo esta tabla resuelve: - El monto del aporte del grupo cambió de Bs 500 a Bs 600 y nadie sabe quién lo cambió. - Una obligación aparece como exonerada y el participante jura que no pidió nada. - La cuenta bancaria de un beneficiario se modificó tres días antes de su turno. Dos campos merecen atención especial: **`hashAnterior` — encadenamiento.** Cada registro incluye el hash del anterior. Alterar un registro viejo rompe la cadena y `verificarCadena()` lo detecta. Esto convierte la bitácora de "un log que un administrador podría editar" a "un libro que, si se toca, se nota". Sin encadenamiento, la auditoría solo protege contra errores, no contra alguien con acceso a la base de datos — que es precisamente contra quien hay que protegerse. **`suplantandoAUsuarioId` — actuación de soporte.** Cuando un agente de soporte opera "como" un usuario para resolverle un problema, este campo deja constancia. **Sin él, nadie distingue lo que hizo el titular de lo que hizo un empleado.** Es la diferencia entre poder afirmar "el usuario autorizó esa entrega" y no poder afirmarlo. `reconstruirEstadoA(entidad, id, fecha)` permite responder "¿cómo estaba esto el 15 de marzo?", que es lo que se necesita cuando el reclamo llega meses después.

**Por qué existe.** Sin bitácora, cualquier disputa se resuelve por la palabra de quien tiene acceso al sistema. Con bitácora encadenada, se resuelve con evidencia verificable.

**Sistema.** Vive en el esquema `comun`: todo servicio INSERTA su rastro y ninguno lee ni edita el de otro; el SELECT es solo de `rol_auditor` (excepción enumerada en `COMPARTIDAS_ESCRITURA`). Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `grupos.grupo`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `secuencia` | BIGSERIAL | — | BIGSERIAL, obligatoria. | NINGUNA |
| `entidad` | VARCHAR(50) | — | VARCHAR(50), obligatoria. | NINGUNA |
| `entidad_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `accion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `actor_usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `actor_rol` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `suplantando_a_usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `origen` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, opcional. | NINGUNA |
| `agente_usuario` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `correlation_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `request_id` | UUID | — | UUID, opcional. | NINGUNA |
| `valor_anterior` | JSONB | — | JSONB, opcional. | NINGUNA |
| `valor_nuevo` | JSONB | — | JSONB, opcional. | NINGUNA |
| `campos_modificados` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `hash_registro` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `hash_anterior` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `registro_acceso_datos`

**Qué es.** El registro de quién **consultó** datos sensibles de quién, con qué justificación.

**Para qué sirve.** **Es el registro que se pide cuando hay una filtración**, y el que casi nunca existe. El escenario: se filtran los documentos de identidad de 200 usuarios. La bitácora de escritura no sirve — nadie los modificó, alguien los *miró*. Sin esta tabla, no hay forma de saber si fue un empleado, cuál, ni cuántos registros consultó. `justificacion` + `ticketSoporteId` imponen una disciplina saludable: **para ver la cédula o la cuenta bancaria de alguien hay que decir por qué**, y ese porqué debe estar atado a un caso real. La sola existencia de ese requisito reduce drásticamente las consultas por curiosidad. `esAccesoInusual()` detecta el patrón peligroso: un consultor que en una hora accede a 300 registros de usuarios sin relación entre sí no está resolviendo un ticket, está extrayendo una base de datos.

**Por qué existe.** Es requisito de cualquier normativa seria de protección de datos, y es la única defensa —y la única prueba— ante un incidente de privacidad.

**Sistema.** Vive en el esquema `comun`: todo servicio INSERTA su rastro y ninguno lee ni edita el de otro; el SELECT es solo de `rol_auditor` (excepción enumerada en `COMPARTIDAS_ESCRITURA`). Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`. Reglas nombradas: `ck_acceso_justificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_consultor_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `usuario_afectado_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo_dato` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `operacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `justificacion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `ticket_soporte_id` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `cantidad_registros` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `cumplimiento`

### `aceptacion_contrato`

**Qué es.** El contrato modelo y la prueba de que cada usuario lo aceptó.

**Para qué sirve.** Los contratos de adhesión de servicios financieros tienen forma reglada y suelen requerir registro previo ante el supervisor. El modelo guarda versión, hash, número de registro y vigencia. La aceptación guarda IP, dispositivo, token de firma y hash de evidencia: eso es lo que convierte un clic en una aceptación oponible. Sin esa fila, el consentimiento del usuario es una afirmación de la empresa. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `contrato_adhesion`, `identidad.dispositivo`, `identidad.token_verificacion`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `contrato_adhesion_id` | UUID | — | UUID, obligatoria, referencia a `contrato_adhesion`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `dispositivo_id` | UUID | — | UUID, opcional, referencia a `dispositivo`. | NINGUNA |
| `token_firma_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | SECRETO |
| `version_aceptada` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `ip` | INET | — | INET, opcional. | NINGUNA |
| `hash_evidencia` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `aceptado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `acta_comite`

**Qué es.** El aparato de gobierno: comités, actas, manuales y designaciones.

**Para qué sirve.** Esto no es burocracia decorativa: en la práctica supervisora, **un manual sin acta de aprobación se considera inexistente**, y la ausencia de manuales de procedimientos es una falta administrativa sancionable. `acta_comite` guarda asistentes, quórum, temas y decisiones con hash del documento; `politica_interna` versiona cada manual con su fecha de próxima revisión; `designacion_regulatoria` unifica quién es oficial de cumplimiento, responsable de seguridad de la información, responsable de riesgos, auditor interno y responsable del punto de reclamo, con acta y fecha de comunicación al organismo. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `comite_gobierno`, `identidad.usuario`. La referencian `designacion_regulatoria`, `politica_interna`, `prueba_continuidad`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `comite_gobierno_id` | UUID | — | UUID, obligatoria, referencia a `comite_gobierno`. | NINGUNA |
| `elaborada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `numero` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `asistentes` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `cumple_quorum` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `temas_tratados` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `decisiones` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `url_documento` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |

### `activo_informacion`

**Qué es.** El inventario clasificado de activos y los incidentes que los afectan.

**Para qué sirve.** El inventario con propietario, custodio y clasificación es el punto de partida de cualquier sistema de gestión de seguridad de la información, y también lo que permite responder "¿dónde están los datos personales?" sin adivinar: `contiene_datos_personales` es una columna consultable. En el incidente, `plazo_reporte` se calcula al detectar y **se guarda**: tres relojes distintos corren en paralelo —contención, reporte al organismo y notificación a los titulares— y hay que poder demostrar cuál regía ese día. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `contrato_tercero`, `identidad.usuario`. La referencian `incidente_seguridad`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `propietario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `custodio_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `contrato_tercero_id` | UUID | — | UUID, opcional, referencia a `contrato_tercero`. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `clasificacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `contiene_datos_personales` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `contiene_datos_sensibles` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `criticidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `ubicacion` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `exige_cifrado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ultima_revision` | DATE | — | DATE, obligatoria. | NINGUNA |

### `alerta_monitoreo_lft`

**Qué es.** La detección y la investigación.

**Para qué sirve.** Están separadas a propósito: una alerta es automática y barata; un caso es humano y caro. Varias alertas del mismo cliente se agrupan en un caso, el analista investiga y **decide**: descartar, mantener bajo monitoreo reforzado, reportar a la autoridad o terminar la relación. Lo importante es que **descartar también deja rastro**: `conclusion` es obligatoria. Un sistema donde las alertas se cierran sin justificación es peor que uno sin alertas, porque documenta que se vio y no se hizo nada. `plazo_limite` en el caso obliga a que la investigación tenga fecha de vencimiento.

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `caso_investigacion_lft`, `identidad.usuario`, `nucleo_financiero.cuenta_billetera`, `nucleo_financiero.transaccion_billetera`, `regla_monitoreo_lft`. La referencian `desvio_perfil`. Reglas nombradas: `ck_alerta_conclusion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `regla_monitoreo_id` | UUID | — | UUID, obligatoria, referencia a `regla_monitoreo_lft`. | PERSONAL |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, opcional, referencia a `cuenta_billetera`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `caso_id` | UUID | — | UUID, opcional, referencia a `caso_investigacion_lft`. | NINGUNA |
| `asignada_a` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `monto_involucrado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `detalle` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `conclusion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `detectada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `beneficiario_final`

**Qué es.** Las tres declaraciones que el marco de prevención exige y que casi ningún sistema casero modela.

**Para qué sirve.** - **PEP**: si el cliente es una persona expuesta políticamente —o familiar o allegado— la relación exige debida diligencia reforzada y aprobación de nivel superior. Guardar cargo, institución, país y período es lo que permite saber cuándo deja de serlo. - **Beneficiario final**: quién controla realmente. Aplica sobre todo cuando el titular es una organización, una asociación o un grupo con personería. - **Origen de fondos**: para operaciones sobre umbral, la declaración de dónde salió la plata, con documento de respaldo y su hash.

**Por qué existe.** Porque son exactamente los tres papeles que se piden en una inspección, y porque cuando aparece un problema con un cliente, la primera pregunta es "¿le preguntaron?". Sin estas tablas la respuesta es "verbalmente". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `nombre` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `porcentaje_participacion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `tipo_control` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `verificado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `registrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `calificacion_riesgo_cliente`

**Qué es.** El nivel de riesgo vigente de un cliente y lo que ese nivel exige.

**Para qué sirve.** Es **la pieza que conecta cumplimiento con producto**. De ella dependen: - el tipo de debida diligencia exigida, - los [[limite_operativo_billetera]] que aplican (M10), - la periodicidad de revisión, - la sensibilidad del monitoreo. Eso convierte la verificación en algo con valor para el usuario: subir de nivel no es un trámite burocrático, es lo que le permite operar más. Y convierte el cumplimiento en algo automático: cambiar la calificación cambia los límites sin que nadie toque una configuración.

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `matriz_riesgo_lft`. La referencian `debida_diligencia`, `revision_periodica_kyc`. Reglas nombradas: `ex_calificacion_vigente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `matriz_riesgo_id` | UUID | — | UUID, opcional, referencia a `matriz_riesgo_lft`. | NINGUNA |
| `calificado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `nivel` | VARCHAR(6) | — | VARCHAR(6), obligatoria. | NINGUNA |
| `puntaje_total` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `nivel_dd_requerido` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `periodicidad_revision_meses` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `proxima_revision` | DATE | — | DATE, obligatoria. | NINGUNA |
| `es_automatica` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `motivo_cambio` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |

### `capacitacion_cumplimiento`

**Qué es.** La designación del responsable y la formación obligatoria del personal.

**Para qué sirve.** Son dos requisitos formales que siempre se piden y que siempre se responden con papeles sueltos: quién es el oficial de cumplimiento —titular y suplente—, desde cuándo, con qué acta y si se comunicó al organismo; y quién recibió capacitación, sobre qué tema, cuántas horas y con qué resultado. Tenerlos como tablas convierte "sí, capacitamos" en un reporte con nombres y fechas. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tema` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `modalidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `horas` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `calificacion` | DECIMAL(5,2) | — | DECIMAL(5,2), opcional. | NINGUNA |
| `aprobada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `evidencia_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `periodo` | CHAR(4) | — | CHAR(4), obligatoria. | NINGUNA |

### `caso_investigacion_lft`

**Qué es.** La detección y la investigación.

**Para qué sirve.** Están separadas a propósito: una alerta es automática y barata; un caso es humano y caro. Varias alertas del mismo cliente se agrupan en un caso, el analista investiga y **decide**: descartar, mantener bajo monitoreo reforzado, reportar a la autoridad o terminar la relación. Lo importante es que **descartar también deja rastro**: `conclusion` es obligatoria. Un sistema donde las alertas se cierran sin justificación es peor que uno sin alertas, porque documenta que se vio y no se hizo nada. `plazo_limite` en el caso obliga a que la investigación tenga fecha de vencimiento.

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `auditoria.reporte_operacion_sospechosa`, `identidad.usuario`. La referencian `alerta_monitoreo_lft`. Reglas nombradas: `ck_caso_plazo`, `ck_caso_revision`, `ck_caso_reporte`, `ix_caso_vencidos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `analista_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `revisado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `reporte_operacion_sospechosa_id` | UUID | — | UUID, opcional, referencia a `reporte_operacion_sospechosa`. | NINGUNA |
| `origen` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `prioridad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `resumen` | VARCHAR(500) | — | VARCHAR(500), obligatoria. | NINGUNA |
| `hallazgos` | TEXT | — | TEXT, opcional. | NINGUNA |
| `decision` | VARCHAR(25) | — | VARCHAR(25), opcional. | NINGUNA |
| `abierto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `plazo_limite` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `catalogo_reporte_regulatorio`

**Qué es.** La agenda de reportes obligatorios y el rastro de cada envío.

**Para qué sirve.** Una entidad supervisada envía información periódica: diaria, mensual, trimestral. El catálogo define **qué se debe enviar, a quién, con qué periodicidad, en qué formato y con qué plazo**. A partir de ahí el sistema genera los vencimientos solo, y un reporte no enviado es visible antes de vencer, no después. El envío guarda `numero_constancia`: la prueba de que se envió. La observación guarda lo que el organismo devolvió —observación, instrucción, multa o requerimiento— con su plazo de respuesta.

**Por qué existe.** Porque el incumplimiento más común y más caro no es hacer las cosas mal: es **no mandar a tiempo**. Un calendario en una planilla no sobrevive a la primera rotación de personal. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `reporte_regulatorio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `organismo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `periodicidad` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `formato` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `plazo_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `base_normativa` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `obligatorio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `comite_gobierno`

**Qué es.** El aparato de gobierno: comités, actas, manuales y designaciones.

**Para qué sirve.** Esto no es burocracia decorativa: en la práctica supervisora, **un manual sin acta de aprobación se considera inexistente**, y la ausencia de manuales de procedimientos es una falta administrativa sancionable. `acta_comite` guarda asistentes, quórum, temas y decisiones con hash del documento; `politica_interna` versiona cada manual con su fecha de próxima revisión; `designacion_regulatoria` unifica quién es oficial de cumplimiento, responsable de seguridad de la información, responsable de riesgos, auditor interno y responsable del punto de reclamo, con acta y fecha de comunicación al organismo. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `acta_comite`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `periodicidad_minima` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `composicion_requerida` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `quorum_minimo` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `contrato_adhesion`

**Qué es.** El contrato modelo y la prueba de que cada usuario lo aceptó.

**Para qué sirve.** Los contratos de adhesión de servicios financieros tienen forma reglada y suelen requerir registro previo ante el supervisor. El modelo guarda versión, hash, número de registro y vigencia. La aceptación guarda IP, dispositivo, token de firma y hash de evidencia: eso es lo que convierte un clic en una aceptación oponible. Sin esa fila, el consentimiento del usuario es una afirmación de la empresa. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `aceptacion_contrato`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `url_documento` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `registrado_ante_regulador` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `numero_registro` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `fecha_registro` | DATE | — | DATE, opcional. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `contrato_tercero`

**Qué es.** La tercerización y su seguimiento.

**Para qué sirve.** Una billetera se apoya en terceros críticos: pasarela, mensajería, nube, antifraude. La responsabilidad frente al cliente **no se terceriza**, así que hay que registrar quién procesa qué, si accede a datos personales, en qué país los procesa, con qué cláusulas de confidencialidad, auditoría y continuidad, y con qué nivel de servicio. La evaluación periódica mide cumplimiento de acuerdo de nivel de servicio e incidentes atribuibles. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `activo_informacion`, `evaluacion_tercero`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `razon_social` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `nit` | VARCHAR(20) | — | VARCHAR(20), opcional. | PERSONAL |
| `servicio_contratado` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `es_critico` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `accede_a_datos_personales` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `pais_procesamiento` | CHAR(2) | — | CHAR(2), obligatoria. | NINGUNA |
| `nivel_riesgo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `evaluacion_riesgo_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `clausula_confidencialidad` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `clausula_auditoria` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `clausula_continuidad` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `acuerdo_nivel_servicio` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `comunicado_al_organismo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `control_interno`

**Qué es.** El inventario de controles y la evidencia de que se ejecutan.

**Para qué sirve.** Un control que nadie prueba es un control que no existe. `prueba_control` guarda período, tamaño de muestra, excepciones encontradas y resultado —efectivo, deficiente o no efectivo—, que es exactamente el formato en el que auditoría interna y externa trabajan. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `prueba_control`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `proceso` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `frecuencia` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `automatizado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `riesgo_mitigado` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `debida_diligencia`

**Qué es.** El expediente de conocimiento del cliente, por nivel.

**Para qué sirve.** Cinco niveles —simplificada, estándar, ampliada, reforzada y continua— porque no todos los clientes justifican el mismo esfuerzo. Una persona que abre cuenta para aportar Bs 300 al mes no puede pasar por el mismo proceso que una que mueve Bs 80.000: exigirle lo mismo es perder al primero sin controlar mejor al segundo. `documentos_requeridos` vs `documentos_recibidos` en JSON permite que el requerimiento documental sea **configurable por nivel** y que la app muestre exactamente qué falta. `segunda_revision_por` implementa el principio de cuatro ojos: en debida diligencia reforzada, quien aprueba no puede ser quien recolectó.

**Por qué existe.** Porque la verificación KYC es un acto técnico —validar que el documento es real y la persona es quien dice ser— y la debida diligencia es un proceso de cumplimiento con vigencia, revisión periódica, aprobación y vencimiento. Fusionarlas hace imposible representar "el KYC está bien pero la debida diligencia venció". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `calificacion_riesgo_cliente`, `identidad.usuario`, `identidad.verificacion_kyc`. Reglas nombradas: `ix_ddd_por_vencer`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `verificacion_kyc_id` | UUID | — | UUID, opcional, referencia a `verificacion_kyc`. | NINGUNA |
| `calificacion_riesgo_id` | UUID | — | UUID, opcional, referencia a `calificacion_riesgo_cliente`. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `segunda_revision_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `documentos_requeridos` | JSONB | — | JSONB, obligatoria. | PERSONAL |
| `documentos_recibidos` | JSONB | — | JSONB, obligatoria. | PERSONAL |
| `observaciones` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `completada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `vence_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `declaracion_origen_fondos`

**Qué es.** Las tres declaraciones que el marco de prevención exige y que casi ningún sistema casero modela.

**Para qué sirve.** - **PEP**: si el cliente es una persona expuesta políticamente —o familiar o allegado— la relación exige debida diligencia reforzada y aprobación de nivel superior. Guardar cargo, institución, país y período es lo que permite saber cuándo deja de serlo. - **Beneficiario final**: quién controla realmente. Aplica sobre todo cuando el titular es una organización, una asociación o un grupo con personería. - **Origen de fondos**: para operaciones sobre umbral, la declaración de dónde salió la plata, con documento de respaldo y su hash.

**Por qué existe.** Porque son exactamente los tres papeles que se piden en una inspección, y porque cuando aparece un problema con un cliente, la primera pregunta es "¿le preguntaron?". Sin estas tablas la respuesta es "verbalmente". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `nucleo_financiero.transaccion_billetera`. La referencian `registro_operacion_relevante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `verificada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `origen` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `documento_respaldo_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `declarada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `declaracion_pep`

**Qué es.** Las tres declaraciones que el marco de prevención exige y que casi ningún sistema casero modela.

**Para qué sirve.** - **PEP**: si el cliente es una persona expuesta políticamente —o familiar o allegado— la relación exige debida diligencia reforzada y aprobación de nivel superior. Guardar cargo, institución, país y período es lo que permite saber cuándo deja de serlo. - **Beneficiario final**: quién controla realmente. Aplica sobre todo cuando el titular es una organización, una asociación o un grupo con personería. - **Origen de fondos**: para operaciones sobre umbral, la declaración de dónde salió la plata, con documento de respaldo y su hash.

**Por qué existe.** Porque son exactamente los tres papeles que se piden en una inspección, y porque cuando aparece un problema con un cliente, la primera pregunta es "¿le preguntaron?". Sin estas tablas la respuesta es "verbalmente". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `verificada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `es_pep` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `tipo_pep` | VARCHAR(25) | — | VARCHAR(25), opcional. | NINGUNA |
| `cargo` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `institucion` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `pais` | CHAR(2) | — | CHAR(2), opcional. | NINGUNA |
| `desde` | DATE | — | DATE, opcional. | NINGUNA |
| `hasta` | DATE | — | DATE, opcional. | NINGUNA |
| `evidencia_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `declarada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `designacion_regulatoria`

**Qué es.** El aparato de gobierno: comités, actas, manuales y designaciones.

**Para qué sirve.** Esto no es burocracia decorativa: en la práctica supervisora, **un manual sin acta de aprobación se considera inexistente**, y la ausencia de manuales de procedimientos es una falta administrativa sancionable. `acta_comite` guarda asistentes, quórum, temas y decisiones con hash del documento; `politica_interna` versiona cada manual con su fecha de próxima revisión; `designacion_regulatoria` unifica quién es oficial de cumplimiento, responsable de seguridad de la información, responsable de riesgos, auditor interno y responsable del punto de reclamo, con acta y fecha de comunicación al organismo. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `acta_comite`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `acta_comite_id` | UUID | — | UUID, opcional, referencia a `acta_comite`. | NINGUNA |
| `cargo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `fecha_designacion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `organismo_comunicado` | VARCHAR(10) | — | VARCHAR(10), opcional. | NINGUNA |
| `comunicada_al_organismo_en` | DATE | — | DATE, opcional. | NINGUNA |
| `fecha_baja` | DATE | — | DATE, opcional. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `desvio_perfil`

**Qué es.** Lo que el cliente declaró que iba a mover, y lo que efectivamente movió.

**Para qué sirve.** Esta es la base de todo el monitoreo con sentido. Sin perfil declarado, la única forma de detectar algo raro es por umbrales absolutos, que generan miles de falsos positivos. Con perfil, "Bs 40.000 este mes" es normal para un comerciante y una alerta seria para un asalariado que declaró Bs 3.000. `tipo` distingue `DECLARADO` de `OBSERVADO`: el segundo se recalcula solo con el comportamiento real, y compararlos es exactamente lo que produce el desvío.

**Por qué existe.** Porque la pregunta del supervisor no es "¿tienen alertas?", es "¿cómo saben que esta operación es coherente con este cliente?". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `alerta_monitoreo_lft`, `identidad.usuario`, `perfil_transaccional`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `perfil_transaccional_id` | UUID | — | UUID, obligatoria, referencia a `perfil_transaccional`. | NINGUNA |
| `alerta_monitoreo_id` | UUID | — | UUID, opcional, referencia a `alerta_monitoreo_lft`. | PERSONAL |
| `periodo` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `monto_observado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_esperado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `desvio_porcentual` | DECIMAL(8,2) | — | DECIMAL(8,2), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `justificacion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `detectado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `documento_publicado`

**Qué es.** El registro de qué documentos estuvieron publicados y desde cuándo.

**Para qué sirve.** Tarifario, contrato, política de privacidad, canales de reclamo y horarios de atención deben estar publicados y accesibles. La obligación no es solo publicarlos: es **poder demostrar que estuvieron publicados en una fecha determinada**. `hash_documento` + vigencias responden eso.

**Por qué existe.** Porque "el tarifario siempre estuvo en la web" no es demostrable si la web se actualiza en el lugar. Con esta tabla, sí. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `referencia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `publicado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `url_publica` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |

### `entorno_prueba_regulado`

**Qué es.** El sandbox: probar un servicio nuevo bajo supervisión, con límites.

**Para qué sirve.** Permite lanzar algo que todavía no está en la licencia, con tope de usuarios, tope de monto por operación, garantía constituida y plazo. Que esos límites sean columnas permite que el sistema los haga cumplir solo, que es la única forma de no salirse del sandbox sin darse cuenta. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.licencia_regulatoria`. Reglas nombradas: `ck_sandbox_limites`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `licencia_regulatoria_id` | UUID | — | UUID, obligatoria, referencia a `licencia_regulatoria`. | NINGUNA |
| `servicio_en_prueba` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `alcance` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `limite_usuarios` | INTEGER | — | INTEGER, opcional. | NINGUNA |
| `limite_monto_operacion` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `garantia_constituida` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `informes_remitidos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `envio_regulatorio`

**Qué es.** La agenda de reportes obligatorios y el rastro de cada envío.

**Para qué sirve.** Una entidad supervisada envía información periódica: diaria, mensual, trimestral. El catálogo define **qué se debe enviar, a quién, con qué periodicidad, en qué formato y con qué plazo**. A partir de ahí el sistema genera los vencimientos solo, y un reporte no enviado es visible antes de vencer, no después. El envío guarda `numero_constancia`: la prueba de que se envió. La observación guarda lo que el organismo devolvió —observación, instrucción, multa o requerimiento— con su plazo de respuesta.

**Por qué existe.** Porque el incumplimiento más común y más caro no es hacer las cosas mal: es **no mandar a tiempo**. Un calendario en una planilla no sobrevive a la primera rotación de personal. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `reporte_regulatorio`. La referencian `observacion_regulatoria`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reporte_regulatorio_id` | UUID | — | UUID, obligatoria, referencia a `reporte_regulatorio`. | NINGUNA |
| `enviado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `organismo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `canal` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fecha_envio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `numero_constancia` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `respuesta` | JSONB | — | JSONB, opcional. | NINGUNA |
| `reintentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `evaluacion_riesgo_producto`

**Qué es.** El análisis de riesgo previo al lanzamiento de un producto o canal nuevo.

**Para qué sirve.** Antes de habilitar, por ejemplo, transferencias entre personas sin límite, o pasanakus entre desconocidos, hay que evaluar qué riesgos de lavado y de fraude introduce eso y qué controles lo mitigan. Guardar la evaluación con su aprobación es lo que permite lanzar sin que el lanzamiento sea la observación del año siguiente. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. Reglas nombradas: `uq_evaluacion_producto_version`, `ck_evaluacion_no_objecion`, `ck_evaluacion_vigente_aprobada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `producto` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `riesgos_identificados` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `nivel_riesgo_lft` | VARCHAR(6) | — | VARCHAR(6), obligatoria. | NINGUNA |
| `controles_definidos` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `requiere_no_objecion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_aprobacion` | DATE | — | DATE, opcional. | NINGUNA |

### `evaluacion_tercero`

**Qué es.** La tercerización y su seguimiento.

**Para qué sirve.** Una billetera se apoya en terceros críticos: pasarela, mensajería, nube, antifraude. La responsabilidad frente al cliente **no se terceriza**, así que hay que registrar quién procesa qué, si accede a datos personales, en qué país los procesa, con qué cláusulas de confidencialidad, auditoría y continuidad, y con qué nivel de servicio. La evaluación periódica mide cumplimiento de acuerdo de nivel de servicio e incidentes atribuibles. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `contrato_tercero`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `contrato_tercero_id` | UUID | — | UUID, obligatoria, referencia a `contrato_tercero`. | NINGUNA |
| `evaluado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `periodo` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `cumplimiento_sla` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `incidentes_atribuibles` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `resultado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `acciones_requeridas` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `evaluada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `evento_riesgo_operativo`

**Qué es.** La base de datos de pérdidas por errores, fallas y fraudes.

**Para qué sirve.** Poner número a lo que cuesta hacer las cosas mal. Cada descuadre de custodia (M10), cada reverso por error operativo, cada acreditación duplicada y cada caída con impacto monetario entra acá con su pérdida bruta, su recuperación y su pérdida neta. `categoria_basilea` y `linea_negocio` permiten comparar con estándares y armar los reportes de gestión de riesgo operativo.

**Por qué existe.** Porque sin base de pérdidas, la discusión sobre invertir en controles es una discusión de opiniones. Con base de pérdidas, es una discusión de números: *"la conciliación manual nos costó Bs 40.000 el año pasado"*. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `auditoria.incidente_operativo`, `identidad.usuario`. La referencian `incidente_seguridad`, `plan_accion_riesgo`. Reglas nombradas: `ck_evento_categoria`, `ck_evento_factor`, `ck_evento_fechas`, `ck_evento_recuperacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `incidente_operativo_id` | UUID | — | UUID, opcional, referencia a `incidente_operativo`. | NINGUNA |
| `registrado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `categoria_evento` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `factor_riesgo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `reportado_central_riesgo_operativo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `linea_negocio` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `fecha_ocurrencia` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_deteccion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_contabilizacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `perdida_bruta` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `recuperacion` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `perdida_neta` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `causa_raiz` | TEXT | — | TEXT, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `expediente_cliente`

**Qué es.** La agenda de actualización y el legajo completo.

**Para qué sirve.** El conocimiento del cliente **se vence**. La revisión periódica se programa según el nivel de riesgo (más riesgo, más frecuente) y queda visible cuando está atrasada. El expediente consolida qué documentos hay, qué completitud tiene y hasta cuándo hay que conservarlo. El período de conservación es largo —no menor a **diez años** para los libros y documentos de las operaciones, contados desde la fecha del último asiento contable, según la Ley de Servicios Financieros— y por eso `retencion_hasta` se calcula y se guarda por expediente en lugar de aplicarse una regla global que nadie puede auditar. `retencion_hasta` es también lo que hace compatible el derecho a la eliminación de datos personales con la obligación de conservación: [[proceso_anonimizacion]] (M9) consulta esta fecha antes de borrar. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. Reglas nombradas: `ck_expediente_retencion_futura`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `completitud_porcentaje` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `documentos` | JSONB | — | JSONB, obligatoria. | PERSONAL |
| `ubicacion_fisica` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `retencion_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ultima_actualizacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `factor_riesgo_evaluado`

**Qué es.** Los factores con los que se puntúa el riesgo de un cliente y el resultado de evaluar cada uno.

**Para qué sirve.** El riesgo no se decide "a ojo": se calcula sobre cuatro dimensiones —cliente, producto, canal y zona geográfica— con factores ponderados. Que la matriz sea una tabla versionada permite dos cosas que un supervisor pide siempre: **explicar por qué este cliente es de riesgo alto** y **demostrar qué criterio regía cuando se lo calificó**. `factor_riesgo_evaluado` guarda el detalle: no solo "puntaje 72", sino "actividad económica: cambista → 25 puntos; zona: frontera → 20 puntos". Sin ese detalle, la calificación es un número que nadie puede defender. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `matriz_riesgo_lft`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `matriz_riesgo_id` | UUID | — | UUID, obligatoria, referencia a `matriz_riesgo_lft`. | NINGUNA |
| `dimension` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `factor` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `valor_observado` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `puntaje` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `evaluado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `hallazgo_auditoria`

**Qué es.** Lo que se encontró mal y lo que se va a hacer al respecto.

**Para qué sirve.** Un hallazgo sin responsable y sin plazo no se corrige. El plan de acción tiene responsable, fecha comprometida, porcentaje de avance y evidencia de cierre. Los hallazgos vencidos son visibles y **son exactamente lo primero que revisa una inspección de seguimiento**. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `plan_accion_riesgo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `origen` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `proceso` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `fecha_identificacion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `plazo_regularizacion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `incidente_seguridad`

**Qué es.** El inventario clasificado de activos y los incidentes que los afectan.

**Para qué sirve.** El inventario con propietario, custodio y clasificación es el punto de partida de cualquier sistema de gestión de seguridad de la información, y también lo que permite responder "¿dónde están los datos personales?" sin adivinar: `contiene_datos_personales` es una columna consultable. En el incidente, `plazo_reporte` se calcula al detectar y **se guarda**: tres relojes distintos corren en paralelo —contención, reporte al organismo y notificación a los titulares— y hay que poder demostrar cuál regía ese día. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `activo_informacion`, `auditoria.incidente_operativo`, `evento_riesgo_operativo`, `identidad.usuario`. Reglas nombradas: `ck_incidente_plazo`, `ck_incidente_notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `activo_informacion_id` | UUID | — | UUID, opcional, referencia a `activo_informacion`. | NINGUNA |
| `incidente_operativo_id` | UUID | — | UUID, opcional, referencia a `incidente_operativo`. | NINGUNA |
| `evento_riesgo_id` | UUID | — | UUID, opcional, referencia a `evento_riesgo_operativo`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `vector_ataque` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `datos_personales_afectados` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `usuarios_afectados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `detectado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `contenido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `reportado_al_organismo_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `notificado_a_titulares_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `plazo_reporte` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `leccion_aprendida` | TEXT | — | TEXT, opcional. | NINGUNA |

### `instancia_reclamo`

**Qué es.** El canal de reclamos, el reclamo y su escalamiento.

**Para qué sirve.** La atención de reclamos es uno de los aspectos más vigilados de la relación con el consumidor financiero, y uno de los más fáciles de hacer mal. Tres decisiones del modelo: 1. **El plazo se guarda, no se calcula.** `plazo_respuesta` se fija al ingresar el reclamo. Si la norma cambia el plazo el mes que viene, los reclamos viejos conservan el plazo que les regía. Un plazo calculado al vuelo reescribe la historia. 2. **El escalamiento es una entidad, no un estado.** Un reclamo puede subir a la defensoría del consumidor financiero, al supervisor, a arbitraje o a la vía judicial, y cada instancia tiene expediente, resolución y monto resarcido propios. 3. **Un reclamo favorable con monto exige una devolución asociada.** No se puede cerrar diciendo "le dimos la razón" sin que exista la [[devolucion_comision]] (M11) o la transacción de resarcimiento (M10). Eso cierra el circuito entre reconocer el error y repararlo.

**Por qué existe.** Porque la tasa de reclamos resueltos a favor del cliente es el indicador que mira el supervisor: si sube, hay un problema de producto, no de atención. Y ese indicador no se puede calcular sin estas tablas. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `reclamo_cliente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reclamo_id` | UUID | — | UUID, obligatoria, referencia a `reclamo_cliente`. | NINGUNA |
| `instancia` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_elevacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `numero_expediente` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `resolucion` | TEXT | — | TEXT, opcional. | NINGUNA |
| `fecha_resolucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `monto_resarcido` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |

### `matriz_riesgo_lft`

**Qué es.** Los factores con los que se puntúa el riesgo de un cliente y el resultado de evaluar cada uno.

**Para qué sirve.** El riesgo no se decide "a ojo": se calcula sobre cuatro dimensiones —cliente, producto, canal y zona geográfica— con factores ponderados. Que la matriz sea una tabla versionada permite dos cosas que un supervisor pide siempre: **explicar por qué este cliente es de riesgo alto** y **demostrar qué criterio regía cuando se lo calificó**. `factor_riesgo_evaluado` guarda el detalle: no solo "puntaje 72", sino "actividad económica: cambista → 25 puntos; zona: frontera → 20 puntos". Sin ese detalle, la calificación es un número que nadie puede defender. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `calificacion_riesgo_cliente`, `factor_riesgo_evaluado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `dimension` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `factor` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `ponderacion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `escala` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `base_normativa` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `observacion_regulatoria`

**Qué es.** La agenda de reportes obligatorios y el rastro de cada envío.

**Para qué sirve.** Una entidad supervisada envía información periódica: diaria, mensual, trimestral. El catálogo define **qué se debe enviar, a quién, con qué periodicidad, en qué formato y con qué plazo**. A partir de ahí el sistema genera los vencimientos solo, y un reporte no enviado es visible antes de vencer, no después. El envío guarda `numero_constancia`: la prueba de que se envió. La observación guarda lo que el organismo devolvió —observación, instrucción, multa o requerimiento— con su plazo de respuesta.

**Por qué existe.** Porque el incumplimiento más común y más caro no es hacer las cosas mal: es **no mandar a tiempo**. Un calendario en una planilla no sobrevive a la primera rotación de personal. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `envio_regulatorio`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `envio_regulatorio_id` | UUID | — | UUID, opcional, referencia a `envio_regulatorio`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `organismo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `numero_documento` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | PERSONAL |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `monto_multa` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `plazo_respuesta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `respuesta` | TEXT | — | TEXT, opcional. | NINGUNA |
| `recibida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `respondida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `oficial_cumplimiento`

**Qué es.** La designación del responsable y la formación obligatoria del personal.

**Para qué sirve.** Son dos requisitos formales que siempre se piden y que siempre se responden con papeles sueltos: quién es el oficial de cumplimiento —titular y suplente—, desde cuándo, con qué acta y si se comunicó al organismo; y quién recibió capacitación, sobre qué tema, cuántas horas y con qué resultado. Tenerlos como tablas convierte "sí, capacitamos" en un reporte con nombres y fechas. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. Reglas nombradas: `uq_oficial_titular_activo`, `ck_oficial_baja_coherente`, `ck_oficial_baja_posterior`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `fecha_designacion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `acta_designacion` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `comunicada_al_regulador_en` | DATE | — | DATE, opcional. | NINGUNA |
| `fecha_baja` | DATE | — | DATE, opcional. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `perfil_transaccional`

**Qué es.** Lo que el cliente declaró que iba a mover, y lo que efectivamente movió.

**Para qué sirve.** Esta es la base de todo el monitoreo con sentido. Sin perfil declarado, la única forma de detectar algo raro es por umbrales absolutos, que generan miles de falsos positivos. Con perfil, "Bs 40.000 este mes" es normal para un comerciante y una alerta seria para un asalariado que declaró Bs 3.000. `tipo` distingue `DECLARADO` de `OBSERVADO`: el segundo se recalcula solo con el comportamiento real, y compararlos es exactamente lo que produce el desvío.

**Por qué existe.** Porque la pregunta del supervisor no es "¿tienen alertas?", es "¿cómo saben que esta operación es coherente con este cliente?". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `desvio_perfil`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `monto_mensual_estimado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `cantidad_operaciones_estimada` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `actividad_economica` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `codigo_ciiu` | VARCHAR(10) | — | VARCHAR(10), opcional. | NINGUNA |
| `origen_fondos_declarado` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `fuente` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `plan_accion_riesgo`

**Qué es.** Lo que se encontró mal y lo que se va a hacer al respecto.

**Para qué sirve.** Un hallazgo sin responsable y sin plazo no se corrige. El plan de acción tiene responsable, fecha comprometida, porcentaje de avance y evidencia de cierre. Los hallazgos vencidos son visibles y **son exactamente lo primero que revisa una inspección de seguimiento**. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evento_riesgo_operativo`, `hallazgo_auditoria`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `evento_riesgo_id` | UUID | — | UUID, opcional, referencia a `evento_riesgo_operativo`. | NINGUNA |
| `hallazgo_id` | UUID | — | UUID, opcional, referencia a `hallazgo_auditoria`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `descripcion` | VARCHAR(500) | — | VARCHAR(500), obligatoria. | NINGUNA |
| `fecha_compromiso` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_cierre` | DATE | — | DATE, opcional. | NINGUNA |
| `avance_porcentaje` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `evidencia_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |

### `plan_continuidad`

**Qué es.** Los planes de continuidad por proceso crítico y la evidencia de que se prueban.

**Para qué sirve.** La norma no pide tener un plan: pide **probarlo y documentar el resultado en acta**. Por eso la prueba guarda RTO y RPO obtenidos contra los comprometidos, el tipo de prueba, los hallazgos y el acta donde se reportó. Un plan sin pruebas registradas es un hallazgo garantizado. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `politica_interna`. La referencian `prueba_continuidad`. Reglas nombradas: `ck_plan_objetivos`, `ck_plan_prueba`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `politica_interna_id` | UUID | — | UUID, opcional, referencia a `politica_interna`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `proceso_critico` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `rto_minutos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `rpo_minutos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `estrategia` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `periodicidad_prueba_meses` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `proxima_prueba` | DATE | — | DATE, obligatoria. | NINGUNA |

### `politica_interna`

**Qué es.** El aparato de gobierno: comités, actas, manuales y designaciones.

**Para qué sirve.** Esto no es burocracia decorativa: en la práctica supervisora, **un manual sin acta de aprobación se considera inexistente**, y la ausencia de manuales de procedimientos es una falta administrativa sancionable. `acta_comite` guarda asistentes, quórum, temas y decisiones con hash del documento; `politica_interna` versiona cada manual con su fecha de próxima revisión; `designacion_regulatoria` unifica quién es oficial de cumplimiento, responsable de seguridad de la información, responsable de riesgos, auditor interno y responsable del punto de reclamo, con acta y fecha de comunicación al organismo. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `acta_comite`, `identidad.usuario`. La referencian `plan_continuidad`. Reglas nombradas: `ck_politica_acta`, `ck_politica_revision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `acta_comite_id` | UUID | — | UUID, opcional, referencia a `acta_comite`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `materia` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `url_documento` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `aprobada_por_directorio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `proxima_revision` | DATE | — | DATE, obligatoria. | NINGUNA |

### `prueba_continuidad`

**Qué es.** Los planes de continuidad por proceso crítico y la evidencia de que se prueban.

**Para qué sirve.** La norma no pide tener un plan: pide **probarlo y documentar el resultado en acta**. Por eso la prueba guarda RTO y RPO obtenidos contra los comprometidos, el tipo de prueba, los hallazgos y el acta donde se reportó. Un plan sin pruebas registradas es un hallazgo garantizado. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `acta_comite`, `identidad.usuario`, `plan_continuidad`. Reglas nombradas: `ck_prueba_resultado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `plan_continuidad_id` | UUID | — | UUID, obligatoria, referencia a `plan_continuidad`. | NINGUNA |
| `acta_comite_id` | UUID | — | UUID, opcional, referencia a `acta_comite`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `rto_obtenido_minutos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `rpo_obtenido_minutos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `resultado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `hallazgos` | TEXT | — | TEXT, opcional. | NINGUNA |
| `evidencia_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |

### `prueba_control`

**Qué es.** El inventario de controles y la evidencia de que se ejecutan.

**Para qué sirve.** Un control que nadie prueba es un control que no existe. `prueba_control` guarda período, tamaño de muestra, excepciones encontradas y resultado —efectivo, deficiente o no efectivo—, que es exactamente el formato en el que auditoría interna y externa trabajan. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `control_interno`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `control_id` | UUID | — | UUID, obligatoria, referencia a `control_interno`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `periodo` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `tamanio_muestra` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `excepciones` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `resultado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `evidencia_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `punto_reclamo`

**Qué es.** El canal de reclamos, el reclamo y su escalamiento.

**Para qué sirve.** La atención de reclamos es uno de los aspectos más vigilados de la relación con el consumidor financiero, y uno de los más fáciles de hacer mal. Tres decisiones del modelo: 1. **El plazo se guarda, no se calcula.** `plazo_respuesta` se fija al ingresar el reclamo. Si la norma cambia el plazo el mes que viene, los reclamos viejos conservan el plazo que les regía. Un plazo calculado al vuelo reescribe la historia. 2. **El escalamiento es una entidad, no un estado.** Un reclamo puede subir a la defensoría del consumidor financiero, al supervisor, a arbitraje o a la vía judicial, y cada instancia tiene expediente, resolución y monto resarcido propios. 3. **Un reclamo favorable con monto exige una devolución asociada.** No se puede cerrar diciendo "le dimos la razón" sin que exista la [[devolucion_comision]] (M11) o la transacción de resarcimiento (M10). Eso cierra el circuito entre reconocer el error y repararlo.

**Por qué existe.** Porque la tasa de reclamos resueltos a favor del cliente es el indicador que mira el supervisor: si sube, hay un problema de producto, no de atención. Y ese indicador no se puede calcular sin estas tablas. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `reclamo_cliente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `horario` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `reclamo_cliente`

**Qué es.** El canal de reclamos, el reclamo y su escalamiento.

**Para qué sirve.** La atención de reclamos es uno de los aspectos más vigilados de la relación con el consumidor financiero, y uno de los más fáciles de hacer mal. Tres decisiones del modelo: 1. **El plazo se guarda, no se calcula.** `plazo_respuesta` se fija al ingresar el reclamo. Si la norma cambia el plazo el mes que viene, los reclamos viejos conservan el plazo que les regía. Un plazo calculado al vuelo reescribe la historia. 2. **El escalamiento es una entidad, no un estado.** Un reclamo puede subir a la defensoría del consumidor financiero, al supervisor, a arbitraje o a la vía judicial, y cada instancia tiene expediente, resolución y monto resarcido propios. 3. **Un reclamo favorable con monto exige una devolución asociada.** No se puede cerrar diciendo "le dimos la razón" sin que exista la [[devolucion_comision]] (M11) o la transacción de resarcimiento (M10). Eso cierra el circuito entre reconocer el error y repararlo.

**Por qué existe.** Porque la tasa de reclamos resueltos a favor del cliente es el indicador que mira el supervisor: si sube, hay un problema de producto, no de atención. Y ese indicador no se puede calcular sin estas tablas. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `auditoria.ticket_soporte`, `identidad.usuario`, `punto_reclamo`, `tarifas.devolucion_comision`. La referencian `instancia_reclamo`, `tarifas.devolucion_comision`. Reglas nombradas: `ck_reclamo_plazo`, `ck_reclamo_dias`, `uq_reclamo_codigo`, `ck_reclamo_prorroga`, `ck_reclamo_prorroga_extendida`, `ck_reclamo_reparacion`, `ck_reclamo_conservacion`, `ix_reclamo_vencidos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `punto_reclamo_id` | UUID | — | UUID, obligatoria, referencia a `punto_reclamo`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `ticket_soporte_id` | UUID | — | UUID, opcional, referencia a `ticket_soporte`. | NINGUNA |
| `devolucion_comision_id` | UUID | — | UUID, opcional, referencia a `devolucion_comision`. | NINGUNA |
| `categoria` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `producto` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_reclamado` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `canal_ingreso` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_ingreso` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `dias_habiles_plazo` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `plazo_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `plazo_prorrogado_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `prorroga_comunicada_al_cliente_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `prorroga_comunicada_al_organismo_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `justificacion_prorroga` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `fecha_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `resultado` | VARCHAR(15) | — | VARCHAR(15), opcional. | NINGUNA |
| `respuesta` | TEXT | — | TEXT, opcional. | NINGUNA |
| `incluido_en_reporte_mensual` | CHAR(7) | — | CHAR(7), opcional. | NINGUNA |
| `conservar_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |

### `registro_operacion_relevante`

**Qué es.** El registro de toda operación que activa una obligación de reporte por umbral.

**Para qué sirve.** Hay operaciones que se registran y se reportan **por monto, sin que haya nada sospechoso**. Es una obligación distinta del reporte de operación sospechosa y hay que poder demostrar las dos por separado: una es objetiva y automática, la otra es subjetiva y fundada. Tres columnas que parecen detalle y son el núcleo del cumplimiento: - **`operacion_inicio_ventana_id`** implementa la regla de reinicio: *"se considera como inicio la operación posterior a la última que hubiera superado el umbral"*. Sin esa referencia, la acumulación se calcula mal y se reporta de menos o de más. - **`monto_equivalente_usd` + `tipo_cambio_aplicado`**: los umbrales están en dólares y las operaciones ocurren en bolivianos. Guardar la conversión y su tipo de cambio hace el cálculo reproducible dos años después. - **`periodo_remision`**: alimenta el envío mensual. La obligación no es solo reportar: es reportar **hasta el día 15 del mes siguiente**, y si no hubo ninguna operación, informar igual que no la hubo — de ahí `reporte_regulatorio.reporte_en_cero`. `exento` + `motivo_exencion` cubren los casos que la norma excluye expresamente (operativa propia entre entidades reguladas, pagos de servicios básicos, impuestos, bonos sociales, pagos con tarjeta). Sin esas columnas, un exento se ve igual que un olvido. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `catalogo.umbral_reporte_uif`, `declaracion_origen_fondos`, `identidad.usuario`, `nucleo_financiero.transaccion_billetera`, `registro_operacion_relevante`, `reporte_regulatorio`. La referencian `registro_operacion_relevante`. Reglas nombradas: `ck_operelev_ventana`, `ck_operelev_tipo_cambio`, `ck_operelev_declaracion`, `ck_operelev_periodo`, `uq_operelev_tx_umbral`, `ix_operelev_periodo`, `ix_operelev_usuario_fecha`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, obligatoria, referencia a `transaccion_billetera`. | NINGUNA |
| `umbral_reporte_id` | UUID | — | UUID, obligatoria, referencia a `umbral_reporte_uif`. | NINGUNA |
| `operacion_inicio_ventana_id` | UUID | — | UUID, opcional, referencia a `registro_operacion_relevante`. | NINGUNA |
| `declaracion_origen_fondos_id` | UUID | — | UUID, opcional, referencia a `declaracion_origen_fondos`. | NINGUNA |
| `reporte_regulatorio_id` | UUID | — | UUID, opcional, referencia a `reporte_regulatorio`. | NINGUNA |
| `formulario` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `concepto_operacion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `es_acumulada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ventana_desde` | DATE | — | DATE, opcional. | NINGUNA |
| `ventana_hasta` | DATE | — | DATE, opcional. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_acumulado_ventana` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `tipo_cambio_aplicado` | DECIMAL(12,6) | — | DECIMAL(12,6), obligatoria. | NINGUNA |
| `monto_equivalente_usd` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `umbral_aplicado_usd` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `origen_declarado` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `destino_declarado` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `exento` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `motivo_exencion` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `periodo_remision` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `fecha_operacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `regla_monitoreo_lft`

**Qué es.** Una tipología de operación inusual, expresada como dato.

**Para qué sirve.** Detectar patrones: fraccionamiento para no llegar al umbral de registro, entrada y salida inmediata del mismo monto, circularidad entre cuentas del mismo círculo, uso de un grupo de pasanaku como pantalla para mover plata entre desconocidos. La expresión es `JSONB` declarativo y `accion_automatica` decide qué hacer: solo alertar, retener la operación o bloquear la cuenta. **Cuando el regulador publica una tipología nueva, se carga una fila**; no se despliega software. Y `fuente_normativa` deja constancia de de dónde salió cada regla.

**Por qué existe.** Porque las tipologías cambian dos o tres veces al año y porque el supervisor pregunta específicamente "¿qué tipologías tienen parametrizadas?". Un `if` en el código no es una respuesta. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `alerta_monitoreo_lft`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `tipologia` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `expresion` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `ventana_evaluacion` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `umbral_monto` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `umbral_cantidad` | INTEGER | — | INTEGER, opcional. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `accion_automatica` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `fuente_normativa` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `reporte_regulatorio`

**Qué es.** La agenda de reportes obligatorios y el rastro de cada envío.

**Para qué sirve.** Una entidad supervisada envía información periódica: diaria, mensual, trimestral. El catálogo define **qué se debe enviar, a quién, con qué periodicidad, en qué formato y con qué plazo**. A partir de ahí el sistema genera los vencimientos solo, y un reporte no enviado es visible antes de vencer, no después. El envío guarda `numero_constancia`: la prueba de que se envió. La observación guarda lo que el organismo devolvió —observación, instrucción, multa o requerimiento— con su plazo de respuesta.

**Por qué existe.** Porque el incumplimiento más común y más caro no es hacer las cosas mal: es **no mandar a tiempo**. Un calendario en una planilla no sobrevive a la primera rotación de personal. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo_reporte_regulatorio`, `identidad.usuario`. La referencian `envio_regulatorio`, `registro_operacion_relevante`. Reglas nombradas: `ck_reporte_en_cero`, `uq_reporte_catalogo_periodo`, `ck_reporte_segregacion`, `ix_reporte_vencidos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `catalogo_reporte_id` | UUID | — | UUID, obligatoria, referencia a `catalogo_reporte_regulatorio`. | NINGUNA |
| `generado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `revisado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `periodo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `fecha_corte` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `cantidad_registros` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `reporte_en_cero` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `url_archivo` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `fecha_limite` | DATE | — | DATE, obligatoria. | NINGUNA |
| `generado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `requerimiento_autoridad`

**Qué es.** Cada oficio recibido de una autoridad.

**Para qué sirve.** Llegan oficios: de fiscalía, de juzgados, de la unidad de inteligencia financiera, del supervisor. Cada uno tiene plazo, alcance y consecuencia. Si ordena inmovilizar fondos, genera un [[bloqueo_saldo]] (M10); si pide información, genera un [[registro_acceso_datos]] (M9).

**Por qué existe.** Porque **nunca se ejecuta una orden sin el documento que la respalda**, y porque el día que el titular reclame por qué le congelaron el saldo, la respuesta tiene que ser el número de oficio, no "nos lo pidieron". ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `nucleo_financiero.bloqueo_saldo`. Reglas nombradas: `ix_requerimiento_vencidos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_afectado_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `bloqueo_saldo_id` | UUID | — | UUID, opcional, referencia a `bloqueo_saldo`. | FINANCIERO |
| `respondido_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `autoridad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `numero_oficio` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `fecha_recepcion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `plazo_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `alcance` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `documento_url` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `respuesta_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `respondido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `revision_periodica_kyc`

**Qué es.** La agenda de actualización y el legajo completo.

**Para qué sirve.** El conocimiento del cliente **se vence**. La revisión periódica se programa según el nivel de riesgo (más riesgo, más frecuente) y queda visible cuando está atrasada. El expediente consolida qué documentos hay, qué completitud tiene y hasta cuándo hay que conservarlo. El período de conservación es largo —no menor a **diez años** para los libros y documentos de las operaciones, contados desde la fecha del último asiento contable, según la Ley de Servicios Financieros— y por eso `retencion_hasta` se calcula y se guarda por expediente en lugar de aplicarse una regla global que nadie puede auditar. `retencion_hasta` es también lo que hace compatible el derecho a la eliminación de datos personales con la obligación de conservación: [[proceso_anonimizacion]] (M9) consulta esta fecha antes de borrar. ---

**Sistema.** Vive en el esquema `cumplimiento`; la escribe y la lee solo el servicio `cumplimiento` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `calificacion_riesgo_cliente`, `identidad.usuario`. Reglas nombradas: `ix_revision_kyc_vencidas`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `calificacion_riesgo_id` | UUID | — | UUID, opcional, referencia a `calificacion_riesgo_cliente`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_programada` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_ejecutada` | DATE | — | DATE, opcional. | NINGUNA |
| `resultado` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

## Esquema `entregas`

### `cesion_derecho`

**Qué es.** La compra de un turno ofrecido: el paso del derecho a cobrar de un participante a otro, con sus estados hasta liquidarse.

**Para qué sirve.** Que el comprador no pierda su plata si la cesión falla, y que el vendedor no pierda su turno sin haber cobrado: primero se retienen los fondos, después se asigna el título y al final se liquida.

**Por qué existe.** La cesión mueve dinero y cambia quién cobra. Sin un registro con estados no hay forma de saber en qué paso quedó una cesión interrumpida ni de compensarla.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.participante`, `grupos.turno`, `oferta_turno`. Reglas nombradas: `uq_cesion_derecho_turno_viva`, `ck_cesion_derecho_partes`, `ck_cesion_derecho_retencion`, `ck_cesion_derecho_titulo`, `ck_cesion_derecho_liquidada`, `ck_cesion_derecho_fallo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `oferta_turno_id` | UUID | — | UUID, obligatoria, referencia a `oferta_turno`. | NINGUNA |
| `turno_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `participante_origen_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `participante_destino_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `comprador_usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_precio` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(16) | — | VARCHAR(16), obligatoria. | NINGUNA |
| `retencion_ref` | UUID | — | UUID, opcional. | NINGUNA |
| `liquidacion_ref` | UUID | — | UUID, opcional. | NINGUNA |
| `motivo_fallo` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `titulo_asignado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `liquidada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `confirmacion_recepcion`

**Qué es.** La confirmación del beneficiario de que efectivamente recibió su plata.

**Para qué sirve.** Cierra el círculo. El sistema puede saber que el banco aceptó la transferencia y aun así la plata no haber llegado (cuenta equivocada, retención bancaria, error del proveedor). **La única confirmación válida es la del que tenía que recibirla.** `montoConfirmado` permite detectar la discrepancia: el sistema mandó Bs 5.450, la persona confirma haber recibido Bs 5.400. Esos Bs 50 son una comisión no prevista o un error, y hay que investigarlos. `autoconfirmadaPorVencimiento` es un compromiso operativo necesario: si nadie confirmara nunca, las entregas quedarían abiertas para siempre y el grupo no podría cerrar el período. Pasado el `plazoLimite` se autoconfirma — pero **queda marcado que fue automática**, no que la persona dijo que sí. La distinción importa si después hay reclamo. `objetar(motivo)` es lo que abre una incidencia formal.

**Por qué existe.** Sin confirmación, "entregado" significa "lo mandamos", y esa diferencia es exactamente donde se pierde la plata y la confianza.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`, `identidad.token_verificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_confirmado` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `token_confirmacion_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | SECRETO |
| `confirmada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `plazo_limite` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `autoconfirmada_por_vencimiento` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ip_confirmacion` | INET | — | INET, opcional. | NINGUNA |
| `comentario` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |

### `cuenta_bancaria_beneficiario`

**Qué es.** La cuenta bancaria o billetera donde el usuario quiere recibir su plata, verificada.

**Para qué sirve.** Es **el punto de mayor riesgo de fraude de toda la plataforma**, y por eso tiene tres defensas explícitas: 1. **`estadoVerificacion` + `metodoVerificacion`.** No se transfiere a una cuenta que nadie validó. El micro-depósito o la coincidencia de nombre con el titular son lo que confirma que la cuenta existe y es de esa persona. 2. **`coincideTitularCon(usuario)`.** La cuenta debe estar a nombre del beneficiario. Si no coincide, o hay un error, o hay alguien cobrando por otro — ambas cosas requieren revisión. 3. **`bloqueadaHasta` — período de enfriamiento.** Esta es la defensa contra el fraude clásico: *"me cambiaron la cuenta justo antes de cobrar"*. Alguien toma control de la cuenta, cambia el destino bancario y espera al turno. Con enfriamiento, un cambio de cuenta no puede usarse inmediatamente, y el usuario real recibe el aviso a tiempo para reaccionar.

**Por qué existe.** Sin esta entidad, el destino del dinero sería un campo de texto en la entrega, editable, sin verificación y sin historial.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `entrega_fondo`, `orden_desembolso`. Reglas nombradas: `uq_cuenta_benef_hash`, `ck_cuenta_benef_hash_completo`, `ck_cuenta_benef_verificada`, `uq_cuenta_benef_principal`, `ck_cuenta_bancaria_sin_claro`, `ck_cuenta_bancaria_version_llave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo_cuenta` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `entidad_financiera` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `numero_cuenta_cifrado` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `version_llave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `hash_numero_cuenta` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `numero_enmascarado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `titular_nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `titular_documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `es_principal` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado_verificacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `metodo_verificacion` | VARCHAR(20) | — | VARCHAR(20), opcional. | NINGUNA |
| `verificada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `bloqueada_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `deduccion_entrega`

**Qué es.** Cada descuento aplicado a la bolsa antes de entregarla, con su monto y su origen.

**Para qué sirve.** Es **la entidad que hace explicable la entrega**. Siete tipos, todos con un caso real detrás: - `APORTE_PROPIO_DEL_PERIODO`: el beneficiario también debe su aporte de este período. No tiene sentido cobrarle Bs 500 y pagarle Bs 6.000 el mismo día: se netea. Sin esta línea, la persona tendría que hacer una transferencia para recibir otra, con dos comisiones bancarias de por medio. - `DEUDA_VENCIDA_PROPIA` y `RECARGO_MORA_PROPIO`: **el turno es el momento de máxima capacidad de cobro de todo el ciclo.** Es la única vez que el sistema tiene plata del deudor en la mano. Descontarla ahí es lo que hace recuperable una cartera morosa. - `REPOSICION_FONDO_GARANTIA`: si el fondo cubrió un aporte suyo hace tres meses, ahora que cobra lo devuelve. - `RETENCION_IMPUESTO` y `COSTO_TRANSFERENCIA`: los descuentos que no decide nadie. - `COMISION_PLATAFORMA`: la comisión del servicio (M11). Es la única deducción que representa un ingreso de la empresa, y por eso es la que más rigor exige: `referenciaOrigenId` apunta al `cargo_comision`, que apunta al concepto y a la versión exacta del tarifario que la calculó. La pantalla puede decir *"Bs 18 — comisión por cobro de turno, 0,3 % de la bolsa"* y el usuario puede verificarlo contra el tarifario publicado. > **Vigencia ([[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]], D-04).** Por la regla de pozo completo la > comisión es un **cargo separado**: el principal se acredita íntegro y el cargo es una línea propia, no una > deducción de la bolsa. El camino con deducciones queda como legado. **Sigue sin haber deducción por comisión del organizador**: administrar no se cobra (RN-18). El cargo es para pagar el servicio de la plataforma, nunca para pagarle a quien administra el grupo. `referenciaOrigenId` apunta al hecho concreto que justifica cada descuento. Eso es lo que permite que la pantalla diga "Bs 500 — tu aporte del período 7" en vez de "deducciones: Bs 550", que es lo que genera desconfianza.

**Por qué existe.** Sin deducciones línea a línea, la única alternativa es un campo `total_descontado` que nadie puede auditar ni explicar. Y el beneficiario tiene todo el derecho a saber por qué recibió menos.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`. La referencian `tarifas.cargo_comision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `tipo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `referencia_origen_id` | UUID | — | UUID, opcional. | NINGUNA |
| `es_obligatoria` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `aplicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revertida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `entrega_fondo`

**Qué es.** El acto de entregar la bolsa de un período al beneficiario del turno.

**Para qué sirve.** Es el producto que el participante compró. Los tres montos que lleva cuentan toda la historia: - `montoBolsaBruto`: lo que se juntó entre todos. - `totalDeducciones`: lo que se le descuenta (y que hay que poder explicar línea por línea). - `montoNetoAEntregar`: lo que efectivamente recibe. Esa separación existe porque **la queja número uno en el momento de cobrar es "yo esperaba Bs 6.000 y me llegaron Bs 5.450"**. Si el sistema solo guarda el neto, no hay forma de responderla. Con las tres cifras y el detalle de deducciones, la respuesta es inmediata y verificable. La máquina de estados es intencionalmente conservadora. Note los dos estados de bloqueo separados: - `BLOQUEADA_POR_FONDO_INCOMPLETO`: falta plata en la bolsa. Es un problema del grupo (alguien no aportó) y dispara el módulo 8. - `BLOQUEADA_POR_VALIDACION`: la bolsa está pero algo del beneficiario no cierra (cuenta sin verificar, deuda propia sin plan, KYC vencido). Es un problema individual. Son dos conversaciones completamente distintas con dos personas distintas. Un solo estado "bloqueada" las confundiría. `RECHAZADA_POR_BENEFICIARIO` cubre un caso real: la persona ve el monto neto, no está de acuerdo con las deducciones y objeta antes de aceptar.

**Por qué existe.** Sin entrega como entidad, el desembolso sería un pago más y no habría dónde registrar el cálculo, las validaciones previas, la autorización ni la confirmación.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_bancaria_beneficiario`, `grupos.cupo`, `grupos.grupo`, `grupos.participante`, `grupos.periodo`, `grupos.turno`, `identidad.usuario`. La referencian `confirmacion_recepcion`, `deduccion_entrega`, `fondeo_entrega`, `garantia.abono_recuperacion`, `garantia.registro_incumplimiento`, `historial_estado_entrega`, `incidencia_entrega`, `orden_desembolso`, `validacion_pre_entrega`. Reglas nombradas: `ck_entrega_segregacion`, `uq_entrega_turno`, `uq_entrega_periodo`, `ck_entrega_neto`, `ck_entrega_neto_no_negativo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, obligatoria, referencia a `periodo`. | NINGUNA |
| `turno_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `beneficiario_participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `cuenta_destino_id` | UUID | — | UUID, opcional, referencia a `cuenta_bancaria_beneficiario`. | NINGUNA |
| `monto_bolsa_bruto` | DECIMAL(14,2) | lo que se juntó entre todos. | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `total_deducciones` | DECIMAL(14,2) | lo que se le descuenta (y que hay que poder explicar línea por línea). | DECIMAL(14,2), obligatoria. | NINGUNA |
| `monto_neto_a_entregar` | DECIMAL(14,2) | lo que efectivamente recibe. | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_efectivamente_entregado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `metodo_desembolso` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `fecha_programada` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_autorizacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `fecha_entrega` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `autorizada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `comprobante_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `hash_comprobante` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `observaciones` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `fondeo_entrega`

**Qué es.** De dónde sale cada boliviano del pozo de una entrega: cuánto está confirmado, cuánto cubrió el fondo mutual, cuánto faltó, cuánto cubrió la empresa y cuánto queda pendiente.

**Para qué sirve.** Que la persona que cobra su turno reciba el pozo completo (ADR-051) y que se sepa, al centavo, quién puso la plata que faltó y quién la sigue debiendo.

**Por qué existe.** El pozo completo exige separar la entrega (lo que recibe la persona) del fondeo (de dónde salió). Sin esta tabla, una cobertura empresarial se confundiría con un aporte pagado y la deuda que se conserva desaparecería.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`, `grupos.turno`. Reglas nombradas: `ck_fondeo_entrega_cuadra`, `ck_fondeo_entrega_pendiente`, `ck_fondeo_entrega_estado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `turno_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_pozo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_confirmado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cubierto_mutual` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_faltante` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cubierto_empresa` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_pendiente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `cobertura_respaldo_id` | UUID | — | UUID, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `corte_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fondeada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `historial_estado_entrega`

**Qué es.** Cada transición de la entrega, con motivo y autor.

**Para qué sirve.** Reconstruye la línea de tiempo: cuándo se programó, cuándo se bloqueó y por qué, quién la autorizó, cuándo se ejecutó, cuándo se confirmó. Es lo primero que se mira ante un reclamo de "me pagaron tarde", y es la única forma de distinguir un retraso del grupo (faltaba plata) de un retraso operativo (el desembolso se quedó trabado dos días).

**Por qué existe.** El campo `estado` dice dónde está; el historial dice cuánto tardó cada paso y quién lo movió. Sin él no hay métricas de tiempo de entrega ni responsables identificables. ---

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `estado_anterior` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `estado_nuevo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `ejecutado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `incidencia_entrega`

**Qué es.** Cualquier problema con una entrega, tipificado, con severidad, responsable y SLA.

**Para qué sirve.** Los ocho tipos son el catálogo de todo lo que puede salir mal en el momento más sensible: fondo incompleto, datos bancarios erróneos, desembolso rechazado, monto que no coincide, el beneficiario no recibió, entrega duplicada, reclamo de un tercero, sospecha de fraude. `slaHoras` y `fechaLimiteSLA` son lo que convierte el reclamo en algo con reloj. **Una entrega que no llega es la emergencia máxima de la plataforma**: la persona esperó meses su turno y contaba con esa plata para algo concreto. Un reclamo así no puede quedar en una bandeja sin dueño ni plazo. `ENTREGA_DUPLICADA` está tipificada aunque las restricciones de base de datos la hagan improbable: si alguna vez ocurre por una vía inesperada, hay que poder gestionarla, no descubrirla en el cierre contable.

**Por qué existe.** Sin incidencias, el problema en una entrega es una conversación de WhatsApp con soporte, sin trazabilidad, sin plazo y sin métrica. ---

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `tipo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `reportada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `asignada_a` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `sla_horas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_limite_sla` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resolucion` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `evidencias` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `abierta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `intento_desembolso`

**Qué es.** Cada ejecución de una orden de desembolso, con el resultado que devolvió el proveedor.

**Para qué sirve.** Guarda por qué falló y cuándo se puede reintentar (`reintentableEn`). Cuando el beneficiario reclama que no le llegó, el `mensajeProveedor` dice si fue "cuenta inexistente", "cuenta bloqueada" o "servicio no disponible" — tres diagnósticos que llevan a tres acciones distintas. `programarReintento()` implementa backoff: no tiene sentido reintentar cada segundo contra un banco caído.

**Por qué existe.** Sin intentos, un desembolso fallido es un estado sin explicación, y el soporte no puede decirle al usuario qué tiene que corregir. ---

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `orden_desembolso`. Reglas nombradas: `uq_intento_desembolso_numero`, `ck_intento_desembolso_fallo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `orden_desembolso_id` | UUID | — | UUID, obligatoria, referencia a `orden_desembolso`. | NINGUNA |
| `numero_intento` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `iniciado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `finalizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `resultado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `codigo_error` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `mensaje_proveedor` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `reintentable_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `oferta_turno`

**Qué es.** La publicación de un participante que ofrece vender su derecho a cobrar un turno.

**Para qué sirve.** Que quien necesita la plata antes (o después) pueda ceder su turno a otra persona por un precio, dentro de la plataforma y con reglas, en lugar de arreglos de palabra fuera del grupo.

**Por qué existe.** El derecho a cobrar un turno vale dinero. Sin una oferta registrada no hay forma de impedir que el mismo turno se venda dos veces ni de saber qué se ofreció, a qué precio y con qué cargos.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.cupo`, `grupos.grupo`, `grupos.participante`, `grupos.turno`. La referencian `cesion_derecho`. Reglas nombradas: `uq_oferta_turno_activa`, `ck_oferta_turno_cargos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `turno_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `participante_origen_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `vendedor_usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_derecho` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_precio` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cargos` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `publicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `orden_desembolso`

**Qué es.** La instrucción concreta al proveedor de pagos para transferir el neto a la cuenta del beneficiario.

**Para qué sirve.** Separa la decisión (la entrega autorizada) de la ejecución (la transferencia). Eso importa porque una transferencia puede fallar, ser devuelta por el banco o quedar en proceso durante horas, **sin que eso signifique que la entrega deba recalcularse**. La entrega ya está decidida; lo que se reintenta es el desembolso. `glosa` es lo que el beneficiario ve en su extracto bancario. Que diga "Pasanaku Los Amigos - Periodo 7" en vez de un código interno es una diferencia real para que la persona reconozca de dónde vino la plata.

**Por qué existe.** Sin orden separada, un desembolso fallido dejaría la entrega en un estado ambiguo. Además, una entrega puede tener varias órdenes: la primera rebotó por datos bancarios, la segunda fue a la cuenta corregida.

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.proveedor_pago`, `cuenta_bancaria_beneficiario`, `entrega_fondo`. La referencian `intento_desembolso`. Reglas nombradas: `uq_orden_desembolso_clave`, `ck_orden_desembolso_monto`, `ck_orden_desembolso_acreditada`, `uq_orden_desembolso_entrega_viva`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_pago`. | NINGUNA |
| `cuenta_destino_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_bancaria_beneficiario`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `referencia_proveedor` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `glosa` | VARCHAR(140) | — | VARCHAR(140), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `acreditada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `regla_entrega`

**Qué es.** El catálogo de controles previos, con su orden, si bloquean y quién puede omitirlos.

**Para qué sirve.** Permite endurecer o relajar los controles según el tipo de grupo y según la experiencia operativa, sin desplegar código. Si aparece un patrón de fraude nuevo (por ejemplo, cambios de cuenta bancaria justo antes del turno), se agrega una regla y aplica a todas las entregas siguientes. `esBloqueante` vs advertencia es la distinción clave: hay controles que deben detener la entrega y otros que solo deben avisar.

**Por qué existe.** Con reglas hardcodeadas, cada ajuste de control es un release y no queda registro de qué reglas estaban vigentes cuando se autorizó una entrega vieja. ---

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `validacion_pre_entrega`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `es_bloqueante` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `permite_omision` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `rol_que_puede_omitir` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `orden` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `validacion_pre_entrega`

**Qué es.** El resultado de aplicar cada regla de control antes de autorizar la entrega.

**Para qué sirve.** Es el **checklist ejecutado y guardado**. Las reglas típicas (RN-05): 1. Todos los aportes del período están pagados o cubiertos por el fondo. 2. El beneficiario no tiene deuda vencida sin plan de regularización. 3. La cuenta bancaria está verificada y a su nombre. 4. El KYC está vigente para el monto que se va a entregar. 5. No existe una entrega previa para el mismo turno. Guardar cada evaluación con `valorEsperado` y `valorObtenido` responde la pregunta del beneficiario bloqueado: *"¿por qué no me pagan?"* Respuesta: "faltan Bs 500 de la bolsa: se esperaban Bs 6.000 y hay Bs 5.500". `omitir(usuarioId, justificacion)` es deliberado y controlado. Siempre hay excepciones legítimas, pero omitir una validación bloqueante exige justificación escrita, queda con autor y va a la bitácora del módulo 9. `ReglaEntrega.rolQuePuedeOmitir` limita quién puede hacerlo.

**Por qué existe.** Sin persistir las validaciones, un bloqueo es un mensaje de error efímero. Con ellas, es un expediente: se sabe qué se revisó, cuándo, con qué resultado, y quién decidió saltarse qué. ---

**Sistema.** Vive en el esquema `entregas`; la escribe y la lee solo el servicio `entregas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `entrega_fondo`, `identidad.usuario`, `regla_entrega`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `entrega_id` | UUID | — | UUID, obligatoria, referencia a `entrega_fondo`. | NINGUNA |
| `regla_id` | UUID | — | UUID, obligatoria, referencia a `regla_entrega`. | NINGUNA |
| `resultado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `valor_esperado` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `valor_obtenido` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `es_bloqueante` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `omitida_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `justificacion_omision` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `evaluada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `erp`

### `activo_fijo`

**Qué es.** Un bien de uso concreto: laptop, servidor, mobiliario.

**Para qué sirve.** una empresa que compra servidores y equipamiento tiene que reconocer que ese gasto se consume con el tiempo, no todo el primer mes. Sin esto, el estado de resultados de enero se ve artificialmente golpeado por la compra de una laptop que en realidad sirve tres años.

**Por qué existe.** es un requisito contable básico para cualquier empresa formal, y hoy no hay ninguna tabla que lo represente.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `categoria_activo_fijo`, `centro_costo`, `factura_proveedor`. La referencian `depreciacion_activo`. Reglas nombradas: `ck_activo_fijo_depreciacion`, `ck_activo_fijo_residual`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `categoria_activo_fijo_id` | UUID | — | UUID, obligatoria, referencia a `categoria_activo_fijo`. | NINGUNA |
| `centro_costo_id` | UUID | — | UUID, opcional, referencia a `centro_costo`. | NINGUNA |
| `codigo_inventario` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `fecha_adquisicion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `costo_adquisicion` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `valor_residual` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `depreciacion_acumulada` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `valor_en_libros` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `factura_proveedor_id` | UUID | — | UUID, opcional, referencia a `factura_proveedor`. | NINGUNA |

### `asiento_plantilla`

**Qué es.** Una plantilla de asiento recurrente (alquiler, planilla mensual) para no recapturar el mismo asiento a mano cada mes.

**Por qué existe.** sin plantilla, un asiento recurrente se recaptura manualmente cada mes — fuente típica de error de tipeo en montos que después no cuadran. Sin `estado_financiero_generado`, cada vez que alguien pide "el balance de marzo" hay que recalcularlo desde cero contra la tabla completa de movimientos, lo cual es lento y — peor — puede dar un resultado distinto si se corre dos veces sobre datos que cambiaron entre medio (algo que no debería poder pasar en un período cerrado, pero la fotografía lo hace verificable).

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `linea_plantilla_asiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(100) | — | VARCHAR(100), obligatoria. | NINGUNA |
| `glosa` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `periodicidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `creada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |

### `categoria_activo_fijo`

**Qué es.** Catálogo: tipo de bien, vida útil, método de depreciación y las tres cuentas contables que le corresponden.

**Para qué sirve.** una empresa que compra servidores y equipamiento tiene que reconocer que ese gasto se consume con el tiempo, no todo el primer mes. Sin esto, el estado de resultados de enero se ve artificialmente golpeado por la compra de una laptop que en realidad sirve tres años.

**Por qué existe.** es un requisito contable básico para cualquier empresa formal, y hoy no hay ninguna tabla que lo represente.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `nucleo_financiero.cuenta_contable`. La referencian `activo_fijo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `vida_util_meses` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `metodo_depreciacion` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `cuenta_activo_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `cuenta_depreciacion_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `cuenta_gasto_depreciacion_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |

### `centro_costo`

**Qué es.** Unidad de costeo: un área, un producto, una campaña.

**Para qué sirve.** separa "cuánto cuesta el área de soporte" de "qué cuenta contable es". Sin centro de costo, una factura de proveedor de mensajería y una de infraestructura de servidores se ven igual en el balance — ambas son "5.2 Gastos operativos" — y nadie puede responder cuánto cuesta cada área del negocio por separado.

**Por qué existe.** sin presupuesto por centro de costo, "nos pasamos de presupuesto" es una sensación, no un hecho verificable. `partida_presupuestaria.monto_ejecutado` compara contra `monto_presupuestado` con datos, no con memoria.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `activo_fijo`, `factura_proveedor`, `orden_compra`, `presupuesto`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(100) | — | VARCHAR(100), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `cierre_periodo_contable`

**Qué es.** El acto de cerrar un mes: quién, cuándo, y el cuadre que quedó.

**Para qué sirve.** sin período, "cerrar el mes" es una convención verbal — alguien decide que enero "ya está" pero nada en el sistema lo impide. Con `periodo_contable.estado`, un asiento contra un período `CERRADO` se rechaza en el modelo, no en la buena voluntad de quien lo captura.

**Por qué existe.** la regulación boliviana (y cualquier auditoría) exige poder decir con certeza "los estados financieros de enero ya no cambiaron después del 5 de febrero". Sin un período que se cierra de verdad, esa afirmación no se puede sostener.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`, `periodo_contable`. Reglas nombradas: `ck_cierre_periodo_cuadrado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `periodo_contable_id` | UUID | — | UUID, obligatoria, referencia a `periodo_contable`. | NINGUNA |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `total_debe` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | NINGUNA |
| `total_haber` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | NINGUNA |
| `diferencia` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `observaciones` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |

### `cobro_cuenta_por_cobrar`

**Qué es.** El ingreso que la cancela.

**Por qué existe.** hasta este módulo, todo lo que la plataforma cobra pasa por la billetera de un `participante` (módulo 03/10). Pero la facturación de publicidad (módulo 14) es a un `anunciante` — que puede ser un `socio_comercial` externo sin billetera en la plataforma. `cuenta_por_cobrar` es el puente: `origen_tipo = 'FACTURA_PUBLICIDAD'` apunta a `factura_publicidad.id` (M14), y desde ahí todo el seguimiento de cobro es igual de riguroso que el de cualquier otra plata que entra: pendiente, cobrada parcial, cobrada, o declarada incobrable con motivo.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `cuenta_por_cobrar`, `nucleo_financiero.asiento_contable`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_por_cobrar_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_por_cobrar`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `fecha_cobro` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `forma_cobro` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |

### `cuenta_por_cobrar`

**Qué es.** Un cobro pendiente a un tercero, de origen polimórfico.

**Por qué existe.** hasta este módulo, todo lo que la plataforma cobra pasa por la billetera de un `participante` (módulo 03/10). Pero la facturación de publicidad (módulo 14) es a un `anunciante` — que puede ser un `socio_comercial` externo sin billetera en la plataforma. `cuenta_por_cobrar` es el puente: `origen_tipo = 'FACTURA_PUBLICIDAD'` apunta a `factura_publicidad.id` (M14), y desde ahí todo el seguimiento de cobro es igual de riguroso que el de cualquier otra plata que entra: pendiente, cobrada parcial, cobrada, o declarada incobrable con motivo.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `tercero_comercial`. La referencian `cobro_cuenta_por_cobrar`, `publicidad.factura_publicidad`. Reglas nombradas: `ck_cxc_cobrado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `origen_tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `origen_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `tercero_comercial_id` | UUID | — | UUID, opcional, referencia a `tercero_comercial`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_cobrado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `saldo_pendiente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria, calculada por la base, no se escribe. | FINANCIERO |
| `fecha_vencimiento` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `depreciacion_activo`

**Qué es.** Una corrida mensual de depreciación de un activo.

**Para qué sirve.** una empresa que compra servidores y equipamiento tiene que reconocer que ese gasto se consume con el tiempo, no todo el primer mes. Sin esto, el estado de resultados de enero se ve artificialmente golpeado por la compra de una laptop que en realidad sirve tres años.

**Por qué existe.** es un requisito contable básico para cualquier empresa formal, y hoy no hay ninguna tabla que lo represente.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `activo_fijo`, `nucleo_financiero.asiento_contable`, `periodo_contable`. Reglas nombradas: `uq_depreciacion_activo_periodo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `activo_fijo_id` | UUID | — | UUID, obligatoria, referencia a `activo_fijo`. | NINGUNA |
| `periodo_contable_id` | UUID | — | UUID, obligatoria, referencia a `periodo_contable`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `calculada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `ejercicio_fiscal`

**Qué es.** El año contable completo, con apertura y cierre.

**Para qué sirve.** sin período, "cerrar el mes" es una convención verbal — alguien decide que enero "ya está" pero nada en el sistema lo impide. Con `periodo_contable.estado`, un asiento contra un período `CERRADO` se rechaza en el modelo, no en la buena voluntad de quien lo captura.

**Por qué existe.** la regulación boliviana (y cualquier auditoría) exige poder decir con certeza "los estados financieros de enero ya no cambiaron después del 5 de febrero". Sin un período que se cierra de verdad, esa afirmación no se puede sostener.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `periodo_contable`, `presupuesto`. Reglas nombradas: `ck_ejercicio_fiscal_rango`, `ck_ejercicio_fiscal_cierre`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `anio` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `cerrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `estado_financiero_generado`

**Qué es.** Una fotografía de balance general o estado de resultados de un período, ya calculada y guardada.

**Por qué existe.** sin plantilla, un asiento recurrente se recaptura manualmente cada mes — fuente típica de error de tipeo en montos que después no cuadran. Sin `estado_financiero_generado`, cada vez que alguien pide "el balance de marzo" hay que recalcularlo desde cero contra la tabla completa de movimientos, lo cual es lento y — peor — puede dar un resultado distinto si se corre dos veces sobre datos que cambiaron entre medio (algo que no debería poder pasar en un período cerrado, pero la fotografía lo hace verificable).

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`, `periodo_contable`. Reglas nombradas: `uq_estado_financiero_periodo_tipo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `periodo_contable_id` | UUID | — | UUID, obligatoria, referencia a `periodo_contable`. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `generado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `generado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `datos` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `hash_contenido` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |

### `factura_proveedor`

**Qué es.** La cuenta por pagar: una factura recibida.

**Por qué existe.** `tercero_comercial` **no es** `usuario` ni `proveedor_pago` (módulo 03). `usuario` es alguien con billetera en la plataforma; `proveedor_pago` es la pasarela que procesa cobros. `tercero_comercial` es la imprenta que factura por las tarjetas de bienvenida, el proveedor de hosting, el estudio contable externo — gente a la que la empresa le compra, no que compra en la plataforma. Fusionarlo con `usuario` obligaría a darle una billetera a un proveedor de oficina, que no tiene sentido.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `centro_costo`, `identidad.usuario`, `nucleo_financiero.asiento_contable`, `orden_compra`, `tercero_comercial`. La referencian `activo_fijo`, `pago_a_proveedor`. Reglas nombradas: `uq_factura_proveedor_numero`, `ck_factura_proveedor_pagado`, `ck_factura_proveedor_vencimiento`, `ck_factura_proveedor_aprobacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tercero_comercial_id` | UUID | — | UUID, obligatoria, referencia a `tercero_comercial`. | NINGUNA |
| `orden_compra_id` | UUID | — | UUID, opcional, referencia a `orden_compra`. | NINGUNA |
| `centro_costo_id` | UUID | — | UUID, opcional, referencia a `centro_costo`. | NINGUNA |
| `numero_factura` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `fecha_emision` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_vencimiento` | DATE | — | DATE, obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_pagado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `saldo_pendiente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria, calculada por la base, no se escribe. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |

### `linea_plantilla_asiento`

**Qué es.** Una plantilla de asiento recurrente (alquiler, planilla mensual) para no recapturar el mismo asiento a mano cada mes.

**Por qué existe.** sin plantilla, un asiento recurrente se recaptura manualmente cada mes — fuente típica de error de tipeo en montos que después no cuadran. Sin `estado_financiero_generado`, cada vez que alguien pide "el balance de marzo" hay que recalcularlo desde cero contra la tabla completa de movimientos, lo cual es lento y — peor — puede dar un resultado distinto si se corre dos veces sobre datos que cambiaron entre medio (algo que no debería poder pasar en un período cerrado, pero la fotografía lo hace verificable).

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `asiento_plantilla`, `nucleo_financiero.cuenta_contable`. Reglas nombradas: `uq_linea_plantilla_orden`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `plantilla_id` | UUID | — | UUID, obligatoria, referencia a `asiento_plantilla`. | NINGUNA |
| `cuenta_contable_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `tipo_movimiento` | VARCHAR(5) | — | VARCHAR(5), obligatoria. | NINGUNA |
| `monto_referencial` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `orden` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `orden_compra`

**Qué es.** Una compra autorizada, previa a recibir la factura.

**Por qué existe.** `tercero_comercial` **no es** `usuario` ni `proveedor_pago` (módulo 03). `usuario` es alguien con billetera en la plataforma; `proveedor_pago` es la pasarela que procesa cobros. `tercero_comercial` es la imprenta que factura por las tarjetas de bienvenida, el proveedor de hosting, el estudio contable externo — gente a la que la empresa le compra, no que compra en la plataforma. Fusionarlo con `usuario` obligaría a darle una billetera a un proveedor de oficina, que no tiene sentido.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `centro_costo`, `identidad.usuario`, `tercero_comercial`. La referencian `factura_proveedor`. Reglas nombradas: `ck_orden_compra_aprobacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tercero_comercial_id` | UUID | — | UUID, obligatoria, referencia a `tercero_comercial`. | NINGUNA |
| `centro_costo_id` | UUID | — | UUID, opcional, referencia a `centro_costo`. | NINGUNA |
| `numero` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `pago_a_proveedor`

**Qué es.** El egreso que la cancela, total o parcial.

**Por qué existe.** `tercero_comercial` **no es** `usuario` ni `proveedor_pago` (módulo 03). `usuario` es alguien con billetera en la plataforma; `proveedor_pago` es la pasarela que procesa cobros. `tercero_comercial` es la imprenta que factura por las tarjetas de bienvenida, el proveedor de hosting, el estudio contable externo — gente a la que la empresa le compra, no que compra en la plataforma. Fusionarlo con `usuario` obligaría a darle una billetera a un proveedor de oficina, que no tiene sentido.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `factura_proveedor`, `identidad.usuario`, `nucleo_financiero.asiento_contable`. Reglas nombradas: `uq_pago_a_proveedor_clave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `factura_proveedor_id` | UUID | — | UUID, obligatoria, referencia a `factura_proveedor`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `fecha_pago` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `forma_pago` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `autorizado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `clave_idempotencia` | UUID | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | UUID, opcional. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |

### `partida_presupuestaria`

**Qué es.** Línea de presupuesto: cuánto se autorizó gastar en una cuenta contable, en un período, y cuánto se ejecutó.

**Para qué sirve.** separa "cuánto cuesta el área de soporte" de "qué cuenta contable es". Sin centro de costo, una factura de proveedor de mensajería y una de infraestructura de servidores se ven igual en el balance — ambas son "5.2 Gastos operativos" — y nadie puede responder cuánto cuesta cada área del negocio por separado.

**Por qué existe.** sin presupuesto por centro de costo, "nos pasamos de presupuesto" es una sensación, no un hecho verificable. `partida_presupuestaria.monto_ejecutado` compara contra `monto_presupuestado` con datos, no con memoria.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `nucleo_financiero.cuenta_contable`, `periodo_contable`, `presupuesto`. Reglas nombradas: `uq_partida_presupuesto_cuenta_periodo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `presupuesto_id` | UUID | — | UUID, obligatoria, referencia a `presupuesto`. | NINGUNA |
| `cuenta_contable_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `periodo_contable_id` | UUID | — | UUID, obligatoria, referencia a `periodo_contable`. | NINGUNA |
| `monto_presupuestado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_ejecutado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |

### `periodo_contable`

**Qué es.** Un mes dentro de un ejercicio.

**Para qué sirve.** sin período, "cerrar el mes" es una convención verbal — alguien decide que enero "ya está" pero nada en el sistema lo impide. Con `periodo_contable.estado`, un asiento contra un período `CERRADO` se rechaza en el modelo, no en la buena voluntad de quien lo captura.

**Por qué existe.** la regulación boliviana (y cualquier auditoría) exige poder decir con certeza "los estados financieros de enero ya no cambiaron después del 5 de febrero". Sin un período que se cierra de verdad, esa afirmación no se puede sostener.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `ejercicio_fiscal`. La referencian `cierre_periodo_contable`, `depreciacion_activo`, `estado_financiero_generado`, `nucleo_financiero.asiento_contable`, `partida_presupuestaria`. Reglas nombradas: `uq_periodo_contable_ejercicio_mes`, `ck_periodo_contable_rango`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `ejercicio_fiscal_id` | UUID | — | UUID, obligatoria, referencia a `ejercicio_fiscal`. | NINGUNA |
| `mes` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |

### `presupuesto`

**Qué es.** Cabecera de presupuesto de un centro de costo para un ejercicio.

**Para qué sirve.** separa "cuánto cuesta el área de soporte" de "qué cuenta contable es". Sin centro de costo, una factura de proveedor de mensajería y una de infraestructura de servidores se ven igual en el balance — ambas son "5.2 Gastos operativos" — y nadie puede responder cuánto cuesta cada área del negocio por separado.

**Por qué existe.** sin presupuesto por centro de costo, "nos pasamos de presupuesto" es una sensación, no un hecho verificable. `partida_presupuestaria.monto_ejecutado` compara contra `monto_presupuestado` con datos, no con memoria.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `centro_costo`, `ejercicio_fiscal`, `identidad.usuario`. La referencian `partida_presupuestaria`. Reglas nombradas: `uq_presupuesto_centro_ejercicio`, `ck_presupuesto_aprobacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `centro_costo_id` | UUID | — | UUID, obligatoria, referencia a `centro_costo`. | NINGUNA |
| `ejercicio_fiscal_id` | UUID | — | UUID, obligatoria, referencia a `ejercicio_fiscal`. | NINGUNA |
| `nombre` | VARCHAR(100) | — | VARCHAR(100), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `aprobado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `tercero_comercial`

**Qué es.** Un proveedor o cliente comercial: a quién la empresa le compra o le vende fuera del circuito del pasanaku.

**Por qué existe.** `tercero_comercial` **no es** `usuario` ni `proveedor_pago` (módulo 03). `usuario` es alguien con billetera en la plataforma; `proveedor_pago` es la pasarela que procesa cobros. `tercero_comercial` es la imprenta que factura por las tarjetas de bienvenida, el proveedor de hosting, el estudio contable externo — gente a la que la empresa le compra, no que compra en la plataforma. Fusionarlo con `usuario` obligaría a darle una billetera a un proveedor de oficina, que no tiene sentido.

**Sistema.** Vive en el esquema `erp`; la escribe y la lee solo el servicio `erp` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `nucleo_financiero.cuenta_contable`. La referencian `cuenta_por_cobrar`, `factura_proveedor`, `orden_compra`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `razon_social` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `numero_documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `email` | VARCHAR(120) | — | VARCHAR(120), opcional. | PERSONAL |
| `telefono` | VARCHAR(20) | — | VARCHAR(20), opcional. | PERSONAL |
| `cuenta_contable_id` | UUID | — | UUID, opcional, referencia a `cuenta_contable`. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `garantia`

### `abono_recuperacion`

**Qué es.** Cada pago que reduce una deuda, con su origen y su imputación.

**Para qué sirve.** El desglose `aplicadoACapital` / `aplicadoARecargos` importa mucho y es fuente frecuente de disputa: **si todo se imputa primero a recargos, el capital nunca baja** y la deuda se vuelve perpetua. La regla de imputación debe ser explícita y visible, no una decisión enterrada en el código. `origen` cubre las cinco formas reales de recuperar: - `PAGO_VOLUNTARIO`: pagó. - `DESCUENTO_DE_ENTREGA`: **la más efectiva de todas.** Cuando le toca cobrar su turno, se le descuenta. Es el único momento del ciclo en que el sistema tiene plata del deudor en la mano. - `EJECUCION_AVAL`: pagó el que respondía por él. - `ACUERDO_QUITA`: pagó una parte negociada. - `COMPENSACION`: se cruzó contra un saldo a favor.

**Por qué existe.** Append-only: los abonos no se editan. Si hubo un error, se registra la reversión (`revertido_en`). Es dinero: la trazabilidad no es opcional. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `aportes.pago`, `deuda_participante`, `entregas.entrega_fondo`, `identidad.usuario`, `movimiento_fondo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `deuda_id` | UUID | — | UUID, obligatoria, referencia a `deuda_participante`. | FINANCIERO |
| `pago_id` | UUID | — | UUID, opcional, referencia a `pago`. | NINGUNA |
| `entrega_id` | UUID | — | UUID, opcional, referencia a `entrega_fondo`. | NINGUNA |
| `movimiento_fondo_id` | UUID | — | UUID, opcional, referencia a `movimiento_fondo`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `origen` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `aplicado_a_capital` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `aplicado_a_recargos` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `saldo_resultante` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `revertido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `accion_cobranza`

**Qué es.** Cada intento de contacto, con su resultado y su costo.

**Para qué sirve.** Es **el registro de que se hizo el esfuerzo**, y eso importa en dos momentos: cuando la persona reclama que nunca la contactaron, y cuando hay que justificar el castigo de una deuda (hay que probar que se agotaron las instancias). `ResultadoContacto` con siete valores captura la información que realmente sirve: `TELEFONO_ERRONEO` (hay que actualizar datos, no insistir), `SOLICITA_PLAN` (quiere pagar, dale un plan), `DISPUTA_LA_DEUDA` (para la cobranza y abre descargo), `CONTACTADO_SE_NIEGA` (escalá). Un booleano "contactado" perdería todo eso. `AVISO_A_AVALISTA` como tipo de acción merece nota: es de las más efectivas, porque activa la presión social que sostiene el pasanaku. Y `costo` por acción es lo que permite calcular el retorno de cada canal de cobranza.

**Por qué existe.** Sin acciones registradas, la gestión de cobranza no se puede auditar, ni medir, ni defender. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `gestion_cobranza`, `identidad.usuario`, `notificaciones.notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `gestion_id` | UUID | — | UUID, obligatoria, referencia a `gestion_cobranza`. | NINGUNA |
| `notificacion_id` | UUID | — | UUID, opcional, referencia a `notificacion`. | NINGUNA |
| `etapa` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `canal` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `resultado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nota_gestor` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `costo` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `acuerdo_quita`

**Qué es.** El acuerdo de condonar parte de la deuda a cambio del pago del resto.

**Para qué sirve.** Es la salida negociada. Pero tiene un problema de fondo que la entidad resuelve explícitamente: **la plata que se condona sale del bolsillo de los otros participantes o del fondo que es de todos.** Por eso `acuerdoGrupoId` apunta a una votación del grupo (M2): la condonación **no es decisión del organizador ni del gestor**, es decisión de los que pierden esa plata. `justificacion` documenta el porqué, que es lo que se revisa si aparece un patrón de quitas sospechosamente frecuentes con ciertos deudores.

**Por qué existe.** Sin acuerdo formal, condonar sería un ajuste manual de saldo — el hueco por donde se va la plata en sistemas mal controlados. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.acuerdo`, `identidad.usuario`, `registro_incumplimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `acuerdo_grupo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `monto_original` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_condonado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_a_pagar` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `justificacion` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `alerta_temprana`

**Qué es.** Señales de comportamiento que anticipan un problema.

**Para qué sirve.** Los tres códigos son patrones observables y accionables: - `PAGA_CADA_VEZ_MAS_TARDE`: la tendencia importa más que el evento aislado. Pagó el día 3, luego el 5, luego el 8. Va camino a no pagar. - `NO_ABRE_MENSAJES`: dejó de leer los recordatorios (dato que viene de M5). Suele preceder al abandono, o indicar un problema de canal que hay que corregir antes de tratarlo como moroso. - `MULTIPLES_GRUPOS_EN_MORA`: se sobreextendió. Está en cuatro pasanakus y no puede con todos.

**Por qué existe.** Estas señales existen en los datos pero no en ninguna tabla si no se materializan. Y sin materializarlas, nadie actúa sobre ellas. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `identidad.usuario`. Reglas nombradas: `uq_alerta_temprana_abierta`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `generada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `apelacion_sancion`

**Qué es.** El recurso del sancionado, con instancia y plazo.

**Para qué sirve.** `instancia` con tres niveles (organizador → comité del grupo → soporte de la plataforma) es lo que hace real el derecho a apelar: apelar ante quien te sancionó no es apelar. `escalarInstancia()` permite subir cuando la primera instancia no resuelve. `fechaLimiteResolucion` protege al apelante de la táctica más simple para neutralizar una apelación: no resolverla nunca.

**Por qué existe.** Sin apelación, un error del sistema o del organizador es definitivo. Y los errores ocurren: expedientes abiertos por pagos mal conciliados son frecuentes al principio de la operación. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `sancion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `sancion_id` | UUID | — | UUID, obligatoria, referencia a `sancion`. | NINGUNA |
| `apelante_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `resuelta_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `argumento` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `evidencias` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `instancia` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `resolucion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `presentada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_limite_resolucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `aval_participante`

**Qué es.** El compromiso de una persona de responder por la deuda de otra, hasta cierto monto.

**Para qué sirve.** **Formaliza la práctica más antigua del pasanaku: quien te invita responde por vos.** En el pasanaku presencial esto es implícito y poderoso: si traés a alguien que no paga, el grupo te lo cobra a vos, socialmente. La entidad lo hace explícito, con monto máximo y aceptación firmada. `avalistaUsuarioId` suele coincidir con `participante.invitadoPorId` (M2): la cadena de confianza se convierte en cadena de responsabilidad. `montoMaximoAvalado` y `alcance` (total / un período / porcentaje) permiten que el aval sea proporcionado: nadie va a avalar de forma ilimitada a un conocido, pero sí puede avalar un período. `esParticipanteDelGrupo` distingue dos situaciones distintas: si el avalista está en el grupo, se le puede descontar de su propio turno; si es externo, hay que cobrarle por fuera.

**Por qué existe.** El aval es lo que hace viable prestarle confianza a alguien sin historial. Sin él, un usuario nuevo no puede entrar a ningún grupo serio y la plataforma no puede crecer más allá de los ya conocidos.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `grupos.participante`, `identidad.token_verificacion`, `identidad.usuario`. La referencian `ejecucion_aval`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `participante_avalado_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `avalista_usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `token_aceptacion_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | SECRETO |
| `es_participante_del_grupo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `monto_maximo_avalado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `alcance` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `porcentaje_responsabilidad` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `aceptado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `liberado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `candidato_reemplazo`

**Qué es.** Cada persona evaluada para ocupar el cupo vacante.

**Para qué sirve.** Permite comparar antes de decidir, y `fuenteCandidato` (lista de espera / invitación / emparejamiento) mide qué canal provee mejores reemplazos. `aceptaCondiciones` es explícito porque entrar a un grupo empezado tiene condiciones especiales: hay que ponerse al día con los períodos ya transcurridos.

**Por qué existe.** Sin candidatos registrados, el reemplazo es una decisión sin alternativas evaluadas, y no se puede medir la efectividad de cada canal. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `reemplazo_participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reemplazo_id` | UUID | — | UUID, obligatoria, referencia a `reemplazo_participante`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `puntaje_reputacion` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `acepta_condiciones` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fuente_candidato` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `capacidad_respaldo`

**Qué es.** El tope de dinero que la empresa está dispuesta a comprometer para cubrir faltantes de pozo, por moneda, y cuánto ya comprometió.

**Para qué sirve.** Que la promesa «el pozo se entrega completo» tenga un límite conocido y gobernado: la empresa no puede respaldar más grupos de los que puede pagar.

**Por qué existe.** Sin un tope explícito, cada grupo nuevo agregaría exposición sin que nadie la viera crecer. Dos grupos que se activan a la vez podrían comprometer la misma plata.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `reserva_respaldo`. Reglas nombradas: `ck_capacidad_respaldo_comprometido`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `ambito` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_tope` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_comprometido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `responsable_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `castigo_deuda`

**Qué es.** La baja contable de una deuda considerada incobrable.

**Para qué sirve.** Reconoce la realidad: hay deudas que no se van a cobrar, y mantenerlas como activo distorsiona los estados financieros del fondo. Los cinco motivos son honestos, incluido `COSTO_MAYOR_QUE_DEUDA` —perseguir Bs 300 cuesta más de Bs 300— y `FALLECIMIENTO`. Pero el campo decisivo es `mantieneRegistroReputacional = true`: > **Castigar no es perdonar.** Es una decisión contable, no un perdón. El registro reputacional se mantiene. Si el deudor aparece años después queriendo entrar a otro grupo, la deuda **se reactiva** (`reactivarSiPaga()`). Sin esta distinción, castigar una deuda sería una amnistía y crearía el incentivo perverso de aguantar hasta que te castiguen.

**Por qué existe.** Sin castigo formal, las deudas incobrables se acumulan para siempre en la cartera y el fondo aparenta tener activos que no existen. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `deuda_participante`, `identidad.usuario`, `nucleo_financiero.asiento_contable`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `deuda_id` | UUID | — | UUID, obligatoria, referencia a `deuda_participante`. | FINANCIERO |
| `aprobado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `monto_castigado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `justificacion` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `mantiene_registro_reputacional` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `cobertura_incumplimiento`

**Qué es.** El acto de que el fondo ponga la plata que un participante no puso.

**Para qué sirve.** Es **el momento en que el grupo se salva**. El beneficiario del período cobra completo, el grupo sigue, y el problema se traslada a un circuito paralelo que no lo detiene. `porcentajeCobertura` reconoce que la cobertura puede ser parcial: el fondo cubre el 80% y el beneficiario absorbe el 20%. Es una decisión de política que balancea la protección con la sostenibilidad del fondo. `requirioAprobacionManual` + `aprobadaPor`: montos altos no se cubren automáticamente. Es control interno básico. `generarDeuda()` es el paso conceptualmente crítico: **cubrir no es perdonar**. En el mismo acto en que el fondo pone la plata, nace una deuda del participante hacia el fondo. Si esos dos hechos no fueran atómicos, existiría el estado imposible de "el fondo pagó y nadie debe".

**Por qué existe.** Sin cobertura como entidad, el fondo sería un saldo que baja sin que quede constancia de por qué, para quién y quién lo autorizó.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.obligacion_aporte`, `fondo_garantia`, `grupos.periodo`, `identidad.usuario`, `movimiento_fondo`, `nucleo_financiero.asiento_contable`, `registro_incumplimiento`. La referencian `deuda_participante`, `subrogacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `fondo_id` | UUID | — | UUID, obligatoria, referencia a `fondo_garantia`. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, obligatoria, referencia a `obligacion_aporte`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, obligatoria, referencia a `periodo`. | NINGUNA |
| `movimiento_fondo_id` | UUID | — | UUID, opcional, referencia a `movimiento_fondo`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `monto_solicitado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cubierto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `porcentaje_cobertura` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `requirio_aprobacion_manual` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `motivo_rechazo` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `aplicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `cobertura_respaldo`

**Qué es.** El uso de la reserva de un grupo para completar el pozo de un turno, al corte, cuando no llegó toda la plata.

**Para qué sirve.** Que quien cobra el turno reciba el pozo completo aunque alguien no haya pagado, y que se sepa cuánto puso la empresa y cuánto recuperó después.

**Por qué existe.** La cobertura es todo o nada y genera un derecho de recuperación. Sin el registro no se distinguiría la plata puesta por la empresa de la pagada por los participantes.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `grupos.periodo`, `grupos.turno`, `identidad.usuario`, `reserva_respaldo`. La referencian `cobertura_respaldo_linea`. Reglas nombradas: `ck_cobertura_respaldo_cuadra`, `ck_cobertura_respaldo_recuperado`, `ck_cobertura_respaldo_estado`, `uq_cobertura_respaldo_turno_viva`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reserva_respaldo_id` | UUID | — | UUID, obligatoria, referencia a `reserva_respaldo`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, obligatoria, referencia a `periodo`. | NINGUNA |
| `turno_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_pozo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_confirmado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_cubierto_mutual` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_faltante` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_recuperado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `corte_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `solicitada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `aplicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `cobertura_respaldo_linea`

**Qué es.** Qué obligación impaga cubrió la cobertura y por cuánto, línea por línea.

**Para qué sirve.** Saber a quién se le cubrió cuánto, y por lo tanto quién le debe cuánto a la empresa. Sin la línea, la deuda sería un total sin dueño.

**Por qué existe.** La recuperación se cobra a cada incumplidor por lo que se le cubrió a él. Sin línea por obligación no se puede perseguir ni descontar la deuda correcta.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.obligacion_aporte`, `cobertura_respaldo`. La referencian `recuperacion_respaldo`. Reglas nombradas: `ck_cobertura_respaldo_linea_recuperada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cobertura_respaldo_id` | UUID | — | UUID, obligatoria, referencia a `cobertura_respaldo`. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, obligatoria, referencia a `obligacion_aporte`. | NINGUNA |
| `monto_cubierto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_recuperado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |

### `descargo_participante`

**Qué es.** La versión del participante: su argumento y sus pruebas.

**Para qué sirve.** **Es el derecho a ser oído**, y no es un formalismo. Los descargos legítimos son frecuentes y concretos: pagó pero la conciliación falló; el sistema le mostró la fecha equivocada; hubo un acuerdo verbal con el organizador; estuvo internado. Sin este canal, la única forma de reclamar es por WhatsApp al soporte, sin trazabilidad, sin plazo y sin criterio uniforme. Y sobre todo: **sin descargo, una sanción es indefendible ante cualquier reclamo formal.** `resueltoPor` + `resolucion` obligan a que alguien lo lea y responda con fundamento, no que se archive.

**Por qué existe.** Sancionar sin oír es arbitrario. Y en un sistema que maneja plata de la gente, la arbitrariedad es el riesgo reputacional más caro que existe. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.participante`, `identidad.usuario`, `registro_incumplimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `argumento` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `evidencias` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `resolucion` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `resuelto_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `presentado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `deuda_participante`

**Qué es.** Lo que un participante debe, como obligación exigible con vida propia.

**Para qué sirve.** El punto clave está en la nota del diagrama y merece subrayarse: **la deuda vive a nivel de usuario, no de participante.** > Si el grupo termina, la deuda sigue exigible. Esa es la diferencia entre el pasanaku digital y el de cuaderno. En el tradicional, cuando el grupo se disuelve, la deuda se evapora: no hay quién la cobre ni dónde esté registrada. Acá persiste, sigue generando recargos, bloquea el ingreso a nuevos grupos (vía `RestriccionUsuario` en M1) y puede recuperarse años después. `acreedor` distingue a quién se le debe: al fondo de garantía (si cubrió), al grupo (si no hubo cobertura) o a la plataforma. Es determinante para saber a quién le llega la recuperación. `esSubrogada` marca que la deuda cambió de acreedor. `fechaPrescripcion` reconoce que las deudas caducan legalmente: no se puede perseguir a alguien indefinidamente, y el sistema debe saber cuándo dejar de hacerlo.

**Por qué existe.** Sin deuda como entidad independiente, el incumplimiento se cierra cuando el grupo cierra, y quien abandonó no tiene ninguna consecuencia duradera.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cobertura_incumplimiento`, `grupos.grupo`, `grupos.participante`, `identidad.usuario`, `registro_incumplimiento`. La referencian `abono_recuperacion`, `castigo_deuda`, `ejecucion_aval`, `subrogacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `cobertura_id` | UUID | — | UUID, opcional, referencia a `cobertura_incumplimiento`. | NINGUNA |
| `acreedor` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `capital_original` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `recargos_acumulados` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `total_abonado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `saldo_actual` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `es_subrogada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fecha_exigibilidad` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_prescripcion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `dias_vencida` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `devolucion_fondo`

**Qué es.** El cálculo de cuánto le toca devolver a cada participante del fondo sobrante al cerrar el grupo.

**Para qué sirve.** Es lo que hace **justo** el fondo. Si el grupo terminó bien y sobró plata, esa plata **es de los participantes** y hay que devolverla a prorrata de lo aportado. `montoConsumido` es el ajuste de equidad: quien consumió cobertura (y no la repuso) recibe menos. Sin esa resta, el que nunca falló financiaría al que falló seguido, que es exactamente lo contrario del incentivo que se busca. `RETENIDA` con `motivoRetencion` cubre al participante que todavía debe: no se le devuelve mientras tenga deuda abierta; se compensa.

**Por qué existe.** Sin devolución explícita, el sobrante del fondo se quedaría en la plataforma, lo que sería —correctamente— percibido como apropiación. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `fondo_garantia`, `grupos.participante`. Reglas nombradas: `ck_devolucion_no_negativa`, `ck_devolucion_hasta_lo_aportado`, `ck_devolucion_cuadra`, `ck_devolucion_retencion_motivada`, `uq_devolucion_fondo_participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `fondo_id` | UUID | — | UUID, obligatoria, referencia a `fondo_garantia`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `monto_aportado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_consumido` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_a_devolver` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `motivo_retencion` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `disolucion_anticipada`

**Qué es.** El cierre del grupo antes de completar el ciclo, con la liquidación de todos.

**Para qué sirve.** Es el peor escenario y por eso hay que modelarlo bien. La pregunta que responde es la más difícil de todo el sistema: **si el grupo se rompe en el período 7 de 12, ¿quién le debe qué a quién?** Unos ya cobraron su turno y todavía deben aportes; otros aportaron siete veces y nunca cobraron. Sin un cálculo formal, esa liquidación es una discusión sin final —y es exactamente donde el pasanaku tradicional termina en pelea familiar o legal. `saldoADistribuir` es lo que queda para repartir después de cruzar todo.

**Por qué existe.** Sin disolución modelada, el cierre anticipado es un conflicto abierto sin árbitro ni cifras. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.acuerdo`, `grupos.grupo`. La referencian `liquidacion_participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `acuerdo_grupo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `causal` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `total_aportado_grupo` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_entregado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `saldo_a_distribuir` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `ejecucion_aval`

**Qué es.** El acto de exigirle al avalista que pague.

**Para qué sirve.** Es el momento más delicado del módulo: **se le está cobrando a alguien la deuda de otro**. Por eso `notificar()` y `plazoRespuesta` son obligatorios: el avalista tiene derecho a enterarse, a verificar y a responder antes de que se le exija. `generaDeudaDelAvalista` cierra el círculo con honestidad: si el avalista tampoco paga, se le abre **su propio expediente** de incumplimiento (tipo `INCUMPLIMIENTO_AVAL`). El aval no es simbólico; tiene consecuencias reales para quien lo firma. Y eso es lo que hace que la gente avale con criterio, que es justamente el filtro que se busca.

**Por qué existe.** Sin ejecución formal, el aval es una declaración de buenas intenciones que nadie hace cumplir — y por lo tanto no filtra nada. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.pago`, `aval_participante`, `deuda_participante`, `registro_incumplimiento`. Reglas nombradas: `uq_ejecucion_aval_registro`, `ck_ejecucion_aval_monto`, `ck_ejecucion_aval_plazo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `aval_id` | UUID | — | UUID, obligatoria, referencia a `aval_participante`. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `deuda_id` | UUID | — | UUID, obligatoria, referencia a `deuda_participante`. | FINANCIERO |
| `pago_id` | UUID | — | UUID, opcional, referencia a `pago`. | NINGUNA |
| `monto_ejecutado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `notificada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `plazo_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `genera_deuda_del_avalista` | BOOLEAN | — | BOOLEAN, obligatoria. | FINANCIERO |

### `estrategia_cobranza`

**Qué es.** Qué se hace en cada etapa: por qué canales, con qué frecuencia, con qué límite de contactos, si requiere gestor humano y si se puede negociar quita.

**Para qué sirve.** Dos equilibrios delicados: 1. **`maxContactosPorSemana` y `frecuenciaDias`.** Cobrar demasiado es contraproducente: genera bloqueos, quejas por spam y hostilidad. Y en muchas jurisdicciones el acoso en la cobranza está regulado. El límite protege al deudor y a la plataforma. 2. **`permiteQuita` + `quitaMaximaPorcentaje`.** Reconoce una verdad incómoda: **a veces cobrar el 70% ahora es mejor que perseguir el 100% durante dos años.** Pero la quita tiene que tener tope y autorización, o se convierte en la salida fácil de cualquier gestor. `requiereGestorHumano` marca dónde termina la automatización. Las primeras etapas son automáticas; a partir de cierto punto, una persona tiene que llamar.

**Por qué existe.** Sin estrategia parametrizada, la cobranza es improvisada, inconsistente entre casos y potencialmente abusiva. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `gestion_cobranza`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `etapa` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `dias_mora_desde` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `dias_mora_hasta` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `canales_permitidos` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `frecuencia_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `max_contactos_por_semana` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `plantilla_notificacion_codigo` | VARCHAR(50) | — | VARCHAR(50), opcional. | NINGUNA |
| `requiere_gestor_humano` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `permite_quita` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `quita_maxima_porcentaje` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `siguiente_etapa` | VARCHAR(20) | — | VARCHAR(20), opcional. | NINGUNA |

### `evidencia_incumplimiento`

**Qué es.** Los respaldos del expediente: captura del estado, extracto, comunicaciones, actas, logs del sistema, documentos.

**Para qué sirve.** **Sin evidencia, el expediente es una acusación.** Cuando se sanciona a alguien, o se ejecuta un aval, o se lo incluye en la lista de restricción, hay que poder mostrar en qué se basó. `LOG_SISTEMA` como tipo es importante: la evidencia más fuerte suele ser automática (el registro de que la obligación venció, de que se enviaron cinco recordatorios entregados y leídos, de que no hubo pago). Es objetiva y no depende de la palabra de nadie. `hashArchivo` + `esInmutable` + `sellar()` garantizan que la evidencia no se modificó después de aportada. Una evidencia editable no es evidencia.

**Por qué existe.** Es la diferencia entre un proceso defendible y uno arbitrario. Y ante un reclamo legal, es lo único que vale. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `registro_incumplimiento`. Reglas nombradas: `ck_evidencia_con_respaldo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `url_archivo` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `contenido_estructurado` | JSONB | — | JSONB, opcional. | NINGUNA |
| `aportada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `es_inmutable` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `fondo_garantia`

**Qué es.** El colchón económico que cubre los aportes impagos para que el grupo no se detenga.

**Para qué sirve.** **Es la innovación central del modelo digital frente al pasanaku tradicional.** En el pasanaku de siempre, si uno no pone, el que cobra recibe menos — y esa persona, que hizo todo bien, paga el error de otro. Es profundamente injusto y es lo que hace que la gente abandone. Con fondo de garantía, el sistema pone la diferencia: **el beneficiario cobra completo, y el que no pagó ahora le debe al fondo, no al grupo.** El conflicto deja de ser entre vecinos y pasa a ser entre el moroso y un mecanismo institucional. Eso solo ya cambia la dinámica social del grupo. El `ambito` define dos modelos de negocio distintos: - `POR_GRUPO`: cada grupo tiene su propio fondo, alimentado con un porcentaje de cada aporte. Lo que sobra al final se devuelve. - `MUTUAL_PLATAFORMA`: un fondo común entre todos los grupos. Diversifica mejor (un grupo con mala suerte no se queda sin cobertura) pero mezcla riesgos entre grupos que no se conocen. La separación `saldoDisponible` / `saldoComprometido` es contabilidad prudente: hay plata reservada para coberturas ya aprobadas pero no aplicadas. Sin esa distinción, el fondo aparentaría tener más de lo que realmente puede comprometer. `indiceSuficiencia()` y `proyectarAgotamiento()` son las funciones de alerta temprana: **un fondo que se va a agotar en dos períodos es un grupo que va a colapsar en tres**, y hay que saberlo antes.

**Por qué existe.** Sin fondo, la única respuesta al impago es que el beneficiario cobre menos. Con fondo, el grupo sigue funcionando y la deuda se persigue por separado.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `nucleo_financiero.cuenta_contable`, `politica_cobertura`. La referencian `cobertura_incumplimiento`, `devolucion_fondo`, `movimiento_fondo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `ambito` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `politica_cobertura_id` | UUID | — | UUID, obligatoria, referencia a `politica_cobertura`. | NINGUNA |
| `cuenta_contable_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `saldo_disponible` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_comprometido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `total_aportado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_cubierto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_recuperado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_castigado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `gestion_cobranza`

**Qué es.** El proceso activo de cobrarle a alguien, con su etapa, su gestor y su próxima acción.

**Para qué sirve.** **Convierte "hay que cobrarle" en un proceso con dueño, etapa y próximo paso.** Las seis etapas (preventiva → temprana → administrativa → prejudicial → judicial → castigo) son la práctica estándar de cobranza, y existen porque **cada etapa tiene un costo y una efectividad distintos**: un recordatorio automático cuesta centavos; una gestión judicial cuesta más que la mayoría de las deudas de un pasanaku. `costoAcumulado()` es la función que responde la pregunta económica clave: **¿vale la pena seguir cobrando?** Si llevo Bs 400 gastados en gestión para recuperar Bs 500, la respuesta es no, y corresponde castigar la deuda (con la constancia reputacional intacta). `proximaAccionEn` es lo que hace que la cobranza no se olvide: es el campo por el que se ordena la bandeja de trabajo diaria.

**Por qué existe.** Sin gestión materializada, cobrar depende de que alguien se acuerde. Y la cobranza que depende de la memoria no ocurre. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `estrategia_cobranza`, `identidad.usuario`, `registro_incumplimiento`. La referencian `accion_cobranza`, `promesa_pago`, `tarifas.cuenta_por_cobrar_comision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `estrategia_id` | UUID | — | UUID, obligatoria, referencia a `estrategia_cobranza`. | NINGUNA |
| `gestor_asignado_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `etapa_actual` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `monto_en_gestion` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `intentos_contacto` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `ultimo_contacto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `proxima_accion_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `abierta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `historial_estado_incumplimiento`

**Qué es.** La línea de tiempo del expediente: cada transición, con motivo, monto y autor.

**Para qué sirve.** Reconstruye la historia completa: detectado el 5, notificado el 6, gestión abierta el 8, promesa de pago el 12, promesa incumplida el 15, cobertura aplicada el 16, sanción propuesta el 20, descargo presentado el 22, sanción firme el 30. Esa línea de tiempo es lo que se presenta cuando alguien dice "nunca me avisaron" o "me sancionaron sin darme oportunidad". `esAutomatico` distingue lo que hizo el sistema de lo que hizo una persona. Importa: una escalada automática por vencimiento de plazo es distinta de una escalada decidida por un gestor.

**Por qué existe.** El campo `estado` dice dónde está; el historial dice cómo llegó y cuánto tardó cada paso. Es la base del debido proceso. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`, `registro_incumplimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `estado_anterior` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `estado_nuevo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_asociado` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `ejecutado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `es_automatico` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `historial_incumplimiento_usuario`

**Qué es.** La proyección agregada del comportamiento de un usuario **a través de todos los grupos**.

**Para qué sirve.** **Es la portabilidad del mal historial**, y es tan importante como la portabilidad del bueno. El problema que resuelve: alguien abandona un pasanaku debiendo tres cuotas, y a la semana siguiente entra a otro grupo en la misma plataforma como si nada. En el mundo presencial, la comunidad se entera; digitalmente, hay que construir esa memoria. `tasaRegularizacion` es el matiz que evita la condena permanente: no es lo mismo alguien que se atrasó cinco veces y regularizó las cinco, que alguien que se atrasó dos y nunca pagó. El primero es un mal pagador puntual; el segundo es un riesgo real. Sin esta métrica, ambos se ven igual. Se consulta al evaluar `SolicitudIngreso` (M2) y en el emparejamiento automático (RF-19).

**Por qué existe.** Sin agregado cross-grupo, cada grupo tendría que descubrir por su cuenta que el nuevo participante ya falló en otros tres.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `usuario_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `usuario`. | NINGUNA |
| `total_incumplimientos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `incumplimientos_leves` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `incumplimientos_graves` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `incumplimientos_abiertos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `monto_total_incumplido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_total_recuperado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_castigado_historico` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `grupos_abandonados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `ultimo_incumplimiento_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `dias_mora_promedio` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `tasa_regularizacion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `esta_en_lista_restriccion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `liquidacion_participante`

**Qué es.** La posición neta de cada participante en la disolución.

**Para qué sirve.** `posicionNeta` es el número que resuelve todo: **positiva, se le devuelve; negativa, debe.** Se calcula como total aportado menos total cobrado menos deuda pendiente. Es transparente, verificable y aplica el mismo criterio a todos. `estado = EN_COBRANZA` reconoce lo obvio: los que quedan debiendo pasan al circuito de cobranza como cualquier otra deuda, y no se evaporan porque el grupo se cerró.

**Por qué existe.** Sin liquidación individual, la disolución sería un reparto discrecional del saldo, y quien lo hiciera quedaría bajo sospecha inevitablemente. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `disolucion_anticipada`, `grupos.participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `disolucion_id` | UUID | — | UUID, obligatoria, referencia a `disolucion_anticipada`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `total_aportado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `total_cobrado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `deuda_pendiente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `posicion_neta` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `lista_restriccion_interna`

**Qué es.** El registro de usuarios con restricciones activas por incumplimiento, con su nivel.

**Para qué sirve.** Es el mecanismo ejecutivo del historial. Tres niveles graduados: - `OBSERVACION`: puede operar, pero se lo monitorea. - `LIMITADO`: puede seguir en sus grupos actuales pero no entrar a nuevos. - `VETADO`: no puede operar. `montoAdeudado` es el dato que permite la salida: **la restricción se levanta pagando.** No es una condena, es una consecuencia con remedio, y eso es deliberado: una restricción sin salida elimina todo incentivo a regularizar. `retiradoPor` + `motivoRetiro` documentan quién la levantó, que es la decisión más sensible del proceso.

**Por qué existe.** Sin lista, el historial es información y no consecuencia. Se materializa como `RestriccionUsuario` en M1, que es donde el resto del sistema la consulta. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `registro_incumplimiento`. Reglas nombradas: `ck_lista_retiro_motivado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `registro_origen_id` | UUID | — | UUID, opcional, referencia a `registro_incumplimiento`. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `nivel_restriccion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `monto_adeudado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `incluido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `retirado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `retirado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo_retiro` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |

### `matriz_sancion`

**Qué es.** La tabla que determina qué sanción corresponde a cada combinación de `tipo de incumplimiento × severidad × número de reincidencia`.

**Para qué sirve.** **Es la entidad que garantiza proporcionalidad.** > La sanción no la decide una persona de humor variable: sale de la matriz. Eso resuelve tres problemas de golpe: 1. **Consistencia**: dos personas con el mismo incumplimiento reciben la misma sanción, sin importar quién las evalúe ni si le caen bien al organizador. 2. **Previsibilidad**: se puede publicar la matriz y todos saben de antemano qué les pasa si incumplen. Eso tiene efecto disuasivo, que una sanción impredecible no tiene. 3. **Proporcionalidad**: el primer atraso leve es una advertencia; el tercero grave es expulsión. La escalada está codificada. `esAutomatica` vs `requiereRevisionHumana` marca el límite: las advertencias se aplican solas; una expulsión pasa por revisión.

**Por qué existe.** Sin matriz, sancionar es un juicio subjetivo, y en un grupo con dinero de por medio la subjetividad se lee siempre como favoritismo.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `politica_sancion`. La referencian `sancion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `politica_id` | UUID | — | UUID, obligatoria, referencia a `politica_sancion`. | NINGUNA |
| `tipo_incumplimiento` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `numero_reincidencia` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `tipo_sancion` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `valor` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | NINGUNA |
| `duracion_dias` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `es_automatica` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_revision_humana` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `movimiento_fondo`

**Qué es.** Cada entrada o salida del fondo, con el saldo resultante.

**Para qué sirve.** Es el extracto del fondo, y es lo que hace que el fondo sea **auditable por los participantes**, que son sus dueños. Los nueve tipos cuentan toda la historia: constitución, aportes, coberturas aplicadas, recuperaciones, ejecuciones de aval, devoluciones, castigos, ajustes, rendimientos. `saldoResultante` en cada movimiento permite verificar la secuencia completa sin recalcular: si un saldo resultante no coincide con el anterior más el monto, hubo manipulación.

**Por qué existe.** Es plata de los participantes. Si el fondo fuera solo un campo `saldo` que sube y baja, nadie podría verificar en qué se usó su aporte al fondo — y eso reproduce exactamente la opacidad que la plataforma quiere eliminar.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `fondo_garantia`, `identidad.usuario`, `nucleo_financiero.asiento_contable`. La referencian `abono_recuperacion`, `cobertura_incumplimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `fondo_id` | UUID | — | UUID, obligatoria, referencia a `fondo_garantia`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `saldo_resultante` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `movimiento_reserva`

**Qué es.** El libro inmutable de la reserva: cada reserva, ampliación, aplicación, reversa, recuperación o liberación, con el disponible y la exposición que dejó.

**Para qué sirve.** Que cualquier número del respaldo se pueda explicar paso a paso: por qué hoy quedan X bolivianos disponibles y cuánto está expuesto la empresa.

**Por qué existe.** Si los saldos de la reserva se editaran directo, un error o un reintento cambiaría el número sin dejar rastro. Con el libro, el saldo se reconstruye y se corrige con un movimiento inverso.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`, `reserva_respaldo`. La referencian `recuperacion_respaldo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reserva_respaldo_id` | UUID | — | UUID, obligatoria, referencia a `reserva_respaldo`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `disponible_resultante` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `exposicion_resultante` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `registrado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `responsable_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `plan_contingencia`

**Qué es.** La respuesta estructurada a una crisis del grupo.

**Para qué sirve.** Es el manual de emergencia. Cuatro disparadores (mora crítica, fondo agotado, abandono masivo, organizador inhabilitado) y cinco respuestas posibles: - `PRORRATEO_ENTRE_ACTIVOS`: los que quedan ponen la diferencia. - `REDUCCION_DE_BOLSA`: todos cobran menos, pero el grupo sigue. - `EXTENSION_DE_PLAZO`: se alargan los períodos. - `REEMPLAZO`: se busca quién entre. - `DISOLUCION`: se corta la pérdida. `simularImpacto()` es la función clave: **antes de someterlo a votación, cada participante ve cuánto le cuesta cada alternativa**. Votar a ciegas entre cinco opciones abstractas no es decidir. `requiereAcuerdo` + `acuerdoGrupoId`: la contingencia la aprueba el grupo (M2), porque todas las opciones implican que alguien pierde algo.

**Por qué existe.** Sin plan de contingencia, la crisis se maneja improvisando por WhatsApp, y el resultado suele ser que el grupo se desarma con pérdidas mal repartidas. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.acuerdo`, `grupos.grupo`. Reglas nombradas: `uq_plan_contingencia_vivo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `acuerdo_grupo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `disparador` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `impacto_estimado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `requiere_acuerdo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `propuesto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ejecutado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `politica_cobertura`

**Qué es.** Las reglas del fondo: cuánto se aporta, cuándo se activa la cobertura, cuánto cubre y con qué límites.

**Para qué sirve.** Cada parámetro protege contra un abuso concreto: - `porcentajeConstitucion`: cuánto de cada aporte alimenta el fondo. Sube el costo del pasanaku, así que es un equilibrio entre seguridad y precio. - `diasMoraParaActivar`: **no se cubre al primer día de atraso.** Si se cubriera de inmediato, el fondo pagaría por gente que simplemente se olvidó y va a pagar mañana, y se agotaría en meses. - `topeCoberturaPorParticipante` y `maxCoberturasPorParticipante`: **evitan el parásito**. Sin límite, alguien podría no pagar nunca y dejar que el fondo cubra siempre — convirtiendo el seguro colectivo en su financiamiento personal. - `topeCoberturaPorPeriodo`: protege contra el colapso masivo. Si en un período fallan seis de doce, el fondo no puede vaciarse cubriéndolos a todos; ahí corresponde un plan de contingencia, no cobertura. - `exigeAvalPrevio`: solo se cubre a quien tiene quién responda. - `requiereAprobacionManualDesde`: montos grandes pasan por revisión humana. - `plazoRecuperacionDias` y `tasaRecargoRecuperacion`: **el fondo no es un regalo**. Es un préstamo forzoso con plazo y con costo.

**Por qué existe.** Un fondo sin política es un fondo que se agota. Y configurarla por grupo permite que un pasanaku familiar sea generoso y uno con desconocidos sea estricto. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`. La referencian `fondo_garantia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `porcentaje_constitucion` | DECIMAL(5,2) | cuánto de cada aporte alimenta el fondo. Sube el costo del pasanaku, así que es un equilibrio entre seguridad y precio. | DECIMAL(5,2), obligatoria. | NINGUNA |
| `dias_mora_para_activar` | SMALLINT | **no se cubre al primer día de atraso.** Si se cubriera de inmediato, el fondo pagaría por gente que simplemente se olvidó y va a pagar mañana, y se agotaría en meses. | SMALLINT, obligatoria. | NINGUNA |
| `porcentaje_maximo_cobertura_por_aporte` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `tope_cobertura_por_participante` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `tope_cobertura_por_periodo` | DECIMAL(14,2) | protege contra el colapso masivo. Si en un período fallan seis de doce, el fondo no puede vaciarse cubriéndolos a todos; ahí corresponde un plan de contingencia, no cobertura. | DECIMAL(14,2), obligatoria. | NINGUNA |
| `max_coberturas_por_participante` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `exige_aval_previo` | BOOLEAN | solo se cubre a quien tiene quién responda. | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_aprobacion_manual_desde` | DECIMAL(14,2) | montos grandes pasan por revisión humana. | DECIMAL(14,2), obligatoria. | NINGUNA |
| `plazo_recuperacion_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `tasa_recargo_recuperacion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `politica_sancion`

**Qué es.** Las reglas del proceso sancionatorio: si requiere acuerdo del grupo, plazos de descargo y apelación, y prescripción.

**Para qué sirve.** Define las **garantías procesales**. `plazoDescargoDias` y `plazoApelacionDias` no son configuración técnica: son los derechos del sancionado. Que sean parametrizables por grupo permite que un grupo formal tenga plazos más largos y uno pequeño resuelva más rápido. `requiereAcuerdoGrupo` decide si las sanciones las aplica el organizador o las vota el grupo. Para expulsiones, votarlas es lo correcto.

**Por qué existe.** Sin política explícita, los plazos los decide quien sanciona — que es la definición de proceso arbitrario. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`. La referencian `grupos.configuracion_grupo`, `matriz_sancion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `requiere_acuerdo_grupo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `plazo_descargo_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `plazo_apelacion_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `prescribe_en_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `promesa_pago`

**Qué es.** El compromiso del deudor de pagar cierto monto en cierta fecha.

**Para qué sirve.** Es la unidad de trabajo real de toda cobranza. El gestor no cobra: **consigue promesas**, y después las hace cumplir. `estado` con `CUMPLIDA / INCUMPLIDA / PARCIAL` mide la variable más predictiva de todas: **una promesa incumplida vale más que diez días de mora** como señal de riesgo. Quien promete y no cumple no tiene un problema de liquidez, tiene un problema de intención — y eso cambia toda la estrategia. `canalCompromiso` registra dónde se comprometió (llamada, WhatsApp, app), lo que importa si después niega haberlo hecho.

**Por qué existe.** Sin promesas registradas no hay seguimiento, y "dijo que iba a pagar el viernes" se pierde en la memoria del gestor. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `gestion_cobranza`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `gestion_id` | UUID | — | UUID, obligatoria, referencia a `gestion_cobranza`. | NINGUNA |
| `monto_prometido` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `fecha_prometida` | DATE | — | DATE, obligatoria. | NINGUNA |
| `canal_compromiso` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `monto_efectivo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `registrada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `evaluada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `recuperacion_respaldo`

**Qué es.** Cada pago del incumplidor que devuelve a la reserva lo que la empresa cubrió por él.

**Para qué sirve.** Que la cobertura no sea un regalo: cuando el deudor paga, esa plata repone el respaldo y la deuda baja.

**Por qué existe.** Sin ella, un pago tardío no se podría atribuir a la cobertura que lo originó, y un mismo pago podría contarse dos veces como recuperación.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `aportes.pago`, `cobertura_respaldo_linea`, `movimiento_reserva`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cobertura_respaldo_linea_id` | UUID | — | UUID, obligatoria, referencia a `cobertura_respaldo_linea`. | NINGUNA |
| `pago_id` | UUID | — | UUID, obligatoria, referencia a `pago`. | NINGUNA |
| `movimiento_reserva_id` | UUID | — | UUID, obligatoria, referencia a `movimiento_reserva`. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `recuperada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `reemplazo_participante`

**Qué es.** La sustitución de un participante que salió por otro que entra al mismo cupo.

**Para qué sirve.** Es la solución preferida frente al abandono: mejor reemplazar que reducir la bolsa de todos. Los tres campos económicos son los que hacen posible el acuerdo: - `deudaAsumidaPorEntrante` y `deudaRetenidaPorSaliente`: **normalmente el entrante no asume la deuda vieja**, porque si la asumiera nadie querría entrar a reemplazar a un moroso. La deuda queda con el saliente y se persigue por M8. - `conservaOrdenDeTurno`: el entrante hereda la posición del cupo en el calendario. Es lo que permite que el resto del grupo no se vea afectado.

**Por qué existe.** Sin reemplazo formal, la salida de una persona obliga a recalcular el calendario de todos, que es inaceptable para once personas que no hicieron nada mal.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.acuerdo`, `grupos.cupo`, `grupos.grupo`, `grupos.participante`, `registro_incumplimiento`. La referencian `candidato_reemplazo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `registro_id` | UUID | — | UUID, opcional, referencia a `registro_incumplimiento`. | NINGUNA |
| `participante_saliente_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `participante_entrante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `acuerdo_grupo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `deuda_asumida_por_entrante` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `deuda_retenida_por_saliente` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `conserva_orden_de_turno` | BOOLEAN | el entrante hereda la posición del cupo en el calendario. Es lo que permite que el resto del grupo no se vea afectado. | BOOLEAN, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `registro_incumplimiento`

**Qué es.** El expediente completo de un hecho de incumplimiento.

**Para qué sirve.** **Esta es la entidad más importante del módulo y posiblemente del sistema entero.** Su justificación es directa: > Un booleano `es_moroso` no responde: **cuánto** debe, **desde cuándo**, **qué se > hizo** para cobrarle, **quién autorizó** la cobertura, **qué dijo él** en su > descargo, **qué sanción** se le aplicó y **si la apeló**. > El expediente responde todo eso, y es la prueba ante un reclamo o una auditoría. `codigoExpediente` (`INC-2026-000123`) es legible a propósito: se cita en notificaciones, en reclamos y en comunicaciones formales. "Tu expediente INC-2026-000123" es infinitamente más manejable que un UUID. Los doce tipos de incumplimiento no son variaciones del mismo hecho. Vale la pena notar los que un modelo ingenuo omitiría: - `PAGO_RECHAZADO_O_REVERSADO`: pagó y después el pago se cayó. **Técnicamente pagó, económicamente no.** - `COMPROBANTE_FALSO`: subió una imagen adulterada. Es fraude, no atraso. - `BENEFICIARIO_NO_CONTINUA_APORTANDO`: **el peor caso del pasanaku.** Cobró su turno y dejó de aportar. Ya se llevó la bolsa entera y ahora le debe a todos los que todavía no cobraron. Es cualitativamente distinto de atrasarse antes de cobrar, y tratarlos igual sería absurdo. - `INCUMPLIMIENTO_AVAL`: el que respondía por otro tampoco responde. `severidad` × `esReincidencia` × `numeroReincidencia` son las tres variables que alimentan la matriz de sanciones. **La cuarta vez no es como la primera**, y el modelo lo sabe. `origenDeteccion` importa para la calidad del proceso: no es lo mismo un incumplimiento detectado automáticamente por vencimiento que uno reportado por un participante (que puede tener conflicto con el señalado). El segundo exige más verificación. `fechaLimiteSubsanacion` es el reloj del debido proceso: la persona tiene un plazo para arreglar antes de que escale.

**Por qué existe.** Sin expediente, la morosidad es un estado y no un proceso. No se puede gestionar, ni probar, ni apelar, ni cerrar formalmente.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `aportes.obligacion_aporte`, `entregas.entrega_fondo`, `grupos.cupo`, `grupos.grupo`, `grupos.participante`, `grupos.periodo`, `identidad.usuario`. La referencian `acuerdo_quita`, `cobertura_incumplimiento`, `descargo_participante`, `deuda_participante`, `ejecucion_aval`, `evidencia_incumplimiento`, `gestion_cobranza`, `historial_estado_incumplimiento`, `lista_restriccion_interna`, `reemplazo_participante`, `sancion`. Reglas nombradas: `ck_incumplimiento_plazo_guardado`, `ck_incumplimiento_plazo_posterior`, `ck_incumplimiento_cierre_motivado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo_expediente` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, opcional, referencia a `periodo`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, opcional, referencia a `cupo`. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, opcional, referencia a `obligacion_aporte`. | NINGUNA |
| `entrega_afectada_id` | UUID | — | UUID, opcional, referencia a `entrega_fondo`. | NINGUNA |
| `responsable_gestion` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `origen_deteccion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_involucrado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_recuperado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_castigado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `dias_mora_al_detectar` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `dias_mora_actuales` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `es_reincidencia` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `numero_reincidencia` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `afecto_a_la_entrega` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `detectado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `notificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `fecha_limite_subsanacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_cierre` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `resumen_resolucion` | TEXT | — | TEXT, opcional. | NINGUNA |
| `reportado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `reserva_respaldo`

**Qué es.** Lo que la empresa aparta de su capacidad para un grupo y un ciclo concretos, antes de activarlo.

**Para qué sirve.** Que cada grupo activo tenga su respaldo separado y visible: cuánto se reservó, cuánto se usó para cubrir, cuánto se recuperó de los deudores y cuánto se liberó al terminar.

**Por qué existe.** La capacidad es global; la cobertura ocurre en un grupo. Sin la reserva por ciclo no se puede saber cuánto respaldo le queda a un grupo ni liberar lo que no se usó.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `capacidad_respaldo`, `grupos.grupo`, `identidad.usuario`. La referencian `cobertura_respaldo`, `movimiento_reserva`. Reglas nombradas: `ck_reserva_respaldo_uso`, `ck_reserva_respaldo_recuperado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `capacidad_respaldo_id` | UUID | — | UUID, obligatoria, referencia a `capacidad_respaldo`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `ciclo_numero` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_reservado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_aplicado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_recuperado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `monto_liberado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `responsable_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `reservada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `sancion`

**Qué es.** La sanción concreta aplicada a una persona por un expediente.

**Para qué sirve.** Los nueve tipos van de la advertencia a la inhabilitación total, e incluyen algunos específicos del contexto: - `PERDIDA_DE_PRIORIDAD_DE_TURNO`: te vas al final de la fila. Es de las más efectivas porque toca lo que más le importa a la gente en un pasanaku. - `SUSPENSION_DE_VOTO`: perdés voz en las decisiones del grupo mientras estés en falta. - `RETENCION_DE_ENTREGA`: no cobrás hasta regularizar. La máquina de estados **es el debido proceso hecho dato**: **La sanción solo se ejecuta cuando está `FIRME`**: antes hubo notificación, plazo de descargo y, si se apeló, resolución. Ejecutar una sanción antes de que quede firme es exactamente lo que la vuelve indefendible.

**Por qué existe.** Sin sanción como entidad con estados, la consecuencia sería inmediata y sin recurso — arbitraria por construcción.

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.acuerdo`, `grupos.participante`, `identidad.usuario`, `matriz_sancion`, `registro_incumplimiento`. La referencian `apelacion_sancion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registro_id` | UUID | — | UUID, obligatoria, referencia a `registro_incumplimiento`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `matriz_id` | UUID | — | UUID, opcional, referencia a `matriz_sancion`. | NINGUNA |
| `acuerdo_grupo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `aplicada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_recargo` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `impacto_reputacion` | DECIMAL(6,2) | — | DECIMAL(6,2), opcional. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `notificada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `firme_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `score_riesgo_incumplimiento`

**Qué es.** La probabilidad estimada de que un participante incumpla, con sus factores.

**Para qué sirve.** Es predictivo, no reactivo. Permite actuar **antes** del incumplimiento: ofrecer un plan de pago a quien se ve venir en problemas, reforzar el recordatorio, o simplemente no aceptarlo en un grupo de monto alto. `factoresPrincipales` (JSON) hace el modelo explicable: no basta con decir "riesgo alto", hay que poder decir por qué. Sin explicabilidad, un rechazo basado en el score es indefendible.

**Por qué existe.** Prevenir un incumplimiento cuesta una fracción de lo que cuesta gestionarlo: cobertura del fondo, gestión de cobranza, sanción, apelación, reemplazo. La prevención es el mejor negocio del módulo. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `probabilidad_incumplimiento` | DECIMAL(5,4) | — | DECIMAL(5,4), obligatoria. | NINGUNA |
| `factores_principales` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `nivel_riesgo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `accion_sugerida` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `calculado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `subrogacion`

**Qué es.** La formalización del cambio de acreedor: el fondo pagó, ahora el fondo es el acreedor.

**Para qué sirve.** Es la figura jurídica que **da fundamento legal a que el fondo cobre**. Sin subrogación formal, el fondo pagó una deuda ajena y no tendría título para reclamarla — que es exactamente el argumento que usaría un deudor bien asesorado. `documentoRespaldoUrl` guarda el instrumento que lo respalda.

**Por qué existe.** Sin subrogación documentada, la deuda con el fondo es una anotación interna sin sustento jurídico. Con ella, es exigible. ---

**Sistema.** Vive en el esquema `garantia`; la escribe y la lee solo el servicio `garantia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cobertura_incumplimiento`, `deuda_participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cobertura_id` | UUID | — | UUID, obligatoria, referencia a `cobertura_incumplimiento`. | NINGUNA |
| `deuda_id` | UUID | — | UUID, obligatoria, referencia a `deuda_participante`. | FINANCIERO |
| `acreedor_original` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `acreedor_subrogado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_subrogado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `documento_respaldo_url` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |

## Esquema `grupos`

### `aceptacion_reglamento`

**Qué es.** La firma de un participante sobre una versión concreta del reglamento.

**Para qué sirve.** Es el equivalente digital de la firma en el acta. Guarda `hashFirmado`, IP, fecha y el token de firma (M1). Cuando un participante sancionado dice "yo no sabía que se podía expulsar por dos cuotas impagas", esta fila responde: firmaste la versión 3 el 2 de febrero, y la cláusula estaba ahí.

**Por qué existe.** Sin ella no hay debido proceso posible: no se puede sancionar a alguien por incumplir reglas que no consta que haya aceptado. Es la base de todo el módulo 8.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.token_verificacion`, `participante`, `reglamento_grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `reglamento_id` | UUID | — | UUID, obligatoria, referencia a `reglamento_grupo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `aceptado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `hash_firmado` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `token_firma_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | SECRETO |

### `acuerdo`

**Qué es.** Una decisión colectiva sometida a votación: expulsar a alguien, cambiar la fecha de cobro, condonar una mora, disolver el grupo.

**Para qué sirve.** **Esta es la entidad que define quién manda en un pasanaku digital, y la respuesta es: el grupo, no el organizador.** Las ocho decisiones tipificadas en `TipoAcuerdo` tienen algo en común: todas afectan el dinero o los derechos de terceros. Expulsar a un moroso libera su cupo pero deja un hueco en la bolsa. Condonar una mora significa que los demás absorben esa pérdida. Cambiar el monto altera el compromiso que cada uno asumió. Ninguna de esas puede ser decisión unilateral de una persona, por más organizadora que sea. En el pasanaku presencial esto se resuelve en reunión. Digitalmente, `Acuerdo` es la reunión: se propone, se abre la votación, se cuenta con quórum, y **queda registrado como prueba ante disputas**. Cuando el expulsado reclame, la respuesta es: se votó, hubo quórum de 8 sobre 12, y estos son los votos.

**Por qué existe.** Sin acuerdos, la plataforma le entrega al organizador un poder que en el pasanaku real nunca tuvo, y reproduce digitalmente el abuso que promete evitar.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`. La referencian `garantia.acuerdo_quita`, `garantia.disolucion_anticipada`, `garantia.plan_contingencia`, `garantia.reemplazo_participante`, `garantia.sancion`, `tarifas.tarifa_congelada_grupo`, `traspaso_cupo`, `voto_participante`. Reglas nombradas: `uq_acuerdo_abierto`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(400) | — | VARCHAR(400), obligatoria. | NINGUNA |
| `propuesto_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `quorum_requerido` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |
| `votos_a_favor` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `votos_en_contra` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `abstenciones` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `referencia_afectada_id` | UUID | — | UUID, opcional. | NINGUNA |
| `abierto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cierra_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `ejecutado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `alta_grupo`

**Qué es.** El registro de que una persona creó un grupo con una clave de idempotencia dada.

**Para qué sirve.** Que tocar dos veces «Crear grupo» (o que la app reintente por mala señal) deje un solo grupo, y que la segunda vez la persona vea el mismo grupo creado en lugar de un error.

**Por qué existe.** Sin ella, un reintento duplicaría el grupo con sus cupos y su reglamento. Tampoco se podría distinguir un reintento legítimo de un pedido distinto mandado con la misma clave.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `creador_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | UUID | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | UUID, obligatoria. | NINGUNA |
| `huella_solicitud` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | NINGUNA |
| `fondo_por_periodo` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `configuracion_grupo`

**Qué es.** Los parámetros operativos finos del grupo, separados de su definición económica.

**Para qué sirve.** Cada campo aquí es una decisión que se toma una vez y afecta el comportamiento diario: - `permiteCuposMultiples` + `maxCuposPorPersona`: ¿se permite que alguien tenga dos manos? Es práctica habitual y a la vez es riesgo concentrado — si esa persona falla, el grupo pierde el doble. - `permitePermutaTurnos`: ¿pueden dos participantes intercambiar posiciones? Permitirlo da flexibilidad real (a alguien se le enfermó un familiar y necesita cobrar antes); prohibirlo evita el mercado gris de compra-venta de turnos. - `requiereAvalista`: exige que cada participante tenga quién responda por él (M8). Sube la fricción de entrada y baja drásticamente la morosidad. - `permiteIngresoTardio`: ¿puede entrar alguien con el grupo ya empezado? Tiene implicaciones económicas serias (debe ponerse al día con los períodos ya transcurridos). - `horaLimitePago` y `toleranciaMontoParcial`: ¿un pago a las 23:50 del día de vencimiento cuenta como puntual? ¿Bs 499 sobre Bs 500 cuenta como pagado? Son las dos preguntas que generan más discusiones reales, y por eso se responden por configuración y no por criterio de quien mire.

**Por qué existe.** Separada del `Grupo` porque son ~10 campos que casi nunca se consultan junto con la definición económica, y porque su cambio tiene otro nivel de autorización. `validarCoherencia()` evita combinaciones absurdas (permitir permutas pero exigir orden estricto por reputación, por ejemplo).

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.politica_mora`, `garantia.politica_sancion`, `grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `grupo_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `grupo`. | NINGUNA |
| `permite_cupos_multiples` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `max_cupos_por_persona` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `permite_permuta_turnos` | BOOLEAN | ¿pueden dos participantes intercambiar posiciones? Permitirlo da flexibilidad real (a alguien se le enfermó un familiar y necesita cobrar antes); prohibirlo evita el mercado gris de compra-venta de turnos. | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_avalista` | BOOLEAN | exige que cada participante tenga quién responda por él (M8). Sube la fricción de entrada y baja drásticamente la morosidad. | BOOLEAN, obligatoria. | NINGUNA |
| `permite_ingreso_tardio` | BOOLEAN | ¿puede entrar alguien con el grupo ya empezado? Tiene implicaciones económicas serias (debe ponerse al día con los períodos ya transcurridos). | BOOLEAN, obligatoria. | NINGUNA |
| `hora_limite_pago` | TIME | — | TIME, obligatoria. | NINGUNA |
| `tolerancia_monto_parcial` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | FINANCIERO |
| `politica_mora_id` | UUID | — | UUID, opcional, referencia a `politica_mora`. | NINGUNA |
| `politica_sancion_id` | UUID | — | UUID, opcional, referencia a `politica_sancion`. | NINGUNA |

### `criterio_emparejamiento`

**Qué es.** Los pesos con los que el motor decide qué tan compatibles son dos postulaciones: reputación, monto, geografía, historial común.

**Para qué sirve.** Permite calibrar el algoritmo según lo que la operación vaya aprendiendo. Si se detecta que los grupos geográficamente dispersos fallan más, se sube `pesoGeografia` sin tocar código. `maxMorososPorGrupo` es el control de riesgo más directo: no se arma un grupo con cuatro personas de historial dudoso, aunque individualmente pasen el mínimo.

**Por qué existe.** Un algoritmo de emparejamiento con constantes hardcodeadas no se puede ajustar frente a la evidencia. `vigenteDesde` permite saber con qué criterio se armó cada grupo histórico. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `propuesta_grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `peso_reputacion` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |
| `peso_monto` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | FINANCIERO |
| `peso_geografia` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |
| `peso_historial_comun` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |
| `reputacion_minima` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `max_morosos_por_grupo` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `cupo`

**Qué es.** Una cuota económica del grupo. La "mano". Numerada del 1 al N.

**Para qué sirve.** Es la unidad económica real del pasanaku. Cada cupo genera una obligación de aporte por período (M3) y tiene derecho a cobrar la bolsa exactamente una vez por ciclo (M2 → M4). `fraccion` permite el medio cupo: dos personas comparten una mano, cada una pone la mitad y cuando les toca se reparten la bolsa. Es común entre parientes o compañeros que no pueden con la cuota entera pero no quieren quedarse fuera. El caso que justifica toda la separación: **un participante abandona en el período 4 de 12**. Si los turnos colgaran de la persona, habría que reordenar el calendario de los otros once. Colgando del cupo, se libera el cupo (`LIBERADO_POR_INCUMPLIMIENTO`), entra un reemplazante y **hereda la posición**: el calendario del grupo no se mueve, y las diez personas que no hicieron nada mal no sufren consecuencias.

**Por qué existe.** Sin `Cupo`, el modelo no soporta manos múltiples, ni medias manos, ni reemplazos sin descuadre. Los tres casos son la norma, no la excepción.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `participante`. La referencian `aportes.obligacion_aporte`, `entregas.entrega_fondo`, `entregas.oferta_turno`, `garantia.reemplazo_participante`, `garantia.registro_incumplimiento`, `traspaso_cupo`, `turno`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `numero` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `fraccion` | DECIMAL(3,2) | — | DECIMAL(3,2), obligatoria. | NINGUNA |
| `asignado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `liberado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `decision_ingreso`

**Qué es.** Cada decisión humana sobre una solicitud de ingreso a un grupo, en dos fases: la propuesta y la resolución.

**Para qué sirve.** Que quién entra a un grupo de plata sea una decisión de una persona con nombre y motivo, no del algoritmo. Si la persona se aparta de lo que recomendó el motor, queda dicho.

**Por qué existe.** Con la admisión humana obligatoria (ADR-052 DD-2), el organizador propone y backoffice resuelve. Sin esta tabla, la propuesta, la resolución y el motivo se pisarían en la solicitud y no habría forma de reconstruir por qué alguien quedó dentro o fuera.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Reglas nombradas: `ck_decision_ingreso_fase`, `ck_decision_ingreso_resultado`, `ck_decision_ingreso_revision`, `ck_decision_ingreso_motivo`, `ck_decision_ingreso_recomendacion`, `ck_decision_ingreso_propuesta`, `uq_decision_ingreso_resolucion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `solicitud_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | UUID | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | UUID, obligatoria. | NINGUNA |
| `fase` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `decision` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `actor_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `motivo` | VARCHAR(1000) | — | VARCHAR(1000), obligatoria. | NINGUNA |
| `propuesta_id` | UUID | — | UUID, opcional. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional. | NINGUNA |
| `revision` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `evidencia_algoritmo` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `version_motor` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `recomendacion_algoritmo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `apartamiento` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ocurrida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `correlacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |

### `enlace_grupo`

**Qué es.** El enlace —y su QR— que el organizador comparte para que **cualquier persona** pida unirse al grupo, sin tener que invitar teléfono por teléfono.

**Para qué sirve.** Es como se arma un pasanaku en la práctica: el organizador pega un enlace en el grupo de WhatsApp de la familia o muestra el QR en persona. Como el enlace se puede reenviar a desconocidos, **no da entrada**: cada pedido es una `solicitud_ingreso` que el organizador aprueba (decisión del dueño, 2026-10-19). Vence a los 7 días o al llenarse el grupo, y el organizador lo puede anular cuando quiera.

**Por qué existe.** Sin entidad propia no se podría anular un enlace que se filtró, ni saber por qué enlace llegó cada solicitud (`solicitud_ingreso.enlace_grupo_id`), ni garantizar que el grupo tenga un solo enlace vivo.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`. La referencian `solicitud_ingreso`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `emisor_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `token_hash` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_creacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_expiracion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_anulacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `grupo`

**Qué es.** El pasanaku en sí: monto del aporte, cada cuánto se aporta, cuántos cupos, cuándo empieza, con qué reglas.

**Para qué sirve.** Es el contrato. Todo lo demás cuelga de acá. Los campos que más importan y por qué: - `montoAporte` + `periodicidad` + `numPeriodos` + `cuposTotales` son los cuatro números que definen el negocio completo: cuánto pongo, cada cuánto, por cuánto tiempo, y cuánto me llevo cuando me toque. `montoBolsaPorPeriodo()` deriva de ellos y es la cifra que la gente realmente quiere saber. - `esAutogestionado` + `organizadorId` distinguían dos modelos de negocio: el grupo que administra una persona habilitada (M7) y el que administra el sistema solo. Por [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] (D-12) **solo existe el primero**: todo grupo tiene un organizador humano habilitado y el organizador digital únicamente automatiza tareas. En ningún caso el organizador cobra comisión: administrar no se cobra (RN-18). La comisión que sí existe es la de la plataforma por el servicio (M11), que se congela por grupo al constituirlo (`tarifa_congelada_grupo`) para que el precio no cambie a mitad del juego. - `requiereKYCMinimo` y `reputacionMinima` son **filtros de entrada**. Un grupo de Bs 200 entre amigos no pide nada; uno de USD 500 entre desconocidos exige documento verificado y buen historial. Sin estos campos, todos los grupos tendrían el mismo nivel de exigencia y la plataforma solo podría servir al segmento más bajo o al más alto, nunca a ambos. - `diasGracia` y `aplicaRecargoMora` codifican la tolerancia del grupo. En un pasanaku de barrio la gracia es de una semana y nadie cobra recargo; en uno formal, el recargo es la única forma de que la gente pague a tiempo. - `usaFondoGarantia` + `porcentajeFondoGarantia` deciden si el grupo se autoasegura (M8). Es la diferencia entre "si uno no paga, el beneficiario del período cobra menos" y "si uno no paga, el fondo cubre y el beneficiario cobra completo". - `quorumDecisiones` define cuánta gente hace falta para aprobar un cambio. Es lo que impide que tres personas decidan expulsar a un cuarto.

**Por qué existe.** Es la unidad de negocio. Sin ella no hay contexto para ninguna obligación, ningún turno ni ninguna entrega.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `organizador.organizador`. La referencian `acuerdo`, `aportes.obligacion_aporte`, `aportes.politica_mora`, `auditoria.alerta_cumplimiento`, `auditoria.ejecucion_reporte`, `catalogo.dia_no_habil`, `comun.bitacora_evento`, `configuracion_grupo`, `cupo`, `enlace_grupo`, `entregas.entrega_fondo`, `entregas.oferta_turno`, `garantia.alerta_temprana`, `garantia.aval_participante`, `garantia.cobertura_respaldo`, `garantia.deuda_participante`, `garantia.disolucion_anticipada`, `garantia.fondo_garantia`, `garantia.plan_contingencia`, `garantia.politica_cobertura`, `garantia.politica_sancion`, `garantia.reemplazo_participante`, `garantia.registro_incumplimiento`, `garantia.reserva_respaldo`, `garantia.score_riesgo_incumplimiento`, `historial_estado_grupo`, `invitacion`, `notificaciones.programacion_recordatorio`, `nucleo_financiero.asiento_contable`, `nucleo_financiero.cuenta_billetera`, `nucleo_financiero.cuenta_contable`, `nucleo_financiero.transaccion_billetera`, `nucleo_financiero.transferencia_p2p`, `organizador.tarea_automatizada`, `participante`, `periodo`, `propuesta_grupo`, `reglamento_grupo`, `solicitud_ingreso`, `sorteo_turnos`, `tarifas.asignacion_tarifario`, `tarifas.devengo_comision`, `tarifas.exencion_comision`, `tarifas.tarifa_congelada_grupo`, `transparencia.bloque_transparencia`, `transparencia.evento_reputacion`, `transparencia.metrica_grupo`, `transparencia.resena_participante`, `turno`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo_publico` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `monto_aporte` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `periodicidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `dia_cobro` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `num_periodos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `cupos_totales` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `cupos_ocupados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin_estimada` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `tipo_conformacion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `modalidad_turnos` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `visibilidad` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, opcional, referencia a `organizador`. | NINGUNA |
| `es_autogestionado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_kyc_minimo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `reputacion_minima` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `dias_gracia` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `aplica_recargo_mora` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `usa_fondo_garantia` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `porcentaje_fondo_garantia` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `quorum_decisiones` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |
| `cancelado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `historial_estado_grupo`

**Qué es.** Cada transición de estado del grupo, con motivo y autor.

**Para qué sirve.** Responde: *¿por qué este grupo está suspendido, quién lo suspendió y cuándo?* Un grupo suspendido significa que doce personas no pueden aportar ni cobrar; nadie debería poder hacerlo sin dejar rastro y sin motivo.

**Por qué existe.** El campo `estado` dice dónde está el grupo hoy; esta tabla dice cómo llegó ahí. Ante un reclamo colectivo, es lo primero que se mira. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `estado_anterior` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `estado_nuevo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `ejecutado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `invitacion`

**Qué es.** La invitación a un teléfono concreto para unirse a un grupo, con su enlace firmado.

**Para qué sirve.** Es **el canal de crecimiento principal**. Así es como realmente se arma un pasanaku: alguien manda el enlace al grupo de WhatsApp de la familia o del trabajo. La invitación se hace a un `telefonoInvitado`, no a un usuario registrado, justamente porque la mayoría de los invitados todavía no existe en la plataforma — la invitación es lo que los trae. `enviosRealizados` y `reenviar()` reconocen algo cotidiano: la gente no contesta a la primera. `fechaExpiracion` (típicamente 72 h) evita que un enlace circule indefinidamente y que alguien se cuele a un grupo que ya arrancó.

**Por qué existe.** Sin invitación como entidad, no se puede medir el embudo (cuántos invitados → cuántos aceptan), ni revocar un enlace mandado por error a un número equivocado, ni saber quién invitó a quién — que es la cadena de confianza que el aval del módulo 8 necesita.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.token_verificacion`, `identidad.usuario`. Reglas nombradas: `uq_invitacion_token`, `ck_invitacion_expira`, `ck_invitacion_respuesta`, `uq_invitacion_activa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `telefono_invitado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `nombre_sugerido` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `emisor_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `token_id` | UUID | — | UUID, obligatoria, referencia a `token_verificacion`. | SECRETO |
| `consumidor_id` | UUID | — | UUID, opcional. | NINGUNA |
| `clave_consumo` | UUID | — | UUID, opcional. | SECRETO |
| `solicitud_ingreso_id` | UUID | — | UUID, opcional. | NINGUNA |
| `huella_canje` | VARCHAR(64) | — | VARCHAR(64), opcional. | NINGUNA |
| `canal` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `envios_realizados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_envio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_expiracion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_respuesta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `participante`

**Qué es.** Una persona dentro de un grupo específico, con su estado y su historia en ese grupo.

**Para qué sirve.** Es la membresía. Su máquina de estados (`INVITADO → POSTULANTE → ACEPTADO_PENDIENTE_FIRMA → ACTIVO → EN_MORA → …`) es el recorrido real de alguien que entra a un pasanaku: lo invitan, postula, lo aceptan, firma, participa, eventualmente se atrasa, y termina de una de cuatro maneras (retirado, reemplazado, expulsado, o el grupo termina bien). `invitadoPorId` es un campo pequeño con consecuencias grandes: construye la **cadena de confianza**. Quien te invitó respondió por vos, y el módulo 8 usa justamente ese vínculo para el aval solidario. Es la digitalización del "yo lo traje, yo respondo". `reputacionAlIngresar` congela el score del día de entrada. Sirve para dos cosas: evaluar después si el filtro de reputación funcionó (¿los que entraron con score bajo fallaron más?), y para no juzgar retroactivamente a alguien cuyo puntaje bajó por hechos posteriores en otro grupo. `alias` existe porque en un grupo de trabajo la gente se conoce por apodo, y mostrar el nombre completo de la cédula es a la vez incómodo y una filtración innecesaria de datos personales al resto del grupo.

**Por qué existe.** Porque el mismo usuario está en varios grupos con estados distintos: al día en uno, en mora en otro. El estado es propiedad de la relación, no de la persona.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`, `participante`. La referencian `aceptacion_reglamento`, `aportes.obligacion_aporte`, `aportes.plan_regularizacion`, `cupo`, `entregas.cesion_derecho`, `entregas.entrega_fondo`, `entregas.oferta_turno`, `garantia.aval_participante`, `garantia.descargo_participante`, `garantia.deuda_participante`, `garantia.devolucion_fondo`, `garantia.liquidacion_participante`, `garantia.reemplazo_participante`, `garantia.registro_incumplimiento`, `garantia.sancion`, `nucleo_financiero.cuenta_contable`, `participante`, `solicitud_permuta`, `solicitud_retiro`, `tarifas.devengo_comision`, `transparencia.evento_reputacion`, `transparencia.resena_participante`, `traspaso_cupo`, `voto_participante`. Reglas nombradas: `uq_participante_vigente_grupo_usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `alias` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `es_organizador` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `invitado_por_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `fecha_ingreso` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_salida` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_salida` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |
| `reputacion_al_ingresar` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `aportes_realizados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `aportes_en_mora` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `periodo`

**Qué es.** Un ciclo de aporte del grupo: del 1 al N, con sus fechas de vencimiento y gracia, y cuánto se recaudó.

**Para qué sirve.** Es la unidad de tiempo del pasanaku y el punto de control financiero. `montoObjetivo` vs `montoRecaudado` responde la pregunta que más importa cada mes: **¿está completa la bolsa para poder entregarla?** `brechaFinanciamiento()` es la cifra que dispara todo el módulo 8 — si faltan Bs 500, o alguien los pone, o el fondo de garantía los cubre, o el beneficiario cobra menos. Las tres fechas (`fechaLimitePago`, `fechaFinGracia`, `fechaEntregaPrevista`) existen porque son tres momentos distintos con consecuencias distintas: pasada la primera se es impuntual, pasada la segunda se entra en mora formal y se activa la cobranza, y en la tercera hay que entregar sí o sí. `cuposMorosos` es un contador denormalizado a propósito: el panel de transparencia (M6) y las alertas de riesgo lo consultan constantemente.

**Por qué existe.** Sin períodos materializados, el calendario sería un cálculo en tiempo de ejecución y no se podría registrar qué pasó realmente en cada uno: cuánto entró, quién falló, si se entregó a tiempo.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`. La referencian `aportes.obligacion_aporte`, `entregas.entrega_fondo`, `garantia.cobertura_incumplimiento`, `garantia.cobertura_respaldo`, `garantia.registro_incumplimiento`, `transparencia.metrica_grupo`, `turno`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `numero` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_limite_pago` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin_gracia` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_entrega_prevista` | DATE | — | DATE, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `monto_objetivo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_recaudado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `cupos_morosos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `postulacion_emparejamiento`

**Qué es.** La declaración de un usuario de que quiere entrar a *algún* grupo con ciertas características, sin tener uno concreto en mente.

**Para qué sirve.** Es la demanda en el marketplace. La persona dice: quiero aportar entre Bs 300 y Bs 500, mensual, empezando en marzo, y prefiero cobrar temprano. `preferenciaTurno` es un campo pequeño con mucho valor: quien quiere cobrar temprano busca liquidez (está pidiendo un préstamo), quien prefiere tarde busca ahorrar. Emparejar dos perfiles complementarios hace grupos más estables que emparejar doce personas que todas quieren cobrar primero.

**Por qué existe.** Sin postulaciones no hay pool de demanda y el emparejamiento no tiene con qué trabajar. `vigenteHasta` evita proponer grupos a gente que ya perdió el interés hace meses. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `propuesta_postulacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `monto_deseado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `rango_monto_min` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `rango_monto_max` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `periodicidad_deseada` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `fecha_inicio_deseada` | DATE | — | DATE, obligatoria. | NINGUNA |
| `preferencia_turno` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `tolerancia_riesgo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `propuesta_grupo`

**Qué es.** Un grupo candidato: un conjunto de postulaciones que el motor considera compatibles, todavía sin confirmar.

**Para qué sirve.** Es el paso intermedio imprescindible: **el sistema propone, la gente acepta**. Nadie debe ser metido automáticamente a un grupo con desconocidos. La propuesta se notifica a los candidatos, se cuentan las aceptaciones (`propuesta_postulacion.acepto`), y solo si se llega al cupo completo antes de `expiraEn` la propuesta se `materializa()` en un `Grupo` real. `puntajeCohesion` y `riesgoEstimado` son los números que se le muestran al candidato para que decida con información: "este grupo tiene riesgo estimado bajo, 9 de 12 personas ya aceptaron".

**Por qué existe.** Sin propuesta como entidad separada, un emparejamiento fallido (5 aceptaron, 7 no) dejaría grupos a medio armar en la tabla real. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `criterio_emparejamiento`, `grupo`. La referencian `propuesta_postulacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `criterio_id` | UUID | — | UUID, obligatoria, referencia a `criterio_emparejamiento`. | NINGUNA |
| `monto_aporte` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `periodicidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `puntaje_cohesion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `riesgo_estimado` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `aceptaciones_recibidas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `grupo_materializado_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |

### `propuesta_postulacion`

**Qué es.** La relación entre una propuesta y cada postulación invitada, con su respuesta.

**Para qué sirve.** Guarda quién aceptó y quién no, y cuándo. Permite medir la tasa de conversión del emparejamiento y reintentar con los que no respondieron. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `postulacion_emparejamiento`, `propuesta_grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `propuesta_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `propuesta_grupo`. | NINGUNA |
| `postulacion_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `postulacion_emparejamiento`. | NINGUNA |
| `acepto` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `respondido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `reglamento_grupo`

**Qué es.** El texto de las reglas del grupo, versionado y sellado con hash.

**Para qué sirve.** Es el documento que la gente firma. Contiene lo que la configuración no puede expresar en números: qué pasa si alguien abandona, cómo se reparte si el grupo se disuelve, qué se considera falta grave. Cuando hay conflicto —y en pasanakus lo hay— este es el texto al que se apela. El versionado (`version`, `vigenteDesde/Hasta`) importa porque los reglamentos se modifican a mitad de ciclo. `requiereReaceptacion()` decide si un cambio es tan sustancial que hay que pedir que todos vuelvan a firmar.

**Por qué existe.** `hashContenido` es lo que convierte el reglamento en prueba: sin él, cualquiera podría alegar que el texto se editó después de que lo firmó. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`. La referencian `aceptacion_reglamento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `contenido` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `hash_contenido` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `clausulas_mora` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `clausulas_abandono` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `redactado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |

### `snapshot_sorteo`

**Qué es.** La foto congelada del plantel, el calendario y las reglas de un grupo en el momento en que se compromete el sorteo de turnos.

**Para qué sirve.** Que nadie pueda decir que el sorteo se hizo sobre otra lista de participantes o con otras reglas. Cualquiera puede reproducir el orden con la semilla revelada y esta foto.

**Por qué existe.** El sorteo publica primero el hash de la semilla y después la revela (CU-60). Si el plantel o las reglas cambiaran entre esos dos momentos, el resultado dejaría de ser verificable. La foto es lo que hace que el sorteo sea reproducible.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Reglas nombradas: `ck_snapshot_sorteo_hash`, `ck_snapshot_sorteo_roster`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `sorteo_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `roster` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `periodos` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `reglas` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `hash_snapshot` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `semilla_sellada` | VARCHAR(128) | — | VARCHAR(128), obligatoria. | NINGUNA |
| `congelado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `correlacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |

### `solicitud_ingreso`

**Qué es.** La postulación de un usuario a un grupo abierto.

**Para qué sirve.** Es el filtro de entrada de los grupos públicos o por enlace. `puntajeCompatibilidad` y `evaluarAutomaticamente()` permiten que el sistema pre-evalúe: reputación suficiente, sin deuda abierta, capacidad declarada compatible con el aporte, sin restricciones vigentes (M1). El organizador —humano o digital— recibe la solicitud ya calificada en vez de tener que investigar a cada postulante.

**Por qué existe.** Un grupo con desconocidos necesita una puerta. Sin solicitud formal, o el grupo es cerrado (y la plataforma no puede crecer más allá de las redes existentes) o es abierto sin filtro (y se llena de morosos que ya fallaron en otros grupos).

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `enlace_grupo`, `grupo`, `identidad.usuario`. Reglas nombradas: `uq_solicitud_ingreso_pendiente`, `ck_solicitud_ingreso_resuelta`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `cupos_solicitados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `mensaje` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `puntaje_compatibilidad` | DECIMAL(5,2) | — | DECIMAL(5,2), opcional. | NINGUNA |
| `enlace_grupo_id` | UUID | — | UUID, opcional, referencia a `enlace_grupo`. | NINGUNA |
| `nombre_visible` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `telefono_contacto` | VARCHAR(20) | — | VARCHAR(20), opcional. | PERSONAL |
| `correo_contacto` | VARCHAR(150) | — | VARCHAR(150), opcional. | PERSONAL |
| `revisada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha_solicitud` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_resolucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `solicitud_permuta`

**Qué es.** El pedido de intercambiar turnos entre dos cupos, con la compensación que se ofrece.

**Para qué sirve.** Cubre una necesidad real y frecuente: a alguien se le presentó un gasto urgente (una operación, una matrícula) y necesita cobrar antes; a otro no le urge y acepta cambiar, a veces a cambio de una compensación. En el pasanaku presencial esto se arregla hablando; el problema es que después nadie se acuerda de lo acordado. El flujo de tres aprobaciones (`solicitante → contraparte → organizador`) es deliberado: **la permuta no es un asunto privado entre dos**. Cambiar el orden altera el riesgo del grupo entero (si el que ahora cobra primero es el más riesgoso, todos quedan más expuestos). Por eso el organizador —o el grupo por acuerdo— tiene que validarla. `compensacionOfrecida` reconoce que estos intercambios tienen precio, y lo registra en vez de dejarlo en un acuerdo verbal.

**Por qué existe.** Sin ella, o se prohíben las permutas (y el producto es rígido frente a una necesidad real) o se permiten sin registro (y aparecen disputas sin prueba). ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `participante`, `turno`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `turno_origen_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `turno_destino_id` | UUID | — | UUID, obligatoria, referencia a `turno`. | NINGUNA |
| `solicitante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `contraparte_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `compensacion_ofrecida` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `aprobada_por_organizador` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fecha_solicitud` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_ejecucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `solicitud_retiro`

**Qué es.** El pedido de un participante de salir del grupo antes de que termine.

**Para qué sirve.** Convierte "me voy" en un proceso con número. `liquidacionCalculada` responde la pregunta económica: si ya aporté 6 períodos y todavía no cobré, ¿cuánto me corresponde? Si ya cobré y me quiero ir sin seguir aportando, ¿cuánto debo? Ese cálculo es la fuente de la mitad de los conflictos de un pasanaku y tiene que estar explícito. `requiereReemplazo` refleja la realidad: en la mayoría de los grupos no te podés ir sin dejar quién ocupe tu lugar, porque tu salida deja un hueco en la bolsa de todos los períodos restantes.

**Por qué existe.** Sin ella, la salida es un cambio de estado sin cálculo, sin aprobación y sin constancia de lo acordado. ---

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.plan_regularizacion`, `participante`. Reglas nombradas: `ck_retiro_deudor_con_plan`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `solicitado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `posicion` | VARCHAR(10) | — | VARCHAR(10), opcional. | NINGUNA |
| `plan_regularizacion_id` | UUID | — | UUID, opcional, referencia a `plan_regularizacion`. | NINGUNA |
| `requiere_reemplazo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `liquidacion_calculada` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |

### `sorteo_turnos`

**Qué es.** El sorteo del orden de cobro, con esquema criptográfico *commit-reveal* verificable.

**Para qué sirve.** **Esta entidad existe por una sola razón: el orden de cobro es el punto de desconfianza número uno del pasanaku.** Todo el mundo sospecha que el organizador acomodó el sorteo para que él o su gente cobre primero. En el pasanaku presencial esto se resuelve sorteando delante de todos con papelitos en una bolsa. Digitalmente hay que reconstruir esa garantía. El mecanismo: 1. **Antes** de sortear, el sistema publica `hashSemillaPrevio` — el hash de una semilla que todavía no reveló. Queda comprometido: ya no puede cambiarla. 2. Se mezcla con `semillaPublica`, un valor que los propios participantes aportan y que el servidor no controla. 3. Después del sorteo se revela `semillaServidor`. **Cualquier participante puede recomputar el orden y verificar que nadie lo manipuló.** `testigos` registra a quiénes se notificó el compromiso previo. `repetirPorAnulacion()` cubre el caso de un sorteo anulado por error, dejando constancia de que hubo dos.

**Por qué existe.** Sin verificabilidad, el sorteo es "confiá en nosotros", que es exactamente lo que la plataforma promete reemplazar. La entidad no es un lujo criptográfico: es el argumento comercial.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupo`, `identidad.usuario`. Reglas nombradas: `uq_sorteo_grupo`, `ck_sorteo_revelado`, `ck_sorteo_compromiso`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `algoritmo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `hash_semilla_previo` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `aportes_entropia` | JSONB | — | JSONB, opcional. | NINGUNA |
| `fecha_compromiso` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_revelado_prevista` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `semilla_servidor` | VARCHAR(128) | — | VARCHAR(128), opcional. | NINGUNA |
| `semilla_publica` | VARCHAR(128) | — | VARCHAR(128), obligatoria. | NINGUNA |
| `resultado` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `ejecutado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `fecha_ejecucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `anulado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `sustitucion_administrador`

**Qué es.** El cambio de quién administra un grupo: quién sale, quién entra, por qué y qué obligaciones conserva el que sale.

**Para qué sirve.** Que un grupo no quede sin administrador cuando el organizador se va o es inhabilitado, y que quien sale siga debiendo lo que debía: cambiar de rol no borra la deuda con el grupo.

**Por qué existe.** Sin ella, el cambio de administrador sería un `UPDATE` sobre el participante y se perdería quién lo decidió, por qué y qué conservó el saliente.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Reglas nombradas: `ck_sustitucion_administrador_distintos`, `ck_sustitucion_administrador_motivo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `saliente_participante_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `entrante_participante_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | UUID | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | UUID, obligatoria. | NINGUNA |
| `motivo` | VARCHAR(1000) | — | VARCHAR(1000), obligatoria. | NINGUNA |
| `actor_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `obligaciones_conservadas` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `ocurrida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `correlacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |

### `traspaso_cupo`

**Qué es.** El cambio de manos de un cupo, de un participante a otro, con lo que se transfiere y lo que no.

**Para qué sirve.** Es donde se resuelve la pregunta más espinosa del reemplazo: **¿quién se hace cargo de la deuda que dejó el que se fue, y quién tiene derecho a cobrar el turno?** Los dos campos `deudaTransferida` y `derechoCobroTransferido` obligan a responderlo explícitamente en cada traspaso, en lugar de dejarlo a interpretación. Los tres motivos (`VENTA`, `REEMPLAZO_POR_MORA`, `RETIRO`) tienen consecuencias distintas: en una venta acordada el entrante suele asumir todo; en un reemplazo por mora, la deuda vieja normalmente **queda con el saliente** (se persigue en M8) y el entrante arranca limpio, porque si no, nadie aceptaría reemplazar a un moroso. `aprobadoPorAcuerdoId` es obligatorio en la práctica: un traspaso no puede ser decisión de dos personas cuando afecta la garantía de las otras diez.

**Por qué existe.** Sin esta tabla, un traspaso es un `UPDATE` de `cupo.participante_id` y se pierde toda la historia económica: quién debía qué, quién asumió qué, quién lo autorizó.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `acuerdo`, `cupo`, `participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `participante_origen_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `participante_destino_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `deuda_transferida` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `derecho_cobro_transferido` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `aprobado_por_acuerdo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revertido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `turno`

**Qué es.** El derecho de un cupo a cobrar la bolsa en un período determinado.

**Para qué sirve.** Es **el activo más disputado del pasanaku**. Cobrar primero vale más que cobrar último: quien cobra en el período 1 recibe un préstamo sin interés; quien cobra en el 12 hizo un ahorro forzado. Esa asimetría es la razón de que existan la subasta (`descuentoSubasta`), las permutas y las peleas por el orden. `criterioAsignacion` guarda **cómo** se asignó ese turno (sorteo, orden de ingreso, reputación, subasta, acuerdo). Es información que el participante tiene derecho a ver: no es lo mismo "me tocó el último por sorteo" que "me pusieron último". `montoEstimadoCobro` es la expectativa; el monto real se calcula en la entrega (M4) descontando deducciones. Tenerlo acá permite mostrarle al participante desde el día uno cuánto va a recibir y cuándo.

**Por qué existe.** Es el calendario de derechos de cobro. Sin él no hay plataforma: es literalmente el producto.

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cupo`, `grupo`, `periodo`, `turno`. La referencian `entregas.cesion_derecho`, `entregas.entrega_fondo`, `entregas.fondeo_entrega`, `entregas.oferta_turno`, `garantia.cobertura_respaldo`, `solicitud_permuta`, `turno`. Reglas nombradas: `uq_turno_periodo`, `uq_turno_orden`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, obligatoria, referencia a `periodo`. | NINGUNA |
| `cupo_id` | UUID | — | UUID, obligatoria, referencia a `cupo`. | NINGUNA |
| `orden_asignado` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `criterio_asignacion` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `monto_estimado_cobro` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `descuento_subasta` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | NINGUNA |
| `permutado_con_turno_id` | UUID | — | UUID, opcional, referencia a `turno`. | NINGUNA |
| `confirmado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `voto_participante`

**Qué es.** El voto individual sobre un acuerdo, con su peso.

**Para qué sirve.** El campo `peso` es la sutileza importante: **vale según cuántos cupos tenés**. Quien puso dos manos arriesga el doble y por eso pesa el doble. Es la regla que la gente aplica intuitivamente en los pasanakus reales, y que un voto por cabeza contradiría. `comentario` permite que quede constancia del argumento, no solo del sentido del voto. En una disputa posterior, eso importa.

**Por qué existe.** Sin votos individuales solo hay un contador agregado, y no se puede verificar el resultado ni detectar votos inválidos (de participantes suspendidos, por ejemplo).

**Sistema.** Vive en el esquema `grupos`; la escribe y la lee solo el servicio `grupos` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `acuerdo`, `participante`. Reglas nombradas: `uq_voto_acuerdo_participante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `acuerdo_id` | UUID | — | UUID, obligatoria, referencia a `acuerdo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `sentido` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `peso` | DECIMAL(4,2) | — | DECIMAL(4,2), obligatoria. | NINGUNA |
| `comentario` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `identidad`

### `alcance_invitacion`

**Qué es.** El alcance de un token de invitación: quién lo emitió, a qué grupo invita y a qué teléfono va dirigido.

**Para qué sirve.** Que una invitación sirva solo para lo que se emitió. Quien recibe el enlace entra al grupo que lo invitó, y nadie más puede usar ese enlace para entrar a otro grupo ni con otro número.

**Por qué existe.** El token (`token_verificacion`) solo prueba que el secreto es válido; no dice para qué. Sin el alcance, un enlace reenviado o filtrado abriría la puerta de cualquier grupo, y una segunda emisión no podría revocar la anterior de forma explícita.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `token_verificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `token_id` | UUID | — | UUID, obligatoria, referencia a `token_verificacion`. | SECRETO |
| `grupo_destino_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `emisor_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `telefono_destino` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `nonce` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | NINGUNA |
| `clave_emision` | UUID | — | UUID, obligatoria. | SECRETO |
| `huella_solicitud` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | NINGUNA |
| `clave_consumo` | UUID | — | UUID, opcional. | SECRETO |
| `consumidor_id` | UUID | — | UUID, opcional. | NINGUNA |

### `asignacion_rol`

**Qué es.** El vínculo usuario–rol, acotado a un ámbito, con vigencia y rastro de quién lo otorgó.

**Para qué sirve.** Responde: *¿quién le dio a esta persona el poder de autorizar entregas en este grupo, cuándo, y hasta cuándo?* Es la tabla que se revisa primero cuando algo salió mal y hay que entender cómo alguien pudo hacer lo que hizo. `vigenteHasta` habilita accesos temporales: darle a un analista de soporte acceso al grupo por 48 horas para resolver un caso, y que caduque solo.

**Por qué existe.** Un campo `es_admin` en `usuario` no tiene ámbito, no tiene vigencia, no tiene autor y no se puede revocar con motivo.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `rol`, `usuario`. Reglas nombradas: `ck_asignacion_no_autoasignada`, `ck_asignacion_ambito_completo`, `ck_asignacion_revocacion_motivada`, `uq_asignacion_vigente`, `ix_asignacion_por_vencer`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `rol_id` | UUID | — | UUID, obligatoria, referencia a `rol`. | NINGUNA |
| `ambito` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ambito_id` | UUID | — | UUID, opcional. | NINGUNA |
| `otorgada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `otorgada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `revocada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_revocacion` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |

### `bloqueo_cuenta`

**Qué es.** El bloqueo de acceso, temporal o indefinido, con su motivo.

**Para qué sirve.** Separa cuatro situaciones que un solo campo `estado = BLOQUEADO` confunde: bloqueo automático por intentos fallidos (se libera solo), bloqueo por sospecha de fraude (lo libera cumplimiento), bloqueo pedido por el propio usuario (perdí el teléfono) y bloqueo por orden administrativa. Cada uno tiene un responsable de liberación distinto y un discurso distinto hacia el usuario.

**Por qué existe.** `liberadaPor` y `liberadaEn` responden la pregunta que siempre aparece en la auditoría: *¿quién desbloqueó esta cuenta y con qué autoridad?* ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `bloqueada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `desbloquea_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `liberada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `liberada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `consentimiento`

**Qué es.** El registro de que el usuario aceptó un documento: términos, privacidad, tratamiento de datos, marketing, reglamento del grupo — con la **versión y el hash** del texto exacto que aceptó.

**Para qué sirve.** Es la prueba legal. Cuando un participante reclama "yo nunca acepté que me cobren recargo por mora", la respuesta no es "sí aceptaste": es *aceptaste la versión 2.1 del reglamento el 14 de marzo a las 19:42 desde la IP tal, y este es el hash del texto que estaba vigente ese día*. Sin el hash del documento, el consentimiento no prueba nada, porque los términos cambian con el tiempo. `revocadoEn` cubre el otro lado: el usuario puede retirar el consentimiento de marketing sin perder los servicios que sí contrató.

**Por qué existe.** Es requisito de la normativa de protección de datos y es la única defensa ante una disputa sobre las condiciones pactadas. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `version_documento` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `otorgado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `agente_usuario` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `revocado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `credencial_acceso`

**Qué es.** La contraseña, guardada como hash con Argon2id/bcrypt.

**Para qué sirve.** Es el segundo factor de posesión para las operaciones sensibles y la alternativa cuando el usuario cambia de teléfono. En una plataforma pensada para operar por OTP, la contraseña no es el mecanismo principal, pero sí el que protege el panel administrativo y las acciones de alto impacto.

**Por qué existe.** `requiereCambio`, `expiraEn` y `estaComprometida()` permiten reaccionar cuando aparece una filtración de credenciales de terceros: forzar cambio a los usuarios afectados sin tocar a los demás.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `hash_contrasena` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | DERIVADO |
| `algoritmo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `parametros_kdf` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `requiere_cambio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `cambiada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `direccion_usuario`

**Qué es.** Dónde vive o trabaja la persona, con coordenadas opcionales.

**Para qué sirve.** Tres usos reales. Primero, el **emparejamiento automático** (RF-19): los pasanakus funcionan mejor entre gente de la misma zona, porque la presión social es lo que hace que la gente pague; agrupar desconocidos de ciudades distintas sube la morosidad. Segundo, la **cobranza**: la etapa prejudicial del módulo 8 contempla visita domiciliaria, y sin dirección no hay visita. Tercero, **cumplimiento**: la normativa de prevención de lavado exige domicilio declarado a partir de ciertos montos.

**Por qué existe.** Si la dirección viviera dentro de `usuario`, cinco campos más engordarían la tabla más consultada del sistema para un dato que se usa esporádicamente. Además la relación 1-a-1 opcional deja claro que un usuario puede operar sin dirección hasta cierto nivel de KYC.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `departamento` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `ciudad` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `zona` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `detalle` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `latitud` | DECIMAL(9,6) | — | DECIMAL(9,6), opcional. | NINGUNA |
| `longitud` | DECIMAL(9,6) | — | DECIMAL(9,6), opcional. | NINGUNA |

### `dispositivo`

**Qué es.** Cada teléfono o navegador desde el que se ha usado la cuenta.

**Para qué sirve.** Tres funciones distintas. **Seguridad**: un login desde un dispositivo nunca visto puede exigir verificación adicional (`esConfiable`, `PoliticaToken.exigeDispositivoConocido`). **Notificaciones**: `tokenPush` es la dirección a la que llegan los avisos push del módulo 5. **Detección de fraude**: varias cuentas operando desde la misma huella de dispositivo es el patrón típico de participantes ficticios en un grupo, algo que el módulo 9 vigila.

**Por qué existe.** Sin identidad de dispositivo, no se puede distinguir "el usuario de siempre desde su teléfono de siempre" de "alguien con la contraseña correcta desde un equipo desconocido". ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`. La referencian `cumplimiento.aceptacion_contrato`, `nucleo_financiero.transaccion_billetera`, `sesion`, `token_verificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `huella` | VARCHAR(128) | — | VARCHAR(128), obligatoria. | NINGUNA |
| `plataforma` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `modelo` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `version_app` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `token_push` | VARCHAR(255) | — | VARCHAR(255), opcional. | SECRETO |
| `es_confiable` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `autorizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `ultimo_uso_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revocado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `documento_identidad`

**Qué es.** La cédula, pasaporte o carnet de extranjería del usuario, con imágenes.

**Para qué sirve.** Es lo que permite subir de nivel operativo. Un grupo de Bs 200 mensuales entre compañeros de trabajo no necesita documento; un grupo de USD 1.000 con desconocidos sí, y la normativa lo exige. Es también la única forma de perseguir una deuda: sin documento, un moroso es un número de teléfono que se descarta.

**Por qué existe.** `hash_numero` es la pieza clave: permite detectar que la misma cédula está abriendo dos cuentas **sin necesidad de descifrar el número**. Ese es el patrón de quien quiere volver a entrar después de haber sido restringido por deuda, y es el que hace que la restricción del punto anterior no sea trivial de esquivar.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`. La referencian `verificacion_kyc`. Reglas nombradas: `ck_documento_hash_completo`, `ck_documento_version_llave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `numero_cifrado` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `version_llave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `hash_numero` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `lugar_expedicion` | VARCHAR(2) | — | VARCHAR(2), opcional. | NINGUNA |
| `complemento` | VARCHAR(10) | — | VARCHAR(10), opcional. | NINGUNA |
| `pais_emision` | CHAR(2) | — | CHAR(2), obligatoria. | NINGUNA |
| `fecha_emision` | DATE | — | DATE, opcional. | NINGUNA |
| `fecha_expiracion` | DATE | — | DATE, opcional. | NINGUNA |
| `url_anverso` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |
| `url_reverso` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |

### `factor_mfa`

**Qué es.** El segundo factor: TOTP, SMS, WhatsApp o códigos de respaldo.

**Para qué sirve.** Se exige selectivamente, no siempre: para administradores, para organizadores que administran carteras grandes, y para acciones críticas como cambiar la cuenta bancaria de cobro o autorizar un desembolso. Ese es el punto de `Permiso.requiereMFA`.

**Por qué existe.** Los `codigosRespaldo` resuelven el caso real de "perdí el teléfono justo antes de mi turno". Sin ellos, la única salida es soporte manual, que es a la vez lento y un vector de ingeniería social. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`. Reglas nombradas: `ck_factor_mfa_version_llave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `secreto_cifrado` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | SECRETO |
| `version_llave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `es_principal` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `confirmado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `ultimo_uso_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `historial_credencial`

**Qué es.** Los hashes de las contraseñas anteriores.

**Para qué sirve.** Impide el ciclo clásico de "cambiar la contraseña y volver a poner la misma de siempre" cuando se fuerza una rotación por incidente. Si hubo un compromiso y se obliga a cambiar, reutilizar la contraseña filtrada deja al usuario igual de expuesto.

**Por qué existe.** Sin historial, la política de no reutilización no se puede aplicar. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `hash_contrasena` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | DERIVADO |
| `reemplazada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `intento_autenticacion`

**Qué es.** El log de cada intento de inicio de sesión, exitoso o no, con un puntaje de riesgo.

**Para qué sirve.** Alimenta el bloqueo automático por fuerza bruta y las alertas de seguridad. `usuarioId` puede ser nulo a propósito: hay que registrar también los intentos contra identificadores que no existen, porque eso es exactamente la firma de un ataque de enumeración de usuarios.

**Por qué existe.** El `puntajeRiesgo` permite decidir de forma graduada: no bloquear al usuario que se equivocó dos veces desde su casa, pero sí exigir MFA al que acierta la contraseña desde una IP nueva en otro país. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `identificador_usado` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `exitoso` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `motivo_fallo` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `agente_usuario` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `huella_dispositivo` | VARCHAR(128) | — | VARCHAR(128), opcional. | NINGUNA |
| `puntaje_riesgo` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |

### `intento_validacion_token`

**Qué es.** El registro de cada vez que alguien intentó usar un código, con el resultado.

**Para qué sirve.** Es el detector de ataques y, sobre todo, la **evidencia** cuando hay un reclamo del tipo "a mí me sacaron la plata de la cuenta". Con esta tabla se puede decir: hubo 47 intentos fallidos desde una IP de otro país entre las 2 y las 3 de la mañana. Sin ella, solo se sabe que el token quedó bloqueado, y no por qué.

**Por qué existe.** El contador `intentosFallidos` del token dice *cuántos*; esta tabla dice *quién, desde dónde y con qué patrón*. Solo lo segundo sirve para investigar.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `token_verificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `token_id` | UUID | — | UUID, obligatoria, referencia a `token_verificacion`. | SECRETO |
| `fecha_hora` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resultado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `agente_usuario` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `huella_dispositivo` | VARCHAR(128) | — | VARCHAR(128), opcional. | NINGUNA |

### `perfil_financiero`

**Qué es.** Lo que la persona declara sobre su capacidad económica: ocupación, ingreso mensual, cuánto puede aportar, de dónde sale la plata, y si es PEP (persona expuesta políticamente).

**Para qué sirve.** Aquí se previene el fracaso más común y más silencioso del pasanaku digital: **gente que se compromete a un aporte que no puede sostener**. En el pasanaku de oficina esto lo filtra el sentido común colectivo ("¿vos vas a poder con Bs 1.000 al mes?"); en una plataforma que empareja desconocidos, ese filtro hay que construirlo. `validarCapacidad(montoAporte)` es la función que le advierte a la persona —y al grupo— que el compromiso no cierra con lo que ella misma declaró. `esPEP` y `fuenteIngresos` son requisitos de cumplimiento antilavado: una plataforma que mueve aportes recurrentes de muchas personas es exactamente el vehículo que un regulador vigila.

**Por qué existe.** Es información autodeclarada y de baja frecuencia de cambio, con implicaciones legales distintas a las de los datos de contacto. Se mantiene separada para poder auditar cuándo se declaró qué, y para poder retenerla o anonimizarla con reglas propias (M9).

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `ocupacion` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `ingreso_mensual_declarado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `capacidad_aporte_declarada` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `fuente_ingresos` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `es_pep` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `permiso`

**Qué es.** Una acción concreta y autorizable: `grupo.crear`, `aporte.exonerar`, `entrega.autorizar`.

**Para qué sirve.** Permite ajustar la política de autorización sin tocar código, y sobre todo declarar **qué acciones exigen segundo factor** (`requiereMFA`). Exonerar el aporte de alguien, autorizar un desembolso o cambiar una cuenta bancaria son acciones con impacto económico directo: deben pedir MFA aunque la sesión ya esté abierta.

**Por qué existe.** Sin permisos granulares, la única distinción posible es "admin / no admin", y toda la operación termina hecha con cuentas de administrador — que es exactamente lo que un auditor marca como hallazgo crítico. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `rol_permiso`. Reglas nombradas: `ck_permiso_decision_exige_mfa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `recurso` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `accion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nivel_riesgo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `exige_motivo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_mfa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `politica_token`

**Qué es.** Los parámetros de seguridad de cada tipo de token: cuánto dura, cuántos intentos, cuántos reenvíos por hora, por qué canales puede salir.

**Para qué sirve.** Permite **endurecer la seguridad sin desplegar código**. Un OTP para autorizar un pago debe durar 5 minutos; una invitación a un grupo debe durar 72 horas porque la gente contesta WhatsApp cuando puede. Si esos números están hardcodeados, cada ajuste operativo —y se ajustan seguido, sobre todo cuando aparece un patrón de fraude— es un release. `maxReenviosPorHora` y `maxEmisionesPorDia` tienen además un efecto directo en costos: cada SMS se paga. Un bucle de reenvíos mal controlado es una factura inesperada.

**Por qué existe.** Convierte decisiones de seguridad en configuración auditable y versionada (`vigenteDesde`), en lugar de constantes enterradas. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `token_verificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `proposito` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `ttl_segundos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `longitud_codigo` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `max_intentos_validacion` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `max_reenvios_por_hora` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `cooldown_reenvio_segundos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `max_emisiones_por_dia` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `canales_permitidos` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `exige_dispositivo_conocido` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `invalida_anteriores` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `preferencia_notificacion`

**Qué es.** Por dónde y cuándo quiere el usuario que le escriban.

**Para qué sirve.** Un cobro que llega por el canal equivocado no cobra. Y un cobro que llega a las 2 de la mañana genera un opt-out, que es peor: después ya no se le puede escribir ni siquiera para avisarle que su turno está listo. El horario de no molestar y el canal preferido protegen la relación con el usuario. `estaEnHorarioSilencio()` tiene una excepción deliberada: los eventos transaccionales (`EventoNotificable.esTransaccional`) la ignoran. Si tu pago fue rechazado o tu entrega se acreditó, eso se avisa a la hora que sea.

**Por qué existe.** Además de la buena práctica, hay una razón económica y regulatoria: WhatsApp exige opt-in y penaliza a los remitentes con quejas de spam. Escribirle a quien no quiere ser escrito pone en riesgo el canal para todos los demás usuarios. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `canal_primario` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `canal_respaldo` | VARCHAR(20) | — | VARCHAR(20), opcional. | NINGUNA |
| `acepta_whatsapp` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `acepta_correo` | BOOLEAN | — | BOOLEAN, obligatoria. | PERSONAL |
| `acepta_sms` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `acepta_push` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `tope_diario_mensajes` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `hora_no_molestar_desde` | TIME | — | TIME, opcional. | NINGUNA |
| `hora_no_molestar_hasta` | TIME | — | TIME, opcional. | NINGUNA |
| `frecuencia_resumen` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `referencia_personal`

**Qué es.** Un contacto de confianza declarado por el usuario, con la relación y si acepta ser avalista.

**Para qué sirve.** Formaliza lo que en el pasanaku presencial es implícito: **alguien responde por vos**. Cumple dos papeles. Primero, verificación blanda de identidad para usuarios sin documento en regla: si tres personas verificadas confirman conocerte, eso es señal. Segundo, y más importante, es la puerta de entrada al **aval solidario del módulo 8**: `aceptaSerAvalista` marca a quién se le puede pedir que garantice la deuda del avalado. En la cobranza también es determinante: la etapa temprana del módulo 8 contempla `AVISO_A_AVALISTA`, y la sola perspectiva de que le avisen a tu cuñado que no estás pagando mueve más plata que tres recordatorios automáticos.

**Por qué existe.** Sin referencias, un moroso sin documento verificado es inalcanzable. Con ellas, la red social que sostiene al pasanaku real se vuelve parte del sistema. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `telefono` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `relacion` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `verificada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `verificada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `acepta_ser_avalista` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `reputacion_usuario`

**Qué es.** La caché de lectura rápida del puntaje de reputación del usuario.

**Para qué sirve.** El puntaje se muestra en el perfil, en la lista de candidatos de un grupo, al evaluar una solicitud de ingreso y en el emparejamiento automático. Son consultas frecuentes y sensibles a latencia. Recalcular el score desde los eventos (M6) en cada una de esas pantallas sería inviable.

**Por qué existe.** Es deliberadamente una **proyección**, no la fuente de la verdad. La fuente es `evento_reputacion` + `puntaje_reputacion` en M6. Esta tabla se mantiene sincronizada como espejo. Confundir ambas —y editar el número aquí— rompería la trazabilidad de por qué alguien tiene el puntaje que tiene.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `puntaje` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `indice_puntualidad` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `total_obligaciones` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `obligaciones_cumplidas` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `obligaciones_en_mora` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `incumplimientos_graves` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `grupos_completados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `version_modelo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `calculado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `restriccion_usuario`

**Qué es.** Una limitación puntual sobre lo que el usuario puede hacer, sin bloquearle el acceso: no crear grupos, no unirse a nuevos, no ser organizador, o un tope de monto.

**Para qué sirve.** Es **la entidad bisagra entre el incumplimiento y el resto de la plataforma**, y una de las más importantes de todo el modelo. El escenario que resuelve: alguien abandona un pasanaku debiendo tres cuotas; se le abre un expediente en M8; ese expediente crea aquí una restricción `NO_UNIRSE`. Resultado: **no puede entrar limpio al siguiente grupo mientras tenga deuda abierta**. Esa es, en la práctica, la sanción que más disuade — mucho más que un puntaje que baja. Y a diferencia del bloqueo, la persona sí puede seguir entrando: puede ver su deuda, pagarla y regularizarse. Cerrarle la puerta del todo elimina la vía por la que podría pagar.

**Por qué existe.** Sin ella, la consecuencia del incumplimiento se queda encerrada en el módulo 8 y no afecta el comportamiento del sistema. La reputación informa; la restricción impide.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`. Reglas nombradas: `ck_restriccion_levantamiento`, `ck_restriccion_vigencia`, `uq_restriccion_usuario_vigente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `origen` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `referencia_origen_id` | UUID | — | UUID, opcional. | NINGUNA |
| `valor_limite` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `levantada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo_levantamiento` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |

### `rol`

**Qué es.** Un conjunto nombrado de permisos: administrador, soporte, auditor, organizador, participante, invitado.

**Para qué sirve.** Permite que la misma persona sea organizadora en un grupo y simple participante en otro, que es lo normal: quien organiza el pasanaku de la oficina probablemente participa en el de su familia sin organizarlo. El `ambito` (GLOBAL / GRUPO / ORGANIZACION) es lo que hace posible esa doble vida. El rol `AUDITOR` merece mención aparte: existe para que alguien pueda **leer todo sin poder modificar nada**, que es el requisito de una revisión externa o de un regulador.

**Por qué existe.** Sin roles con ámbito, el permiso "expulsar participante" o se le da a todos los organizadores en todos los grupos, o no se le da a ninguno. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `asignacion_rol`, `rol_permiso`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `ambito` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `departamento` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `descripcion` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `es_sistema` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `rol_permiso`

**Qué es.** La relación muchos-a-muchos entre roles y permisos.

**Para qué sirve.** Permite redefinir qué puede hacer un rol sin reasignar usuarios uno por uno. Si mañana se decide que los organizadores ya no pueden condonar mora unilateralmente, se quita el permiso de la tabla y el cambio aplica a todos los organizadores del sistema. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `permiso`, `rol`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `rol_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `rol`. | NINGUNA |
| `permiso_id` | UUID | — | UUID, obligatoria, clave primaria, referencia a `permiso`. | NINGUNA |

### `sesion`

**Qué es.** Una sesión activa: usuario, dispositivo, desde cuándo, desde dónde.

**Para qué sirve.** Le da al usuario la pantalla de "dispositivos conectados" y el botón de "cerrar todas las sesiones". Es la primera reacción de alguien que sospecha que le entraron a la cuenta, y es también lo que ejecuta el soporte cuando se reporta un robo de teléfono.

**Por qué existe.** Sin sesiones materializadas solo hay tokens: se pueden revocar, pero no mostrar ni explicar al usuario. La revocación deja de ser una acción que el usuario puede tomar por sí mismo.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `dispositivo`, `usuario`. La referencian `nucleo_financiero.transaccion_billetera`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `dispositivo_id` | UUID | — | UUID, obligatoria, referencia a `dispositivo`. | NINGUNA |
| `refresco_familia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ultima_actividad_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `geolocalizacion_aprox` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `revocada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_revocacion` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |

### `solicitud_baja`

**Qué es.** El pedido formal de un usuario de dejar la plataforma.

**Para qué sirve.** Traduce un derecho (irse cuando quiera) a un proceso que no puede atropellar a terceros. El campo clave es `bloqueadaPorObligaciones`: **nadie puede darse de baja para escapar de una deuda o de un grupo en curso**. Si el usuario tiene aportes pendientes, deuda con el fondo de garantía o un turno por cobrar, la baja queda registrada pero con fecha efectiva condicionada. Esto protege a los otros diez participantes que siguen poniendo su plata contando con él. También sirve para separar la baja de la eliminación de datos: irse de la plataforma no es lo mismo que ejercer el derecho de cancelación de datos personales, que se tramita en el módulo 9 y tiene sus propias excepciones legales.

**Por qué existe.** Sin esta entidad, la baja es un `UPDATE` de estado y se pierde la razón, la fecha de solicitud y el motivo del bloqueo. Ante un reclamo ("pedí darme de baja y me siguieron cobrando"), esto es la prueba.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `motivo` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_efectiva` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `bloqueada_por_obligaciones` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `token_verificacion`

**Qué es.** Cualquier credencial de un solo uso y vida corta que el sistema emite para probar que alguien controla un canal (teléfono, correo) o para autorizar una acción puntual.

**Para qué sirve.** Es el mecanismo con el que la plataforma **convierte "dice que es él" en "probó que es él"** sin exigir contraseña en cada paso. Concretamente habilita: - el registro sin fricción (código por WhatsApp en vez de formulario largo); - la invitación a un grupo por enlace (RF-19, M2); - el **pago en un toque** desde el recordatorio de WhatsApp (RF-16, M5) — el participante toca el enlace y llega directo al QR de su aporte, sin login; - la confirmación de que el beneficiario recibió su bolsa (M4); - la firma del reglamento del grupo con sello de tiempo (M2); - la aceptación de un aval solidario (M8), que es un compromiso económico real y necesita constancia de quién aceptó y cuándo.

**Por qué existe.** Porque el punto de ataque número uno de una app de dinero es el flujo de recuperación de cuenta. Los campos `intentosFallidos`, `maxIntentos`, `reenvios`, `ipOrigen` y `dispositivoId` no son burocracia: son lo que impide que alguien pruebe códigos de 6 dígitos hasta entrar a la cuenta de un participante la semana de su turno. `claveIdempotencia` resuelve un problema cotidiano y no de seguridad: el usuario toca dos veces "Enviar código" porque la red va lenta. Sin idempotencia se emiten dos códigos, el primero se invalida, la persona recibe el primero por SMS y escribe ese — y el sistema lo rechaza. Es una de las causas más frecuentes de abandono en el registro.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `dispositivo`, `politica_token`, `token_verificacion`, `usuario`. La referencian `alcance_invitacion`, `aportes.enlace_pago_rapido`, `cumplimiento.aceptacion_contrato`, `entregas.confirmacion_recepcion`, `garantia.aval_participante`, `grupos.aceptacion_reglamento`, `grupos.invitacion`, `intento_validacion_token`, `notificaciones.enlace_pago_notificado`, `organizador.contrato_organizador`, `token_verificacion`. Reglas nombradas: `uq_token_verificacion_idem`, `ck_token_refresco_familia`, `uq_token_refresco_vivo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `politica_id` | UUID | — | UUID, obligatoria, referencia a `politica_token`. | NINGUNA |
| `dispositivo_id` | UUID | — | UUID, opcional, referencia a `dispositivo`. | NINGUNA |
| `tipo_token` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | SECRETO |
| `proposito` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `hash_token` | VARCHAR(128) | — | VARCHAR(128), obligatoria. | DERIVADO |
| `algoritmo_hash` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | DERIVADO |
| `canal_entrega` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `destino_enmascarado` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `enviado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `consumido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `invalidado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_invalidacion` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `intentos_fallidos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `max_intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `reenvios` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, obligatoria. | NINGUNA |
| `agente_usuario` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `correlation_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `longitud` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `url_destino` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `firma_hmac` | VARCHAR(128) | — | VARCHAR(128), opcional. | NINGUNA |
| `uso_unico` | BOOLEAN | — | BOOLEAN, opcional. | NINGUNA |
| `clicks` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `familia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `rotado_de_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | NINGUNA |

### `usuario`

**Qué es.** La persona real detrás de la cuenta. Una sola por número de teléfono.

**Para qué sirve.** Es el sujeto de todo lo demás: quien aporta, quien cobra, quien organiza, quien debe y quien tiene reputación. Su ciclo de vida (`PRE_REGISTRO → PENDIENTE_VERIFICACION → ACTIVO`) modela el onboarding real: en Bolivia la gente se baja la app porque un amigo le mandó un enlace de WhatsApp, no porque haya decidido "registrarse en una fintech". Por eso existe el estado `PRE_REGISTRO`: hay que poder guardar un teléfono e invitarlo a un grupo **antes** de que la persona complete su perfil, sin obligarla a un formulario largo cuando todavía no entiende qué es la plataforma. El campo `codigoPublico` existe porque nadie debería mostrar un UUID en pantalla ni tener que dictar su número de teléfono para que lo agreguen a un grupo. Es el identificador que la persona lee en voz alta. `idioma` con quechua y aymara no es decorativo: buena parte del mercado real de pasanakus está en poblaciones donde el español es segunda lengua, y un recordatorio de cobro que no se entiende es un recordatorio que no cobra. `nivelKYC` está en el usuario (y no solo en la tabla de verificaciones) porque es la variable que decide, en tiempo de operación y sin joins, cuánto puede mover esa persona. `limiteOperativoVigente()` es la función que impide que alguien sin documento verificado entre a un grupo de Bs 5.000 mensuales.

**Por qué existe.** Es el ancla de todo el sistema. Sin ella no hay reputación portable entre grupos, que es justamente la propuesta de valor: que tu buen historial en un pasanaku te sirva para entrar al siguiente.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `aportes.comprobante_manual`, `aportes.conciliacion`, `aportes.excepcion_conciliacion`, `aportes.extracto_bancario`, `aportes.pago`, `aportes.plan_regularizacion`, `aportes.reembolso`, `asignacion_rol`, `auditoria.alerta_cumplimiento`, `auditoria.coincidencia_lista`, `auditoria.ejecucion_reporte`, `auditoria.proceso_anonimizacion`, `auditoria.reporte_operacion_sospechosa`, `auditoria.solicitud_datos_personales`, `auditoria.ticket_soporte`, `bloqueo_cuenta`, `catalogo.licencia_regulatoria`, `catalogo.tarifario`, `comun.bitacora_evento`, `comun.registro_acceso_datos`, `consentimiento`, `credencial_acceso`, `cumplimiento.aceptacion_contrato`, `cumplimiento.acta_comite`, `cumplimiento.activo_informacion`, `cumplimiento.alerta_monitoreo_lft`, `cumplimiento.beneficiario_final`, `cumplimiento.calificacion_riesgo_cliente`, `cumplimiento.capacitacion_cumplimiento`, `cumplimiento.caso_investigacion_lft`, `cumplimiento.contrato_adhesion`, `cumplimiento.contrato_tercero`, `cumplimiento.control_interno`, `cumplimiento.debida_diligencia`, `cumplimiento.declaracion_origen_fondos`, `cumplimiento.declaracion_pep`, `cumplimiento.designacion_regulatoria`, `cumplimiento.desvio_perfil`, `cumplimiento.documento_publicado`, `cumplimiento.envio_regulatorio`, `cumplimiento.evaluacion_riesgo_producto`, `cumplimiento.evaluacion_tercero`, `cumplimiento.evento_riesgo_operativo`, `cumplimiento.expediente_cliente`, `cumplimiento.factor_riesgo_evaluado`, `cumplimiento.hallazgo_auditoria`, `cumplimiento.incidente_seguridad`, `cumplimiento.matriz_riesgo_lft`, `cumplimiento.observacion_regulatoria`, `cumplimiento.oficial_cumplimiento`, `cumplimiento.perfil_transaccional`, `cumplimiento.plan_accion_riesgo`, `cumplimiento.plan_continuidad`, `cumplimiento.politica_interna`, `cumplimiento.prueba_continuidad`, `cumplimiento.prueba_control`, `cumplimiento.punto_reclamo`, `cumplimiento.reclamo_cliente`, `cumplimiento.registro_operacion_relevante`, `cumplimiento.regla_monitoreo_lft`, `cumplimiento.reporte_regulatorio`, `cumplimiento.requerimiento_autoridad`, `cumplimiento.revision_periodica_kyc`, `direccion_usuario`, `dispositivo`, `documento_identidad`, `entregas.cuenta_bancaria_beneficiario`, `entregas.entrega_fondo`, `entregas.historial_estado_entrega`, `entregas.incidencia_entrega`, `entregas.validacion_pre_entrega`, `erp.asiento_plantilla`, `erp.cierre_periodo_contable`, `erp.ejercicio_fiscal`, `erp.estado_financiero_generado`, `erp.factura_proveedor`, `erp.orden_compra`, `erp.pago_a_proveedor`, `erp.presupuesto`, `factor_mfa`, `garantia.abono_recuperacion`, `garantia.accion_cobranza`, `garantia.acuerdo_quita`, `garantia.alerta_temprana`, `garantia.apelacion_sancion`, `garantia.aval_participante`, `garantia.candidato_reemplazo`, `garantia.capacidad_respaldo`, `garantia.castigo_deuda`, `garantia.cobertura_incumplimiento`, `garantia.cobertura_respaldo`, `garantia.descargo_participante`, `garantia.deuda_participante`, `garantia.evidencia_incumplimiento`, `garantia.gestion_cobranza`, `garantia.historial_estado_incumplimiento`, `garantia.historial_incumplimiento_usuario`, `garantia.lista_restriccion_interna`, `garantia.movimiento_fondo`, `garantia.movimiento_reserva`, `garantia.promesa_pago`, `garantia.registro_incumplimiento`, `garantia.reserva_respaldo`, `garantia.sancion`, `garantia.score_riesgo_incumplimiento`, `grupos.acuerdo`, `grupos.enlace_grupo`, `grupos.historial_estado_grupo`, `grupos.invitacion`, `grupos.participante`, `grupos.postulacion_emparejamiento`, `grupos.reglamento_grupo`, `grupos.solicitud_ingreso`, `grupos.sorteo_turnos`, `historial_credencial`, `intento_autenticacion`, `notificaciones.aviso_interno`, `notificaciones.bandeja_entrada`, `notificaciones.canal_vinculado`, `notificaciones.notificacion`, `nucleo_financiero.asiento_contable`, `nucleo_financiero.bloqueo_saldo`, `nucleo_financiero.certificado_saldo`, `nucleo_financiero.cierre_diario`, `nucleo_financiero.conciliacion_custodia`, `nucleo_financiero.cuenta_billetera`, `nucleo_financiero.descuadre_custodia`, `nucleo_financiero.evaluacion_antifraude`, `nucleo_financiero.evidencia_mfa_consumida`, `nucleo_financiero.instrumento_fondeo`, `nucleo_financiero.orden_retiro`, `nucleo_financiero.politica_billetera`, `nucleo_financiero.regla_antifraude`, `nucleo_financiero.respuesta_idempotente`, `nucleo_financiero.retencion_saldo`, `nucleo_financiero.reverso_transaccion`, `nucleo_financiero.solicitud_cierre_billetera`, `nucleo_financiero.transaccion_billetera`, `organizador.apelacion_sancion_org`, `organizador.organizador`, `organizador.sancion_organizador`, `organizador.solicitud_organizador`, `perfil_financiero`, `preferencia_notificacion`, `publicidad.campana_publicitaria`, `publicidad.clic_anuncio`, `publicidad.impresion_anuncio`, `publicidad.revision_creativa`, `publicidad.segmento_audiencia`, `publicidad.socio_comercial`, `referencia_personal`, `reputacion_usuario`, `restriccion_usuario`, `sesion`, `solicitud_baja`, `tarifas.asignacion_tarifario`, `tarifas.cambio_tarifario`, `tarifas.campana_promocional`, `tarifas.cuenta_por_cobrar_comision`, `tarifas.datos_facturacion`, `tarifas.devengo_comision`, `tarifas.devolucion_comision`, `tarifas.evento_significativo_sin`, `tarifas.exencion_comision`, `tarifas.factura_electronica`, `tarifas.liquidacion_ingresos`, `tarifas.simulacion_tarifa`, `token_verificacion`, `transparencia.certificado_reputacion`, `transparencia.evento_reputacion`, `transparencia.insignia_otorgada`, `transparencia.puntaje_reputacion`, `transparencia.resena_participante`, `transparencia.snapshot_reputacion`, `usuario_interno`, `verificacion_kyc`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo_publico` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `nombres` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `apellidos` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | PERSONAL |
| `telefono_e164` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `correo` | VARCHAR(150) | — | VARCHAR(150), opcional. | PERSONAL |
| `fecha_nacimiento` | DATE | — | DATE, obligatoria. | PERSONAL |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `nivel_kyc` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `idioma` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `zona_horaria` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `url_avatar` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `telefono_verificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | PERSONAL |
| `correo_verificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | PERSONAL |
| `ultimo_acceso_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `fecha_registro` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `eliminado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `usuario_interno`

**Qué es.** El perfil laboral y operativo del usuario que desempeña funciones internas en la plataforma.

**Para qué sirve.** Separa al cliente o participante final del personal operativo de AportaYa. Asocia al empleado con su departamento corporativo (Sistemas, Operaciones, Riesgos, Cumplimiento, Finanzas, Comercial, Soporte, Auditoría), su cargo formal, si debe cambiar la contraseña provisional enviada por correo al ingresar por primera vez, y el bloqueo temporal administrativo con su motivo justificado.

**Por qué existe.** La segregación de responsabilidades y las directrices de control interno de ASFI e ISO/IEC 27001 exigen que los atributos laborales y las restricciones administrativas de los operadores no contaminen la entidad general de usuario ni se gestionen de manera informal.

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `departamento` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `cargo` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `debe_cambiar_contrasena` | BOOLEAN | — | BOOLEAN, obligatoria. | SECRETO |
| `bloqueado_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_bloqueo` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |

### `verificacion_kyc`

**Qué es.** El proceso de validar ese documento: envío al proveedor, comparación biométrica con selfie, resultado, y revisión manual si hace falta.

**Para qué sirve.** Un documento cargado no es un documento verificado. Esta entidad separa el *dato* del *proceso*: guarda quién lo validó (proveedor externo o revisor humano), con qué puntaje biométrico, cuándo, y hasta cuándo vale esa verificación (`vigenteHasta` — los documentos vencen). Cuando un participante reclama que le rechazaron el KYC, `motivoRechazo` y `revisadaPor` son la respuesta.

**Por qué existe.** Sin historial de verificaciones no se puede reintentar sin perder el rastro del rechazo anterior, ni auditar la tasa de aprobación del proveedor, ni detectar a un revisor que aprueba todo sin mirar. ---

**Sistema.** Vive en el esquema `identidad`; la escribe y la lee solo el servicio `identidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `documento_identidad`, `usuario`. La referencian `cumplimiento.debida_diligencia`, `organizador.solicitud_organizador`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `documento_id` | UUID | — | UUID, opcional, referencia a `documento_identidad`. | PERSONAL |
| `nivel_solicitado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `proveedor` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `referencia_proveedor` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `puntaje_biometrico` | DECIMAL(5,2) | — | DECIMAL(5,2), opcional. | NINGUNA |
| `url_selfie` | VARCHAR(255) | — | VARCHAR(255), opcional. | PERSONAL |
| `url_perfil_izquierdo` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `url_perfil_derecho` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `motivo_rechazo` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |
| `revisada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |

## Esquema `inversiones`

### `comision_exito`

**Qué es.** La comisión por resultado cobrada en el rescate de un fondo, con la marca máxima antes y después.

**Para qué sirve.** Cobrar solo sobre la ganancia que supera la marca máxima previa: una pérdida no se cobra, una recuperación sin pasar la marca no se cobra y el mismo tramo de ganancia no se cobra dos veces (CU-127).

**Por qué existe.** Sin la marca máxima registrada por rescate, la comisión podría cobrarse dos veces sobre la misma ganancia.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `posicion_inversion`, `rescate_inversion`. Reglas nombradas: `ck_comision_exito_marca`, `ck_comision_exito_sin_base`, `ck_comision_exito_tope`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `rescate_inversion_id` | UUID | — | UUID, obligatoria, referencia a `rescate_inversion`. | NINGUNA |
| `cuotas` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `valor_cuota` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `marca_maxima_previa` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `tasa` | DECIMAL(7,6) | — | DECIMAL(7,6), obligatoria. | NINGUNA |
| `base_elegible` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `comision` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `cuota_neta` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `marca_maxima_nueva` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `origen_datos` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `calculada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `comprobante_inversion`

**Qué es.** El comprobante de una suscripción o de una liquidación, con el dinero descompuesto en principal, interés, impuesto, comisión y pérdida realizada.

**Para qué sirve.** Que la persona vea de dónde sale cada boliviano que recibe o que le descontaron, y que sirva para su reporte fiscal (CU-128).

**Por qué existe.** Sin la descomposición, el neto sería un número que nadie puede explicar ni reconciliar con los impuestos retenidos.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `posicion_inversion`. Reglas nombradas: `ck_comprobante_inv_neto`, `ck_comprobante_inv_costo_base`, `ck_comprobante_inv_sentido`, `ck_comprobante_inv_resultado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `origen_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `sentido_titular` | VARCHAR(8) | — | VARCHAR(8), obligatoria. | NINGUNA |
| `principal` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `interes` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `impuesto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `comision` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `perdida_realizada` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `neto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `costo_base` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `origen_datos` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `conciliacion_interes`

**Qué es.** La comparación, por rescate, entre el interés y la retención que calculó la plataforma y los que informó el aliado.

**Para qué sirve.** Que nada de lo que informa el aliado se acepte porque venga firmado: el interés se recalcula y se concilia (CU-125).

**Por qué existe.** Sin ella, una diferencia entre lo devengado y lo pagado pasaría sin que nadie la vea.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `posicion_inversion`, `rescate_inversion`. Reglas nombradas: `ck_conciliacion_interes_periodo`, `ck_conciliacion_interes_estado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `rescate_inversion_id` | UUID | — | UUID, obligatoria, referencia a `rescate_inversion`. | NINGUNA |
| `periodo_desde` | DATE | — | DATE, obligatoria. | NINGUNA |
| `periodo_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `interes_devengado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `interes_externo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `retencion_calculada` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `retencion_externa` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `conciliada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `consentimiento_inversion`

**Qué es.** La aceptación, por una persona, de una versión de condiciones concreta.

**Para qué sirve.** Que la persona invierta exactamente bajo las condiciones que vio, y que eso se pueda demostrar (CU-121).

**Por qué existe.** Sin un consentimiento separado de la orden, una orden podría apuntar a condiciones que la persona nunca aceptó.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `version_condiciones`. La referencian `orden_inversion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `version_condiciones_id` | UUID | — | UUID, obligatoria, referencia a `version_condiciones`. | NINGUNA |
| `texto_hash` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `hash_solicitud` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `aceptado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `corrida_trabajo`

**Qué es.** Cada corrida de una operación de inversiones: sincronizar el catálogo o los valores, devengar o reintentar instrucciones, disparada por API o por el programador.

**Para qué sirve.** Que Operaciones vea cuándo corrió cada trabajo, con qué resultado y si se cortó, y que repetir el pedido no repita el efecto.

**Por qué existe.** Sin ella, un trabajo programado sería invisible y un reintento de la API volvería a sincronizar o a devengar.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Reglas nombradas: `ck_corrida_trabajo_fin`, `ck_corrida_trabajo_error`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `operacion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `hash_solicitud` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `origen` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `resultado` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `error` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `terminada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `devengo_dpf`

**Qué es.** El interés acumulado de un depósito a plazo fijo, día por día, calculado por la plataforma.

**Para qué sirve.** Mostrar cuánto lleva ganado un depósito y tener un número propio contra el cual controlar lo que paga el emisor (CU-123, CU-126).

**Por qué existe.** Si el único número fuera el que informa el aliado, no habría forma de detectar que pagó de menos.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `posicion_inversion`. Reglas nombradas: `ck_devengo_dpf_acumulado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `dias_acumulados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `interes_acumulado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `distribucion_interes`

**Qué es.** El reparto al titular del interés pagado por el emisor, por el período de su posición y con la retención del comprobante.

**Para qué sirve.** Que la persona reciba el interés que le corresponde, neto de retención, y que una diferencia con lo esperado quede a la vista (CU-126).

**Por qué existe.** Sin el reparto registrado, el interés acreditado no se podría explicar por período ni conciliar con el devengo propio.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `posicion_inversion`, `rescate_inversion`. Reglas nombradas: `ck_distribucion_interes_periodo`, `ck_distribucion_interes_esperado`, `ck_distribucion_interes_neto`, `ck_distribucion_interes_estado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `rescate_inversion_id` | UUID | — | UUID, obligatoria, referencia a `rescate_inversion`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `periodo_desde` | DATE | — | DATE, obligatoria. | NINGUNA |
| `periodo_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `interes_devengado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `penalizacion` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `interes_esperado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `interes_pagado` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `retencion` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `interes_neto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `motivo_discrepancia` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `distribuida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `instruccion_libro`

**Qué es.** La intención persistida de un efecto sobre la billetera del titular: retener, liberar, debitar o acreditar.

**Para qué sirve.** Que el dinero se mueva una sola vez aunque el sistema se corte en el medio: lo peor que puede pasar es «retenido y sin posición todavía», visible y reintentable (CU-122).

**Por qué existe.** El libro vive en `nucleo-financiero`. Sin una intención guardada antes de pedirle el movimiento, un corte haría perder el pedido o repetirlo.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Reglas nombradas: `ck_instruccion_libro_aplicada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `origen_tipo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `origen_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `referencia_libro` | UUID | — | UUID, opcional. | NINGUNA |
| `intentos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `ultimo_error` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `aplicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `orden_inversion`

**Qué es.** El pedido de invertir un importe del saldo libre en un producto, con sus estados hasta que el aliado confirma o rechaza.

**Para qué sirve.** Que el importe quede reservado antes de que nada salga hacia el aliado, y que la persona vea en qué paso está su inversión (CU-121, CU-122).

**Por qué existe.** Entre pedir y tener la posición pasan pasos que pueden cortarse. Sin la orden con estado, un corte dejaría plata retenida sin explicación o una posición sin débito.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `consentimiento_inversion`, `producto_inversion`, `version_condiciones`. La referencian `posicion_inversion`. Reglas nombradas: `ck_orden_inv_rechazo`, `ck_orden_inv_confirmada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `producto_inversion_id` | UUID | — | UUID, obligatoria, referencia a `producto_inversion`. | NINGUNA |
| `version_condiciones_id` | UUID | — | UUID, obligatoria, referencia a `version_condiciones`. | NINGUNA |
| `consentimiento_inversion_id` | UUID | — | UUID, obligatoria, referencia a `consentimiento_inversion`. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `monto` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `hash_solicitud` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `transaccion_externa` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `fecha_valor` | DATE | — | DATE, opcional. | NINGUNA |
| `motivo_rechazo` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `actualizada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `posicion_inversion`

**Qué es.** Lo que la persona tiene invertido en un producto: un depósito con su vencimiento o una cantidad de cuotas de un fondo.

**Para qué sirve.** Mostrarle a la persona qué tiene, desde cuándo y, en un fondo, a qué valor de cuota entró.

**Por qué existe.** La orden es el pedido; la posición es el resultado confirmado por el aliado. Sin separarlas, una orden rechazada parecería una inversión.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `orden_inversion`, `producto_inversion`, `version_condiciones`. La referencian `comision_exito`, `comprobante_inversion`, `conciliacion_interes`, `devengo_dpf`, `distribucion_interes`, `rescate_inversion`. Reglas nombradas: `ck_posicion_inv_tipo`, `ck_posicion_inv_cierre`, `ck_posicion_inv_cerrada_sin_cuotas`, `ck_posicion_inv_vencimiento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `producto_inversion_id` | UUID | — | UUID, obligatoria, referencia a `producto_inversion`. | NINGUNA |
| `version_condiciones_id` | UUID | — | UUID, obligatoria, referencia a `version_condiciones`. | NINGUNA |
| `orden_inversion_id` | UUID | — | UUID, obligatoria, referencia a `orden_inversion`. | NINGUNA |
| `tipo` | VARCHAR(5) | — | VARCHAR(5), obligatoria. | NINGUNA |
| `principal` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `cuotas` | DECIMAL(18,6) | — | DECIMAL(18,6), opcional. | NINGUNA |
| `valor_cuota_entrada` | DECIMAL(18,6) | — | DECIMAL(18,6), opcional. | NINGUNA |
| `marca_maxima` | DECIMAL(18,6) | — | DECIMAL(18,6), opcional. | NINGUNA |
| `fecha_constitucion` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_vencimiento` | DATE | — | DATE, opcional. | NINGUNA |
| `posicion_externa` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `producto_inversion`

**Qué es.** Un producto que ofrece un aliado: un depósito a plazo fijo (`DPF`) o un fondo (`FONDO`), con su emisor, su moneda y su nivel de riesgo.

**Para qué sirve.** Que la persona vea qué se le ofrece y de quién, sin ninguna promesa de rentabilidad (CU-120).

**Por qué existe.** El producto es estable; sus condiciones cambian con cada cotización. Separarlos permite actualizar las condiciones sin perder a qué producto se refería lo que alguien ya aceptó.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `orden_inversion`, `posicion_inversion`, `valor_cuota`, `version_condiciones`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(5) | — | VARCHAR(5), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `emisor` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `nivel_riesgo` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `rescate_inversion`

**Qué es.** El pedido de sacar dinero de una posición: parcial, total, al vencimiento o anticipado, con sus estados hasta liquidarse.

**Para qué sirve.** Que la persona recupere su dinero sabiendo en qué paso está, y que lo que pidió no se ofrezca dos veces (CU-124).

**Por qué existe.** El rescate pasa por el aliado y puede quedar incierto. Sin el registro, un reintento podría rescatar dos veces las mismas cuotas.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `posicion_inversion`. La referencian `comision_exito`, `conciliacion_interes`, `distribucion_interes`. Reglas nombradas: `ck_rescate_inv_cuotas`, `ck_rescate_inv_rechazo`, `uq_rescate_inv_dpf_vigente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `posicion_inversion_id` | UUID | — | UUID, obligatoria, referencia a `posicion_inversion`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `cuotas` | DECIMAL(18,6) | — | DECIMAL(18,6), opcional. | NINGUNA |
| `estado` | VARCHAR(14) | — | VARCHAR(14), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |
| `hash_solicitud` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `fecha_valor` | DATE | — | DATE, opcional. | NINGUNA |
| `liquida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `transaccion_externa` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `motivo_rechazo` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `actualizada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `valor_cuota`

**Qué es.** El valor de la cuota de un fondo en una fecha, según el aliado.

**Para qué sirve.** Valorar las posiciones en fondos y calcular cuánto vale un rescate (CU-123).

**Por qué existe.** El valor de un fondo cambia todos los días. Sin la serie guardada, no se puede reconstruir a qué valor se rescató ni cobrar la comisión de éxito sobre la ganancia real.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `producto_inversion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `producto_inversion_id` | UUID | — | UUID, obligatoria, referencia a `producto_inversion`. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `valor` | DECIMAL(18,6) | — | DECIMAL(18,6), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `fuente` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `origen_datos` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `publicado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `recibido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `version_condiciones`

**Qué es.** Cada cotización de las condiciones de un producto: plazo, tasa, mínimo, costos, retención, comisión de éxito y el texto que la persona acepta.

**Para qué sirve.** Que lo que la persona aceptó quede fijo aunque el aliado cambie las condiciones al día siguiente: la orden apunta a la versión exacta.

**Por qué existe.** Si las condiciones se pisaran, nadie podría demostrar qué tasa o qué penalización se le ofreció a quien invirtió el mes pasado.

**Sistema.** Vive en el esquema `inversiones`; la escribe y la lee solo el servicio `inversiones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `producto_inversion`. La referencian `consentimiento_inversion`, `orden_inversion`, `posicion_inversion`. Reglas nombradas: `ck_version_condiciones_sintetico`, `ck_version_condiciones_hora_corte`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `producto_inversion_id` | UUID | — | UUID, obligatoria, referencia a `producto_inversion`. | NINGUNA |
| `numero` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `plazo_dias` | INTEGER | — | INTEGER, opcional. | NINGUNA |
| `base_dias` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `tasa_nominal_anual` | DECIMAL(9,6) | — | DECIMAL(9,6), opcional. | NINGUNA |
| `permite_rescate_anticipado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `penalizacion_anticipo` | DECIMAL(7,6) | — | DECIMAL(7,6), opcional. | NINGUNA |
| `dias_rescate` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `hora_corte` | VARCHAR(5) | — | VARCHAR(5), opcional. | NINGUNA |
| `monto_minimo` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `tasa_retencion` | DECIMAL(7,6) | — | DECIMAL(7,6), opcional. | NINGUNA |
| `tasa_comision_exito` | DECIMAL(7,6) | — | DECIMAL(7,6), opcional. | NINGUNA |
| `costos_texto` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `texto_condiciones` | VARCHAR(2000) | — | VARCHAR(2000), obligatoria. | NINGUNA |
| `texto_hash` | CHAR(64) | — | CHAR(64), obligatoria. | DERIVADO |
| `fuente` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `fecha_cotizacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `origen_datos` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `apto_produccion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `notificaciones`

### `aviso_interno`

**Qué es.** El registro persistente de avisos dirigidos a operadores internos o a roles funcionales en el backoffice.

**Para qué sirve.** Alerta a operadores sobre hechos operativos críticos: una nueva alerta de monitoreo, un caso asignado, una ficha técnica pendiente de revisión o un fallo de despliegue. Puede dirigirse a un operador específico o a todo un rol (ej. `ADMIN_PLATAFORMA`, `OFICIAL_CUMPLIMIENTO`).

**Por qué existe.** Sin este registro, los operadores no tendrían una campana de notificaciones real que les informe de eventos urgentes con enlace directo a la pantalla de resolución. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `rol_destinatario` | VARCHAR(50) | — | VARCHAR(50), opcional. | NINGUNA |
| `titulo` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `mensaje` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `enlace` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `leido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `bandeja_entrada`

**Qué es.** El espejo de las notificaciones dentro de la app.

**Para qué sirve.** WhatsApp y SMS se pierden en la marea de mensajes. La bandeja en la app es el registro persistente: el usuario entra y ve todo lo que le comunicaron, con su acción pendiente. Es también la red de seguridad cuando el usuario hizo opt-out de todos los canales externos — **algo tiene que poder avisarle que su turno está listo para cobrar**. `urlAccion` lleva directo a lo que hay que hacer.

**Por qué existe.** Sin bandeja, un usuario que borró el WhatsApp no tiene cómo enterarse de nada, y la única constancia de lo comunicado está fuera de la plataforma. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `notificacion_id` | UUID | — | UUID, obligatoria, referencia a `notificacion`. | NINGUNA |
| `titulo` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `resumen` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `url_accion` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `leida` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `leida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `archivada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `canal_vinculado`

**Qué es.** Un canal concreto de un usuario —su WhatsApp, su SMS, su correo, su token push— con su estado de verificación y consentimiento.

**Para qué sirve.** Es el destinatario real. Tres campos hacen el trabajo pesado: - **`optInEn` / `optOutEn`.** El consentimiento es por canal, no por usuario: alguien puede aceptar WhatsApp y rechazar SMS. Enviar sin opt-in no solo es problema legal, es problema operativo: genera reportes de spam que degradan la reputación del remitente y afectan la entregabilidad **de todos los demás usuarios**. - **`ventanaConversacionHasta`.** La ventana de 24 horas de WhatsApp: después de que el usuario escribe, hay 24 horas para responderle con mensajes libres; pasadas esas horas solo se pueden enviar plantillas aprobadas. Guardar la ventana evita intentar envíos que el proveedor va a rechazar **y que en algunos esquemas igual se cobran**. - **`rebotesConsecutivos` + `estado = NO_ALCANZABLE`.** El número cambió de dueño, el correo no existe. Seguir escribiendo a un destino muerto cuesta plata y ensucia las métricas de entregabilidad.

**Por qué existe.** Si el destinatario fuera simplemente `usuario.telefono`, no habría dónde guardar el opt-in, la ventana de conversación ni el estado de alcanzabilidad — y el sistema seguiría escribiéndole a quien pidió que no le escriban. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `envio_notificacion`, `respuesta_entrante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `identificador` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `verificado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `verificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `opt_in_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `opt_out_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_opt_out` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `ventana_conversacion_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `rebotes_consecutivos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |

### `cola_envio`

**Qué es.** La cola de trabajo de los envíos pendientes.

**Para qué sirve.** El pico de envíos es brutalmente concentrado: todos los grupos con día de cobro 5 generan sus recordatorios el día 3 a la misma hora. La cola con `particion` y `disponibleEn` distribuye ese pico para no saturar al proveedor ni exceder su límite de tasa. `bloqueadaHasta` implementa el lease: dos workers no toman el mismo envío y no se manda el mensaje dos veces.

**Por qué existe.** Sin cola persistente, un reinicio del servicio en pleno envío pierde los mensajes pendientes y nadie recibe su recordatorio ese día. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `envio_notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `envio_id` | UUID | — | UUID, obligatoria, referencia a `envio_notificacion`. | NINGUNA |
| `particion` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `disponible_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `bloqueada_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `cola_muerta`

**Qué es.** Los envíos que fallaron definitivamente, con su payload, para revisión manual.

**Para qué sirve.** Un mensaje que falla para siempre y desaparece es un usuario que no se enteró de que tenía que pagar y al que después se le va a cobrar mora. La cola muerta hace visible ese fallo: alguien puede revisarlo, corregir el canal y reprocesar.

**Por qué existe.** Sin ella, los fallos permanentes son invisibles y se descubren como reclamos. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `envio_notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `envio_id` | UUID | — | UUID, obligatoria, referencia a `envio_notificacion`. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `payload` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `reprocesado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `enlace_pago_notificado`

**Qué es.** El enlace de pago que viaja dentro de una notificación, con su seguimiento de clics y conversión.

**Para qué sirve.** Es la materialización del **"pago en un toque"** (RF-16) y, sobre todo, **la métrica que dice si el módulo funciona**: `clicks` vs `convertidoEnPago` es el embudo del recordatorio. Si mucha gente toca el enlace y poca paga, el problema está en la pantalla de pago, no en el mensaje. Si poca gente toca, el problema está en el mensaje.

**Por qué existe.** Sin esta entidad no se puede atribuir un pago al recordatorio que lo causó, y por lo tanto no se puede saber si la inversión en mensajería se justifica.

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.orden_cobro`, `identidad.token_verificacion`, `notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `notificacion_id` | UUID | — | UUID, obligatoria, referencia a `notificacion`. | NINGUNA |
| `orden_cobro_id` | UUID | — | UUID, obligatoria, referencia a `orden_cobro`. | NINGUNA |
| `token_id` | UUID | — | UUID, obligatoria, referencia a `token_verificacion`. | SECRETO |
| `url_corta` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `clicks` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `primer_click_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `convertido_en_pago` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `envio_notificacion`

**Qué es.** Un intento de entregar una notificación por un canal concreto, con su costo y su estado.

**Para qué sirve.** Es donde vive la **cadena de respaldo** (RF-16): `orden = 1` WhatsApp; si a los N minutos no hay confirmación de entrega, se crea `orden = 2` por SMS; después correo. Cada eslabón guarda su propio `costo` y su propio `estado`. Eso permite responder la pregunta que decide el presupuesto de la operación: **¿qué canal realmente hace que la gente pague?** Si el 80% de los pagos ocurre tras el WhatsApp y el SMS solo agrega 3% a un costo diez veces mayor, la cadena de respaldo se recorta. Sin costo y estado por eslabón, esa decisión se toma a ciegas. `contenidoEnviado` guarda el texto exacto que se envió. Es la prueba ante "a mí nunca me avisaron".

**Por qué existe.** Sin envíos separados, no hay respaldo entre canales, ni costo por canal, ni forma de reintentar un canal sin reintentar todos.

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `canal_vinculado`, `notificacion`, `proveedor_mensajeria`, `version_plantilla`. La referencian `cola_envio`, `cola_muerta`, `evento_entrega_mensaje`. Reglas nombradas: `uq_envio_idempotencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `notificacion_id` | UUID | — | UUID, obligatoria, referencia a `notificacion`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_mensajeria`. | NINGUNA |
| `version_plantilla_id` | UUID | — | UUID, obligatoria, referencia a `version_plantilla`. | NINGUNA |
| `canal_vinculado_id` | UUID | — | UUID, opcional, referencia a `canal_vinculado`. | NINGUNA |
| `canal` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `destinatario` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(120) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(120), obligatoria. | NINGUNA |
| `encolado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `contenido_enviado` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `id_mensaje_proveedor` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |
| `orden` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `max_intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `costo` | DECIMAL(10,4) | — | DECIMAL(10,4), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `codigo_error` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `enviado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `entregado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `leido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `proximo_reintento_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `evento_entrega_mensaje`

**Qué es.** Cada callback del proveedor sobre el destino de un mensaje: enviado, entregado, leído, fallido, rechazado.

**Para qué sirve.** Es la única fuente confiable de si el mensaje llegó. "Enviado" no es "entregado" y "entregado" no es "leído". Esa distinción es la que alimenta la cadena de respaldo: si a los 15 minutos no hay `delivered`, se escala al siguiente canal. El estado `leído` tiene un uso adicional en cobranza: **alguien que lee los recordatorios y no paga es un caso distinto de alguien que no los lee**. El primero es una decisión; el segundo puede ser un problema de canal. El módulo 8 usa exactamente esa señal (`NO_ABRE_MENSAJES` como alerta temprana).

**Por qué existe.** Los proveedores reintentan sus callbacks. `claveIdempotencia` `UNIQUE` evita aplicar dos veces el mismo evento y contar dos entregas donde hubo una.

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `envio_notificacion`. Reglas nombradas: `uq_evento_entrega_idempotencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `envio_id` | UUID | — | UUID, obligatoria, referencia a `envio_notificacion`. | NINGUNA |
| `tipo_evento` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fecha_hora_proveedor` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `recibido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `payload_crudo` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `codigo_error` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(120) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(120), obligatoria. | NINGUNA |

### `evento_notificable`

**Qué es.** El catálogo de los 20 hechos del negocio que ameritan avisarle a alguien, cada uno con su política de comunicación.

**Para qué sirve.** Centraliza decisiones que de otro modo quedarían dispersas en el código de ocho módulos distintos: - `prioridad`: un código de verificación es CRÍTICO (bloquea al usuario si no llega); un resumen mensual es BAJO. - `esTransaccional`: **si es transaccional, ignora el horario de no molestar.** "Tu entrega se acreditó" o "tu pago fue rechazado" se avisan a la hora que sea. Un recordatorio de aporte, no. - `permiteAgrupacion`: evita mandar cinco mensajes cuando se pueden juntar en uno. - `ventanaDeduplicacionMin`: cuánto tiempo tiene que pasar para que el mismo aviso se considere nuevo y no repetido. - `cadenaRespaldo`: el orden de canales a intentar. WhatsApp → SMS → correo.

**Por qué existe.** Sin catálogo, cada módulo decide por su cuenta cómo notificar, y el usuario recibe cinco mensajes de cinco estilos distintos por cosas relacionadas. Además, ajustar la política de comunicación (por ejemplo, dejar de mandar SMS porque se disparó el costo) requeriría tocar ocho módulos. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `notificacion`, `plantilla_mensaje`, `programacion_recordatorio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `categoria` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `es_obligatorio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `prioridad` | VARCHAR(10) | un código de verificación es CRÍTICO (bloquea al usuario si no llega); un resumen mensual es BAJO. | VARCHAR(10), obligatoria. | NINGUNA |
| `es_transaccional` | BOOLEAN | **si es transaccional, ignora el horario de no molestar.** "Tu entrega se acreditó" o "tu pago fue rechazado" se avisan a la hora que sea. Un recordatorio de aporte, no. | BOOLEAN, obligatoria. | NINGUNA |
| `permite_agrupacion` | BOOLEAN | evita mandar cinco mensajes cuando se pueden juntar en uno. | BOOLEAN, obligatoria. | NINGUNA |
| `ventana_deduplicacion_min` | SMALLINT | cuánto tiempo tiene que pasar para que el mismo aviso se considere nuevo y no repetido. | SMALLINT, obligatoria. | NINGUNA |
| `canales_permitidos` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `cadena_respaldo` | VARCHAR(120) | el orden de canales a intentar. WhatsApp → SMS → correo. | VARCHAR(120), obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `lista_supresion`

**Qué es.** La lista negra de identificadores a los que no se les vuelve a escribir, por canal.

**Para qué sirve.** Es la protección de la reputación del remitente. Tres motivos: queja por spam, rebote duro, solicitud legal. Se consulta **antes de cada envío**. La diferencia con el opt-out del `CanalVinculado` es importante: la supresión es **por identificador, no por usuario**. Si un número reportó spam, no se le escribe a ese número aunque mañana lo registre otra persona con otra cuenta. Es una protección a nivel de infraestructura de mensajería, no de relación con el usuario.

**Por qué existe.** Una tasa alta de quejas hace que el proveedor limite o suspenda la cuenta de envío. Cuando eso pasa, **ningún** usuario recibe recordatorios, y la cobranza de toda la plataforma se cae. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `identificador` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `canal` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `categoria` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `agregado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `permanente` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `notificacion`

**Qué es.** El hecho a comunicar a una persona. Una por evento y destinatario.

**Para qué sirve.** Es la intención: "hay que avisarle a Juan que su aporte vence mañana". Independiente de por dónde se le avise y de cuántas veces se intente. `contexto` (JSON) lleva los datos del hecho: grupo, aporte, monto, fecha. Se guarda con la notificación, no se recalcula al enviar, porque **el mensaje debe decir lo que era cierto cuando se generó**. Si el monto cambió entre la generación y el envío, mandar el nuevo confundiría al usuario. `claveDeduplicacion` es el campo que resuelve el problema más molesto de todo sistema de recordatorios: **el bombardeo**. El job de recordatorios se reejecuta —por un reintento, por un despliegue, por un error— y la persona recibe cinco mensajes idénticos. Eso genera opt-outs y quejas por spam. La clave (hash de tipoEvento + usuarioId + aporteId + día) con índice único parcial lo hace imposible por construcción. `correlationId` permite trazar: este recordatorio pertenece al mismo flujo que generó la orden de cobro y que registró el intento de pago (M3, M9).

**Por qué existe.** Sin el nivel de "hecho a comunicar" separado del envío, no se puede escalar a canal de respaldo sin duplicar la notificación, ni deduplicar, ni mostrar en la bandeja de la app.

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evento_notificable`, `identidad.usuario`. La referencian `bandeja_entrada`, `enlace_pago_notificado`, `envio_notificacion`, `garantia.accion_cobranza`, `respuesta_entrante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `evento_id` | UUID | — | UUID, obligatoria, referencia a `evento_notificable`. | NINGUNA |
| `prioridad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `contexto` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `clave_deduplicacion` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | SECRETO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `programada_para` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `finalizada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `correlation_id` | UUID | — | UUID, obligatoria. | NINGUNA |

### `plantilla_mensaje`

**Qué es.** Una plantilla de mensaje para un evento y un canal, con su estado de aprobación ante el proveedor.

**Para qué sirve.** Modela una restricción externa que no se puede ignorar: **WhatsApp exige que las plantillas se registren y se aprueben antes de poder usarse**, y las rechaza con frecuencia (por tono comercial, por variables mal definidas, por categoría incorrecta). `categoriaProveedor` (UTILITY / AUTHENTICATION / MARKETING) tiene consecuencias económicas y funcionales directas: las de marketing cuestan más, requieren opt-in más estricto y **no se pueden enviar fuera de la ventana de 24 horas**. Clasificar mal un recordatorio de cobro como marketing significa no poder enviarlo cuando más falta hace. `idPlantillaProveedor` guarda el identificador que devuelve Meta/Twilio: es el que hay que mandar en cada envío. `estadoAprobacion = PAUSADA` refleja algo real: el proveedor puede pausar una plantilla en producción si acumula reportes de spam. Si el sistema no lo sabe, sigue intentando enviarla y falla en silencio.

**Por qué existe.** Sin esta entidad, el sistema intenta enviar mensajes que el proveedor va a rechazar —y que en algunos esquemas igual se cobran— y no hay dónde ver qué plantillas están vivas. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evento_notificable`. La referencian `version_plantilla`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(50) | — | VARCHAR(50), obligatoria. | NINGUNA |
| `evento_id` | UUID | — | UUID, obligatoria, referencia a `evento_notificable`. | NINGUNA |
| `canal` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `categoria_proveedor` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `estado_aprobacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `id_plantilla_proveedor` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `programacion_recordatorio`

**Qué es.** La regla de cuándo mandar cada recordatorio, relativa a una fecha de referencia.

**Para qué sirve.** `desfaseDias` es elegante en su simplicidad: `-3` es tres días antes del vencimiento, `+1` es un día después. Con eso se arma toda la secuencia de cobranza preventiva sin escribir código: - `-3`: "tu aporte vence el viernes" - `-1`: "tu aporte vence mañana" - `0`: "hoy vence tu aporte" - `+1`: "tu aporte venció ayer" - `+3`, repetido: escalamiento `horaEnvio` importa más de lo que parece: mandar el recordatorio de cobro a las 9 de la mañana de un lunes tiene tasa de conversión distinta que a las 8 de la noche de un viernes. Poder ajustarlo por configuración permite optimizar sin desplegar. `grupoId` nullable permite que un grupo tenga su propia cadencia (los grupos familiares toleran menos recordatorios que los formales).

**Por qué existe.** Sin programación configurable, la secuencia de recordatorios está hardcodeada y no se puede experimentar con lo que realmente mejora la puntualidad — que es la métrica que define el éxito del producto. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evento_notificable`, `grupos.grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `evento_id` | UUID | — | UUID, obligatoria, referencia a `evento_notificable`. | NINGUNA |
| `desfase_dias` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `hora_envio` | TIME | — | TIME, obligatoria. | NINGUNA |
| `repetir_cada` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |
| `max_repeticiones` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `condicion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `proveedor_mensajeria`

**Qué es.** Cada proveedor de mensajería con el que se opera, con su costo, su límite de tasa y su salud.

**Para qué sirve.** `costoPorMensaje` permite calcular el costo real de la cobranza por grupo — un número que la mayoría de las plataformas descubre tarde. `limiteMensajesPorSegundo` evita que el envío masivo del día de vencimiento haga que el proveedor rechace la mitad de los mensajes por exceso de tasa. `saludPorcentaje` + `estaSaludable()` permiten enrutar a un proveedor alternativo cuando el principal se degrada, justo el día de mayor volumen.

**Por qué existe.** Con un solo proveedor hardcodeado, su caída es la caída del cobro de todos los grupos ese día. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `envio_notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `canales_soportados` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `url_base` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `referencia_credenciales` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `costo_por_mensaje` | DECIMAL(10,4) | — | DECIMAL(10,4), obligatoria. | NINGUNA |
| `limite_mensajes_por_segundo` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `salud_porcentaje` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |

### `respuesta_entrante`

**Qué es.** Lo que el participante contesta por el canal, con la intención detectada.

**Para qué sirve.** Esta entidad reconoce algo que todo sistema de notificaciones descubre a la mala: **la gente contesta**. Escriben "YA PAGUÉ", "cuánto debo", "sáquenme de aquí", "1". Si nadie procesa esas respuestas, o se pierden (y el usuario siente que le habla a una pared) o alguien tiene que leer chats a mano. `intencionDetectada` clasifica en cinco intenciones accionables: - `YA_PAGUE` → dispara el flujo de comprobante manual del módulo 3. - `CONSULTAR_SALDO` → responde con el estado de cuenta. - `AYUDA` → abre ticket de soporte (M9). - `BAJA` → registra opt-out en el canal. **Esto no es opcional: es requisito regulatorio y de los proveedores** que responder "BAJA" funcione. - `DESCONOCIDA` → escala a un humano.

**Por qué existe.** Sin respuestas entrantes, el canal es unidireccional y el usuario que quiere cooperar (avisar que ya pagó) no tiene cómo hacerlo por el canal que sí usa. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `canal_vinculado`, `notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `canal_vinculado_id` | UUID | — | UUID, obligatoria, referencia a `canal_vinculado`. | NINGUNA |
| `notificacion_relacionada_id` | UUID | — | UUID, opcional, referencia a `notificacion`. | NINGUNA |
| `contenido` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `intencion_detectada` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `recibida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `procesada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `accion_ejecutada` | VARCHAR(120) | — | VARCHAR(120), opcional. | NINGUNA |

### `version_plantilla`

**Qué es.** El texto concreto de una plantilla, en un idioma, en una versión.

**Para qué sirve.** Dos razones: 1. **Idioma.** Un recordatorio de cobro en español a alguien que opera en quechua o aymara es un recordatorio que no cobra. El modelo soporta versiones por idioma de la misma plantilla. 2. **Versionado.** Cuando alguien reclama "a mí nunca me avisaron que había recargo", hay que poder reconstruir el texto exacto que se le envió ese día. `contenidoEnviado` en el envío guarda el resultado renderizado; la versión guarda la plantilla de la que salió. `botones` (JSON) es lo que habilita el CTA "Pagar ahora" y las respuestas rápidas del tipo "Ya pagué / Necesito plazo". Ese botón es literalmente la diferencia entre un mensaje que informa y uno que cobra. `validarVariables(contexto)` evita el error clásico de mandar "Hola {{1}}, tu aporte de {{2}} vence el {{3}}" con una variable sin resolver.

**Por qué existe.** Sin versiones, cambiar un texto reescribe la historia y no se puede probar qué se comunicó. ---

**Sistema.** Vive en el esquema `notificaciones`; la escribe y la lee solo el servicio `notificaciones` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `plantilla_mensaje`. La referencian `envio_notificacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `plantilla_id` | UUID | — | UUID, obligatoria, referencia a `plantilla_mensaje`. | NINGUNA |
| `version` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `idioma` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `asunto` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |
| `cuerpo` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `variables` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `botones` | JSONB | — | JSONB, opcional. | NINGUNA |
| `url_encabezado_media` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |

## Esquema `nucleo_financiero`

### `asiento_contable`

**Qué es.** El registro de un hecho económico, compuesto por movimientos que suman igual en el debe y en el haber.

**Para qué sirve.** Cada aporte acreditado, cada entrega y cada cobertura del fondo genera un asiento. La regla `SUM(debe) = SUM(haber)` **hace estructuralmente imposible** que la plata aparezca o desaparezca: si algo entró a un lado, salió de otro. `origenTipo` + `origenId` conectan el asiento con el hecho que lo generó (un pago, una entrega, una cobertura). Eso permite ir del número al hecho y viceversa, que es lo primero que pide un auditor. **La regla de oro: los asientos no se editan ni se borran. Se reversan.** `asientoReversaId` apunta al asiento que anula a otro. Un error contable queda visible con su corrección al lado, y esa es exactamente la trazabilidad que se exige cuando hay dinero de terceros.

**Por qué existe.** Es lo que convierte un montón de filas de pagos en información financiera confiable y auditable.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `asiento_contable`, `erp.periodo_contable`, `grupos.grupo`, `identidad.usuario`. La referencian `asiento_contable`, `erp.cobro_cuenta_por_cobrar`, `erp.depreciacion_activo`, `erp.factura_proveedor`, `erp.pago_a_proveedor`, `garantia.castigo_deuda`, `garantia.cobertura_incumplimiento`, `garantia.movimiento_fondo`, `movimiento_contable`, `tarifas.devengo_comision`, `tarifas.liquidacion_ingresos`, `transaccion_billetera`. Reglas nombradas: `ck_asiento_reversa_distinta`, `ck_asiento_reversado_enlazado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `numero` | BIGSERIAL | — | BIGSERIAL, obligatoria. | NINGUNA |
| `fecha` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `glosa` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `origen_tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `origen_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `periodo_contable_id` | UUID | — | UUID, opcional, referencia a `periodo_contable`. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `asiento_reversa_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `registrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `bloqueo_saldo`

**Qué es.** Una orden de autoridad —judicial, de la unidad de inteligencia financiera, del supervisor— que inmoviliza saldo.

**Para qué sirve.** Cumplir el oficio **sin mentirle al usuario ni perder el rastro del dinero**. El bloqueo se materializa como una retención: la plata sigue siendo del titular y sigue apareciendo en su saldo total, pero no está disponible, y la app puede explicar por qué.

**Por qué existe.** Porque llegan oficios, y la alternativa habitual —bajar el saldo a mano o "congelar la cuenta" con una bandera— destruye la contabilidad y deja a la empresa sin defensa cuando el titular reclama. Aquí quedan el número de oficio, la autoridad, el alcance, el documento y su hash.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `identidad.usuario`, `retencion_saldo`. La referencian `cumplimiento.requerimiento_autoridad`. Reglas nombradas: `uq_bloqueo_oficio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `retencion_id` | UUID | — | UUID, opcional, referencia a `retencion_saldo`. | NINGUNA |
| `levantada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `autoridad` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `tipo_orden` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `numero_oficio` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `monto_bloqueado` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `alcance` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `documento_url` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `recibido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vence_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `levantado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `certificado_saldo`

**Qué es.** Un documento con folio que certifica el saldo a una fecha.

**Para qué sirve.** Trámites de terceros: visas, créditos, procesos judiciales. `folio` + `hash_documento` permiten que quien lo recibe verifique su autenticidad contra la plataforma, lo que evita el certificado falsificado en Photoshop. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `solicitado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `folio` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `saldo_certificado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `fecha_corte` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `url_documento` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `cierre_diario`

**Qué es.** El corte del día: cuánto se recaudó, cuánto se concilió, cuánto quedó en excepción, y si el día cuadró.

**Para qué sirve.** Es el **control de calidad diario del dinero**. La disciplina que impone es simple y poderosa: cada día se cierra, y si no cuadra, no se cierra. Los descuadres se resuelven el mismo día, cuando todavía se recuerda qué pasó, en vez de acumularse hasta volverse irresolubles. `reabrir(motivo, adminId)` existe porque a veces hay que corregir un día ya cerrado — pero deja constancia de quién lo reabrió y por qué, que es lo que un auditor va a mirar primero.

**Por qué existe.** Sin cierre diario, un descuadre puede vivir meses. `cuadrado = FALSE` mientras exista una excepción sin resolver de esa fecha: la excepción no se puede ignorar, bloquea el cierre. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `conciliacion_custodia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `total_recaudado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_conciliado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_excepciones` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `cantidad_pagos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `cuadrado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `cerrado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `reabierto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `conciliacion_custodia`

**Qué es.** El cuadre diario entre lo que el sistema dice que la gente tiene y lo que el banco dice que hay.

**Para qué sirve.** Es **la prueba diaria de solvencia del dinero electrónico**. Tres cifras y una división: Cuando el ratio baja de 1, el sistema entra en modo restringido de forma automática: se siguen aceptando recargas y aportes (que aumentan la cobertura), se suspenden los retiros discrecionales, y el descuadre escala como incidente operativo (M9) y como evento de riesgo operativo (M12).

**Por qué existe.** Porque el modo de fallar de todas las billeteras que quebraron es el mismo: el descuadre existió durante meses y nadie lo vio, porque nadie lo calculaba todos los días. Esta tabla obliga a calcularlo todos los días y deja rastro de cada cálculo.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cierre_diario`, `cuenta_custodia`, `identidad.usuario`. La referencian `descuadre_custodia`. Reglas nombradas: `uq_conciliacion_cuenta_fecha`, `ck_conciliacion_encaje`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_custodia_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_custodia`. | NINGUNA |
| `cierre_diario_id` | UUID | — | UUID, opcional, referencia a `cierre_diario`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `saldo_dinero_electronico` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `saldo_custodia` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `saldo_en_transito` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `diferencia` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `ratio_cobertura` | DECIMAL(9,6) | — | DECIMAL(9,6), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `cumple_encaje` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `consumo_limite`

**Qué es.** Cuánto lleva consumido cada cuenta de cada límite en la ventana actual.

**Para qué sirve.** Poder decir "te quedan Bs 1.400 de tu límite mensual de retiro" antes de que el usuario intente la operación, en vez de rechazarla al final. Y poder auditar por qué una operación fue rechazada un día concreto.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.limite_operativo_billetera`, `cuenta_billetera`. Reglas nombradas: `uq_consumo_ventana`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `limite_id` | UUID | — | UUID, obligatoria, referencia a `limite_operativo_billetera`. | NINGUNA |
| `ventana_inicio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ventana_fin` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `monto_acumulado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `cantidad_acumulada` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `cuenta_billetera`

**Qué es.** El contenedor de saldo. Una por titular y moneda.

**Para qué sirve.** Es la pieza que permite que el pasanaku funcione como funciona en la vida real —plata que entra, se junta y sale— sin que nadie tenga que confiar en la cuenta bancaria personal de otro. Cuatro familias de cuentas, y las cuatro son necesarias: - **`USUARIO`**: el saldo de una persona. Es lo que ve en la app. - **`GRUPO`**: **la bolsa del pasanaku tiene cuenta propia.** Este es el cambio más importante que introduce el módulo respecto del modelo anterior. Antes, el dinero del grupo era una abstracción contable; ahora es un saldo real con titular jurídico definido: el grupo, no el organizador. El organizador puede operarla dentro del reglamento, pero **no es titular y no puede retirar hacia una cuenta propia** (RN-18 sigue viva y ahora es estructural). - **`PLATAFORMA_INGRESOS`** y **`PLATAFORMA_IMPUESTOS_POR_PAGAR`**: donde caen la comisión y los impuestos retenidos. Separadas a propósito: el IVA cobrado no es ingreso, es plata del fisco que la plataforma solo transporta. - **`SUSPENSO_NO_IDENTIFICADO`**: llegó dinero que no se sabe de quién es. Sin esta cuenta, ese depósito se "acomoda" en algún lado y se pierde la trazabilidad. Con ella, queda visible hasta que alguien lo identifique o se devuelva.

**Por qué existe.** Sin cuenta de grupo, el dinero del pasanaku vuelve a depender de que alguien lo tenga en su cuenta personal —que es exactamente el problema que el producto promete resolver. Sin cuenta de plataforma separada, los ingresos por comisión se mezclan con el dinero de los usuarios y el encaje deja de ser demostrable.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_contable`, `grupos.grupo`, `identidad.usuario`, `politica_billetera`. La referencian `bloqueo_saldo`, `certificado_saldo`, `consumo_limite`, `cumplimiento.alerta_monitoreo_lft`, `estado_cuenta_billetera`, `evaluacion_antifraude`, `movimiento_billetera`, `orden_recarga`, `orden_retiro`, `qr_transferencia`, `retencion_saldo`, `saldo_diario_billetera`, `solicitud_cierre_billetera`, `transferencia_p2p`. Reglas nombradas: `ck_cuenta_saldo_no_negativo`, `ck_cuenta_retenido_no_negativo`, `uq_cuenta_usuario_moneda`, `uq_cuenta_grupo_moneda`, `ck_cuenta_titularidad`, `uq_cuenta_custodia_inversion_moneda`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `numero_cuenta` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `tipo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `politica_billetera_id` | UUID | — | UUID, opcional, referencia a `politica_billetera`. | NINGUNA |
| `cuenta_contable_id` | UUID | — | UUID, opcional, referencia a `cuenta_contable`. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `nivel_debida_diligencia` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `saldo_disponible` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_retenido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_total` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria, calculada por la base, no se escribe. | FINANCIERO |
| `permite_saldo_negativo` | BOOLEAN | — | BOOLEAN, obligatoria. | FINANCIERO |
| `fecha_apertura` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_cierre` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `cuenta_contable`

**Qué es.** Cada bolsillo del sistema: la caja del grupo, la cuenta corriente de cada participante y el fondo de garantía.

**Para qué sirve.** Permite responder, en cualquier momento y de forma verificable: *¿cuánta plata hay en el grupo? ¿cuánto de eso le corresponde a cada uno? ¿cuánto es del fondo de garantía?* La cuenta corriente por participante (`participanteId`) es la que sostiene el estado de cuenta individual: cuánto aportó, cuánto cobró, cuál es su posición neta en el grupo. Ese número es el que se necesita cuando alguien se retira o cuando el grupo se disuelve anticipadamente (M8).

**Por qué existe.** Sin plan de cuentas, cada saldo se calcula con una consulta distinta escrita por una persona distinta, y ninguna coincide con las otras. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_contable`, `grupos.grupo`, `grupos.participante`. La referencian `catalogo.impuesto`, `cuenta_billetera`, `cuenta_contable`, `erp.categoria_activo_fijo`, `erp.linea_plantilla_asiento`, `erp.partida_presupuestaria`, `erp.tercero_comercial`, `garantia.fondo_garantia`, `movimiento_contable`, `tarifas.concepto_tarifa`. Reglas nombradas: `ck_cuenta_contable_padre_distinto`, `ck_cuenta_contable_nivel`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `naturaleza` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `cuenta_padre_id` | UUID | — | UUID, opcional, referencia a `cuenta_contable`. | NINGUNA |
| `nivel` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `es_cuenta_de_movimiento` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `saldo` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |

### `cuenta_custodia`

**Qué es.** La cuenta bancaria real donde está depositado el dinero que respalda todo el saldo de todas las billeteras.

**Para qué sirve.** Materializa la separación patrimonial: **el dinero de los usuarios no está en la caja de la empresa**. Está en una cuenta específica, idealmente en fideicomiso, cuyo único propósito es respaldar dinero electrónico. Si la plataforma quiebra, ese dinero no forma parte de la masa concursal.

**Por qué existe.** Porque sin ella el encaje es una afirmación, no un hecho verificable. Y porque es la primera cosa que revisa cualquier supervisión: "muéstrenme la cuenta y muéstrenme el extracto". ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `conciliacion_custodia`, `movimiento_custodia`. Reglas nombradas: `ck_cuenta_custodia_version_llave`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `entidad_financiera` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `numero_cuenta_cifrado` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | PERSONAL |
| `version_llave` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `numero_enmascarado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `saldo_segun_banco` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `saldo_segun_libro` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `fecha_saldo` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | FINANCIERO |
| `contrato_referencia` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `es_principal` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `abierta_en` | DATE | — | DATE, obligatoria. | NINGUNA |

### `descuadre_custodia`

**Qué es.** El expediente de cada diferencia detectada.

**Para qué sirve.** Distingue los cuatro tipos de diferencia —faltante, sobrante, desfase temporal y error de registro— porque cada uno tiene un dueño y un plazo distintos. Y obliga a escribir la explicación y el plan de acción: un descuadre sin explicación queda abierto y visible para siempre.

**Por qué existe.** Porque "hubo una diferencia de Bs 1.240 el 12 de mayo" sin expediente es exactamente el tipo de cabo suelto que aparece en una inspección dos años después, cuando ya nadie recuerda qué pasó. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `auditoria.incidente_operativo`, `conciliacion_custodia`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `conciliacion_custodia_id` | UUID | — | UUID, obligatoria, referencia a `conciliacion_custodia`. | NINGUNA |
| `incidente_operativo_id` | UUID | — | UUID, opcional, referencia a `incidente_operativo`. | NINGUNA |
| `resuelto_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `monto_diferencia` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `explicacion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `plan_accion` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `detectado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelto_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `discrepancia_proveedor`

**Qué es.** Una respuesta del proveedor de pagos que no se puede creer: firma inválida, estado que contradice al libro, monto o referencia distintos, o una respuesta mal formada.

**Para qué sirve.** Que una recarga o un retiro dudoso ni se acredite ni se descarte en silencio: queda para que una persona lo revise.

**Por qué existe.** No es lo mismo que un proveedor que no contesta. Un timeout deja el resultado desconocido y se vuelve a consultar; una firma que no coincide es evidencia y se conserva.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Reglas nombradas: `ck_discrepancia_moneda`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `referencia_tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_esperado` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `monto_informado` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), opcional. | NINGUNA |
| `detalle` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `huella` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | NINGUNA |
| `correlacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `detectada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `estado_cuenta_billetera`

**Qué es.** El extracto periódico emitido y archivado.

**Para qué sirve.** No basta con que el usuario pueda ver sus movimientos en pantalla: hay que **emitir** el extracto, guardarlo con su hash y poder demostrar que se puso a disposición. Esa es una obligación típica de transparencia con el consumidor financiero, y también lo que el usuario necesita para un trámite de crédito o un juicio. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`. Reglas nombradas: `ck_extracto_hash`, `ck_extracto_cuadra`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `periodo_desde` | DATE | — | DATE, obligatoria. | NINGUNA |
| `periodo_hasta` | DATE | — | DATE, obligatoria. | NINGUNA |
| `saldo_inicial` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `total_creditos` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_debitos` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `saldo_final` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `cantidad_movimientos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `url_archivo` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `hash_archivo` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `entregado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `evaluacion_antifraude`

**Qué es.** El motor de decisión en tiempo real y el registro de cada decisión.

**Para qué sirve.** La regla es **declarativa** (`expresion JSONB`): "más de tres retiros en una hora hacia un instrumento agregado hoy" se escribe como dato, no como `if`. La evaluación guarda el resultado, el puntaje, qué reglas se dispararon y cuánto tardó. Guardar la evaluación importa por dos razones opuestas: cuando el motor **acertó**, justifica ante el cliente por qué se le pidió MFA o se le rechazó una operación; cuando **se equivocó**, permite medir la tasa de falsos positivos y afinarlo. Sin registro, el motor antifraude es una caja negra que nadie puede mejorar ni defender. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `identidad.usuario`, `transaccion_billetera`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `revisada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motor_version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `puntaje_riesgo` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `decision` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `reglas_disparadas` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `latencia_ms` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `evaluada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revisada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `evidencia_mfa_consumida`

**Qué es.** La marca de que una evidencia de segundo factor (el `jti` del JWT de step-up) ya se usó.

**Para qué sirve.** Que una verificación de segundo factor autorice una sola operación sensible (un retiro, un cambio de cuenta): quien robe esa evidencia no puede reusarla.

**Por qué existe.** El JWT de evidencia es válido hasta que vence. Sin registrar su consumo, podría presentarse dos veces dentro de su vigencia.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `jti` | UUID | — | UUID, obligatoria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `proposito` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `consumida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `instrumento_fondeo`

**Qué es.** El medio por el que entra o sale plata: cuenta bancaria, tarjeta, QR, agente, efectivo.

**Para qué sirve.** Que el usuario no tenga que tipear su cuenta cada vez, y que la plataforma pueda exigir **coincidencia de titularidad**: `titular_coincide` es la bandera que impide sacar plata hacia la cuenta de un tercero. Ese solo control corta de raíz el uso de la billetera como puente de lavado y la mayoría de las estafas de "prestame tu cuenta".

**Por qué existe.** Es también el punto donde se decide qué se guarda y qué no: se persiste `token_proveedor` y `hash_identificador`, **nunca el número completo de tarjeta ni de cuenta en claro**.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `orden_recarga`, `orden_retiro`. Reglas nombradas: `ck_instrumento_sin_pan`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `entidad_financiera` | VARCHAR(60) | — | VARCHAR(60), obligatoria. | NINGUNA |
| `token_proveedor` | VARCHAR(255) | — | VARCHAR(255), opcional. | SECRETO |
| `hash_identificador` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `enmascarado` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `titular_nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `titular_documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `titular_coincide` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `es_principal` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `estado_verificacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `metodo_verificacion` | VARCHAR(20) | — | VARCHAR(20), opcional. | NINGUNA |
| `verificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `bloqueado_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `movimiento_billetera`

**Qué es.** Cada débito o crédito individual sobre una cuenta.

**Para qué sirve.** Es **el extracto**. Lo que el usuario ve cuando abre "mis movimientos", y lo que el auditor recorre cuando quiere reconstruir un saldo desde cero. `saldo_disponible_posterior` guarda el saldo resultante de cada movimiento: eso permite que el extracto muestre la columna de saldo corrido sin recalcular, y permite detectar de inmediato una inconsistencia (si el saldo posterior del movimiento N no coincide con el saldo previo del N+1, algo se insertó fuera de orden).

**Por qué existe.** Es el registro primario del que se deriva todo lo demás. Si se elimina, no hay sistema.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `cuenta_billetera`, `transaccion_billetera`. Reglas nombradas: `ix_movimiento_cuenta_fecha`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, obligatoria, referencia a `transaccion_billetera`. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `orden` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `sentido` | VARCHAR(7) | — | VARCHAR(7), obligatoria. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_disponible_posterior` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_retenido_posterior` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `glosa` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `registrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `movimiento_contable`

**Qué es.** Cada línea de un asiento: qué cuenta, cuánto al debe, cuánto al haber.

**Para qué sirve.** Es el detalle. Permite explicar un asiento en lenguaje entendible: "de la caja del grupo salieron Bs 4.800 (haber), entraron Bs 4.500 a la cuenta del beneficiario y Bs 300 a reposición del fondo de garantía (debe)".

**Por qué existe.** Sin las líneas no hay doble partida, solo un total.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `asiento_contable`, `cuenta_contable`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `asiento_id` | UUID | — | UUID, obligatoria, referencia a `asiento_contable`. | NINGUNA |
| `cuenta_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_contable`. | NINGUNA |
| `debe` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `haber` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |

### `movimiento_custodia`

**Qué es.** Cada movimiento real de esa cuenta bancaria.

**Para qué sirve.** Es el lado "banco" de la conciliación. Se enlaza con [[movimiento_bancario]] (M3) para reutilizar la ingesta de extractos que ya existe: no hay dos pipelines de conciliación, hay uno. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `aportes.movimiento_bancario`, `cuenta_custodia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_custodia_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_custodia`. | NINGUNA |
| `movimiento_bancario_id` | UUID | — | UUID, opcional, referencia a `movimiento_bancario`. | NINGUNA |
| `fecha_valor` | DATE | — | DATE, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `sentido` | VARCHAR(7) | — | VARCHAR(7), obligatoria. | NINGUNA |
| `monto` | DECIMAL(18,2) | — | DECIMAL(18,2), obligatoria. | FINANCIERO |
| `referencia_bancaria` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `glosa` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `conciliado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `registrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `orden_recarga`

**Qué es.** El *cash-in* y el *cash-out*: los dos puntos donde el dinero cruza la frontera entre el mundo bancario y el saldo de la app.

**Para qué sirve.** Son las operaciones de mayor riesgo del sistema, y por eso son asimétricas a propósito: - **La recarga es optimista**: se acredita cuando el banco confirma, se enlaza con [[pago]] (M3) y con el movimiento real de la cuenta de custodia. - **El retiro es pesimista**: exige MFA verificado, puede exigir doble aprobación por monto, tiene ventana de enfriamiento configurable y reserva el saldo con una retención antes de intentar el pago. Si el proveedor falla, la retención se libera; el saldo nunca queda "perdido en el limbo".

**Por qué existe.** Porque esas entidades hablan de *obligaciones del grupo*; estas hablan de *saldo del titular*. Un retiro no es una entrega de fondo: es una persona sacando su propio dinero, sin relación con ningún pasanaku.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.pago`, `aportes.proveedor_pago`, `cuenta_billetera`, `instrumento_fondeo`, `transaccion_billetera`. Reglas nombradas: `uq_recarga_idem`, `uq_recarga_referencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `instrumento_fondeo_id` | UUID | — | UUID, opcional, referencia a `instrumento_fondeo`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, opcional, referencia a `proveedor_pago`. | NINGUNA |
| `pago_id` | UUID | — | UUID, opcional, referencia a `pago`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `monto_bruto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `costo_proveedor` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `monto_acreditado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `medio` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `cotizacion_id` | UUID | — | UUID, opcional. | NINGUNA |
| `referencia_externa` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `acreditada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `orden_retiro`

**Qué es.** El *cash-in* y el *cash-out*: los dos puntos donde el dinero cruza la frontera entre el mundo bancario y el saldo de la app.

**Para qué sirve.** Son las operaciones de mayor riesgo del sistema, y por eso son asimétricas a propósito: - **La recarga es optimista**: se acredita cuando el banco confirma, se enlaza con [[pago]] (M3) y con el movimiento real de la cuenta de custodia. - **El retiro es pesimista**: exige MFA verificado, puede exigir doble aprobación por monto, tiene ventana de enfriamiento configurable y reserva el saldo con una retención antes de intentar el pago. Si el proveedor falla, la retención se libera; el saldo nunca queda "perdido en el limbo".

**Por qué existe.** Porque esas entidades hablan de *obligaciones del grupo*; estas hablan de *saldo del titular*. Un retiro no es una entrega de fondo: es una persona sacando su propio dinero, sin relación con ningún pasanaku.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.proveedor_pago`, `cuenta_billetera`, `identidad.usuario`, `instrumento_fondeo`, `retencion_saldo`, `transaccion_billetera`. La referencian `solicitud_cierre_billetera`. Reglas nombradas: `uq_retiro_idem`, `ck_retiro_mfa`, `ck_retiro_doble_aprobacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `instrumento_destino_id` | UUID | — | UUID, obligatoria, referencia a `instrumento_fondeo`. | NINGUNA |
| `retencion_id` | UUID | — | UUID, opcional, referencia a `retencion_saldo`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `solicitada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, opcional, referencia a `proveedor_pago`. | NINGUNA |
| `monto_solicitado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `costo_retiro` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `monto_neto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `mfa_verificado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_doble_aprobacion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `ventana_enfriamiento_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `cotizacion_id` | UUID | — | UUID, opcional. | NINGUNA |
| `referencia_proveedor` | VARCHAR(80) | — | VARCHAR(80), opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `pagada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `politica_billetera`

**Qué es.** Los parámetros de comportamiento de las billeteras, versionados.

**Para qué sirve.** Que "el retiro tiene 24 horas de enfriamiento" o "desde Bs 500 se pide MFA" sean **filas, no constantes en el código**. El día que el área de riesgos quiera bajar el umbral de MFA a Bs 200 porque apareció una modalidad de fraude nueva, eso es un `INSERT` con nueva vigencia y efecto inmediato, no un despliegue con ventana de cambio.

**Por qué existe.** Porque los parámetros de riesgo cambian con la realidad del fraude, que se mueve más rápido que un ciclo de release. Y porque cuando el regulador pregunta "¿desde cuándo aplicaban ese umbral?", `vigente_desde` responde con fecha exacta. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `cuenta_billetera`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `dias_inactividad_para_limitar` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `permite_transferencia_p2p` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `requiere_mfa_desde` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `ventana_enfriamiento_retiro_horas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `dias_vigencia_retencion` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `permite_saldo_negativo` | BOOLEAN | — | BOOLEAN, obligatoria. | FINANCIERO |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `qr_transferencia`

**Qué es.** Un QR de cobro que publica una billetera de Pasanaku, para que otra billetera le pague.

**Para qué sirve.** Cobrar entre personas o a un comercio dentro de la plataforma. El dinámico fija importe y vencimiento y se paga una sola vez; el estático no fija importe, no vence y se puede pagar muchas veces.

**Por qué existe.** Sin él, cobrar dentro de la plataforma exigiría pasar el número de billetera a mano. Además no se podría impedir que un QR dinámico se pague dos veces ni por un importe distinto del que se mostró.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `transaccion_billetera`. Reglas nombradas: `ck_qr_modalidad`, `ck_qr_uso`, `ck_qr_estatico_no_se_consume`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `modalidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `concepto` | VARCHAR(140) | — | VARCHAR(140), opcional. | NINGUNA |
| `estado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `usado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `regla_antifraude`

**Qué es.** El motor de decisión en tiempo real y el registro de cada decisión.

**Para qué sirve.** La regla es **declarativa** (`expresion JSONB`): "más de tres retiros en una hora hacia un instrumento agregado hoy" se escribe como dato, no como `if`. La evaluación guarda el resultado, el puntaje, qué reglas se dispararon y cuánto tardó. Guardar la evaluación importa por dos razones opuestas: cuando el motor **acertó**, justifica ante el cliente por qué se le pidió MFA o se le rechazó una operación; cuando **se equivocó**, permite medir la tasa de falsos positivos y afinarlo. Sin registro, el motor antifraude es una caja negra que nadie puede mejorar ni defender. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `expresion` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `accion` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `umbral_puntaje` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `respuesta_idempotente`

**Qué es.** La respuesta guardada de una operación de dinero, indexada por usuario, operación y clave de idempotencia.

**Para qué sirve.** Que si la app reintenta una recarga, un retiro o una aprobación porque se cortó la conexión, la persona reciba la misma respuesta y la plata no se mueva dos veces.

**Por qué existe.** La regla 91.1.4 exige idempotencia en toda operación de dinero. Sin la respuesta guardada, el reintento solo podría rechazarse, y la app no sabría si la primera vez salió bien.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. Reglas nombradas: `ck_respuesta_idem_hash`, `ck_respuesta_idem_expira`, `ck_respuesta_idem_http`, `ix_respuesta_idem_expiradas`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `operacion` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |
| `hash_solicitud` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `codigo_http` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `cuerpo_respuesta` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `registrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `retencion_saldo`

**Qué es.** Plata que sigue siendo del titular pero deja de estar disponible.

**Para qué sirve.** Resuelve seis situaciones que sin ella se resuelven mal: - **`APORTE_PROGRAMADO`**: el participante autorizó el débito automático del día 10. Se retiene el 10 y se ejecuta cuando corresponde. Sin retención, el usuario puede gastar el saldo el día 9 y el grupo se queda corto. - **`ENTREGA_EN_CURSO`**: la bolsa está lista para desembolsar; se congela para que nadie la mueva mientras se autoriza. - **`DISPUTA`** y **`ANTIFRAUDE`**: hay un reclamo o una alerta abierta sobre esa plata. - **`ORDEN_AUTORIDAD`**: llegó un oficio judicial. - **`COMISION_PENDIENTE`**: la comisión devengada que todavía no se cobró.

**Por qué existe.** Sin retenciones, la única forma de "apartar" plata es moverla a otra cuenta, lo que ensucia el extracto del usuario con movimientos que no entiende y rompe la trazabilidad. La retención es visible, reversible y explicable: *"Bs 500 reservados para tu aporte del 10 de marzo"*.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `identidad.usuario`, `transaccion_billetera`. La referencian `bloqueo_saldo`, `orden_retiro`. Reglas nombradas: `ck_retencion_referencia_motivo`, `ck_retencion_expira`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `transaccion_origen_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `liberada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), opcional. | NINGUNA |
| `referencia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(12) | — | VARCHAR(12), obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `liberada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `reverso_transaccion`

**Qué es.** El expediente de por qué se deshizo una operación.

**Para qué sirve.** Separar el *hecho* (la transacción compensatoria) de la *justificación* (quién lo autorizó, por qué, con qué respaldo). Cuatro tipos: anulación, contracargo, error operativo y orden de autoridad. Cada uno tiene consecuencias distintas: un error operativo alimenta el registro de pérdidas por riesgo operativo (M12); un contracargo puede generar cargo al usuario.

**Por qué existe.** Sin él, en el libro solo se ve que hubo un movimiento inverso, pero no por qué. Y "por qué" es la primera pregunta de toda auditoría. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `transaccion_billetera`. Reglas nombradas: `uq_reverso_original`, `ck_reverso_segregacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `transaccion_original_id` | UUID | — | UUID, obligatoria, referencia a `transaccion_billetera`. | NINGUNA |
| `transaccion_reverso_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `autorizada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_reversado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `saldo_diario_billetera`

**Qué es.** La foto del saldo de cada cuenta al cierre de cada día, encadenada por hash.

**Para qué sirve.** Responder "¿cuánto tenía esta persona el 31 de marzo?" sin recorrer millones de movimientos, y —más importante— **poder probar que la respuesta no fue fabricada después**. `hash_registro` incluye el hash del día anterior: alterar el saldo de un día obliga a recalcular todos los días siguientes, lo que es detectable de inmediato.

**Por qué existe.** Un embargo, una sucesión, una auditoría o un reclamo siempre preguntan por una fecha pasada. Sin esta tabla la única respuesta posible es "recalculamos y nos dio esto", que es exactamente la respuesta que un auditor no acepta.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `cuenta_billetera`. Reglas nombradas: `uq_saldo_diario_cuenta_fecha`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `fecha` | DATE | — | DATE, obligatoria. | NINGUNA |
| `saldo_disponible` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `saldo_retenido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `cantidad_movimientos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `hash_registro` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `hash_anterior` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `cerrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `solicitud_cierre_billetera`

**Qué es.** El proceso de cerrar la cuenta y devolver el saldo.

**Para qué sirve.** El derecho a irse con la plata es tan importante como el derecho a abrir. La solicitud valida que no queden obligaciones abiertas —aportes pendientes, deuda, retenciones vigentes, grupos activos— y deja constancia del destino del saldo.

**Por qué existe.** Porque el cierre de cuenta es el momento de mayor fricción y el que más reclamos genera. Sin expediente, es la palabra del usuario contra la del soporte. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_billetera`, `identidad.usuario`, `orden_retiro`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_billetera_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `orden_retiro_id` | UUID | — | UUID, opcional, referencia a `orden_retiro`. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `saldo_al_solicitar` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `destino_saldo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `transaccion_billetera`

**Qué es.** El sobre atómico que agrupa los movimientos de una operación.

**Para qué sirve.** Es **la unidad de "algo pasó con el dinero"**. Doce tipos, cada uno con un hecho de negocio detrás: recarga, retiro, aporte a grupo, entrega de fondo, cobro de comisión, cobro de impuesto, transferencia entre personas, cobertura del fondo de garantía, reposición, devolución, ajuste operativo y reverso. Que la transacción sea una entidad separada de sus movimientos es lo que permite: 1. **Idempotencia real.** `UNIQUE (clave_idempotencia)`. Si la app reintenta porque se cortó el 4G, o si la pasarela reenvía el webhook, la segunda vez no pasa nada. Este es el defecto más caro y más común de los sistemas de pago caseros: acreditar dos veces. 2. **Reversar en vez de editar.** Nunca se corrige un movimiento. Se emite una transacción de tipo `REVERSO` que lo compensa, y ambas quedan visibles. El extracto del usuario muestra el error *y* su corrección, que es lo que exige cualquier norma contable seria. 3. **Contexto forense.** `sesion_id`, `dispositivo_id`, `ip_origen` y `canal` quedan pegados a la transacción. Cuando alguien dice "yo no hice esa transferencia", esa fila es la que contesta.

**Por qué existe.** Sin sobre transaccional, cada movimiento sería independiente y no habría forma de garantizar que un aporte deje la bolsa cuadrada: podría acreditarse el crédito al grupo y fallar el débito al participante.

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `asiento_contable`, `grupos.grupo`, `identidad.dispositivo`, `identidad.sesion`, `identidad.usuario`. La referencian `cumplimiento.alerta_monitoreo_lft`, `cumplimiento.declaracion_origen_fondos`, `cumplimiento.registro_operacion_relevante`, `evaluacion_antifraude`, `movimiento_billetera`, `orden_recarga`, `orden_retiro`, `qr_transferencia`, `retencion_saldo`, `reverso_transaccion`, `tarifas.cargo_comision`, `tarifas.costo_proveedor_operacion`, `tarifas.devolucion_comision`, `transferencia_p2p`. Reglas nombradas: `uq_tx_idem`, `uq_tx_inversion_origen`, `uq_tx_rescate_origen`, `uq_tx_compra_mercado_origen`, `ix_transaccion_ocurrida`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `secuencia` | BIGSERIAL | — | BIGSERIAL, obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `sesion_id` | UUID | — | UUID, opcional, referencia a `sesion`. | NINGUNA |
| `dispositivo_id` | UUID | — | UUID, opcional, referencia a `dispositivo`. | NINGUNA |
| `iniciada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `origen_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `origen_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `canal` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ip_origen` | INET | — | INET, opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |
| `hash_registro` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `hash_anterior` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `ocurrida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `transferencia_p2p`

**Qué es.** Movimiento de saldo entre dos billeteras.

**Para qué sirve.** Es el mecanismo por el que **el aporte al pasanaku se vuelve instantáneo y gratuito**: el participante ya tiene saldo, el grupo tiene cuenta, y el aporte es una transferencia interna que no toca la red bancaria, no paga comisión de pasarela y se acredita en el acto. Ese es el argumento comercial central de la billetera. `obligacion_id` enlaza la transferencia con la [[obligacion_aporte]] que salda, para que un aporte hecho desde saldo tenga exactamente la misma trazabilidad que uno hecho por QR. ---

**Sistema.** Vive en el esquema `nucleo_financiero`; la escribe y la lee solo el servicio `nucleo-financiero` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.obligacion_aporte`, `cuenta_billetera`, `grupos.grupo`, `transaccion_billetera`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, obligatoria, referencia a `transaccion_billetera`. | NINGUNA |
| `cuenta_billetera_origen_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `cuenta_billetera_destino_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_billetera`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, opcional, referencia a `obligacion_aporte`. | NINGUNA |
| `monto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `concepto` | VARCHAR(140) | — | VARCHAR(140), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `organizador`

### `apelacion_sancion_org`

**Qué es.** El recurso del organizador contra una sanción.

**Para qué sirve.** Debido proceso. Las evaluaciones automáticas se equivocan: un grupo puede tener alta morosidad por razones ajenas al organizador (una crisis local, un participante que resultó ser un estafador). Sin apelación, un buen organizador puede ser sancionado por un dato que no controlaba. Esto pesa más en un modelo sin retribución: **si administrar no da ingreso y encima expone a sanciones injustas, nadie va a querer hacerlo.** La apelación es lo que mantiene el rol atractivo para la gente que uno quiere que lo asuma.

**Por qué existe.** Una sanción automática sin recurso es arbitraria, y disuade a la gente buena de asumir el rol. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `sancion_organizador`. Reglas nombradas: `uq_apelacion_por_sancion`, `ck_apelacion_org_resuelta`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `sancion_organizador_id` | UUID | — | UUID, obligatoria, referencia a `sancion_organizador`. | NINGUNA |
| `argumento` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `evidencias` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `resuelta_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `resolucion` | VARCHAR(400) | — | VARCHAR(400), opcional. | NINGUNA |
| `presentada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `resuelta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `capacitacion_organizador`

**Qué es.** Los módulos de formación que el organizador completó, con su nota y vigencia.

**Para qué sirve.** La mayoría de los problemas de un organizador no son mala fe: son desconocimiento. No sabe cómo manejar a alguien que se atrasa, no entiende qué puede y qué no puede decidir solo, no sabe cómo se calcula una liquidación. La capacitación reduce incidentes y le da a la plataforma un argumento sólido cuando tiene que sancionar: *se te capacitó en esto, aprobaste con esta nota, y aun así lo hiciste mal.* Un contenido se vuelve obligatorio en este modelo: **que administrar no se cobra**, y que pedir dinero por fuera de la plataforma es causal de inhabilitación. `vigenteHasta` obliga a recertificar cuando cambian las reglas.

**Por qué existe.** Sin registro de capacitación no se puede exigir formación como requisito ni distinguir el error de buena fe del incumplimiento consciente. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `organizador`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, obligatoria, referencia a `organizador`. | NINGUNA |
| `modulo` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `completada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `puntaje_evaluacion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `aprobada` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |

### `contrato_organizador`

**Qué es.** El contrato firmado entre el organizador y la plataforma, versionado y sellado con hash.

**Para qué sirve.** Es el documento que hace exigibles las obligaciones del organizador y que fundamenta las `causalesRescision`. Cuando hay que deshabilitar a alguien, la pregunta legal es "¿en base a qué?", y la respuesta es este contrato, en la versión que firmó, con su hash y su token de firma. **Con la comisión eliminada, este contrato carga el peso de la regla.** Es donde queda escrito, firmado y fechado que el organizador (a) no custodia fondos y (b) **no percibe contraprestación por administrar, ni dentro ni fuera de la plataforma**. Si alguien cobra "por debajo" a los participantes, está violando el contrato de forma documentada, y eso es lo que sostiene la inhabilitación.

**Por qué existe.** Sin contrato firmado y versionado, la relación con el organizador es informal, y sancionarlo o darlo de baja queda sin fundamento. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.token_verificacion`, `organizador`. Reglas nombradas: `ck_contrato_org_vigencia`, `ck_contrato_org_rescision`, `ck_contrato_org_firma`, `ex_contrato_org_vigente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, obligatoria, referencia a `organizador`. | NINGUNA |
| `version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `contenido_hash` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `obligaciones` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `causales_rescision` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `firmado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `token_firma_id` | UUID | — | UUID, opcional, referencia a `token_verificacion`. | SECRETO |
| `vigente_desde` | DATE | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | DATE, obligatoria. | NINGUNA |
| `vigente_hasta` | DATE | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | DATE, opcional. | NINGUNA |
| `rescindido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_rescision` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |

### `decision_habilitacion`

**Qué es.** Cada resolución sobre una postulación a organizador: aprobar, rechazar y, en revisión, confirmar o revocar.

**Para qué sirve.** Que habilitar a alguien para administrar grupos de plata ajena sea una decisión registrada, con autor y motivo, y que una revocación posterior quede tan documentada como la aprobación.

**Por qué existe.** Sin ella, la habilitación sería un estado en la solicitud: se perdería quién aprobó, con qué requisitos cumplidos y por qué se revocó después.

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Reglas nombradas: `ck_decision_habilitacion_fase`, `ck_decision_habilitacion_resultado`, `ck_decision_habilitacion_revision`, `ck_decision_habilitacion_motivo`, `ck_decision_habilitacion_coherencia`, `uq_decision_habilitacion_resolucion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `solicitud_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `clave_idempotencia` | UUID | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | UUID, obligatoria. | NINGUNA |
| `fase` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `decision` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `actor_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `motivo` | VARCHAR(1000) | — | VARCHAR(1000), obligatoria. | NINGUNA |
| `revision` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, opcional. | NINGUNA |
| `evidencia_requisitos` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `ocurrida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `correlacion_id` | UUID | — | UUID, obligatoria. | NINGUNA |

### `ejecucion_tarea`

**Qué es.** El registro de cada ejecución: cuándo, con qué resultado, cuántos registros afectó.

**Para qué sirve.** Es la **rendición de cuentas de la automatización**. Cuando un participante pregunta "¿por qué me aplicaron mora?", la respuesta es: la tarea de aplicación de mora del 6 de marzo a las 00:15 procesó 47 obligaciones vencidas, y la tuya era una. Con `detalle` en JSON, se puede reconstruir exactamente qué hizo. `resultado = PARCIAL` es realista: una tarea puede procesar 40 de 47 registros y fallar en 7. Sin ese estado, o se marca éxito (y se ocultan los 7 fallos) o se marca error (y se pierden los 40 éxitos).

**Por qué existe.** **Un sistema que actúa solo sobre el dinero de la gente tiene que poder explicar cada acción que tomó.** Sin registro de ejecución, la automatización es una caja negra que mueve plata, y eso es inaceptable.

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `tarea_automatizada`. Reglas nombradas: `ck_ejecucion_tarea_error`, `ck_ejecucion_tarea_fin`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tarea_id` | UUID | — | UUID, obligatoria, referencia a `tarea_automatizada`. | NINGUNA |
| `iniciada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `finalizada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `resultado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `registros_afectados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `detalle` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `mensaje_error` | TEXT | — | TEXT, opcional. | NINGUNA |

### `evaluacion_desempeno`

**Qué es.** La evaluación periódica del organizador sobre seis indicadores objetivos.

**Para qué sirve.** Implementa RN-22 y responde: *¿este organizador está haciendo bien su trabajo?* Con datos, no con impresiones: - `indiceMorosidadCartera`: sus grupos, ¿pagan a tiempo? - `tasaFinalizacionGrupos`: sus grupos, ¿terminan o se caen? - `satisfaccionParticipantes`: la gente que administró, ¿lo recomendaría? - `tiempoRespuestaPromedioHoras`: cuando alguien tiene un problema, ¿contesta? - `incidenciasAbiertas` y `coberturasConsumidas`: ¿cuánto le cuesta al sistema? `nivelSugerido` + `accionRecomendada` cierran el ciclo: la evaluación **tiene consecuencias automáticas**, hacia arriba (ascenso de nivel, más límite) o hacia abajo (reducción de límite, suspensión). Esta entidad gana importancia con la comisión eliminada. En un modelo con comisión, un organizador mediocre igual tiene el incentivo económico de seguir administrando. Sin ella, el único incentivo es el reconocimiento — **y este es el registro que lo produce**. Sin evaluación, administrar bien y administrar mal se ven exactamente igual, y no queda ningún motivo para hacerlo bien.

**Por qué existe.** Sin evaluación sistemática, un organizador que administra mal sigue acumulando grupos hasta que el daño es grande y visible. Y uno que administra bien no tiene forma de demostrarlo para crecer.

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `organizador`. La referencian `metrica_organizador`, `sancion_organizador`. Reglas nombradas: `uq_evaluacion_org_periodo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, obligatoria, referencia a `organizador`. | NINGUNA |
| `periodo_evaluado` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `indice_morosidad_cartera` | DECIMAL(5,2) | sus grupos, ¿pagan a tiempo? | DECIMAL(5,2), obligatoria. | NINGUNA |
| `tasa_finalizacion_grupos` | DECIMAL(5,2) | sus grupos, ¿terminan o se caen? | DECIMAL(5,2), obligatoria. | NINGUNA |
| `satisfaccion_participantes` | DECIMAL(3,2) | la gente que administró, ¿lo recomendaría? | DECIMAL(3,2), obligatoria. | NINGUNA |
| `tiempo_respuesta_promedio_horas` | DECIMAL(6,2) | cuando alguien tiene un problema, ¿contesta? | DECIMAL(6,2), obligatoria. | NINGUNA |
| `incidencias_abiertas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `coberturas_consumidas` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `puntaje_global` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `nivel_sugerido` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `accion_recomendada` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `evaluado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `metrica_organizador`

**Qué es.** El detalle de cada indicador dentro de la evaluación: valor, meta, si cumple y cuánto pesa.

**Para qué sirve.** Hace la evaluación **explicable y apelable**. El organizador no recibe "puntaje 62, nivel reducido"; recibe el desglose: morosidad 18% contra meta de 10% (no cumple, peso 0,35); tiempo de respuesta 4 h contra meta de 6 h (cumple, peso 0,15). Con eso sabe qué corregir, y si apela, sabe qué discutir.

**Por qué existe.** Una evaluación sin desglose es una calificación arbitraria, y sancionar en base a ella es indefendible. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evaluacion_desempeno`. Reglas nombradas: `uq_metrica_org_codigo`, `ck_metrica_org_peso`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `evaluacion_id` | UUID | — | UUID, obligatoria, referencia a `evaluacion_desempeno`. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `valor` | DECIMAL(12,4) | — | DECIMAL(12,4), obligatoria. | NINGUNA |
| `meta` | DECIMAL(12,4) | — | DECIMAL(12,4), obligatoria. | NINGUNA |
| `cumple` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `peso` | DECIMAL(4,3) | — | DECIMAL(4,3), obligatoria. | NINGUNA |

### `organizador`

**Qué es.** Un usuario habilitado para administrar grupos de terceros, con su nivel, sus límites y sus indicadores de cartera.

**Para qué sirve.** Convierte "el que organiza" en **un rol con requisitos, límites y consecuencias** — aunque no tenga retribución. Los campos clave: - `limiteGruposSimultaneos` y `limiteMontoAdministrado` son **control de riesgo puro**. Un organizador nuevo puede llevar dos grupos chicos; uno con historial probado, quince grupos grandes. Sin límites, alguien puede acumular cincuenta grupos y su fracaso arrastra a cientos de personas a la vez. - `nivel` (aprendiz → estándar → sénior → maestro) es la escalera de progresión. Sin comisión de por medio, el incentivo deja de ser económico y pasa a ser reputacional: administrar bien habilita a administrar más, y eso se refleja en el perfil público (M6). - `indiceMorosidadCartera` es **el indicador que más importa de todo el módulo**. Un organizador cuyos grupos tienen 30% de mora está haciendo mal el trabajo. Y ahora es el único indicador que importa, porque ya no hay un volumen de comisiones que compense un mal desempeño. - `calificacionPromedio` viene de las reseñas (M6) y captura la dimensión de trato.

**Por qué existe.** Porque ser organizador es un rol con estado propio (`POSTULADO → EN_EVALUACION → CAPACITACION_PENDIENTE → HABILITADO → LIMITADO → SUSPENDIDO → DESHABILITADO`), con métricas propias y con un proceso de habilitación que la cuenta de usuario no tiene. Y porque el mismo usuario es organizador en unos grupos y participante en otros. **Qué NO lleva, y por qué importa.** No tiene `cuentaCobroId` ni `comisionAcumulada()`: **no hay nada que pagarle**, así que no hay a dónde pagarle. Si además participa en el grupo que administra, lo hace como participante normal (M2), con sus propias obligaciones (M3) y su turno (M4) — por la vía de siempre, sin trato especial.

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `capacitacion_organizador`, `contrato_organizador`, `evaluacion_desempeno`, `grupos.grupo`, `publicidad.anunciante`, `sancion_organizador`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `nivel` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `limite_grupos_simultaneos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `limite_monto_administrado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `grupos_activos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `grupos_historicos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `monto_administrado_actual` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `calificacion_promedio` | DECIMAL(3,2) | — | DECIMAL(3,2), obligatoria. | NINGUNA |
| `indice_morosidad_cartera` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `fecha_postulacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_habilitacion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `fecha_suspension` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `version` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |

### `regla_automatizacion`

**Qué es.** Qué se automatiza, cuándo se dispara, bajo qué condición y qué acción ejecuta.

**Para qué sirve.** Es el cerebro configurable. Las seis acciones tipificadas son exactamente el trabajo del organizador: generar cobros, enviar recordatorios, aplicar mora, liquidar el período, ejecutar la entrega, escalar la cobranza. `requiereConfirmacionHumana` es el freno de seguridad **por acción, no por sistema**. Generar recordatorios puede ser totalmente automático; ejecutar una entrega de Bs 20.000 probablemente no. La granularidad permite automatizar lo rutinario sin automatizar lo irreversible.

**Por qué existe.** Con automatización hardcodeada no se puede ajustar qué se automatiza por tipo de grupo, ni desactivar una regla que está causando problemas sin apagar todo el sistema. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `tarea_automatizada`. Reglas nombradas: `ck_regla_confirmacion_humana`, `ck_regla_prioridad`, `uq_regla_automatizacion_prioridad`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `disparador` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `expresion_disparo` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `condicion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `accion` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `requiere_confirmacion_humana` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `requisito_habilitacion`

**Qué es.** El catálogo de requisitos por nivel: KYC, reputación, antigüedad, capacitación, garantía económica.

**Para qué sirve.** Permite endurecer o relajar la barrera de entrada según la etapa del negocio y según lo que la experiencia enseñe. Si aparece un patrón de organizadores nuevos que fallan en el primer grupo, se sube la antigüedad mínima sin desplegar código. `GARANTIA_ECONOMICA` como tipo es relevante: para niveles altos se puede exigir un depósito que responda si su gestión genera pérdidas. Sin comisión que retener, es **el único mecanismo económico que alinea el interés del organizador con el del grupo**.

**Por qué existe.** Con requisitos hardcodeados, cada ajuste es un release y no queda registro de qué se le exigió a un organizador habilitado hace un año. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `valor_minimo` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | NINGUNA |
| `es_obligatorio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `nivel_requerido` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `sancion_organizador`

**Qué es.** La consecuencia por mal desempeño: advertencia, reducción de límite, suspensión o inhabilitación.

**Para qué sirve.** La escalera es deliberadamente gradual. Se empieza por advertencia, no por inhabilitación, porque **inhabilitar a un organizador deja a todos sus grupos sin administrador** — el remedio puede ser peor que la enfermedad (aunque el organizador digital, en este modelo, es una red de contención razonable). `REDUCCION_LIMITE` es la sanción más útil del catálogo: no le quita los grupos que tiene —lo que perjudicaría a participantes que no hicieron nada—, pero le impide tomar más hasta que mejore. **Nota sobre el catálogo:** ya no existe `RETENCION_COMISION`. Sin comisión no hay qué retener, y por eso **todas las sanciones de este módulo son de habilitación, no económicas**. Es una limitación real del modelo y conviene tenerla presente: la única palanca económica disponible es la `GARANTIA_ECONOMICA` exigida como requisito de habilitación.

**Por qué existe.** Sin sanciones graduadas, la única alternativa es tolerar o expulsar, y ninguna de las dos sirve para la mayoría de los casos reales. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `evaluacion_desempeno`, `identidad.usuario`, `organizador`. La referencian `apelacion_sancion_org`. Reglas nombradas: `ck_sancion_org_vigencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, obligatoria, referencia a `organizador`. | NINGUNA |
| `evaluacion_id` | UUID | — | UUID, opcional, referencia a `evaluacion_desempeno`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aplicada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |

### `solicitud_organizador`

**Qué es.** La postulación de un usuario para ser habilitado como organizador.

**Para qué sirve.** Es la puerta de entrada, y tiene que ser una puerta real: **quien administra plata ajena tiene que pasar un filtro**, cobre o no cobre. La solicitud guarda motivación, experiencia declarada, referencias, el KYC reforzado y `puntajeReputacionAlSolicitar`: con qué historial se lo aceptó. Sin comisión de por medio, `motivacion` gana peso en la evaluación: hay que entender por qué alguien quiere asumir el trabajo sin retribución. Los motivos legítimos abundan (organiza el pasanaku de su oficina, de su familia, de su gremio), pero es exactamente la pregunta que hay que hacerle a quien no tiene vínculo previo con el grupo que quiere administrar. `motivoRechazo` y `revisadaPor` cierran el proceso: el rechazo tiene autor y fundamento, y el postulante sabe qué le faltó para volver a intentar.

**Por qué existe.** Sin solicitud formal, ser organizador sería marcar una casilla, y la plataforma estaría entregando la administración de dinero ajeno sin evaluación ni registro de quién autorizó qué. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `identidad.verificacion_kyc`. Reglas nombradas: `uq_solicitud_organizador_pendiente`, `ck_solicitud_org_resuelta`, `ck_solicitud_org_rechazo_motivado`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `motivacion` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `experiencia_declarada` | TEXT | — | TEXT, obligatoria. | NINGUNA |
| `kyc_reforzado_id` | UUID | — | UUID, opcional, referencia a `verificacion_kyc`. | NINGUNA |
| `puntaje_reputacion_al_solicitar` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `revisada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `motivo_rechazo` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `fecha_solicitud` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_resolucion` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `tarea_automatizada`

**Qué es.** Una instancia concreta de trabajo programada por una regla.

**Para qué sirve.** Hace visible lo que el sistema va a hacer **antes de hacerlo**. `tareasPendientes()` es la agenda del organizador digital, y en modo `SOLO_SUGIERE` es literalmente la lista de sugerencias que el humano revisa. `REQUIERE_APROBACION` como estado materializa el freno: la tarea está lista, sabe qué hacer, y espera el visto bueno.

**Por qué existe.** `claveIdempotencia` `UNIQUE` es crítica: **impide que un reintento del planificador genere cobros o recordatorios duplicados**. Un cron que se ejecuta dos veces por un problema de infraestructura no puede cobrarle dos veces a nadie ni mandar el recordatorio dos veces. ---

**Sistema.** Vive en el esquema `organizador`; la escribe y la lee solo el servicio `organizador` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `regla_automatizacion`. La referencian `ejecucion_tarea`. Reglas nombradas: `uq_tarea_automatizada_clave`, `ck_tarea_intentos`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `regla_id` | UUID | — | UUID, obligatoria, referencia a `regla_automatizacion`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `tipo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `programada_para` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(80) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(80), obligatoria. | NINGUNA |

## Esquema `publicidad`

### `anunciante`

**Qué es.** la entidad que efectivamente contrata: `tipo` decide si el anunciante es un `organizador` existente (M07) o un `socio_comercial` nuevo, con un CHECK que exige exactamente una de las dos referencias según el tipo. Todo lo demás del módulo (cuenta publicitaria, campaña, factura) cuelga de `anunciante`, no de `organizador` ni de `socio_comercial` directamente — así el resto del modelo no necesita saber si detrás hay un organizador o un tercero.

**Por qué existe.** sin `anunciante`, cada tabla de campaña necesitaría dos FK opcionales (una a organizador, otra a socio comercial) repetidas en cada entidad de la cadena. Con `anunciante` como punto único, el resto del módulo es agnóstico a quién hay detrás.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `organizador.organizador`, `socio_comercial`. La referencian `cuenta_publicitaria`, `pieza_creativa`. Reglas nombradas: `ck_anunciante_tipo_exclusivo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `organizador_id` | UUID | — | UUID, opcional, referencia a `organizador`. | NINGUNA |
| `socio_comercial_id` | UUID | — | UUID, opcional, referencia a `socio_comercial`. | NINGUNA |
| `razon_social_facturacion` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `anuncio`

**Qué es.** Une un conjunto de anuncios con una pieza creativa aprobada.

**Por qué existe.** una pieza creativa puede reutilizarse en varios `anuncio` (la misma imagen en distintos conjuntos), y puede volver a revisión si el anunciante la reemplaza. Fusionarla con `anuncio` obligaría a re-moderar y a duplicar el archivo cada vez que se reutiliza.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `conjunto_anuncios`, `pieza_creativa`. La referencian `impresion_anuncio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `conjunto_anuncios_id` | UUID | — | UUID, obligatoria, referencia a `conjunto_anuncios`. | NINGUNA |
| `pieza_creativa_id` | UUID | — | UUID, obligatoria, referencia a `pieza_creativa`. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `iniciado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `finalizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `campana_publicitaria`

**Qué es.** Objetivo, presupuesto total y vigencia de una campaña.

**Para qué sirve.** separa "quién es el anunciante" de "cuánto está gastando y en qué". Un mismo anunciante puede tener varias campañas activas (una para captar postulantes a su grupo, otra para descargas de la app) y el límite de gasto se controla a nivel de cuenta, no de campaña, para que el anunciante no pueda desbordar su presupuesto abriendo campañas nuevas.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cuenta_publicitaria`, `identidad.usuario`. La referencian `conjunto_anuncios`. Reglas nombradas: `ck_campana_pub_consumo`, `ck_campana_pub_aprobacion`, `ck_campana_pub_vigencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_publicitaria_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_publicitaria`. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `objetivo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `presupuesto_total` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `presupuesto_consumido` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `fecha_inicio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_fin` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |

### `clic_anuncio`

**Qué es.** Un evento de clic, derivado de una impresión.

**Por qué existe.** `impresion_anuncio` y `clic_anuncio` pueden crecer mucho más rápido que cualquier otra tabla del sistema. Se marcaron `APPEND_ONLY` desde ya. **No** se particionaron por fecha todavía porque `conversion_anuncio` las referencia por FK, y este modelo solo particiona tablas que nadie referencia (ver `scripts/modelo.py`, `PARTICIONADAS`, y la nota de `bitacora_evento`/`notificacion` sobre el mismo trade-off). Cuando el volumen lo justifique, la migración es mover esas FK a clave compuesta antes de particionar — está declarado como riesgo en el plan de implementación, no ignorado. **Cómo se factura, sin inventar un tercer libro**: `factura_publicidad` es la liquidación de **origen** — cuánto gastó una cuenta publicitaria en un período — y dispara dos caminos que ya existen: `factura_electronica` (M11) para el comprobante fiscal ante el SIN, y `cuenta_por_cobrar` (M13) para el seguimiento de cobro. No hay un tercer sistema de facturación paralelo: publicidad reutiliza lo que la empresa ya tiene para cobrarle a alguien que no es un participante del pasanaku.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `identidad.usuario`, `impresion_anuncio`. La referencian `conversion_anuncio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `impresion_id` | UUID | — | UUID, obligatoria, referencia a `impresion_anuncio`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `clic_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `costo` | DECIMAL(10,4) | — | DECIMAL(10,4), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |

### `conjunto_anuncios`

**Qué es.** Une una campaña con un segmento, un espacio y un presupuesto/puja concretos — el equivalente al *ad set* de Meta.

**Por qué existe.** el inventario de una app financiera es limitado y sensible — no se puede poner un banner de un tercero en medio del flujo de pago. Tener el inventario como catálogo con `capacidad_maxima_simultanea` permite controlar cuántos anuncios compiten por el mismo espacio a la vez, y qué espacios existen es una decisión de producto, no algo que cualquier anunciante inventa.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `campana_publicitaria`, `espacio_publicitario`, `segmento_audiencia`. La referencian `anuncio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `campana_publicitaria_id` | UUID | — | UUID, obligatoria, referencia a `campana_publicitaria`. | NINGUNA |
| `segmento_audiencia_id` | UUID | — | UUID, obligatoria, referencia a `segmento_audiencia`. | NINGUNA |
| `espacio_publicitario_id` | UUID | — | UUID, obligatoria, referencia a `espacio_publicitario`. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `presupuesto_diario` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `puja_maxima` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `modelo_puja` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `conversion_anuncio`

**Qué es.** Un resultado atribuido (postulación a un grupo, registro, descarga).

**Por qué existe.** `impresion_anuncio` y `clic_anuncio` pueden crecer mucho más rápido que cualquier otra tabla del sistema. Se marcaron `APPEND_ONLY` desde ya. **No** se particionaron por fecha todavía porque `conversion_anuncio` las referencia por FK, y este modelo solo particiona tablas que nadie referencia (ver `scripts/modelo.py`, `PARTICIONADAS`, y la nota de `bitacora_evento`/`notificacion` sobre el mismo trade-off). Cuando el volumen lo justifique, la migración es mover esas FK a clave compuesta antes de particionar — está declarado como riesgo en el plan de implementación, no ignorado. **Cómo se factura, sin inventar un tercer libro**: `factura_publicidad` es la liquidación de **origen** — cuánto gastó una cuenta publicitaria en un período — y dispara dos caminos que ya existen: `factura_electronica` (M11) para el comprobante fiscal ante el SIN, y `cuenta_por_cobrar` (M13) para el seguimiento de cobro. No hay un tercer sistema de facturación paralelo: publicidad reutiliza lo que la empresa ya tiene para cobrarle a alguien que no es un participante del pasanaku.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `clic_anuncio`, `impresion_anuncio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `clic_id` | UUID | — | UUID, opcional, referencia a `clic_anuncio`. | NINGUNA |
| `impresion_id` | UUID | — | UUID, opcional, referencia a `impresion_anuncio`. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, opcional. | NINGUNA |
| `ocurrida_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `cuenta_publicitaria`

**Qué es.** La cuenta de facturación de un anunciante: límite de gasto mensual, saldo consumido, estado.

**Para qué sirve.** separa "quién es el anunciante" de "cuánto está gastando y en qué". Un mismo anunciante puede tener varias campañas activas (una para captar postulantes a su grupo, otra para descargas de la app) y el límite de gasto se controla a nivel de cuenta, no de campaña, para que el anunciante no pueda desbordar su presupuesto abriendo campañas nuevas.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `anunciante`. La referencian `campana_publicitaria`, `factura_publicidad`. Reglas nombradas: `uq_cuenta_publicitaria_anunciante`, `ck_cuenta_publicitaria_consumo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `anunciante_id` | UUID | — | UUID, obligatoria, referencia a `anunciante`. | NINGUNA |
| `limite_gasto_mensual` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `saldo_consumido_mes` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `espacio_publicitario`

**Qué es.** El catálogo de inventario propio de la app: dónde puede aparecer un anuncio.

**Por qué existe.** el inventario de una app financiera es limitado y sensible — no se puede poner un banner de un tercero en medio del flujo de pago. Tener el inventario como catálogo con `capacidad_maxima_simultanea` permite controlar cuántos anuncios compiten por el mismo espacio a la vez, y qué espacios existen es una decisión de producto, no algo que cualquier anunciante inventa.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `conjunto_anuncios`. Reglas nombradas: `ck_espacio_pub_capacidad`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `capacidad_maxima_simultanea` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `factura_publicidad`

**Qué es.** La liquidación periódica de gasto de una cuenta publicitaria.

**Por qué existe.** `impresion_anuncio` y `clic_anuncio` pueden crecer mucho más rápido que cualquier otra tabla del sistema. Se marcaron `APPEND_ONLY` desde ya. **No** se particionaron por fecha todavía porque `conversion_anuncio` las referencia por FK, y este modelo solo particiona tablas que nadie referencia (ver `scripts/modelo.py`, `PARTICIONADAS`, y la nota de `bitacora_evento`/`notificacion` sobre el mismo trade-off). Cuando el volumen lo justifique, la migración es mover esas FK a clave compuesta antes de particionar — está declarado como riesgo en el plan de implementación, no ignorado. **Cómo se factura, sin inventar un tercer libro**: `factura_publicidad` es la liquidación de **origen** — cuánto gastó una cuenta publicitaria en un período — y dispara dos caminos que ya existen: `factura_electronica` (M11) para el comprobante fiscal ante el SIN, y `cuenta_por_cobrar` (M13) para el seguimiento de cobro. No hay un tercer sistema de facturación paralelo: publicidad reutiliza lo que la empresa ya tiene para cobrarle a alguien que no es un participante del pasanaku.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `cuenta_publicitaria`, `erp.cuenta_por_cobrar`, `tarifas.factura_electronica`. Reglas nombradas: `uq_factura_publicidad_cuenta_periodo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `cuenta_publicitaria_id` | UUID | — | UUID, obligatoria, referencia a `cuenta_publicitaria`. | NINGUNA |
| `periodo` | VARCHAR(7) | — | VARCHAR(7), obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `factura_electronica_id` | UUID | — | UUID, opcional, referencia a `factura_electronica`. | NINGUNA |
| `cuenta_por_cobrar_id` | UUID | — | UUID, opcional, referencia a `cuenta_por_cobrar`. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `generada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `impresion_anuncio`

**Qué es.** Un evento de exhibición.

**Por qué existe.** `impresion_anuncio` y `clic_anuncio` pueden crecer mucho más rápido que cualquier otra tabla del sistema. Se marcaron `APPEND_ONLY` desde ya. **No** se particionaron por fecha todavía porque `conversion_anuncio` las referencia por FK, y este modelo solo particiona tablas que nadie referencia (ver `scripts/modelo.py`, `PARTICIONADAS`, y la nota de `bitacora_evento`/`notificacion` sobre el mismo trade-off). Cuando el volumen lo justifique, la migración es mover esas FK a clave compuesta antes de particionar — está declarado como riesgo en el plan de implementación, no ignorado. **Cómo se factura, sin inventar un tercer libro**: `factura_publicidad` es la liquidación de **origen** — cuánto gastó una cuenta publicitaria en un período — y dispara dos caminos que ya existen: `factura_electronica` (M11) para el comprobante fiscal ante el SIN, y `cuenta_por_cobrar` (M13) para el seguimiento de cobro. No hay un tercer sistema de facturación paralelo: publicidad reutiliza lo que la empresa ya tiene para cobrarle a alguien que no es un participante del pasanaku.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `anuncio`, `identidad.usuario`. La referencian `clic_anuncio`, `conversion_anuncio`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `anuncio_id` | UUID | — | UUID, obligatoria, referencia a `anuncio`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `mostrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `costo` | DECIMAL(10,4) | — | DECIMAL(10,4), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |

### `pieza_creativa`

**Qué es.** La imagen/texto/video concreto de un anuncio.

**Por qué existe.** una pieza creativa puede reutilizarse en varios `anuncio` (la misma imagen en distintos conjuntos), y puede volver a revisión si el anunciante la reemplaza. Fusionarla con `anuncio` obligaría a re-moderar y a duplicar el archivo cada vez que se reutiliza.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `anunciante`. La referencian `anuncio`, `revision_creativa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `anunciante_id` | UUID | — | UUID, obligatoria, referencia a `anunciante`. | NINGUNA |
| `titulo` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `texto` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `url_recurso` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `tipo_recurso` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `estado_moderacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `revision_creativa`

**Qué es.** La aprobación o rechazo de una pieza, con motivo.

**Por qué existe.** una pieza creativa puede reutilizarse en varios `anuncio` (la misma imagen en distintos conjuntos), y puede volver a revisión si el anunciante la reemplaza. Fusionarla con `anuncio` obligaría a re-moderar y a duplicar el archivo cada vez que se reutiliza.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `pieza_creativa`. Reglas nombradas: `ck_revision_creativa_motivo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `pieza_creativa_id` | UUID | — | UUID, obligatoria, referencia a `pieza_creativa`. | NINGUNA |
| `revisada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `decision` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `revisada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `segmento_audiencia`

**Qué es.** Un criterio de targeting reutilizable (ubicación, nivel de KYC, actividad en grupos, etc.).

**Por qué existe.** el inventario de una app financiera es limitado y sensible — no se puede poner un banner de un tercero en medio del flujo de pago. Tener el inventario como catálogo con `capacidad_maxima_simultanea` permite controlar cuántos anuncios compiten por el mismo espacio a la vez, y qué espacios existen es una decisión de producto, no algo que cualquier anunciante inventa.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `conjunto_anuncios`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `nombre` | VARCHAR(100) | — | VARCHAR(100), obligatoria. | NINGUNA |
| `criterios` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `reutilizable` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `creado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `socio_comercial`

**Qué es.** un negocio externo (una marca, un comercio) que no tiene rol operativo en la plataforma. No es `usuario` ni `organizador`: es alguien que quiere visibilidad ante la base de usuarios de AportaYa sin participar de ningún pasanaku.

**Por qué existe.** sin `anunciante`, cada tabla de campaña necesitaría dos FK opcionales (una a organizador, otra a socio comercial) repetidas en cada entidad de la cadena. Con `anunciante` como punto único, el resto del módulo es agnóstico a quién hay detrás.

**Sistema.** Vive en el esquema `publicidad`; la escribe y la lee solo el servicio `publicidad` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `anunciante`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `razon_social` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `numero_documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `rubro` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `email_contacto` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | PERSONAL |
| `telefono_contacto` | VARCHAR(20) | — | VARCHAR(20), opcional. | PERSONAL |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `verificado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `creado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

## Esquema `tarifas`

### `aplicacion_promocion`

**Qué es.** No cobrar, con nombre y apellido.

**Para qué sirve.** Toda plataforma termina regalando comisiones: por campaña, por cortesía tras un error, por convenio institucional. La diferencia entre hacerlo bien y hacerlo mal es si queda registro de **quién lo autorizó, por qué motivo, con qué tope y hasta cuándo**. `campana_promocional` agrega presupuesto: una promoción con `presupuesto_maximo` se apaga sola cuando lo consume, en vez de descubrirse a fin de mes.

**Por qué existe.** Porque sin ellas la exención se implementa como un `monto = 0` a mano, y entonces es imposible distinguir "no correspondía cobrar" de "alguien no cobró". ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `campana_promocional`, `devengo_comision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `campana_id` | UUID | — | UUID, obligatoria, referencia a `campana_promocional`. | NINGUNA |
| `devengo_id` | UUID | — | UUID, obligatoria, referencia a `devengo_comision`. | NINGUNA |
| `monto_descontado` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `aplicada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `asignacion_tarifario`

**Qué es.** A quién se le aplica cada tarifario y con qué prioridad.

**Para qué sirve.** Resuelven la pregunta "¿qué precio le corresponde a esta operación?" con una jerarquía explícita: usuario > grupo > segmento > global. La asignación puede tener vigencia y motivo, de modo que un precio especial siempre tiene fecha de inicio, fecha de fin y una razón escrita.

**Por qué existe.** Porque los precios especiales existen igual: si el modelo no los soporta, aparecen como excepciones a mano en producción, que es la peor forma posible de tenerlos. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `grupos.grupo`, `identidad.usuario`, `segmento_comercial`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `segmento_id` | UUID | — | UUID, opcional, referencia a `segmento_comercial`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `autorizado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `ambito` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |

### `calculo_impuesto`

**Qué es.** Las alícuotas vigentes y su aplicación a cada devengo.

**Para qué sirve.** Que el impuesto sea una fila con `vigente_desde` y `base_legal`, y que su cálculo quede guardado por devengo. Cuando cambia una alícuota, el histórico conserva la que se aplicó, que es lo que exige cualquier revisión tributaria. `precio_incluye_impuesto` en el concepto decide si el precio publicado es final o si el impuesto se suma encima. Para el consumidor final, el precio publicado tiene que ser el final: la comisión de Bs 18 es Bs 18, impuestos incluidos. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.impuesto`, `devengo_comision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `devengo_id` | UUID | — | UUID, obligatoria, referencia a `devengo_comision`. | NINGUNA |
| `impuesto_id` | UUID | — | UUID, obligatoria, referencia a `impuesto`. | NINGUNA |
| `base_imponible` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | NINGUNA |
| `alicuota_aplicada` | DECIMAL(6,4) | — | DECIMAL(6,4), obligatoria. | NINGUNA |
| `monto_impuesto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `incluido_en_precio` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `periodo_fiscal` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |

### `cambio_tarifario`

**Qué es.** El ensayo previo y el expediente del cambio.

**Para qué sirve.** La simulación permite correr el tarifario nuevo sobre la historia real antes de publicarlo: cuánto más se habría cobrado, a cuánta gente le habría subido y en cuánto. Eso convierte una decisión de precios en una decisión informada y deja constancia de que se evaluó el impacto. `cambio_tarifario` guarda el cumplimiento del deber de aviso: tipo de cambio, días de preaviso, fecha y canal del aviso, cuántos usuarios fueron notificados y si el cambio habilita rescindir sin costo. Un aumento de comisiones sin aviso previo documentado es una observación segura. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tarifario_anterior_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `tarifario_nuevo_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `aprobado_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo_cambio` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `requiere_preaviso` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `dias_preaviso` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `fecha_aviso` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `canal_aviso` | VARCHAR(40) | — | VARCHAR(40), opcional. | NINGUNA |
| `usuarios_notificados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `permite_rescision_sin_costo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `publicado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `campana_promocional`

**Qué es.** No cobrar, con nombre y apellido.

**Para qué sirve.** Toda plataforma termina regalando comisiones: por campaña, por cortesía tras un error, por convenio institucional. La diferencia entre hacerlo bien y hacerlo mal es si queda registro de **quién lo autorizó, por qué motivo, con qué tope y hasta cuándo**. `campana_promocional` agrega presupuesto: una promoción con `presupuesto_maximo` se apaga sola cuando lo consume, en vez de descubrirse a fin de mes.

**Por qué existe.** Porque sin ellas la exención se implementa como un `monto = 0` a mano, y entonces es imposible distinguir "no correspondía cobrar" de "alguien no cobró". ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `aplicacion_promocion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `presupuesto_maximo` | DECIMAL(16,2) | — | DECIMAL(16,2), opcional. | NINGUNA |
| `presupuesto_consumido` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `condiciones` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aprobada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |

### `cargo_comision`

**Qué es.** El intento —y el resultado— de cobrar efectivamente el devengo.

**Para qué sirve.** Materializa las tres vías de cobro y las mantiene unificadas: - **`DEDUCCION_DE_ENTREGA`** → una línea de [[deduccion_entrega]] (M4). Es la vía por defecto: la plataforma cobra donde ya tiene el dinero. - **`DEBITO_DE_BILLETERA`** → una [[transaccion_billetera]] (M10). - **`OBLIGACION_DE_APORTE`** → una [[obligacion_aporte]] (M3), para el escenario de prorrateo entre participantes. Que las tres vías cuelguen del mismo devengo es lo que permite cambiar la forma de cobro por configuración: el devengo no cambia, cambia por dónde se cobra.

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.obligacion_aporte`, `devengo_comision`, `entregas.deduccion_entrega`, `nucleo_financiero.transaccion_billetera`. Reglas nombradas: `uq_cargo_deduccion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `devengo_id` | UUID | — | UUID, obligatoria, referencia a `devengo_comision`. | NINGUNA |
| `deduccion_entrega_id` | UUID | — | UUID, opcional, referencia a `deduccion_entrega`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `obligacion_id` | UUID | — | UUID, opcional, referencia a `obligacion_aporte`. | NINGUNA |
| `forma_cobro` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `monto_cobrado` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `intentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `ultimo_error` | VARCHAR(300) | — | VARCHAR(300), opcional. | NINGUNA |
| `cobrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `catalogo_hecho_generador`

**Qué es.** El catálogo de eventos del sistema sobre los que se puede cobrar.

**Para qué sirve.** Es el vocabulario del motor. Un concepto de tarifa no dice "cobrar cuando alguien cobre su turno" en prosa: apunta a un hecho del catálogo, que a su vez sabe de qué tabla viene y qué campo usar como base. Eso es lo que hace posible agregar un cobro nuevo sin programar la fórmula.

**Por qué existe.** Sin catálogo, cada concepto tendría que traer su propia lógica de "cuándo dispara", y esa lógica terminaría en el código. Con catálogo, el motor escucha eventos de dominio (M9) y busca qué conceptos están enganchados a ese hecho. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `concepto_tarifa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `entidad_evento` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `campo_monto_base` | VARCHAR(40) | — | VARCHAR(40), opcional. | FINANCIERO |
| `unidad_conteo` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `modulo_origen` | CHAR(2) | — | CHAR(2), obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `concepto_tarifa`

**Qué es.** Un cobro concreto: nombre comercial, fórmula, sujeto, vía y momento.

**Para qué sirve.** Es **la fila donde vive la política**. Vale la pena leer sus seis columnas de decisión como una oración: > Sobre `hecho_generador_id`, calcular con `metodo_calculo` sobre `base_calculo`, > a cargo de `sujeto_obligado`, cobrando por `forma_cobro` en el momento > `momento_cobro`. `nombre_comercial` y `descripcion_usuario` existen para que la app muestre *"Comisión por cobro de turno — se descuenta de la bolsa cuando cobrás tu turno"* y no `COM_ENTREGA`. Un cobro que el usuario no puede leer en su idioma es un reclamo esperando a suceder. `gravado_iva`, `gravado_it` y `precio_incluye_impuesto` definen el tratamiento tributario por concepto, porque no todos los cobros tributan igual y porque el precio que se muestra tiene que ser el precio final.

**Por qué existe.** Porque la alternativa —constantes, `if`s por tipo de operación, o un campo `porcentaje_comision` en `configuracion_grupo`— hace que cada cambio comercial sea un cambio de software, con su ciclo de QA, su despliegue y su riesgo. Y hace imposible contestar qué se cobraba el año pasado.

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `catalogo_hecho_generador`, `nucleo_financiero.cuenta_contable`, `politica_redondeo`. La referencian `cotizacion_comision`, `devengo_comision`, `exencion_comision`, `regla_tarifa`. Reglas nombradas: `ck_concepto_metodo`, `ck_concepto_piso_techo`, `ck_concepto_precio_final`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `hecho_generador_id` | UUID | — | UUID, obligatoria, referencia a `catalogo_hecho_generador`. | NINGUNA |
| `politica_redondeo_id` | UUID | — | UUID, opcional, referencia a `politica_redondeo`. | NINGUNA |
| `cuenta_ingreso_id` | UUID | — | UUID, opcional, referencia a `cuenta_contable`. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `nombre_comercial` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `descripcion_usuario` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `metodo_calculo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `base_calculo` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `valor_porcentual` | DECIMAL(7,4) | — | DECIMAL(7,4), opcional. | NINGUNA |
| `valor_fijo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | NINGUNA |
| `monto_minimo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | FINANCIERO |
| `monto_maximo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | FINANCIERO |
| `sujeto_obligado` | VARCHAR(35) | — | VARCHAR(35), obligatoria. | NINGUNA |
| `forma_cobro` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `momento_cobro` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `gravado_iva` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `gravado_it` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `precio_incluye_impuesto` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `orden_aplicacion` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `costo_proveedor_operacion`

**Qué es.** Lo que cobra la pasarela, el banco o el agente por cada operación.

**Para qué sirve.** Saber el **margen real**. Una comisión de Bs 18 con un costo de pasarela de Bs 14 es un negocio distinto del que parece. Sin esta tabla, el costo aparece agregado a fin de mes y nunca se puede atribuir a la operación que lo generó, ni decidir con datos qué proveedor conviene para qué rango de monto. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `aportes.proveedor_pago`, `liquidacion_ingresos`, `nucleo_financiero.transaccion_billetera`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `proveedor_id` | UUID | — | UUID, obligatoria, referencia a `proveedor_pago`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `liquidacion_ingresos_id` | UUID | — | UUID, opcional, referencia a `liquidacion_ingresos`. | NINGUNA |
| `tipo_operacion` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `monto_operacion` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `costo_fijo` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `costo_porcentual` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `costo_total` | DECIMAL(10,2) | — | DECIMAL(10,2), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `periodo` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `conciliado_con_factura` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `cotizacion_comision`

**Qué es.** El cálculo mostrado al usuario **antes** de que la operación ocurra.

**Para qué sirve.** Que la pantalla diga *"vas a recibir Bs 5.982: Bs 6.000 menos Bs 18 de comisión"* y que ese número quede guardado con su desglose. Cuando el usuario reclama, no se discute contra un recálculo: se muestra la cotización que él vio y aceptó.

**Por qué existe.** Porque el reclamo típico no es "me cobraron de más", es "nadie me avisó". La cotización es la prueba del aviso. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `concepto_tarifa`. La referencian `devengo_comision`. Reglas nombradas: `uq_cotizacion_idem`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `concepto_tarifa_id` | UUID | — | UUID, obligatoria, referencia a `concepto_tarifa`. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `monto_base` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_comision` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_impuesto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_total` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `desglose` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `valida_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `mostrada_al_usuario_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `aceptada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |

### `cuenta_por_cobrar_comision`

**Qué es.** Los dos finales alternativos del devengo: se devuelve o queda por cobrar.

**Para qué sirve.** La devolución tipifica el motivo —entrega anulada, error de tarifa, reclamo procedente, falla de servicio— porque cada uno alimenta un indicador distinto: los errores de tarifa son deuda técnica, las fallas de servicio son riesgo operativo, y los reclamos procedentes son el indicador que mira el defensor del consumidor financiero. La cuenta por cobrar conecta con la gestión de cobranza (M8), reutilizando toda la maquinaria que ya existe para la mora de aportes. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `devengo_comision`, `garantia.gestion_cobranza`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `devengo_id` | UUID | — | UUID, obligatoria, referencia a `devengo_comision`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `gestion_cobranza_id` | UUID | — | UUID, opcional, referencia a `gestion_cobranza`. | NINGUNA |
| `monto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `saldo` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `dias_vencido` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `vence_en` | DATE | — | DATE, obligatoria. | NINGUNA |
| `castigada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `datos_facturacion`

**Qué es.** El documento fiscal y su ciclo de vida.

**Para qué sirve.** Cobrar una comisión obliga a facturarla. El modelo cubre la facturación electrónica boliviana con sus piezas propias: NIT emisor, sucursal, punto de venta, número correlativo, CUF, CUFD, código de control, leyenda y QR de verificación. `estado_fiscal` incluye `EMITIDA_OFFLINE`, porque el servicio de impuestos se cae y la operación no puede detenerse: se emite fuera de línea y se envía después por lote. Una factura **no se edita ni se borra**: se anula y se emite nota de crédito. La nota de crédito se enlaza con la devolución que la origina, de modo que la devolución de plata y el documento fiscal quedan atados.

**Por qué existe.** Porque la alternativa —facturar a mano por fuera— es inviable al primer mes de operación y es una contingencia tributaria desde el primer día. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `factura_electronica`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `tipo_documento` | VARCHAR(5) | — | VARCHAR(5), obligatoria. | PERSONAL |
| `numero_documento` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `razon_social` | VARCHAR(150) | — | VARCHAR(150), obligatoria. | NINGUNA |
| `email_envio` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | PERSONAL |
| `es_predeterminado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `verificado` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `actualizado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `devengo_comision`

**Qué es.** El hecho económico: la plataforma ganó esta comisión, por esta operación, en esta fecha.

**Para qué sirve.** Es **el ingreso**, separado del cobro. La distinción importa: se puede haber devengado y no cobrado (el usuario no tenía saldo), o haber devengado y devuelto (la entrega se anuló). Contablemente son situaciones distintas y el estado las distingue: `DEVENGADO`, `COBRADO_PARCIAL`, `COBRADO`, `EXONERADO`, `DEVUELTO`, `INCOBRABLE`, `REVERSADO`. `periodo_contable` permite cerrar meses; `asiento_contable_id` conecta con la doble partida de M3, donde el ingreso impacta contra la cuenta configurada en el concepto.

**Por qué existe.** Porque mezclar devengo y cobro produce el error clásico: reconocer como ingreso lo que nunca se cobró, o perder de vista lo cobrado que había que devolver.

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `catalogo.tarifario`, `concepto_tarifa`, `cotizacion_comision`, `grupos.grupo`, `grupos.participante`, `identidad.usuario`, `nucleo_financiero.asiento_contable`. La referencian `aplicacion_promocion`, `calculo_impuesto`, `cargo_comision`, `cuenta_por_cobrar_comision`, `devolucion_comision`, `factura_electronica`. Reglas nombradas: `uq_devengo_idem`, `uq_devengo_hecho`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `concepto_tarifa_id` | UUID | — | UUID, obligatoria, referencia a `concepto_tarifa`. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `cotizacion_id` | UUID | — | UUID, opcional, referencia a `cotizacion_comision`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `usuario_obligado_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `monto_base` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_comision` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_descuento` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_impuesto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_total` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fecha_devengo` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `periodo_contable` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `clave_idempotencia` | VARCHAR(100) | Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4). | VARCHAR(100), obligatoria. | NINGUNA |

### `devolucion_comision`

**Qué es.** Los dos finales alternativos del devengo: se devuelve o queda por cobrar.

**Para qué sirve.** La devolución tipifica el motivo —entrega anulada, error de tarifa, reclamo procedente, falla de servicio— porque cada uno alimenta un indicador distinto: los errores de tarifa son deuda técnica, las fallas de servicio son riesgo operativo, y los reclamos procedentes son el indicador que mira el defensor del consumidor financiero. La cuenta por cobrar conecta con la gestión de cobranza (M8), reutilizando toda la maquinaria que ya existe para la mora de aportes. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `cumplimiento.reclamo_cliente`, `devengo_comision`, `identidad.usuario`, `nucleo_financiero.transaccion_billetera`. La referencian `cumplimiento.reclamo_cliente`, `nota_credito_debito`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `devengo_id` | UUID | — | UUID, obligatoria, referencia a `devengo_comision`. | NINGUNA |
| `transaccion_id` | UUID | — | UUID, opcional, referencia a `transaccion_billetera`. | NINGUNA |
| `reclamo_id` | UUID | — | UUID, opcional, referencia a `reclamo_cliente`. | NINGUNA |
| `autorizada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `motivo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `detalle` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `monto_devuelto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `forma` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `solicitada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `evento_significativo_sin`

**Qué es.** Un período en que la facturación electrónica no pudo enviarse al SIN (por ejemplo, porque el servicio no respondió), declarado como evento significativo.

**Para qué sirve.** Que las facturas emitidas sin conexión sigan siendo válidas: se registra el evento y después se envían en lote dentro del plazo que fija el SIN.

**Por qué existe.** Sin el evento, las facturas emitidas fuera de línea quedarían sin respaldo ante el SIN y la plataforma incumpliría su obligación tributaria.

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `factura_electronica`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `registrado_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `codigo_evento` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `sucursal` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `punto_venta` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `cufd_evento` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `fecha_inicio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `fecha_fin` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `cantidad_documentos_offline` | INTEGER | — | INTEGER, obligatoria. | PERSONAL |
| `plazo_registro` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrado_en_sin` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `codigo_recepcion_evento` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |

### `exencion_comision`

**Qué es.** No cobrar, con nombre y apellido.

**Para qué sirve.** Toda plataforma termina regalando comisiones: por campaña, por cortesía tras un error, por convenio institucional. La diferencia entre hacerlo bien y hacerlo mal es si queda registro de **quién lo autorizó, por qué motivo, con qué tope y hasta cuándo**. `campana_promocional` agrega presupuesto: una promoción con `presupuesto_maximo` se apaga sola cuando lo consume, en vez de descubrirse a fin de mes.

**Por qué existe.** Porque sin ellas la exención se implementa como un `monto = 0` a mano, y entonces es imposible distinguir "no correspondía cobrar" de "alguien no cobró". ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `concepto_tarifa`, `grupos.grupo`, `identidad.usuario`, `segmento_comercial`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `concepto_tarifa_id` | UUID | — | UUID, opcional, referencia a `concepto_tarifa`. | NINGUNA |
| `segmento_id` | UUID | — | UUID, opcional, referencia a `segmento_comercial`. | NINGUNA |
| `autorizada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `alcance` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `justificacion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `porcentaje_exencion` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `monto_tope` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | FINANCIERO |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `activa` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `factura_electronica`

**Qué es.** El documento fiscal y su ciclo de vida.

**Para qué sirve.** Cobrar una comisión obliga a facturarla. El modelo cubre la facturación electrónica boliviana con sus piezas propias: NIT emisor, sucursal, punto de venta, número correlativo, CUF, CUFD, código de control, leyenda y QR de verificación. `estado_fiscal` incluye `EMITIDA_OFFLINE`, porque el servicio de impuestos se cae y la operación no puede detenerse: se emite fuera de línea y se envía después por lote. Una factura **no se edita ni se borra**: se anula y se emite nota de crédito. La nota de crédito se enlaza con la devolución que la origina, de modo que la devolución de plata y el documento fiscal quedan atados.

**Por qué existe.** Porque la alternativa —facturar a mano por fuera— es inviable al primer mes de operación y es una contingencia tributaria desde el primer día. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `datos_facturacion`, `devengo_comision`, `evento_significativo_sin`, `identidad.usuario`, `lote_envio_sin`. La referencian `nota_credito_debito`, `publicidad.factura_publicidad`. Reglas nombradas: `uq_factura_cuf`, `uq_factura_correlativo`, `ck_factura_offline_evento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `devengo_id` | UUID | — | UUID, opcional, referencia a `devengo_comision`. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `datos_facturacion_id` | UUID | — | UUID, obligatoria, referencia a `datos_facturacion`. | NINGUNA |
| `lote_envio_sin_id` | UUID | — | UUID, opcional, referencia a `lote_envio_sin`. | NINGUNA |
| `evento_significativo_id` | UUID | — | UUID, opcional, referencia a `evento_significativo_sin`. | NINGUNA |
| `nit_emisor` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | PERSONAL |
| `sucursal` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `punto_venta` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `numero_factura` | BIGINT | — | BIGINT, obligatoria. | NINGUNA |
| `cuf` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `cufd` | VARCHAR(120) | — | VARCHAR(120), obligatoria. | NINGUNA |
| `codigo_control` | VARCHAR(20) | — | VARCHAR(20), opcional. | NINGUNA |
| `fecha_emision` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `monto_total` | DECIMAL(14,2) | — | DECIMAL(14,2), obligatoria. | FINANCIERO |
| `monto_iva` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `monto_no_sujeto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `estado_fiscal` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `url_pdf` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `url_xml` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `hash_documento` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `qr_verificacion` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |
| `leyenda` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |
| `anulada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_anulacion` | VARCHAR(200) | — | VARCHAR(200), opcional. | NINGUNA |

### `liquidacion_ingresos`

**Qué es.** El cierre mensual del ingreso: devengado, cobrado, exonerado, devuelto, incobrable, impuestos, costos de proveedores e ingreso neto.

**Para qué sirve.** Es el estado de resultados del producto, construido desde los devengos y no desde una planilla aparte. Y es el número que se compara contra la contabilidad: si `liquidacion_ingresos.total_cobrado` no coincide con el saldo de la cuenta de ingresos del mayor, hay un problema y se ve el mismo mes. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `nucleo_financiero.asiento_contable`. La referencian `costo_proveedor_operacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `periodo` | CHAR(7) | — | CHAR(7), obligatoria. | NINGUNA |
| `fecha_inicio` | DATE | — | DATE, obligatoria. | NINGUNA |
| `fecha_fin` | DATE | — | DATE, obligatoria. | NINGUNA |
| `total_devengado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_cobrado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_exonerado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_devuelto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_incobrable` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_impuestos` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `total_costo_proveedores` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `ingreso_neto` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria, calculada por la base, no se escribe. | NINGUNA |
| `cantidad_operaciones` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `asiento_contable_id` | UUID | — | UUID, opcional, referencia a `asiento_contable`. | NINGUNA |
| `cerrada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `lote_envio_sin`

**Qué es.** El documento fiscal y su ciclo de vida.

**Para qué sirve.** Cobrar una comisión obliga a facturarla. El modelo cubre la facturación electrónica boliviana con sus piezas propias: NIT emisor, sucursal, punto de venta, número correlativo, CUF, CUFD, código de control, leyenda y QR de verificación. `estado_fiscal` incluye `EMITIDA_OFFLINE`, porque el servicio de impuestos se cae y la operación no puede detenerse: se emite fuera de línea y se envía después por lote. Una factura **no se edita ni se borra**: se anula y se emite nota de crédito. La nota de crédito se enlaza con la devolución que la origina, de modo que la devolución de plata y el documento fiscal quedan atados.

**Por qué existe.** Porque la alternativa —facturar a mano por fuera— es inviable al primer mes de operación y es una contingencia tributaria desde el primer día. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `factura_electronica`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tipo_envio` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `cantidad_documentos` | INTEGER | — | INTEGER, obligatoria. | PERSONAL |
| `fecha_envio` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `codigo_recepcion` | VARCHAR(60) | — | VARCHAR(60), opcional. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `respuesta` | JSONB | — | JSONB, opcional. | NINGUNA |
| `reintentos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |

### `nota_credito_debito`

**Qué es.** El documento fiscal y su ciclo de vida.

**Para qué sirve.** Cobrar una comisión obliga a facturarla. El modelo cubre la facturación electrónica boliviana con sus piezas propias: NIT emisor, sucursal, punto de venta, número correlativo, CUF, CUFD, código de control, leyenda y QR de verificación. `estado_fiscal` incluye `EMITIDA_OFFLINE`, porque el servicio de impuestos se cae y la operación no puede detenerse: se emite fuera de línea y se envía después por lote. Una factura **no se edita ni se borra**: se anula y se emite nota de crédito. La nota de crédito se enlaza con la devolución que la origina, de modo que la devolución de plata y el documento fiscal quedan atados.

**Por qué existe.** Porque la alternativa —facturar a mano por fuera— es inviable al primer mes de operación y es una contingencia tributaria desde el primer día. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `devolucion_comision`, `factura_electronica`. Reglas nombradas: `uq_nota_cuf`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `factura_id` | UUID | — | UUID, obligatoria, referencia a `factura_electronica`. | NINGUNA |
| `devolucion_comision_id` | UUID | — | UUID, opcional, referencia a `devolucion_comision`. | NINGUNA |
| `tipo` | VARCHAR(8) | — | VARCHAR(8), obligatoria. | NINGUNA |
| `motivo` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `monto` | DECIMAL(12,2) | — | DECIMAL(12,2), obligatoria. | FINANCIERO |
| `cuf` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `fecha_emision` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `estado_fiscal` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |

### `politica_redondeo`

**Qué es.** Cómo se redondea el resultado del cálculo.

**Para qué sirve.** En Bolivia el circulante mínimo práctico es Bs 0,10. Un cálculo de 0,3 % sobre Bs 5.437 da Bs 16,311, y hay que decidir —una vez, de forma explícita y auditable— si eso es 16,30 o 16,40. Sin política, cada desarrollador redondea distinto y aparecen diferencias de centavos que rompen conciliaciones. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `concepto_tarifa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `moneda` | CHAR(3) | Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2). | CHAR(3), obligatoria. | NINGUNA |
| `unidad_minima` | DECIMAL(6,4) | — | DECIMAL(6,4), obligatoria. | NINGUNA |
| `modo` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `aplica_a` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |

### `regla_tarifa`

**Qué es.** Los escalones y condiciones de un concepto.

**Para qué sirve.** Permite precios que dependen del contexto sin multiplicar conceptos: tramos por monto, descuentos por tamaño de grupo, tarifa distinta por canal, por antigüedad del usuario o por nivel de verificación. La condición es `JSONB` declarativo —`{"tamanio_grupo": {">=": 20}, "canal": "APP"}`— evaluado por el motor.

**Por qué existe.** Porque la primera negociación comercial seria (un convenio con una cooperativa, una campaña regional) pide precio diferenciado, y sin reglas eso se resuelve con código especial que nadie vuelve a tocar.

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `concepto_tarifa`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `concepto_tarifa_id` | UUID | — | UUID, obligatoria, referencia a `concepto_tarifa`. | NINGUNA |
| `orden` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `condicion` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `monto_base_desde` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `monto_base_hasta` | DECIMAL(14,2) | — | DECIMAL(14,2), opcional. | FINANCIERO |
| `valor_porcentual` | DECIMAL(7,4) | — | DECIMAL(7,4), opcional. | NINGUNA |
| `valor_fijo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | NINGUNA |
| `monto_minimo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | FINANCIERO |
| `monto_maximo` | DECIMAL(12,2) | — | DECIMAL(12,2), opcional. | FINANCIERO |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |

### `segmento_comercial`

**Qué es.** A quién se le aplica cada tarifario y con qué prioridad.

**Para qué sirve.** Resuelven la pregunta "¿qué precio le corresponde a esta operación?" con una jerarquía explícita: usuario > grupo > segmento > global. La asignación puede tener vigencia y motivo, de modo que un precio especial siempre tiene fecha de inicio, fecha de fin y una razón escrita.

**Por qué existe.** Porque los precios especiales existen igual: si el modelo no los soporta, aparecen como excepciones a mano en producción, que es la peor forma posible de tenerlos. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `asignacion_tarifario`, `exencion_comision`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `criterio` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `prioridad` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `activo` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `simulacion_tarifa`

**Qué es.** El ensayo previo y el expediente del cambio.

**Para qué sirve.** La simulación permite correr el tarifario nuevo sobre la historia real antes de publicarlo: cuánto más se habría cobrado, a cuánta gente le habría subido y en cuánto. Eso convierte una decisión de precios en una decisión informada y deja constancia de que se evaluó el impacto. `cambio_tarifario` guarda el cumplimiento del deber de aviso: tipo de cambio, días de preaviso, fecha y canal del aviso, cuántos usuarios fueron notificados y si el cambio habilita rescindir sin costo. Un aumento de comisiones sin aviso previo documentado es una observación segura. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `identidad.usuario`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `ejecutada_por` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `escenario` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `resultado` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `delta_ingreso_estimado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | NINGUNA |
| `usuarios_impactados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `ejecutada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `tarifa_congelada_grupo`

**Qué es.** El snapshot del tarifario que aceptó un grupo al constituirse.

**Para qué sirve.** **El precio no cambia a mitad del juego.** Un pasanaku de doce meses se acuerda al inicio; si la comisión sube en el mes cinco, los que ya estaban adentro siguen con el precio pactado. Un aumento aplica a los grupos nuevos. `hash_snapshot` permite demostrar que el snapshot no fue alterado, y `acuerdo_id` lo enlaza con el [[acuerdo]] (M2) por el cual los participantes lo aceptaron.

**Por qué existe.** Porque sin él, subir el tarifario reescribe retroactivamente el costo de ciclos ya aceptados. Eso es, a la vez, un problema de confianza, un reclamo con razón y un incumplimiento contractual. ---

**Sistema.** Vive en el esquema `tarifas`; la escribe y la lee solo el servicio `tarifas` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `catalogo.tarifario`, `grupos.acuerdo`, `grupos.grupo`. Reglas nombradas: `uq_tarifa_congelada_grupo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `tarifario_id` | UUID | — | UUID, obligatoria, referencia a `tarifario`. | NINGUNA |
| `acuerdo_id` | UUID | — | UUID, opcional, referencia a `acuerdo`. | NINGUNA |
| `snapshot_conceptos` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `hash_snapshot` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `congelada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta_ciclo_nro` | SMALLINT | — | SMALLINT, opcional. | NINGUNA |

## Esquema `transparencia`

### `alerta_riesgo`

**Qué es.** Una señal automática de que algo anda mal con un usuario, un grupo o un organizador.

**Para qué sirve.** Los cuatro códigos son patrones aprendidos: - `CAIDA_ABRUPTA_SCORE`: alguien que era confiable empezó a fallar. Puede ser un problema personal (y ahí conviene ofrecer un plan de pago antes de sancionar) o puede ser una cuenta comprometida. - `MORA_CONCENTRADA`: varios morosos en el mismo grupo. **Es el predictor más fuerte de colapso**: cuando la gente ve que otros no pagan, deja de pagar. - `RETIRO_MASIVO`: varias solicitudes de salida juntas. Algo pasó en ese grupo que el sistema no ve. - `GRUPO_INVIABLE`: los números ya no cierran. `escalarACumplimiento()` conecta con M9 cuando el patrón huele a fraude y no a dificultad económica.

**Por qué existe.** Sin alertas, los problemas se descubren cuando alguien reclama —es decir, cuando ya hay daño. Con alertas, hay una ventana para intervenir: ofrecer un plan, activar un plan de contingencia, reforzar la cobranza. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Reglas nombradas: `ck_alerta_riesgo_cierre`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `ambito` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `ambito_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `severidad` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `evidencia` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `estado` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `detectada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `cerrada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `bloque_transparencia`

**Qué es.** Un bloque sellado de la historia del grupo, encadenado por hash al anterior y con raíz Merkle de los eventos que contiene.

**Para qué sirve.** Resuelve la pregunta que ninguna base de datos común puede responder: ***¿cómo sé que no editaron un pago viejo después de que pasó?*** La respuesta es criptográfica: cada bloque incluye el hash del anterior. Alterar un registro antiguo cambia el hash de su bloque, lo que invalida el siguiente, y el siguiente, hasta el último. **Manipular la historia se nota siempre.** La raíz Merkle permite algo más fino: `pruebaDeInclusion(eventoId)` le da a un participante la prueba de que **su pago concreto** estaba en el libro antes de que se cerrara el ciclo, sin necesidad de revelar los pagos de los demás. `selloExterno` (anclaje a un servicio de timestamping externo) es la vuelta de tuerca: prueba que el bloque existía en una fecha determinada, incluso contra la sospecha de que la propia plataforma haya regenerado toda la cadena.

**Por qué existe.** Sin sello, la transparencia es una promesa: "confiá en que no editamos nada". Con sello, es verificable por cualquiera, incluso contra la plataforma misma. **Esa diferencia es la propuesta de valor del módulo.**

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`. La referencian `registro_sellado`. Reglas nombradas: `uq_bloque_grupo_numero`, `ck_bloque_genesis`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `numero_bloque` | BIGINT | — | BIGINT, obligatoria. | NINGUNA |
| `hash_bloque_anterior` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `raiz_merkle` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | NINGUNA |
| `hash_bloque` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `cantidad_eventos` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `periodo_cubierto_desde` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `periodo_cubierto_hasta` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `sellado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `sello_externo` | VARCHAR(255) | — | VARCHAR(255), opcional. | NINGUNA |

### `certificado_reputacion`

**Qué es.** Un documento firmado digitalmente que acredita el historial del usuario, verificable públicamente.

**Para qué sirve.** Es **la reputación portable hecha producto** (RF-18). El usuario puede presentar su certificado a un grupo fuera de la plataforma, a un prestamista, a una cooperativa, a alguien que le va a fiar mercadería. Para mucha gente sin historial bancario formal, **este certificado puede ser el primer documento que acredite que cumple sus compromisos financieros**. Ese es un impacto real, no una función más. `codigoVerificacion` + `firmaDigital` + `urlPublica` permiten que un tercero —sin cuenta en la plataforma— verifique que el certificado es auténtico y no está revocado. `expiraEn` es necesario: un certificado de hace dos años no dice nada del presente.

**Por qué existe.** Sin certificado, la reputación solo sirve dentro de la plataforma. Con certificado, sale al mundo y se convierte en un activo de la persona. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `snapshot_reputacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `snapshot_id` | UUID | — | UUID, obligatoria, referencia a `snapshot_reputacion`. | NINGUNA |
| `codigo_verificacion` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `hash_contenido` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `firma_digital` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `url_publica` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |
| `emitido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `expira_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revocado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

### `componente_score`

**Qué es.** El desglose del puntaje por factor: valor crudo, normalizado, contribución y tendencia.

**Para qué sirve.** Es lo que hace **explicable** el score. Permite mostrar: "puntualidad: 340 puntos (subiendo); permanencia: 210 puntos (estable); incumplimientos: -80 puntos (mejorando)". Sin el desglose, el usuario ve un número que se mueve sin entender por qué, y eso genera desconfianza en el sistema mismo. `tendencia` es lo que convierte el score en algo accionable: saber que vas subiendo motiva; saber solo que tenés 640 no.

**Por qué existe.** Un score opaco es un score que no se puede defender ante un reclamo, y que no motiva ningún cambio de comportamiento. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `puntaje_reputacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `puntaje_id` | UUID | — | UUID, obligatoria, referencia a `puntaje_reputacion`. | NINGUNA |
| `codigo_factor` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `valor_crudo` | DECIMAL(12,4) | — | DECIMAL(12,4), obligatoria. | NINGUNA |
| `valor_normalizado` | DECIMAL(5,4) | — | DECIMAL(5,4), obligatoria. | NINGUNA |
| `contribucion` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `tendencia` | VARCHAR(10) | — | VARCHAR(10), obligatoria. | NINGUNA |

### `evento_reputacion`

**Qué es.** Un hecho reputacional inmutable: aportó puntual, abandonó el grupo, consumió cobertura, completó un ciclo, recibió una sanción.

**Para qué sirve.** **Es la fuente de la verdad de toda la reputación.** El puntaje no es un dato: es una función de estos eventos. Que sea append-only tiene una consecuencia práctica enorme: **corregir un error no es editar, es registrar el evento inverso** (`revertidoPorId`). Si se marcó incorrectamente un aporte como impago y después se descubre que sí había pagado, no se borra el evento negativo: se registra el positivo que lo revierte. El historial queda completo y explica el puntaje actual, incluido el error y su corrección. Los 19 tipos cubren todo el ciclo: desde `APORTE_ANTICIPADO` (que **suma**, porque pagar antes merece reconocimiento) hasta `FRAUDE_CONFIRMADO`. Vale la pena notar `SANCION_REVOCADA` y `DEUDA_RECUPERADA`: **el sistema modela explícitamente la redención.** Quien se atrasó y después pagó todo tiene un evento positivo que lo refleja. Un modelo que solo registra lo malo condena a la gente y no le da razones para regularizarse.

**Por qué existe.** Sin eventos, el puntaje es un número que alguien escribió y que no se puede explicar, auditar ni recalcular con un modelo nuevo.

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `evento_reputacion`, `grupos.grupo`, `grupos.participante`, `identidad.usuario`. La referencian `evento_reputacion`. Reglas nombradas: `uq_evento_reputacion_hecho`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `grupo_id` | UUID | — | UUID, opcional, referencia a `grupo`. | NINGUNA |
| `participante_id` | UUID | — | UUID, opcional, referencia a `participante`. | NINGUNA |
| `tipo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `referencia_tipo` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | NINGUNA |
| `referencia_origen_id` | UUID | — | UUID, opcional. | NINGUNA |
| `impacto` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `factor_afectado` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `modelo_version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `es_reversible` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `revertido_por_id` | UUID | — | UUID, opcional, referencia a `evento_reputacion`. | NINGUNA |
| `ocurrido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `registrado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `insignia_logro`

**Qué es.** Reconocimientos concretos: `PUNTUAL_12_MESES`, `PRIMER_GRUPO`, `SIN_MORA`, `ORGANIZADOR_5_ESTRELLAS`.

**Para qué sirve.** Un puntaje numérico es abstracto y se mueve poco a poco; una insignia es concreta, se muestra y se persigue. Cumplen dos funciones: **motivación** (una meta alcanzable frente a un score que sube lentamente) y **señalización social** (en la lista de un grupo, "12 meses puntual" comunica más que "score 780"). Separar el catálogo (`InsigniaLogro`) del otorgamiento (`InsigniaOtorgada`) permite agregar insignias nuevas y evaluarlas retroactivamente. `revocadaEn` importa: si una insignia se otorgó por un logro que después se demostró falso —o si el usuario incurre en algo grave—, se revoca con motivo, no se borra.

**Por qué existe.** Sin gamificación, el único incentivo a pagar puntual es evitar el castigo. Con insignias hay también un incentivo positivo, que es más sostenible. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `insignia_otorgada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `nombre` | VARCHAR(80) | — | VARCHAR(80), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `criterio` | VARCHAR(300) | — | VARCHAR(300), obligatoria. | NINGUNA |
| `icono_url` | VARCHAR(255) | — | VARCHAR(255), obligatoria. | NINGUNA |

### `insignia_otorgada`

**Qué es.** Reconocimientos concretos: `PUNTUAL_12_MESES`, `PRIMER_GRUPO`, `SIN_MORA`, `ORGANIZADOR_5_ESTRELLAS`.

**Para qué sirve.** Un puntaje numérico es abstracto y se mueve poco a poco; una insignia es concreta, se muestra y se persigue. Cumplen dos funciones: **motivación** (una meta alcanzable frente a un score que sube lentamente) y **señalización social** (en la lista de un grupo, "12 meses puntual" comunica más que "score 780"). Separar el catálogo (`InsigniaLogro`) del otorgamiento (`InsigniaOtorgada`) permite agregar insignias nuevas y evaluarlas retroactivamente. `revocadaEn` importa: si una insignia se otorgó por un logro que después se demostró falso —o si el usuario incurre en algo grave—, se revoca con motivo, no se borra.

**Por qué existe.** Sin gamificación, el único incentivo a pagar puntual es evitar el castigo. Con insignias hay también un incentivo positivo, que es más sostenible. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `insignia_logro`. Reglas nombradas: `uq_insignia_usuario`, `ck_insignia_revocacion_motivada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `insignia_id` | UUID | — | UUID, obligatoria, referencia a `insignia_logro`. | NINGUNA |
| `otorgada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `revocada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `motivo_revocacion` | VARCHAR(160) | — | VARCHAR(160), opcional. | NINGUNA |

### `metrica_grupo`

**Qué es.** Indicadores calculados del grupo: tasa de puntualidad, días de mora promedio, cobertura consumida, rotación.

**Para qué sirve.** Es el **tablero de salud del grupo**, y sobre todo el sistema de alerta temprana. `umbralAlerta` + `enAlerta` permiten detectar que un grupo se está deteriorando antes de que colapse: la mora se concentra, el fondo se consume rápido, la gente empieza a salirse. Detectarlo a tiempo permite intervenir (activar un plan de contingencia, M8) en lugar de reaccionar cuando el grupo ya es inviable y hay que liquidarlo con pérdidas para todos.

**Por qué existe.** Sin métricas persistidas, cada consulta recalcularía sobre toda la historia del grupo, y no habría serie temporal para ver la tendencia — que es justamente lo que anticipa el colapso.

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `grupos.periodo`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `periodo_id` | UUID | — | UUID, opcional, referencia a `periodo`. | NINGUNA |
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `valor` | DECIMAL(12,4) | — | DECIMAL(12,4), obligatoria. | NINGUNA |
| `unidad` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `umbral_alerta` | DECIMAL(12,4) | — | DECIMAL(12,4), opcional. | NINGUNA |
| `en_alerta` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |
| `calculada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `modelo_scoring`

**Qué es.** La versión del algoritmo que convierte eventos en un puntaje: puntaje base, mínimo, máximo, decaimiento y ventana histórica.

**Para qué sirve.** Tres cosas que sin esta entidad son imposibles: 1. **Responder "¿por qué me bajó el score?".** Guardando `modeloVersion` en cada puntaje, se puede recalcular exactamente con el modelo que estaba vigente ese día. Sin versión, cualquier ajuste del algoritmo reescribe retroactivamente la historia de todos y nadie puede auditar nada. 2. **Probar un modelo nuevo en sombra.** `esProduccion = false` permite calcular en paralelo y comparar antes de aplicarlo a decisiones reales. Cambiar el scoring de golpe puede dejar afuera a miles de usuarios que ayer eran elegibles. 3. **`factorDecaimientoMensual`: los errores viejos pesan menos.** Es una decisión de negocio deliberada, no un detalle técnico. Alguien que se atrasó dos veces hace tres años y desde entonces es puntual no debería cargar eso para siempre. Un sistema sin decaimiento condena a la gente y elimina el incentivo a mejorar. `minEventosParaScore` evita el problema del usuario nuevo: con dos eventos no se puede afirmar nada, y mostrar un puntaje "alto" basado en nada es engañoso para quien lo lee.

**Por qué existe.** Sin modelo versionado, el puntaje es un número mágico que nadie puede explicar ni defender, y el primer reclamo serio lo tumba. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. La referencian `peso_factor`, `puntaje_reputacion`, `regla_impacto_evento`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(200) | — | VARCHAR(200), obligatoria. | NINGUNA |
| `puntaje_base` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `puntaje_minimo` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `puntaje_maximo` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `factor_decaimiento_mensual` | DECIMAL(5,4) | — | DECIMAL(5,4), obligatoria. | NINGUNA |
| `ventana_historica_meses` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `min_eventos_para_score` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `es_produccion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `peso_factor`

**Qué es.** Cuánto pesa cada dimensión dentro del modelo: puntualidad, permanencia, severidad de incumplimientos, volumen, antigüedad, comportamiento social.

**Para qué sirve.** Permite calibrar sin código. Si la operación descubre que la antigüedad predice mucho menos que la puntualidad, se ajustan los pesos. `topeAporteAlScore` evita que un solo factor domine: alguien con veinte grupos completados pero que abandona el vigésimo primero no debería seguir apareciendo como excelente solo por volumen. `esPenalizador` distingue los factores que suman de los que restan, lo que importa para explicarle al usuario qué le conviene hacer.

**Por qué existe.** Con pesos hardcodeados no se puede ajustar el modelo con la evidencia que la operación va acumulando, ni auditar con qué ponderación se calculó un score histórico. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `modelo_scoring`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `modelo_id` | UUID | — | UUID, obligatoria, referencia a `modelo_scoring`. | NINGUNA |
| `codigo_factor` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `descripcion` | VARCHAR(160) | — | VARCHAR(160), obligatoria. | NINGUNA |
| `peso` | DECIMAL(5,4) | — | DECIMAL(5,4), obligatoria. | NINGUNA |
| `tope_aporte_al_score` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `es_penalizador` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `puntaje_reputacion`

**Qué es.** El puntaje vigente del usuario, con las métricas que lo componen.

**Para qué sirve.** Es la **proyección materializada** de los eventos. Existe por rendimiento: mostrar el score en un perfil no puede implicar recorrer tres años de eventos. Pero no es solo un número. Los campos que lo acompañan son los que la gente realmente quiere ver antes de meterse a un grupo con alguien: `indicePuntualidad`, `gruposCompletados`, `gruposAbandonados`, `incumplimientosAbiertos`, `antiguedadMeses`. **"820 puntos" no dice nada; "completó 6 grupos, abandonó 0, 97% de puntualidad" sí.** `nivelConfianza` traduce el número a lenguaje humano (`SIN_HISTORIAL`, `EN_OBSERVACION`, `BASICO`, `CONFIABLE`, `MUY_CONFIABLE`, `REFERENTE`, `RESTRINGIDO`). Es lo que se muestra en la UI, porque un puntaje numérico invita a comparaciones odiosas y a la ansiedad de ver bajar dos puntos. `esElegiblePara(grupo)` es la función que conecta con M2: un grupo con `reputacionMinima = 600` filtra automáticamente.

**Por qué existe.** Sin proyección, cada consulta de reputación recalcularía desde los eventos. Con proyección pero sin eventos, el número no se puede explicar. Hacen falta las dos.

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`, `modelo_scoring`. La referencian `componente_score`. Reglas nombradas: `ex_puntaje_vigente`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `modelo_id` | UUID | — | UUID, obligatoria, referencia a `modelo_scoring`. | NINGUNA |
| `puntaje` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `nivel_confianza` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `indice_puntualidad` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `tasa_incumplimiento` | DECIMAL(5,2) | — | DECIMAL(5,2), obligatoria. | NINGUNA |
| `monto_total_aportado` | DECIMAL(16,2) | — | DECIMAL(16,2), obligatoria. | FINANCIERO |
| `grupos_completados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `grupos_abandonados` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `incumplimientos_abiertos` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `antiguedad_meses` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `eventos_considerados` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `modelo_version` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `vigente_desde` | TIMESTAMPTZ | Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan. | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `vigente_hasta` | TIMESTAMPTZ | Hasta cuándo rigió esta versión; vacía mientras sigue vigente. | TIMESTAMPTZ, opcional. | NINGUNA |
| `calculado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |
| `proximo_recalculo_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `registro_sellado`

**Qué es.** Cada hecho incluido en un bloque: un pago, una entrega, una cobertura, un acuerdo, una sanción — con su hash y un resumen público.

**Para qué sirve.** `resumenPublico` es el campo clave y la decisión más delicada: contiene lo suficiente para verificar **sin exponer datos personales**. Se sella que "el cupo 7 aportó Bs 500 el 3 de marzo", no el nombre, el documento ni la cuenta bancaria de nadie. Esto permite que la verificación sea genuinamente pública sin convertir el libro en una filtración de datos.

**Por qué existe.** El bloque sella un conjunto; el registro es cada elemento del conjunto y lo que hace posible la prueba de inclusión individual. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Es append-only: UPDATE y DELETE revocados al rol del servicio; se corrige con una fila nueva. Depende de `bloque_transparencia`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `bloque_id` | UUID | — | UUID, obligatoria, referencia a `bloque_transparencia`. | NINGUNA |
| `tipo_entidad` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `entidad_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `hash_contenido` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `resumen_publico` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `ocurrido_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `regla_impacto_evento`

**Qué es.** Cuánto suma o resta cada tipo de evento, y sobre qué factor.

**Para qué sirve.** Traduce hechos a puntos. Aquí es donde se decide, explícitamente, que un aporte puntual suma poco y un abandono de grupo resta mucho. `multiplicadorPorReincidencia` es la pieza de justicia: **la segunda vez pesa más que la primera.** Atrasarse una vez le pasa a cualquiera; atrasarse la cuarta vez es un patrón. Sin multiplicador, cuatro atrasos leves pesan lo mismo que cuatro atrasos independientes de cuatro personas distintas, lo que no refleja el riesgo real. `requiereConfirmacion` protege contra el impacto prematuro: un fraude *sospechado* no puede destruir la reputación de alguien antes de estar confirmado.

**Por qué existe.** Sin tabla de impactos, la fórmula está enterrada en el código y cambiarla es un release sin trazabilidad de qué cambió y cuándo. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `modelo_scoring`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `modelo_id` | UUID | — | UUID, obligatoria, referencia a `modelo_scoring`. | NINGUNA |
| `tipo_evento` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `codigo_factor` | VARCHAR(40) | — | VARCHAR(40), obligatoria. | NINGUNA |
| `impacto_base` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `multiplicador_por_reincidencia` | DECIMAL(4,2) | — | DECIMAL(4,2), obligatoria. | NINGUNA |
| `impacto_maximo` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `requiere_confirmacion` | BOOLEAN | — | BOOLEAN, obligatoria. | NINGUNA |

### `resena_participante`

**Qué es.** La calificación de 1 a 5 que un participante le da a otro, por dimensión (puntualidad, comunicación, organización).

**Para qué sirve.** Captura lo que los datos duros no ven. Alguien puede pagar siempre a tiempo y ser insoportable de tratar; alguien puede atrasarse un día pero avisar siempre y cumplir lo que promete. Esa información existe solo en la experiencia de los otros participantes. Es especialmente relevante para evaluar organizadores: `ORGANIZADOR_5_ESTRELLAS` y `satisfaccionParticipantes` (M7) salen de acá. `estadoModeracion` es obligatorio, no opcional: **un sistema de reseñas sin moderación se convierte en un canal de acoso**, sobre todo en un contexto donde hay conflictos por plata. `moderadaPor` deja constancia de quién decidió publicar u ocultar. `dimension` evita el "le pongo 1 estrella porque me cae mal": obliga a calificar algo concreto.

**Por qué existe.** Sin reseñas, la reputación solo mide comportamiento de pago, y se pierde la dimensión de trato y comunicación que en un grupo colectivo pesa mucho.

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `grupos.grupo`, `grupos.participante`, `identidad.usuario`. Reglas nombradas: `uq_resena_autor_evaluado`, `ck_resena_moderada`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `grupo_id` | UUID | — | UUID, obligatoria, referencia a `grupo`. | NINGUNA |
| `autor_participante_id` | UUID | — | UUID, obligatoria, referencia a `participante`. | NINGUNA |
| `evaluado_usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `calificacion` | SMALLINT | — | SMALLINT, obligatoria. | NINGUNA |
| `comentario` | VARCHAR(500) | — | VARCHAR(500), opcional. | NINGUNA |
| `dimension` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `estado_moderacion` | VARCHAR(15) | — | VARCHAR(15), obligatoria. | NINGUNA |
| `moderada_por` | UUID | — | UUID, opcional, referencia a `usuario`. | NINGUNA |
| `creada_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `snapshot_reputacion`

**Qué es.** Una fotografía del puntaje en un momento concreto, con el detalle de sus factores.

**Para qué sirve.** Congela el estado en los momentos que importan: - `INGRESO_A_GRUPO`: con qué reputación entró. **Es lo que impide juzgarlo después con información que no existía cuando se lo aceptó.** - `CIERRE_DE_GRUPO`: con qué terminó, para medir el efecto del grupo. - `AUDITORIA`: estado en la fecha de una revisión. - `PERIODICO`: serie histórica para ver la evolución.

**Por qué existe.** El puntaje vigente cambia; para comparar "antes y después" o para defender una decisión pasada hace falta la foto de ese momento. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos. Depende de `identidad.usuario`. La referencian `certificado_reputacion`.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `id` | UUID | Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios. | UUID, obligatoria, clave primaria. | NINGUNA |
| `usuario_id` | UUID | — | UUID, obligatoria, referencia a `usuario`. | NINGUNA |
| `puntaje` | DECIMAL(6,2) | — | DECIMAL(6,2), obligatoria. | NINGUNA |
| `nivel_confianza` | VARCHAR(20) | — | VARCHAR(20), obligatoria. | NINGUNA |
| `fotografia_factores` | JSONB | — | JSONB, obligatoria. | NINGUNA |
| `motivo` | VARCHAR(25) | — | VARCHAR(25), obligatoria. | NINGUNA |
| `tomado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, obligatoria. | NINGUNA |

### `verificacion_publica`

**Qué es.** El endpoint de verificación de un código: constancia de pago, certificado de reputación o estado de grupo.

**Para qué sirve.** Es la cara pública de todo el módulo. Alguien que recibe una constancia de pago o un certificado de reputación —**sin tener cuenta en la plataforma**— entra el código y comprueba que es auténtico. Ese detalle es lo que hace que los documentos de la plataforma valgan afuera. Un PDF sin verificación es una imagen que se edita en dos minutos. `consultas` y `ultimaConsultaEn` sirven además como señal: un certificado consultado muchas veces desde muchos lugares puede indicar que está circulando de forma inesperada.

**Por qué existe.** Sin verificación pública, la transparencia solo existe para quien ya está adentro y ya confía. ---

**Sistema.** Vive en el esquema `transparencia`; la escribe y la lee solo el servicio `transparencia` (regla 98.1.1): otro servicio la conoce por su API o por sus eventos.

| Columna | Tipo | Negocio | Sistema | Sensibilidad |
|---|---|---|---|---|
| `codigo` | VARCHAR(40) | — | VARCHAR(40), obligatoria, clave primaria. | NINGUNA |
| `tipo_documento` | VARCHAR(30) | — | VARCHAR(30), obligatoria. | PERSONAL |
| `referencia_id` | UUID | — | UUID, obligatoria. | NINGUNA |
| `hash_esperado` | VARCHAR(64) | — | VARCHAR(64), obligatoria. | DERIVADO |
| `hash_recomputado` | VARCHAR(64) | — | VARCHAR(64), opcional. | DERIVADO |
| `resultado` | VARCHAR(15) | — | VARCHAR(15), opcional. | NINGUNA |
| `verificado_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |
| `consultas` | INTEGER | — | INTEGER, obligatoria. | NINGUNA |
| `ultima_consulta_en` | TIMESTAMPTZ | — | TIMESTAMPTZ, opcional. | NINGUNA |

