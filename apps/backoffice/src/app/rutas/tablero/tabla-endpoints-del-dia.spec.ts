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
})
