# Propuestas de texto escritas a mano (F3.S8.M1). Clave: id del inventario.
FONDO_LARGO = ("Cada integrante aporta un poco extra a un fondo del propio grupo, en una cuenta separada que no es "
               "de la empresa. Si alguien no paga, el fondo cubre su cuota y esa persona queda debiéndole al fondo. "
               "Al cerrar el ciclo, a cada uno se le devuelve lo que aportó, menos su parte de lo que no se haya "
               "podido cobrar.")
WA_CANAL = "Recordatorio antes del vencimiento y confirmación cuando el aporte entra, por notificación, SMS o correo. Vos elegís qué te llega."
SIS_LLEVA = "Los cobros, los recordatorios, el sorteo y el cierre de cada período los hace el sistema. Nadie cobra por administrar el grupo y nadie aprueba nada a mano."
SELLADO = "Una vez sellado un período, nadie lo puede modificar — ni nosotros ni nadie del grupo. Y no tenés que creernos: podés comprobarlo por tu cuenta."
FAQ_FALTA = ("El fondo del grupo, que aportan los propios integrantes, cubre esa cuota para que el resto no se vea "
             "afectado, y quien no pagó queda debiéndole al fondo. Antes de cualquier sanción hay un proceso con descargo.")
KYC_TXT = ("Revisar los casos que la verificación automática no pudo resolver y los de personas expuestas "
           "políticamente. La decisión es de una persona de Cumplimiento y queda con su nombre.")
LIMITE = " **DR:** existe solo si se mantiene un límite de grupos activos por persona (no está en DECISIONES.md)."

