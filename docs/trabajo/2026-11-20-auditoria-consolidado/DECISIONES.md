# Decisiones de Pablo para el plan de correcciones (2026-11-21)

Respuestas dadas por Pablo en la sesión. Lo que no está acá sigue abierto.

| ID | Pregunta | Decisión |
|---|---|---|
| P-14 | ¿Se mueve dinero real hoy? | **No.** Todo sigue condicionado al dossier H15. Los S1 son deuda bloqueante antes de producción, no un incidente en curso. |
| P-04 | Respaldo de la empresa (tablas, cuentas 9.x, endpoints, consumidor) | **Eliminar todo.** |
| P-01/P-02 | Faltante y fondo mutual | **Modelo A + D.** A: «Tu garantía vuelve». Cada participante hace un aporte chico extra a un fondo **del grupo**, en una cuenta segregada que nunca es de la empresa. El fondo cubre la cuota de quien no paga; esa persona queda debiendo al fondo (subrogación) y lo que se recupera vuelve al fondo. Al cierre del ciclo se devuelve a cada participante lo que aportó; la deuda que no se pudo recuperar se descuenta de esa devolución, en partes iguales. D: «La puntualidad vale plata». La puntualidad da beneficios (turnos tempranos, menos aporte al fondo, parte de los recargos). Descartados por ahora: B (turno con precio), C (colchón propio) y E (fondo que rinde). |
| P-05 | Recargo por mora | **Va al perjudicado** (quien tuvo que esperar), no a la empresa. |
| — | Parámetros financieros (% del fondo, días de gracia, tasa de recargo) | **Configurables y obligatorios, sin valor por defecto.** Los fijan Pablo y Cumplimiento antes de producción; el servicio no arranca sin ellos. |
| — | Ingreso de la empresa | **Comisión fija por servicio**, transparente e igual para todos. |

## Para validar con Cumplimiento
- Que el fondo del grupo en cuenta segregada no constituya captación de dinero ante ASFI.
- Que la cobertura automática con el fondo del grupo, y la subrogación, no requieran expediente previo (la sanción y la reputación sí lo requieren, regla 91.4).

## Tandas 2 a 4

| ID | Tema | Decisión |
|---|---|---|
| P-03 | Aval entre participantes | **Eliminarlo.** El fondo del grupo (A) cubre esos casos. |
| P-06 | Pagar la cuota con saldo de billetera recargado por QR | **Cuenta como pago por QR.** El dinero entró por un QR confirmado por webhook, y esto permite el pago automático. |
| P-07 | Comprobante manual | **Eliminarlo del todo:** pantalla, endpoint, tabla y canales manuales. |
| P-08 | KYC | **Validación automática por un proveedor.** Una persona de Cumplimiento revisa solo las excepciones y los casos PEP. |
| P-09 | Doble aprobación de retiros | **Automático con controles:** segundo factor, cuenta verificada, enfriamiento tras un cambio de cuenta y monitoreo UIF. Cumplimiento revisa solo lo que dispare una alerta. Requiere un ADR que reemplace a ADR-049. |
| P-10 | Cuatro ojos en la entrega del pozo | **Entrega automática** a la cuenta verificada del beneficiario, con confirmación del proveedor por webhook firmado. |
| P-11 | Organizador profesional | **Eliminarlo.** Todos los grupos los gestiona el sistema. Se quitan postulaciones, habilitación, contrato y evaluación de desempeño. |
| P-12 | Debido proceso | **El sistema declara el incumplimiento** al vencer el plazo de gracia. El descargo lo resuelve Soporte o Riesgos; la apelación, otra persona. |
| — | Admisión a grupos | **Automática con requisitos:** KYC aprobado, reglamento aceptado y sin deudas vivas. El nivel de puntualidad (D) limita a qué turnos se accede. |
| — | Inicio y sorteo | **Automáticos** al completarse los cupos, con sorteo verificable (compromiso y revelación). |
| P-13 | Reembolsos, reversos y cierre de billetera | **Cuatro ojos reales:** dos personas distintas, tomadas del token de cada una y nunca del cuerpo de la petición. |
| P-17 | Descuentos y exenciones de comisión | **Ninguno.** Comisión igual para todos. |
| P-15 | Pasarela QR y proveedor de desembolso | **No hay todavía.** Se define un puerto y un doble con firma real en tres niveles (regla 65). |
| P-16 | Textos nuevos | **Los redacta Claude y Pablo los aprueba** antes de subirlos. |
| P-18 | Orden de ejecución | **Fase 1:** S1 de seguridad e integridad del mayor. **Fase 2:** retirar el respaldo y las aprobaciones. **Fase 3:** QR → webhook → cobro, y el modelo A + D. **Fase 4:** S2 a S4. Un PR por cambio hacia `dev`. |
| — | Revisor | **Pablo** revisa y mergea cada PR. |

