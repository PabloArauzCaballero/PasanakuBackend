# Módulo 8 — Garantía, Incumplimiento, Cobranza y Sanciones

> **Pregunta de negocio que responde este módulo:**
> *Alguien no pagó. ¿El grupo se detiene? ¿Quién pone esa plata? ¿Cómo se la
> cobro? ¿Qué le pasa al que no pagó, y cómo me aseguro de que eso sea justo y
> defendible?*

Este es el módulo más grande del sistema (34 clases, 33 tablas) y no es
casualidad: **el incumplimiento es la razón por la que fracasan los pasanakus**.
No la falta de tecnología, no la falta de organización: alguien deja de pagar, el
que le tocaba cobrar recibe menos de lo prometido, la confianza se rompe y el grupo
se desarma.

La tesis del módulo:

> **La reputación no alcanza.**
> Un puntaje resume, pero no permite cobrar, ni sancionar con debido proceso, ni
> probar nada ante un reclamo. Por eso el incumplimiento se modela como un
> **expediente** propio, con evidencia, gestión de cobranza, deuda exigible,
> garantía que la cubre, aval que responde, sanción apelable y cierre trazable.
> La reputación (M6) es apenas **una** de las consecuencias.

La cadena completa que el módulo implementa:

```
Detección → RegistroIncumplimiento (expediente)
  → GestionCobranza (escalonada, con promesas de pago)
  → Cobertura desde el FondoGarantia (el grupo no se detiene)
  → DeudaParticipante exigible + subrogación a favor del fondo
  → Recuperación / Ejecución de aval / Castigo
  → Sanción con derecho a apelación
  → Efectos en reputación (M6) y restricciones (M1)
```

---

## Paquete: Fondo de Garantía

### `FondoGarantia` / `fondo_garantia` — Raíz de agregado

**Qué es.** El colchón económico que cubre los aportes impagos para que el grupo
no se detenga.

**Para qué sirve (negocio).** **Es la innovación central del modelo digital frente
al pasanaku tradicional.** En el pasanaku de siempre, si uno no pone, el que cobra
recibe menos — y esa persona, que hizo todo bien, paga el error de otro. Es
profundamente injusto y es lo que hace que la gente abandone.

Con fondo de garantía, el sistema pone la diferencia: **el beneficiario cobra
completo, y el que no pagó ahora le debe al fondo, no al grupo.** El conflicto deja
de ser entre vecinos y pasa a ser entre el moroso y un mecanismo institucional. Eso
solo ya cambia la dinámica social del grupo.

El `ambito` define dos modelos de negocio distintos:
- `POR_GRUPO`: cada grupo tiene su propio fondo, alimentado con un porcentaje de
  cada aporte. Lo que sobra al final se devuelve.
- `MUTUAL_PLATAFORMA`: un fondo común entre todos los grupos. Diversifica mejor
  (un grupo con mala suerte no se queda sin cobertura) pero mezcla riesgos entre
  grupos que no se conocen.

La separación `saldoDisponible` / `saldoComprometido` es contabilidad prudente: hay
plata reservada para coberturas ya aprobadas pero no aplicadas. Sin esa distinción,
el fondo aparentaría tener más de lo que realmente puede comprometer.

`indiceSuficiencia()` y `proyectarAgotamiento()` son las funciones de alerta
temprana: **un fondo que se va a agotar en dos períodos es un grupo que va a
colapsar en tres**, y hay que saberlo antes.

**Por qué debe existir.** Sin fondo, la única respuesta al impago es que el
beneficiario cobre menos. Con fondo, el grupo sigue funcionando y la deuda se
persigue por separado.

**A nivel de sistema.** `CHECK saldo_disponible >= 0`: el fondo no puede quedar en
rojo. `version` para bloqueo optimista: dos coberturas simultáneas no pueden
comprometer la misma plata.

---

### `PoliticaCobertura` / `politica_cobertura` — Política configurable

**Qué es.** Las reglas del fondo: cuánto se aporta, cuándo se activa la cobertura,
cuánto cubre y con qué límites.

**Para qué sirve (negocio).** Cada parámetro protege contra un abuso concreto:

- `porcentajeConstitucion`: cuánto de cada aporte alimenta el fondo. Sube el costo
  del pasanaku, así que es un equilibrio entre seguridad y precio.
- `diasMoraParaActivar`: **no se cubre al primer día de atraso.** Si se cubriera de
  inmediato, el fondo pagaría por gente que simplemente se olvidó y va a pagar
  mañana, y se agotaría en meses.
- `topeCoberturaPorParticipante` y `maxCoberturasPorParticipante`: **evitan el
  parásito**. Sin límite, alguien podría no pagar nunca y dejar que el fondo cubra
  siempre — convirtiendo el seguro colectivo en su financiamiento personal.
- `topeCoberturaPorPeriodo`: protege contra el colapso masivo. Si en un período
  fallan seis de doce, el fondo no puede vaciarse cubriéndolos a todos; ahí
  corresponde un plan de contingencia, no cobertura.
- `exigeAvalPrevio`: solo se cubre a quien tiene quién responda.
- `requiereAprobacionManualDesde`: montos grandes pasan por revisión humana.
- `plazoRecuperacionDias` y `tasaRecargoRecuperacion`: **el fondo no es un regalo**.
  Es un préstamo forzoso con plazo y con costo.

**Por qué debe existir.** Un fondo sin política es un fondo que se agota. Y
configurarla por grupo permite que un pasanaku familiar sea generoso y uno con
desconocidos sea estricto.

---

### `MovimientoFondo` / `movimiento_fondo` — **append-only**

**Qué es.** Cada entrada o salida del fondo, con el saldo resultante.

**Para qué sirve (negocio).** Es el extracto del fondo, y es lo que hace que el
fondo sea **auditable por los participantes**, que son sus dueños. Los nueve tipos
cuentan toda la historia: constitución, aportes, coberturas aplicadas,
recuperaciones, ejecuciones de aval, devoluciones, castigos, ajustes,
rendimientos.

`saldoResultante` en cada movimiento permite verificar la secuencia completa sin
recalcular: si un saldo resultante no coincide con el anterior más el monto, hubo
manipulación.

