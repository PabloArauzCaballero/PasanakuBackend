import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { Paginacion } from './paginacion'

describe('Paginacion', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(total = 134, opciones: number[] = [10, 20, 50]) {
    const fixture = TestBed.createComponent(Paginacion)
    fixture.componentRef.setInput('totalDeFilas', total)
    fixture.componentRef.setInput('opcionesPorPagina', opciones)
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement, c: fixture.componentInstance }
  }

  it('correcto: dice página, total y registros; Anterior deshabilitado en la primera', () => {
    const { el } = montar()
    expect(el.querySelector('.resumen')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('Página 1 de 7 · 134 registros')
    const [anterior, siguiente] = Array.from(el.querySelectorAll('.pasos button')) as HTMLButtonElement[]
    expect(anterior!.disabled).toBe(true)
    expect(siguiente!.disabled).toBe(false)
  })

  it('correcto: cambiar las filas por página recalcula el total y vuelve a la primera', () => {
    const { fixture, el, c } = montar()
    c.pagina.set(5)
    const select = el.querySelector('select') as HTMLSelectElement
    select.value = '50'
    select.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    expect(c.porPagina()).toBe(50)
    expect(c.pagina()).toBe(1)
    expect(c.total()).toBe(3)
  })

  it('límite: ir a una página fuera de rango se acota; con basura no se mueve', () => {
    const { c } = montar()
    c.irA('99')
    expect(c.pagina()).toBe(7)
    c.irA('0')
    expect(c.pagina()).toBe(1)
    c.pagina.set(3)
    c.irA('abc')
    expect(c.pagina()).toBe(3)
  })

  it('límite: con una sola opción no se ofrece cambiar el tamaño, y con dos páginas no hay «Ir a»', () => {
    const { el } = montar(30, [20])
    expect(el.querySelector('select')).toBeNull()
    expect(el.querySelector('.ir-a')).toBeNull()
  })
})
