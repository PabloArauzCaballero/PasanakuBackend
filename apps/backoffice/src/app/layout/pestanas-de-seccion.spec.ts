import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../nucleo/sesion'
import { PestanasDeSeccion } from './pestanas-de-seccion'

@Component({ imports: [PestanasDeSeccion], template: '<ap-pestanas-de-seccion />' })
class Pagina {}

/** Las pantallas de la sección viven en pestañas arriba del contenido, con ícono, y solo las que la sesión abre. */
describe('PestanasDeSeccion', () => {
  async function abrir(url: string, permisos: string[] = ['ver:sistemas', 'ver:contabilidad', 'TABLERO_SISTEMAS_VER'], rol = 'PLATAFORMA') {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Pagina }])] })
    TestBed.inject(Sesion).abrir('token', permisos, rol)
    const arnes = await RouterTestingHarness.create(url)
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement as HTMLElement
  }
  const textos = (nav: Element | null | undefined) => Array.from(nav?.querySelectorAll('a') ?? []).map((a) => a.textContent?.trim())

  it('correcto: en Sistemas, siete pestañas con ícono; la de grupo despliega su segunda fila y marca la actual', async () => {
    const el = await abrir('/sistemas/outbox')
    const [principal, sub] = Array.from(el.querySelectorAll('nav'))
    expect(principal?.getAttribute('aria-label')).toBe('Pantallas de Sistemas')
    expect(textos(principal)).toEqual(['Servicios y SLO', 'Despliegues e interruptores', 'Base y migraciones', 'Respaldos', 'Integraciones', 'Personal y accesos', 'Registro del sistema'])
    expect(principal?.querySelectorAll('a ap-icono').length).toBe(7)
    expect(principal?.querySelector('a.activa')?.textContent?.trim()).toBe('Integraciones')
    expect(sub?.getAttribute('aria-label')).toBe('Pantallas de Integraciones')
    expect(textos(sub)).toEqual(['Proveedores', 'Outbox y descartados', 'Webhooks'])
    const actual = el.querySelector('a[aria-current="page"]')
    expect(actual?.textContent?.trim()).toBe('Outbox y descartados')
    expect(actual?.getAttribute('href')).toBe('/sistemas/outbox')
  })

  it('límite: la pantalla raíz de Contabilidad (ruta /contabilidad) es la activa en /contabilidad y en un período elegido', async () => {
    expect((await abrir('/contabilidad')).querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Períodos contables')
  })

  it('límite: una pestaña de otra rama de URL (Catálogo de datos en Configuración) se marca en su ficha', async () => {
    const el = await abrir('/tablero/catalogo-de-datos/comun/pago')
    expect(el.querySelector('nav')?.getAttribute('aria-label')).toBe('Pantallas de Configuración')
    expect(el.querySelector('a.activa')?.textContent?.trim()).toBe('Catálogo de datos')
  })

  it('inválido: sin el permiso, la pestaña no se dibuja (no hay pestaña que rebote); el inicio no tiene pestañas', async () => {
    const el = await abrir('/contabilidad', ['ver:contabilidad'])
    expect(textos(el.querySelector('nav'))).toEqual(['Períodos contables'])
    TestBed.resetTestingModule()
    expect((await abrir('/tablero')).querySelector('nav')).toBeNull()
  })
})
