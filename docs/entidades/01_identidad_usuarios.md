# Módulo 1 — Identidad, Usuarios y Seguridad de Acceso

> **Pregunta de negocio que responde este módulo:**
> *¿A quién le estoy confiando la plata del grupo, y cómo pruebo que la persona
> que está operando en la app es realmente esa persona?*

En un pasanaku presencial la identidad se resuelve sola: todos se conocen, son
del barrio, del trabajo o de la familia. En un pasanaku digital esa garantía
desaparece. Alguien puede entrar con un número prestado, cobrar su turno y
desaparecer; o peor, alguien puede tomar control de la cuenta de un participante
justo antes de su turno y desviar la bolsa. Todo este módulo existe para cerrar
esas dos puertas.

---

## Paquete: Núcleo de Identidad

### `Usuario` / `usuario` — Raíz de agregado

**Qué es.** La persona real detrás de la cuenta. Una sola por número de teléfono.

**Para qué sirve (negocio).** Es el sujeto de todo lo demás: quien aporta, quien
cobra, quien organiza, quien debe y quien tiene reputación. Su ciclo de vida
(`PRE_REGISTRO → PENDIENTE_VERIFICACION → ACTIVO`) modela el onboarding real: en
Bolivia la gente se baja la app porque un amigo le mandó un enlace de WhatsApp,
no porque haya decidido "registrarse en una fintech". Por eso existe el estado
`PRE_REGISTRO`: hay que poder guardar un teléfono e invitarlo a un grupo **antes**
de que la persona complete su perfil, sin obligarla a un formulario largo cuando
todavía no entiende qué es la plataforma.

El campo `codigoPublico` existe porque nadie debería mostrar un UUID en pantalla
ni tener que dictar su número de teléfono para que lo agreguen a un grupo. Es el
identificador que la persona lee en voz alta.

`idioma` con quechua y aymara no es decorativo: buena parte del mercado real de
pasanakus está en poblaciones donde el español es segunda lengua, y un
recordatorio de cobro que no se entiende es un recordatorio que no cobra.

`nivelKYC` está en el usuario (y no solo en la tabla de verificaciones) porque es
la variable que decide, en tiempo de operación y sin joins, cuánto puede mover
esa persona. `limiteOperativoVigente()` es la función que impide que alguien sin
documento verificado entre a un grupo de Bs 5.000 mensuales.

**Por qué debe existir.** Es el ancla de todo el sistema. Sin ella no hay
reputación portable entre grupos, que es justamente la propuesta de valor: que tu
buen historial en un pasanaku te sirva para entrar al siguiente.

**A nivel de sistema.** `telefono_e164` es `UNIQUE` — implementa RN-01 (teléfono
único). `estado` va indexado porque casi toda consulta operativa filtra por
usuarios activos. Lleva `version` para bloqueo optimista y `eliminado_en` para
borrado lógico: un usuario con obligaciones abiertas jamás se borra físicamente.

---

### `Telefono` — Objeto de valor

**Qué es.** El número de teléfono normalizado a formato internacional E.164.

**Para qué sirve (negocio).** El teléfono es la identidad primaria del sistema:
es el canal de cobro (WhatsApp), el de verificación (OTP) y el de invitación. Que
sea un objeto de valor y no un `VARCHAR` suelto resuelve un problema muy concreto
y muy caro: la misma persona escribe su número de cinco maneras distintas
(`70123456`, `+591 70123456`, `591-70-12-34-56`). Si no se normaliza en un solo
lugar, se crean cuentas duplicadas, se envían invitaciones a números que "no
existen" y una persona termina figurando dos veces en el mismo grupo.

`enmascarar()` existe por una razón de negocio, no técnica: cuando el sistema
dice "te enviamos un código al +591 7\*\*\*4321", el usuario confirma que es su
número sin que la pantalla exponga el número completo a quien esté mirando.

**Por qué debe existir.** Concentra la validación y la normalización. Sin él, esa
lógica se repite en el registro, en la invitación, en la notificación y en la
cobranza — y se desincroniza.

**A nivel de sistema.** Se persiste embebido en `usuario.telefono_e164`. La
operadora se guarda porque el costo y la tasa de entrega de un SMS varían por
operadora, dato que el módulo 5 usa para decidir por dónde mandar.

