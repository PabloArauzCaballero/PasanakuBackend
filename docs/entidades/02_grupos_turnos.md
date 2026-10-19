# Módulo 2 — Grupos, Cupos, Turnos y Gobernanza

> **Pregunta de negocio que responde este módulo:**
> *¿Cuáles son las reglas del juego de este pasanaku, quién está adentro, en qué
> orden cobra cada uno, y quién decide cuando hay que cambiar algo?*

Un pasanaku es un **contrato colectivo rotativo**: N personas ponen la misma
cantidad cada período, y cada período uno se lleva toda la bolsa, hasta que todos
cobraron una vez. Suena simple, y por eso se subestima. En la práctica el modelo
tiene que soportar: gente con dos "manos", medio cupo compartido entre dos
personas, alguien que se va a la mitad y hay que reemplazarlo sin descuadrar el
calendario, permutas de turno entre participantes, y decisiones colectivas que el
organizador no puede tomar solo.

Y sobre todo tiene que resolver **el punto de desconfianza número uno del
pasanaku: el orden de cobro**.

---

## Paquete: Definición del Grupo

### `Grupo` / `grupo` — Raíz de agregado

**Qué es.** El pasanaku en sí: monto del aporte, cada cuánto se aporta, cuántos
cupos, cuándo empieza, con qué reglas.

**Para qué sirve (negocio).** Es el contrato. Todo lo demás cuelga de acá. Los
campos que más importan y por qué:

- `montoAporte` + `periodicidad` + `numPeriodos` + `cuposTotales` son los cuatro
  números que definen el negocio completo: cuánto pongo, cada cuánto, por cuánto
  tiempo, y cuánto me llevo cuando me toque. `montoBolsaPorPeriodo()` deriva de
  ellos y es la cifra que la gente realmente quiere saber.
- `esAutogestionado` + `organizadorId` distinguían dos modelos de negocio: el grupo que
  administra una persona habilitada (M7) y el que administra el sistema solo. Por
  [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] (D-12) **solo existe el primero**: todo grupo tiene un
  organizador humano habilitado y el organizador digital únicamente automatiza tareas. En ningún caso
  el organizador cobra comisión: administrar no se cobra (RN-18). La comisión que
  sí existe es la de la plataforma por el servicio (M11), que se congela por grupo
  al constituirlo (`tarifa_congelada_grupo`) para que el precio no cambie a mitad
  del juego.
- `requiereKYCMinimo` y `reputacionMinima` son **filtros de entrada**. Un grupo de
  Bs 200 entre amigos no pide nada; uno de USD 500 entre desconocidos exige
  documento verificado y buen historial. Sin estos campos, todos los grupos
  tendrían el mismo nivel de exigencia y la plataforma solo podría servir al
  segmento más bajo o al más alto, nunca a ambos.
- `diasGracia` y `aplicaRecargoMora` codifican la tolerancia del grupo. En un
  pasanaku de barrio la gracia es de una semana y nadie cobra recargo; en uno
  formal, el recargo es la única forma de que la gente pague a tiempo.
- `usaFondoGarantia` + `porcentajeFondoGarantia` deciden si el grupo se
  autoasegura (M8). Es la diferencia entre "si uno no paga, el beneficiario del
  período cobra menos" y "si uno no paga, el fondo cubre y el beneficiario cobra
  completo".
- `quorumDecisiones` define cuánta gente hace falta para aprobar un cambio. Es lo
  que impide que tres personas decidan expulsar a un cuarto.

**Por qué debe existir.** Es la unidad de negocio. Sin ella no hay contexto para
ninguna obligación, ningún turno ni ninguna entrega.

**A nivel de sistema.** `codigo_publico` es `UNIQUE` y es lo que se comparte por
WhatsApp para invitar. `num_periodos >= 3` por CHECK: un "pasanaku" de dos
períodos no es un pasanaku. `estado` indexado porque las tareas programadas del
organizador digital (M7) barren por estado todos los días.

---

### `ConfiguracionGrupo` / `configuracion_grupo`

**Qué es.** Los parámetros operativos finos del grupo, separados de su definición
económica.

**Para qué sirve (negocio).** Cada campo aquí es una decisión que se toma una vez
y afecta el comportamiento diario:

- `permiteCuposMultiples` + `maxCuposPorPersona`: ¿se permite que alguien tenga
  dos manos? Es práctica habitual y a la vez es riesgo concentrado — si esa
  persona falla, el grupo pierde el doble.
- `permitePermutaTurnos`: ¿pueden dos participantes intercambiar posiciones?
  Permitirlo da flexibilidad real (a alguien se le enfermó un familiar y necesita
  cobrar antes); prohibirlo evita el mercado gris de compra-venta de turnos.
