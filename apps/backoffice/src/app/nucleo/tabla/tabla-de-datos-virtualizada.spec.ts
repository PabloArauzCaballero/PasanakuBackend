import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { TablaDeDatosVirtualizada } from './tabla-de-datos-virtualizada'
import type { CargadorDePagina, PaginaServidor } from './tipos'

type Fila = { id: string; nombre: string; monto: number }

/** Un dataset grande, paginado por el "servidor" en memoria — nunca se manda entero al cliente. */
const TOTAL = 100_000
const servidor: CargadorDePagina<Fila> = async (pedido) => {
  const desde = (pedido.pagina - 1) * pedido.tamano
  const filas = Array.from({ length: Math.min(pedido.tamano, Math.max(0, TOTAL - desde)) }, (_, i) => ({
    id: String(desde + i),
    nombre: `Fila ${desde + i}`,
    monto: desde + i,
  }))
  return { filas, total: TOTAL } satisfies PaginaServidor<Fila>
}

describe('TablaDeDatosVirtualizada · dataset grande paginado del servidor', () => {
  beforeEach(() => {
    // jsdom no implementa `Element.scrollTo`; el CDK lo usa para `scrollToIndex`.
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  })

  async function montar(cargador: CargadorDePagina<Fila> = servidor) {
    const fixture = TestBed.createComponent(TablaDeDatosVirtualizada<Fila>)
    fixture.componentRef.setInput('titulo', 'Prueba de carga')
    fixture.componentRef.setInput('columnas', [
      { clave: 'nombre', titulo: 'Nombre', ordenable: true },
      { clave: 'monto', titulo: 'Monto', ordenable: true, numerica: true },
    ])
    fixture.componentRef.setInput('cargador', cargador)
    fixture.componentRef.setInput('ordenPermitido', ['nombre', 'monto'])
    fixture.componentRef.setInput('tamanoDePagina', 50)
    fixture.detectChanges()
    await fixture.whenStable()
    return fixture
  }

  it('pide solo la página actual al "servidor", nunca las 100 000 filas juntas', async () => {
    const fixture = await montar()
    const instancia = fixture.componentInstance
    expect(instancia.total()).toBe(TOTAL)
    expect(instancia.filas().length).toBe(50)
  })

  it('cambiar de página vuelve a pedir al servidor, con el tamaño de página fijo', async () => {
    const fixture = await montar()
    fixture.componentInstance.pagina.set(3)
    fixture.detectChanges()
    await fixture.whenStable()
    expect(fixture.componentInstance.filas()[0]?.id).toBe('100')
  })

  it('ordenar por un campo permitido cambia el orden y no marca error', async () => {
    const fixture = await montar()
    fixture.componentInstance.ordenarPor('monto')
    expect(fixture.componentInstance.orden()).toEqual({ clave: 'monto', sentido: 'asc' })
    expect(fixture.componentInstance.error()).toBeNull()
  })

  it('ordenar por un campo fuera de la lista blanca se rechaza de forma visible, no se ignora', async () => {
    const fixture = await montar()
    const ordenAntes = fixture.componentInstance.orden()
    fixture.componentInstance.ordenarPor('idSecreto')
    expect(fixture.componentInstance.orden()).toEqual(ordenAntes)
    expect(fixture.componentInstance.error()).toContain('idSecreto')
  })

  it('la selección múltiple acumula ids entre filas marcadas', async () => {
    const fixture = await montar()
    fixture.componentInstance.marcar('3', true)
    fixture.componentInstance.marcar('7', true)
    expect(fixture.componentInstance.elegidas()).toEqual(['3', '7'])
    fixture.componentInstance.marcar('3', false)
    expect(fixture.componentInstance.elegidas()).toEqual(['7'])
  })

  it('la navegación fila a fila mueve el foco lógico sin recargar del servidor', async () => {
    const fixture = await montar()
    const antes = fixture.componentInstance.total()
    const evento = new KeyboardEvent('keydown', { key: 'ArrowDown' })
    fixture.componentInstance.onKeydown(evento, 0)
    expect(fixture.componentInstance.focoIndice()).toBe(1)
    expect(fixture.componentInstance.total()).toBe(antes)
  })
})
