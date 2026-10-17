# Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación

> **Pregunta de negocio que responde este módulo:**
> *La plataforma cobra una comisión pequeña por cada juego. ¿Sobre qué hecho se
> cobra, cuánto, a quién, por qué vía y cuándo? ¿Cómo se cambia esa política el
> día que el negocio decida otra cosa? ¿Y cómo se explica —tres años después—
> cuánto se le cobró exactamente a una persona y con qué respaldo legal?*

Este módulo introduce el ingreso de la plataforma. Es el módulo que hace viable
el producto, y también el que más rápido destruye la confianza si está mal hecho:
un cobro que el usuario no entiende es un reclamo, y un cobro que la empresa no
puede explicar es una observación del regulador.

La decisión de diseño que gobierna todo:

> **La política de cobro es dato, no código.**
> Sobre qué evento se cobra, sobre qué monto, con qué fórmula, a cargo de quién,
> por qué vía y en qué momento son **seis columnas de una fila**, no seis
> decisiones repartidas por el código. Cambiar la política comercial significa
> cargar un tarifario nuevo —un *seeder*— simularlo, publicarlo con preaviso y
> dejar que entre a regir. Ninguna de esas operaciones toca una línea de código
> ni requiere un despliegue.

Y su corolario, igual de importante:

> **El tarifario viejo nunca se borra.**
> Cada comisión cobrada apunta a la versión exacta del tarifario que la generó.
> Por eso siempre se puede responder "en marzo de 2027 te cobramos Bs 18 porque
> regía la versión 3, concepto `COM_ENTREGA`, 0,3% con piso de Bs 10 y techo de
> Bs 50".

---

## La política vigente es un seeder, no una regla del sistema

La configuración con la que arranca el producto es esta:

| Decisión | Valor inicial | Dónde vive |
| --- | --- | --- |
| Hecho que genera el cobro | entrega de fondo acreditada (la "jugada") | `catalogo_hecho_generador.codigo = 'ENTREGA_FONDO_ACREDITADA'` |
| Base de cálculo | monto bruto de la bolsa | `concepto_tarifa.base_calculo` |
| Fórmula | 0,30 % con piso Bs 10 y techo Bs 50 | `concepto_tarifa.valor_porcentual / monto_minimo / monto_maximo` |
| Quién la paga | el beneficiario del turno | `concepto_tarifa.sujeto_obligado` |
| Por qué vía se cobra | deducción de la entrega | `concepto_tarifa.forma_cobro` |
| Cuándo | al liquidar la entrega | `concepto_tarifa.momento_cobro` |
| Organizador | sigue sin cobrar nada | no existe concepto con `sujeto_obligado = ORGANIZADOR` a favor suyo |

**Nada de eso está en el código.** Son filas. El seeder inicial se ve así:

```sql
-- 1) El hecho generador: que se acredite una entrega de fondo
INSERT INTO catalogo_hecho_generador
  (codigo, descripcion, entidad_evento, campo_monto_base, unidad_conteo, modulo_origen, activo)
VALUES
  ('ENTREGA_FONDO_ACREDITADA', 'El beneficiario del turno cobró la bolsa',
   'entrega_fondo', 'monto_bolsa_bruto', 'ENTREGA', '04', TRUE),
  ('APORTE_ACREDITADO',  'Un aporte fue conciliado', 'pago', 'monto', 'PAGO', '03', TRUE),
  ('RETIRO_EJECUTADO',   'El usuario retiró saldo',  'orden_retiro', 'monto_solicitado', 'RETIRO', '10', TRUE),
  ('RECARGA_ACREDITADA', 'El usuario cargó saldo',   'orden_recarga', 'monto_bruto', 'RECARGA', '10', TRUE),
  ('CICLO_INICIADO',     'Arrancó un ciclo del grupo','periodo', NULL, 'CICLO', '02', TRUE);

-- 2) El tarifario versión 1
INSERT INTO tarifario (codigo, version, nombre, estado, moneda_base, vigente_desde, dias_preaviso)
VALUES ('GENERAL', 1, 'Tarifario general v1', 'VIGENTE', 'BOB', now(), 30);

-- 3) El único concepto que cobra al arrancar
INSERT INTO concepto_tarifa
  (tarifario_id, hecho_generador_id, codigo, nombre_comercial, descripcion_usuario,
   metodo_calculo, base_calculo, valor_porcentual, monto_minimo, monto_maximo,
   sujeto_obligado, forma_cobro, momento_cobro,
   gravado_iva, gravado_it, precio_incluye_impuesto, orden_aplicacion, activo)
VALUES
  (:tarifario_v1, :hecho_entrega, 'COM_ENTREGA', 'Comisión por cobro de turno',
   'Se descuenta de la bolsa cuando cobrás tu turno.',
   'PORCENTUAL', 'MONTO_BOLSA_BRUTO', 0.3000, 10.00, 50.00,
   'BENEFICIARIO_DEL_TURNO', 'DEDUCCION_DE_ENTREGA', 'AL_LIQUIDAR_ENTREGA',
   TRUE, TRUE, TRUE, 1, TRUE);

-- 4) Todo lo demás, gratis y explícito (para que se vea en el tarifario publicado)
INSERT INTO concepto_tarifa
  (tarifario_id, hecho_generador_id, codigo, nombre_comercial, metodo_calculo,
   base_calculo, sujeto_obligado, forma_cobro, momento_cobro, activo)
VALUES
  (:tarifario_v1, :hecho_aporte,  'COM_APORTE',  'Aporte al grupo',  'GRATUITO', 'SIN_BASE',
   'PAGADOR_DE_LA_OPERACION', 'DEBITO_DE_BILLETERA', 'AL_DEVENGAR', TRUE),
  (:tarifario_v1, :hecho_recarga, 'COM_RECARGA', 'Carga de saldo',   'GRATUITO', 'SIN_BASE',
   'PAGADOR_DE_LA_OPERACION', 'DEBITO_DE_BILLETERA', 'AL_DEVENGAR', TRUE);
```

### Cambiar la política, sin tocar el código

Estos son cambios reales que el negocio va a pedir, y lo que cuesta cada uno:

| El negocio pide | Qué se hace | Qué se toca del código |
| --- | --- | --- |
| "Ahora cobremos 0,25 % en vez de 0,30 %" | tarifario v2 con el concepto modificado, preaviso 30 días, publicar | nada |
| "Cobremos por aporte y no por entrega" | mismo concepto con `hecho_generador = APORTE_ACREDITADO` y `base_calculo = MONTO_APORTE` | nada |
| "Que la comisión la paguen todos y no solo el que cobra" | `sujeto_obligado = PRORRATEO_ENTRE_PARTICIPANTES` y `forma_cobro = OBLIGACION_DE_APORTE` | nada |
| "Cobro fijo de Bs 15 por ciclo y participante" | `metodo_calculo = FIJO`, `base_calculo = MONTO_FIJO_POR_PARTICIPANTE`, `hecho = CICLO_INICIADO` | nada |
| "Escalonado: 0,4 % hasta Bs 5.000 y 0,2 % arriba" | dos filas en `regla_tarifa` | nada |
| "Grupos de más de 20 personas pagan menos" | `regla_tarifa.condicion = {"tamanio_grupo": {">=": 20}}` | nada |
| "El primer ciclo es gratis" | `campana_promocional` + `aplicacion_promocion` | nada |
| "Los grupos de la campaña de El Alto no pagan" | `exencion_comision` con alcance `SEGMENTO` | nada |
| "Cobrar comisión sobre un evento que hoy no existe" | fila nueva en `catalogo_hecho_generador` | **sí**: emitir el evento desde el módulo de origen |

Solo la última fila toca código, y es razonable: si el hecho no existe, hay que
empezar a emitirlo. Todo lo demás es configuración.