- `requiereAvalista`: exige que cada participante tenga quién responda por él
  (M8). Sube la fricción de entrada y baja drásticamente la morosidad.
- `permiteIngresoTardio`: ¿puede entrar alguien con el grupo ya empezado? Tiene
  implicaciones económicas serias (debe ponerse al día con los períodos ya
  transcurridos).
- `horaLimitePago` y `toleranciaMontoParcial`: ¿un pago a las 23:50 del día de
  vencimiento cuenta como puntual? ¿Bs 499 sobre Bs 500 cuenta como pagado? Son
  las dos preguntas que generan más discusiones reales, y por eso se responden
  por configuración y no por criterio de quien mire.

**Por qué debe existir.** Separada del `Grupo` porque son ~10 campos que casi
nunca se consultan junto con la definición económica, y porque su cambio tiene
otro nivel de autorización. `validarCoherencia()` evita combinaciones absurdas
(permitir permutas pero exigir orden estricto por reputación, por ejemplo).

**A nivel de sistema.** PK compartida con `grupo_id`: relación 1-a-1 estricta.

---

### `ReglamentoGrupo` / `reglamento_grupo`

**Qué es.** El texto de las reglas del grupo, versionado y sellado con hash.

**Para qué sirve (negocio).** Es el documento que la gente firma. Contiene lo que
la configuración no puede expresar en números: qué pasa si alguien abandona, cómo
se reparte si el grupo se disuelve, qué se considera falta grave. Cuando hay
conflicto —y en pasanakus lo hay— este es el texto al que se apela.

El versionado (`version`, `vigenteDesde/Hasta`) importa porque los reglamentos se
modifican a mitad de ciclo. `requiereReaceptacion()` decide si un cambio es tan
sustancial que hay que pedir que todos vuelvan a firmar.

**Por qué debe existir.** `hashContenido` es lo que convierte el reglamento en
prueba: sin él, cualquiera podría alegar que el texto se editó después de que lo
firmó.

---

### `AceptacionReglamento` / `aceptacion_reglamento`

**Qué es.** La firma de un participante sobre una versión concreta del
reglamento.

**Para qué sirve (negocio).** Es el equivalente digital de la firma en el acta.
Guarda `hashFirmado`, IP, fecha y el token de firma (M1). Cuando un participante
sancionado dice "yo no sabía que se podía expulsar por dos cuotas impagas", esta
fila responde: firmaste la versión 3 el 2 de febrero, y la cláusula estaba ahí.

**Por qué debe existir.** Sin ella no hay debido proceso posible: no se puede
sancionar a alguien por incumplir reglas que no consta que haya aceptado. Es la
base de todo el módulo 8.

**A nivel de sistema.** `token_firma_id` apunta a `token_verificacion` (M1,
propósito `FIRMA_REGLAMENTO`): la firma se hizo con un código enviado al teléfono
verificado del participante, no con un simple clic en un checkbox.

---

### `HistorialEstadoGrupo` / `historial_estado_grupo`

**Qué es.** Cada transición de estado del grupo, con motivo y autor.

**Para qué sirve (negocio).** Responde: *¿por qué este grupo está suspendido,
quién lo suspendió y cuándo?* Un grupo suspendido significa que doce personas no
pueden aportar ni cobrar; nadie debería poder hacerlo sin dejar rastro y sin
motivo.

**Por qué debe existir.** El campo `estado` dice dónde está el grupo hoy; esta
tabla dice cómo llegó ahí. Ante un reclamo colectivo, es lo primero que se mira.

---

## Paquete: Membresía — Participantes y Cupos

> **La decisión de diseño más importante del módulo: separar `Participante` de
> `Cupo`.**
> Un participante es la *persona* dentro del grupo. Un cupo es la *mano*, la
> cuota económica. No son lo mismo, y confundirlos rompe tres casos reales:
> (1) una persona con dos manos —paga doble y cobra dos veces—, práctica
> habitual; (2) media mano compartida entre dos personas que no pueden con la
> cuota entera; (3) reemplazar a un moroso **conservando la posición económica de
> ese cupo en el calendario**, para que el resto del grupo no tenga que reordenar
> sus turnos por culpa de uno.
> Por eso las obligaciones (M3) y los turnos cuelgan del **cupo**, no de la
> persona.

### `Participante` / `participante` — Raíz de agregado

**Qué es.** Una persona dentro de un grupo específico, con su estado y su
historia en ese grupo.