---

### `CorreoElectronico` — Objeto de valor

**Qué es.** El correo, opcional, normalizado y enmascarable.

**Para qué sirve (negocio).** Es el canal de respaldo cuando WhatsApp y SMS
fallan, y el canal por el que se entregan documentos formales: constancias,
certificados de reputación, estados de cuenta. Es **opcional a propósito**: exigir
correo en el registro expulsa a una porción importante del público objetivo, que
opera solo con teléfono.

**Por qué debe existir.** Separar el correo del `Usuario` permite validarlo y
enmascararlo con las mismas reglas en todos lados, y deja explícito que la
plataforma no depende del correo para funcionar.

**A nivel de sistema.** `UNIQUE` pero `NULL`-able: dos usuarios sin correo no
colisionan.

---

### `Direccion` / `direccion_usuario` — Objeto de valor

**Qué es.** Dónde vive o trabaja la persona, con coordenadas opcionales.

**Para qué sirve (negocio).** Tres usos reales. Primero, el **emparejamiento
automático** (RF-19): los pasanakus funcionan mejor entre gente de la misma zona,
porque la presión social es lo que hace que la gente pague; agrupar desconocidos
de ciudades distintas sube la morosidad. Segundo, la **cobranza**: la etapa
prejudicial del módulo 8 contempla visita domiciliaria, y sin dirección no hay
visita. Tercero, **cumplimiento**: la normativa de prevención de lavado exige
domicilio declarado a partir de ciertos montos.

**Por qué debe existir.** Si la dirección viviera dentro de `usuario`, cinco
campos más engordarían la tabla más consultada del sistema para un dato que se
usa esporádicamente. Además la relación 1-a-1 opcional deja claro que un usuario
puede operar sin dirección hasta cierto nivel de KYC.

**A nivel de sistema.** `usuario_id` es `UNIQUE`: una dirección por usuario.
Latitud/longitud en `DECIMAL(9,6)`, precisión de metros, suficiente para
agrupamiento por zona.

---

### `PerfilFinanciero` / `perfil_financiero`

**Qué es.** Lo que la persona declara sobre su capacidad económica: ocupación,
ingreso mensual, cuánto puede aportar, de dónde sale la plata, y si es PEP
(persona expuesta políticamente).

**Para qué sirve (negocio).** Aquí se previene el fracaso más común y más
silencioso del pasanaku digital: **gente que se compromete a un aporte que no
puede sostener**. En el pasanaku de oficina esto lo filtra el sentido común
colectivo ("¿vos vas a poder con Bs 1.000 al mes?"); en una plataforma que
empareja desconocidos, ese filtro hay que construirlo.
`validarCapacidad(montoAporte)` es la función que le advierte a la persona —y al
grupo— que el compromiso no cierra con lo que ella misma declaró.

`esPEP` y `fuenteIngresos` son requisitos de cumplimiento antilavado: una
plataforma que mueve aportes recurrentes de muchas personas es exactamente el
vehículo que un regulador vigila.

**Por qué debe existir.** Es información autodeclarada y de baja frecuencia de
cambio, con implicaciones legales distintas a las de los datos de contacto. Se
mantiene separada para poder auditar cuándo se declaró qué, y para poder
retenerla o anonimizarla con reglas propias (M9).

**A nivel de sistema.** 1-a-1 con usuario. `actualizado_en` explícito porque un
ingreso declarado hace tres años no sirve para autorizar un límite hoy.

---

### `SolicitudBaja` / `solicitud_baja`

**Qué es.** El pedido formal de un usuario de dejar la plataforma.

**Para qué sirve (negocio).** Traduce un derecho (irse cuando quiera) a un
proceso que no puede atropellar a terceros. El campo clave es
`bloqueadaPorObligaciones`: **nadie puede darse de baja para escapar de una deuda
o de un grupo en curso**. Si el usuario tiene aportes pendientes, deuda con el
fondo de garantía o un turno por cobrar, la baja queda registrada pero con fecha
efectiva condicionada. Esto protege a los otros diez participantes que siguen
poniendo su plata contando con él.

También sirve para separar la baja de la eliminación de datos: irse de la
plataforma no es lo mismo que ejercer el derecho de cancelación de datos
personales, que se tramita en el módulo 9 y tiene sus propias excepciones
legales.

