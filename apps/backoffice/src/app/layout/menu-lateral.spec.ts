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

  it('correcto: bajo cualquier sección permitida sus pantallas están siempre abiertas y visibles con sus respectivos íconos', async () => {
    const { el } = await montar(['ver:tablero', 'ver:sistemas'], '/tablero')
    const hijos = Array.from(el.querySelectorAll('.hijos a')).map((a) => a.textContent?.trim())
    expect(hijos).toContain('Outbox y descartados')
    expect(hijos).toContain('Servicios y SLO')
    expect(hijos).toContain('Webhooks')
    // Cada hijo tiene su ícono respectivo
    const iconosHijos = el.querySelectorAll('.hijos a ap-icono-de-seccion')
    expect(iconosHijos.length).toBe(hijos.length)
  })

  it('límite: todas las opciones tienen ícono asignado y no existen botones de despliegue o colapso', async () => {
    const { el } = await montar(['ver:tablero', 'ver:operacion', 'ver:cumplimiento'])
    expect(el.querySelectorAll('.chevron')).toHaveLength(0)
    expect(el.querySelectorAll('.boton-chevron-hijos')).toHaveLength(0)
    // Todas las opciones principales e hijas cuentan con icono de seccion
    const todosLosEnlaces = el.querySelectorAll('a')
    const iconos = el.querySelectorAll('a ap-icono-de-seccion')
    expect(iconos.length).toBe(todosLosEnlaces.length)
  })
})