**Para qué sirve (negocio).** Es la membresía. Su máquina de estados
(`INVITADO → POSTULANTE → ACEPTADO_PENDIENTE_FIRMA → ACTIVO → EN_MORA → …`) es el
recorrido real de alguien que entra a un pasanaku: lo invitan, postula, lo
aceptan, firma, participa, eventualmente se atrasa, y termina de una de cuatro
maneras (retirado, reemplazado, expulsado, o el grupo termina bien).

`invitadoPorId` es un campo pequeño con consecuencias grandes: construye la
**cadena de confianza**. Quien te invitó respondió por vos, y el módulo 8 usa
justamente ese vínculo para el aval solidario. Es la digitalización del "yo lo
traje, yo respondo".

`reputacionAlIngresar` congela el score del día de entrada. Sirve para dos cosas:
evaluar después si el filtro de reputación funcionó (¿los que entraron con score
bajo fallaron más?), y para no juzgar retroactivamente a alguien cuyo puntaje bajó
por hechos posteriores en otro grupo.

`alias` existe porque en un grupo de trabajo la gente se conoce por apodo, y
mostrar el nombre completo de la cédula es a la vez incómodo y una filtración
innecesaria de datos personales al resto del grupo.

**Por qué debe existir separado de `Usuario`.** Porque el mismo usuario está en
varios grupos con estados distintos: al día en uno, en mora en otro. El estado es
propiedad de la relación, no de la persona.

**A nivel de sistema.** `UNIQUE (grupo_id, usuario_id)`: la misma persona no
figura dos veces en el mismo grupo. Para varias manos se crean varios registros en
`cupo`, no en `participante`.

---

### `Cupo` / `cupo`

**Qué es.** Una cuota económica del grupo. La "mano". Numerada del 1 al N.

**Para qué sirve (negocio).** Es la unidad económica real del pasanaku. Cada cupo
genera una obligación de aporte por período (M3) y tiene derecho a cobrar la bolsa
exactamente una vez por ciclo (M2 → M4).

`fraccion` permite el medio cupo: dos personas comparten una mano, cada una pone
la mitad y cuando les toca se reparten la bolsa. Es común entre parientes o
compañeros que no pueden con la cuota entera pero no quieren quedarse fuera.

El caso que justifica toda la separación: **un participante abandona en el período
4 de 12**. Si los turnos colgaran de la persona, habría que reordenar el
calendario de los otros once. Colgando del cupo, se libera el cupo
(`LIBERADO_POR_INCUMPLIMIENTO`), entra un reemplazante y **hereda la posición**:
el calendario del grupo no se mueve, y las diez personas que no hicieron nada mal
no sufren consecuencias.

**Por qué debe existir.** Sin `Cupo`, el modelo no soporta manos múltiples, ni
medias manos, ni reemplazos sin descuadre. Los tres casos son la norma, no la
excepción.

**A nivel de sistema.** `UNIQUE (grupo_id, numero)`. Restricción adicional por
trigger: `SUM(fraccion)` por grupo debe igualar `cupos_totales`. `participante_id`
es nullable: un cupo puede estar libre esperando reemplazo.

---

### `TraspasoCupo` / `traspaso_cupo`

**Qué es.** El cambio de manos de un cupo, de un participante a otro, con lo que
se transfiere y lo que no.

**Para qué sirve (negocio).** Es donde se resuelve la pregunta más espinosa del
reemplazo: **¿quién se hace cargo de la deuda que dejó el que se fue, y quién
tiene derecho a cobrar el turno?** Los dos campos `deudaTransferida` y
`derechoCobroTransferido` obligan a responderlo explícitamente en cada traspaso,
en lugar de dejarlo a interpretación.

Los tres motivos (`VENTA`, `REEMPLAZO_POR_MORA`, `RETIRO`) tienen consecuencias
distintas: en una venta acordada el entrante suele asumir todo; en un reemplazo
por mora, la deuda vieja normalmente **queda con el saliente** (se persigue en M8)
y el entrante arranca limpio, porque si no, nadie aceptaría reemplazar a un
moroso.

`aprobadoPorAcuerdoId` es obligatorio en la práctica: un traspaso no puede ser
decisión de dos personas cuando afecta la garantía de las otras diez.

**Por qué debe existir.** Sin esta tabla, un traspaso es un `UPDATE` de
`cupo.participante_id` y se pierde toda la historia económica: quién debía qué,
quién asumió qué, quién lo autorizó.

**A nivel de sistema.** `revertido_en` permite deshacer un traspaso ejecutado por
error sin borrar el registro.

---

### `SolicitudRetiro` / `solicitud_retiro`

**Qué es.** El pedido de un participante de salir del grupo antes de que termine.