### Lo que el motor de cobro sí tiene cableado

Para que quede claro dónde está el límite: el motor tiene cableado *el mecanismo*,
no *la política*. Sabe resolver qué tarifario aplica, evaluar reglas por orden,
aplicar piso y techo, redondear, calcular impuestos, devengar, cobrar por las tres
vías y facturar. Lo que nunca sabe es "cuánto" ni "a quién": eso lo lee.

---

## Paquete: Motor de tarifas

### `CatalogoHechoGenerador` / `catalogo_hecho_generador`

**Qué es.** El catálogo de eventos del sistema sobre los que se puede cobrar.

**Para qué sirve (negocio).** Es el vocabulario del motor. Un concepto de tarifa
no dice "cobrar cuando alguien cobre su turno" en prosa: apunta a un hecho del
catálogo, que a su vez sabe de qué tabla viene y qué campo usar como base. Eso es
lo que hace posible agregar un cobro nuevo sin programar la fórmula.

**Por qué debe existir.** Sin catálogo, cada concepto tendría que traer su propia
lógica de "cuándo dispara", y esa lógica terminaría en el código. Con catálogo, el
motor escucha eventos de dominio (M9) y busca qué conceptos están enganchados a
ese hecho.

---

### `Tarifario` / `tarifario` — Raíz de agregado

**Qué es.** Una versión completa y publicada de la política de precios.

**Para qué sirve (negocio).** Tres cosas a la vez:

1. **Congelar el precio histórico.** Cada devengo apunta al tarifario que rigió.
2. **Publicar.** `url_publicacion` y `hash_documento` guardan el documento que se
   puso a disposición del público. La transparencia de tarifas no es opcional para
   un proveedor de servicios de pago: es exigible y verificable.
3. **Ordenar el cambio.** Un tarifario nuevo no entra de golpe: pasa por
   `EN_PREAVISO` durante `dias_preaviso` antes de volverse `VIGENTE`.

**A nivel de sistema.** Restricción de exclusión: no puede haber dos tarifarios
`VIGENTE` con el mismo código y vigencias solapadas. Un tarifario `VIGENTE` es
inmutable —corregir un precio obliga a crear la versión siguiente— y los
sustituidos no se borran nunca.

---

### `ConceptoTarifa` / `concepto_tarifa`

**Qué es.** Un cobro concreto: nombre comercial, fórmula, sujeto, vía y momento.

**Para qué sirve (negocio).** Es **la fila donde vive la política**. Vale la pena
leer sus seis columnas de decisión como una oración:

> Sobre `hecho_generador_id`, calcular con `metodo_calculo` sobre `base_calculo`,
> a cargo de `sujeto_obligado`, cobrando por `forma_cobro` en el momento
> `momento_cobro`.

`nombre_comercial` y `descripcion_usuario` existen para que la app muestre
*"Comisión por cobro de turno — se descuenta de la bolsa cuando cobrás tu turno"*
y no `COM_ENTREGA`. Un cobro que el usuario no puede leer en su idioma es un
reclamo esperando a suceder.

`gravado_iva`, `gravado_it` y `precio_incluye_impuesto` definen el tratamiento
tributario por concepto, porque no todos los cobros tributan igual y porque el
precio que se muestra tiene que ser el precio final.

**Por qué debe existir.** Porque la alternativa —constantes, `if`s por tipo de
operación, o un campo `porcentaje_comision` en `configuracion_grupo`— hace que
cada cambio comercial sea un cambio de software, con su ciclo de QA, su despliegue
y su riesgo. Y hace imposible contestar qué se cobraba el año pasado.

**A nivel de sistema.** `UNIQUE (tarifario_id, codigo)`. `CHECK` de coherencia:
método `PORCENTUAL` exige `valor_porcentual`; `FIJO` exige `valor_fijo`;
`ESCALONADO_*` exige al menos una `regla_tarifa`.

