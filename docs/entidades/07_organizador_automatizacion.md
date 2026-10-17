# Módulo 7 — Organizador y Automatización

> **Pregunta de negocio que responde este módulo:**
> *¿Quién administra el grupo, con qué requisitos, y qué pasa cuando lo hace mal?
> ¿Y qué pasa si no hay nadie que lo administre?*

En el pasanaku tradicional siempre hay alguien que organiza: recuerda las fechas,
persigue a los que se atrasan, junta la plata y la entrega. El problema del modelo
tradicional es que **ese mismo organizador es quien custodia la plata**, y ahí es
donde se pierden los pasanakus: se la presta, la usa, o simplemente desaparece.

Dos invariantes gobiernan este módulo:

> **RN-18 — El organizador no custodia el dinero del grupo y no cobra por
> administrarlo.**
>
> Es **un participante más**: aporta y cobra su turno igual que todos. Su rol solo
> le agrega funciones administrativas, no un ingreso.

La segunda parte es una decisión de producto deliberada y tiene una consecuencia
directa sobre el modelo: **no existe ninguna entidad que represente un ingreso del
organizador**. No hay esquema de comisión suya, no hay devengo a su favor, no hay
liquidación ni pago hacia él. La ausencia de la estructura es lo que hace imposible
el cobro — una garantía mucho más fuerte que un permiso o una bandera que alguien
pueda cambiar.

> [!important] La plataforma sí cobra; el organizador no
> Desde la incorporación de la billetera (M10), **la plataforma cobra una comisión
> por el servicio** —custodia, cobro, conciliación, notificación, garantía y
> soporte— modelada en el módulo 11. Eso no cambia RN-18 en nada, y conviene ser
> preciso sobre por qué:
>
> - El ingreso pertenece a la **empresa que presta el servicio**, con tarifario
>   público, versionado, con preaviso de cambios y factura. No a la persona que
>   administra el grupo.
> - El organizador **no percibe ninguna porción** de esa comisión: no existe
>   concepto de tarifa con beneficiario `ORGANIZADOR`, ni cuenta por pagar hacia
>   él, ni deducción a su favor.
> - El conflicto de interés que RN-18 evita sigue evitado: quien decide si se
>   exonera un aporte o se expulsa a alguien **no gana más ni menos** por esas
>   decisiones. La comisión de plataforma no depende de ninguna decisión
>   discrecional del organizador.
>
> En una frase: *administrar sigue sin ser un negocio; prestar el servicio sí lo
> es, y se cobra con reglas públicas.*

Qué se gana con eso, en términos de negocio:

- **Desaparece el conflicto de interés más incómodo.** Quien decide si se exonera
  un aporte, si se cambia una fecha o si se expulsa a alguien no tiene un ingreso
  atado a esas decisiones.
- **Desaparece la discusión de "¿quién paga la comisión?"**, que es la fuente
  número uno de conflicto entre organizador y participantes.
- **El producto es más simple y más barato.** El aporte del participante va
  íntegro a la bolsa y al fondo de garantía.
- **El organizador digital deja de ser la opción barata y pasa a ser el caso
  normal**: si administrar no genera ingreso, automatizarlo no le quita nada a
  nadie.

Lo que el módulo sí conserva: **habilitación** (quién puede administrar plata
ajena), **límites** (cuánto puede administrar), **desempeño** (lo está haciendo
bien) y **sanciones** (qué pasa si no).

---

## Paquete: Habilitación del Organizador

### `Organizador` / `organizador` — Raíz de agregado

**Qué es.** Un usuario habilitado para administrar grupos de terceros, con su
nivel, sus límites y sus indicadores de cartera.

**Para qué sirve (negocio).** Convierte "el que organiza" en **un rol con
requisitos, límites y consecuencias** — aunque no tenga retribución. Los campos
clave:

- `limiteGruposSimultaneos` y `limiteMontoAdministrado` son **control de riesgo
  puro**. Un organizador nuevo puede llevar dos grupos chicos; uno con historial
  probado, quince grupos grandes. Sin límites, alguien puede acumular cincuenta
  grupos y su fracaso arrastra a cientos de personas a la vez.
