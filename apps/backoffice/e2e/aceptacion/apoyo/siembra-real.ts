import type { Mundo } from './escenario'

/**
 * Siembra del modo REAL: cuentas sintéticas con credenciales generadas en el momento (nunca en el repositorio) contra el stack completo.
 *
 * El stack lo levanta el carril D (evidencia/carril-D3/ENTORNO-E2E.md). Este módulo es el único punto donde la suite sabe cómo se
 * crean los usuarios en ese stack; el resto de la suite no cambia entre modos.
 */
export async function sembrarReal(): Promise<Mundo> {
  throw new Error('BLOQUEADO: siembra del modo real no implementada todavía (falta el contrato de siembra del stack, evidencia/carril-D3/ENTORNO-E2E.md).')
}
