import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { SalidaEndpointsDelDia } from 'clientes/angular/auditoria'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { filasDeEndpoints } from './dominio/endpoints-del-dia'
import { TablaEndpointsDelDia } from './tabla-endpoints-del-dia'

const URL = 'http://gw/api/v1/indicadores/operacion/endpoints'
const base = (extra: Partial<SalidaEndpointsDelDia>): SalidaEndpointsDelDia => ({
  desde: '2026-10-16T04:00:00Z',
  hasta: '2026-10-16T18:00:00Z',
  fuenteDisponible: true,
  endpoints: [],
  ...extra,
})

describe('filasDeEndpoints · la tabla recibe texto, con la ruta plantilla', () => {
  it('correcto: conteos con separador de miles, errores 5xx y 4xx y p95 en ms', () => {
    const [f] = filasDeEndpoints(
      base({ endpoints: [{ servicio: 'grupos', metodo: 'GET', ruta: '/grupos/{id}', solicitudes: 12345, errores4xx: 3, errores5xx: 1, p95Ms: '120.50' }] }),
    )
    expect(f).toMatchObject({ ruta: '/grupos/{id}', solicitudes: '12.345', errores: '1 5xx · 3 4xx', p95: '120,5 ms' })
  })

  it('límite: sin histograma lo dice; sin salida no hay filas', () => {
    const [f] = filasDeEndpoints(base({ endpoints: [{ servicio: 's', metodo: 'POST', ruta: '/r', solicitudes: 1, errores4xx: 0, errores5xx: 0 }] }))
    expect(f!.p95).toBe('Sin histograma')
    expect(filasDeEndpoints(undefined)).toEqual([])
  })
})

describe('TablaEndpointsDelDia · «no sabemos» no es «nadie llamó»', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function con(cuerpo: SalidaEndpointsDelDia) {
    const fixture = TestBed.createComponent(TablaEndpointsDelDia)
    fixture.detectChanges()
    TestBed.inject(ApplicationRef).tick()
    const req = http.expectOne((r) => r.url === URL)
    expect(req.request.params.get('limite')).toBe('50')
    req.flush(cuerpo)
    await fixture.whenStable()
    fixture.detectChanges()
    return (fixture.nativeElement as HTMLElement).textContent ?? ''
  }

  it('correcto: con tráfico muestra la ruta plantilla', async () => {
    const texto = await con(base({ endpoints: [{ servicio: 'grupos', metodo: 'GET', ruta: '/grupos/{id}', solicitudes: 3, errores4xx: 0, errores5xx: 0 }] }))
    expect(texto).toContain('/grupos/{id}')
  })

  it('límite: con la fuente sana y sin llamadas, dice que nadie llamó', async () => {
    expect(await con(base({}))).toContain('Todavía nadie llamó a la plataforma hoy')
  })

  it('inválido: sin fuente dice que no pudo leer el tráfico, NO que nadie llamó', async () => {
    const texto = await con(base({ fuenteDisponible: false }))
    expect(texto).toContain('No pudimos leer el tráfico')
    expect(texto).not.toContain('Todavía nadie llamó')
  })

  async function montar(responder: (r: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(TablaEndpointsDelDia)
    fixture.detectChanges()
    TestBed.inject(ApplicationRef).tick()
    responder(http.expectOne((r) => r.url === URL))
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture
  }
  const filas = (raiz: HTMLElement) => [...raiz.querySelectorAll('ap-tabla-de-datos tbody tr')].map((tr) => tr.textContent ?? '')

  it('correcto: abre ordenada por solicitudes (la más llamada primero), filtra por servicio y exporta lo filtrado', async () => {
    const fixture = await montar((r) =>
      r.flush(base({ endpoints: [
        { servicio: 'grupos', metodo: 'GET', ruta: '/grupos/{id}', solicitudes: 3, errores4xx: 0, errores5xx: 0 },
        { servicio: 'pagos', metodo: 'POST', ruta: '/pagos', solicitudes: 1200, errores4xx: 2, errores5xx: 0 },
      ] })),
    )
    const raiz = fixture.nativeElement as HTMLElement
    expect(filas(raiz)[0]).toContain('/pagos')
    const select = [...raiz.querySelectorAll('ap-lista-de-opciones')].find((l) => l.textContent?.includes('Servicio'))!.querySelector('select')!
    select.value = 'grupos'
    select.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    expect(filas(raiz)).toHaveLength(1)
    expect(raiz.querySelector('ap-exportar-listado')?.textContent).toContain('1 fila')
  })

  it('inválido: un 500 se dice con su código de seguimiento y «Volver a intentar» vuelve a pedir', async () => {
    const fixture = await montar((r) => r.flush({ mensaje: 'x', trazaId: 'tz-e' }, { status: 500, statusText: 'x' }))
    const raiz = fixture.nativeElement as HTMLElement
    expect(raiz.querySelector('[role="alert"]')).not.toBeNull()
    ;[...raiz.querySelectorAll('button')].find((b) => b.textContent?.includes('Volver a intentar'))!.click()
    TestBed.inject(ApplicationRef).tick()
    expect(http.match((r) => r.url === URL)).toHaveLength(1)
  })
})