**Por qué debe existir.** Sin esta entidad, la baja es un `UPDATE` de estado y se
pierde la razón, la fecha de solicitud y el motivo del bloqueo. Ante un reclamo
("pedí darme de baja y me siguieron cobrando"), esto es la prueba.

**A nivel de sistema.** `verificarObligacionesAbiertas()` cruza contra M2
(participaciones activas), M3 (obligaciones pendientes) y M8 (deuda vigente).

---

## Paquete: Tokens de Verificación y Credenciales

> **Por qué los tokens son un agregado propio y no tres campos en `usuario`.**
> Este es probablemente el diseño menos obvio del módulo. La tentación es guardar
> `codigo_otp`, `expira_en` e `intentos` directamente en el usuario. El problema:
> el mismo mecanismo se necesita para verificar el teléfono, para recuperar
> contraseña, para invitar a un grupo por WhatsApp, para el enlace de pago en un
> toque, para confirmar la recepción de una entrega y para firmar el reglamento.
> Son seis flujos con TTLs, canales y políticas de seguridad completamente
> distintos. Modelarlos una sola vez, bien, evita seis implementaciones que se
> desincronizan.

### `TokenVerificacion` / `token_verificacion` — Raíz de agregado (abstracta)

**Qué es.** Cualquier credencial de un solo uso y vida corta que el sistema emite
para probar que alguien controla un canal (teléfono, correo) o para autorizar una
acción puntual.

**Para qué sirve (negocio).** Es el mecanismo con el que la plataforma
**convierte "dice que es él" en "probó que es él"** sin exigir contraseña en cada
paso. Concretamente habilita:

- el registro sin fricción (código por WhatsApp en vez de formulario largo);
- la invitación a un grupo por enlace (RF-19, M2);
- el **pago en un toque** desde el recordatorio de WhatsApp (RF-16, M5) — el
  participante toca el enlace y llega directo al QR de su aporte, sin login;
- la confirmación de que el beneficiario recibió su bolsa (M4);
- la firma del reglamento del grupo con sello de tiempo (M2);
- la aceptación de un aval solidario (M8), que es un compromiso económico real y
  necesita constancia de quién aceptó y cuándo.

**Por qué debe existir.** Porque el punto de ataque número uno de una app de
dinero es el flujo de recuperación de cuenta. Los campos `intentosFallidos`,
`maxIntentos`, `reenvios`, `ipOrigen` y `dispositivoId` no son burocracia: son
lo que impide que alguien pruebe códigos de 6 dígitos hasta entrar a la cuenta de
un participante la semana de su turno.

`claveIdempotencia` resuelve un problema cotidiano y no de seguridad: el usuario
toca dos veces "Enviar código" porque la red va lenta. Sin idempotencia se emiten
dos códigos, el primero se invalida, la persona recibe el primero por SMS y
escribe ese — y el sistema lo rechaza. Es una de las causas más frecuentes de
abandono en el registro.

**A nivel de sistema.** Herencia por tabla única (STI) con discriminador
`tipo_token`. `hash_token` es `UNIQUE`: una colisión es literalmente un intento de
replay. **Nunca se persiste el valor plano**, solo su hash con *pepper* del
servidor. Índice parcial recomendado:
`UNIQUE (usuario_id, proposito) WHERE estado IN ('EMITIDO','ENVIADO')` — garantiza
un solo token vigente por propósito.

---

### `TokenOTP`

**Qué es.** El código numérico de 6 dígitos que llega por SMS o WhatsApp.

**Para qué sirve (negocio).** Es el verificador universal para el público que la
plataforma atiende: no requiere app adicional, no requiere correo, funciona en
cualquier teléfono. `permiteAutollenado` (SMS Retriever / WebOTP) existe porque
cada dígito que el usuario tiene que copiar a mano es un porcentaje de abandono
en el registro.

**Por qué debe existir.** Separarlo permite que el OTP tenga su longitud y su
formato propios sin contaminar el diseño de los enlaces firmados.

---

### `TokenEnlaceFirmado`

**Qué es.** Un enlace de un solo uso, firmado con HMAC, que ejecuta una acción sin
que el usuario inicie sesión.