Siguen abiertas: P-19 (dónde correr Docker) y P-20 (el candado `blocker_gate`).

## Tanda 5 (producto y operación)

| ID | Tema | Decisión |
|---|---|---|
| — | Entrada a los grupos | **Solo por invitación** de un miembro: entre conocidos. Requisitos: KYC aprobado, reglamento aceptado y sin deudas vivas. No hay grupos públicos. Esto reemplaza lo de «grupos públicos» de la tanda 4. |
| — | Orden de turnos | **Por nivel de puntualidad:** los niveles más altos eligen primero. Dentro del mismo nivel se ordena por sorteo verificable (compromiso y revelación). En el primer ciclo, como todos son «Semilla», el orden sale entero del sorteo. |
| — | Calificación y gamificación | Pieza D, ampliada. La calificación se calcula solo con hechos (pagos confirmados por QR y su fecha), sin calificaciones entre personas. Incluye niveles con beneficios concretos, rachas, insignias y el logro de grupo «Grupo perfecto». La reputación negativa solo nace de un expediente (regla 91.4). Se construye sobre el servicio `transparencia` existente. |
| P-19 | Dónde correr Docker y la integración | **En la máquina de Pablo**, por Remote Control, con permiso en cada caso. |
| P-20 | Las 9 microtareas viejas que traban `blocker_gate` | **Marcarlas DESCARTADO**, con el motivo «reemplazadas por el plan de correcciones del 2026-11-21», en un PR que revisa Pablo. |
| — | Fase 5 «Poder de compra» | Propuesta aceptada como dirección. Es un piloto condicionado a dos cosas: la opinión legal (ASFI y garantía sobre bienes) y al menos un comercio firmado. Fuera de las fases 1 a 4. |

## Tanda 6 (sobre el plan)

| Tema | Decisión |
|---|---|
| «Poder de compra» | **Diseñar ahora, construir después.** En paralelo a F1–F3 se escriben el ADR, el contrato tipo con comercios y el diseño de pantallas. El código arranca cuando haya opinión legal y un comercio firmado. |
| Opiniones legales y de Cumplimiento | **Pablo tiene abogado/Cumplimiento.** El plan deja un documento con las preguntas listas para enviarle. |
| Parámetros financieros | **Configurables desde el portal de administración (backoffice).** Ninguno queda fijo en el código ni en un archivo de configuración. |
| Ejecución del plan | **Sesiones en paralelo por fase**, cada una con su propia rama y su PR a `dev`. Las pruebas con Docker se agrupan y corren en la máquina de Pablo. |

## Tanda 7 (ambigüedades de los anexos)

