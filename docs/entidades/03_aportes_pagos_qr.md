# Módulo 3 — Aportes, Pagos QR, Conciliación y Contabilidad

> **Pregunta de negocio que responde este módulo:**
> *¿Cuánto debe cada uno, cómo se lo cobro sin fricción, y cómo sé con certeza —no
> por lo que la persona diga— que la plata efectivamente entró?*

Este es el módulo donde el sistema deja de ser una app de listas y pasa a ser una
plataforma financiera. La regla que lo gobierna es una sola:

> **Un aporte pasa a `PAGADO` únicamente cuando existe un pago conciliado contra
> el banco. Nunca por declaración del usuario, y nunca por criterio del
> organizador.**

Esa regla es la que rompe con el pasanaku de cuaderno, donde "ya te pagué" contra
"a mí no me llegó" no tiene árbitro. Y es la que obliga a que el dinero no se
represente con un solo campo `monto_pagado`, sino con una cadena de seis pasos,
cada uno idempotente y auditable por separado:

```
ObligacionAporte → OrdenCobro/QR → IntentoPago → Pago → Conciliacion → AsientoContable
```

---

## Paquete: Obligación de Aporte

### `ObligacionAporte` / `obligacion_aporte` — Raíz de agregado

**Qué es.** Lo que un cupo debe, en un período, por un concepto. Es la deuda
formal que la plataforma le reconoce a un participante.

**Para qué sirve (negocio).** Es **el eje de todo el sistema financiero**. La
cubre el fondo de garantía (M8), la deduce la entrega (M4), la puntúa la
reputación (M6) y la persigue la cobranza (M8). Si hubiera que elegir una sola
tabla como corazón del modelo, es esta.

Los cinco campos de monto no son redundancia, son la historia económica completa
de esa obligación:

- `montoEsperado`: lo que se pactó. **Nunca cambia.**
- `montoPagado`: lo que efectivamente entró y se concilió.
- `montoRecargo`: la mora acumulada.
- `montoCondonado`: lo que el grupo decidió perdonar (por acuerdo, M2).
- `montoCubiertoGarantia`: lo que puso el fondo para que el grupo no se detenga
  (M8). **Ojo: cubierto no es pagado.** El participante sigue debiendo esa plata,
  pero ahora se la debe al fondo, no al grupo.

`saldoPendiente` es columna generada de los cinco anteriores: no se puede
desincronizar porque no se escribe a mano.

`tipo` distingue seis conceptos distintos que un solo "aporte" confundiría:
el aporte del período, el recargo por mora, el aporte al fondo de garantía, la
reposición de una cobertura consumida y el ajuste manual. Cada uno se cobra
distinto, se contabiliza en cuenta distinta y tiene distinto tratamiento si el
grupo se disuelve. Hay un sexto tipo, `COMISION_PLATAFORMA`, que solo se usa
cuando el tarifario define que la comisión del servicio se prorratea entre los
participantes en vez de deducirse de la entrega (M11). **Ninguno de los tipos
representa un ingreso del organizador: administrar sigue sin cobrarse (RN-18).**

`obligacionOrigenId` implementa una decisión de diseño importante: **un recargo
por mora es OTRA obligación**, que apunta a la original. Así el `montoEsperado`
del aporte base nunca se "ensucia". Si mañana se condona el recargo pero no el
capital, se condona una fila y la otra queda intacta. Si se mezclaran, esa
distinción sería imposible.

`estado` con trece valores parece excesivo hasta que se enumeran los casos reales:
programado (todavía no vence), pendiente, reportado por el usuario (dice que pagó,
falta verificar), en verificación, pagado parcial, pagado, vencido, en mora,
cubierto por garantía, exonerado, condonado, reprogramado, anulado. Cada uno
implica una acción distinta del sistema y un mensaje distinto al usuario.

**Por qué debe existir.** Sin obligación explícita, "quién debe cuánto" sería un
cálculo derivado del calendario, y no habría dónde registrar recargos,
condonaciones, coberturas ni exoneraciones.

**A nivel de sistema.**
`UNIQUE (periodo_id, cupo_id, tipo)` para `tipo = 'APORTE_PERIODICO'`: un cupo debe
exactamente un aporte por período. `fecha_vencimiento` indexada — el barrido
diario de vencimientos la recorre. `version` para bloqueo optimista: dos pagos
simultáneos sobre la misma obligación no pueden pisarse.