**Para qué sirve (negocio).** Es la pieza que hace posible la promesa central del
módulo 5: **cobrar por WhatsApp**. El participante recibe "Tu aporte de Bs 500
vence mañana — [Pagar]" y con un toque llega a su QR ya generado. Cada paso extra
(abrir la app, recordar la contraseña, buscar el grupo) baja la tasa de pago
puntual. `clicks` y `primerClickEn` permiten medir exactamente eso: cuánta gente
abre el enlace y cuánta termina pagando.

**Por qué debe existir.** Un enlace con parámetros en la URL sin firma es un
agujero: cualquiera podría cambiar el ID y ver o pagar la obligación de otro. La
firma HMAC y el uso único son lo que hace que el enlace sea seguro para mandar por
un canal que no controlamos.

---

### `TokenRefresco`

**Qué es.** La credencial de sesión larga que evita que el usuario tenga que
iniciar sesión todos los días.

**Para qué sirve (negocio).** Comodidad con red de seguridad. `familiaId`,
`generacion` y `detectarReuso()` implementan rotación con detección de robo: si
un token ya usado vuelve a aparecer, significa que alguien copió la sesión, y el
sistema revoca **toda la familia** — cierra la sesión del atacante y la del
usuario legítimo, que vuelve a autenticarse. Es preferible molestar a un usuario
que dejar entrar a un intruso a una cuenta con plata.

**Por qué debe existir.** Sin rotación con detección de reuso, un token robado
vale para siempre.

---

### `PoliticaToken` / `politica_token` — Política configurable

**Qué es.** Los parámetros de seguridad de cada tipo de token: cuánto dura,
cuántos intentos, cuántos reenvíos por hora, por qué canales puede salir.

**Para qué sirve (negocio).** Permite **endurecer la seguridad sin desplegar
código**. Un OTP para autorizar un pago debe durar 5 minutos; una invitación a un
grupo debe durar 72 horas porque la gente contesta WhatsApp cuando puede. Si esos
números están hardcodeados, cada ajuste operativo —y se ajustan seguido, sobre
todo cuando aparece un patrón de fraude— es un release.

`maxReenviosPorHora` y `maxEmisionesPorDia` tienen además un efecto directo en
costos: cada SMS se paga. Un bucle de reenvíos mal controlado es una factura
inesperada.

**Por qué debe existir.** Convierte decisiones de seguridad en configuración
auditable y versionada (`vigenteDesde`), en lugar de constantes enterradas.

---

### `IntentoValidacionToken` / `intento_validacion_token`

**Qué es.** El registro de cada vez que alguien intentó usar un código, con el
resultado.

**Para qué sirve (negocio).** Es el detector de ataques y, sobre todo, la
**evidencia** cuando hay un reclamo del tipo "a mí me sacaron la plata de la
cuenta". Con esta tabla se puede decir: hubo 47 intentos fallidos desde una IP de
otro país entre las 2 y las 3 de la mañana. Sin ella, solo se sabe que el token
quedó bloqueado, y no por qué.

**Por qué debe existir.** El contador `intentosFallidos` del token dice *cuántos*;
esta tabla dice *quién, desde dónde y con qué patrón*. Solo lo segundo sirve para
investigar.

**A nivel de sistema.** Alto volumen; candidata a retención acotada por
`politica_retencion` (M9).

---

### `ServicioVerificacion` — Servicio de dominio

**Qué es.** El único lugar del sistema autorizado a emitir, reenviar y validar
tokens.

**Para qué sirve (negocio).** Garantiza que las reglas (invalidar los anteriores,
respetar el cooldown, aplicar la política del propósito) se cumplan siempre, sin
importar si el token lo pidió el registro, la invitación o el flujo de pago.

**Por qué debe existir.** Si cada módulo emite sus propios tokens, cada módulo
implementa su propia versión —incompleta— de las reglas de seguridad.

---

### `CredencialAcceso` / `credencial_acceso`

**Qué es.** La contraseña, guardada como hash con Argon2id/bcrypt.

**Para qué sirve (negocio).** Es el segundo factor de posesión para las
operaciones sensibles y la alternativa cuando el usuario cambia de teléfono. En
una plataforma pensada para operar por OTP, la contraseña no es el mecanismo
principal, pero sí el que protege el panel administrativo y las acciones de alto
impacto.