---

### `ReglaTarifa` / `regla_tarifa`

**Qué es.** Los escalones y condiciones de un concepto.

**Para qué sirve (negocio).** Permite precios que dependen del contexto sin
multiplicar conceptos: tramos por monto, descuentos por tamaño de grupo, tarifa
distinta por canal, por antigüedad del usuario o por nivel de verificación. La
condición es `JSONB` declarativo —`{"tamanio_grupo": {">=": 20}, "canal": "APP"}`—
evaluado por el motor.

**Por qué debe existir.** Porque la primera negociación comercial seria (un
convenio con una cooperativa, una campaña regional) pide precio diferenciado, y sin
reglas eso se resuelve con código especial que nadie vuelve a tocar.

**A nivel de sistema.** Se evalúan por `orden`; gana la primera que coincide. Las
vigencias permiten programar un cambio de escalón a futuro.

---

### `PoliticaRedondeo` / `politica_redondeo`

**Qué es.** Cómo se redondea el resultado del cálculo.

**Para qué sirve (negocio).** En Bolivia el circulante mínimo práctico es Bs 0,10.
Un cálculo de 0,3 % sobre Bs 5.437 da Bs 16,311, y hay que decidir —una vez, de
forma explícita y auditable— si eso es 16,30 o 16,40. Sin política, cada
desarrollador redondea distinto y aparecen diferencias de centavos que rompen
conciliaciones.

---

### `SegmentoComercial` / `segmento_comercial` y `AsignacionTarifario` / `asignacion_tarifario`

**Qué son.** A quién se le aplica cada tarifario y con qué prioridad.

**Para qué sirven (negocio).** Resuelven la pregunta "¿qué precio le corresponde a
esta operación?" con una jerarquía explícita: usuario > grupo > segmento > global.
La asignación puede tener vigencia y motivo, de modo que un precio especial siempre
tiene fecha de inicio, fecha de fin y una razón escrita.

**Por qué deben existir.** Porque los precios especiales existen igual: si el
modelo no los soporta, aparecen como excepciones a mano en producción, que es la
peor forma posible de tenerlos.

---

### `TarifaCongeladaGrupo` / `tarifa_congelada_grupo`

**Qué es.** El snapshot del tarifario que aceptó un grupo al constituirse.

**Para qué sirve (negocio).** **El precio no cambia a mitad del juego.** Un
pasanaku de doce meses se acuerda al inicio; si la comisión sube en el mes cinco,
los que ya estaban adentro siguen con el precio pactado. Un aumento aplica a los
grupos nuevos.

`hash_snapshot` permite demostrar que el snapshot no fue alterado, y `acuerdo_id`
lo enlaza con el [[acuerdo]] (M2) por el cual los participantes lo aceptaron.

**Por qué debe existir.** Porque sin él, subir el tarifario reescribe
retroactivamente el costo de ciclos ya aceptados. Eso es, a la vez, un problema de
confianza, un reclamo con razón y un incumplimiento contractual.

---

### `SimulacionTarifa` / `simulacion_tarifa` y `CambioTarifario` / `cambio_tarifario`

**Qué son.** El ensayo previo y el expediente del cambio.

**Para qué sirven (negocio).** La simulación permite correr el tarifario nuevo
sobre la historia real antes de publicarlo: cuánto más se habría cobrado, a cuánta
gente le habría subido y en cuánto. Eso convierte una decisión de precios en una
decisión informada y deja constancia de que se evaluó el impacto.

`cambio_tarifario` guarda el cumplimiento del deber de aviso: tipo de cambio, días
de preaviso, fecha y canal del aviso, cuántos usuarios fueron notificados y si el
cambio habilita rescindir sin costo. Un aumento de comisiones sin aviso previo
documentado es una observación segura.

---

## Paquete: Devengo y cobro

### `CotizacionComision` / `cotizacion_comision`