**Para qué sirve (negocio).** Convierte "me voy" en un proceso con número.
`liquidacionCalculada` responde la pregunta económica: si ya aporté 6 períodos y
todavía no cobré, ¿cuánto me corresponde? Si ya cobré y me quiero ir sin seguir
aportando, ¿cuánto debo? Ese cálculo es la fuente de la mitad de los conflictos de
un pasanaku y tiene que estar explícito.

`requiereReemplazo` refleja la realidad: en la mayoría de los grupos no te podés
ir sin dejar quién ocupe tu lugar, porque tu salida deja un hueco en la bolsa de
todos los períodos restantes.

**Por qué debe existir.** Sin ella, la salida es un cambio de estado sin cálculo,
sin aprobación y sin constancia de lo acordado.

---

### `SolicitudIngreso` / `solicitud_ingreso`

**Qué es.** La postulación de un usuario a un grupo abierto.

**Para qué sirve (negocio).** Es el filtro de entrada de los grupos públicos o
por enlace. `puntajeCompatibilidad` y `evaluarAutomaticamente()` permiten que el
sistema pre-evalúe: reputación suficiente, sin deuda abierta, capacidad declarada
compatible con el aporte, sin restricciones vigentes (M1). El organizador —humano
o digital— recibe la solicitud ya calificada en vez de tener que investigar a cada
postulante.

**Por qué debe existir.** Un grupo con desconocidos necesita una puerta. Sin
solicitud formal, o el grupo es cerrado (y la plataforma no puede crecer más allá
de las redes existentes) o es abierto sin filtro (y se llena de morosos que ya
fallaron en otros grupos).

**A nivel de sistema.** Consulta `historial_incumplimiento_usuario` (M8) y
`puntaje_reputacion` (M6) al evaluar. Es el punto donde el mal historial
cross-grupo tiene efecto real.

---

### `Invitacion` / `invitacion`

**Qué es.** La invitación a un teléfono concreto para unirse a un grupo, con su
enlace firmado.

**Para qué sirve (negocio).** Es **el canal de crecimiento principal**. Así es
como realmente se arma un pasanaku: alguien manda el enlace al grupo de WhatsApp
de la familia o del trabajo. La invitación se hace a un `telefonoInvitado`, no a
un usuario registrado, justamente porque la mayoría de los invitados todavía no
existe en la plataforma — la invitación es lo que los trae.

`enviosRealizados` y `reenviar()` reconocen algo cotidiano: la gente no contesta a
la primera. `fechaExpiracion` (típicamente 72 h) evita que un enlace circule
indefinidamente y que alguien se cuele a un grupo que ya arrancó.

**Por qué debe existir.** Sin invitación como entidad, no se puede medir el embudo
(cuántos invitados → cuántos aceptan), ni revocar un enlace mandado por error a un
número equivocado, ni saber quién invitó a quién — que es la cadena de confianza
que el aval del módulo 8 necesita.

**A nivel de sistema.** `token_id` apunta a `TokenEnlaceFirmado` (M1): el enlace
es de un solo uso y está firmado con HMAC, así nadie puede alterar el `grupo_id`
en la URL para colarse a otro grupo.

---

### `EnlaceGrupo` / `enlace_grupo`

**Qué es.** El enlace —y su QR— que el organizador comparte para que **cualquier
persona** pida unirse al grupo, sin tener que invitar teléfono por teléfono.

**Para qué sirve (negocio).** Es como se arma un pasanaku en la práctica: el
organizador pega un enlace en el grupo de WhatsApp de la familia o muestra el QR en
persona. Como el enlace se puede reenviar a desconocidos, **no da entrada**: cada
pedido es una `solicitud_ingreso` que el organizador aprueba (decisión del dueño,
2026-10-19). Vence a los 7 días o al llenarse el grupo, y el organizador lo puede
anular cuando quiera.

**Por qué debe existir.** Sin entidad propia no se podría anular un enlace que se
filtró, ni saber por qué enlace llegó cada solicitud (`solicitud_ingreso.enlace_grupo_id`),
ni garantizar que el grupo tenga un solo enlace vivo.

**A nivel de sistema.** Se guarda **solo el hash** del código (`token_hash`, único):
con la base en la mano no se reconstruyen enlaces vivos. `estado` es `ACTIVO` o
`ANULADO`; generar uno nuevo anula el anterior en la misma transacción.

---

## Paquete: Calendario y Turnos

### `Periodo` / `periodo`

**Qué es.** Un ciclo de aporte del grupo: del 1 al N, con sus fechas de
vencimiento y gracia, y cuánto se recaudó.

