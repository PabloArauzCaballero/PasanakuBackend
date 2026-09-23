import type { Respaldo } from './puertos'

/**
 * Las dos funciones puras del dominio `sistemas` (H2.S1.M2): lo único que se queda
 * acá tras mover los arrays de ejemplo a `@aportaya/simulado/backoffice-sistemas`. No
 * dependen de qué fuente entregue los datos — valen igual con la fuente simulada o con
 * la real, el día que exista.
 */

/**
 * Un interruptor que toca dinero exige dos personas distintas: quien lo pide no puede
 * ser quien lo confirma. `puedeConfirmar` es la regla pura; la pantalla la envuelve en
 * un diálogo de doble confirmación simulado (no hay todavía un flujo real de "otra
 * sesión" en el backoffice — se simula pidiendo el correo de quien confirma).
 */
export function puedeConfirmar(solicitanteEmail: string, confirmanteEmail: string): boolean {
  const s = solicitanteEmail.trim().toLowerCase()
  const c = confirmanteEmail.trim().toLowerCase()
  return c.length > 0 && c !== s
}

/** Una restauración vencida es la que se probó hace más de 30 días, o nunca se probó. */
export function restauracionVencida(respaldo: Respaldo, hoy: Date = new Date()): boolean {
  if (!respaldo.ultimaRestauracionProbadaEl) return true
  const probada = new Date(respaldo.ultimaRestauracionProbadaEl)
  const dias = (hoy.getTime() - probada.getTime()) / (1000 * 60 * 60 * 24)
  return dias > 30
}
