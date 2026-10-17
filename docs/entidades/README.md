# Diccionario razonado de entidades — AportaYa v2.0 + Parches A y B

Este directorio documenta **qué hace cada entidad del modelo y por qué debería
existir**. No es un diccionario de datos (los tipos y las claves ya están en los
`.puml`); es la justificación de negocio de cada pieza: qué problema real del
pasanaku resuelve, qué se rompería si se elimina, y qué papel cumple en el
sistema.

## Dónde encaja este directorio

Estas fichas responden **por qué** existe cada entidad. Para la estructura —columnas,
claves, cardinalidades— está la bóveda de Obsidian en [`docs/`](../Index.md), con una
nota por tabla ([Entidades](../Modelos/Entidades/_Entidades.md)) y una por clave
foránea ([Relaciones](../Modelos/Relaciones/_Relaciones.md)). Esas notas se generan
desde los `.puml` con `scripts/generar_boveda.py`; estas fichas están escritas a mano.

## Cómo leer cada ficha

Cada entidad se documenta con cuatro preguntas:

| Sección | Responde |
| --- | --- |
| **Qué es** | Definición en una línea, en lenguaje del negocio. |
| **Para qué sirve (negocio)** | Qué necesidad real del pasanaku cubre, qué decisión habilita, quién la usa. |
| **Por qué debe existir** | Qué pasa si se fusiona con otra tabla o se elimina. El costo concreto de no tenerla. |
| **A nivel de sistema** | Invariantes, claves, índices, transaccionalidad, integraciones. |

## Índice de módulos

Cada módulo tiene dos archivos en este directorio: el `.puml` con los diagramas
(modelo de clases + modelo relacional) y el `.md` con la justificación de negocio
de cada entidad.

| Documento | Diagramas | Foco de negocio |
| --- | --- | --- |
| [01 — Identidad, Usuarios y Seguridad](01_identidad_usuarios.md) | [`.puml`](01_identidad_usuarios.puml) | Saber con certeza a quién le estás confiando plata ajena |
| [02 — Grupos, Cupos, Turnos y Gobernanza](02_grupos_turnos.md) | [`.puml`](02_grupos_turnos.puml) | Reglas del juego, orden de cobro y decisiones colectivas |
| [03 — Aportes, Pagos QR y Conciliación](03_aportes_pagos_qr.md) | [`.puml`](03_aportes_pagos_qr.puml) | Que "pagué" signifique "el banco lo confirmó" |
| [04 — Entregas de Fondo](04_entregas_fondo.md) | [`.puml`](04_entregas_fondo.puml) | Que la bolsa llegue completa, a la persona correcta, una sola vez |
| [05 — Notificaciones y Comunicaciones](05_notificaciones.md) | [`.puml`](05_notificaciones.puml) | WhatsApp como canal real de cobro, sin spam ni doble aviso |
| [06 — Transparencia y Reputación](06_transparencia_reputacion.md) | [`.puml`](06_transparencia_reputacion.puml) | Que nadie tenga que "creerle" al organizador |
| [07 — Organizador y Automatización](07_organizador_automatizacion.md) | [`.puml`](07_organizador_automatizacion.puml) | Administrar es un rol, no un negocio: el organizador no cobra ni custodia |
| [08 — Garantía, Incumplimiento, Cobranza y Sanciones](08_garantia_incumplimiento.md) | [`.puml`](08_garantia_incumplimiento.puml) | El grupo no se detiene, pero la deuda no se perdona sola |
| [09 — Auditoría, Reportes y Cumplimiento](09_auditoria_reportes.md) | [`.puml`](09_auditoria_reportes.puml) | Poder demostrar todo lo anterior ante un reclamo o un regulador |
| [10 — Billetera, Custodia y Dinero Electrónico](10_billetera_custodia.md) | [`.puml`](10_billetera_custodia.puml) | El saldo no se guarda: se deriva, y todos los días cuadra contra el banco |
| [11 — Tarifas, Comisiones, Impuestos y Facturación](11_tarifas_comisiones.md) | [`.puml`](11_tarifas_comisiones.puml) | La política de cobro es dato, no código: se cambia con un seeder |
| [12 — Cumplimiento Regulatorio y Consumidor Financiero](12_cumplimiento_asfi.md) | [`.puml`](12_cumplimiento_asfi.puml) | Que una inspección se responda con consultas, no armando carpetas |
| [13 — Contabilidad Financiera y ERP](13_contabilidad_erp.md) | [`.puml`](13_contabilidad_erp.puml) | Que cerrar un mes no dependa de un Excel armado a mano |
| [14 — Publicidad y Campañas](14_publicidad_campanas.md) | [`.puml`](14_publicidad_campanas.puml) | Que un partner se anuncie dentro de la app sin inventar un segundo cobro |
| [15 — Inversiones](15_inversiones.md) | [`.puml`](15_inversiones.puml) | Invertir saldo propio y libre sin prometer una rentabilidad que no se controla |

## Cinco tesis que explican el 80% del modelo

Antes de entrar a las fichas, conviene tener claras las ideas que hacen que este
modelo tenga tantas entidades. Casi toda entidad "de más" existe por una de estas
cinco razones:

**1. El pasanaku falla por desconfianza, no por falta de plata.**
La gente no abandona un pasanaku porque no pueda pagar: lo abandona porque
sospecha que el orden de cobro fue arreglado, que el organizador se quedó con
algo, o que el moroso no va a tener consecuencias. Por eso hay entidades que un
sistema "normal" no tendría: `SorteoTurnos` con commit-reveal, `Acuerdo` con
votación ponderada, `BloqueTransparencia` con cadena de hashes, `Sancion` con
derecho a descargo. Todas existen para convertir una promesa verbal en una prueba
verificable.

**2. Se custodia dinero de terceros, no dinero propio.**
Eso obliga a que el dinero nunca se represente con un solo campo `monto_pagado`.
El circuito es `ObligacionAporte → OrdenCobro → IntentoPago → Pago →
Conciliacion → AsientoContable`, cada paso idempotente y auditable por separado.
Parece exagerado hasta el día en que la pasarela reenvía un webhook y acredita
dos veces el mismo aporte, o hasta que alguien pide el detalle de por qué recibió
Bs 200 menos de lo que esperaba.

**3. El incumplimiento es un expediente, no una bandera.**
Un booleano `es_moroso` no responde: cuánto debe, desde cuándo, qué se hizo para
cobrarle, quién autorizó cubrirlo con el fondo, qué dijo él en su descargo, qué
sanción se le aplicó y si la apeló. Todo el módulo 8 existe para responder eso.
La reputación (módulo 6) es *una consecuencia* de ese expediente, no el registro
mismo.

**4. Si hay saldo, hay custodia; y la custodia se demuestra o no existe.**
Desde que la plataforma guarda dinero, el saldo deja de poder ser un número. Es el
resultado de un libro *append-only* con partida doble interna (M10), y la suma de
todos los saldos tiene que cuadrar todos los días contra el dinero real depositado
en la cuenta de custodia. De ahí salen `transaccion_billetera`,
`movimiento_billetera`, `retencion_saldo`, `cuenta_custodia`,
`conciliacion_custodia` y `saldo_diario_billetera`: seis entidades para responder
"¿está la plata?" con una consulta en vez de con una afirmación.

**5. El precio es una decisión de negocio, no una constante del código.**
La plataforma cobra una comisión por cada juego, y esa política va a cambiar. Por
eso el módulo 11 modela un motor de tarifas parametrizable: el hecho generador, la
base, la fórmula, quién paga, por qué vía y cuándo son columnas con vigencia, y
cada comisión cobrada apunta a la versión del tarifario que la calculó. Cambiar la
política es un seeder; explicar un cobro de hace dos años es una consulta.

## Una decisión que atraviesa todo: administrar no se cobra (RN-18)

El organizador **no percibe comisión** y **no custodia el dinero del grupo**. Es un
participante más: aporta y cobra su turno como cualquiera, y su rol solo le agrega
funciones administrativas.

Eso no está implementado como un permiso apagado ni como una bandera: **el modelo
simplemente no tiene dónde representar un ingreso del organizador**. No existen
`EsquemaComision`, `LiquidacionComision` ni `PagoComision` a su favor, ni cuenta de
cobro, ni tipo de deducción o de asiento que le acredite algo. La ausencia de la
estructura es una garantía más fuerte que cualquier validación, porque no hay nada
que desactivar.

Consecuencias que se ven en varios módulos: la bolsa de la entrega (M4) nunca se
descuenta a favor de quien administra, no hay egresos hacia el organizador en la
contabilidad (M3), y las sanciones al organizador (M7) son todas de habilitación
—advertencia, reducción de límite, suspensión, inhabilitación— porque no hay pago
que retener. El detalle está en la
[ficha del módulo 7](07_organizador_automatizacion.md).

> [!important] Lo que sí cobra la plataforma
> El módulo 11 introduce la comisión **del servicio**, que pertenece a la empresa
> que lo presta —custodia, cobro, conciliación, notificación, garantía, soporte— y
> se cobra con tarifario público, versionado, con preaviso de cambios y factura.
> No contradice RN-18 y conviene no confundirlas:
>
> | | Comisión del organizador | Comisión de la plataforma |
> | --- | --- | --- |
> | ¿Existe? | **No**, y no hay dónde representarla | Sí, módulo 11 |
> | Beneficiario | — | la empresa; nunca una persona del grupo |
> | ¿Depende de decisiones discrecionales? | — | No: se calcula sobre un hecho objetivo |
> | Trazabilidad | — | concepto + tarifario versión N + factura |
>
> Ninguna tabla del módulo 11 tiene una clave foránea hacia `organizador`.

## Convenciones

- Se usa `NombreClase` para el modelo orientado a objetos y `nombre_tabla` para
  el relacional. Cuando ambos existen, la ficha los cita juntos.
- Las entidades marcadas **append-only** no admiten `UPDATE` ni `DELETE`: se
  corrigen registrando el movimiento inverso. A esas se les revoca el privilegio
  a nivel de rol de base de datos, no solo por convención de código.
- "M1"…"M12" refieren a los módulos de la tabla de arriba.
- Los montos van siempre acompañados de `moneda` ISO-4217: un pasanaku en dólares
  y uno en bolivianos conviven en la misma tabla.