**Por qué debe existir.** Es plata de los participantes. Si el fondo fuera solo un
campo `saldo` que sube y baja, nadie podría verificar en qué se usó su aporte al
fondo — y eso reproduce exactamente la opacidad que la plataforma quiere eliminar.

**A nivel de sistema.** Append-only, con `UPDATE`/`DELETE` revocados a nivel de rol.
`asiento_contable_id` → M3: cada movimiento del fondo tiene su contrapartida
contable.

---

### `DevolucionFondo` / `devolucion_fondo`

**Qué es.** El cálculo de cuánto le toca devolver a cada participante del fondo
sobrante al cerrar el grupo.

**Para qué sirve (negocio).** Es lo que hace **justo** el fondo. Si el grupo
terminó bien y sobró plata, esa plata **es de los participantes** y hay que
devolverla a prorrata de lo aportado.

`montoConsumido` es el ajuste de equidad: quien consumió cobertura (y no la
repuso) recibe menos. Sin esa resta, el que nunca falló financiaría al que falló
seguido, que es exactamente lo contrario del incentivo que se busca.

`RETENIDA` con `motivoRetencion` cubre al participante que todavía debe: no se le
devuelve mientras tenga deuda abierta; se compensa.

**Por qué debe existir.** Sin devolución explícita, el sobrante del fondo se
quedaría en la plataforma, lo que sería —correctamente— percibido como
apropiación.

---

## Paquete: Expediente de Incumplimiento

### `RegistroIncumplimiento` / `registro_incumplimiento` — Raíz de agregado

**Qué es.** El expediente completo de un hecho de incumplimiento.

**Para qué sirve (negocio).** **Esta es la entidad más importante del módulo y
posiblemente del sistema entero.** Su justificación es directa:

> Un booleano `es_moroso` no responde: **cuánto** debe, **desde cuándo**, **qué se
> hizo** para cobrarle, **quién autorizó** la cobertura, **qué dijo él** en su
> descargo, **qué sanción** se le aplicó y **si la apeló**.
> El expediente responde todo eso, y es la prueba ante un reclamo o una auditoría.

`codigoExpediente` (`INC-2026-000123`) es legible a propósito: se cita en
notificaciones, en reclamos y en comunicaciones formales. "Tu expediente
INC-2026-000123" es infinitamente más manejable que un UUID.

Los doce tipos de incumplimiento no son variaciones del mismo hecho. Vale la pena
notar los que un modelo ingenuo omitiría:
- `PAGO_RECHAZADO_O_REVERSADO`: pagó y después el pago se cayó. **Técnicamente
  pagó, económicamente no.**
- `COMPROBANTE_FALSO`: subió una imagen adulterada. Es fraude, no atraso.
- `BENEFICIARIO_NO_CONTINUA_APORTANDO`: **el peor caso del pasanaku.** Cobró su
  turno y dejó de aportar. Ya se llevó la bolsa entera y ahora le debe a todos los
  que todavía no cobraron. Es cualitativamente distinto de atrasarse antes de
  cobrar, y tratarlos igual sería absurdo.
- `INCUMPLIMIENTO_AVAL`: el que respondía por otro tampoco responde.

`severidad` × `esReincidencia` × `numeroReincidencia` son las tres variables que
alimentan la matriz de sanciones. **La cuarta vez no es como la primera**, y el
modelo lo sabe.

`origenDeteccion` importa para la calidad del proceso: no es lo mismo un
incumplimiento detectado automáticamente por vencimiento que uno reportado por un
participante (que puede tener conflicto con el señalado). El segundo exige más
verificación.

`fechaLimiteSubsanacion` es el reloj del debido proceso: la persona tiene un plazo
para arreglar antes de que escale.

**Por qué debe existir.** Sin expediente, la morosidad es un estado y no un
proceso. No se puede gestionar, ni probar, ni apelar, ni cerrar formalmente.

**A nivel de sistema.**
`UNIQUE` parcial sobre `obligacion_id WHERE estado NOT IN ('ANULADO_POR_ERROR')`:
un aporte impago no genera dos expedientes.
Índices operativos: `(estado, dias_mora_actuales DESC)` para la bandeja de
cobranza; `(usuario_id, detectado_en DESC)` para el historial portable.

---

### `EvidenciaIncumplimiento` / `evidencia_incumplimiento`

**Qué es.** Los respaldos del expediente: captura del estado, extracto,
comunicaciones, actas, logs del sistema, documentos.

**Para qué sirve (negocio).** **Sin evidencia, el expediente es una acusación.**
Cuando se sanciona a alguien, o se ejecuta un aval, o se lo incluye en la lista de
restricción, hay que poder mostrar en qué se basó.

`LOG_SISTEMA` como tipo es importante: la evidencia más fuerte suele ser
automática (el registro de que la obligación venció, de que se enviaron cinco
recordatorios entregados y leídos, de que no hubo pago). Es objetiva y no depende
de la palabra de nadie.

`hashArchivo` + `esInmutable` + `sellar()` garantizan que la evidencia no se
modificó después de aportada. Una evidencia editable no es evidencia.

**Por qué debe existir.** Es la diferencia entre un proceso defendible y uno
arbitrario. Y ante un reclamo legal, es lo único que vale.

---

### `HistorialEstadoIncumplimiento` / `historial_estado_incumplimiento` — **append-only**

**Qué es.** La línea de tiempo del expediente: cada transición, con motivo, monto y
autor.

**Para qué sirve (negocio).** Reconstruye la historia completa: detectado el 5,
notificado el 6, gestión abierta el 8, promesa de pago el 12, promesa incumplida el
15, cobertura aplicada el 16, sanción propuesta el 20, descargo presentado el 22,
sanción firme el 30.

Esa línea de tiempo es lo que se presenta cuando alguien dice "nunca me avisaron" o
"me sancionaron sin darme oportunidad".

`esAutomatico` distingue lo que hizo el sistema de lo que hizo una persona.
Importa: una escalada automática por vencimiento de plazo es distinta de una
escalada decidida por un gestor.

