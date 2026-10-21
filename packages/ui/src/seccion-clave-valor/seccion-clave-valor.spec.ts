import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { SeccionClaveValor } from './seccion-clave-valor'

describe('SeccionClaveValor', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar() {
    const fixture = TestBed.createComponent(SeccionClaveValor)
    fixture.componentRef.setInput('titulo', 'Identificación técnica')
    fixture.componentRef.setInput('descripcion', 'Cómo existe la tabla en la base.')
    fixture.componentRef.setInput('items', [
      { etiqueta: 'Esquema', valor: 'aportes', mono: true },
      { etiqueta: 'Append-only', valor: false },
      { etiqueta: 'Crítica', valor: true, tono: 'aviso' },
      { etiqueta: 'Responsable', valor: null },
      { etiqueta: 'Columnas', valor: 12 },
    ])
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('correcto: región con nombre, cabecera, y cada rótulo con su valor en palabras', () => {
    const el = montar()
    expect(el.getAttribute('role')).toBe('region')
    expect(el.getAttribute('aria-label')).toBe('Identificación técnica')
    expect(el.querySelector('.cabecera p')?.textContent).toBe('Cómo existe la tabla en la base.')
    // Un valor mono trae además el botón de copiar (con su tooltip): se lee el código, no el texto entero.
    const filas = Array.from(el.querySelectorAll('dl > div')).map((f) => [f.querySelector('dt')?.textContent, (f.querySelector('dd code') ?? f.querySelector('dd'))?.textContent?.trim()])
    expect(filas).toEqual([
      ['Esquema', 'aportes'],
      ['Append-only', 'No'],
      ['Crítica', 'Sí'],
      ['Responsable', '—'],
      ['Columnas', '12'],
    ])
  })

  it('correcto: un valor mono va como código copiable con nombre; el tono se aplica a la fila', () => {
    const el = montar()
    const codigo = el.querySelector('ap-codigo-copiable')
    expect(codigo?.querySelector('code')?.textContent).toBe('aportes')
    expect(codigo?.querySelector('button')?.getAttribute('aria-label')).toBe('Copiar Esquema')
    expect(el.querySelectorAll('dl > div')[2]?.classList.contains('tono-aviso')).toBe(true)
  })

  it('texto largo: párrafos y viñetas de la narrativa quedan como <p>, <ul> y <ol>, no como texto corrido', () => {
    const fixture = TestBed.createComponent(SeccionClaveValor)
    fixture.componentRef.setInput('titulo', 'Contexto')
    fixture.componentRef.setInput('items', [
      { etiqueta: 'Para qué sirve', texto: true, valor: 'Resuelve problemas:\n\n- Varias órdenes.\n- Glosa conciliable.\n\nCierre.\n1. Primero.\n2. Después.' },
      { etiqueta: 'Qué es', texto: true, valor: 'Una sola línea.' },
    ])
    fixture.detectChanges()
    const [larga, corta] = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('dd'))
    expect(Array.from(larga!.querySelectorAll('p')).map((p) => p.textContent)).toEqual(['Resuelve problemas:', 'Cierre.'])
    expect(Array.from(larga!.querySelectorAll('ul > li')).map((li) => li.textContent)).toEqual(['Varias órdenes.', 'Glosa conciliable.'])
    expect(Array.from(larga!.querySelectorAll('ol > li')).map((li) => li.textContent)).toEqual(['Primero.', 'Después.'])
    expect(corta!.querySelector('p')?.textContent).toBe('Una sola línea.')
  })
})
