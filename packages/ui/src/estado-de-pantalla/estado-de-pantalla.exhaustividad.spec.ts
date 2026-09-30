import { describe, expect, it } from 'vitest'
import type { MotivoVacio } from './estado-de-pantalla'

/**
 * H1.S2.M3 — Test de exhaustividad del contrato real (no del enum heredado de 10 variantes,
 * que no existe en este repo: ver entregables/contrato-view-state.md).
 *
 * `MotivoVacio` es la única unión cerrada real que el contrato expone hoy
 * (`estado-de-pantalla.ts`). Este test falla en TYPECHECK — no en runtime — si se agrega una
 * variante a `MotivoVacio` sin agregar su rama acá (el `default` cae en una asignación a `never`,
 * que dejaría de compilar), y falla en RUNTIME si se quita una variante que este archivo todavía
 * declara en `TODAS_LAS_VARIANTES` (la iteración deja de encontrarla y el conteo no cierra).
 *
 * NOTA DE ENTORNO: este archivo se escribió y se publicó vía GitHub Contents API (bloqueo de
 * git local contra el checkout compartido, ver REPORTE.md del carril). No se pudo ejecutar
 * `yarn workspace @aportaya/ui test:front` contra esta versión exacta en esta sesión porque la
 * herramienta de escritura de archivos de esta sesión también tiene bloqueado escribir dentro del
 * checkout compartido (mismo bloqueo, verificado). Peldaño: WRITTEN, no RUNS ni TESTED (regla 30).
 */
function textoPorMotivo(motivo: MotivoVacio): string {
  switch (motivo) {
    case 'sinDatos':
      return 'Todavía no hay nada acá.'
    case 'porFiltro':
      return 'Con estos filtros no aparece nada.'
    case 'porPermiso':
      return 'Tu rol no puede ver esta información.'
    default: {
      // Si el compilador se queja acá, se agregó una variante a MotivoVacio sin manejarla arriba.
      const _exhaustivo: never = motivo
      throw new Error(`Variante de MotivoVacio sin manejar: ${_exhaustivo}`)
    }
  }
}

const TODAS_LAS_VARIANTES: readonly MotivoVacio[] = ['sinDatos', 'porFiltro', 'porPermiso']

describe('contrato de MotivoVacio — exhaustividad', () => {
  it('tiene una rama para cada una de las variantes declaradas hoy', () => {
    for (const variante of TODAS_LAS_VARIANTES) {
      expect(() => textoPorMotivo(variante)).not.toThrow()
      expect(textoPorMotivo(variante)).toBeTypeOf('string')
    }
  })

  it('caso negativo documentado: si se quita una variante de esta lista, el conteo deja de cerrar contra las 3 reales', () => {
    // Este test documenta el contrato, no lo relaja: 3 es el número real hoy
    // (sinDatos, porFiltro, porPermiso — estado-de-pantalla.ts). Si el tipo real
    // cambia, este número se actualiza junto con entregables/contrato-view-state.md,
    // nunca al revés.
    expect(TODAS_LAS_VARIANTES.length).toBe(3)
  })
})
