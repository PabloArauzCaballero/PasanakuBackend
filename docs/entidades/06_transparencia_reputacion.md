# Módulo 6 — Transparencia, Reputación y Confianza

> **Pregunta de negocio que responde este módulo:**
> *¿Por qué debería alguien confiar su plata a un grupo de desconocidos en una
> app? ¿Y cómo hago para que el buen comportamiento en un pasanaku le sirva a la
> persona en el siguiente?*

Este es el módulo que contiene la propuesta de valor de la plataforma. Todo lo
demás —cobrar, entregar, sancionar— también lo hace un cuaderno bien llevado. Lo
que un cuaderno no puede hacer es:

1. **Probar** que nadie manipuló la historia del grupo.
2. **Portar** tu buen historial de un pasanaku al siguiente, incluso con gente que
   no te conoce.

Tres principios de diseño gobiernan el módulo:

> **La reputación es event-sourced.** Nunca se "escribe un número": se acumulan
> eventos inmutables con su impacto, y el puntaje se deriva de ellos.
>
> **El modelo de scoring está versionado.** Se puede recalcular el histórico y
> explicarle a cualquiera por qué tiene el puntaje que tiene.
>
> **La transparencia se prueba, no se promete.** Libro sellado con cadena de
> hashes, verificable por cualquier participante sin tener que creerle a nadie.

---

## Paquete: Motor de Reputación

### `ModeloScoring` / `modelo_scoring` — Política configurable

**Qué es.** La versión del algoritmo que convierte eventos en un puntaje: puntaje
base, mínimo, máximo, decaimiento y ventana histórica.

**Para qué sirve (negocio).** Tres cosas que sin esta entidad son imposibles:

1. **Responder "¿por qué me bajó el score?".** Guardando `modeloVersion` en cada
   puntaje, se puede recalcular exactamente con el modelo que estaba vigente ese
   día. Sin versión, cualquier ajuste del algoritmo reescribe retroactivamente la
   historia de todos y nadie puede auditar nada.
2. **Probar un modelo nuevo en sombra.** `esProduccion = false` permite calcular en
   paralelo y comparar antes de aplicarlo a decisiones reales. Cambiar el scoring
   de golpe puede dejar afuera a miles de usuarios que ayer eran elegibles.
3. **`factorDecaimientoMensual`: los errores viejos pesan menos.** Es una decisión
   de negocio deliberada, no un detalle técnico. Alguien que se atrasó dos veces
   hace tres años y desde entonces es puntual no debería cargar eso para siempre.
   Un sistema sin decaimiento condena a la gente y elimina el incentivo a mejorar.

`minEventosParaScore` evita el problema del usuario nuevo: con dos eventos no se
puede afirmar nada, y mostrar un puntaje "alto" basado en nada es engañoso para
quien lo lee.

**Por qué debe existir.** Sin modelo versionado, el puntaje es un número mágico
que nadie puede explicar ni defender, y el primer reclamo serio lo tumba.

---

### `PesoFactor` / `peso_factor`

**Qué es.** Cuánto pesa cada dimensión dentro del modelo: puntualidad,
permanencia, severidad de incumplimientos, volumen, antigüedad, comportamiento
social.

**Para qué sirve (negocio).** Permite calibrar sin código. Si la operación
descubre que la antigüedad predice mucho menos que la puntualidad, se ajustan los
pesos. `topeAporteAlScore` evita que un solo factor domine: alguien con veinte
grupos completados pero que abandona el vigésimo primero no debería seguir
apareciendo como excelente solo por volumen.

`esPenalizador` distingue los factores que suman de los que restan, lo que importa
para explicarle al usuario qué le conviene hacer.

**Por qué debe existir.** Con pesos hardcodeados no se puede ajustar el modelo con
la evidencia que la operación va acumulando, ni auditar con qué ponderación se
calculó un score histórico.

---

### `ReglaImpactoEvento` / `regla_impacto_evento`

**Qué es.** Cuánto suma o resta cada tipo de evento, y sobre qué factor.

**Para qué sirve (negocio).** Traduce hechos a puntos. Aquí es donde se decide,
explícitamente, que un aporte puntual suma poco y un abandono de grupo resta
mucho.

`multiplicadorPorReincidencia` es la pieza de justicia: **la segunda vez pesa más
que la primera.** Atrasarse una vez le pasa a cualquiera; atrasarse la cuarta vez
es un patrón. Sin multiplicador, cuatro atrasos leves pesan lo mismo que cuatro
atrasos independientes de cuatro personas distintas, lo que no refleja el riesgo
real.