---

### `PoliticaMora` / `politica_mora` — Política configurable

**Qué es.** Las reglas de recargo por atraso: cuántos días de gracia, qué tipo de
recargo, cuánto, con qué tope, y a partir de cuántos días se considera mora grave
o incumplimiento.

**Para qué sirve (negocio).** El recargo por mora es un instrumento delicado: **si
es muy bajo no cambia el comportamiento; si es muy alto ahoga al que ya está
ahogado y garantiza que nunca pague.** Que sea configurable por grupo permite
calibrarlo: un pasanaku familiar puede tener cero recargo y solo presión social;
uno formal necesita que atrasarse cueste.

`topeRecargo` es una protección al deudor y a la plataforma: sin tope, un recargo
diario compuesto convierte una deuda de Bs 500 en una impagable en pocos meses, lo
que es a la vez injusto y contraproducente (nadie paga una deuda que no puede
alcanzar).

`diasParaMoraGrave` y `diasParaIncumplimiento` son los umbrales que disparan el
módulo 8: dejan de ser un juicio de valor y pasan a ser un parámetro explícito.

**Por qué debe existir.** Sin política parametrizable, la fórmula de mora está
hardcodeada y todos los grupos comparten la misma tolerancia — lo que hace
imposible atender segmentos distintos.

**A nivel de sistema.** `grupo_id` nullable: `NULL` significa política global por
defecto. `vigente_desde` versiona: un cambio de política no puede reescribir
retroactivamente los recargos ya aplicados.

---

### `PlanRegularizacion` / `plan_regularizacion`

**Qué es.** El acuerdo de pago en cuotas para alguien que se atrasó.

**Para qué sirve (negocio).** Es la salida para el participante que quiere pagar
pero no puede de golpe. Sin esta figura, el sistema solo ofrece dos caminos —pagar
todo o ser expulsado— y la experiencia de cobranza real dice que la mayoría de la
gente sí quiere pagar, solo que no puede en el plazo original.

`generarCuotas()` crea nuevas obligaciones de aporte que reemplazan a las
originales. Eso es importante: el plan no es una anotación al margen, se
materializa en obligaciones cobrables con sus propias fechas.

`registrarIncumplimiento()` cierra el círculo: **si incumplís el plan de pago, eso
también es un incumplimiento** (`INCUMPLIMIENTO_PLAN_REGULARIZACION` en M8), y
normalmente más grave que el original, porque ya se te dio una oportunidad.

**Por qué debe existir.** Es lo que convierte una cartera morosa en una cartera
recuperable. Sin plan, la única gestión posible es la presión, y la tasa de
recuperación cae.

---

## Paquete: Cobro — QR y órdenes

### `OrdenCobro` / `orden_cobro` — Raíz de agregado

**Qué es.** La instrucción concreta de cobro para una obligación: monto exacto,
proveedor, referencia única, vigencia.

**Para qué sirve (negocio).** Es la traducción de "debés Bs 500" a "acá está el
QR para pagar esos Bs 500". Separarla de la obligación resuelve varios problemas
prácticos:

- Una obligación puede tener **varias órdenes**: la primera expiró, se generó otra;
  o la primera era por el banco A y falló, se generó otra por el banco B.
- `referenciaUnica` es la **glosa conciliable**: el texto que viaja con la
  transferencia y que después permite cruzarla automáticamente con el extracto
  bancario. Sin ella, la conciliación es manual, que es exactamente lo que RF-15
  quiere eliminar.
- `permiteMontoAbierto` cubre el pago parcial: hay grupos que aceptan que alguien
  ponga la mitad ahora y la mitad después.

**Por qué debe existir.** Sin orden de cobro no hay forma de saber a qué
obligación corresponde una transferencia que llegó al banco. Ese cruce es
precisamente lo que automatiza el módulo.

**A nivel de sistema.** `clave_idempotencia` `UNIQUE`: dos toques en "Generar QR"
no generan dos órdenes con dos referencias distintas (lo que después produciría un
pago sin obligación identificable). `expira_en` indexada para la limpieza y la
regeneración automática.

---

### `QRCobro` / `qr_cobro`

