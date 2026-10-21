import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { Sesion } from '../nucleo/sesion'
import { MenuLateral } from './menu-lateral'

@Component({ template: '' })
class Vacia {}

describe('MenuLateral', () => {
  let sesion: Sesion
  beforeEach(() => {
    sessionStorage.clear()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Vacia }])] })
    sesion = TestBed.inject(Sesion)
  })

  async function montar(permisos: string[], url = '/tablero') {
    sesion.abrir('token', permisos, 'ADMIN_PLATAFORMA')
    await TestBed.inject(Router).navigateByUrl(url)
    const fixture = TestBed.createComponent(MenuLateral)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement }
  }

  it('correcto: solo los grupos con alguna entrada permitida; cada entrada conserva su gancho de tutorial', async () => {
    const { el } = await montar(['ver:tablero', 'ver:operacion'])
    expect(Array.from(el.querySelectorAll('.rotulo span')).map((s) => s.textContent)).toEqual(['Operación', 'Plataforma'])
    expect(Array.from(el.querySelectorAll('a[data-tutorial-id]')).map((a) => a.getAttribute('data-tutorial-id'))).toEqual(['menu-tablero', 'menu-operacion', 'menu-ayuda'])
    expect(el.getAttribute('data-tutorial-id')).toBe('menu-principal')
  })

  it('correcto: una entrada por sección (las pantallas van en las pestañas de sección); la activa se marca; plegar un grupo se recuerda', async () => {
    const { fixture, el } = await montar(['ver:tablero', 'ver:sistemas'], '/sistemas/outbox')
    const enlaces = Array.from(el.querySelectorAll('a')).map((a) => a.textContent?.trim())
    expect(enlaces).not.toContain('Outbox y descartados')
    expect(el.querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Sistemas')
    const rotulo = Array.from(el.querySelectorAll('.rotulo')).find((b) => b.textContent?.includes('Plataforma')) as HTMLButtonElement
    expect(rotulo.getAttribute('aria-expanded')).toBe('true')
    rotulo.click()
    fixture.detectChanges()
    expect(rotulo.getAttribute('aria-expanded')).toBe('false')
    expect(JSON.parse(sessionStorage.getItem('ap-menu:plegados') ?? '[]')).toEqual(['Plataforma'])
  })

  it('límite: el grupo de la ruta activa se abre aunque esté plegado', async () => {
    sessionStorage.setItem('ap-menu:plegados', JSON.stringify(['Control']))
    const { el } = await montar(['ver:tablero', 'ver:sistemas'], '/sistemas')
    const rotulo = Array.from(el.querySelectorAll('.rotulo')).find((b) => b.textContent?.includes('Control')) as HTMLButtonElement
    expect(rotulo.getAttribute('aria-expanded')).toBe('true')
  })
})
