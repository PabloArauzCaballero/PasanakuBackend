import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BarraDeFiltros } from './barra-de-filtros'

describe('BarraDeFiltros', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  })
  afterEach(() => vi.useRealTimers())

  function montar(busqueda = '', valor = '') {
    const fixture = TestBed.createComponent(BarraDeFiltros)
    fixture.componentRef.setInput('etiquetaDeBusqueda', 'Buscar tabla')
    fixture.componentRef.setInput('busqueda', busqueda)
    fixture.componentRef.setInput('selectores', [
      { nombre: 'estado', etiqueta: 'Estado', valor, opciones: [{ valor: 'ACTIVA', texto: 'Activa' }, { valor: 'RETIRADA', texto: 'Retirada' }] },
    ])
    const busquedas: string[] = []
    const cambios: { nombre: string; valor: string }[] = []
    let limpiados = 0
    fixture.componentInstance.busquedaChange.subscribe((v) => busquedas.push(v))
    fixture.componentInstance.selectorChange.subscribe((c) => cambios.push(c))
    fixture.componentInstance.limpiar.subscribe(() => limpiados++)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    return { fixture, el, busquedas, cambios, limpiados: () => limpiados }
  }

  it('correcto: escribir avisa recién al dejar de escribir 350 ms; la opción vacía dice «Todos»', () => {
    const { el, busquedas } = montar()
    const input = el.querySelector('input[type="search"]') as HTMLInputElement
    input.value = 'pa'
    input.dispatchEvent(new Event('input'))
    input.value = 'pago'
    input.dispatchEvent(new Event('input'))
    vi.advanceTimersByTime(300)
    expect(busquedas).toEqual([])
    vi.advanceTimersByTime(100)
    expect(busquedas).toEqual(['pago'])
    expect(el.querySelector('select option')?.textContent?.trim()).toBe('Todos')
  })

  it('correcto: elegir un selector emite su nombre y valor; «Limpiar» aparece solo con algo filtrado', () => {
    const { fixture, el, cambios } = montar()
    expect(el.querySelector('ap-boton')).toBeNull()
    const select = el.querySelector('select') as HTMLSelectElement
    select.value = 'ACTIVA'
    select.dispatchEvent(new Event('change'))
    expect(cambios).toEqual([{ nombre: 'estado', valor: 'ACTIVA' }])
    fixture.componentRef.setInput('selectores', [{ nombre: 'estado', etiqueta: 'Estado', valor: 'ACTIVA', opciones: [] }])
    fixture.detectChanges()
    expect(el.querySelector('ap-boton')?.textContent).toContain('Limpiar')
  })

  it('límite: borrar la búsqueda avisa al instante, sin esperar', () => {
    const { el, busquedas } = montar('pago')
    const input = el.querySelector('input[type="search"]') as HTMLInputElement
    input.value = ''
    input.dispatchEvent(new Event('input'))
    expect(busquedas).toEqual([''])
  })

  it('inválido: destruir la barra con una búsqueda pendiente no emite después', () => {
    const { fixture, el, busquedas } = montar()
    const input = el.querySelector('input[type="search"]') as HTMLInputElement
    input.value = 'x'
    input.dispatchEvent(new Event('input'))
    fixture.destroy()
    vi.advanceTimersByTime(500)
    expect(busquedas).toEqual([])
  })
})