**Qué es.** El código QR interoperable propiamente dicho: el payload EMVCo, la
imagen, el CRC, el banco emisor.

**Para qué sirve (negocio).** Es la interfaz de pago que el mercado boliviano
efectivamente usa. El QR interoperable permite que alguien pague desde la app de
**cualquier** banco, no solo del banco de la plataforma. Ese detalle define si el
producto es usable o no: obligar a los participantes a abrir cuenta en un banco
específico mataría la adopción.

`escaneos` y `registrarEscaneo()` permiten medir el embudo real: cuánta gente ve
el QR, cuánta lo escanea, cuánta termina pagando. Si hay muchos escaneos y pocos
pagos, hay un problema en la app del banco, no en el recordatorio.

`esReutilizable` distingue dos casos: el QR de un aporte concreto (uso único,
monto fijo) y el QR permanente del grupo (reutilizable, monto abierto).

**Por qué debe existir.** Separado de la orden porque el payload EMV tiene su
propio formato, su propio CRC validable y su propia representación gráfica, y
porque un mismo cobro puede necesitar QR en un canal y enlace en otro.

**A nivel de sistema.** `validarCRC()` antes de mostrarlo: un QR con CRC inválido
es un QR que la app del banco va a rechazar, y es mejor detectarlo antes de
enviárselo al usuario.

---

### `ProveedorPago` / `proveedor_pago`

**Qué es.** Cada pasarela, banco o billetera con la que la plataforma opera.

**Para qué sirve (negocio).** Tres cosas que importan comercialmente:

1. **Multi-proveedor con respaldo.** `prioridad` y `activo` permiten que si el QR
   del banco A falla, el `IntentoPago` reintente con el proveedor B. En un país
   donde los servicios bancarios se caen con cierta regularidad, esto es la
   diferencia entre "no pude pagar" y "pagué". Y el día de vencimiento es
   justamente cuando más carga hay.
2. **Costo real por transacción.** `comisionFija` y `comisionPorcentual` permiten
   calcular `costoDe(monto)` y decidir por dónde enrutar. En aportes chicos —que
   son la mayoría— la comisión fija puede ser un porcentaje enorme del aporte.
3. **Capacidades declaradas.** `soportaWebhook` y `soportaConsultaEstado` cambian
   la estrategia: con webhook la acreditación es en segundos; sin él hay que hacer
   polling, con la latencia y el costo que eso implica.

`referenciaCredenciales` es explícitamente **un puntero a la bóveda de secretos,
nunca la llave**. Las credenciales de una pasarela de pagos no viven en la base de
datos de la aplicación.

**Por qué debe existir.** Sin abstracción de proveedor, integrar un segundo banco
significa reescribir el flujo de cobro.

---

### `EnlacePagoRapido` / `enlace_pago_rapido`

**Qué es.** El enlace corto y firmado que lleva directo al pago de una obligación
concreta.

**Para qué sirve (negocio).** Es la mitad técnica del **"pago en un toque"**
(RF-16). El participante recibe por WhatsApp "Tu aporte vence mañana — [Pagar]",
toca, y llega a su QR sin login, sin buscar el grupo, sin recordar cuánto era.
Cada paso que se le quita a ese flujo se traduce directamente en tasa de pago
puntual.

`clicks` mide la conversión del recordatorio.

**Por qué debe existir.** Sin enlace firmado y de un solo uso, un enlace de pago
por WhatsApp expondría el identificador de la obligación y permitiría que
cualquiera viera —o pagara, o manipulara— la deuda de otro.

**A nivel de sistema.** `token_id` apunta a `TokenEnlaceFirmado` (M1): HMAC + uso
único + expiración. El enlace no expone datos del grupo.

---

## Paquete: Ejecución del Pago

### `IntentoPago` / `intento_pago`

**Qué es.** Cada vez que se trata de ejecutar el cobro de una orden, con su
resultado.

**Para qué sirve (negocio).** Registra los fracasos, no solo los éxitos. Cuando un
participante dice "intenté pagar tres veces y no me dejó", esta tabla lo confirma
o lo desmiente, con el código de error y el mensaje que devolvió el proveedor. Es
la diferencia entre creerle o no a alguien que pide que no le cobren mora.

Operativamente permite `reintentarConRespaldo()`: el intento 1 falló con el
proveedor A, el intento 2 va por el B, y **la trazabilidad del primero no se
pierde**.

