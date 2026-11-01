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

  // Requisito cambiado el 2026-10-21 (pedido de Pablo: «comprimí las opciones»): antes todas las
  // secciones estaban siempre abiertas; ahora solo la activa se despliega y el resto se pliega.
  function abiertas(el: HTMLElement): string[] {
    return Array.from(el.querySelectorAll('.plegable.abierto .hijos')).map((u) => u.id)
  }

  it('correcto: solo la sección activa muestra sus pantallas, con sus íconos', async () => {
    const { el } = await montar(['ver:tablero', 'ver:operacion', 'ver:sistemas'], '/sistemas/servicios')
    expect(abiertas(el)).toEqual(['menu-hijos-sistemas'])
    const hijos = Array.from(el.querySelectorAll('.plegable.abierto .hijos a'))
    expect(hijos.map((a) => a.textContent?.trim())).toContain('Permisos internos')
    expect(el.querySelectorAll('.plegable.abierto .hijos a ap-icono-de-seccion').length).toBe(hijos.length)
    // Las pantallas plegadas no se alcanzan con el teclado.
    expect(el.querySelector('#menu-hijos-operacion')!.closest('.plegable')!.hasAttribute('inert')).toBe(true)
  })

  it('límite: en el inicio ninguna sección queda desplegada', async () => {
    const { el } = await montar(['ver:tablero', 'ver:operacion', 'ver:sistemas'], '/tablero')
    expect(abiertas(el)).toEqual([])
  })

  it('correcto: el botón de la flecha despliega y vuelve a plegar una sección, con aria-expanded', async () => {
    const { fixture, el } = await montar(['ver:tablero', 'ver:operacion', 'ver:sistemas'], '/tablero')
    const boton = el.querySelector<HTMLButtonElement>('button[aria-controls="menu-hijos-operacion"]')!
    expect(boton.getAttribute('aria-expanded')).toBe('false')
    expect(boton.getAttribute('aria-label')).toBe('Mostrar las pantallas de Operación')
    boton.click()
    fixture.detectChanges()
    expect(boton.getAttribute('aria-expanded')).toBe('true')
    expect(abiertas(el)).toEqual(['menu-hijos-operacion'])
    boton.click()
    fixture.detectChanges()
    expect(abiertas(el)).toEqual([])
  })

  it('correcto: al navegar a otra sección se pliega la anterior y se abre la nueva', async () => {
    const { fixture, el } = await montar(['ver:tablero', 'ver:operacion', 'ver:sistemas'], '/sistemas/servicios')
    await TestBed.inject(Router).navigateByUrl('/operacion/reclamos')
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(abiertas(el)).toEqual(['menu-hijos-operacion'])
  })

  it('límite: todas las opciones tienen ícono y solo las secciones con pantallas tienen botón de plegar', async () => {
    const { el } = await montar(['ver:tablero', 'ver:operacion', 'ver:cumplimiento', 'ver:admisiones'])
    const todosLosEnlaces = el.querySelectorAll('a')
    expect(el.querySelectorAll('a ap-icono-de-seccion').length).toBe(todosLosEnlaces.length)
    expect(Array.from(el.querySelectorAll('button.plegar')).map((b) => b.getAttribute('aria-controls'))).toEqual(['menu-hijos-operacion', 'menu-hijos-cumplimiento'])
  })
})