`requiereConfirmacion` protege contra el impacto prematuro: un fraude *sospechado*
no puede destruir la reputación de alguien antes de estar confirmado.

**Por qué debe existir.** Sin tabla de impactos, la fórmula está enterrada en el
código y cambiarla es un release sin trazabilidad de qué cambió y cuándo.

---

### `EventoReputacion` / `evento_reputacion` — **append-only**

**Qué es.** Un hecho reputacional inmutable: aportó puntual, abandonó el grupo,
consumió cobertura, completó un ciclo, recibió una sanción.

**Para qué sirve (negocio).** **Es la fuente de la verdad de toda la reputación.**
El puntaje no es un dato: es una función de estos eventos.

Que sea append-only tiene una consecuencia práctica enorme: **corregir un error no
es editar, es registrar el evento inverso** (`revertidoPorId`). Si se marcó
incorrectamente un aporte como impago y después se descubre que sí había pagado,
no se borra el evento negativo: se registra el positivo que lo revierte. El
historial queda completo y explica el puntaje actual, incluido el error y su
corrección.

Los 19 tipos cubren todo el ciclo: desde `APORTE_ANTICIPADO` (que **suma**, porque
pagar antes merece reconocimiento) hasta `FRAUDE_CONFIRMADO`. Vale la pena notar
`SANCION_REVOCADA` y `DEUDA_RECUPERADA`: **el sistema modela explícitamente la
redención.** Quien se atrasó y después pagó todo tiene un evento positivo que lo
refleja. Un modelo que solo registra lo malo condena a la gente y no le da razones
para regularizarse.

**Por qué debe existir.** Sin eventos, el puntaje es un número que alguien
escribió y que no se puede explicar, auditar ni recalcular con un modelo nuevo.

**A nivel de sistema.** Se revoca `UPDATE`/`DELETE` a nivel de rol de base de
datos, no por convención. `referencia_origen_id` polimórfica:
`obligacion_aporte.id` (M3), `entrega_fondo.id` (M4),
`registro_incumplimiento.id` / `sancion.id` (M8), `acuerdo.id` (M2).
Índice compuesto `(usuario_id, ocurrido_en DESC)` para reconstruir el score en una
sola pasada.

---

### `PuntajeReputacion` / `puntaje_reputacion` — Raíz de agregado

**Qué es.** El puntaje vigente del usuario, con las métricas que lo componen.

**Para qué sirve (negocio).** Es la **proyección materializada** de los eventos.
Existe por rendimiento: mostrar el score en un perfil no puede implicar recorrer
tres años de eventos.

Pero no es solo un número. Los campos que lo acompañan son los que la gente
realmente quiere ver antes de meterse a un grupo con alguien:
`indicePuntualidad`, `gruposCompletados`, `gruposAbandonados`,
`incumplimientosAbiertos`, `antiguedadMeses`. **"820 puntos" no dice nada;
"completó 6 grupos, abandonó 0, 97% de puntualidad" sí.**

`nivelConfianza` traduce el número a lenguaje humano (`SIN_HISTORIAL`,
`EN_OBSERVACION`, `BASICO`, `CONFIABLE`, `MUY_CONFIABLE`, `REFERENTE`,
`RESTRINGIDO`). Es lo que se muestra en la UI, porque un puntaje numérico invita a
comparaciones odiosas y a la ansiedad de ver bajar dos puntos.

`esElegiblePara(grupo)` es la función que conecta con M2: un grupo con
`reputacionMinima = 600` filtra automáticamente.

**Por qué debe existir.** Sin proyección, cada consulta de reputación recalcularía
desde los eventos. Con proyección pero sin eventos, el número no se puede explicar.
Hacen falta las dos.

**A nivel de sistema.** `usuario_id` `UNIQUE`. `reputacion_usuario` (M1) se
mantiene sincronizada como caché de lectura para el login y el perfil.
`proximo_recalculo_en` indexado para el barrido de recálculo.

---

### `ComponenteScore` / `componente_score`

**Qué es.** El desglose del puntaje por factor: valor crudo, normalizado,
contribución y tendencia.

**Para qué sirve (negocio).** Es lo que hace **explicable** el score. Permite
mostrar: "puntualidad: 340 puntos (subiendo); permanencia: 210 puntos (estable);
incumplimientos: -80 puntos (mejorando)". Sin el desglose, el usuario ve un número
que se mueve sin entender por qué, y eso genera desconfianza en el sistema mismo.