P = {
 "T001": "Fondo del grupo: tu garantía vuelve",
 "T002": WA_CANAL,
 "T003": "Fondo del grupo",
 "T004": FONDO_LARGO,
 "T005": "El sistema lleva el grupo",
 "T006": SIS_LLEVA,
 "T007": "Aportaron 9 de 10 · el fondo del grupo cubrió la cuota que faltó · recibió Juan C. Bs2.500,00",
 "T008": SELLADO,
 "T009": ("Primero se le avisa. Al vencer el plazo de gracia, el fondo del grupo cubre su cuota para que quien "
          "tenía el turno cobre igual, y esa persona queda debiéndole al fondo: lo que pague después vuelve al fondo. "
          "Cualquier sanción o efecto en su reputación pasa antes por un procedimiento con descargo, decisión motivada y apelación."),
 "T010": ("La plataforma cobra una comisión de servicio de porcentaje único, igual para todos, sobre la cuota. Se "
          "paga en el mismo QR como línea aparte y nunca sale del pozo. El tarifario está publicado y queda congelado "
          "al iniciar tu grupo. No hay descuentos ni exenciones, y nadie más cobra por administrar el grupo."),
 "T011": "Fondo del grupo",
 "T012": WA_CANAL,
 "T013": "Fondo del grupo",
 "T014": FONDO_LARGO,
 "T015": "El sistema lleva el grupo",
 "T016": SIS_LLEVA,
 "T017": SELLADO,
 "T018": "· el fondo del grupo cubrió la cuota que faltó",
 "T019": ("Primero se le avisa. Al vencer el plazo de gracia, el fondo del grupo cubre su cuota para que quien "
          "tenía el turno cobre igual, y esa persona queda debiéndole al fondo."),
 "T020": "Si cumplís los requisitos (identidad verificada, reglamento aceptado y sin deudas pendientes), entrás al grupo al instante. Nadie tiene que aprobarte.",
 "T021": "Con la invitación de alguien del grupo entrás directo, si cumplís los requisitos. No hay que esperar a que nadie te acepte.",
 "T022": FONDO_LARGO + " Quien incumple pasa por un proceso con descargo y apelación antes de cualquier sanción.",
 "T023": FAQ_FALTA,
 "T024": "Billetera móvil que digitaliza el pasanaku boliviano: ahorro rotativo entre conocidos, con custodia, sorteo verificable y un fondo del grupo que vuelve a cada uno al cierre.",
 "T025": FAQ_FALTA,
 "T026": ("El fondo del grupo, que aportan todos, cubre la cuota para que el turno se entregue igual. La deuda no se "
          "perdona: quien no pagó queda debiéndole al fondo, y al cierre del ciclo el fondo se devuelve a cada uno."),
 "T027": "Es plata comprometida: un aporte en curso o tu aporte al fondo del grupo, que se te devuelve al cerrar el ciclo. No la perdiste; está reservada para algo que ya aceptaste.",
 "T028": "Estamos verificando tu identidad. Mientras tanto ya podés entrar y conocer AportaYa.",
 "T029": "Recibimos tus documentos. Apenas termine la verificación vas a poder hacer esta operación.",
 "T030": "Último paso: mandamos tu carnet y tus fotos para verificarlos. Mientras tanto podés seguir usando la app.",
 "T031": "Tu identidad sigue en verificación. Te avisamos apenas termine.",
 "T032": "Mientras verificamos tu identidad podés ver tus grupos e invitaciones. Para crear un grupo o unirte a uno necesitamos terminar la verificación.",
 "T033": "Después verificamos tu documento de forma automática; si hace falta una revisión adicional, te avisamos. Cómo va lo ves en tu perfil.",
 "T034": "Estamos verificando tu documento. El resultado aparece en tu perfil.",
 "T035": "Ya mandaste todo. Estamos verificando tu documento; el resultado aparece acá.",
 "T036": "Crear un grupo",
 "T061": "Tus grupos · $n / Tus grupos · $n de $tope" + LIMITE,
 "T062": "Todavía no creaste ningún grupo.",
 "T063": "Creaste este grupo",
 "T064": "Todavía no participás en ningún grupo. Unite con la invitación que te mandó alguien del grupo, o creá uno con tu gente.",
 "T067": "Elegí el monto y cada cuánto se aporta. Después invitás a tu gente: quien cumpla los requisitos entra directo.",
 "T068": "Ahora sumá a tu gente: compartí la invitación del grupo. Quien cumpla los requisitos entra sin que nadie tenga que aceptarlo.",
 "T069": "Compartí la invitación. Quien la abra y cumpla los requisitos entra directo al grupo.",
 "T072": "Crear un grupo nuevo",
 "T073": "Ya tenés $tope grupos activos, el máximo. Cuando uno termine vas a poder crear otro." + LIMITE,
 "T074": "Compartir invitación",
 "T075": "Apuntá la cámara al QR de la invitación que te mandaron.",
 "T076": "Eliminar la opción «WhatsApp»: queda una sola acción «Compartir» que abre el menú de compartir del teléfono (ver ambigüedad A-1).",
 "T077": "Compartilo desde tu teléfono o mostrá su QR. Sirve 7 días o hasta que se llene el grupo.",
 "T078": "Mandá esta imagen a quien quieras invitar. Quien la reciba la guarda y la sube en AportaYa: Grupos → Unirse a un grupo.",
 "T079": "Compartir QR",
 "T080": "Compartir enlace",
 "T081": "Compartir",
 "T088": "Este grupo ya está completo. Si se libera un cupo, el grupo puede invitar a alguien para reemplazarlo.",
 "T089": "Las personas del grupo van a ver tu nombre completo para saber quién sos.",
 "T092": "Este enlace ya no sirve: venció o lo cambiaron. Pedile uno nuevo a quien te invitó.",
 "T093": "Pedile a alguien del grupo la invitación: el QR o el enlace.",
 "T094": "Elegí de tu galería la imagen del QR que te mandaron.",
 "T095": "Pegá el enlace de invitación que te mandaron.",
 "T096": "Ese enlace no es de un grupo de AportaYa. Copialo entero del mensaje que te mandaron.",
 "T097": "Creó el grupo",
 "T179": "Ya tenés el máximo de grupos activos que permite tu cuenta." + LIMITE,
 "T180": "No podés invitar a este grupo: solo pueden invitar sus integrantes.",
 "T181": "No pudimos sumarte con este enlace. Pedile una invitación nueva a alguien del grupo.",
 "T182": "Para crear un grupo necesitás tener tu identidad verificada. Mientras tanto, podés sumarte a un grupo con una invitación.",
 "T183": "El grupo se inicia solo cuando se completan los cupos.",
 "T202": ("La entrega de este turno todavía no puede completarse: al pozo le faltan cuotas y el fondo del grupo no "
          "alcanza para cubrirlas. Ninguna deuda se cancela; la entrega sigue en espera y te avisamos apenas cambie."),
 "T208": "El ciclo del grupo todavía no terminó: el fondo se devuelve al cierre.",
 "T209": "Ese grupo todavía no tiene su fondo constituido.",
 "T210": "Ese grupo todavía no tiene su fondo constituido.",
 "T211": "El grupo {grupoId} todavía no tiene su fondo constituido: el ingreso queda pendiente.",
 "T212": "El fondo del grupo",
 "T213": "La cuenta del fondo del grupo",
 "T216": "Ese reemplazo no fue aprobado por la votación del grupo: no se ejecuta.",
 "T217": "Sin cambio: es el control dual del restablecimiento de credenciales privilegiadas del personal (docs/Cumplimiento.md §1.5), fuera del circuito del pasanaku.",
 "T251": "Ese enlace del grupo ya no sirve. Pedile uno nuevo a quien te invitó.",
 "T253": "Ya tenés %d grupos activos, el máximo. Terminá o cancelá uno antes de crear otro." + LIMITE,
 "T254": "Iniciado automáticamente con %d de %d cupos ocupados.",
 "T279": "El anunciante está {estado}: no puede anunciar.",
 "T280": "Invertir puede generar pérdidas. El rendimiento no está garantizado ni por Pasanaku ni por el aliado, y el fondo del grupo no cubre esta inversión.",
 "T306": "El fondo de tu grupo cubrió tu cuota",
 "T307": ("El fondo del grupo {{grupo}} cubrió {{monto}} de tu cuota y quedaste debiéndole {{deuda}} al fondo. Tenés "
          "hasta el {{fecha_limite}} para regularizar o presentar tu descargo. Lo que pagues vuelve al fondo del grupo."),
 "T308": "El fondo de tu grupo cubrió tu cuota",
 "T309": "Quedaste debiéndole al fondo de {{grupo}}. Tenés tiempo hasta el {{fecha_limite}}. Mirá el detalle en la app.",
 "T310": "El fondo de tu grupo cubrió tu cuota",
 "T311": ("El fondo de tu grupo {{grupo}} cubrió tu cuota de este período para que el turno se entregue a tiempo. "
          "Quedaste debiéndole al fondo: lo que pagues vuelve al fondo del grupo. Tenés hasta el {{fecha_limite}} para "
          "regularizar o presentar tu descargo desde la app. AportaYa · Este es un mensaje automático; no respondas a esta dirección."),
 "T322": "Quitar «retención de organizadores»; el resto sin cambio.",
 "T323": "Quitar «y qué organizadores tienen la mejor reputación» (reemplazar por «y cuántos grupos lograron «Grupo perfecto»»); el resto sin cambio.",
 "T324": ("Bandeja de retiros retenidos por una alerta de monitoreo (patrón inusual, cuenta cambiada hace poco o monto "
          "fuera de lo habitual). Los retiros normales salen solos; acá solo llega lo que disparó una alerta, para que "
          "Cumplimiento lo resuelva con motivo."),
 "T333": ("Supervisa la comunicación con proveedores externos: pasarelas de cobro QR, proveedores de desembolso, SMS, "
          "notificaciones push y correo. Monitorea los cortes automáticos (circuit breakers) cuando un proveedor falla."),
 "T334": "Reemplazar «(SMS, WhatsApp, Push)» por «(SMS, push y correo)»; el resto sin cambio.",
 "T335": "Reemplazar «Soporta canales WhatsApp, SMS y notificaciones Push» por «Soporta SMS, notificaciones push y correo»; el resto sin cambio.",
 "T336": "Resolver un retiro retenido por alerta",
 "T337": "Este retiro quedó retenido porque disparó una alerta de monitoreo. Revisá el motivo y liberalo o rechazalo con fundamento, a tu nombre.",
 "T339": "Resolver retiros retenidos es de Cumplimiento. Podés ver esta pantalla, pero el servidor rechazaría la decisión.",
 "T340": "Este retiro ya no está retenido: otra persona lo resolvió primero. No se hizo nada de más.",
 "T341": "No podés resolver un retiro que pediste vos. Lo tiene que resolver otra persona de Cumplimiento.",
 "T342": "Retiros retenidos por alerta",
 "T343": "Los retiros normales salen solos. Acá llegan solo los que dispararon una alerta de monitoreo, del más antiguo al más nuevo.",
 "T344": "No hay retiros retenidos.",
 "T345": "Cuando un retiro dispare una alerta va a aparecer acá.",
 "T346": "Esta bandeja es de Cumplimiento.",
 "T347": "Revisar la alerta",
 "T348": "Es tuyo: lo resuelve otra persona.",
 "T350": "Lo que entró, lo que salió, la mora y el saldo de los fondos de los grupos, calculado desde el mayor y cada servicio dueño.",
 "T356": "Dar de alta un anunciante (socio comercial) y su cuenta publicitaria. El socio comercial se verifica antes …(resto sin cambio).",
 "T358": "El anunciante es un socio comercial ya verificado. Tiene la forma 8-4-4-4-12.",
 "T382": KYC_TXT,
 "T387": ("Esta es la cola de excepciones de la verificación de identidad: altas que la verificación automática no "
          "pudo aprobar ni rechazar con certeza, y personas expuestas políticamente. El resto se resuelve solo."),
 "T388": KYC_TXT,
 "T389": "Por qué la mayoría de las altas las resuelve la verificación automática y solo las excepciones llegan acá",
}

