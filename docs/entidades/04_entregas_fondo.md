# Módulo 4 — Entregas de Fondo (liquidación y desembolso)

> **Pregunta de negocio que responde este módulo:**
> *Llegó el día en que a esta persona le toca cobrar. ¿Está completa la bolsa?
> ¿Cuánto le corresponde realmente? ¿La cuenta a la que voy a mandar la plata es
> suya? ¿Y cómo sé que la recibió?*

Este es el momento de la verdad del pasanaku. Todo lo anterior —invitar, aportar,
sortear turnos— existe para llegar acá. Y es también el momento de máximo riesgo:
es cuando sale plata de verdad, hacia afuera, y donde un error no se corrige con
un `UPDATE`.

El principio que gobierna el módulo:

> **La entrega es una liquidación, no una transferencia.**
> Se calcula la bolsa bruta, se aplican deducciones línea a línea (deuda propia,
> reposición de cobertura), y recién entonces se desembolsa el neto contra una
> cuenta bancaria previamente verificada.

---

## Paquete: Entrega y Liquidación

### `EntregaFondo` / `entrega_fondo` — Raíz de agregado

**Qué es.** El acto de entregar la bolsa de un período al beneficiario del turno.

**Para qué sirve (negocio).** Es el producto que el participante compró. Los tres
montos que lleva cuentan toda la historia:

- `montoBolsaBruto`: lo que se juntó entre todos.
- `totalDeducciones`: lo que se le descuenta (y que hay que poder explicar línea
  por línea).
- `montoNetoAEntregar`: lo que efectivamente recibe.

Esa separación existe porque **la queja número uno en el momento de cobrar es "yo
esperaba Bs 6.000 y me llegaron Bs 5.450"**. Si el sistema solo guarda el neto, no
hay forma de responderla. Con las tres cifras y el detalle de deducciones, la
respuesta es inmediata y verificable.

La máquina de estados es intencionalmente conservadora. Note los dos estados de
bloqueo separados:
- `BLOQUEADA_POR_FONDO_INCOMPLETO`: falta plata en la bolsa. Es un problema del
  grupo (alguien no aportó) y dispara el módulo 8.
- `BLOQUEADA_POR_VALIDACION`: la bolsa está pero algo del beneficiario no cierra
  (cuenta sin verificar, deuda propia sin plan, KYC vencido). Es un problema
  individual.

Son dos conversaciones completamente distintas con dos personas distintas. Un solo
estado "bloqueada" las confundiría.

`RECHAZADA_POR_BENEFICIARIO` cubre un caso real: la persona ve el monto neto, no
está de acuerdo con las deducciones y objeta antes de aceptar.

**Por qué debe existir.** Sin entrega como entidad, el desembolso sería un pago
más y no habría dónde registrar el cálculo, las validaciones previas, la
autorización ni la confirmación.

**A nivel de sistema.** Restricciones clave:
- `UNIQUE (turno_id)` y `UNIQUE (periodo_id)`: **es imposible entregar dos veces el
  mismo turno.** Esta es la protección más importante del módulo.
- `CHECK monto_neto_a_entregar = monto_bolsa_bruto - total_deducciones`.
- Trigger: no se permite pasar a `AUTORIZADA` si existe una validación previa
  bloqueante con resultado `RECHAZADA`.
- `autorizadaPor` ≠ `ejecutadaPor` habilita segregación de funciones: quien
  autoriza no es quien ejecuta.

---

### `DeduccionEntrega` / `deduccion_entrega`

**Qué es.** Cada descuento aplicado a la bolsa antes de entregarla, con su monto y
su origen.

**Para qué sirve (negocio).** Es **la entidad que hace explicable la entrega**.
Siete tipos, todos con un caso real detrás:

- `APORTE_PROPIO_DEL_PERIODO`: el beneficiario también debe su aporte de este
  período. No tiene sentido cobrarle Bs 500 y pagarle Bs 6.000 el mismo día: se
  netea. Sin esta línea, la persona tendría que hacer una transferencia para
  recibir otra, con dos comisiones bancarias de por medio.
- `DEUDA_VENCIDA_PROPIA` y `RECARGO_MORA_PROPIO`: **el turno es el momento de
  máxima capacidad de cobro de todo el ciclo.** Es la única vez que el sistema
  tiene plata del deudor en la mano. Descontarla ahí es lo que hace recuperable
  una cartera morosa.
- `REPOSICION_FONDO_GARANTIA`: si el fondo cubrió un aporte suyo hace tres meses,
  ahora que cobra lo devuelve.