**Por qué debe existir.** El campo `estado` dice dónde está; el historial dice cómo
llegó y cuánto tardó cada paso. Es la base del debido proceso.

---

### `DescargoParticipante` / `descargo_participante`

**Qué es.** La versión del participante: su argumento y sus pruebas.

**Para qué sirve (negocio).** **Es el derecho a ser oído**, y no es un formalismo.
Los descargos legítimos son frecuentes y concretos: pagó pero la conciliación
falló; el sistema le mostró la fecha equivocada; hubo un acuerdo verbal con el
organizador; estuvo internado.

Sin este canal, la única forma de reclamar es por WhatsApp al soporte, sin
trazabilidad, sin plazo y sin criterio uniforme. Y sobre todo: **sin descargo, una
sanción es indefendible ante cualquier reclamo formal.**

`resueltoPor` + `resolucion` obligan a que alguien lo lea y responda con
fundamento, no que se archive.

**Por qué debe existir.** Sancionar sin oír es arbitrario. Y en un sistema que
maneja plata de la gente, la arbitrariedad es el riesgo reputacional más caro que
existe.

---

### `HistorialIncumplimientoUsuario` / `historial_incumplimiento_usuario`

**Qué es.** La proyección agregada del comportamiento de un usuario **a través de
todos los grupos**.

**Para qué sirve (negocio).** **Es la portabilidad del mal historial**, y es tan
importante como la portabilidad del bueno.

El problema que resuelve: alguien abandona un pasanaku debiendo tres cuotas, y a la
semana siguiente entra a otro grupo en la misma plataforma como si nada. En el
mundo presencial, la comunidad se entera; digitalmente, hay que construir esa
memoria.

`tasaRegularizacion` es el matiz que evita la condena permanente: no es lo mismo
alguien que se atrasó cinco veces y regularizó las cinco, que alguien que se atrasó
dos y nunca pagó. El primero es un mal pagador puntual; el segundo es un riesgo
real. Sin esta métrica, ambos se ven igual.

Se consulta al evaluar `SolicitudIngreso` (M2) y en el emparejamiento automático
(RF-19).

**Por qué debe existir.** Sin agregado cross-grupo, cada grupo tendría que
descubrir por su cuenta que el nuevo participante ya falló en otros tres.

**A nivel de sistema.** PK = `usuario_id`: una fila por usuario, recalculada por
evento.

---

### `ListaRestriccionInterna` / `lista_restriccion_interna`

**Qué es.** El registro de usuarios con restricciones activas por incumplimiento,
con su nivel.

**Para qué sirve (negocio).** Es el mecanismo ejecutivo del historial. Tres
niveles graduados:
- `OBSERVACION`: puede operar, pero se lo monitorea.
- `LIMITADO`: puede seguir en sus grupos actuales pero no entrar a nuevos.
- `VETADO`: no puede operar.

`montoAdeudado` es el dato que permite la salida: **la restricción se levanta
pagando.** No es una condena, es una consecuencia con remedio, y eso es
deliberado: una restricción sin salida elimina todo incentivo a regularizar.

`retiradoPor` + `motivoRetiro` documentan quién la levantó, que es la decisión más
sensible del proceso.

**Por qué debe existir.** Sin lista, el historial es información y no consecuencia.
Se materializa como `RestriccionUsuario` en M1, que es donde el resto del sistema
la consulta.

---

### `DetectorIncumplimiento` — Servicio de dominio

**Qué es.** El barredor automático que detecta incumplimientos.

**Para qué sirve (negocio).** Sus cuatro funciones cubren las formas en que un
incumplimiento aparece: `barrerVencimientos()` (lo obvio),
`detectarPagosReversados()` (pagó y se cayó el pago —**este es el que se olvida y
el que más descuadra**—), `detectarAbandonos()` (dejó de aportar sin avisar),
`evaluarPrescripcion()` (deudas tan viejas que ya no son exigibles).

**Por qué debe existir.** La detección manual depende de que alguien mire, y nadie
mira todos los días en todos los grupos. La detección automática es lo que hace que
la consecuencia sea consistente para todos.

---

### `ScoreRiesgoIncumplimiento` / `score_riesgo_incumplimiento`

**Qué es.** La probabilidad estimada de que un participante incumpla, con sus
factores.

**Para qué sirve (negocio).** Es predictivo, no reactivo. Permite actuar **antes**
del incumplimiento: ofrecer un plan de pago a quien se ve venir en problemas,
reforzar el recordatorio, o simplemente no aceptarlo en un grupo de monto alto.

`factoresPrincipales` (JSON) hace el modelo explicable: no basta con decir "riesgo
alto", hay que poder decir por qué. Sin explicabilidad, un rechazo basado en el
score es indefendible.

**Por qué debe existir.** Prevenir un incumplimiento cuesta una fracción de lo que
cuesta gestionarlo: cobertura del fondo, gestión de cobranza, sanción, apelación,
reemplazo. La prevención es el mejor negocio del módulo.

---

### `AlertaTemprana` / `alerta_temprana`

**Qué es.** Señales de comportamiento que anticipan un problema.

**Para qué sirve (negocio).** Los tres códigos son patrones observables y
accionables:
- `PAGA_CADA_VEZ_MAS_TARDE`: la tendencia importa más que el evento aislado. Pagó
  el día 3, luego el 5, luego el 8. Va camino a no pagar.
- `NO_ABRE_MENSAJES`: dejó de leer los recordatorios (dato que viene de M5). Suele
  preceder al abandono, o indicar un problema de canal que hay que corregir antes
  de tratarlo como moroso.
- `MULTIPLES_GRUPOS_EN_MORA`: se sobreextendió. Está en cuatro pasanakus y no
  puede con todos.

**Por qué debe existir.** Estas señales existen en los datos pero no en ninguna
tabla si no se materializan. Y sin materializarlas, nadie actúa sobre ellas.

---

## Paquete: Gestión de Cobranza

### `GestionCobranza` / `gestion_cobranza` — Raíz de agregado

**Qué es.** El proceso activo de cobrarle a alguien, con su etapa, su gestor y su
próxima acción.

