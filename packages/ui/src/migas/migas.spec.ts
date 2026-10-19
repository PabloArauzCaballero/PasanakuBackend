import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { Migas } from './migas'

describe('Migas', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar() {
    const fixture = TestBed.createComponent(Migas)
    fixture.componentRef.setInput('migas', [
      { texto: 'Inicio', ruta: '/tablero' },
      { texto: 'Sistemas', ruta: '/sistemas' },
      { texto: 'Catálogo de datos' },
    ])
    const destinos: string[] = []
    fixture.componentInstance.navegar.subscribe((r) => destinos.push(r))
    fixture.detectChanges()
    return { el: fixture.nativeElement as HTMLElement, destinos }
  }

  it('correcto: una navegación con nombre, enlaces para las migas con ruta y la última como página actual', () => {
    const { el } = montar()
    expect(el.getAttribute('role')).toBe('navigation')
    expect(el.getAttribute('aria-label')).toBe('Migas de pan')
    expect(Array.from(el.querySelectorAll('a')).map((a) => a.getAttribute('href'))).toEqual(['/tablero', '/sistemas'])
    const actual = el.querySelector('[aria-current="page"]')
    expect(actual?.textContent?.trim()).toBe('Catálogo de datos')
    expect(actual?.tagName).toBe('SPAN')
  })

  it('correcto: el clic en una miga no recarga la página y emite la ruta', () => {
    const { el, destinos } = montar()
    const evento = new MouseEvent('click', { cancelable: true, bubbles: true })
    el.querySelector('a')!.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(true)
    expect(destinos).toEqual(['/tablero'])
  })

  it('límite: hay un separador menos que migas', () => {
    const { el } = montar()
    expect(el.querySelectorAll('.separador')).toHaveLength(2)
  })
})
