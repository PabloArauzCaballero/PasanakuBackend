import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para el módulo Cumplimiento y Control Normativo de AportaYa.
 * Combina la terminología regulatoria y legal con explicaciones empáticas y claras para cualquier usuario.
 */
export const moduloCumplimientoExplicacion: ExplicacionDeModulo = {
  modulo: 'Cumplimiento',
  prefijos: ['/cumplimiento'],
  sistemas:
    'Implementa los controles regulatorios exigidos por la Unidad de Investigaciones Financieras (UIF) y la Autoridad de Supervisión del Sistema Financiero (ASFI): debida diligencia de clientes (KYC), monitoreo automatizado de transacciones con listas de sanciones internacionales (OFAC, ONU), umbrales PCC-01/ROG, registro de incidentes de seguridad y generación de actas de comité con firma digital inmutable.',
  negocio:
    'Es el escudo legal y ético de AportaYa. Su trabajo es asegurar que la plataforma sea un espacio 100% limpio y seguro, impidiendo que personas con intenciones ilegales o dinero de origen dudoso usen el pasanaku. Al mismo tiempo, defiende los derechos de los usuarios honestos garantizando debido proceso, transparencia y apego estricto a las leyes.',
  vistas: {
    '/cumplimiento': {
      sistemas:
        'Centro de control de cumplimiento normativo que agrega el estado de los expedientes de debida diligencia, alertas de monitoreo pendientes de triaje, casos de investigación activos y el calendario de reportes obligatorios para la UIF.',
      negocio:
        'Resumen para el Oficial de Cumplimiento y el área legal. Permite supervisar que la empresa cumpla todas sus obligaciones legales a tiempo y que no exista ninguna situación sospechosa sin investigar.',
    },
    '/cumplimiento/verificaciones': {
      sistemas:
        'Gestión de expedientes de debida diligencia y validación de identidad (CU-02). Compara los datos del documento de identidad con el padrón electoral oficial, valida vigencia del carnet y examina las fotos del anverso, reverso y selfie con prueba de vida biométrica.',
      negocio:
        'Verificación de que cada persona sea realmente quien dice ser. Protege a la comunidad contra la suplantación de identidad (que alguien use el carnet de otra persona para sacar dinero o estafar a un grupo). El usuario común tiene la certeza de que todos sus compañeros de ahorro son personas reales y verificadas.',
    },
    '/cumplimiento/alertas': {
      sistemas:
        'Bandeja de alertas de monitoreo transaccional (CU-97). Se disparan cuando una cuenta supera los umbrales de acumulación mensual (PCC-01), realiza transacciones fraccionadas para eludir límites (pitufeo) o coincide con Personas Expuestas Políticamente (PEP). Exige permiso `CUMPLIMIENTO_ALERTAS`.',
      negocio:
        'Sistema de alarma preventiva ante comportamientos extraños. Si alguien que habitualmente ahorra sumas pequeñas de pronto recibe o mueve cantidades desproporcionadas sin justificación, el sistema genera una alerta para que un analista revise si todo está en orden o si se trata de un intento de fraude.',
    },
    '/cumplimiento/alertas/[alertaId]': {
      sistemas:
        'Detalle de la señal de riesgo: presenta la línea temporal de movimientos que dispararon el umbral, los factores de riesgo calculados y las opciones de resolución: archivar como falso positivo con motivo fundado o elevar formalmente a caso de investigación antilavado.',
      negocio:
        'Análisis profundo de una alerta específica. El analista revisa los hechos reales (sin prejuicios) para entender qué pasó: si fue simplemente un usuario que vendió un bien y decidió adelantar sus cuotas de ahorro, o si existe una anomalía que requiera pedirle comprobantes.',
    },
    '/cumplimiento/casos': {
      sistemas:
        'Bandeja de casos de investigación de lavado de activos y financiamiento del terrorismo (CU-44). Administra el ciclo de vida del caso: APERTURA -> INVESTIGACION -> DICTAMEN -> REPORTE_ROS o ARCHIVO. Requiere permiso `CUMPLIMIENTO_CASOS`.',
      negocio:
        'Expedientes de investigación formal. Cuando una sospecha persiste tras revisar una alerta, se abre un expediente confidencial donde se recopila toda la información patrimonial y transaccional para determinar si debe notificarse a las autoridades financieras del país.',
    },
    '/cumplimiento/casos/[casoId]': {
      sistemas:
        'Pantalla de resolución del caso de cumplimiento. Contiene la cronología de movimientos, los descargos presentados por el cliente, las notas confidenciales del analista y el formulario de dictamen formal firmado por el Oficial de Cumplimiento.',
      negocio:
        'La conclusión de la investigación. Garantiza el debido proceso: si el cliente aportó la documentación que demuestra el origen lícito de sus fondos, el caso se cierra a su favor; si no hay justificación, se procede conforme a la ley sin vulnerar sus derechos.',
    },
    '/cumplimiento/uif/reportes': {
      sistemas:
        'Generación, validación y remisión de reportes regulatorios periódicos a la UIF (CU-43): Registro de Operaciones en Efectivo o Equivalentes (PCC-01), Registro de Operaciones Grandes (ROG) y Reporte de Operaciones Sospechosas (ROS). Valida formato XML/JSON con firma digital.',
      negocio:
        'Envío de informes oficiales a la Unidad de Investigaciones Financieras. Cumple el mandato de la ley de informar mensualmente las operaciones que superan los topes legales, manteniendo a AportaYa como una entidad financiera tecnológica transparente y regulada.',
    },
    '/cumplimiento/uif/requerimientos': {
      sistemas:
        'Bandeja de requerimientos de información judicial, fiscal o de supervisión bancaria (CU-45). Controla plazos perentorios de respuesta con cálculo automático de días hábiles, registro de número de oficio fiscal y trazabilidad estricta de la información entregada.',
      negocio:
        'Atención de solicitudes de información enviadas por jueces, fiscales o reguladores. Asegura que la empresa responda con exactitud y dentro de los plazos legales, protegiendo los datos confidenciales de la comunidad y colaborando con la justicia.',
    },
    '/cumplimiento/continuidad': {
      sistemas:
        'Registro y ejecución de pruebas periódicas de Continuidad del Negocio y Plan de Recuperación ante Desastres (DRP/BCP - CU-56). Mide los tiempos de recuperación objetivo (RTO < 4 horas) y el punto objetivo de recuperación de datos (RPO = 0 transacciones perdidas).',
      negocio:
        'Simulacros de emergencia y desastre. Verifica con pruebas reales que si los servidores principales sufren un incendio, corte general de luz o ataque cibernético, la plataforma pueda levantarse en minutos en otro centro de datos sin perder un solo centavo del dinero de la gente.',
    },
    '/cumplimiento/actas': {
      sistemas:
        'Bandeja de actas de comité de cumplimiento, riesgo y tecnología (CU-94). Requiere quórum reglamentario y registro de votos nominales individuales. Una vez firmada, el hash criptográfico del documento sella el acta haciéndola inmutable.',
      negocio:
        'Las decisiones de gobierno corporativo. Las políticas de riesgo y los cambios críticos no los decide una persona sola; se reúnen los comités directivos, debaten las medidas y firman actas oficiales que respaldan cada paso ante la ley.',
    },
    '/cumplimiento/actas/[actaId]': {
      sistemas:
        'Visualizador y firmador de actas de comité: muestra el orden del día, las deliberaciones, los acuerdos transaccionales y el panel de firmas con certificado digital.',
      negocio:
        'Documento formal del acuerdo alcanzado por los directores. Sirve como evidencia jurídica irrefutable de que la empresa actúa con diligencia, seriedad y responsabilidad corporativa.',
    },
    '/cumplimiento/riesgos/eventos': {
      sistemas:
        'Bitácora de eventos de riesgo operativo: fallos tecnológicos, errores de proceso, pérdidas financieras o incidentes humanos. Cuantifica el impacto económico directo, indirecto y el plan de mitigación correctivo.',
      negocio:
        'Registro de incidentes para aprender y mejorar. Si algo sale mal (por ejemplo, una pasarela bancaria que se cayó 20 minutos), se registra con honestidad, se calcula el impacto y se aplican mejoras para que jamás vuelva a suceder.',
    },
    '/cumplimiento/seguridad/incidentes': {
      sistemas:
        'Gestión de incidentes de seguridad de la información según norma ISO/IEC 27001. Clasifica incidentes por impacto (confidencialidad, integridad, disponibilidad), documenta la contención y genera el informe post-mortem obligatorio.',
      negocio:
        'La defensa contra ciberataques. Si se detecta un intento de hackeo, robo de contraseñas o actividad no autorizada, este módulo guía la respuesta del equipo para bloquear la amenaza, proteger las cuentas de los usuarios y avisar si fuera necesario.',
    },
    '/cumplimiento/reclamos/[reclamoId]': {
      sistemas:
        'Expediente completo de un reclamo escalado ante la defensoría del cliente financiero (CU-52). Incluye grabaciones, comprobantes de pago, historial de comunicaciones y plazo máximo legal para dictar resolución reparadora.',
      negocio:
        'Revisión a fondo del reclamo de un cliente cuando no quedó conforme en primera instancia. Aquí se examinan todas las pruebas de forma imparcial: si el usuario tiene la razón, se le da la razón de inmediato y se repara el problema con compensación si corresponde.',
    },
    '/cumplimiento/organizadores/[organizadorId]/habilitacion': {
      sistemas:
        'Evaluación de idoneidad y habilitación de organizadores de grupos (CU-90). Evalúa antecedentes de cumplimiento en la plataforma, capacitación aprobada con vigencia, contrato de organizador firmado y límite de concentración de riesgo por cupo.',
      negocio:
        'Certificación de los líderes de grupo. Un organizador en Pasanaku tiene la hermosa responsabilidad de coordinar a su comunidad; aquí nos aseguramos de que esté capacitado, que conozca las reglas del pasanaku digital y que tenga una reputación intachable.',
    },
  },
}