**Por qué debe existir.** Sin intentos materializados, un pago fallido no deja
rastro y el soporte no puede diagnosticar nada. También es la base para medir la
tasa de éxito por proveedor, que es lo que justifica cambiar de pasarela.

**A nivel de sistema.** `clave_idempotencia` `UNIQUE` por intento.

---

### `Pago` / `pago` — Raíz de agregado

**Qué es.** Plata que efectivamente se movió. El hecho económico.

**Para qué sirve (negocio).** Es el registro que respalda que alguien cumplió.
Varios campos merecen explicación:

- `montoComisionProveedor` y `montoNetoAcreditado`: **lo que el participante paga
  no es lo que llega a la bolsa.** La pasarela se queda con su comisión. Si esa
  diferencia no se modela explícitamente, la bolsa nunca cuadra y nadie entiende
  por qué faltan Bs 3,50.
- `pagadorNombre` y `pagadorDocumento`: quién pagó realmente. Muchas veces paga el
  esposo, la madre o el jefe por el participante. Registrarlo es necesario para
  cumplimiento (detectar que una sola persona está pagando por seis participantes
  distintos, patrón de participantes ficticios) y para resolver reclamos.
- `esManual` + `registradoPor`: distingue el pago que entró solo por webhook del
  que un operador cargó a mano contra un comprobante. Los segundos son
  intrínsecamente más riesgosos y deben poder auditarse aparte.
- `canal = EFECTIVO_AL_ORGANIZADOR`: reconoce que en la realidad boliviana una
  parte del dinero se paga en efectivo. Negarlo no lo elimina, solo lo saca del
  sistema y lo vuelve invisible. Mejor registrarlo, marcarlo como manual y
  someterlo a doble revisión.

**Por qué debe existir.** Es el hecho económico. Sin él no hay contabilidad, ni
constancia, ni conciliación, ni reputación basada en cumplimiento real.

**A nivel de sistema.** Reglas duras:
`UNIQUE (proveedor, referencia_proveedor)` — el mismo cobro no entra dos veces.
`UNIQUE (clave_idempotencia)`.
Trigger: la suma de pagos acreditados por obligación no puede superar
`monto_esperado + monto_recargo` salvo que exista un reembolso compensatorio.

---

### `comprobante_manual` (tabla, sin clase equivalente)

**Qué es.** La foto del comprobante que sube el participante cuando pagó por fuera
del circuito automático.

**Para qué sirve (negocio).** Cubre el caso real más frecuente de fricción:
alguien transfirió desde su banco sin usar el QR, o pagó en efectivo, y ahora
reclama que le acrediten. `estadoRevision`, `revisadoPor` y **`segundaRevisionPor`**
implementan doble control: acreditar un pago que no existe es la forma más simple
de robar en esta plataforma, y por eso un solo par de ojos no alcanza para montos
relevantes.

`hashArchivo` detecta el mismo comprobante subido dos veces —para dos aportes
distintos—, que es el fraude más común y más fácil de intentar.

**Por qué debe existir.** Sin flujo de comprobante manual, la gente que paga por
fuera queda marcada como morosa injustamente; con flujo manual sin doble revisión
ni hash, el sistema se vuelve trivial de defraudar.

---

### `ConstanciaPago` / `constancia_pago`

**Qué es.** El comprobante que la plataforma emite, con código de verificación
público.

**Para qué sirve (negocio).** Es lo que el participante muestra cuando alguien
pone en duda que pagó. `codigoVerificacion` es público y verificable por
cualquiera —incluido alguien que no tiene cuenta— contra el módulo 6. Eso
convierte el comprobante en algo que no se puede falsificar con un editor de
imágenes.

**Por qué debe existir.** En el pasanaku de cuaderno, la prueba de pago es la
palabra del organizador. Acá es un documento verificable de forma independiente.
Es una de las razones por las que alguien elegiría la plataforma sobre el cuaderno.

---

### `Reembolso` / `reembolso`

**Qué es.** La devolución de un pago, total o parcial.

**Para qué sirve (negocio).** Cuatro motivos, todos reales: pago duplicado (la
persona pagó dos veces), error de monto, grupo cancelado antes de arrancar, o
resolución de disputa. Es la única manera legítima de sacar plata que ya entró.

