/**
 * El singular del nombre de registro de un listado, para no escribir «1 anunciantes» (2026-10-27). La mayoría solo
 * pierde la «s» final; los que cambian más («conciliaciones» → «conciliación», «órdenes» → «orden») van en la tabla.
 */
const IRREGULARES: Readonly<Record<string, string>> = {
  conciliaciones: 'conciliación',
  ejecuciones: 'ejecución',
  migraciones: 'migración',
  versiones: 'versión',
  solicitudes: 'solicitud',
  proveedores: 'proveedor',
  roles: 'rol',
  órdenes: 'orden',
  'eventos (aprox.)': 'evento (aprox.)',
}

export function singularDe(plural: string): string {
  return IRREGULARES[plural] ?? plural.replace(/s$/, '')
}

/** «1 alerta», «2 alertas». */
export function nombreSegunCantidad(cantidad: number, plural: string): string {
  return cantidad === 1 ? singularDe(plural) : plural
}