`tendencia` es lo que convierte el score en algo accionable: saber que vas
subiendo motiva; saber solo que tenés 640 no.

**Por qué debe existir.** Un score opaco es un score que no se puede defender ante
un reclamo, y que no motiva ningún cambio de comportamiento.

---

### `ContribucionFactor` — Objeto de valor

**Qué es.** El resultado legible de `explicar()`: qué factor, cuánto contribuye, y
qué recomendación.

**Para qué sirve (negocio).** `recomendacion` es el campo con más valor de
producto de toda la entidad: *"si pagás los próximos 3 aportes antes del
vencimiento, subís a nivel CONFIABLE"*. Convierte la reputación de un castigo en
una meta.

---

### `SnapshotReputacion` / `snapshot_reputacion`

**Qué es.** Una fotografía del puntaje en un momento concreto, con el detalle de
sus factores.

**Para qué sirve (negocio).** Congela el estado en los momentos que importan:
- `INGRESO_A_GRUPO`: con qué reputación entró. **Es lo que impide juzgarlo después
  con información que no existía cuando se lo aceptó.**
- `CIERRE_DE_GRUPO`: con qué terminó, para medir el efecto del grupo.
- `AUDITORIA`: estado en la fecha de una revisión.
- `PERIODICO`: serie histórica para ver la evolución.

**Por qué debe existir.** El puntaje vigente cambia; para comparar "antes y
después" o para defender una decisión pasada hace falta la foto de ese momento.

---

### `CertificadoReputacion` / `certificado_reputacion`

**Qué es.** Un documento firmado digitalmente que acredita el historial del
usuario, verificable públicamente.

**Para qué sirve (negocio).** Es **la reputación portable hecha producto** (RF-18).
El usuario puede presentar su certificado a un grupo fuera de la plataforma, a un
prestamista, a una cooperativa, a alguien que le va a fiar mercadería. Para mucha
gente sin historial bancario formal, **este certificado puede ser el primer
documento que acredite que cumple sus compromisos financieros**. Ese es un impacto
real, no una función más.

`codigoVerificacion` + `firmaDigital` + `urlPublica` permiten que un tercero
—sin cuenta en la plataforma— verifique que el certificado es auténtico y no está
revocado.

`expiraEn` es necesario: un certificado de hace dos años no dice nada del presente.

**Por qué debe existir.** Sin certificado, la reputación solo sirve dentro de la
plataforma. Con certificado, sale al mundo y se convierte en un activo de la
persona.

---

### `InsigniaLogro` / `insignia_logro` e `InsigniaOtorgada` / `insignia_otorgada`

**Qué es.** Reconocimientos concretos: `PUNTUAL_12_MESES`, `PRIMER_GRUPO`,
`SIN_MORA`, `ORGANIZADOR_5_ESTRELLAS`.

**Para qué sirve (negocio).** Un puntaje numérico es abstracto y se mueve poco a
poco; una insignia es concreta, se muestra y se persigue. Cumplen dos funciones:
**motivación** (una meta alcanzable frente a un score que sube lentamente) y
**señalización social** (en la lista de un grupo, "12 meses puntual" comunica más
que "score 780").

Separar el catálogo (`InsigniaLogro`) del otorgamiento (`InsigniaOtorgada`)
permite agregar insignias nuevas y evaluarlas retroactivamente.

`revocadaEn` importa: si una insignia se otorgó por un logro que después se
demostró falso —o si el usuario incurre en algo grave—, se revoca con motivo, no se
borra.

**Por qué debe existir.** Sin gamificación, el único incentivo a pagar puntual es
evitar el castigo. Con insignias hay también un incentivo positivo, que es más
sostenible.

---

### `Arena` / `arena`

**Qué es.** Los tramos con nombre en que se parte la escala del puntaje de un modelo
(con el v1: Campamento, Semilla, Brote… hasta Inti). Cada arena es un intervalo
[`desde`, `hasta`) del puntaje; la última incluye su `hasta`.

**Para qué sirve (negocio).** El puntaje es un número continuo y abstracto; la arena lo
vuelve un lugar con nombre, con una barra continua hacia la siguiente, y le da al
organizador una frontera que entiende («este grupo es desde Aguayo»). La arena **no se
gana ni se escribe**: se deriva del puntaje, que a su vez se deriva de eventos. Nadie
sube de arena a mano (regla 91.4.2).

