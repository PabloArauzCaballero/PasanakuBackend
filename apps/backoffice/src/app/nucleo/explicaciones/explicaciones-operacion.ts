import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para el módulo Operación y Admisiones de AportaYa.
 * Aplica principios de marketing de confianza, psicología del comportamiento,
 * Jobs to Be Done y máxima claridad para usuarios y operadores.
 */
export const moduloOperacionExplicacion: ExplicacionDeModulo = {
  modulo: 'Operación',
  prefijos: ['/operacion', '/admisiones'],
  sistemas:
    'Gobierna los procesos críticos de dinero y transaccionalidad: depósitos por QR, retiros bancarios, entrega de fondos colectivos, conciliación bancaria y reversos. Todas las operaciones financieras se registran con el principio contable de partida doble, claves de idempotencia y control de concurrencia pesimista (SELECT ... FOR UPDATE).',
  negocio:
    'Es el corazón operativo donde se cuida el dinero de las familias y amigos que ahorran en pasanaku. Su misión es que cada aporte llegue a tiempo, que cada persona que gana su turno cobre sin retrasos y que ninguna transacción sospechosa o duplicada se escape. Protege la confianza mutua, que es el valor más sagrado del ahorro comunitario.',
  vistas: {
    '/operacion': {
      sistemas:
        'Vista de coordinación operativa que agrega el estado de las órdenes en tránsito, retiros pendientes de firma, solicitudes escaladas y alertas de custodia consumiendo endpoints del servicio de tesorería y billetera.',
      negocio:
        'Resumen de las tareas que el equipo de operaciones debe resolver hoy. Prioriza lo urgente: retiros de dinero solicitados por los usuarios, aclaraciones de pagos no reconocidos y grupos que necesitan asistencia para cerrar su ciclo con éxito.',
    },
    '/operacion/estado': {
      sistemas:
        'Visualiza el estado de la máquina de estados de los grupos de pasanaku: FORMACION -> SORTEO -> ACTIVO -> FINALIZADO (o DISUELTO). Verifica que cada transición cumpla las invariantes del contrato inteligente y que las firmas de compromiso criptográfico estén registradas.',
      negocio:
        'Muestra en qué etapa está cada grupo de pasanaku. Permite saber si un grupo ya completó sus miembros, si el sorteo de números se realizó de forma limpia y transparente, y si los turnos de entrega se están cumpliendo sin atrasos.',
    },
    '/operacion/retiros': {
      sistemas:
        'Bandeja de órdenes de retiro que superan el umbral automático de seguridad o provienen de cuentas con patrones inusuales. Requiere el permiso `RETIRO_APROBAR`. Consulta la tabla `ordenes_retiro` donde `estado = EN_REVISION`.',
      negocio:
        'Control de seguridad para proteger los ahorros de la gente ante posibles hackeos o fraudes. Cuando un usuario pide retirar una suma grande a su cuenta de banco, un operador humano verifica la legitimidad de la solicitud antes de que el dinero salga de la cuenta de custodia.',
    },
    '/operacion/retiros/[ordenId]': {
      sistemas:
        'Pantalla de segunda firma (segregación de funciones de 4 ojos). El operador que aprueba debe ser diferente al que originó la orden. Al confirmar, se invoca `POST /operaciones/retiros/{id}/aprobar` con token de un solo uso y clave de idempotencia, encolando el desembolso hacia la pasarela bancaria.',
      negocio:
        'Regla de los "cuatro ojos": ninguna persona sola puede autorizar la salida de dinero importante. Esta pantalla exige que un segundo responsable revise los datos bancarios del beneficiario y autorice formalmente la transferencia, garantizando que el dinero llegue al dueño correcto.',
    },
    '/operacion/reclamos': {
      sistemas:
        'Bandeja de atención de reclamos (CU-52) conectada al servicio de atención al cliente. Controla los plazos hábiles obligatorios establecidos por la normativa de protección al consumidor financiero. Si un reclamo no se atiende antes del vencimiento legal, se escala automáticamente.',
      negocio:
        'El canal de escucha y solución para los usuarios. Si alguien tuvo un problema con un depósito, no entendió un cobro o necesita ayuda con su grupo, aquí se atiende con amabilidad, rapidez y respeto, dejando constancia de cada paso hasta dejar al cliente 100% satisfecho.',
    },
    '/operacion/solicitudes-escaladas': {
      sistemas:
        'Cola de solicitudes de ingreso que fueron observadas por el motor de scoring de riesgo o presentaron discrepancias en las referencias comunitarias. Permite al analista aprobar o rechazar con causal fundada que se guarda en la bitácora.',
      negocio:
        'Revisión manual de personas que desean unirse a un grupo pero el sistema automático detectó alguna duda (por ejemplo, datos no legibles en su documento). El operador analiza el caso con criterio humano para no dejar afuera a nadie honesto sin motivo.',
    },
    '/operacion/custodia': {
      sistemas:
        'Ejecución y consulta de la conciliación diaria de custodia (CU-50). Compara el saldo de la cuenta bancaria fiduciaria contra el total de dinero electrónico emitido en las billeteras. Requiere rol `TESORERIA` para registrar extractos y bloquea retiros si existe desbalance mayor a cero.',
      negocio:
        'La prueba máxima de honestidad de AportaYa. Todos los días a primera hora, se compara el dinero que los usuarios tienen en su app contra el dinero que está en la cuenta protegida del banco. Si falta un solo peso, la plataforma frena desembolsos inmediatamente hasta encontrar el error.',
    },
    '/operacion/cierres-diarios': {
      sistemas:
        'Cierre contable de la jornada (CU-51). Ejecuta el cuadre de todos los asientos de partida doble generados en las últimas 24 horas, congela la ventana transaccional y genera el snapshot inmutable de balances para auditoría. Exige rol `CONTABILIDAD`.',
      negocio:
        'El cierre de caja diario. Como en cualquier institución financiera seria, al finalizar el día se cierran los libros contables, se asegura que cada débito tenga su crédito exacto y se congela el registro para que nadie pueda alterar el pasado.',
    },
    '/operacion/reversos': {
      sistemas:
        'Historial de reversos y compensaciones de transferencias (CU-14). Muestra las transacciones que fueron canceladas por la pasarela de pagos, errores de digitación de cuenta destino o contracargos bancarios. Opera exclusivamente en modo solo lectura.',
      negocio:
        'Registro de pagos que tuvieron que devolverse. Si un banco rechazó una transferencia o un usuario pagó dos veces por error, aquí se comprueba que el dinero haya sido devuelto íntegramente a su legítimo dueño sin cobrar comisiones indebidas.',
    },
    '/operacion/desembolsos': {
      sistemas:
        'Monitorea la cola de órdenes de pago hacia bancos y pasarelas QR (CU-28). Cada desembolso cuenta con un identificador único que impide transferir dos veces aunque la red bancaria sufra timeouts o microcortes.',
      negocio:
        'La entrega del dinero acumulado al participante que le toca cobrar su pasanaku en esa fecha. Permite verificar que el ganador reciba su fondo completo en su cuenta bancaria puntualmente y con su recibo de constancia.',
    },
    '/operacion/billetera/[cuentaId]': {
      sistemas:
        'Consulta de extracto y movimientos de una billetera de usuario bajo estricto permiso `BILLETERA_VER_TERCEROS`. La consulta deja una huella inmutable en la bitácora de auditoría registrando qué operador accedió y con qué motivo justificado.',
      negocio:
        'Visualización protegida de la billetera de un cliente para atender solicitudes de soporte técnico. Para proteger la privacidad del usuario, solo personal autorizado puede ingresar aquí y cada acceso queda grabado en la bitácora de seguridad.',
    },
    '/admisiones': {
      sistemas:
        'Bandeja de admisión de nuevos participantes y organizadores (CU-68). Consulta `solicitudes_ingreso` evaluando identidad, puntaje crediticio reputacional y capacidad de pago según el monto del ciclo de ahorro.',
      negocio:
        'La puerta de entrada a la comunidad de Pasanaku. Ayuda a formar grupos armoniosos donde todos los miembros sean personas cumplidoras y de confianza, garantizando que el ciclo de ahorro comience y termine sin sobresaltos ni abandonos.',
    },
    '/admisiones/[solicitudId]': {
      sistemas:
        'Detalle de postulación individual: coteja el documento de identidad escaneado, la prueba de vida biométrica, las referencias vecinales y las respuestas a las preguntas de compromiso. Permite dictar resolución fundada con causal de rechazo codificada.',
      negocio:
        'Evaluación profunda de la solicitud de un usuario. Aquí se revisa que sus documentos sean legítimos y que el grupo al que desea entrar sea adecuado para su capacidad de aporte, cuidando tanto a la persona como al grupo que la recibe.',
    },
  },
}