**Para qué sirve (negocio).** **Convierte "hay que cobrarle" en un proceso con
dueño, etapa y próximo paso.** Las seis etapas (preventiva → temprana →
administrativa → prejudicial → judicial → castigo) son la práctica estándar de
cobranza, y existen porque **cada etapa tiene un costo y una efectividad
distintos**: un recordatorio automático cuesta centavos; una gestión judicial
cuesta más que la mayoría de las deudas de un pasanaku.

`costoAcumulado()` es la función que responde la pregunta económica clave:
**¿vale la pena seguir cobrando?** Si llevo Bs 400 gastados en gestión para
recuperar Bs 500, la respuesta es no, y corresponde castigar la deuda (con la
constancia reputacional intacta).

`proximaAccionEn` es lo que hace que la cobranza no se olvide: es el campo por el
que se ordena la bandeja de trabajo diaria.

**Por qué debe existir.** Sin gestión materializada, cobrar depende de que alguien
se acuerde. Y la cobranza que depende de la memoria no ocurre.

---

### `EstrategiaCobranza` / `estrategia_cobranza` — Política configurable

**Qué es.** Qué se hace en cada etapa: por qué canales, con qué frecuencia, con qué
límite de contactos, si requiere gestor humano y si se puede negociar quita.

**Para qué sirve (negocio).** Dos equilibrios delicados:

1. **`maxContactosPorSemana` y `frecuenciaDias`.** Cobrar demasiado es
   contraproducente: genera bloqueos, quejas por spam y hostilidad. Y en muchas
   jurisdicciones el acoso en la cobranza está regulado. El límite protege al
   deudor y a la plataforma.
2. **`permiteQuita` + `quitaMaximaPorcentaje`.** Reconoce una verdad incómoda:
   **a veces cobrar el 70% ahora es mejor que perseguir el 100% durante dos años.**
   Pero la quita tiene que tener tope y autorización, o se convierte en la salida
   fácil de cualquier gestor.

`requiereGestorHumano` marca dónde termina la automatización. Las primeras etapas
son automáticas; a partir de cierto punto, una persona tiene que llamar.

**Por qué debe existir.** Sin estrategia parametrizada, la cobranza es improvisada,
inconsistente entre casos y potencialmente abusiva.

---

### `AccionCobranza` / `accion_cobranza`

**Qué es.** Cada intento de contacto, con su resultado y su costo.

**Para qué sirve (negocio).** Es **el registro de que se hizo el esfuerzo**, y eso
importa en dos momentos: cuando la persona reclama que nunca la contactaron, y
cuando hay que justificar el castigo de una deuda (hay que probar que se agotaron
las instancias).

`ResultadoContacto` con siete valores captura la información que realmente sirve:
`TELEFONO_ERRONEO` (hay que actualizar datos, no insistir), `SOLICITA_PLAN` (quiere
pagar, dale un plan), `DISPUTA_LA_DEUDA` (para la cobranza y abre descargo),
`CONTACTADO_SE_NIEGA` (escalá). Un booleano "contactado" perdería todo eso.

`AVISO_A_AVALISTA` como tipo de acción merece nota: es de las más efectivas, porque
activa la presión social que sostiene el pasanaku. Y `costo` por acción es lo que
permite calcular el retorno de cada canal de cobranza.

**Por qué debe existir.** Sin acciones registradas, la gestión de cobranza no se
puede auditar, ni medir, ni defender.

---

### `PromesaPago` / `promesa_pago`

**Qué es.** El compromiso del deudor de pagar cierto monto en cierta fecha.

**Para qué sirve (negocio).** Es la unidad de trabajo real de toda cobranza. El
gestor no cobra: **consigue promesas**, y después las hace cumplir.

`estado` con `CUMPLIDA / INCUMPLIDA / PARCIAL` mide la variable más predictiva de
todas: **una promesa incumplida vale más que diez días de mora** como señal de
riesgo. Quien promete y no cumple no tiene un problema de liquidez, tiene un
problema de intención — y eso cambia toda la estrategia.

`canalCompromiso` registra dónde se comprometió (llamada, WhatsApp, app), lo que
importa si después niega haberlo hecho.

**Por qué debe existir.** Sin promesas registradas no hay seguimiento, y "dijo que
iba a pagar el viernes" se pierde en la memoria del gestor.

---

### `AcuerdoQuita` / `acuerdo_quita`

**Qué es.** El acuerdo de condonar parte de la deuda a cambio del pago del resto.

**Para qué sirve (negocio).** Es la salida negociada. Pero tiene un problema de
fondo que la entidad resuelve explícitamente: **la plata que se condona sale del
bolsillo de los otros participantes o del fondo que es de todos.** Por eso
`acuerdoGrupoId` apunta a una votación del grupo (M2): la condonación **no es
decisión del organizador ni del gestor**, es decisión de los que pierden esa
plata.

`justificacion` documenta el porqué, que es lo que se revisa si aparece un patrón
de quitas sospechosamente frecuentes con ciertos deudores.

**Por qué debe existir.** Sin acuerdo formal, condonar sería un ajuste manual de
saldo — el hueco por donde se va la plata en sistemas mal controlados.

---

## Paquete: Cobertura, Deuda y Recuperación

### `CoberturaIncumplimiento` / `cobertura_incumplimiento` — Raíz de agregado

**Qué es.** El acto de que el fondo ponga la plata que un participante no puso.

**Para qué sirve (negocio).** Es **el momento en que el grupo se salva**. El
beneficiario del período cobra completo, el grupo sigue, y el problema se traslada
a un circuito paralelo que no lo detiene.

`porcentajeCobertura` reconoce que la cobertura puede ser parcial: el fondo cubre
el 80% y el beneficiario absorbe el 20%. Es una decisión de política que balancea
la protección con la sostenibilidad del fondo.

`requirioAprobacionManual` + `aprobadaPor`: montos altos no se cubren
automáticamente. Es control interno básico.

`generarDeuda()` es el paso conceptualmente crítico: **cubrir no es perdonar**. En
el mismo acto en que el fondo pone la plata, nace una deuda del participante hacia
el fondo. Si esos dos hechos no fueran atómicos, existiría el estado imposible de
"el fondo pagó y nadie debe".