`desde` es incluido y `hasta` excluido, salvo en la última arena del modelo, que incluye
su `hasta` (el máximo). Las arenas cuelgan del modelo: si un modelo nuevo cambia la
escala, trae sus propias arenas en vez de reinterpretar las del anterior.

**Por qué debe existir.** Sin arenas, la «arena mínima» de un grupo sería un número
suelto (`reputacion_minima`) que nadie entiende, y cada pantalla inventaría sus propios
cortes. Con el catálogo en la base, la app, grupos y transparencia leen el mismo corte.

- `codigo`: Código estable de la arena dentro de su modelo (`SEMILLA`, `BROTE`…). Es lo que viaja entre servicios y lo que guarda un grupo como arena mínima.
- `orden`: Posición de la arena en la escala, de la más baja (0) a la más alta. Ordena el camino de arenas en la app.
- `desde`: Puntaje desde el que se está en esta arena, incluido.
- `hasta`: Puntaje donde empieza la siguiente, excluido; en la última arena, el máximo del modelo, incluido.

---

### `DesbloqueoArena` / `desbloqueo_arena`

**Qué es.** Lo que desbloquea una arena (a lo sumo uno por arena): cuántos cupos
totales puede tener como máximo un grupo y cuánto aporte por periodo como máximo, con
su moneda, puede crear o integrar quien está en esa arena. Lleva un `estado`: solo el
`VIGENTE` limita.

**Para qué sirve (negocio).** El desbloqueo es un **parámetro financiero**: limita cuánto
dinero ajeno puede comprometer alguien según su historial, y crece a medida que la
persona sube de arena. Por eso se siembra `PROPUESTO` y solo limita cuando está
`VIGENTE` (regla 97.5.4); mientras tanto se muestra, pero no rechaza. Pasar a `VIGENTE`
se hace en el seeder (`seeders/minimos/16-reputacion-y-scoring.json`, que actualiza la
fila existente por su arena), nunca con un `UPDATE` a mano sobre la base.

**Por qué debe existir.** Sin un tope por arena, quien recién llega podría crear o entrar
a un grupo de cuarenta cupos y aportes altos sin historial que lo respalde. Separarlo de
`arena` deja que el tramo exista sin límite (una arena sin desbloqueo no limita) y que el
límite se confirme por separado.

- `cupos_max`: Cupos totales máximos del grupo que puede crear o integrar quien está en la arena.
- `aporte_max`: Aporte máximo por periodo, en `moneda`, del grupo que puede crear o integrar quien está en la arena.
- `estado`: `PROPUESTO` se muestra pero no limita; solo `VIGENTE` se aplica.

---

### `ServicioReputacion` — Servicio de dominio

**Qué es.** El orquestador: registra eventos, recalcula usuarios, recalcula en
lote al cambiar de modelo, emite certificados y evalúa insignias.

**Para qué sirve (negocio).** `recalcularLote(modeloVersion)` es la operación que
hace viable el versionado: cuando entra un modelo nuevo, se recalcula toda la base
de forma controlada y se puede comparar contra el anterior antes de publicarlo.

---

## Paquete: Panel de Transparencia

### `PanelTransparencia` — Vista (sin tabla propia)

**Qué es.** La vista de solo lectura que le muestra a cualquier participante el
estado completo y verificable de su grupo.

**Para qué sirve (negocio).** **Es la respuesta directa a la desconfianza que mata
los pasanakus.** En el modelo tradicional, solo el organizador sabe quién pagó y
cuánto hay en la bolsa; todos los demás confían. El panel elimina esa asimetría:

- `estadoPagoDeTodos()`: quién está al día y quién no. Es incómodo y es
  exactamente el punto: **la presión social es el mecanismo de cobro más efectivo
  del pasanaku**, y solo funciona si es visible.
- `fondoAcumulado()` vs `fondoEsperado()` vs `brechaDelPeriodo()`: cuánta plata hay
  realmente, sin depender de la palabra de nadie.
- `proximoBeneficiario()` y `calendarioCompleto()`: el orden de cobro a la vista de
  todos, todo el tiempo.
- `selloIntegridad()`: el hash que prueba que ese estado no fue alterado.

**Por qué NO tiene tabla propia.** Es una decisión deliberada y correcta: **se
calcula desde el mayor contable (M3), no duplica datos.** Si tuviera tabla propia,
podría desincronizarse de la contabilidad — y entonces el panel de transparencia
podría mentir, que sería la peor falla posible de este módulo. Se materializa en
vistas para rendimiento, pero la fuente sigue siendo el mayor.