# Eliminaciones explícitas (además de las de categoría), con su motivo.
E = {
 **{t: "Eliminar: el pago es solo por QR confirmado por webhook; no hay referencia escrita a mano (P-07)." for t in ("T060", "T070", "T071")},
 **{t: "Eliminar: el respaldo de la empresa no existe (P-04; F2). Mensaje interno de un flujo que se borra." for t in ("T201", "T203", "T204", "T205", "T206", "T207")},
 **{t: "Eliminar: retiros automáticos con controles; sin doble aprobación (P-09; ADR que reemplaza ADR-049)." for t in ("T218", "T219", "T220", "T221", "T319", "T325", "T326", "T338")},
 **{t: "Eliminar: la admisión es automática con requisitos; nadie valora a mano un ingreso (tanda 5)." for t in ("T247", "T248", "T249", "T250", "T314", "T327", "T328", "T329", "T330", "T331", "T332", "T351", "T352", "T354", "T355")},
 **{t: "Eliminar: ya no hay solicitudes de ingreso que alguien acepte; la invitación válida con requisitos cumplidos es el ingreso (tanda 5; F3.S4.M1)." for t in ("T082", "T083", "T084", "T085", "T086", "T087", "T090", "T091", "T100")},
 "T357": "Eliminar el campo: el anunciante ya no puede ser un organizador (P-11).",
}

DEF = {
 "ORGANIZADOR": "Eliminar con la funcionalidad del organizador (P-11; F2.M6).",
 "SUBASTA_CESION": "Eliminar: subasta, cesión, traspaso y permuta de turno no existen (tanda 7; F3.S4.M6). El reemplazo por invitación votado (F3.S4.M5) tendrá sus propios textos.",
 "GRUPO_PUBLICO": "Eliminar: no hay grupos públicos, postulación ni emparejamiento; solo invitación (tanda 5; F3.S4.M1).",
 "AVAL": "Eliminar con el aval entre participantes (P-03; F2.M5).",
 "COMPROBANTE_MANUAL": "Eliminar: el pago es solo por QR confirmado por webhook (P-07).",
 "WHATSAPP": ("Eliminar la plantilla del canal WhatsApp (tanda 8; PX7.M05). Si el evento no tiene variante de SMS, push o "
              "correo, se crea en PX7.M02 sin montos atribuibles (RF-PX7-04)."),
 "RESPALDO_EMPRESA": "Eliminar con el respaldo de la empresa (P-04; F2).",
 "REVISION_HUMANA": "Eliminar (P-08 a P-10: sin aprobaciones humanas en el circuito).",
 "OTRO": "Revisar caso a caso.",
}