El flujo `solicitado → aprobado → ejecutado`, con `solicitadoPor` y `aprobadoPor`
distintos, es control interno básico: **quien pide una devolución no puede ser
quien la aprueba.**

**Por qué debe existir.** Sin reembolso modelado, "devolver plata" se haría con un
ajuste manual sobre el saldo, sin rastro, sin aprobación y sin asiento contable.
Es el hueco por donde se va la plata en sistemas mal diseñados.

---

### `HistorialEstadoPago` / `historial_estado_pago`

**Qué es.** Cada cambio de estado de un pago, como una fila nueva: de qué estado
salió, a cuál llegó, por qué y cuándo.

**Para qué sirve (negocio).** El pago es el hecho económico y no se edita
(`R-BIL-30`). Cuando un aporte se compensa porque el libro no lo pudo aplicar, el
pago pasa a `REVERSADO` agregando una fila acá; la fila del pago queda tal cual se
escribió. El estado vigente es la última fila; sin filas, el de alta.

**Por qué debe existir.** Un `UPDATE` sobre el pago borra lo que el pago decía antes
y deja a la base sin forma de probarlo. Con el historial, desde cada estado se sale
una sola vez (`uq_historial_estado_pago_salida`): dos reversos simultáneos del mismo
pago no pueden pasar los dos.

---

### `HistorialEstadoWebhook` / `historial_estado_webhook`

**Qué es.** Cada cambio de estado de un aviso de la pasarela, como una fila nueva: de
`RECIBIDO` a `PROCESADO`, `DESCARTADO` o `FALLIDO`, con el pago que generó o el error,
y cuándo.

**Para qué sirve (negocio).** El aviso firmado de la pasarela es la prueba de que la
plata llegó y no se edita (`R-BIL-33`). Cerrar su procesamiento agrega una fila acá en
vez de pisar el aviso; el estado vigente es la última fila y, sin filas, `RECIBIDO`.

**Por qué debe existir.** Sin historial, el cierre del procesamiento era un `UPDATE`
sobre un hecho de dinero (R-AUD-01). Con él, desde cada estado se sale una sola vez
(`uq_historial_estado_webhook_salida`) y la tabla se parte por la fecha del aviso para
soltarse junto con él al vencer su retención.

---

### `WebhookClave` / `webhook_clave`

**Qué es.** La clave de idempotencia que emitió la pasarela para cada aviso, junto con
el proveedor que la emitió y el aviso al que corresponde.

**Para qué sirve (negocio).** Un mismo aviso puede llegar dos veces; la segunda vez no
puede acreditar otra vez (regla 91.1.4). La clave primaria `(proveedor, clave)` es la
que rechaza el duplicado, en todo el tiempo y no solo dentro de un mes (`R-BIL-33`).

**Por qué debe existir.** El aviso vive en una tabla particionada por mes, y PostgreSQL
no admite un índice único sobre ella sin la fecha: el mismo aviso reenviado en otro mes
pasaría como nuevo. Esta tabla chica, sin particionar, es la única fuente de esa
unicidad. No es un hecho de dinero: cuando vence la retención del aviso se borra con
él (`fn_aportes_purgar_webhooks`).

---

### `HistorialEstadoReembolso` / `historial_estado_reembolso`

**Qué es.** Cada cambio de estado de un reembolso: de qué estado a cuál, quién lo
ejecutó y cuándo.

**Para qué sirve (negocio).** La aprobación de un reembolso (`SOLICITADO` →
`EJECUTADO`) queda como una fila nueva con quien aprobó, en vez de pisar la
solicitud (`R-BIL-30`). Quien pidió la devolución y quien la aprobó quedan los dos
a la vista, cada uno en su fila.

**Por qué debe existir.** La devolución es la única salida legítima de plata que ya
entró. Si la aprobación se escribe con `UPDATE`, una segunda escritura puede cambiar
quién aprobó sin dejar rastro. Con el historial append-only y una sola salida por
estado (`uq_historial_estado_reembolso_salida`), dos aprobaciones simultáneas no
devuelven la plata dos veces.

---

### `DisputaPago` / `disputa_pago`

**Qué es.** El reclamo formal sobre un pago: contracargo, desconocimiento del
cargo, monto incorrecto.

