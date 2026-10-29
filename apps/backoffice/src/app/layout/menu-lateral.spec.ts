import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
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
  const textos = (el: Element, selector: string) => Array.from(el.querySelectorAll(selector)).map((a) => a.textContent?.trim())

  it('correcto: solo las entradas con alguna pantalla que la sesión abre; sin «Inicio» (al inicio se vuelve con el logo)', async () => {
    const { el } = await montar(['ver:tablero', 'RECLAMO_VER'])
    expect(textos(el, '.rotulo')).toEqual([])
    expect(Array.from(el.querySelectorAll('a[data-tutorial-id]')).map((a) => a.getAttribute('data-tutorial-id'))).toEqual(['menu-operacion'])
    expect(textos(el, 'a')).not.toContain('Inicio')
    // La entrada lleva a la pantalla que abre, no a una fija que rebotaría; debajo, solo la que puede abrir.
    expect(el.querySelector('a[data-tutorial-id="menu-operacion"]')?.getAttribute('href')).toBe('/operacion/reclamos')
    expect(textos(el, '.pantallas a')).toEqual(['Reclamos'])
    expect(el.getAttribute('data-tutorial-id')).toBe('menu-principal')
  })

  it('límite (F1): con el permiso de sistemas pero sin el rol, Sistemas no se dibuja; con PLATAFORMA lleva a /sistemas/servicios', async () => {
    const sinRol = await montar(['ACCESOS_ADMINISTRAR'], '/tablero', 'oficial')
    expect(sinRol.el.querySelector('a[data-tutorial-id="menu-sistemas"]')).toBeNull()
    sinRol.fixture.destroy()
    const conRol = await montar(['ACCESOS_ADMINISTRAR'], '/tablero', 'PLATAFORMA')
    expect(conRol.el.querySelector('a[data-tutorial-id="menu-sistemas"]')?.getAttribute('href')).toBe('/sistemas/servicios')
  })

  it('correcto: Sistemas fijo con todas sus pantallas a la vista y su ícono, incluidas las de los subgrupos y la metadata', async () => {
    const { el } = await montar(['ver:tablero', 'ver:sistemas', 'TABLERO_SISTEMAS_VER'], '/sistemas/outbox', 'PLATAFORMA')
    const sistemas = el.querySelector('ul[aria-label="Pantallas de Sistemas"]')!
    expect(textos(sistemas, 'a')).toEqual([
      'Servicios y SLO',
      'Despliegues',
      'Base de datos',
      'Respaldos',
      'Proveedores',
      'Outbox por servicio',
      'Webhooks',
      'Usuarios internos',
      'Roles internos',
      'Permisos internos',
      'Revisión de accesos',
      'Operaciones del sistema',
      'Mapa de flujos',
      'Registro del sistema',
    ])
    expect(textos(sistemas, '.subgrupo')).toEqual(['Integraciones', 'Personal y accesos', 'Endpoints y flujos'])
    expect(sistemas.querySelectorAll('a ap-icono').length).toBe(14)
    // La pantalla actual se marca con aria-current, no solo con color; la sección, con su barra.
    expect(el.querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Outbox por servicio')
    expect(el.querySelector('a.seccion.activo')?.textContent?.trim()).toBe('Sistemas')
    // Nada se pliega: ningún control de despliegue y los grupos son rótulos, no botones.
    expect(el.querySelector('[aria-expanded]')).toBeNull()
    expect(textos(el, 'h2.rotulo')).toContain('Plataforma')
  })

  it('correcto (2026-10-28): el rótulo «Plataforma» lleva su ícono y abre su primera sección', async () => {
    const { fixture, el } = await montar(['ver:sistemas'], '/tablero', 'PLATAFORMA')
    const rotulo = el.querySelector('h2.rotulo a') as HTMLAnchorElement
    expect(rotulo.textContent?.trim()).toBe('Plataforma')
    expect(rotulo.querySelector('ap-icono-de-seccion')).not.toBeNull()
    expect(rotulo.getAttribute('href')).toBe('/sistemas/servicios')
    const emitidos: unknown[] = []
    fixture.componentInstance.navegado.subscribe((v) => emitidos.push(v))
    rotulo.click()
    expect(emitidos).toHaveLength(1)
  })

  it('límite: en un subgrupo se marca la hoja exacta (la ruta más larga), no la primera del grupo', async () => {
    const { el } = await montar(['ver:sistemas'], '/sistemas/roles-internos', 'PLATAFORMA')
    expect(textos(el, 'a[aria-current="page"]')).toEqual(['Roles internos'])
  })

  it('límite (2026-10-29): un rótulo largo que se corta con «…» conserva el texto completo en title y su nombre accesible', async () => {
    const { el } = await montar(['ver:operacion', 'ver:postulaciones', 'ver:admisiones'], '/postulaciones-organizador')
    const largo = Array.from(el.querySelectorAll('a')).find((a) => a.textContent?.trim() === 'Postulaciones a organizador')!
    expect(largo.getAttribute('title')).toBe('Postulaciones a organizador')
    expect(largo.getAttribute('aria-label')).toBeNull()
    expect(largo.getAttribute('aria-current')).toBe('page')
    expect(el.querySelector('.subgrupo')?.getAttribute('title')).toBe('Grupos y admisiones')
    for (const a of Array.from(el.querySelectorAll('a'))) expect(a.getAttribute('title')).toBe(a.textContent?.trim())
  })

  it('accesibilidad: sin violaciones serias con el menú completo de sistemas', async () => {
    const { el } = await montar(['ver:sistemas', 'ver:configuracion', 'AUDITORIA_LEER', 'TABLERO_SISTEMAS_VER'], '/sistemas/servicios', 'PLATAFORMA')
    expect(await axe(el)).toHaveNoViolations()
  })
})