**Por qué debe existir.** `requiereCambio`, `expiraEn` y `estaComprometida()`
permiten reaccionar cuando aparece una filtración de credenciales de terceros:
forzar cambio a los usuarios afectados sin tocar a los demás.

**A nivel de sistema.** `parametrosKDF` en JSON permite migrar el costo del hash
con el tiempo sin invalidar las credenciales existentes.

---

### `HistorialCredencial` / `historial_credencial`

**Qué es.** Los hashes de las contraseñas anteriores.

**Para qué sirve (negocio).** Impide el ciclo clásico de "cambiar la contraseña y
volver a poner la misma de siempre" cuando se fuerza una rotación por incidente.
Si hubo un compromiso y se obliga a cambiar, reutilizar la contraseña filtrada
deja al usuario igual de expuesto.

**Por qué debe existir.** Sin historial, la política de no reutilización no se
puede aplicar.

---

### `FactorMFA` / `factor_mfa`

**Qué es.** El segundo factor: TOTP, SMS, WhatsApp o códigos de respaldo.

**Para qué sirve (negocio).** Se exige selectivamente, no siempre: para
administradores, para organizadores que administran carteras grandes, y para
acciones críticas como cambiar la cuenta bancaria de cobro o autorizar un
desembolso. Ese es el punto de `Permiso.requiereMFA`.

**Por qué debe existir.** Los `codigosRespaldo` resuelven el caso real de "perdí
el teléfono justo antes de mi turno". Sin ellos, la única salida es soporte
manual, que es a la vez lento y un vector de ingeniería social.

---

## Paquete: Sesiones, Dispositivos y Defensa

### `Sesion` / `sesion`

**Qué es.** Una sesión activa: usuario, dispositivo, desde cuándo, desde dónde.

**Para qué sirve (negocio).** Le da al usuario la pantalla de "dispositivos
conectados" y el botón de "cerrar todas las sesiones". Es la primera reacción de
alguien que sospecha que le entraron a la cuenta, y es también lo que ejecuta el
soporte cuando se reporta un robo de teléfono.

**Por qué debe existir.** Sin sesiones materializadas solo hay tokens: se pueden
revocar, pero no mostrar ni explicar al usuario. La revocación deja de ser una
acción que el usuario puede tomar por sí mismo.

**A nivel de sistema.** `revocada_en` + `motivo_revocacion` en vez de borrado: una
sesión revocada por sospecha de fraude es evidencia.

---

### `Dispositivo` / `dispositivo`

**Qué es.** Cada teléfono o navegador desde el que se ha usado la cuenta.

**Para qué sirve (negocio).** Tres funciones distintas. **Seguridad**: un login
desde un dispositivo nunca visto puede exigir verificación adicional
(`esConfiable`, `PoliticaToken.exigeDispositivoConocido`). **Notificaciones**:
`tokenPush` es la dirección a la que llegan los avisos push del módulo 5.
**Detección de fraude**: varias cuentas operando desde la misma huella de
dispositivo es el patrón típico de participantes ficticios en un grupo, algo que
el módulo 9 vigila.

**Por qué debe existir.** Sin identidad de dispositivo, no se puede distinguir
"el usuario de siempre desde su teléfono de siempre" de "alguien con la
contraseña correcta desde un equipo desconocido".

---

### `IntentoAutenticacion` / `intento_autenticacion`

**Qué es.** El log de cada intento de inicio de sesión, exitoso o no, con un
puntaje de riesgo.

**Para qué sirve (negocio).** Alimenta el bloqueo automático por fuerza bruta y
las alertas de seguridad. `usuarioId` puede ser nulo a propósito: hay que
registrar también los intentos contra identificadores que no existen, porque eso
es exactamente la firma de un ataque de enumeración de usuarios.

**Por qué debe existir.** El `puntajeRiesgo` permite decidir de forma graduada:
no bloquear al usuario que se equivocó dos veces desde su casa, pero sí exigir
MFA al que acierta la contraseña desde una IP nueva en otro país.

---

### `BloqueoCuenta` / `bloqueo_cuenta`

**Qué es.** El bloqueo de acceso, temporal o indefinido, con su motivo.

