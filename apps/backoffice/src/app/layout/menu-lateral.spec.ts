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

  async function montar(permisos: string[], url = '/tablero', rol = 'ADMIN_PLATAFORMA') {
    sesion.abrir('token', permisos, rol)
    await TestBed.inject(Router).navigateByUrl(url)
    const fixture = TestBed.createComponent(MenuLateral)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    return { fixture, el: fixture.nativeElement as HTMLElement }
  }

  it('correcto: solo las entradas con alguna pestaña que la sesión abre; cada entrada conserva su gancho de tutorial', async () => {
    const { el } = await montar(['ver:tablero', 'RECLAMO_VER'])
    // Sin entradas de Plataforma, ese grupo no se dibuja.
    expect(Array.from(el.querySelectorAll('.rotulo span')).map((s) => s.textContent)).toEqual([])
    expect(Array.from(el.querySelectorAll('a[data-tutorial-id]')).map((a) => a.getAttribute('data-tutorial-id'))).toEqual(['menu-tablero', 'menu-operacion'])
    // La entrada lleva a la pestaña que abre, no a una fija que rebotaría.
    expect(el.querySelector('a[data-tutorial-id="menu-operacion"]')?.getAttribute('href')).toBe('/operacion/reclamos')
    expect(el.getAttribute('data-tutorial-id')).toBe('menu-principal')
  })

  it('límite (F1): con el permiso de sistemas pero sin el rol, Sistemas no se dibuja; con PLATAFORMA lleva a /sistemas/servicios', async () => {
    const sinRol = await montar(['ACCESOS_ADMINISTRAR'], '/tablero', 'oficial')
    expect(sinRol.el.querySelector('a[data-tutorial-id="menu-sistemas"]')).toBeNull()
    sinRol.fixture.destroy()
    const conRol = await montar(['ACCESOS_ADMINISTRAR'], '/tablero', 'PLATAFORMA')
    expect(conRol.el.querySelector('a[data-tutorial-id="menu-sistemas"]')?.getAttribute('href')).toBe('/sistemas/servicios')
  })

  it('correcto: una entrada por sección (las pantallas van en las pestañas de sección); la activa se marca; plegar un grupo se recuerda', async () => {
    const { fixture, el } = await montar(['ver:tablero', 'ver:sistemas'], '/sistemas/outbox', 'PLATAFORMA')
    const enlaces = Array.from(el.querySelectorAll('a')).map((a) => a.textContent?.trim())
    expect(enlaces).not.toContain('Outbox y descartados')
    expect(el.querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Sistemas')
    // El grupo de la sección activa no se pliega; afuera de ella, sí.
    await TestBed.inject(Router).navigateByUrl('/tablero')
    fixture.detectChanges()
    const rotulo = Array.from(el.querySelectorAll('.rotulo')).find((b) => b.textContent?.includes('Plataforma')) as HTMLButtonElement
    expect(rotulo.getAttribute('aria-expanded')).toBe('true')
    rotulo.click()
    fixture.detectChanges()
    expect(rotulo.getAttribute('aria-expanded')).toBe('false')
    expect(JSON.parse(sessionStorage.getItem('ap-menu:plegados') ?? '[]')).toEqual(['Plataforma'])
  })

  it('límite: el grupo de la ruta activa se abre aunque esté plegado', async () => {
    sessionStorage.setItem('ap-menu:plegados', JSON.stringify(['Plataforma']))
    const { el } = await montar(['ver:tablero', 'ver:sistemas'], '/sistemas/servicios', 'PLATAFORMA')
    const rotulo = Array.from(el.querySelectorAll('.rotulo')).find((b) => b.textContent?.includes('Plataforma')) as HTMLButtonElement
    expect(rotulo.getAttribute('aria-expanded')).toBe('true')
  })
})