**Para qué sirve (negocio).** Es la unidad de tiempo del pasanaku y el punto de
control financiero. `montoObjetivo` vs `montoRecaudado` responde la pregunta que
más importa cada mes: **¿está completa la bolsa para poder entregarla?**
`brechaFinanciamiento()` es la cifra que dispara todo el módulo 8 — si faltan
Bs 500, o alguien los pone, o el fondo de garantía los cubre, o el beneficiario
cobra menos.

Las tres fechas (`fechaLimitePago`, `fechaFinGracia`, `fechaEntregaPrevista`)
existen porque son tres momentos distintos con consecuencias distintas: pasada la
primera se es impuntual, pasada la segunda se entra en mora formal y se activa la
cobranza, y en la tercera hay que entregar sí o sí.

`cuposMorosos` es un contador denormalizado a propósito: el panel de
transparencia (M6) y las alertas de riesgo lo consultan constantemente.

**Por qué debe existir.** Sin períodos materializados, el calendario sería un
cálculo en tiempo de ejecución y no se podría registrar qué pasó realmente en cada
uno: cuánto entró, quién falló, si se entregó a tiempo.

**A nivel de sistema.** `UNIQUE (grupo_id, numero)`. `fecha_limite_pago` indexada
porque el barrido diario de vencimientos (M8) la usa.

---

### `Turno` / `turno`

**Qué es.** El derecho de un cupo a cobrar la bolsa en un período determinado.

**Para qué sirve (negocio).** Es **el activo más disputado del pasanaku**.
Cobrar primero vale más que cobrar último: quien cobra en el período 1 recibe un
préstamo sin interés; quien cobra en el 12 hizo un ahorro forzado. Esa asimetría
es la razón de que existan la subasta (`descuentoSubasta`), las permutas y las
peleas por el orden.

`criterioAsignacion` guarda **cómo** se asignó ese turno (sorteo, orden de
ingreso, reputación, subasta, acuerdo). Es información que el participante tiene
derecho a ver: no es lo mismo "me tocó el último por sorteo" que "me pusieron
último".

`montoEstimadoCobro` es la expectativa; el monto real se calcula en la entrega
(M4) descontando deducciones. Tenerlo acá permite mostrarle al participante desde
el día uno cuánto va a recibir y cuándo.

**Por qué debe existir.** Es el calendario de derechos de cobro. Sin él no hay
plataforma: es literalmente el producto.

**A nivel de sistema.** `UNIQUE (grupo_id, orden_asignado)` y `UNIQUE (grupo_id,
cupo_id)`: un cupo cobra exactamente una vez por ciclo. Trigger de coherencia: el
cupo debe pertenecer al mismo grupo que el período.

---

### `SorteoTurnos` / `sorteo_turnos`

**Qué es.** El sorteo del orden de cobro, con esquema criptográfico
*commit-reveal* verificable.

**Para qué sirve (negocio).** **Esta entidad existe por una sola razón: el orden
de cobro es el punto de desconfianza número uno del pasanaku.** Todo el mundo
sospecha que el organizador acomodó el sorteo para que él o su gente cobre
primero. En el pasanaku presencial esto se resuelve sorteando delante de todos con
papelitos en una bolsa. Digitalmente hay que reconstruir esa garantía.

El mecanismo:
1. **Antes** de sortear, el sistema publica `hashSemillaPrevio` — el hash de una
   semilla que todavía no reveló. Queda comprometido: ya no puede cambiarla.
2. Se mezcla con `semillaPublica`, un valor que los propios participantes aportan
   y que el servidor no controla.
3. Después del sorteo se revela `semillaServidor`. **Cualquier participante puede
   recomputar el orden y verificar que nadie lo manipuló.**

`testigos` registra a quiénes se notificó el compromiso previo. `repetirPorAnulacion()`
cubre el caso de un sorteo anulado por error, dejando constancia de que hubo dos.

**Por qué debe existir.** Sin verificabilidad, el sorteo es "confiá en nosotros",
que es exactamente lo que la plataforma promete reemplazar. La entidad no es un
lujo criptográfico: es el argumento comercial.

**A nivel de sistema.** `resultado` en JSONB con el mapeo cupo→posición.
`semilla_servidor` es `NULL` hasta el momento de revelar — el orden temporal está
codificado en el esquema.

---

### `SolicitudPermuta` / `solicitud_permuta`

**Qué es.** El pedido de intercambiar turnos entre dos cupos, con la compensación
que se ofrece.