---

### `MetricaGrupo` / `metrica_grupo`

**Qué es.** Indicadores calculados del grupo: tasa de puntualidad, días de mora
promedio, cobertura consumida, rotación.

**Para qué sirve (negocio).** Es el **tablero de salud del grupo**, y sobre todo el
sistema de alerta temprana. `umbralAlerta` + `enAlerta` permiten detectar que un
grupo se está deteriorando antes de que colapse: la mora se concentra, el fondo se
consume rápido, la gente empieza a salirse.

Detectarlo a tiempo permite intervenir (activar un plan de contingencia, M8) en
lugar de reaccionar cuando el grupo ya es inviable y hay que liquidarlo con
pérdidas para todos.

**Por qué debe existir.** Sin métricas persistidas, cada consulta recalcularía
sobre toda la historia del grupo, y no habría serie temporal para ver la
tendencia — que es justamente lo que anticipa el colapso.

**A nivel de sistema.** `UNIQUE (grupo_id, periodo_id, codigo)`. Alimenta el panel
y las alertas tempranas del módulo 8.

---

### `ResumenGrupo` y `FilaEstadoPago` — Objetos de valor

**Qué son.** Las estructuras de lectura que el panel entrega: el resumen del grupo
y una fila por participante.

**Para qué sirven (negocio).** `saludDelGrupo` en `ResumenGrupo` traduce todos los
indicadores a una palabra que cualquiera entiende. `FilaEstadoPago` usa
`participanteAlias`, no el nombre completo: el panel muestra quién está en mora
**sin exponer datos personales innecesarios** al resto del grupo. Ese equilibrio
entre transparencia y privacidad está codificado en el objeto de valor.

---

## Paquete: Libro Sellado y Verificación Pública

### `BloqueTransparencia` / `bloque_transparencia`

**Qué es.** Un bloque sellado de la historia del grupo, encadenado por hash al
anterior y con raíz Merkle de los eventos que contiene.

**Para qué sirve (negocio).** Resuelve la pregunta que ninguna base de datos común
puede responder: ***¿cómo sé que no editaron un pago viejo después de que pasó?***

La respuesta es criptográfica: cada bloque incluye el hash del anterior. Alterar
un registro antiguo cambia el hash de su bloque, lo que invalida el siguiente, y el
siguiente, hasta el último. **Manipular la historia se nota siempre.**

La raíz Merkle permite algo más fino: `pruebaDeInclusion(eventoId)` le da a un
participante la prueba de que **su pago concreto** estaba en el libro antes de que
se cerrara el ciclo, sin necesidad de revelar los pagos de los demás.

`selloExterno` (anclaje a un servicio de timestamping externo) es la vuelta de
tuerca: prueba que el bloque existía en una fecha determinada, incluso contra la
sospecha de que la propia plataforma haya regenerado toda la cadena.

**Por qué debe existir.** Sin sello, la transparencia es una promesa: "confiá en
que no editamos nada". Con sello, es verificable por cualquiera, incluso contra la
plataforma misma. **Esa diferencia es la propuesta de valor del módulo.**

**A nivel de sistema.**
`hash_bloque = H(numero_bloque || hash_bloque_anterior || raiz_merkle ||
periodo_cubierto_hasta)`. `UNIQUE (grupo_id, numero_bloque)`.

---

### `RegistroSellado` / `registro_sellado` — **append-only**

**Qué es.** Cada hecho incluido en un bloque: un pago, una entrega, una cobertura,
un acuerdo, una sanción — con su hash y un resumen público.

**Para qué sirve (negocio).** `resumenPublico` es el campo clave y la decisión más
delicada: contiene lo suficiente para verificar **sin exponer datos personales**.
Se sella que "el cupo 7 aportó Bs 500 el 3 de marzo", no el nombre, el documento ni
la cuenta bancaria de nadie.

Esto permite que la verificación sea genuinamente pública sin convertir el libro en
una filtración de datos.

**Por qué debe existir.** El bloque sella un conjunto; el registro es cada
elemento del conjunto y lo que hace posible la prueba de inclusión individual.

---

### `VerificacionPublica` / `verificacion_publica`

**Qué es.** El endpoint de verificación de un código: constancia de pago,
certificado de reputación o estado de grupo.

