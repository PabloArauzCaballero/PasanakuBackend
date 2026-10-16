import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import type { PartidaPresupuestaria } from '../dominio/cu101-presupuestos'
import { PantallaDePresupuesto } from './pantalla-de-presupuesto'

/** H1.S3.M2 · CU-101 · la pantalla sobre el listado por cursor real. Datos sintéticos con la forma de `PartidaEnLista`. */
const URL = 'http://gw/api/v1/erp/partidas-presupuestarias?limite=50'

function partida(parcial: Partial<PartidaPresupuestaria> = {}): PartidaPresupuestaria {
  return {
    partidaId: 'pp-1',
    presupuestoId: 'p-1',
    presupuestoNombre: 'Operación 2026',
    estado: 'APROBADO',
    centroCostoId: 'cc-1',
    centroCostoNombre: 'Tecnología',
    cuentaContableId: 'cta-1',
    periodoContableId: 'per-3',
    anio: 2026,
    mes: 3,
    fechaInicioPeriodo: '2026-03-01',
    montoPresupuestado: { monto: '1000.00', moneda: 'BOB' },
    montoEjecutado: { monto: '1250.75', moneda: 'BOB' },
    ...parcial,
  }
}

async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
}

describe('PantallaDePresupuesto · CU-101 por cursor', () => {
  let http: HttpTestingController

  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_PRESUPUESTO'], 'contabilidad')
  })

  async function montar(responder: (url: string) => void) {
    const fixture = TestBed.createComponent(PantallaDePresupuesto)
    fixture.detectChanges()
    await esperar(fixture)
    responder(URL)
    await esperar(fixture)
    await esperar(fixture)
    return fixture
  }

  it('correcto · nombra el período en español, no la cuenta contable (que es de nucleo-financiero), y resalta la desviación', async () => {
    const fixture = await montar((url) => http.expectOne(url).flush({ items: [partida()], proximoAntesDe: null, proximoAntesDeId: null }))
    const texto = fixture.nativeElement.textContent as string
    expect(texto).toContain('marzo 2026')
    expect(texto).toContain('Operación 2026')
    expect(texto).not.toContain('cta-1')
    expect(fixture.nativeElement.querySelector('.desviacion.sobre')).not.toBeNull()
    expect(fixture.nativeElement.querySelector('ap-estado-de-listado')?.textContent?.trim()).toBe('')
  })

  it('no ofrece ordenar por columna: el orden lo fija el servidor y ordenar una página mentiría sobre el resto', async () => {
    const fixture = await montar((url) => http.expectOne(url).flush({ items: [partida()] }))
    expect(fixture.nativeElement.querySelectorAll('[role="columnheader"] button')).toHaveLength(0)
  })

  it('límite · sin partidas lo dice', async () => {
    const fixture = await montar((url) => http.expectOne(url).flush({ items: [], proximoAntesDe: null, proximoAntesDeId: null }))
    expect(fixture.nativeElement.textContent).toContain('No hay partidas presupuestarias')
  })

  it('inválido · 403: alerta con el mensaje traducido, sin el cuerpo del backend', async () => {
    const fixture = await montar((url) => http.expectOne(url).flush({ mensaje: 'detalle interno', trazaId: 'tz-9' }, { status: 403, statusText: 'Forbidden' }))
    const alerta = fixture.nativeElement.querySelector('ap-estado-de-listado [role="alert"]') as HTMLElement
    expect(alerta.textContent).toContain('No tenés acceso a esto.')
    expect(alerta.textContent).toContain('tz-9')
    expect(fixture.nativeElement.textContent).not.toContain('detalle interno')
  })

  it('inválido · 422 AP-CU101-04: filtro inválido, con el texto del dominio', async () => {
    const fixture = await montar((url) =>
      http.expectOne(url).flush({ codigo: 'AP-CU101-04', mensaje: 'interno', trazaId: 'tz-4' }, { status: 422, statusText: 'Unprocessable Entity' }),
    )
    expect(fixture.nativeElement.querySelector('ap-estado-de-listado [role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })
})