- `RETENCION_IMPUESTO` y `COSTO_TRANSFERENCIA`: los descuentos que no decide nadie.
- `COMISION_PLATAFORMA`: la comisión del servicio (M11). Es la única deducción que
  representa un ingreso de la empresa, y por eso es la que más rigor exige:
  `referenciaOrigenId` apunta al `cargo_comision`, que apunta al concepto y a la
  versión exacta del tarifario que la calculó. La pantalla puede decir *"Bs 18 —
  comisión por cobro de turno, 0,3 % de la bolsa"* y el usuario puede verificarlo
  contra el tarifario publicado.

  > **Vigencia ([[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]], D-04).** Por la regla de pozo completo la
  > comisión es un **cargo separado**: el principal se acredita íntegro y el cargo es una línea propia, no una
  > deducción de la bolsa. El camino con deducciones queda como legado.

  **Sigue sin haber deducción por comisión del organizador**: administrar no se
  cobra (RN-18). El cargo es para pagar el servicio de la plataforma,
  nunca para pagarle a quien administra el grupo.

`referenciaOrigenId` apunta al hecho concreto que justifica cada descuento. Eso es
lo que permite que la pantalla diga "Bs 500 — tu aporte del período 7" en vez de
"deducciones: Bs 550", que es lo que genera desconfianza.

**Por qué debe existir.** Sin deducciones línea a línea, la única alternativa es un
campo `total_descontado` que nadie puede auditar ni explicar. Y el beneficiario
tiene todo el derecho a saber por qué recibió menos.

**A nivel de sistema.** `referencia_origen_id` polimórfica según el tipo:
`obligacion_aporte.id` (M3) y `cobertura_incumplimiento.id` (M8). `revertida_en`
permite anular una deducción aplicada por error sin borrar el rastro.

---

### `ValidacionPreEntrega` / `validacion_pre_entrega`

**Qué es.** El resultado de aplicar cada regla de control antes de autorizar la
entrega.

**Para qué sirve (negocio).** Es el **checklist ejecutado y guardado**. Las reglas
típicas (RN-05):

1. Todos los aportes del período están pagados o cubiertos por el fondo.
2. El beneficiario no tiene deuda vencida sin plan de regularización.
3. La cuenta bancaria está verificada y a su nombre.
4. El KYC está vigente para el monto que se va a entregar.
5. No existe una entrega previa para el mismo turno.

Guardar cada evaluación con `valorEsperado` y `valorObtenido` responde la pregunta
del beneficiario bloqueado: *"¿por qué no me pagan?"* Respuesta: "faltan Bs 500 de
la bolsa: se esperaban Bs 6.000 y hay Bs 5.500".

`omitir(usuarioId, justificacion)` es deliberado y controlado. Siempre hay
excepciones legítimas, pero omitir una validación bloqueante exige justificación
escrita, queda con autor y va a la bitácora del módulo 9. `ReglaEntrega.rolQuePuedeOmitir`
limita quién puede hacerlo.

**Por qué debe existir.** Sin persistir las validaciones, un bloqueo es un mensaje
de error efímero. Con ellas, es un expediente: se sabe qué se revisó, cuándo, con
qué resultado, y quién decidió saltarse qué.

---

### `ReglaEntrega` / `regla_entrega` — Política configurable

**Qué es.** El catálogo de controles previos, con su orden, si bloquean y quién
puede omitirlos.

**Para qué sirve (negocio).** Permite endurecer o relajar los controles según el
tipo de grupo y según la experiencia operativa, sin desplegar código. Si aparece
un patrón de fraude nuevo (por ejemplo, cambios de cuenta bancaria justo antes del
turno), se agrega una regla y aplica a todas las entregas siguientes.

`esBloqueante` vs advertencia es la distinción clave: hay controles que deben
detener la entrega y otros que solo deben avisar.

**Por qué debe existir.** Con reglas hardcodeadas, cada ajuste de control es un
release y no queda registro de qué reglas estaban vigentes cuando se autorizó una
entrega vieja.

---

## Paquete: Desembolso

### `CuentaBancariaBeneficiario` / `cuenta_bancaria_beneficiario`

**Qué es.** La cuenta bancaria o billetera donde el usuario quiere recibir su
plata, verificada.

**Para qué sirve (negocio).** Es **el punto de mayor riesgo de fraude de toda la
plataforma**, y por eso tiene tres defensas explícitas:

1. **`estadoVerificacion` + `metodoVerificacion`.** No se transfiere a una cuenta
   que nadie validó. El micro-depósito o la coincidencia de nombre con el titular
   son lo que confirma que la cuenta existe y es de esa persona.