**Para qué sirve (negocio).** Cubre una necesidad real y frecuente: a alguien se
le presentó un gasto urgente (una operación, una matrícula) y necesita cobrar
antes; a otro no le urge y acepta cambiar, a veces a cambio de una compensación.
En el pasanaku presencial esto se arregla hablando; el problema es que después
nadie se acuerda de lo acordado.

El flujo de tres aprobaciones (`solicitante → contraparte → organizador`) es
deliberado: **la permuta no es un asunto privado entre dos**. Cambiar el orden
altera el riesgo del grupo entero (si el que ahora cobra primero es el más
riesgoso, todos quedan más expuestos). Por eso el organizador —o el grupo por
acuerdo— tiene que validarla.

`compensacionOfrecida` reconoce que estos intercambios tienen precio, y lo
registra en vez de dejarlo en un acuerdo verbal.

**Por qué debe existir.** Sin ella, o se prohíben las permutas (y el producto es
rígido frente a una necesidad real) o se permiten sin registro (y aparecen
disputas sin prueba).

---

### `DiaNoHabil` / `dia_no_habil`

**Qué es.** El calendario de feriados nacionales, departamentales o propios del
grupo.

**Para qué sirve (negocio).** Si el día de cobro cae feriado, el banco no procesa
y la transferencia no se acredita. Cobrar mora a alguien porque el 6 de agosto no
había atención bancaria es injusto y genera reclamos legítimos. `siguienteHabil()`
corre las fechas automáticamente.

El alcance departamental importa en Bolivia: cada departamento tiene su efeméride,
y un grupo con gente de La Paz y Santa Cruz no comparte todos los feriados.

**Por qué debe existir.** Es la diferencia entre un sistema que entiende el
contexto y uno que castiga a la gente por el calendario.

---

## Paquete: Conformación Automática (RF-19)

> **Por qué existe este paquete.** El crecimiento por invitación tiene un techo:
> solo alcanza a las redes que ya existen. Para crecer más allá, la plataforma
> tiene que poder **armar grupos entre desconocidos compatibles**. Eso es el
> emparejamiento automático, y es riesgoso: juntar desconocidos sube la
> morosidad. Estas cuatro entidades existen para hacerlo con criterio.

### `PostulacionEmparejamiento` / `postulacion_emparejamiento`

**Qué es.** La declaración de un usuario de que quiere entrar a *algún* grupo con
ciertas características, sin tener uno concreto en mente.

**Para qué sirve (negocio).** Es la demanda en el marketplace. La persona dice:
quiero aportar entre Bs 300 y Bs 500, mensual, empezando en marzo, y prefiero
cobrar temprano. `preferenciaTurno` es un campo pequeño con mucho valor: quien
quiere cobrar temprano busca liquidez (está pidiendo un préstamo), quien prefiere
tarde busca ahorrar. Emparejar dos perfiles complementarios hace grupos más
estables que emparejar doce personas que todas quieren cobrar primero.

**Por qué debe existir.** Sin postulaciones no hay pool de demanda y el
emparejamiento no tiene con qué trabajar. `vigenteHasta` evita proponer grupos a
gente que ya perdió el interés hace meses.

---

### `CriterioEmparejamiento` / `criterio_emparejamiento` — Política

**Qué es.** Los pesos con los que el motor decide qué tan compatibles son dos
postulaciones: reputación, monto, geografía, historial común.

**Para qué sirve (negocio).** Permite calibrar el algoritmo según lo que la
operación vaya aprendiendo. Si se detecta que los grupos geográficamente dispersos
fallan más, se sube `pesoGeografia` sin tocar código. `maxMorososPorGrupo` es el
control de riesgo más directo: no se arma un grupo con cuatro personas de historial
dudoso, aunque individualmente pasen el mínimo.

**Por qué debe existir.** Un algoritmo de emparejamiento con constantes
hardcodeadas no se puede ajustar frente a la evidencia. `vigenteDesde` permite
saber con qué criterio se armó cada grupo histórico.

---

### `PropuestaGrupo` / `propuesta_grupo`

**Qué es.** Un grupo candidato: un conjunto de postulaciones que el motor
considera compatibles, todavía sin confirmar.

**Para qué sirve (negocio).** Es el paso intermedio imprescindible: **el sistema
propone, la gente acepta**. Nadie debe ser metido automáticamente a un grupo con
desconocidos. La propuesta se notifica a los candidatos, se cuentan las
aceptaciones (`propuesta_postulacion.acepto`), y solo si se llega al cupo completo
antes de `expiraEn` la propuesta se `materializa()` en un `Grupo` real.

`puntajeCohesion` y `riesgoEstimado` son los números que se le muestran al
candidato para que decida con información: "este grupo tiene riesgo estimado bajo,
9 de 12 personas ya aceptaron".