**Para qué sirve (negocio).** Separa cuatro situaciones que un solo campo
`estado = BLOQUEADO` confunde: bloqueo automático por intentos fallidos (se libera
solo), bloqueo por sospecha de fraude (lo libera cumplimiento), bloqueo pedido por
el propio usuario (perdí el teléfono) y bloqueo por orden administrativa. Cada uno
tiene un responsable de liberación distinto y un discurso distinto hacia el
usuario.

**Por qué debe existir.** `liberadaPor` y `liberadaEn` responden la pregunta que
siempre aparece en la auditoría: *¿quién desbloqueó esta cuenta y con qué
autoridad?*

---

### `RestriccionUsuario` / `restriccion_usuario`

**Qué es.** Una limitación puntual sobre lo que el usuario puede hacer, sin
bloquearle el acceso: no crear grupos, no unirse a nuevos, no ser organizador, o
un tope de monto.

**Para qué sirve (negocio).** Es **la entidad bisagra entre el incumplimiento y
el resto de la plataforma**, y una de las más importantes de todo el modelo. El
escenario que resuelve: alguien abandona un pasanaku debiendo tres cuotas; se le
abre un expediente en M8; ese expediente crea aquí una restricción `NO_UNIRSE`.
Resultado: **no puede entrar limpio al siguiente grupo mientras tenga deuda
abierta**. Esa es, en la práctica, la sanción que más disuade — mucho más que un
puntaje que baja.

Y a diferencia del bloqueo, la persona sí puede seguir entrando: puede ver su
deuda, pagarla y regularizarse. Cerrarle la puerta del todo elimina la vía por la
que podría pagar.

**Por qué debe existir.** Sin ella, la consecuencia del incumplimiento se queda
encerrada en el módulo 8 y no afecta el comportamiento del sistema. La reputación
informa; la restricción impide.

**A nivel de sistema.** `referencia_origen_id` es polimórfica: apunta a
`registro_incumplimiento.id` (M8) cuando `origen = 'INCUMPLIMIENTO'`. Se valida
por aplicación o trigger, no por FK física, para no acoplar M1 con M8.
`vigenteDesde/Hasta` permite restricciones con vencimiento automático.

---

## Paquete: Verificación de Identidad (KYC)

### `DocumentoIdentidad` / `documento_identidad`

**Qué es.** La cédula, pasaporte o carnet de extranjería del usuario, con
imágenes.

**Para qué sirve (negocio).** Es lo que permite subir de nivel operativo. Un
grupo de Bs 200 mensuales entre compañeros de trabajo no necesita documento; un
grupo de USD 1.000 con desconocidos sí, y la normativa lo exige. Es también la
única forma de perseguir una deuda: sin documento, un moroso es un número de
teléfono que se descarta.

**Por qué debe existir.** `hash_numero` es la pieza clave: permite detectar que la
misma cédula está abriendo dos cuentas **sin necesidad de descifrar el número**.
Ese es el patrón de quien quiere volver a entrar después de haber sido restringido
por deuda, y es el que hace que la restricción del punto anterior no sea trivial
de esquivar.

**A nivel de sistema.** `numero_cifrado` con cifrado a nivel de columna
(pgcrypto/KMS); las imágenes en almacenamiento cifrado; `hash_archivo` para
detectar que dos usuarios subieron literalmente la misma foto.

**`url_anverso` y `hash_archivo` admiten nulos, y tienen que admitirlos.** La fila
del documento nace en el alta, con los datos que la persona escribe; las fotos
llegan **después**, en un paso aparte que puede cortarse, reintentarse o quedar a
medias. Mientras fueron `NOT NULL`, el registro tuvo que inventar una clave de
objeto que apuntaba a nada y un hash de sesenta y cuatro ceros sólo para poder
insertar la fila — y entonces la cola de verificación leía «la columna no es nula»
y anunciaba que **todo el mundo** tenía anverso cargado. Una columna obligatoria
que se llena con relleno no hace cumplir nada: sólo convierte «no sé» en una
mentira que el resto del sistema lee como verdad. Nulo significa «todavía no
llegó», y el expediente incompleto se ve como lo que es.

---

### `VerificacionKYC` / `verificacion_kyc`

**Qué es.** El proceso de validar ese documento: envío al proveedor, comparación
biométrica con selfie, resultado, y revisión manual si hace falta.