2. **`coincideTitularCon(usuario)`.** La cuenta debe estar a nombre del
   beneficiario. Si no coincide, o hay un error, o hay alguien cobrando por otro —
   ambas cosas requieren revisión.
3. **`bloqueadaHasta` — período de enfriamiento.** Esta es la defensa contra el
   fraude clásico: *"me cambiaron la cuenta justo antes de cobrar"*. Alguien toma
   control de la cuenta, cambia el destino bancario y espera al turno. Con
   enfriamiento, un cambio de cuenta no puede usarse inmediatamente, y el usuario
   real recibe el aviso a tiempo para reaccionar.

**Por qué debe existir.** Sin esta entidad, el destino del dinero sería un campo
de texto en la entrega, editable, sin verificación y sin historial.

**A nivel de sistema.** `numero_cuenta_cifrado` con cifrado a nivel de columna.
`hash_numero_cuenta` permite detectar que **dos usuarios distintos declararon la
misma cuenta destino** — patrón típico de mula financiera o de participantes
ficticios — y dispara alerta de cumplimiento en el módulo 9, sin necesidad de
descifrar nada.

---

### `OrdenDesembolso` / `orden_desembolso`

**Qué es.** La instrucción concreta al proveedor de pagos para transferir el neto
a la cuenta del beneficiario.

**Para qué sirve (negocio).** Separa la decisión (la entrega autorizada) de la
ejecución (la transferencia). Eso importa porque una transferencia puede fallar,
ser devuelta por el banco o quedar en proceso durante horas, **sin que eso
signifique que la entrega deba recalcularse**. La entrega ya está decidida; lo que
se reintenta es el desembolso.

`glosa` es lo que el beneficiario ve en su extracto bancario. Que diga "Pasanaku
Los Amigos - Periodo 7" en vez de un código interno es una diferencia real para
que la persona reconozca de dónde vino la plata.

**Por qué debe existir.** Sin orden separada, un desembolso fallido dejaría la
entrega en un estado ambiguo. Además, una entrega puede tener varias órdenes: la
primera rebotó por datos bancarios, la segunda fue a la cuenta corregida.

**A nivel de sistema.** `clave_idempotencia` `UNIQUE`: **el riesgo de duplicar un
desembolso es plata perdida de verdad**, no un registro duplicado. Es la
protección más crítica de la tabla.

---

### `IntentoDesembolso` / `intento_desembolso`

**Qué es.** Cada ejecución de una orden de desembolso, con el resultado que
devolvió el proveedor.

**Para qué sirve (negocio).** Guarda por qué falló y cuándo se puede reintentar
(`reintentableEn`). Cuando el beneficiario reclama que no le llegó, el
`mensajeProveedor` dice si fue "cuenta inexistente", "cuenta bloqueada" o
"servicio no disponible" — tres diagnósticos que llevan a tres acciones distintas.

`programarReintento()` implementa backoff: no tiene sentido reintentar cada
segundo contra un banco caído.

**Por qué debe existir.** Sin intentos, un desembolso fallido es un estado sin
explicación, y el soporte no puede decirle al usuario qué tiene que corregir.

---

## Paquete: Confirmación e Incidencias

### `ConfirmacionRecepcion` / `confirmacion_recepcion`

**Qué es.** La confirmación del beneficiario de que efectivamente recibió su
plata.

**Para qué sirve (negocio).** Cierra el círculo. El sistema puede saber que el
banco aceptó la transferencia y aun así la plata no haber llegado (cuenta
equivocada, retención bancaria, error del proveedor). **La única confirmación
válida es la del que tenía que recibirla.**

`montoConfirmado` permite detectar la discrepancia: el sistema mandó Bs 5.450, la
persona confirma haber recibido Bs 5.400. Esos Bs 50 son una comisión no prevista
o un error, y hay que investigarlos.

`autoconfirmadaPorVencimiento` es un compromiso operativo necesario: si nadie
confirmara nunca, las entregas quedarían abiertas para siempre y el grupo no podría
cerrar el período. Pasado el `plazoLimite` se autoconfirma — pero **queda marcado
que fue automática**, no que la persona dijo que sí. La distinción importa si
después hay reclamo.

`objetar(motivo)` es lo que abre una incidencia formal.

**Por qué debe existir.** Sin confirmación, "entregado" significa "lo mandamos", y
esa diferencia es exactamente donde se pierde la plata y la confianza.

**A nivel de sistema.** `token_confirmacion_id` apunta a M1: la confirmación se
hace con un código enviado al teléfono verificado, no con un clic que cualquiera
con acceso a la sesión podría dar.

---