**Por qué debe existir.** Sin propuesta como entidad separada, un emparejamiento
fallido (5 aceptaron, 7 no) dejaría grupos a medio armar en la tabla real.

---

### `propuesta_postulacion` (tabla puente)

**Qué es.** La relación entre una propuesta y cada postulación invitada, con su
respuesta.

**Para qué sirve (negocio).** Guarda quién aceptó y quién no, y cuándo. Permite
medir la tasa de conversión del emparejamiento y reintentar con los que no
respondieron.

---

### `MotorEmparejamiento` — Servicio de dominio

**Qué es.** El algoritmo que busca candidatos, propone grupos y puede asignar
turnos por reputación.

**Para qué sirve (negocio).** `explicarEmparejamiento()` merece atención: la
persona tiene derecho a saber **por qué la juntaron con esa gente**. Un
emparejamiento opaco genera desconfianza justo en el momento en que más falta
hace.

---

## Paquete: Gobernanza del Grupo

### `Acuerdo` / `acuerdo`

**Qué es.** Una decisión colectiva sometida a votación: expulsar a alguien,
cambiar la fecha de cobro, condonar una mora, disolver el grupo.

**Para qué sirve (negocio).** **Esta es la entidad que define quién manda en un
pasanaku digital, y la respuesta es: el grupo, no el organizador.**

Las ocho decisiones tipificadas en `TipoAcuerdo` tienen algo en común: todas
afectan el dinero o los derechos de terceros. Expulsar a un moroso libera su cupo
pero deja un hueco en la bolsa. Condonar una mora significa que los demás
absorben esa pérdida. Cambiar el monto altera el compromiso que cada uno asumió.
Ninguna de esas puede ser decisión unilateral de una persona, por más organizadora
que sea.

En el pasanaku presencial esto se resuelve en reunión. Digitalmente, `Acuerdo` es
la reunión: se propone, se abre la votación, se cuenta con quórum, y **queda
registrado como prueba ante disputas**. Cuando el expulsado reclame, la respuesta
es: se votó, hubo quórum de 8 sobre 12, y estos son los votos.

**Por qué debe existir.** Sin acuerdos, la plataforma le entrega al organizador un
poder que en el pasanaku real nunca tuvo, y reproduce digitalmente el abuso que
promete evitar.

**A nivel de sistema.** `referencia_afectada_id` es polimórfica según `tipo`:
`participante.id`, `turno.id` o `registro_incumplimiento.id` (M8). Muchas
entidades de otros módulos exigen un
`acuerdo_id` para poder ejecutarse.

---

### `VotoParticipante` / `voto_participante`

**Qué es.** El voto individual sobre un acuerdo, con su peso.

**Para qué sirve (negocio).** El campo `peso` es la sutileza importante: **vale
según cuántos cupos tenés**. Quien puso dos manos arriesga el doble y por eso pesa
el doble. Es la regla que la gente aplica intuitivamente en los pasanakus reales, y
que un voto por cabeza contradiría.

`comentario` permite que quede constancia del argumento, no solo del sentido del
voto. En una disputa posterior, eso importa.

**Por qué debe existir.** Sin votos individuales solo hay un contador agregado, y
no se puede verificar el resultado ni detectar votos inválidos (de participantes
suspendidos, por ejemplo).

**A nivel de sistema.** `esValido()` chequea que el participante estuviera activo
al momento de votar: un expulsado no vota su propia expulsión.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `Grupo` + `ConfiguracionGrupo` | No hay contrato: cada grupo tendría las mismas reglas y la plataforma sirve a un solo segmento. |
| `Reglamento` + `Aceptacion` | No se puede sancionar a nadie: no consta que haya aceptado las reglas. |
| Separación `Participante` / `Cupo` | Se rompen las manos múltiples, las medias manos y el reemplazo sin descuadrar el calendario. |
| `TraspasoCupo` | Nadie sabe quién se quedó con la deuda ni con el derecho de cobro tras un reemplazo. |
| `Periodo` | No se puede saber si la bolsa está completa antes de entregar (RN-05). |
| `Turno` | No hay producto. |
| `SorteoTurnos` | El orden de cobro vuelve a ser "confiá en el organizador". |
| Emparejamiento (RF-19) | El crecimiento tiene techo en las redes sociales ya existentes. |
| `Acuerdo` + `Voto` | El organizador concentra un poder que en el pasanaku real nunca tuvo. |


---

## Tablas incorporadas después de la redacción original