**Por qué debe existir.** Sin cobertura como entidad, el fondo sería un saldo que
baja sin que quede constancia de por qué, para quién y quién lo autorizó.

**A nivel de sistema.** `UNIQUE (obligacion_id)`: **el fondo cubre una obligación
una sola vez.** La aplicación es atómica con `movimiento_fondo` y con el asiento
contable de M3, en la misma transacción, para que el saldo del fondo nunca mienta.

---

### `DeudaParticipante` / `deuda_participante` — Raíz de agregado

**Qué es.** Lo que un participante debe, como obligación exigible con vida propia.

**Para qué sirve (negocio).** El punto clave está en la nota del diagrama y merece
subrayarse: **la deuda vive a nivel de usuario, no de participante.**

> Si el grupo termina, la deuda sigue exigible.

Esa es la diferencia entre el pasanaku digital y el de cuaderno. En el tradicional,
cuando el grupo se disuelve, la deuda se evapora: no hay quién la cobre ni dónde
esté registrada. Acá persiste, sigue generando recargos, bloquea el ingreso a
nuevos grupos (vía `RestriccionUsuario` en M1) y puede recuperarse años después.

`acreedor` distingue a quién se le debe: al fondo de garantía (si cubrió), al grupo
(si no hubo cobertura) o a la plataforma. Es determinante para saber a quién le
llega la recuperación.

`esSubrogada` marca que la deuda cambió de acreedor.
`fechaPrescripcion` reconoce que las deudas caducan legalmente: no se puede
perseguir a alguien indefinidamente, y el sistema debe saber cuándo dejar de
hacerlo.

**Por qué debe existir.** Sin deuda como entidad independiente, el incumplimiento
se cierra cuando el grupo cierra, y quien abandonó no tiene ninguna consecuencia
duradera.

**A nivel de sistema.**
`CHECK saldo_actual = capital_original + recargos_acumulados - total_abonado`.
`fecha_prescripcion` indexada para el barrido de prescripciones.

---

### `Subrogacion` / `subrogacion`

**Qué es.** La formalización del cambio de acreedor: el fondo pagó, ahora el fondo
es el acreedor.

**Para qué sirve (negocio).** Es la figura jurídica que **da fundamento legal a que
el fondo cobre**. Sin subrogación formal, el fondo pagó una deuda ajena y no
tendría título para reclamarla — que es exactamente el argumento que usaría un
deudor bien asesorado.

`documentoRespaldoUrl` guarda el instrumento que lo respalda.

**Por qué debe existir.** Sin subrogación documentada, la deuda con el fondo es
una anotación interna sin sustento jurídico. Con ella, es exigible.

---

### `AbonoRecuperacion` / `abono_recuperacion` — **append-only**

**Qué es.** Cada pago que reduce una deuda, con su origen y su imputación.

**Para qué sirve (negocio).** El desglose `aplicadoACapital` / `aplicadoARecargos`
importa mucho y es fuente frecuente de disputa: **si todo se imputa primero a
recargos, el capital nunca baja** y la deuda se vuelve perpetua. La regla de
imputación debe ser explícita y visible, no una decisión enterrada en el código.

`origen` cubre las cinco formas reales de recuperar:
- `PAGO_VOLUNTARIO`: pagó.
- `DESCUENTO_DE_ENTREGA`: **la más efectiva de todas.** Cuando le toca cobrar su
  turno, se le descuenta. Es el único momento del ciclo en que el sistema tiene
  plata del deudor en la mano.
- `EJECUCION_AVAL`: pagó el que respondía por él.
- `ACUERDO_QUITA`: pagó una parte negociada.
- `COMPENSACION`: se cruzó contra un saldo a favor.

**Por qué debe existir.** Append-only: los abonos no se editan. Si hubo un error,
se registra la reversión (`revertido_en`). Es dinero: la trazabilidad no es
opcional.

---

### `CastigoDeuda` / `castigo_deuda`

**Qué es.** La baja contable de una deuda considerada incobrable.

**Para qué sirve (negocio).** Reconoce la realidad: hay deudas que no se van a
cobrar, y mantenerlas como activo distorsiona los estados financieros del fondo.
Los cinco motivos son honestos, incluido `COSTO_MAYOR_QUE_DEUDA` —perseguir Bs 300
cuesta más de Bs 300— y `FALLECIMIENTO`.

Pero el campo decisivo es `mantieneRegistroReputacional = true`:

> **Castigar no es perdonar.** Es una decisión contable, no un perdón.

El registro reputacional se mantiene. Si el deudor aparece años después queriendo
entrar a otro grupo, la deuda **se reactiva** (`reactivarSiPaga()`). Sin esta
distinción, castigar una deuda sería una amnistía y crearía el incentivo perverso
de aguantar hasta que te castiguen.

**Por qué debe existir.** Sin castigo formal, las deudas incobrables se acumulan
para siempre en la cartera y el fondo aparenta tener activos que no existen.

---

## Paquete: Aval Solidario

### `AvalParticipante` / `aval_participante`

**Qué es.** El compromiso de una persona de responder por la deuda de otra, hasta
cierto monto.

**Para qué sirve (negocio).** **Formaliza la práctica más antigua del pasanaku:
quien te invita responde por vos.** En el pasanaku presencial esto es implícito y
poderoso: si traés a alguien que no paga, el grupo te lo cobra a vos, socialmente.
La entidad lo hace explícito, con monto máximo y aceptación firmada.

`avalistaUsuarioId` suele coincidir con `participante.invitadoPorId` (M2): la
cadena de confianza se convierte en cadena de responsabilidad.

`montoMaximoAvalado` y `alcance` (total / un período / porcentaje) permiten que el
aval sea proporcionado: nadie va a avalar de forma ilimitada a un conocido, pero sí
puede avalar un período.

`esParticipanteDelGrupo` distingue dos situaciones distintas: si el avalista está
en el grupo, se le puede descontar de su propio turno; si es externo, hay que
cobrarle por fuera.

