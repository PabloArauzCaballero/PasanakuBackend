import { computed, inject, type Signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'

/** Las etiquetas de un enum del contrato como chips de `BarraDeFiltros` (el valor es el código, el texto la etiqueta). */
export const chipsDe = (etiquetas: Readonly<Record<string, string>>): Filtro[] => Object.entries(etiquetas).map(([valor, texto]) => ({ valor, texto }))

/**
 * El filtro de estado que `BarraDeFiltros` guarda en la URL (`?filtro=A,B`) como `{ estado: 'A,B' }` para el cargador, que
 * lo manda como `estados` repetido. Solo valores que el contrato declara: un código pegado a mano en la URL no viaja.
 */
export function filtroDeEstadoDesdeUrl(permitidos: readonly string[]): Signal<Readonly<Record<string, string>>> {
  const consulta = toSignal(inject(ActivatedRoute).queryParamMap, { requireSync: false })
  return computed<Readonly<Record<string, string>>>(() => {
    const elegidos = (consulta()?.get('filtro') ?? '').split(',').filter((v) => permitidos.includes(v))
    const filtros: Record<string, string> = {}
    if (elegidos.length) filtros['estado'] = elegidos.join(',')
    return filtros
  })
}