**Qué es.** El cálculo mostrado al usuario **antes** de que la operación ocurra.

**Para qué sirve (negocio).** Que la pantalla diga *"vas a recibir Bs 5.982:
Bs 6.000 menos Bs 18 de comisión"* y que ese número quede guardado con su
desglose. Cuando el usuario reclama, no se discute contra un recálculo: se
muestra la cotización que él vio y aceptó.

**Por qué debe existir.** Porque el reclamo típico no es "me cobraron de más", es
"nadie me avisó". La cotización es la prueba del aviso.

---

### `DevengoComision` / `devengo_comision` — Raíz de agregado, append-only

**Qué es.** El hecho económico: la plataforma ganó esta comisión, por esta
operación, en esta fecha.

**Para qué sirve (negocio).** Es **el ingreso**, separado del cobro. La distinción
importa: se puede haber devengado y no cobrado (el usuario no tenía saldo), o
haber devengado y devuelto (la entrega se anuló). Contablemente son situaciones
distintas y el estado las distingue: `DEVENGADO`, `COBRADO_PARCIAL`, `COBRADO`,
`EXONERADO`, `DEVUELTO`, `INCOBRABLE`, `REVERSADO`.

`periodo_contable` permite cerrar meses; `asiento_contable_id` conecta con la
doble partida de M3, donde el ingreso impacta contra la cuenta configurada en el
concepto.

**Por qué debe existir separado de `CargoComision`.** Porque mezclar devengo y
cobro produce el error clásico: reconocer como ingreso lo que nunca se cobró, o
perder de vista lo cobrado que había que devolver.

**A nivel de sistema.** append-only. `UNIQUE (clave_idempotencia)` y
`UNIQUE (referencia_tipo, referencia_id, concepto_tarifa_id)`: **la misma entrega
no puede devengar dos veces la misma comisión**, ni siquiera si el evento se
procesa dos veces.

---

### `CargoComision` / `cargo_comision`

**Qué es.** El intento —y el resultado— de cobrar efectivamente el devengo.

**Para qué sirve (negocio).** Materializa las tres vías de cobro y las mantiene
unificadas:

- **`DEDUCCION_DE_ENTREGA`** → una línea de [[deduccion_entrega]] (M4). Es la vía
  por defecto: la plataforma cobra donde ya tiene el dinero.
- **`DEBITO_DE_BILLETERA`** → una [[transaccion_billetera]] (M10).
- **`OBLIGACION_DE_APORTE`** → una [[obligacion_aporte]] (M3), para el escenario
  de prorrateo entre participantes.

Que las tres vías cuelguen del mismo devengo es lo que permite cambiar la forma de
cobro por configuración: el devengo no cambia, cambia por dónde se cobra.

**A nivel de sistema.** `UNIQUE (deduccion_entrega_id)`. Tras tres intentos
fallidos, el devengo pasa a [[cuenta_por_cobrar_comision]] y entra al circuito de
cobranza de M8 en lugar de reintentar indefinidamente.

---

### `ExencionComision` / `exencion_comision`, `CampanaPromocional` / `campana_promocional`, `AplicacionPromocion` / `aplicacion_promocion`

**Qué son.** No cobrar, con nombre y apellido.

**Para qué sirven (negocio).** Toda plataforma termina regalando comisiones: por
campaña, por cortesía tras un error, por convenio institucional. La diferencia
entre hacerlo bien y hacerlo mal es si queda registro de **quién lo autorizó, por
qué motivo, con qué tope y hasta cuándo**.

`campana_promocional` agrega presupuesto: una promoción con `presupuesto_maximo`
se apaga sola cuando lo consume, en vez de descubrirse a fin de mes.

**Por qué deben existir.** Porque sin ellas la exención se implementa como un
`monto = 0` a mano, y entonces es imposible distinguir "no correspondía cobrar" de
"alguien no cobró".

---

### `DevolucionComision` / `devolucion_comision` y `CuentaPorCobrarComision` / `cuenta_por_cobrar_comision`