| Ambigüedad | Decisión |
|---|---|
| Vigencia de los parámetros | **Rigen solo para grupos nuevos.** Cada grupo congela sus parámetros al iniciar, como parte del reglamento que aceptaron sus integrantes. |
| Quién cambia un parámetro | **Dos personas.** Una lo propone y otra distinta lo aprueba, con las dos identidades tomadas del token. |
| D-L2-1 / D-L3-2: subasta y cesión de turno | **Se elimina.** |
| D-L2-2: reemplazo y disolución sin organizador | Se acepta la recomendación: **reemplazo por invitación votado por el grupo** y **disolución votada**, con liquidación calculada desde el mayor. |
| D-L3-1: qué vota el grupo | Se acepta la recomendación: **reemplazo de un participante** y **disolución**. No hay permuta (el orden lo fija el nivel y el sorteo). No hay condonación (lo incobrable se reparte al cierre según el modelo A). |
| D-L3-3: retiro sin haber cobrado | **Se devuelve lo aportado al final del ciclo:** las cuotas más el aporte al fondo. Su cupo lo puede tomar un reemplazo invitado. |
| D-L5-1 / D-L5-2: comisión | **Porcentaje único, igual para todos, sobre la cuota.** Se cobra en el mismo QR de la cuota como línea aparte y nunca sale del pozo. |
| AMB-L1-1: recargo por mora | **Automático y anulable.** Se calcula solo al vencer el plazo de gracia; si el descargo prospera, se revierte con un contra-asiento. La sanción y la reputación sí esperan al expediente firme. |
| D-L2-3: residuo de redondeo | **De a un centavo, empezando por el de mayor nivel de puntualidad.** Si hay empate, decide el sorteo del grupo. Nunca se pierde un centavo. |
| AMB-L1-2: transferencias P2P | **Se permite transferir a otra persona por su identificador único. No se permite pagar la cuota de otro.** |

Lo que queda abierto es solo para el abogado o Cumplimiento: [`PREGUNTAS-LEGALES.md`](../2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md).

## Tanda 8 (proveedores externos)

| Tema | Decisión |
|---|---|
| Canales de mensajería | **SMS, notificaciones push y correo. Sin WhatsApp.** |
| Segundo proveedor de cobro QR y de desembolso | **Obligatorio antes de producción.** |
| Contactos con proveedores | **Ninguno.** Las RFP salen al mercado abierto. |
| Facturación de la comisión | **Con un proveedor de facturación autorizado**, no integrada directo con el SIN. |

## Tanda 9 (decisiones que pidió la sesión F0)

| Tema | Decisión |
|---|---|
| Tamaño del grupo | El **creador** del grupo, que es su administrador, configura exactamente el tamaño. **Antes de arrancar lo cambia libremente.** Con el grupo en marcha solo se pueden **agregar cupos al final del calendario**, con aprobación del grupo por votación y sin tocar los turnos ya asignados. |
| Grupo sin llenar a la fecha límite | **Decide el creador:** achicar el grupo a los que ya entraron (si todos aceptan el nuevo pozo), extender la fecha o cancelar. Si no hace nada en X días (parámetro), el grupo se cancela. Hasta el arranque nadie pagó nada. Esto reemplaza el arranque con el 75 % de ADR-054. |
| Poderes del creador | Los cuatro ofrecidos, con los límites recomendados. (1) Invitar y revocar invitaciones mientras haya cupos. (2) Configurar el grupo (monto, periodicidad, fecha de inicio y nombre) **solo antes de arrancar**. (3) Fijar la fecha límite y cancelar el grupo **solo antes de arrancar**. (4) Pasar el rol a otro participante, que tiene que aceptarlo. **No puede** aprobar pagos ni entregas, mover dinero (regla 91.5), expulsar a nadie ni cambiar turnos. Lo que afecta a otros va por votación. No es el «organizador profesional», que se eliminó (P-11): es un participante con rol de administración de la configuración. |
| Disolución automática por mora | **Sigue, con umbral:** si el fondo del grupo se agota y la mora supera un umbral (parámetro), el grupo se disuelve solo y cada uno se liquida desde el mayor. Convive con la disolución por votación. |
| Quién es «el perjudicado» del recargo | **El fondo del grupo.** Si el fondo cubrió la cuota, el recargo vuelve al fondo y se devuelve a todos al cierre. Si alguien tuvo que esperar porque el fondo no alcanzó, el recargo va a quien esperó. Esto precisa la tanda 1 (P-05). |
| Lo incobrable al cierre | **Primero se compensa** con lo que el fondo le iba a devolver al propio moroso. El resto se reparte en partes iguales **solo entre los que cumplieron**. El moroso no recibe devolución mientras deba. |
| «Grupo perfecto» | El fondo se devuelve igual al cierre. Si el mismo grupo arma otro ciclo, **aporta un porcentaje menor al fondo** y todos suben de nivel juntos. |