### `IncidenciaEntrega` / `incidencia_entrega`

**Qué es.** Cualquier problema con una entrega, tipificado, con severidad,
responsable y SLA.

**Para qué sirve (negocio).** Los ocho tipos son el catálogo de todo lo que puede
salir mal en el momento más sensible: fondo incompleto, datos bancarios erróneos,
desembolso rechazado, monto que no coincide, el beneficiario no recibió, entrega
duplicada, reclamo de un tercero, sospecha de fraude.

`slaHoras` y `fechaLimiteSLA` son lo que convierte el reclamo en algo con reloj.
**Una entrega que no llega es la emergencia máxima de la plataforma**: la persona
esperó meses su turno y contaba con esa plata para algo concreto. Un reclamo así
no puede quedar en una bandeja sin dueño ni plazo.

`ENTREGA_DUPLICADA` está tipificada aunque las restricciones de base de datos la
hagan improbable: si alguna vez ocurre por una vía inesperada, hay que poder
gestionarla, no descubrirla en el cierre contable.

**Por qué debe existir.** Sin incidencias, el problema en una entrega es una
conversación de WhatsApp con soporte, sin trazabilidad, sin plazo y sin métrica.

---

### `HistorialEstadoEntrega` / `historial_estado_entrega`

**Qué es.** Cada transición de la entrega, con motivo y autor.

**Para qué sirve (negocio).** Reconstruye la línea de tiempo: cuándo se programó,
cuándo se bloqueó y por qué, quién la autorizó, cuándo se ejecutó, cuándo se
confirmó. Es lo primero que se mira ante un reclamo de "me pagaron tarde", y es la
única forma de distinguir un retraso del grupo (faltaba plata) de un retraso
operativo (el desembolso se quedó trabado dos días).

**Por qué debe existir.** El campo `estado` dice dónde está; el historial dice
cuánto tardó cada paso y quién lo movió. Sin él no hay métricas de tiempo de
entrega ni responsables identificables.

---

### `ServicioEntregas` — Servicio de dominio

**Qué es.** El orquestador: detecta entregas listas, liquida períodos, procesa
confirmaciones vencidas y reintenta desembolsos fallidos.

**Para qué sirve (negocio).** Es lo que hace que las entregas ocurran solas, sin
que un organizador tenga que acordarse. Como todo grupo tiene un organizador humano habilitado
([[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]]), este servicio automatiza las entregas, no reemplaza a quien administra.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `EntregaFondo` | El desembolso es un pago más, sin cálculo, sin autorización ni confirmación. |
| `DeduccionEntrega` | No se puede explicar por qué el beneficiario recibió menos de lo que esperaba. |
| `ValidacionPreEntrega` | Se entrega con la bolsa incompleta o a una cuenta sin verificar (viola RN-05). |
| `ReglaEntrega` | Cada ajuste de control es un release y no se sabe qué reglas regían para una entrega vieja. |
| `CuentaBancariaBeneficiario` | Se abre la puerta al fraude de "me cambiaron la cuenta justo antes de cobrar". |
| `OrdenDesembolso` | Un reintento puede transferir dos veces: plata perdida de verdad. |
| `IntentoDesembolso` | El beneficiario no sabe qué debe corregir para que le llegue. |
| `ConfirmacionRecepcion` | "Entregado" significa "lo mandamos", que no es lo mismo. |
| `IncidenciaEntrega` | La emergencia máxima de la plataforma queda sin dueño y sin plazo. |


---

## Tablas incorporadas después de la redacción original

> Fichas escritas para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Cada
> afirmación sale de la fuente que se cita al pie de la ficha: no agregan reglas, las explican.

### `FondeoEntrega` / `fondeo_entrega`

**Qué es.** De dónde sale cada boliviano del pozo de una entrega: cuánto está confirmado, cuánto cubrió el fondo mutual, cuánto faltó, cuánto cubrió la empresa y cuánto queda pendiente.

**Para qué sirve (negocio).** Que la persona que cobra su turno reciba el pozo completo (ADR-051) y que se sepa, al centavo, quién puso la plata que faltó y quién la sigue debiendo.

**Por qué debe existir.** El pozo completo exige separar la entrega (lo que recibe la persona) del fondeo (de dónde salió). Sin esta tabla, una cobertura empresarial se confundiría con un aporte pagado y la deuda que se conserva desaparecería.

**A nivel de sistema.** Una por entrega y por turno. R-DES-03: `monto_pozo = monto_confirmado + monto_cubierto_mutual + monto_faltante`; `monto_pendiente = monto_faltante - monto_cubierto_empresa`; el estado es `FONDEADO` solo con pendiente cero. `cobertura_respaldo_id` apunta a la cobertura empresarial del módulo 8 cuando la hubo.