- `nivel` (aprendiz → estándar → sénior → maestro) es la escalera de progresión.
  Sin comisión de por medio, el incentivo deja de ser económico y pasa a ser
  reputacional: administrar bien habilita a administrar más, y eso se refleja en
  el perfil público (M6).
- `indiceMorosidadCartera` es **el indicador que más importa de todo el módulo**.
  Un organizador cuyos grupos tienen 30% de mora está haciendo mal el trabajo. Y
  ahora es el único indicador que importa, porque ya no hay un volumen de
  comisiones que compense un mal desempeño.
- `calificacionPromedio` viene de las reseñas (M6) y captura la dimensión de trato.

**Por qué debe existir separado de `Usuario`.** Porque ser organizador es un rol
con estado propio (`POSTULADO → EN_EVALUACION → CAPACITACION_PENDIENTE →
HABILITADO → LIMITADO → SUSPENDIDO → DESHABILITADO`), con métricas propias y con
un proceso de habilitación que la cuenta de usuario no tiene. Y porque el mismo
usuario es organizador en unos grupos y participante en otros.

**Qué NO lleva, y por qué importa.** No tiene `cuentaCobroId` ni
`comisionAcumulada()`: **no hay nada que pagarle**, así que no hay a dónde pagarle.
Si además participa en el grupo que administra, lo hace como participante normal
(M2), con sus propias obligaciones (M3) y su turno (M4) — por la vía de siempre,
sin trato especial.

**A nivel de sistema.** `usuario_id` `UNIQUE`. `grupos_activos` y
`monto_administrado_actual` son contadores mantenidos por trigger: hacen cumplir
los límites sin recorrer toda la cartera en cada validación.

---

### `SolicitudOrganizador` / `solicitud_organizador`

**Qué es.** La postulación de un usuario para ser habilitado como organizador.

**Para qué sirve (negocio).** Es la puerta de entrada, y tiene que ser una puerta
real: **quien administra plata ajena tiene que pasar un filtro**, cobre o no
cobre. La solicitud guarda motivación, experiencia declarada, referencias, el KYC
reforzado y `puntajeReputacionAlSolicitar`: con qué historial se lo aceptó.

Sin comisión de por medio, `motivacion` gana peso en la evaluación: hay que
entender por qué alguien quiere asumir el trabajo sin retribución. Los motivos
legítimos abundan (organiza el pasanaku de su oficina, de su familia, de su
gremio), pero es exactamente la pregunta que hay que hacerle a quien no tiene
vínculo previo con el grupo que quiere administrar.

`motivoRechazo` y `revisadaPor` cierran el proceso: el rechazo tiene autor y
fundamento, y el postulante sabe qué le faltó para volver a intentar.

**Por qué debe existir.** Sin solicitud formal, ser organizador sería marcar una
casilla, y la plataforma estaría entregando la administración de dinero ajeno sin
evaluación ni registro de quién autorizó qué.

---

### `RequisitoHabilitacion` / `requisito_habilitacion` — Política configurable

**Qué es.** El catálogo de requisitos por nivel: KYC, reputación, antigüedad,
capacitación, garantía económica.

**Para qué sirve (negocio).** Permite endurecer o relajar la barrera de entrada
según la etapa del negocio y según lo que la experiencia enseñe. Si aparece un
patrón de organizadores nuevos que fallan en el primer grupo, se sube la
antigüedad mínima sin desplegar código.

`GARANTIA_ECONOMICA` como tipo es relevante: para niveles altos se puede exigir un
depósito que responda si su gestión genera pérdidas. Sin comisión que retener, es
**el único mecanismo económico que alinea el interés del organizador con el del
grupo**.

**Por qué debe existir.** Con requisitos hardcodeados, cada ajuste es un release y
no queda registro de qué se le exigió a un organizador habilitado hace un año.

---

### `CapacitacionOrganizador` / `capacitacion_organizador`

**Qué es.** Los módulos de formación que el organizador completó, con su nota y
vigencia.

