import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { SaludDeServicioEstadoEnum as E, type SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaServicios } from './pantalla-servicios'

const URL = 'http://gw/api/v1/indicadores/operacion/servicios'

// Sintético: la forma de `SalidaSaludDeServicios` (auditoria.yaml), no una medición real.
const DOS: SalidaSaludDeServicios = {
  consultadoEn: '2026-10-28T19:04:05Z',
  servicios: [
    { servicio: 'identidad', estado: E.Arriba, latenciaMs: 12 },
    { servicio: 'aportes', estado: E.Caido },
  ],
}

describe('PantallaServicios · contra consultarSaludDeServicios (HTTP)', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar(responder: (h: HttpTestingController) => void) {
    const fixture = TestBed.createComponent(PantallaServicios)
    fixture.detectChanges()
    responder(http)
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('cargando: antes de que conteste auditoria se ve el estado de carga', () => {
    const fixture = TestBed.createComponent(PantallaServicios)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Cargando')
    http.expectOne(URL)
  })

  it('correcto: pinta la salud que contestó cada servicio, con su latencia y la hora de la consulta', async () => {
    const el = await montar((h) => h.expectOne(URL).flush(DOS))
    const texto = el.textContent ?? ''
    expect(texto).toContain('identidad')
    expect(texto).toContain('arriba')
    expect(texto).toContain('12 ms')
    expect(texto).toContain('no responde')
    expect(texto).toContain('Consultado a las 15:04:05')
    expect(texto).not.toContain('99.95')
  })

  it('límite: si auditoria no trae ningún servicio, los catorce quedan «sin dato» (nunca «arriba»)', async () => {
    const el = await montar((h) => h.expectOne(URL).flush({ consultadoEn: '2026-10-28T19:04:05Z', servicios: [] }))
    const chips = Array.from(el.querySelectorAll('ap-chip-estado')).map((c) => c.textContent?.trim())
    expect(chips.length).toBeGreaterThan(0)
    expect(chips.every((c) => c === 'sin dato')).toBe(true)
  })

  it('inválido: con 500 muestra el error accionable con reintento, sin filas inventadas', async () => {
    const el = await montar((h) => h.expectOne(URL).flush({ codigo: 'AP-INT' }, { status: 500, statusText: 'Server Error' }))
    expect(el.querySelector('[role="alert"]')).toBeTruthy()
    expect(el.textContent).not.toContain('identidad')
  })

  it('inválido: con 403 (sin TABLERO_SISTEMAS_VER) tampoco pinta servicios', async () => {
    const el = await montar((h) => h.expectOne(URL).flush({ codigo: 'AP-AUT' }, { status: 403, statusText: 'Forbidden' }))
    expect(el.querySelectorAll('ap-chip-estado').length).toBe(0)
    expect(el.textContent).not.toContain('identidad')
  })
})