**Para qué sirve (negocio).** Un documento cargado no es un documento verificado.
Esta entidad separa el *dato* del *proceso*: guarda quién lo validó (proveedor
externo o revisor humano), con qué puntaje biométrico, cuándo, y hasta cuándo vale
esa verificación (`vigenteHasta` — los documentos vencen). Cuando un participante
reclama que le rechazaron el KYC, `motivoRechazo` y `revisadaPor` son la
respuesta.

**Por qué debe existir.** Sin historial de verificaciones no se puede reintentar
sin perder el rastro del rechazo anterior, ni auditar la tasa de aprobación del
proveedor, ni detectar a un revisor que aprueba todo sin mirar.

---

### `ReferenciaPersonal` / `referencia_personal`

**Qué es.** Un contacto de confianza declarado por el usuario, con la relación y
si acepta ser avalista.

**Para qué sirve (negocio).** Formaliza lo que en el pasanaku presencial es
implícito: **alguien responde por vos**. Cumple dos papeles. Primero, verificación
blanda de identidad para usuarios sin documento en regla: si tres personas
verificadas confirman conocerte, eso es señal. Segundo, y más importante, es la
puerta de entrada al **aval solidario del módulo 8**: `aceptaSerAvalista` marca a
quién se le puede pedir que garantice la deuda del avalado.

En la cobranza también es determinante: la etapa temprana del módulo 8 contempla
`AVISO_A_AVALISTA`, y la sola perspectiva de que le avisen a tu cuñado que no
estás pagando mueve más plata que tres recordatorios automáticos.

**Por qué debe existir.** Sin referencias, un moroso sin documento verificado es
inalcanzable. Con ellas, la red social que sostiene al pasanaku real se vuelve
parte del sistema.

---

## Paquete: Autorización (RBAC) y Cumplimiento

### `Rol` / `rol`

**Qué es.** Un conjunto nombrado de permisos: administrador, soporte, auditor,
organizador, participante, invitado.

**Para qué sirve (negocio).** Permite que la misma persona sea organizadora en un
grupo y simple participante en otro, que es lo normal: quien organiza el pasanaku
de la oficina probablemente participa en el de su familia sin organizarlo. El
`ambito` (GLOBAL / GRUPO / ORGANIZACION) es lo que hace posible esa doble vida.

El rol `AUDITOR` merece mención aparte: existe para que alguien pueda **leer todo
sin poder modificar nada**, que es el requisito de una revisión externa o de un
regulador.

**Por qué debe existir.** Sin roles con ámbito, el permiso "expulsar participante"
o se le da a todos los organizadores en todos los grupos, o no se le da a ninguno.

---

### `Permiso` / `permiso`

**Qué es.** Una acción concreta y autorizable: `grupo.crear`, `aporte.exonerar`,
`entrega.autorizar`.

**Para qué sirve (negocio).** Permite ajustar la política de autorización sin
tocar código, y sobre todo declarar **qué acciones exigen segundo factor**
(`requiereMFA`). Exonerar el aporte de alguien, autorizar un desembolso o cambiar
una cuenta bancaria son acciones con impacto económico directo: deben pedir MFA
aunque la sesión ya esté abierta.

**Por qué debe existir.** Sin permisos granulares, la única distinción posible es
"admin / no admin", y toda la operación termina hecha con cuentas de administrador
— que es exactamente lo que un auditor marca como hallazgo crítico.

---

### `AsignacionRol` / `asignacion_rol`

**Qué es.** El vínculo usuario–rol, acotado a un ámbito, con vigencia y rastro de
quién lo otorgó.

**Para qué sirve (negocio).** Responde: *¿quién le dio a esta persona el poder de
autorizar entregas en este grupo, cuándo, y hasta cuándo?* Es la tabla que se
revisa primero cuando algo salió mal y hay que entender cómo alguien pudo hacer lo
que hizo.

`vigenteHasta` habilita accesos temporales: darle a un analista de soporte acceso
al grupo por 48 horas para resolver un caso, y que caduque solo.

**Por qué debe existir.** Un campo `es_admin` en `usuario` no tiene ámbito, no
tiene vigencia, no tiene autor y no se puede revocar con motivo.

