/**
 * Tipos de datos para el explicador de vistas del backoffice de AportaYa.
 * Sigue el patrón probado de Atlas Admin Portal, con separación nítida entre
 * el plano de negocio (por qué existe, valor al usuario, decisiones) y el plano
 * de sistemas (cómo funciona técnicamente, endpoints, tablas, eventos y permisos).
 */

export type ExplicacionDeVista = {
  /**
   * Cómo funciona a nivel técnico: arquitectura interna, endpoints REST/RPC,
   * tablas PostgreSQL afectadas, restricciones transaccionales, eventos outbox/Kafka
   * y permisos RBAC exigidos.
   */
  sistemas: string
  /**
   * Para qué sirve a nivel negocio: el trabajo que la pantalla realiza (Jobs to Be Done),
   * qué valor y tranquilidad le da a las personas usuarias del pasanaku, qué decisiones
   * toma el operador y qué riesgos financieros o regulatorios previene. Redactado en lenguaje
   * natural, claro y accesible para un usuario común.
   */
  negocio: string
}

export type ExplicacionDeModulo = {
  /** Nombre visible del módulo en la navegación (ej. 'Operación', 'Cumplimiento', 'Sistemas'). */
  modulo: string
  /** Prefijos de ruta que pertenecen a este módulo. */
  prefijos: string[]
  /** Si es true, el prefijo solo matchea si la ruta es idéntica (ej. '/tablero' para el inicio). */
  exacto?: boolean
  /** Explicación de arquitectura técnica general del módulo. */
  sistemas: string
  /** Explicación de impacto y razón de ser de negocio general del módulo. */
  negocio: string
  /** Mapa de explicaciones específicas por cada vista o subruta del módulo. */
  vistas: Record<string, ExplicacionDeVista>
}

export type ExplicacionResuelta = {
  modulo: ExplicacionDeModulo
  vista: ExplicacionDeVista | null
}