**Para qué sirve (negocio).** Es la cara pública de todo el módulo. Alguien que
recibe una constancia de pago o un certificado de reputación —**sin tener cuenta en
la plataforma**— entra el código y comprueba que es auténtico.

Ese detalle es lo que hace que los documentos de la plataforma valgan afuera. Un
PDF sin verificación es una imagen que se edita en dos minutos.

`consultas` y `ultimaConsultaEn` sirven además como señal: un certificado
consultado muchas veces desde muchos lugares puede indicar que está circulando de
forma inesperada.

**Por qué debe existir.** Sin verificación pública, la transparencia solo existe
para quien ya está adentro y ya confía.

---

### `ResenaParticipante` / `resena_participante`

**Qué es.** La calificación de 1 a 5 que un participante le da a otro, por
dimensión (puntualidad, comunicación, organización).

**Para qué sirve (negocio).** Captura lo que los datos duros no ven. Alguien puede
pagar siempre a tiempo y ser insoportable de tratar; alguien puede atrasarse un
día pero avisar siempre y cumplir lo que promete. Esa información existe solo en la
experiencia de los otros participantes.

Es especialmente relevante para evaluar organizadores: `ORGANIZADOR_5_ESTRELLAS` y
`satisfaccionParticipantes` (M7) salen de acá.

`estadoModeracion` es obligatorio, no opcional: **un sistema de reseñas sin
moderación se convierte en un canal de acoso**, sobre todo en un contexto donde hay
conflictos por plata. `moderadaPor` deja constancia de quién decidió publicar u
ocultar.

`dimension` evita el "le pongo 1 estrella porque me cae mal": obliga a calificar
algo concreto.

**Por qué debe existir.** Sin reseñas, la reputación solo mide comportamiento de
pago, y se pierde la dimensión de trato y comunicación que en un grupo colectivo
pesa mucho.

**A nivel de sistema.** `impactarReputacion()` genera un `EventoReputacion`
(`RESENA_POSITIVA` / `RESENA_NEGATIVA`) — pero solo después de moderada, lo que
evita que una reseña maliciosa afecte el score antes de ser revisada.

---

### `AlertaRiesgo` / `alerta_riesgo`

**Qué es.** Una señal automática de que algo anda mal con un usuario, un grupo o
un organizador.

**Para qué sirve (negocio).** Los cuatro códigos son patrones aprendidos:

- `CAIDA_ABRUPTA_SCORE`: alguien que era confiable empezó a fallar. Puede ser un
  problema personal (y ahí conviene ofrecer un plan de pago antes de sancionar) o
  puede ser una cuenta comprometida.
- `MORA_CONCENTRADA`: varios morosos en el mismo grupo. **Es el predictor más
  fuerte de colapso**: cuando la gente ve que otros no pagan, deja de pagar.
- `RETIRO_MASIVO`: varias solicitudes de salida juntas. Algo pasó en ese grupo que
  el sistema no ve.
- `GRUPO_INVIABLE`: los números ya no cierran.

`escalarACumplimiento()` conecta con M9 cuando el patrón huele a fraude y no a
dificultad económica.

**Por qué debe existir.** Sin alertas, los problemas se descubren cuando alguien
reclama —es decir, cuando ya hay daño. Con alertas, hay una ventana para
intervenir: ofrecer un plan, activar un plan de contingencia, reforzar la cobranza.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `ModeloScoring` versionado | No se puede explicar ni defender un puntaje histórico; cualquier ajuste reescribe la historia de todos. |
| `EventoReputacion` (append-only) | El puntaje es un número que alguien escribió; no se puede auditar ni recalcular. |
| `PuntajeReputacion` + `ComponenteScore` | O cada consulta recalcula desde cero, o el score es opaco e indefendible. |
| `SnapshotReputacion` | Se juzga a la gente con información que no existía cuando se la aceptó. |
| `CertificadoReputacion` | La reputación no sale de la plataforma; se pierde el mayor impacto social del producto. |
| `PanelTransparencia` | Vuelve la asimetría: solo el organizador sabe cuánta plata hay. |
| `MetricaGrupo` | Los grupos colapsan sin aviso previo. |
| `BloqueTransparencia` + `RegistroSellado` | La transparencia es una promesa, no una prueba. |
| `VerificacionPublica` | Los documentos de la plataforma no valen nada fuera de ella. |
| `ResenaParticipante` | La reputación solo mide pagos y pierde la dimensión de trato. |
| `AlertaRiesgo` | Los problemas se descubren cuando ya hay daño. |