**Por qué debe existir.** El aval es lo que hace viable prestarle confianza a
alguien sin historial. Sin él, un usuario nuevo no puede entrar a ningún grupo
serio y la plataforma no puede crecer más allá de los ya conocidos.

**A nivel de sistema.** `token_aceptacion_id` → M1: **el aval se acepta con firma
verificada**, no con un checkbox. Es un compromiso económico real y necesita
constancia de quién aceptó, qué y cuándo.

---

### `EjecucionAval` / `ejecucion_aval`

**Qué es.** El acto de exigirle al avalista que pague.

**Para qué sirve (negocio).** Es el momento más delicado del módulo: **se le está
cobrando a alguien la deuda de otro**. Por eso `notificar()` y `plazoRespuesta`
son obligatorios: el avalista tiene derecho a enterarse, a verificar y a responder
antes de que se le exija.

`generaDeudaDelAvalista` cierra el círculo con honestidad: si el avalista tampoco
paga, se le abre **su propio expediente** de incumplimiento (tipo
`INCUMPLIMIENTO_AVAL`). El aval no es simbólico; tiene consecuencias reales para
quien lo firma. Y eso es lo que hace que la gente avale con criterio, que es
justamente el filtro que se busca.

**Por qué debe existir.** Sin ejecución formal, el aval es una declaración de
buenas intenciones que nadie hace cumplir — y por lo tanto no filtra nada.

---

## Paquete: Sanciones con Debido Proceso

### `PoliticaSancion` / `politica_sancion` — Política configurable

**Qué es.** Las reglas del proceso sancionatorio: si requiere acuerdo del grupo,
plazos de descargo y apelación, y prescripción.

**Para qué sirve (negocio).** Define las **garantías procesales**. `plazoDescargoDias`
y `plazoApelacionDias` no son configuración técnica: son los derechos del
sancionado. Que sean parametrizables por grupo permite que un grupo formal tenga
plazos más largos y uno pequeño resuelva más rápido.

`requiereAcuerdoGrupo` decide si las sanciones las aplica el organizador o las vota
el grupo. Para expulsiones, votarlas es lo correcto.

**Por qué debe existir.** Sin política explícita, los plazos los decide quien
sanciona — que es la definición de proceso arbitrario.

---

### `MatrizSancion` / `matriz_sancion`

**Qué es.** La tabla que determina qué sanción corresponde a cada combinación de
`tipo de incumplimiento × severidad × número de reincidencia`.

**Para qué sirve (negocio).** **Es la entidad que garantiza proporcionalidad.**

> La sanción no la decide una persona de humor variable: sale de la matriz.

Eso resuelve tres problemas de golpe:
1. **Consistencia**: dos personas con el mismo incumplimiento reciben la misma
   sanción, sin importar quién las evalúe ni si le caen bien al organizador.
2. **Previsibilidad**: se puede publicar la matriz y todos saben de antemano qué
   les pasa si incumplen. Eso tiene efecto disuasivo, que una sanción impredecible
   no tiene.
3. **Proporcionalidad**: el primer atraso leve es una advertencia; el tercero grave
   es expulsión. La escalada está codificada.

`esAutomatica` vs `requiereRevisionHumana` marca el límite: las advertencias se
aplican solas; una expulsión pasa por revisión.

**Por qué debe existir.** Sin matriz, sancionar es un juicio subjetivo, y en un
grupo con dinero de por medio la subjetividad se lee siempre como favoritismo.

**A nivel de sistema.** `UNIQUE (tipo_incumplimiento, severidad, numero_reincidencia)`
dentro de cada política.

---

### `Sancion` / `sancion`

**Qué es.** La sanción concreta aplicada a una persona por un expediente.

**Para qué sirve (negocio).** Los nueve tipos van de la advertencia a la
inhabilitación total, e incluyen algunos específicos del contexto:
- `PERDIDA_DE_PRIORIDAD_DE_TURNO`: te vas al final de la fila. Es de las más
  efectivas porque toca lo que más le importa a la gente en un pasanaku.
- `SUSPENSION_DE_VOTO`: perdés voz en las decisiones del grupo mientras estés en
  falta.
- `RETENCION_DE_ENTREGA`: no cobrás hasta regularizar.

La máquina de estados **es el debido proceso hecho dato**:

```
PROPUESTA → NOTIFICADA → EN_DESCARGO → FIRME → [EN_APELACION] → CUMPLIDA
                                    ↘ REVOCADA / PRESCRITA
```

**La sanción solo se ejecuta cuando está `FIRME`**: antes hubo notificación, plazo
de descargo y, si se apeló, resolución. Ejecutar una sanción antes de que quede
firme es exactamente lo que la vuelve indefendible.

**Por qué debe existir.** Sin sanción como entidad con estados, la consecuencia
sería inmediata y sin recurso — arbitraria por construcción.

**A nivel de sistema.** Toda transición va a la bitácora de M9 y genera
`evento_reputacion` en M6.

---

### `ApelacionSancion` / `apelacion_sancion`

**Qué es.** El recurso del sancionado, con instancia y plazo.

**Para qué sirve (negocio).** `instancia` con tres niveles (organizador → comité
del grupo → soporte de la plataforma) es lo que hace real el derecho a apelar:
apelar ante quien te sancionó no es apelar. `escalarInstancia()` permite subir
cuando la primera instancia no resuelve.

`fechaLimiteResolucion` protege al apelante de la táctica más simple para
neutralizar una apelación: no resolverla nunca.

**Por qué debe existir.** Sin apelación, un error del sistema o del organizador es
definitivo. Y los errores ocurren: expedientes abiertos por pagos mal conciliados
son frecuentes al principio de la operación.

---

## Paquete: Continuidad del Grupo

> **Por qué este paquete existe.** Porque sancionar al que falló no resuelve el
> problema del grupo: **sigue faltando la plata**. Estas cuatro entidades se
> ocupan de que el grupo sobreviva al incumplimiento.

### `ReemplazoParticipante` / `reemplazo_participante`

**Qué es.** La sustitución de un participante que salió por otro que entra al
mismo cupo.

**Para qué sirve (negocio).** Es la solución preferida frente al abandono: mejor
reemplazar que reducir la bolsa de todos. Los tres campos económicos son los que
hacen posible el acuerdo:

- `deudaAsumidaPorEntrante` y `deudaRetenidaPorSaliente`: **normalmente el
  entrante no asume la deuda vieja**, porque si la asumiera nadie querría entrar a
  reemplazar a un moroso. La deuda queda con el saliente y se persigue por M8.
- `conservaOrdenDeTurno`: el entrante hereda la posición del cupo en el
  calendario. Es lo que permite que el resto del grupo no se vea afectado.

**Por qué debe existir.** Sin reemplazo formal, la salida de una persona obliga a
recalcular el calendario de todos, que es inaceptable para once personas que no
hicieron nada mal.

**A nivel de sistema.** Ejecuta un `TraspasoCupo` en M2.

---

### `CandidatoReemplazo` / `candidato_reemplazo`

**Qué es.** Cada persona evaluada para ocupar el cupo vacante.

**Para qué sirve (negocio).** Permite comparar antes de decidir, y `fuenteCandidato`
(lista de espera / invitación / emparejamiento) mide qué canal provee mejores
reemplazos. `aceptaCondiciones` es explícito porque entrar a un grupo empezado
tiene condiciones especiales: hay que ponerse al día con los períodos ya
transcurridos.

**Por qué debe existir.** Sin candidatos registrados, el reemplazo es una decisión
sin alternativas evaluadas, y no se puede medir la efectividad de cada canal.

---

### `PlanContingencia` / `plan_contingencia`

**Qué es.** La respuesta estructurada a una crisis del grupo.

**Para qué sirve (negocio).** Es el manual de emergencia. Cuatro disparadores
(mora crítica, fondo agotado, abandono masivo, organizador inhabilitado) y cinco
respuestas posibles:

- `PRORRATEO_ENTRE_ACTIVOS`: los que quedan ponen la diferencia.
- `REDUCCION_DE_BOLSA`: todos cobran menos, pero el grupo sigue.
- `EXTENSION_DE_PLAZO`: se alargan los períodos.
- `REEMPLAZO`: se busca quién entre.
- `DISOLUCION`: se corta la pérdida.

`simularImpacto()` es la función clave: **antes de someterlo a votación, cada
participante ve cuánto le cuesta cada alternativa**. Votar a ciegas entre cinco
opciones abstractas no es decidir.

`requiereAcuerdo` + `acuerdoGrupoId`: la contingencia la aprueba el grupo (M2),
porque todas las opciones implican que alguien pierde algo.

**Por qué debe existir.** Sin plan de contingencia, la crisis se maneja
improvisando por WhatsApp, y el resultado suele ser que el grupo se desarma con
pérdidas mal repartidas.

---

### `DisolucionAnticipada` / `disolucion_anticipada`

**Qué es.** El cierre del grupo antes de completar el ciclo, con la liquidación de
todos.

**Para qué sirve (negocio).** Es el peor escenario y por eso hay que modelarlo
bien. La pregunta que responde es la más difícil de todo el sistema: **si el grupo
se rompe en el período 7 de 12, ¿quién le debe qué a quién?**

Unos ya cobraron su turno y todavía deben aportes; otros aportaron siete veces y
nunca cobraron. Sin un cálculo formal, esa liquidación es una discusión sin final
—y es exactamente donde el pasanaku tradicional termina en pelea familiar o legal.

`saldoADistribuir` es lo que queda para repartir después de cruzar todo.

**Por qué debe existir.** Sin disolución modelada, el cierre anticipado es un
conflicto abierto sin árbitro ni cifras.

---

### `LiquidacionParticipante` / `liquidacion_participante`

**Qué es.** La posición neta de cada participante en la disolución.

**Para qué sirve (negocio).** `posicionNeta` es el número que resuelve todo:
**positiva, se le devuelve; negativa, debe.** Se calcula como total aportado menos
total cobrado menos deuda pendiente. Es transparente, verificable y aplica el mismo
criterio a todos.

`estado = EN_COBRANZA` reconoce lo obvio: los que quedan debiendo pasan al circuito
de cobranza como cualquier otra deuda, y no se evaporan porque el grupo se cerró.

**Por qué debe existir.** Sin liquidación individual, la disolución sería un
reparto discrecional del saldo, y quien lo hiciera quedaría bajo sospecha
inevitablemente.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `FondoGarantia` | Si uno no paga, el beneficiario cobra menos: la injusticia que desarma los grupos. |
| `PoliticaCobertura` | El fondo se agota cubriendo atrasos de un día y a parásitos sin límite. |
| `MovimientoFondo` | Los dueños del fondo no pueden verificar en qué se usó su plata. |
| `RegistroIncumplimiento` | La morosidad es un flag: no se puede gestionar, probar, apelar ni cerrar. |
| `EvidenciaIncumplimiento` | El expediente es una acusación sin respaldo. |
| `DescargoParticipante` | Se sanciona sin oír: indefendible ante cualquier reclamo. |
| `HistorialIncumplimientoUsuario` | Quien abandonó un grupo entra limpio al siguiente. |
| `GestionCobranza` + `Estrategia` | Cobrar depende de que alguien se acuerde; o se cae en acoso. |
| `PromesaPago` | Se pierde la señal más predictiva de todas. |
| `CoberturaIncumplimiento` | El fondo baja sin constancia de por qué, para quién y con qué autorización. |
| `DeudaParticipante` | Cuando el grupo cierra, la deuda se evapora: sin consecuencias para el que abandonó. |
| `Subrogacion` | El fondo pagó una deuda ajena y no tiene título para cobrarla. |
| `CastigoDeuda` | Las incobrables se acumulan y el fondo aparenta activos inexistentes. |
| `AvalParticipante` | Nadie sin historial puede entrar a un grupo serio. |
| `MatrizSancion` | Sancionar es subjetivo, y lo subjetivo se lee como favoritismo. |
| `Sancion` con estados | La consecuencia es inmediata y sin recurso: arbitraria por construcción. |
| `ApelacionSancion` | Un error del sistema es definitivo. |
| `Reemplazo` / `PlanContingencia` / `Disolucion` | La crisis se improvisa por WhatsApp y el grupo se desarma con pérdidas mal repartidas. |