**A nivel de sistema.** `ambito_id` es polimórfico: apunta a `grupo.id` (M2)
cuando `ambito = 'GRUPO'`. Se valida por trigger.

---

### `rol_permiso` (tabla puente)

**Qué es.** La relación muchos-a-muchos entre roles y permisos.

**Para qué sirve (negocio).** Permite redefinir qué puede hacer un rol sin
reasignar usuarios uno por uno. Si mañana se decide que los organizadores ya no
pueden condonar mora unilateralmente, se quita el permiso de la tabla y el cambio
aplica a todos los organizadores del sistema.

---

### `Consentimiento` / `consentimiento`

**Qué es.** El registro de que el usuario aceptó un documento: términos,
privacidad, tratamiento de datos, marketing, reglamento del grupo — con la
**versión y el hash** del texto exacto que aceptó.

**Para qué sirve (negocio).** Es la prueba legal. Cuando un participante reclama
"yo nunca acepté que me cobren recargo por mora", la respuesta no es "sí
aceptaste": es *aceptaste la versión 2.1 del reglamento el 14 de marzo a las
19:42 desde la IP tal, y este es el hash del texto que estaba vigente ese día*.
Sin el hash del documento, el consentimiento no prueba nada, porque los términos
cambian con el tiempo.

`revocadoEn` cubre el otro lado: el usuario puede retirar el consentimiento de
marketing sin perder los servicios que sí contrató.

**Por qué debe existir.** Es requisito de la normativa de protección de datos y es
la única defensa ante una disputa sobre las condiciones pactadas.

---

### `PreferenciaNotificacion` / `preferencia_notificacion`

**Qué es.** Por dónde y cuándo quiere el usuario que le escriban.

**Para qué sirve (negocio).** Un cobro que llega por el canal equivocado no
cobra. Y un cobro que llega a las 2 de la mañana genera un opt-out, que es peor:
después ya no se le puede escribir ni siquiera para avisarle que su turno está
listo. El horario de no molestar y el canal preferido protegen la relación con el
usuario.

`estaEnHorarioSilencio()` tiene una excepción deliberada: los eventos
transaccionales (`EventoNotificable.esTransaccional`) la ignoran. Si tu pago fue
rechazado o tu entrega se acreditó, eso se avisa a la hora que sea.

**Por qué debe existir.** Además de la buena práctica, hay una razón económica y
regulatoria: WhatsApp exige opt-in y penaliza a los remitentes con quejas de spam.
Escribirle a quien no quiere ser escrito pone en riesgo el canal para todos los
demás usuarios.

---

## Paquete: Reputación (enlace al Módulo 6)

### `ReputacionUsuario` / `reputacion_usuario`

**Qué es.** La caché de lectura rápida del puntaje de reputación del usuario.

**Para qué sirve (negocio).** El puntaje se muestra en el perfil, en la lista de
candidatos de un grupo, al evaluar una solicitud de ingreso y en el emparejamiento
automático. Son consultas frecuentes y sensibles a latencia. Recalcular el score
desde los eventos (M6) en cada una de esas pantallas sería inviable.

**Por qué debe existir.** Es deliberadamente una **proyección**, no la fuente de
la verdad. La fuente es `evento_reputacion` + `puntaje_reputacion` en M6. Esta
tabla se mantiene sincronizada como espejo. Confundir ambas —y editar el número
aquí— rompería la trazabilidad de por qué alguien tiene el puntaje que tiene.

**A nivel de sistema.** `version_modelo` viaja con el puntaje: sin ella no se
puede explicar un score calculado con una versión anterior del modelo de scoring.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| Núcleo de identidad | No hay reputación portable entre grupos: la propuesta de valor desaparece. |
| Tokens | El registro exige formulario largo, no hay pago en un toque por WhatsApp, y el flujo de recuperación es el agujero por donde entran. |
| Sesiones y dispositivos | El usuario no puede reaccionar ante un robo de cuenta; no se detectan cuentas ficticias. |
| KYC | No se puede operar montos altos, ni cumplir normativa, ni perseguir una deuda. |
| RBAC | Todo se opera con cuentas de administrador. |
| Consentimiento | No hay defensa ante un reclamo sobre las condiciones pactadas. |
| RestricciónUsuario | El incumplimiento del módulo 8 no tiene consecuencia práctica. |