**Para qué sirve (negocio).** La mayoría de los problemas de un organizador no son
mala fe: son desconocimiento. No sabe cómo manejar a alguien que se atrasa, no
entiende qué puede y qué no puede decidir solo, no sabe cómo se calcula una
liquidación. La capacitación reduce incidentes y le da a la plataforma un
argumento sólido cuando tiene que sancionar: *se te capacitó en esto, aprobaste con
esta nota, y aun así lo hiciste mal.*

Un contenido se vuelve obligatorio en este modelo: **que administrar no se cobra**,
y que pedir dinero por fuera de la plataforma es causal de inhabilitación.

`vigenteHasta` obliga a recertificar cuando cambian las reglas.

**Por qué debe existir.** Sin registro de capacitación no se puede exigir formación
como requisito ni distinguir el error de buena fe del incumplimiento consciente.

---

### `ContratoOrganizador` / `contrato_organizador`

**Qué es.** El contrato firmado entre el organizador y la plataforma, versionado y
sellado con hash.

**Para qué sirve (negocio).** Es el documento que hace exigibles las obligaciones
del organizador y que fundamenta las `causalesRescision`. Cuando hay que
deshabilitar a alguien, la pregunta legal es "¿en base a qué?", y la respuesta es
este contrato, en la versión que firmó, con su hash y su token de firma.

**Con la comisión eliminada, este contrato carga el peso de la regla.** Es donde
queda escrito, firmado y fechado que el organizador (a) no custodia fondos y (b)
**no percibe contraprestación por administrar, ni dentro ni fuera de la
plataforma**. Si alguien cobra "por debajo" a los participantes, está violando el
contrato de forma documentada, y eso es lo que sostiene la inhabilitación.

**Por qué debe existir.** Sin contrato firmado y versionado, la relación con el
organizador es informal, y sancionarlo o darlo de baja queda sin fundamento.

---

## Paquete: Desempeño y Sanciones al Organizador

### `EvaluacionDesempeno` / `evaluacion_desempeno`

**Qué es.** La evaluación periódica del organizador sobre seis indicadores
objetivos.

**Para qué sirve (negocio).** Implementa RN-22 y responde: *¿este organizador está
haciendo bien su trabajo?* Con datos, no con impresiones:

- `indiceMorosidadCartera`: sus grupos, ¿pagan a tiempo?
- `tasaFinalizacionGrupos`: sus grupos, ¿terminan o se caen?
- `satisfaccionParticipantes`: la gente que administró, ¿lo recomendaría?
- `tiempoRespuestaPromedioHoras`: cuando alguien tiene un problema, ¿contesta?
- `incidenciasAbiertas` y `coberturasConsumidas`: ¿cuánto le cuesta al sistema?

`nivelSugerido` + `accionRecomendada` cierran el ciclo: la evaluación **tiene
consecuencias automáticas**, hacia arriba (ascenso de nivel, más límite) o hacia
abajo (reducción de límite, suspensión).

Esta entidad gana importancia con la comisión eliminada. En un modelo con
comisión, un organizador mediocre igual tiene el incentivo económico de seguir
administrando. Sin ella, el único incentivo es el reconocimiento — **y este es el
registro que lo produce**. Sin evaluación, administrar bien y administrar mal se
ven exactamente igual, y no queda ningún motivo para hacerlo bien.

**Por qué debe existir.** Sin evaluación sistemática, un organizador que administra
mal sigue acumulando grupos hasta que el daño es grande y visible. Y uno que
administra bien no tiene forma de demostrarlo para crecer.

**A nivel de sistema.** `UNIQUE (organizador_id, periodo_evaluado)`.

---

### `MetricaOrganizador` / `metrica_organizador`

**Qué es.** El detalle de cada indicador dentro de la evaluación: valor, meta, si
cumple y cuánto pesa.

**Para qué sirve (negocio).** Hace la evaluación **explicable y apelable**. El
organizador no recibe "puntaje 62, nivel reducido"; recibe el desglose: morosidad
18% contra meta de 10% (no cumple, peso 0,35); tiempo de respuesta 4 h contra meta
de 6 h (cumple, peso 0,15). Con eso sabe qué corregir, y si apela, sabe qué
discutir.