---

## Tablas incorporadas después de la redacción original: respaldo empresarial

> Fichas escritas para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Explican
> el camino vigente de ADR-051 (D-15): el faltante del pozo lo cubre una **reserva empresarial por ciclo**, todo o
> nada, y la deuda del incumplidor se conserva como derecho de recuperación. Fuente común: CU-23 (sección de
> vigencia y reglas R-GAR-08 a R-GAR-12) y `docs/Restricciones.md`.

### `CapacidadRespaldo` / `capacidad_respaldo`

**Qué es.** El tope de dinero que la empresa está dispuesta a comprometer para cubrir faltantes de pozo, por moneda, y cuánto ya comprometió.

**Para qué sirve (negocio).** Que la promesa «el pozo se entrega completo» tenga un límite conocido y gobernado: la empresa no puede respaldar más grupos de los que puede pagar.

**Por qué debe existir.** Sin un tope explícito, cada grupo nuevo agregaría exposición sin que nadie la viera crecer. Dos grupos que se activan a la vez podrían comprometer la misma plata.

**A nivel de sistema.** Una fila por ámbito y moneda. R-GAR-08: `monto_comprometido <= monto_tope`. Dos activaciones simultáneas compiten por la misma fila: se bloquea primero (`CU23ReservarRespaldo`) y la base lo vuelve a exigir. No hay capacidad por omisión: sin fila cargada por gobierno se deniega, y el tope es un dato (ambigüedad A2). `monto_comprometido` es caché del libro `movimiento_reserva`.

### `ReservaRespaldo` / `reserva_respaldo`

**Qué es.** Lo que la empresa aparta de su capacidad para un grupo y un ciclo concretos, antes de activarlo.

**Para qué sirve (negocio).** Que cada grupo activo tenga su respaldo separado y visible: cuánto se reservó, cuánto se usó para cubrir, cuánto se recuperó de los deudores y cuánto se liberó al terminar.

**Por qué debe existir.** La capacidad es global; la cobertura ocurre en un grupo. Sin la reserva por ciclo no se puede saber cuánto respaldo le queda a un grupo ni liberar lo que no se usó.

**A nivel de sistema.** Una por grupo y ciclo. R-GAR-09: `monto_aplicado + monto_liberado <= monto_reservado` y `monto_recuperado <= monto_aplicado`. Los cuatro contadores son caché del libro `movimiento_reserva`, que mantiene la base por trigger. Una reserva `LIBERADA` no admite ampliación ni aplicación.

### `MovimientoReserva` / `movimiento_reserva`

**Qué es.** El libro inmutable de la reserva: cada reserva, ampliación, aplicación, reversa, recuperación o liberación, con el disponible y la exposición que dejó.

**Para qué sirve (negocio).** Que cualquier número del respaldo se pueda explicar paso a paso: por qué hoy quedan X bolivianos disponibles y cuánto está expuesto la empresa.

**Por qué debe existir.** Si los saldos de la reserva se editaran directo, un error o un reintento cambiaría el número sin dejar rastro. Con el libro, el saldo se reconstruye y se corrige con un movimiento inverso.

**A nivel de sistema.** Append-only. `tipo` ∈ RESERVA/AMPLIACION/APLICACION/REVERSA_APLICACION/RECUPERACION/LIBERACION. El trigger `tg_movimiento_reserva_aplica` actualiza los contadores de `reserva_respaldo` y `capacidad_respaldo` (R-GAR-09). `clave_idempotencia` única. `referencia_tipo` y `referencia_id` dicen qué lo causó.

### `CoberturaRespaldo` / `cobertura_respaldo`

**Qué es.** El uso de la reserva de un grupo para completar el pozo de un turno, al corte, cuando no llegó toda la plata.

**Para qué sirve (negocio).** Que quien cobra el turno reciba el pozo completo aunque alguien no haya pagado, y que se sepa cuánto puso la empresa y cuánto recuperó después.

**Por qué debe existir.** La cobertura es todo o nada y genera un derecho de recuperación. Sin el registro no se distinguiría la plata puesta por la empresa de la pagada por los participantes.

**A nivel de sistema.** R-GAR-10: una cobertura viva por turno (la `REVERSADA` deja de contar) y `monto_faltante = monto_pozo - monto_confirmado - monto_cubierto_mutual`. `RECUPERADA_TOTAL` si y solo si lo recuperado iguala el faltante. R-GAR-12: el turno es del grupo y del período que declara; la base lo comprueba con una consulta de alcance mínimo a `grupos`.

### `CoberturaRespaldoLinea` / `cobertura_respaldo_linea`

**Qué es.** Qué obligación impaga cubrió la cobertura y por cuánto, línea por línea.

**Para qué sirve (negocio).** Saber a quién se le cubrió cuánto, y por lo tanto quién le debe cuánto a la empresa. Sin la línea, la deuda sería un total sin dueño.

**Por qué debe existir.** La recuperación se cobra a cada incumplidor por lo que se le cubrió a él. Sin línea por obligación no se puede perseguir ni descontar la deuda correcta.

**A nivel de sistema.** Una por cobertura y obligación. R-GAR-11: las líneas suman exactamente el faltante de la cobertura (trigger diferido `tg_cobertura_respaldo_linea_cuadra`, al confirmar) y cada línea recupera a lo sumo lo que cubrió.

### `RecuperacionRespaldo` / `recuperacion_respaldo`

**Qué es.** Cada pago del incumplidor que devuelve a la reserva lo que la empresa cubrió por él.

**Para qué sirve (negocio).** Que la cobertura no sea un regalo: cuando el deudor paga, esa plata repone el respaldo y la deuda baja.

**Por qué debe existir.** Sin ella, un pago tardío no se podría atribuir a la cobertura que lo originó, y un mismo pago podría contarse dos veces como recuperación.

**A nivel de sistema.** Append-only. `pago_id` único: un pago recupera una sola vez (R-GAR-11). Apunta a la línea recuperada y al `movimiento_reserva` de tipo RECUPERACION que la registró en el libro.
