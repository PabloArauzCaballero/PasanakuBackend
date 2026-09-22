# 91 — Dinero y movimiento de fondos

Pasanaku no guarda "registros": guarda **plata de gente que confía**. Un bug de UI se disculpa;
un centavo que aparece o desaparece rompe el producto entero, porque lo único que un pasanaku
vende es confianza.

Esta regla manda sobre cualquier código que **calcule, prometa, mueva, deduzca, condone o
reporte** un importe. No admite "es solo un endpoint de lectura": un panel que muestra un saldo
mal calculado es una mentira con la misma firma que un desembolso mal hecho.

## 91.1 Las cinco prohibiciones duras

1. **Prohibido `float`/`double` para dinero**, en cualquier capa: base, DTO, front, script de
   migración, planilla de verificación. `DECIMAL(14,2)` (o `16,2` para acumulados) en base,
   entero de la menor unidad o decimal exacto en código. `0.1 + 0.2 !== 0.3` y ese error se
   acumula en cada iteración de un pasanaku de 12 periodos.
2. **Prohibido un importe sin moneda.** Toda columna, DTO y evento que lleve un importe lleva su
   `moneda CHAR(3)` ISO-4217 al lado. Prohibido sumar importes de monedas distintas sin registrar
   el tipo de cambio usado y su fecha.
3. **Prohibido editar o borrar un asiento, un movimiento contable, una bitácora, un evento de
   reputación, un movimiento de fondo o un abono de recuperación.** Son *append-only* con
   `UPDATE`/`DELETE` revocados a nivel de rol de base. Se corrige con **contra-asiento**, nunca
   con `UPDATE`. Si tu fix necesita un `UPDATE` sobre el mayor, tu fix está mal.
4. **Prohibida una operación de dinero sin `clave_idempotencia`.** Webhook de pasarela, orden de
   cobro, desembolso, deducción, tarea automatizada y notificación con enlace de pago: todas.
   Un reintento del proveedor, del usuario o de la cola **no puede acreditar dos veces**.
5. **Prohibido que el saldo sea una suma ad-hoc.** El panel de transparencia, el estado del grupo
   y cualquier total se calculan **desde el mayor** (`asiento_contable` + `movimiento_contable`),
   no desde un `SUM()` sobre la tabla de pagos. Dos fuentes de verdad para el mismo número es
   garantía de que en algún momento van a discrepar, y nadie va a saber cuál miente.

## 91.2 Invariantes que se verifican en la base, no en la app

La aplicación se reinicia, se despliega mal, se llama desde un script. La base es el último
lugar donde la invariante todavía vale.

1. `SUM(debe) = SUM(haber)` por asiento — *constraint* diferido o trigger, no un `if` en el servicio.
2. Un importe de aporte, cobertura o deducción **nunca es negativo**; el signo lo da la cuenta,
   no el número (`CHECK monto >= 0`).
3. La suma de deducciones de una entrega **nunca supera** la bolsa bruta.
4. Unicidad real sobre `clave_idempotencia` (índice `UNIQUE`), no un `SELECT` previo: entre el
   `SELECT` y el `INSERT` entra el duplicado.
5. Las tablas *append-only* tienen los permisos revocados en el rol de la aplicación. Que el
   código "no lo haga" no es un control.

## 91.3 Nada se promete antes de estar cobrado

1. **Un pago no es un pago hasta que la pasarela lo confirma por webhook verificado.** Ni la
   captura del comprobante, ni el `return_url`, ni que el usuario diga que pagó. El comprobante
   manual es evidencia para un humano, no un acreditamiento automático.
2. **La firma del webhook se valida siempre**, incluso en desarrollo. Un webhook sin firma
   verificada es un endpoint que acredita dinero a cualquiera que sepa la URL.
3. **El desembolso va contra cuenta bancaria verificada** y respeta el periodo de enfriamiento
   tras un cambio de cuenta. Cambio de cuenta + desembolso inmediato es el patrón de fraude más
   barato que existe.
4. **Ninguna deducción se aplica sin su línea.** Una entrega neta que no explica, línea por
   línea, de dónde salió cada descuento, es una entrega que el participante va a disputar y vas
   a tener que reconstruir a mano.

## 91.4 Debido proceso: el sistema no castiga solo

Marcar mora, ejecutar un aval, sancionar o bajar reputación **afecta el patrimonio y la
reputación de una persona**. Todo eso pasa por expediente:

1. Estado, evidencia, plazo de descargo y estado `FIRME` **antes** de ejecutar.
2. La reputación es *consecuencia* del expediente, no un campo que se escribe.
3. Toda sanción tiene apelación y la ruta de apelación existe en el código, no solo en el
   reglamento.
4. Prohibido automatizar la ejecución de una sanción sin el plazo de descargo cumplido, aunque
   el caso sea obvio.

## 91.5 El organizador no es caja

Regla de negocio RN-18, y es una **restricción de arquitectura**, no una preferencia:

1. **No existe comisión del organizador.** Si un cálculo la introduce, el cálculo está mal.
2. **El dinero del grupo no pasa por la cuenta del organizador.** Ningún flujo, ni "temporal",
   ni "para simplificar la conciliación".
3. El organizador tiene funciones administrativas y responsabilidad de desempeño; no tiene
   custodia. Cualquier endpoint que le permita mover fondos a discreción viola esta regla.

## 91.6 Evidencia obligatoria para cerrar un cambio con dinero

Un cambio que toca importes **no se cierra** sin esto en el `REPORTE.md` (regla 40):

1. **Asientos generados** por la operación, pegados, con débitos y créditos y su suma.
2. **Prueba de idempotencia ejecutada**: la misma operación disparada dos veces, con la salida
   que demuestra que el segundo intento no duplicó nada (`HTTP 200` + mismo `id`, o violación de
   unicidad capturada).
3. **Cuadre**: el total calculado desde el mayor comparado con el total esperado, con la
   consulta usada pegada.
4. **Caso de reversa** ejercitado: qué contra-asiento se generó.
5. **Redondeo declarado**: modo usado y qué pasa con el residuo cuando la división no es exacta
   (nunca se pierde: se asigna).

Un "los tests pasan" sin estos cinco puntos es peldaño `TESTED`, no `VERIFIED` (regla 30).

## 91.7 Skills relacionadas

`accounting-double-entry` · `money-movement-safety` · `payment-reconciliation` ·
`payments-qr-integration` · `disbursement-payouts` · `guarantee-fund-workflows` ·
`collections-delinquency` · `dispute-resolution` · `financial-close-reporting` ·
`distributed-data-integrity` · `saga-distributed-transactions` · `concurrency-and-locking` ·
`audit-trail-history` · `data-privacy-financial`
