import { describe, expect, it } from 'vitest'
import { ubicar, type Medida } from './ubicacion'

const VENTANA: Medida = { ancho: 1280, alto: 900 }
const GLOBO: Medida = { ancho: 340, alto: 240 }
const centro = { x: 500, y: 400, ancho: 100, alto: 40 }

describe('ubicar el globo', () => {
  it('respeta el lado pedido cuando entra', () => {
    expect(ubicar(centro, 'abajo', VENTANA, GLOBO).lado).toBe('abajo')
    expect(ubicar(centro, 'derecha', VENTANA, GLOBO).lado).toBe('derecha')
  })

  it('lo planta debajo del elemento, con aire de por medio', () => {
    const u = ubicar(centro, 'abajo', VENTANA, GLOBO)
    expect(u.arriba).toBeGreaterThan(centro.y + centro.alto)
  })

  it('si arriba no entra, se va abajo en vez de salirse de la pantalla', () => {
    const pegadoArriba = { x: 500, y: 10, ancho: 100, alto: 40 }
    expect(ubicar(pegadoArriba, 'arriba', VENTANA, GLOBO).lado).toBe('abajo')
  })

  it('si el elemento está pegado al borde derecho, el globo no se sale', () => {
    const pegadoDerecha = { x: 1240, y: 400, ancho: 40, alto: 40 }
    const u = ubicar(pegadoDerecha, 'derecha', VENTANA, GLOBO)
    expect(u.izquierda + GLOBO.ancho).toBeLessThanOrEqual(VENTANA.ancho)
  })

  it('sin recuadro va centrado: un paso que no señala nada habla de la pantalla entera', () => {
    const u = ubicar(null, 'abajo', VENTANA, GLOBO)
    expect(u.lado).toBe('centro')
    expect(u.izquierda).toBe((VENTANA.ancho - GLOBO.ancho) / 2)
  })

  it('en una ventana chiquita cae centrado antes que cortado', () => {
    const chica: Medida = { ancho: 360, alto: 300 }
    const u = ubicar(centro, 'derecha', chica, GLOBO)
    expect(u.lado).toBe('centro')
    expect(u.izquierda).toBeGreaterThanOrEqual(0)
    expect(u.arriba).toBeGreaterThanOrEqual(0)
  })

  it('el lado «centro» pedido se respeta siempre', () => {
    expect(ubicar(centro, 'centro', VENTANA, GLOBO).lado).toBe('centro')
  })
})

describe('ubicar · no pisar lo que se señala', () => {
  it('de costado no vale «casi entra»: si no cabe entero, se va al centro', () => {
    // Un elemento pegado al borde izquierdo: a la izquierda no hay 340 px libres.
    const pegadoIzquierda = { x: 20, y: 300, ancho: 200, alto: 40 }
    expect(ubicar(pegadoIzquierda, 'izquierda', VENTANA, GLOBO).lado).not.toBe('izquierda')
  })

  it('un elemento tan alto que no deja lugar ni arriba ni abajo cae centrado', () => {
    const casiTodaLaPantalla = { x: 260, y: 180, ancho: 900, alto: 250 }
    const chica: Medida = { ancho: 1200, alto: 677 }
    expect(ubicar(casiTodaLaPantalla, 'abajo', chica, { ancho: 340, alto: 320 }).lado).toBe('centro')
  })

  it('cuando de un lado entra, se usa ese lado y no el centro', () => {
    const arribaDeTodo = { x: 500, y: 60, ancho: 120, alto: 40 }
    expect(ubicar(arribaDeTodo, 'abajo', VENTANA, GLOBO).lado).toBe('abajo')
  })
})