**Para qué sirve (negocio).** Los contracargos son una realidad de cualquier
sistema con tarjetas, y tienen **plazo legal de respuesta**
(`fechaLimiteRespuesta`). Perder ese plazo significa perder la plata
automáticamente, sin importar quién tenga razón. Esta entidad existe en buena
medida para que ese reloj sea visible y accionable.

`evidencias` acumula lo que se va a presentar: la constancia, el registro de
escaneo del QR, la confirmación de recepción.

**Por qué debe existir.** Sin gestión de disputas, cada contracargo es una pérdida
silenciosa y una potencial descuadratura de la bolsa de un grupo.

---

## Paquete: Conciliación

> **Por qué la conciliación es un paquete y no un campo booleano.**
> Que la pasarela diga "aprobado" no significa que la plata esté en la cuenta.
> Entre una cosa y la otra hay horas, comisiones, rechazos posteriores y errores
> de referencia. La conciliación es el proceso de cruzar **lo que el sistema cree
> que pasó** con **lo que el banco dice que pasó**, y de gestionar las
> diferencias. Sin esto, el saldo de un grupo es una opinión.

### `ExtractoBancario` / `extracto_bancario`

**Qué es.** El estado de cuenta de un período, importado desde el banco.

**Para qué sirve (negocio).** Es la **fuente de verdad externa**. Todo lo que el
sistema afirma sobre el dinero se contrasta contra esto. `cuadrar()` verifica que
saldo inicial + movimientos = saldo final: si no cuadra, el archivo está
incompleto y conciliar contra él daría un falso "todo bien".

`importadoPor` deja constancia de quién cargó el extracto, porque un extracto
adulterado invalidaría toda la conciliación de ese día.

**Por qué debe existir.** Sin extracto no hay contra qué conciliar y la
plataforma solo puede confiar en lo que la pasarela le informa.

---

### `MovimientoBancario` / `movimiento_bancario`

**Qué es.** Cada línea del extracto: fecha, monto, glosa, referencia del banco.

**Para qué sirve (negocio).** Es lo que se cruza con los pagos.
`buscarCoincidencias()` intenta la conciliación automática por referencia exacta y,
si falla, por monto+fecha. La `glosa` va con índice full-text porque la
conciliación real, cuando la referencia se pierde, se hace buscando el nombre o el
número que el pagador escribió a mano.

**Por qué debe existir.** El caso que justifica esta tabla: **entró plata que
nadie identificó.** Alguien transfirió sin referencia. Sin `MovimientoBancario`
como entidad propia, ese dinero es invisible; con ella, queda como movimiento no
conciliado, visible, con `conciliado = FALSE`, esperando que alguien lo reclame.

**A nivel de sistema.** `UNIQUE (extracto_id, referencia_banco)`: reimportar el
mismo extracto no duplica movimientos.

---

### `Conciliacion` / `conciliacion`

**Qué es.** El cruce entre un pago del sistema y un movimiento del banco.

**Para qué sirve (negocio).** **Es la entidad que hace verdadero el "pagado".**
Mientras no exista una conciliación en estado `CONCILIADO_*`, el aporte no está
realmente saldado por más que la pasarela haya dicho "aprobado".

`metodo` guarda cómo se logró el cruce (referencia exacta, monto+fecha, manual).
Es un indicador de calidad operativa: si el 40% de la conciliación es manual, la
generación de referencias tiene un problema. `diferenciaMonto` captura los
centavos y las comisiones no previstas.

`forzarManual(usuarioId, justificacion)` existe porque siempre hay casos que el
algoritmo no resuelve — pero exige justificación y deja autor, porque forzar una
conciliación es afirmar que entró plata.

**Por qué debe existir.** Sin conciliación explícita, la única fuente de verdad es
la pasarela, y las pasarelas se equivocan, reintentan y reversan.

---

### `ExcepcionConciliacion` / `excepcion_conciliacion`

**Qué es.** Cada descuadre detectado, tipificado y asignado a alguien para
resolver.

**Para qué sirve (negocio).** Los siete tipos son el catálogo de todo lo que sale
mal con el dinero en la práctica: monto distinto, referencia ausente, pago
duplicado, pago sin obligación, obligación sin pago, fuera de plazo, moneda
distinta. Tipificarlos permite medir cuál predomina y atacar la causa.

