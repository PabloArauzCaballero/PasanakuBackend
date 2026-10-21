import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { describe, expect, it } from 'vitest'
import { PestanasDeSeccion } from './pestanas-de-seccion'

@Component({ imports: [PestanasDeSeccion], template: '<ap-pestanas-de-seccion />' })
class Pagina {}

/** Las pantallas de la sección viven en pestañas arriba del contenido, no en el menú lateral. */
describe('PestanasDeSeccion', () => {
  async function abrir(url: string) {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([{ path: '**', component: Pagina }])] })
    const arnes = await RouterTestingHarness.create(url)
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return arnes.routeNativeElement as HTMLElement
  }

  it('correcto: en una pantalla de Sistemas lista sus pantallas y marca la actual con aria-current', async () => {
    const el = await abrir('/sistemas/outbox')
    expect(el.querySelector('nav')?.getAttribute('aria-label')).toBe('Pantallas de Sistemas')
    expect(el.querySelectorAll('a').length).toBeGreaterThan(5)
    const actual = el.querySelector('a[aria-current="page"]')
    expect(actual?.textContent?.trim()).toBe('Outbox y descartados')
    expect(actual?.getAttribute('href')).toBe('/sistemas/outbox')
  })

  it('límite: la pantalla raíz de Contabilidad (ruta vacía) es la activa en /contabilidad', async () => {
    const el = await abrir('/contabilidad')
    expect(el.querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Períodos contables')
  })

  it('inválido: una sección sin pantallas (Catálogo de datos) no muestra pestañas', async () => {
    const el = await abrir('/tablero/catalogo-de-datos')
    expect(el.querySelector('nav')).toBeNull()
  })
})
