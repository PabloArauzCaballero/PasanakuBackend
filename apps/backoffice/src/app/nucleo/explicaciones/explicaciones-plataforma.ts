import type { ExplicacionDeModulo } from './tipos'

/**
 * Explicaciones para los módulos de Configuración de Plataforma y Centro de Ayuda de AportaYa.
 * Redactado con empatía, claridad meridiana y total rigor de sistemas.
 */
export const moduloConfiguracionExplicacion: ExplicacionDeModulo = {
  modulo: 'Configuración',
  prefijos: ['/configuracion'],
  sistemas:
    'Administra las políticas globales y parámetros de servicio de AportaYa: tarifario parametrizable (CU-34), plantillas de mensajería multicanal (CU-80/83) y definiciones de reportes programados (CU-58). Toda modificación requiere aprobación formal y genera una nueva versión inmutable en el repositorio de políticas.',
  negocio:
    'Las reglas claras del juego. Aquí se fijan los precios transparentes de los servicios, los mensajes oficiales que la aplicación envía a los teléfonos de los usuarios y los informes que se generan para la administración. Garantiza que no existan cobros ocultos ni sorpresas desagradables para nadie.',
  vistas: {
    '/configuracion': {
      sistemas:
        'Panel general de configuración: muestra el tarifario vigente con su código de versión, el estado de los canales de mensajería (SMS, WhatsApp, Push) y los reportes activos programados para ejecución automática.',
      negocio:
        'Resumen de las políticas y parámetros globales. Permite a los supervisores verificar que la plataforma esté operando con los valores aprobados por la dirección y el comité regulatorio.',
    },
    '/configuracion/tarifario': {
      sistemas:
        'Tarifario parametrizado de comisiones y servicios (CU-34). Muestra la comisión por administración de grupo, costo de retiros bancarios inmediatos y umbrales de gratuidad. Los valores se congelan al momento de abrir un grupo para respetar el contrato pactado.',
      negocio:
        'La lista pública y transparente de costos de AportaYa. Los usuarios saben exactamente cuánto cuesta cada servicio antes de usarlo. Además, si las tarifas cambian en el futuro, los grupos que ya comenzaron mantienen su tarifa inicial pactada hasta terminar su ciclo.',
    },
    '/configuracion/mensajeria': {
      sistemas:
        'Gestión de plantillas versionadas de notificaciones (CU-80 y CU-83). Soporta canales WhatsApp, SMS y notificaciones Push. Cada plantilla contiene variables tipadas (`{{nombre}}`, `{{monto}}`, `{{fecha}}`) y valida que no se envíe información sensible sin enmascarar.',
      negocio:
        'Los mensajes que cuidan y acompañan a los usuarios. Aquí se redactan con calidez y respeto los recordatorios de aporte, las felicitaciones al ganador del turno y las alertas de seguridad, asegurando que el lenguaje sea siempre claro, humano y comprensible.',
    },
    '/configuracion/reportes': {
      sistemas:
        'Catálogo y programación de reportes operativos y gerenciales (CU-58). Define parámetros de extracción, periodicidad (diaria, semanal, mensual) y genera archivos cifrados con firma digital para descarga de auditoría.',
      negocio:
        'La generación de informes periódicos para la toma de decisiones. Permite que la administración y los auditores cuenten con datos consolidados y fiables sobre la evolución del ahorro comunitario en el país.',
    },
  },
}

export const moduloAyudaExplicacion: ExplicacionDeModulo = {
  modulo: 'Ayuda y Tutoriales',
  prefijos: ['/ayuda'],
  sistemas:
    'Centro de capacitación interactiva para operadores del backoffice. Almacena el progreso de tutoriales en el almacenamiento seguro de sesión y guía al usuario mediante reflectores y anfitriones de interfaz (Driver.js / Walkthroughs) contextuales a cada pantalla.',
  negocio:
    'La escuela de aprendizaje de AportaYa. Si un nuevo operador o analista se suma al equipo, aquí encuentra guías interactivas paso a paso que le enseñan cómo usar cada pantalla del backoffice con seguridad y confianza, sin miedo a equivocarse.',
  vistas: {
    '/ayuda': {
      sistemas:
        'Catálogo de tutoriales disponibles filtrados según los permisos del operador: guías de conciliación de custodia, atención de reclamos, verificación de documentos KYC y administración de grupos.',
      negocio:
        'El menú de cursos y guías interactivas. Permite a cualquier miembro del equipo repasar los procedimientos operativos estándar y certificarse en las mejores prácticas de atención y seguridad comunitaria.',
    },
  },
}