**Por qué debe existir.** Una evaluación sin desglose es una calificación
arbitraria, y sancionar en base a ella es indefendible.

---

### `SancionOrganizador` / `sancion_organizador`

**Qué es.** La consecuencia por mal desempeño: advertencia, reducción de límite,
suspensión o inhabilitación.

**Para qué sirve (negocio).** La escalera es deliberadamente gradual. Se empieza
por advertencia, no por inhabilitación, porque **inhabilitar a un organizador deja
a todos sus grupos sin administrador** — el remedio puede ser peor que la
enfermedad (aunque el organizador digital, en este modelo, es una red de contención
razonable).

`REDUCCION_LIMITE` es la sanción más útil del catálogo: no le quita los grupos que
tiene —lo que perjudicaría a participantes que no hicieron nada—, pero le impide
tomar más hasta que mejore.

**Nota sobre el catálogo:** ya no existe `RETENCION_COMISION`. Sin comisión no hay
qué retener, y por eso **todas las sanciones de este módulo son de habilitación,
no económicas**. Es una limitación real del modelo y conviene tenerla presente: la
única palanca económica disponible es la `GARANTIA_ECONOMICA` exigida como
requisito de habilitación.

**Por qué debe existir.** Sin sanciones graduadas, la única alternativa es tolerar
o expulsar, y ninguna de las dos sirve para la mayoría de los casos reales.

---

### `ApelacionSancion` / `apelacion_sancion_org`

**Qué es.** El recurso del organizador contra una sanción.

**Para qué sirve (negocio).** Debido proceso. Las evaluaciones automáticas se
equivocan: un grupo puede tener alta morosidad por razones ajenas al organizador
(una crisis local, un participante que resultó ser un estafador). Sin apelación, un
buen organizador puede ser sancionado por un dato que no controlaba.

Esto pesa más en un modelo sin retribución: **si administrar no da ingreso y encima
expone a sanciones injustas, nadie va a querer hacerlo.** La apelación es lo que
mantiene el rol atractivo para la gente que uno quiere que lo asuma.

**Por qué debe existir.** Una sanción automática sin recurso es arbitraria, y
disuade a la gente buena de asumir el rol.

---

## Paquete: Organizador Digital (automatización, RF-20)

> **Por qué existe este paquete.** Porque el organizador humano es el mayor riesgo
> operativo del modelo: depende de que una persona esté disponible y cumpla. Si el
> sistema puede hacer el trabajo —generar cobros, mandar recordatorios, aplicar
> mora, liquidar el período, ejecutar la entrega, escalar la cobranza—, el grupo
> puede ser **autogestionado** y no depender de nadie.
>
> Sin comisión, la comparación entre organizador humano y digital deja de tener un
> componente de costo: **ambos son gratis**. La elección pasa a ser solo sobre
> criterio y disponibilidad, que es una decisión más sana.

### `OrganizadorDigital` — Servicio de dominio

**Qué es.** El agente automático que ejecuta las funciones del organizador en un
grupo.

**Para qué sirve (negocio).** `nivelAutonomia` es el campo de negocio importante,
con tres modos:

- `SOLO_SUGIERE`: el sistema propone y un humano decide. Para grupos nuevos o
  organizadores que quieren mantener el control.
- `EJECUTA_CON_AVISO`: actúa y notifica. El punto medio razonable.
- `AUTONOMO`: actúa solo. Para grupos autogestionados maduros.

Poder graduar la autonomía es lo que permite adoptar la automatización sin pedirle
a la gente un acto de fe. Se empieza sugiriendo, se gana confianza, se sube el
nivel.

**Por qué debe existir.** Sin organizador digital, todo grupo necesita una persona
disponible y confiable — y ese es el cuello de botella que limita el crecimiento y
concentra el riesgo.

---

### `ReglaAutomatizacion` / `regla_automatizacion` — Política configurable

**Qué es.** Qué se automatiza, cuándo se dispara, bajo qué condición y qué acción
ejecuta.

**Para qué sirve (negocio).** Es el cerebro configurable. Las seis acciones
tipificadas son exactamente el trabajo del organizador: generar cobros, enviar
recordatorios, aplicar mora, liquidar el período, ejecutar la entrega, escalar la
cobranza.