> Fichas escritas para el catálogo de datos (trabajo `docs/trabajo/2026-10-16-tableros-por-rol`, H7). Cada
> afirmación sale de la fuente que se cita al pie de la ficha: no agregan reglas, las explican.

### `AltaGrupo` / `alta_grupo`

**Qué es.** El registro de que una persona creó un grupo con una clave de idempotencia dada.

**Para qué sirve (negocio).** Que tocar dos veces «Crear grupo» (o que la app reintente por mala señal) deje un solo grupo, y que la segunda vez la persona vea el mismo grupo creado en lugar de un error.

**Por qué debe existir.** Sin ella, un reintento duplicaría el grupo con sus cupos y su reglamento. Tampoco se podría distinguir un reintento legítimo de un pedido distinto mandado con la misma clave.

**A nivel de sistema.** Append-only. `clave_idempotencia` única; `huella_solicitud` es el hash del pedido original y una clave repetida con otra huella se rechaza. El alta se serializa por creador con un `pg_advisory_xact_lock`. `fondo_por_periodo` guarda el importe pactado con que se creó. La política RLS de `participante` la consulta para que el creador vea a los participantes de su grupo (`Restricciones.md`, bloque de políticas por `usuario_id`).

*Fuente: `servicios/grupos/.../CreacionRepositorio.java`; CU-68.*

### `DecisionIngreso` / `decision_ingreso`

**Qué es.** Cada decisión humana sobre una solicitud de ingreso a un grupo, en dos fases: la propuesta y la resolución.

**Para qué sirve (negocio).** Que quién entra a un grupo de plata sea una decisión de una persona con nombre y motivo, no del algoritmo. Si la persona se aparta de lo que recomendó el motor, queda dicho.

**Por qué debe existir.** Con la admisión humana obligatoria (ADR-052 DD-2), el organizador propone y backoffice resuelve. Sin esta tabla, la propuesta, la resolución y el motivo se pisarían en la solicitud y no habría forma de reconstruir por qué alguien quedó dentro o fuera.

**A nivel de sistema.** Append-only (R-GRP-17). `fase` ∈ PROPUESTA/RESOLUCION y `decision` ∈ ACEPTAR/RECHAZAR. `revision` crece por solicitud y hay una sola RESOLUCION por solicitud (`uq_decision_ingreso_resolucion`). `recomendacion_algoritmo`, `evidencia_algoritmo`, `version_motor` y `apartamiento` conservan qué dijo el motor y si la persona se apartó. El motivo no puede ser vacío.

*Fuente: CU-68; `docs/Restricciones.md` R-GRP-17; `AdmisionRepositorio.java`.*

### `SnapshotSorteo` / `snapshot_sorteo`

**Qué es.** La foto congelada del plantel, el calendario y las reglas de un grupo en el momento en que se compromete el sorteo de turnos.

**Para qué sirve (negocio).** Que nadie pueda decir que el sorteo se hizo sobre otra lista de participantes o con otras reglas. Cualquiera puede reproducir el orden con la semilla revelada y esta foto.

**Por qué debe existir.** El sorteo publica primero el hash de la semilla y después la revela (CU-60). Si el plantel o las reglas cambiaran entre esos dos momentos, el resultado dejaría de ser verificable. La foto es lo que hace que el sorteo sea reproducible.

**A nivel de sistema.** Append-only (R-GRP-18). Una por sorteo (`sorteo_id` único). `roster`, `periodos` y `reglas` en texto; `hash_snapshot` es SHA-256 (64 caracteres) y `semilla_sellada` acompaña a la foto. Se escribe una sola vez (`SorteoRepositorio.guardarSnapshot`).

*Fuente: CU-60; `docs/Restricciones.md` R-GRP-18.*

### `SustitucionAdministrador` / `sustitucion_administrador`

**Qué es.** El cambio de quién administra un grupo: quién sale, quién entra, por qué y qué obligaciones conserva el que sale.

**Para qué sirve (negocio).** Que un grupo no quede sin administrador cuando el organizador se va o es inhabilitado, y que quien sale siga debiendo lo que debía: cambiar de rol no borra la deuda con el grupo.

**Por qué debe existir.** Sin ella, el cambio de administrador sería un `UPDATE` sobre el participante y se perdería quién lo decidió, por qué y qué conservó el saliente.

**A nivel de sistema.** Append-only (R-GRP-19). El saliente y el entrante son participantes distintos del mismo grupo. El motivo no puede ser vacío. `obligaciones_conservadas` guarda en texto la participación y los cupos que conserva el saliente. `clave_idempotencia` única.

*Fuente: CU-68; `docs/Restricciones.md` R-GRP-19; `SustitucionRepositorio.java`.*