`asignadaA` convierte el descuadre en trabajo de alguien, con dueño. Sin
asignación, las excepciones se acumulan y nadie las mira hasta que son cien.

**Por qué debe existir.** Y sobre todo: **toda excepción abierta bloquea el cierre
diario.** Esa es la regla que impide que los descuadres se arrastren. Sin la
entidad no hay qué bloquear.

---

### `WebhookPasarela` / `webhook_pasarela`

**Qué es.** Cada notificación que la pasarela envía al sistema, guardada en crudo
antes de procesarla.

**Para qué sirve (negocio).** Es el punto de entrada del dinero automático, y por
eso es también el punto de mayor riesgo. Dos protecciones críticas:

1. **`claveIdempotencia` es `UNIQUE`.** Las pasarelas reenvían el mismo evento —lo
   hacen sistemáticamente, por diseño, cuando no reciben confirmación a tiempo. Sin
   idempotencia, **un reintento del proveedor acredita el aporte dos veces**. Ese
   es el bug clásico que descuadra la bolsa y hace que alguien cobre de más.
2. **`firmaValida` se verifica ANTES de confiar en el payload.** Un webhook es un
   endpoint público: cualquiera puede enviarle un JSON que diga "pago aprobado por
   Bs 5.000". La firma es lo único que distingue a la pasarela real de un
   atacante.

`payloadCrudo` se guarda completo y sin interpretar. Cuando hay una disputa con el
proveedor, lo que vale es lo que él mandó, no lo que el sistema entendió.

**Por qué debe existir.** Sin persistir los webhooks, un fallo de procesamiento
pierde el evento para siempre y el pago nunca se acredita. Con la tabla, se
reintenta (`intentosProcesamiento`) o se manda a cola muerta para revisión manual.

---

## Paquete: Contabilidad de doble partida

> **Por qué doble partida en una app de pasanakus.**
> Porque el sistema custodia expectativas sobre dinero ajeno, y porque el panel de
> transparencia (M6) tiene que calcularse desde el mayor contable, no desde sumas
> ad-hoc esparcidas por el código. Con débito y crédito por cada evento, cualquier
> descuadre se detecta el mismo día. Sin doble partida, un descuadre se detecta
> cuando alguien reclama que le falta plata — meses después y sin forma de
> reconstruir qué pasó.

### `CuentaContable` / `cuenta_contable`

**Qué es.** Cada bolsillo del sistema: la caja del grupo, la cuenta corriente de
cada participante y el fondo de garantía.

**Para qué sirve (negocio).** Permite responder, en cualquier momento y de forma
verificable: *¿cuánta plata hay en el grupo? ¿cuánto de eso le corresponde a cada
uno? ¿cuánto es del fondo de garantía?*

La cuenta corriente por participante (`participanteId`) es la que sostiene el
estado de cuenta individual: cuánto aportó, cuánto cobró, cuál es su posición neta
en el grupo. Ese número es el que se necesita cuando alguien se retira o cuando el
grupo se disuelve anticipadamente (M8).

**Por qué debe existir.** Sin plan de cuentas, cada saldo se calcula con una
consulta distinta escrita por una persona distinta, y ninguna coincide con las
otras.

---

### `AsientoContable` / `asiento_contable`

**Qué es.** El registro de un hecho económico, compuesto por movimientos que
suman igual en el debe y en el haber.

**Para qué sirve (negocio).** Cada aporte acreditado, cada entrega y cada
cobertura del fondo genera un asiento. La regla `SUM(debe) =
SUM(haber)` **hace estructuralmente imposible** que la plata aparezca o desaparezca:
si algo entró a un lado, salió de otro.

`origenTipo` + `origenId` conectan el asiento con el hecho que lo generó (un pago,
una entrega, una cobertura). Eso permite ir del
número al hecho y viceversa, que es lo primero que pide un auditor.

**La regla de oro: los asientos no se editan ni se borran. Se reversan.**
`asientoReversaId` apunta al asiento que anula a otro. Un error contable queda
visible con su corrección al lado, y esa es exactamente la trazabilidad que se
exige cuando hay dinero de terceros.

**Por qué debe existir.** Es lo que convierte un montón de filas de pagos en
información financiera confiable y auditable.