`requiereConfirmacionHumana` es el freno de seguridad **por acción, no por
sistema**. Generar recordatorios puede ser totalmente automático; ejecutar una
entrega de Bs 20.000 probablemente no. La granularidad permite automatizar lo
rutinario sin automatizar lo irreversible.

**Por qué debe existir.** Con automatización hardcodeada no se puede ajustar qué se
automatiza por tipo de grupo, ni desactivar una regla que está causando problemas
sin apagar todo el sistema.

---

### `TareaAutomatizada` / `tarea_automatizada`

**Qué es.** Una instancia concreta de trabajo programada por una regla.

**Para qué sirve (negocio).** Hace visible lo que el sistema va a hacer **antes de
hacerlo**. `tareasPendientes()` es la agenda del organizador digital, y en modo
`SOLO_SUGIERE` es literalmente la lista de sugerencias que el humano revisa.

`REQUIERE_APROBACION` como estado materializa el freno: la tarea está lista, sabe
qué hacer, y espera el visto bueno.

**Por qué debe existir.** `claveIdempotencia` `UNIQUE` es crítica: **impide que un
reintento del planificador genere cobros o recordatorios duplicados**. Un cron que
se ejecuta dos veces por un problema de infraestructura no puede cobrarle dos veces
a nadie ni mandar el recordatorio dos veces.

---

### `EjecucionTarea` / `ejecucion_tarea`

**Qué es.** El registro de cada ejecución: cuándo, con qué resultado, cuántos
registros afectó.

**Para qué sirve (negocio).** Es la **rendición de cuentas de la automatización**.
Cuando un participante pregunta "¿por qué me aplicaron mora?", la respuesta es: la
tarea de aplicación de mora del 6 de marzo a las 00:15 procesó 47 obligaciones
vencidas, y la tuya era una. Con `detalle` en JSON, se puede reconstruir exactamente
qué hizo.

`resultado = PARCIAL` es realista: una tarea puede procesar 40 de 47 registros y
fallar en 7. Sin ese estado, o se marca éxito (y se ocultan los 7 fallos) o se marca
error (y se pierden los 40 éxitos).

**Por qué debe existir.** **Un sistema que actúa solo sobre el dinero de la gente
tiene que poder explicar cada acción que tomó.** Sin registro de ejecución, la
automatización es una caja negra que mueve plata, y eso es inaceptable.

**A nivel de sistema.** Toda ejecución va también a la bitácora del módulo 9, con
`origen = ORGANIZADOR_DIGITAL` — lo que permite distinguir en la auditoría lo que
hizo el sistema de lo que hizo una persona.

---

## Qué se eliminó del modelo al quitar la comisión del organizador

Estas entidades existían en la versión anterior del módulo y **ya no forman parte
del modelo**. Se documentan acá para que quede constancia de qué se quitó y qué
consecuencia tuvo. Ninguna reapareció con el módulo 11: las estructuras de
comisión que ahora existen pertenecen a la plataforma y viven fuera de este
módulo, sin ninguna FK hacia `organizador`.

| Entidad eliminada | Qué hacía | Efecto de quitarla |
| --- | --- | --- |
| `EsquemaComision` | Cómo, cuánto y quién le pagaba al organizador | Ya no hay que negociar ni aceptar comisión: desaparece la discusión de "¿quién la paga?" |
| `AceptacionComision` | Aceptación firmada de cada participante (RN-19) | Innecesaria: no hay nada que aceptar |
| `TramoComision` | Escalones de comisión regresiva | Innecesaria |
| `TopeRegulatorio` | Techo máximo de comisión (RN-20) | Innecesario: el techo es cero por construcción |
| `DevengoComision` | Comisión ganada por hito | Ya no hay ingreso que devengar |
| `LiquidacionComision` | Consolidación mensual con retenciones | Ya no hay liquidación que aprobar ni pagar |
| `RetencionImpuesto` | Retenciones impositivas sobre la comisión | **La plataforma deja de ser agente de retención por este concepto**, lo que simplifica el cumplimiento tributario |
| `DeduccionLiquidacion` | Descuentos sobre la liquidación (sanciones, anticipos) | Se pierde la palanca de sanción económica: ver `SancionOrganizador` |
| `PagoComision` | Transferencia del neto al organizador | **No existe ningún egreso hacia el organizador** |
| `DisputaComision` | Reclamos sobre la liquidación | Innecesaria: desaparece toda una clase de conflicto |

