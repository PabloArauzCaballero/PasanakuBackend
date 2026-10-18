/**
 * **Qué permiso real abre cada sección del backoffice.**
 *
 * Las secciones se protegen con `ver:<seccion>`, un nombre que el carril B inventó
 * mientras no existía el catálogo de permisos (planes/informes/carril-B.md). El catálogo
 * ya existe —`sql/60_semillas/10-roles-y-permisos.sql`— y el token de `identidad` trae
 * esos códigos, no los `ver:*`. Sin esta traducción, un operador con sesión abierta veía
 * el menú vacío.
 *
 * Una sección se abre si el token trae **al menos uno** de sus permisos. Cada lista sale
 * de lo que hacen las pantallas de la sección, no de un rol: así el mapa sobrevive a que
 * cambie qué rol recibe qué permiso.
 *
 * Es un solo archivo a propósito, para revisarlo de un vistazo. Recordatorio de la
 * regla 4 del shell: **el rol oculta, no protege** — cada endpoint vuelve a decidir.
 */
export const PERMISOS_POR_SECCION: Readonly<Record<string, readonly string[]>> = {
  // Billetera de terceros, reclamos, solicitudes escaladas, estado de la plataforma.
  // `RECLAMO_VER`, `SOLICITUD_INGRESO_*` y `RETIRO_APROBAR` ya están sembrados y son los que piden las rutas `operacion/reclamos`,
  // `operacion/solicitudes-escaladas` y `operacion/retiros`: sin ellos acá, quien solo los tuviera no veía la sección.
  'ver:operacion': [
    'BILLETERA_VER_TERCEROS',
    'RECLAMO_ATENDER',
    'RECLAMO_VER',
    'SOLICITUD_INGRESO_VER',
    'SOLICITUD_INGRESO_RESOLVER',
    'RETIRO_APROBAR',
    'ENTREGA_AUTORIZAR',
    'ENTREGA_EJECUTAR',
    'REVERSO_AUTORIZAR',
    // Carril B · L1: conciliación de custodia (CU-50) y cierre diario (CU-51) los exige el servidor por código de rol
    // (`@Permiso("TESORERIA")`, `@Permiso("CONTABILIDAD")`); el token trae esos códigos (EmitirAcceso: permisos + roles vigentes).
    // AUDITORIA_LEER NO se agrega: abriría Operación al administrador de plataforma (ver secciones.spec y AMB-B4 del plan).
    'TESORERIA',
    'CONTABILIDAD',
  ],
  // Verificación de identidad, alertas, casos, actas, habilitación de organizadores.
  // Carril B · L2: las colas de UIF, requerimientos, actas, continuidad, riesgo e incidentes (CU-43/45/94/56/54/55) las abre el
  // ROL del plan §3 (oficial, riesgos, seguridad, auditor), no AUDITORIA_LEER: ese permiso lo tienen casi todos los roles y le
  // abriría Cumplimiento a Tesorería y Contabilidad. Cada ruta vuelve a filtrar y el servidor decide.
  'ver:cumplimiento': [
    'VERIFICACION_RESOLVER',
    'CUMPLIMIENTO_ALERTAS',
    'CUMPLIMIENTO_CASOS',
    'CUMPLIMIENTO_REPORTAR',
    'OFICIAL_CUMPLIMIENTO',
    'RESPONSABLE_RIESGOS',
    'RESPONSABLE_SEGURIDAD',
    'AUDITOR_INTERNO',
  ],
  // Accesos y factores de operadores, catálogos, despliegues e interruptores.
  // Solicitudes de ingreso a grupos: resolver la admisión es de la plataforma (`@Permiso("ADMIN_PLATAFORMA")` en `grupos`).
  'ver:admisiones': ['ADMIN_PLATAFORMA'],
  // Catálogo de datos con su ficha de gobierno (CU-130): el mismo permiso que exige su ruta.
  'ver:catalogo': ['TABLERO_SISTEMAS_VER'],
  'ver:sistemas': ['ACCESOS_ADMINISTRAR', 'SEGURIDAD_ACCESO_RESTABLECER', 'SEGURIDAD_FACTOR_REINSCRIBIR', 'CATALOGO_EDITAR'],
  'ver:contabilidad': [
    'CONTABILIDAD_ERP_CERRAR',
    'CONTABILIDAD_ERP_PRESUPUESTO',
    'CONTABILIDAD_ERP_COMPRAS',
    'CONTABILIDAD_ERP_CUENTAS_POR_PAGAR',
    'CONTABILIDAD_ERP_PAGAR',
    'CONTABILIDAD_ERP_COBRAR',
    'CONTABILIDAD_ERP_ACTIVOS_FIJOS',
    'CONTABILIDAD_ERP_REPORTES',
  ],
  'ver:publicidad': ['PUBLICIDAD_ANUNCIANTES', 'PUBLICIDAD_APROBAR_CAMPANA', 'PUBLICIDAD_LIQUIDAR', 'PUBLICIDAD_MODERAR'],
  // Tarifario, mensajería y reportes: `listarTarifarios` pide TARIFARIO_PUBLICAR o AUDITORIA_LEER (tarifas.yaml); los listados de
  // `notificaciones.yaml` y `auditoria.yaml` piden AUDITORIA_LEER. La sección la abren TARIFARIO_PUBLICAR y los roles a los que el
  // plan §3 da Reportes (oficial, auditor, riesgos); AUDITORIA_LEER solo no la abre (lo tienen casi todos los roles).
  'ver:configuracion': ['TARIFARIO_PUBLICAR', 'OFICIAL_CUMPLIMIENTO', 'AUDITOR_INTERNO', 'RESPONSABLE_RIESGOS'],
}

/**
 * ¿Estos permisos reales alcanzan para `permiso`?
 *
 * - Si el permiso viene tal cual en la sesión, alcanza (así lo arman las pruebas y
 *   cualquier otorgamiento explícito).
 * - `ver:tablero` lo tiene toda sesión abierta: es el punto de entrada.
 * - Otro `ver:*` se traduce con el mapa; uno que no esté en el mapa **no abre** (un
 *   nombre nuevo sin traducir queda cerrado, nunca abierto por omisión).
 */
export function alcanza(permiso: string, reales: readonly string[], sesionAbierta: boolean): boolean {
  if (!sesionAbierta) return false
  if (reales.includes(permiso)) return true
  if (permiso === 'ver:tablero') return true
  // El centro de ayuda lo tiene toda sesión abierta, como el tablero: aprender a usar
  // el backoffice no es un privilegio. Lo que se muestra ADENTRO sí se filtra por rol
  // (`RegistroDeTutoriales`), así que nadie ve el tutorial de una sección que no puede
  // abrir.
  if (permiso === 'ver:ayuda') return true
  if (permiso.startsWith('ver:')) {
    const requeridos = PERMISOS_POR_SECCION[permiso]
    return requeridos !== undefined && requeridos.some((codigo) => reales.includes(codigo))
  }
  return false
}