**A nivel de sistema.** Trigger `AFTER` que valida el cuadre en cada asiento
confirmado. `numero` correlativo (`BIGSERIAL`): un salto en la numeración es una
señal de alarma.

---

### `MovimientoContable` / `movimiento_contable`

**Qué es.** Cada línea de un asiento: qué cuenta, cuánto al debe, cuánto al haber.

**Para qué sirve (negocio).** Es el detalle. Permite explicar un asiento en
lenguaje entendible: "de la caja del grupo salieron Bs 4.800 (haber), entraron
Bs 4.500 a la cuenta del beneficiario y Bs 300 a reposición del fondo de
garantía (debe)".

**Por qué debe existir.** Sin las líneas no hay doble partida, solo un total.

**A nivel de sistema.** Cardinalidad mínima 2 por asiento: un asiento con una sola
línea no puede cuadrar.

---

### `CierreDiario` / `cierre_diario`

**Qué es.** El corte del día: cuánto se recaudó, cuánto se concilió, cuánto quedó
en excepción, y si el día cuadró.

**Para qué sirve (negocio).** Es el **control de calidad diario del dinero**. La
disciplina que impone es simple y poderosa: cada día se cierra, y si no cuadra, no
se cierra. Los descuadres se resuelven el mismo día, cuando todavía se recuerda
qué pasó, en vez de acumularse hasta volverse irresolubles.

`reabrir(motivo, adminId)` existe porque a veces hay que corregir un día ya
cerrado — pero deja constancia de quién lo reabrió y por qué, que es lo que un
auditor va a mirar primero.

**Por qué debe existir.** Sin cierre diario, un descuadre puede vivir meses.
`cuadrado = FALSE` mientras exista una excepción sin resolver de esa fecha: la
excepción no se puede ignorar, bloquea el cierre.

---

### `ServicioConciliacion` — Servicio de dominio

**Qué es.** El orquestador: procesa webhooks, concilia en lote, detecta duplicados
y ejecuta el cierre.

**Para qué sirve (negocio).** `detectarDuplicados()` merece mención: encuentra
pagos que entraron dos veces por distintas vías (webhook + carga manual, por
ejemplo), que es la causa más común de que un participante figure con saldo a
favor inexistente.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `ObligacionAporte` | No hay forma de saber quién debe qué; los recargos ensucian el monto pactado. |
| `PoliticaMora` | Todos los grupos comparten la misma tolerancia al atraso. |
| `OrdenCobro` + `QRCobro` | No hay referencia conciliable: toda la conciliación es manual. |
| `ProveedorPago` | Si se cae el banco A el día de vencimiento, nadie puede pagar. |
| `IntentoPago` | Los pagos fallidos no dejan rastro y el soporte no puede diagnosticar. |
| `Pago` | No hay hecho económico; el "pagué" vuelve a ser palabra contra palabra. |
| `comprobante_manual` | O se castiga injustamente a quien pagó por fuera, o se abre la puerta al fraude. |
| `Conciliacion` + `Excepcion` | El saldo del grupo es una opinión, no un hecho. |
| `WebhookPasarela` | Un reintento de la pasarela acredita dos veces el mismo aporte. |
| Doble partida | Los descuadres se descubren meses después, sin forma de reconstruirlos. |
| `CierreDiario` | Los descuadres se arrastran hasta volverse irresolubles. |

---

### `TipoCambio` / `tipo_cambio`

**Qué es.** El tipo de cambio por moneda y fecha, con su fuente.

**Para qué sirve (negocio).** Los umbrales de reporte están expresados en dólares
y las operaciones ocurren en bolivianos. Sin el tipo de cambio del día guardado,
la pregunta "¿esta operación superaba el umbral?" no tiene respuesta reproducible
dos años después: cada recálculo daría un número distinto.

**Por qué debe existir.** Porque el registro de la operación relevante
([[registro_operacion_relevante]], M12) **copia** el `tipo_cambio_aplicado` al
momento del hecho. Esta tabla es de dónde sale ese número, y `fuente` dice si vino
del banco central, de un proveedor o de una carga manual.

**A nivel de sistema.** `UNIQUE (moneda_origen, moneda_destino, fecha)`. La
función `fn_fx_a_usd()` del motor de umbrales toma la cotización vigente más
reciente y **falla si no hay ninguna**: es preferible bloquear la operación a
registrarla con una conversión inventada.