Y los cambios que esto propagó a otros módulos:

| Módulo | Qué se quitó |
| --- | --- |
| M2 | `acuerdo.referencia_afectada_id` ya no puede apuntar a `esquema_comision.id` |
| M3 | `TipoObligacion.COMISION_ORGANIZADOR` |
| M4 | `TipoDeduccion.COMISION_ORGANIZADOR`: **la bolsa ya no se descuenta para pagarle a quien administra** |
| M5 | Evento notificable `LIQUIDACION_COMISION` (del organizador) |
| M9 | Reporte `LIQUIDACION_COMISIONES` (del organizador) |

Y lo que sí existe hoy, para que no se confunda con lo anterior:

| Módulo | Qué existe, y a favor de quién |
| --- | --- |
| M3 | `TipoObligacion.COMISION_PLATAFORMA` — solo si el tarifario prorratea la comisión del **servicio** |
| M4 | `TipoDeduccion.COMISION_PLATAFORMA` — deducción a favor de la **empresa**, con concepto y tarifario trazables |
| M11 | `tarifario`, `concepto_tarifa`, `devengo_comision`, `factura_electronica` — ingresos de la **plataforma** |
| M7 | nada: **ninguna tabla de M11 tiene FK hacia `organizador`** |

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `Organizador` con límites | Alguien acumula cincuenta grupos y su fracaso arrastra a cientos de personas. |
| `SolicitudOrganizador` + `Requisito` | Se entrega la administración de plata ajena sin filtro ni registro. |
| `ContratoOrganizador` | Deshabilitar a un organizador queda sin fundamento legal, y la regla de no cobrar no queda firmada por nadie. |
| `EvaluacionDesempeno` + `Metrica` | Administrar bien y administrar mal se ven igual: sin comisión, el reconocimiento es el único incentivo que queda. |
| `SancionOrganizador` graduada | La única alternativa es tolerar o expulsar; ninguna sirve. |
| `ApelacionSancion` | Un error del sistema es definitivo, y nadie quiere asumir un rol sin paga y con sanciones injustas. |
| `OrganizadorDigital` + reglas | Todo grupo depende de que una persona esté disponible y sea confiable. |
| `TareaAutomatizada` (idempotente) | Un cron ejecutado dos veces cobra dos veces. |
| `EjecucionTarea` | La automatización es una caja negra que mueve plata sin poder explicarse. |


---

## Tablas incorporadas después de la redacción original

> Fichas escritas para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Cada
> afirmación sale de la fuente que se cita al pie de la ficha: no agregan reglas, las explican.

### `DecisionHabilitacion` / `decision_habilitacion`

**Qué es.** Cada resolución sobre una postulación a organizador: aprobar, rechazar y, en revisión, confirmar o revocar.

**Para qué sirve (negocio).** Que habilitar a alguien para administrar grupos de plata ajena sea una decisión registrada, con autor y motivo, y que una revocación posterior quede tan documentada como la aprobación.

**Por qué debe existir.** Sin ella, la habilitación sería un estado en la solicitud: se perdería quién aprobó, con qué requisitos cumplidos y por qué se revocó después.

**A nivel de sistema.** Append-only. `fase` ∈ RESOLUCION/REVISION y `decision` ∈ APROBAR/RECHAZAR/CONFIRMAR/REVOCAR. `revision` crece por solicitud. El motivo no puede ser vacío. `evidencia_requisitos` conserva qué se verificó. La escritura pasa por un solo lugar (`BitacoraDeHabilitacion`), así ninguna resolución queda fuera del historial.

*Fuente: CU-90; `docs/Restricciones.md`; `servicios/organizador/.../BitacoraDeHabilitacion.java`.*
