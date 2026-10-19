import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { ListaDeOpciones } from './lista-de-opciones'
import { Esqueleto } from '../esqueleto/esqueleto'

describe('ListaDeOpciones y Esqueleto', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(extra: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(ListaDeOpciones)
    fixture.componentRef.setInput('etiqueta', 'Estado')
    fixture.componentRef.setInput('opciones', [{ valor: 'A', texto: 'Activa', descripcion: 'En uso' }])
    for (const [k, v] of Object.entries(extra)) fixture.componentRef.setInput(k, v)
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement, c: fixture.componentInstance }
  }

  it('correcto: etiqueta vinculada al select, ayuda por aria-describedby y tooltip de ayuda con nombre', () => {
    const { el } = montar({ ayuda: 'Si sigue en uso.', tooltip: 'Más detalle.' })
    const select = el.querySelector('select') as HTMLSelectElement
    expect(el.querySelector('label')?.getAttribute('for')).toBe(select.id)
    expect(select.getAttribute('aria-describedby')).toBe(`${select.id}-ayuda`)
    expect(el.querySelector('.ayuda-boton')?.getAttribute('aria-label')).toBe('Ayuda sobre Estado')
    expect(el.querySelector('option[value="A"]')?.getAttribute('title')).toBe('En uso')
  })

  it('correcto: cambiar emite el valor; el error reemplaza a la ayuda y marca aria-invalid', () => {
    const { fixture, el, c } = montar({ ayuda: 'x' })
    const select = el.querySelector('select') as HTMLSelectElement
    select.value = 'A'
    select.dispatchEvent(new Event('change'))
    expect(c.valor()).toBe('A')
    fixture.componentRef.setInput('error', 'Elegí un estado.')
    fixture.detectChanges()
    expect(select.getAttribute('aria-invalid')).toBe('true')
    expect(select.getAttribute('aria-describedby')).toBe(`${select.id}-error`)
    expect(el.querySelector('[role="alert"]')?.textContent).toBe('Elegí un estado.')
  })

  it('esqueleto: la forma de filas pinta tantas filas como se pidan y se anuncia una vez', () => {
    const fixture = TestBed.createComponent(Esqueleto)
    fixture.componentRef.setInput('forma', 'filas')
    fixture.componentRef.setInput('lineas', 6)
    fixture.componentRef.setInput('etiqueta', 'Consultando el catálogo')
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    expect(el.getAttribute('role')).toBe('status')
    expect(el.getAttribute('aria-label')).toBe('Consultando el catálogo')
    expect(el.querySelectorAll('span')).toHaveLength(6)
    expect(el.classList.contains('forma-filas')).toBe(true)
  })
})