*Fuente: CU-22 (R-DES-03, ADR-051); `docs/Restricciones.md`.*

### `OfertaTurno` / `oferta_turno`

**Qué es.** La publicación de un participante que ofrece vender su derecho a cobrar un turno.

**Para qué sirve (negocio).** Que quien necesita la plata antes (o después) pueda ceder su turno a otra persona por un precio, dentro de la plataforma y con reglas, en lugar de arreglos de palabra fuera del grupo.

**Por qué debe existir.** El derecho a cobrar un turno vale dinero. Sin una oferta registrada no hay forma de impedir que el mismo turno se venda dos veces ni de saber qué se ofreció, a qué precio y con qué cargos.

**A nivel de sistema.** R-DES-04: una sola oferta viva por turno (PUBLICADA, RESERVADA o LIQUIDANDO; `uq_oferta_turno_activa`). Los cargos no superan el precio. `monto_derecho` es lo que vale el turno y `monto_precio` lo que se pide. Vence en `vigente_hasta`.

*Fuente: CU-22 (R-DES-04); `docs/Restricciones.md`.*

### `CesionDerecho` / `cesion_derecho`

**Qué es.** La compra de un turno ofrecido: el paso del derecho a cobrar de un participante a otro, con sus estados hasta liquidarse.

**Para qué sirve (negocio).** Que el comprador no pierda su plata si la cesión falla, y que el vendedor no pierda su turno sin haber cobrado: primero se retienen los fondos, después se asigna el título y al final se liquida.

**Por qué debe existir.** La cesión mueve dinero y cambia quién cobra. Sin un registro con estados no hay forma de saber en qué paso quedó una cesión interrumpida ni de compensarla.

**A nivel de sistema.** R-DES-05: una cesión viva por turno, entre partes distintas. Sin `retencion_ref` no hay `FONDOS_RETENIDOS`, `TITULO_ASIGNADO` ni `LIQUIDADA`. `LIQUIDADA` exige `liquidacion_ref` y fecha, y `FALLIDA` exige motivo. `clave_idempotencia` única.

*Fuente: CU-22 (R-DES-05); `docs/Restricciones.md`.*

### `SubastaCupo` / `subasta_cupo`

**Qué es.** El cupo que su titular pone en subasta: precio base, plazo de cierre y el estado de la venta, de `ABIERTA` hasta `VENDIDA`, `DESIERTA`, `CANCELADA` o `FALLIDA`.

**Para qué sirve (negocio).** Que quien ya no puede seguir en el pasanaku venda su lugar sin votación del grupo: cualquiera que cumpla lo que pide el grupo ofrece, gana la oferta más alta al cierre y el turno no se mueve.

**Por qué debe existir.** El cierre cruza tres servicios (entregas adjudica, grupos pasa el cupo, la billetera paga) y lo irreversible va al final. Sin un registro con estados no se sabe en qué paso quedó una subasta interrumpida ni a quién se adjudicó.

**A nivel de sistema.** `monto_aporte` y `monto_base` mayores que cero, con `moneda`. `puja_ganadora_id`, `traspaso_ref` y `liquidacion_ref` se llenan a medida que avanza el cierre; `FALLIDA` lleva `motivo_fallo`. `clave_idempotencia` y `version` para que el cierre programado no adjudique dos veces.

*Fuente: CU-64 (traspaso por subasta, PR #346); `sql/10_tablas/04_entregas_fondo/subasta_cupo.sql`.*

### `PujaSubasta` / `puja_subasta`

**Qué es.** Una oferta por un cupo en subasta: quién ofrece, cuánto y en qué estado está (`VIGENTE`, `SUPERADA`, `LIBERADA`, `GANADORA`, `RECHAZADA`).

**Para qué sirve (negocio).** Que ofrecer sea serio y no cueste plata a quien pierde: el monto queda retenido en la billetera del postor y, si alguien lo supera, se le devuelve al instante.

**Por qué debe existir.** Cada puja mueve saldo retenido (`retencion_ref`). Sin su registro no hay forma de liberar la retención correcta cuando la superan, ni de saber cuál ganó al cierre.

**A nivel de sistema.** `monto` mayor que cero, con `moneda`. La retención la crea y la libera `nucleo-financiero` (CU-13, liberación de mercado). `clave_idempotencia` para que reintentar una oferta no retenga dos veces.

*Fuente: CU-64 y CU-13 (PR #346); `sql/10_tablas/04_entregas_fondo/puja_subasta.sql`.*