**Qué son.** Los dos finales alternativos del devengo: se devuelve o queda por
cobrar.

**Para qué sirven (negocio).** La devolución tipifica el motivo —entrega anulada,
error de tarifa, reclamo procedente, falla de servicio— porque cada uno alimenta
un indicador distinto: los errores de tarifa son deuda técnica, las fallas de
servicio son riesgo operativo, y los reclamos procedentes son el indicador que
mira el defensor del consumidor financiero.

La cuenta por cobrar conecta con la gestión de cobranza (M8), reutilizando toda la
maquinaria que ya existe para la mora de aportes.

---

## Paquete: Impuestos y facturación electrónica

### `Impuesto` / `impuesto` y `CalculoImpuesto` / `calculo_impuesto`

**Qué son.** Las alícuotas vigentes y su aplicación a cada devengo.

**Para qué sirven (negocio).** Que el impuesto sea una fila con `vigente_desde` y
`base_legal`, y que su cálculo quede guardado por devengo. Cuando cambia una
alícuota, el histórico conserva la que se aplicó, que es lo que exige cualquier
revisión tributaria.

`precio_incluye_impuesto` en el concepto decide si el precio publicado es final o
si el impuesto se suma encima. Para el consumidor final, el precio publicado tiene
que ser el final: la comisión de Bs 18 es Bs 18, impuestos incluidos.

---

### `DatosFacturacion` / `datos_facturacion`, `FacturaElectronica` / `factura_electronica`, `NotaCreditoDebito` / `nota_credito_debito`, `LoteEnvioSin` / `lote_envio_sin`

**Qué son.** El documento fiscal y su ciclo de vida.

**Para qué sirven (negocio).** Cobrar una comisión obliga a facturarla. El modelo
cubre la facturación electrónica boliviana con sus piezas propias: NIT emisor,
sucursal, punto de venta, número correlativo, CUF, CUFD, código de control,
leyenda y QR de verificación. `estado_fiscal` incluye `EMITIDA_OFFLINE`, porque el
servicio de impuestos se cae y la operación no puede detenerse: se emite fuera de
línea y se envía después por lote.

Una factura **no se edita ni se borra**: se anula y se emite nota de crédito. La
nota de crédito se enlaza con la devolución que la origina, de modo que la
devolución de plata y el documento fiscal quedan atados.

**Por qué deben existir.** Porque la alternativa —facturar a mano por fuera— es
inviable al primer mes de operación y es una contingencia tributaria desde el
primer día.

---

## Paquete: Resultado del negocio

### `LiquidacionIngresos` / `liquidacion_ingresos`

**Qué es.** El cierre mensual del ingreso: devengado, cobrado, exonerado, devuelto,
incobrable, impuestos, costos de proveedores e ingreso neto.

**Para qué sirve (negocio).** Es el estado de resultados del producto, construido
desde los devengos y no desde una planilla aparte. Y es el número que se compara
contra la contabilidad: si `liquidacion_ingresos.total_cobrado` no coincide con el
saldo de la cuenta de ingresos del mayor, hay un problema y se ve el mismo mes.

---

### `CostoProveedorOperacion` / `costo_proveedor_operacion`

**Qué es.** Lo que cobra la pasarela, el banco o el agente por cada operación.

**Para qué sirve (negocio).** Saber el **margen real**. Una comisión de Bs 18 con
un costo de pasarela de Bs 14 es un negocio distinto del que parece. Sin esta
tabla, el costo aparece agregado a fin de mes y nunca se puede atribuir a la
operación que lo generó, ni decidir con datos qué proveedor conviene para qué
rango de monto.

---

## Qué NO cambia: el organizador sigue sin cobrar (RN-18)

Conviene ser explícito, porque este módulo introduce comisiones y podría leerse al
revés:

- **La comisión es de la plataforma**, por prestar el servicio: custodia, cobro,
  conciliación, notificación, garantía y soporte.
