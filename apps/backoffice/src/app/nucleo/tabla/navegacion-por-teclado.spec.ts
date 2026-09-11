import { describe, expect, it } from 'vitest'
import { accionParaTecla } from './navegacion-por-teclado'

describe('accionParaTecla · navegación fila a fila', () => {
  it('ArrowDown mueve al índice siguiente, sin pasar del último', () => {
    expect(accionParaTecla('ArrowDown', 2, 5, false)).toEqual({ tipo: 'mover', indice: 3 })
    expect(accionParaTecla('ArrowDown', 5, 5, false)).toEqual({ tipo: 'mover', indice: 5 })
  })

  it('ArrowUp mueve al índice anterior, sin bajar de cero', () => {
    expect(accionParaTecla('ArrowUp', 2, 5, false)).toEqual({ tipo: 'mover', indice: 1 })
    expect(accionParaTecla('ArrowUp', 0, 5, false)).toEqual({ tipo: 'mover', indice: 0 })
  })

  it('Home y End van a los extremos de la página cargada', () => {
    expect(accionParaTecla('Home', 3, 9, false)).toEqual({ tipo: 'mover', indice: 0 })
    expect(accionParaTecla('End', 3, 9, false)).toEqual({ tipo: 'mover', indice: 9 })
  })

  it('Espacio y Enter alternan selección solo si la tabla es seleccionable', () => {
    expect(accionParaTecla(' ', 1, 9, true)).toEqual({ tipo: 'alternar' })
    expect(accionParaTecla('Enter', 1, 9, true)).toEqual({ tipo: 'alternar' })
    expect(accionParaTecla(' ', 1, 9, false)).toBeNull()
  })

  it('una tecla que no navega ni selecciona no produce ninguna acción', () => {
    expect(accionParaTecla('a', 1, 9, true)).toBeNull()
  })
})
