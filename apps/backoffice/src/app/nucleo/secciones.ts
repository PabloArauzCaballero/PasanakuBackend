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
  'ver:operacion': ['BILLETERA_VER_TERCEROS', 'RECLAMO_ATENDER', 'ENTREGA_AUTORIZAR', 'ENTREGA_EJECUTAR', 'REVERSO_AUTORIZAR'],
  // Verificación de identidad, alertas, casos, actas, habilitación de organizadores.
  'ver:cumplimiento': ['VERIFICACION_RESOLVER', 'CUMPLIMIENTO_ALERTAS', 'CUMPLIMIENTO_CASOS', 'CUMPLIMIENTO_REPORTAR'],
  // Accesos y factores de operadores, catálogos, despliegues e interruptores.
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
  if (permiso.startsWith('ver:')) {
    const requeridos = PERMISOS_POR_SECCION[permiso]
    return requeridos !== undefined && requeridos.some((codigo) => reales.includes(codigo))
  }
  return false
}