- **El organizador no percibe nada.** No existe ningún concepto de tarifa cuyo
  beneficiario sea el organizador, ni cuenta por pagar hacia él, ni devengo a su
  favor. Sigue siendo un participante más con funciones administrativas.
- **La bolsa se descuenta por comisión de plataforma, nunca por comisión de
  organizador.** El tipo de deducción es `COMISION_PLATAFORMA` y su origen es un
  `cargo_comision`, trazable hasta el concepto y el tarifario que lo justifican.

La razón de fondo es la misma de siempre: cuando quien administra el dinero del
grupo también gana con él, el incentivo se tuerce. La plataforma cobra por un
servicio que presta con reglas públicas y auditables; el organizador administra sin
ganar, y por eso no tiene motivo para inflar el grupo ni para apurar una entrega.

---

## Cómo se conecta con el resto del modelo

| Con | Por dónde | Para qué |
| --- | --- | --- |
| **M2 Grupos** | `tarifa_congelada_grupo.grupo_id`, `acuerdo_id` | el precio se pacta al constituir el grupo |
| **M3 Pagos** | `cuenta_ingreso_id`, `asiento_contable_id`, `obligacion_id` | el ingreso impacta el mayor por doble partida |
| **M4 Entregas** | `cargo_comision.deduccion_entrega_id` | la comisión se deduce de la bolsa, línea explícita |
| **M8 Cobranza** | `cuenta_por_cobrar_comision.gestion_cobranza_id` | la comisión impaga se cobra con la maquinaria existente |
| **M9 Auditoría** | eventos de dominio, reportes | cada devengo emite evento y entra a los reportes |
| **M10 Billetera** | `cargo_comision.transaccion_id` | cobro por débito de saldo |
| **M12 Cumplimiento** | `devolucion_comision.reclamo_id`, tarifario publicado | reclamos por cobros y transparencia de tarifas |

---

## Las cinco preguntas que este módulo tiene que poder responder

1. *"¿Por qué me descontaron Bs 18?"* → [[cotizacion_comision]] + [[devengo_comision]] + el concepto del tarifario vigente al momento.
2. *"¿Cuánto cobraba la plataforma en marzo del año pasado?"* → el tarifario versión N, intacto.
3. *"¿Avisaron antes de subir la comisión?"* → [[cambio_tarifario]] con fecha, canal y cantidad de notificados.
4. *"¿Cuánto ganó realmente la plataforma este mes?"* → [[liquidacion_ingresos]], neta de devoluciones, exenciones y costos de proveedor.
5. *"¿Quién autorizó no cobrarle a este grupo?"* → [[exencion_comision]], con autorizante, motivo y vigencia.


---

## Tablas incorporadas después de la redacción original

> Fichas escritas para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Cada
> afirmación sale de la fuente que se cita al pie de la ficha: no agregan reglas, las explican.

### `EventoSignificativoSin` / `evento_significativo_sin`

**Qué es.** Un período en que la facturación electrónica no pudo enviarse al SIN (por ejemplo, porque el servicio no respondió), declarado como evento significativo.

**Para qué sirve (negocio).** Que las facturas emitidas sin conexión sigan siendo válidas: se registra el evento y después se envían en lote dentro del plazo que fija el SIN.

**Por qué debe existir.** Sin el evento, las facturas emitidas fuera de línea quedarían sin respaldo ante el SIN y la plataforma incumpliría su obligación tributaria.

**A nivel de sistema.** `codigo_evento` y `cufd_evento` identifican el evento ante el SIN. `fecha_inicio`, `fecha_fin` y `cantidad_documentos_offline` delimitan lo emitido sin conexión. `plazo_registro` es el límite para registrarlo. `codigo_recepcion_evento` es único cuando el SIN lo acepta.

*Fuente: CU-32 (flujo 3a); `servicios/tarifas/.../CU32EmitirFactura.java`.*
