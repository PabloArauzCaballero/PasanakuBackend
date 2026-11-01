import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { EndpointDeLaOperacionMetodoEnum, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { OBLIGACION, tabla } from './catalogo-sintetico'
import { VistaOperaciones } from './vista-operaciones'

/** La pestaña «Operaciones»: las directas del catálogo de operaciones y los casos de uso que nombran la tabla. */
describe('VistaOperaciones', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
  })

  async function montar(t: TablaDelCatalogo) {
    const fixture = TestBed.createComponent(VistaOperaciones)
    fixture.componentRef.setInput('tabla', t)
    fixture.detectChanges()
    await fixture.whenStable()
    return fixture.nativeElement as HTMLElement
  }

  it('lista los casos de uso que nombran la tabla con sus endpoints, en el orden que llegan', async () => {
    const el = await montar(
      tabla('pago', 'aportes', {
        operaciones: [
          {
            casoDeUso: 'CU-21',
            titulo: 'Cobrar el aporte del período',
            endpoints: [{ servicio: 'aportes', metodo: EndpointDeLaOperacionMetodoEnum.Post, ruta: '/aportes/obligaciones/{obligacionId}/pagos', operationId: 'cobrarAporte' }],
          },
          { casoDeUso: 'CU-24', titulo: 'Reembolsar un pago', endpoints: [] },
        ],
      }),
    )
    const lista = el.querySelector('ul[aria-label="Casos de uso que la nombran"]')!
    const casos = Array.from(lista.querySelectorAll(':scope > li h3')).map((h) => h.textContent?.replace(/\s+/g, ' ').trim())
    expect(casos).toEqual(['CU-21 Cobrar el aporte del período', 'CU-24 Reembolsar un pago'])
    const endpoints = lista.querySelector('ul[aria-label="Endpoints de CU-21"]')!
    expect(endpoints.textContent).toContain('POST')
    expect(endpoints.textContent).toContain('/aportes/obligaciones/{obligacionId}/pagos')
    expect(endpoints.textContent).toContain('cobrarAporte · aportes')
    // El caso sin endpoint lo dice, no deja un hueco.
    expect(lista.textContent).toContain('Sin endpoint propio todavía')
  })

  it('la ruta se copia entera (los cortes son <wbr>, no texto) y el endpoint conocido enlaza a su ficha', async () => {
    const el = await montar(
      tabla('pago', 'aportes', {
        operaciones: [
          {
            casoDeUso: 'CU-21',
            titulo: 'Cobrar el aporte del período',
            endpoints: [
              { servicio: 'aportes', metodo: EndpointDeLaOperacionMetodoEnum.Post, ruta: '/aportes/obligaciones/{obligacionId}/pagos', operationId: 'cobrarAporte' },
              { servicio: 'aportes', metodo: EndpointDeLaOperacionMetodoEnum.Get, ruta: '/no/esta/en/el/catalogo', operationId: 'operacionQueNoConoceElCatalogo' },
            ],
          },
        ],
      }),
    )
    const rutas = Array.from(el.querySelectorAll('code.ruta')).map((c) => c.textContent)
    expect(rutas).toEqual(['/aportes/obligaciones/{obligacionId}/pagos', '/no/esta/en/el/catalogo'])
    expect(el.querySelectorAll('code.ruta wbr').length).toBeGreaterThan(0)
    const enlaces = Array.from(el.querySelectorAll('.detalle a')).map((a) => a.textContent?.trim())
    expect(enlaces).toEqual(['cobrarAporte'])
    expect(el.querySelector('.detalle a')?.getAttribute('href')).toMatch(/^\/sistemas\/operaciones\/.+/)
    expect(el.querySelectorAll('.detalle').length).toBe(2)
  })

  it('la sección de directas tiene su título aunque esté vacía, y el vacío dice por qué y qué hacer', async () => {
    const el = await montar(tabla('tabla_inexistente_en_operaciones', 'aportes'))
    const directas = el.querySelector('section[aria-label="Operaciones que usan la tabla"]')!
    expect(directas.querySelector('h2, h3')?.textContent).toContain('Operaciones que usan la tabla')
    expect(directas.textContent).toContain('inventario parcial')
    expect(directas.textContent).toContain('avisale a quien mantiene el catálogo')
  })

  it('sin casos de uso muestra el vacío que orienta (infraestructura o tabla sin enlazar)', async () => {
    const el = await montar(tabla('evento_dominio', 'aportes', { tipo: 'INFRAESTRUCTURA' as TablaDelCatalogo['tipo'], operaciones: [] }))
    expect(el.querySelector('ul[aria-label="Casos de uso que la nombran"]')).toBeNull()
    expect(el.textContent).toContain('Ningún caso de uso la nombra')
    expect(el.textContent).toContain('falta enlazarla en el caso de uso que la toca')
  })

  it('una ficha vieja sin el campo no rompe la pestaña', async () => {
    const sinCampo = { ...OBLIGACION } as Partial<TablaDelCatalogo>
    delete sinCampo.operaciones
    const el = await montar(sinCampo as TablaDelCatalogo)
    expect(el.textContent).toContain('Ningún caso de uso la nombra')
  })

  it('conserva la sección de operaciones directas (vacía cuando el catálogo de operaciones no la nombra)', async () => {
    const el = await montar(tabla('tabla_inexistente_en_operaciones', 'aportes'))
    const directas = el.querySelector('section[aria-label="Operaciones que usan la tabla"]')!
    expect(directas.textContent).toContain('Sin operaciones directas registradas')
  })
})
