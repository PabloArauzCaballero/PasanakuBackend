import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { SalidaOutboxPorServicio } from 'clientes/angular/auditoria'
import { axe } from 'vitest-axe'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { espera, filasDeOutbox } from '../dominio/outbox'
import { PantallaOutbox } from './pantalla-outbox'

const URL = 'http://gw/api/v1/indicadores/operacion/outbox'

// Sintético: la forma de `SalidaOutboxPorServicio` (auditoria.yaml).
const SALIDA: SalidaOutboxPorServicio = {
  consultadoEn: '2026-10-29T19:04:05Z',
  fuenteDisponible: true,
  servicios: [
    { servicio: 'aportes', pendientes: 3, fallidos: 1, edadMasViejoSegundos: 42, publicados24h: 121, erroresDePublicacion24h: 2 },
    { servicio: 'entregas', pendientes: 7, fallidos: 0, edadMasViejoSegundos: 900 },
    { servicio: 'erp', pendientes: 0, fallidos: 0, edadMasViejoSegundos: 0 },
    { servicio: 'publicidad' },
  ],
}

describe('filasDeOutbox · estado de cada servicio', () => {
  it('correcto y límite: fallidos gana, después la espera de más de 5 min; sin dato no es «al día»', () => {
    expect(filasDeOutbox(SALIDA).map((f) => [f.servicio, f.estado, f.tono])).toEqual([
      ['aportes', 'Con fallidos', 'error'],
      ['entregas', 'Atrasado', 'aviso'],
      ['erp', 'Al día', 'ok'],
      ['publicidad', 'Sin dato', 'neutro'],
    ])
    expect(filasDeOutbox(SALIDA)[3]).toMatchObject({ pendientes: '—', publicados24h: '—' })
    expect([espera(42), espera(900), espera(7500), espera(undefined)]).toEqual(['42 s', '15 min', '2 h 5 min', '—'])
  })
})

describe('PantallaOutbox · contra consultarOutboxPorServicio (HTTP)', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar(responder: (h: HttpTestingController) => void) {
    const fixture = TestBed.createComponent(PantallaOutbox)
    fixture.detectChanges()
    responder(http)
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('correcto: un servicio por fila con sus números y la hora de la consulta; sin acciones de editar o borrar', async () => {
    const el = await montar((h) => h.expectOne(URL).flush(SALIDA))
    const texto = el.textContent ?? ''
    expect(texto).toContain('aportes')
    expect(texto).toContain('Con fallidos')
    expect(texto).toContain('15 min')
    expect(texto).toContain('Consultado a las 15:04:05')
    expect(Array.from(el.querySelectorAll('button')).some((b) => /Eliminar|Editar/.test(b.textContent ?? ''))).toBe(false)
  })

  it('límite: Prometheus no respondió → «sin fuente», nunca una lista vacía que diga que no hay pendientes', async () => {
    const el = await montar((h) => h.expectOne(URL).flush({ consultadoEn: '2026-10-29T19:04:05Z', fuenteDisponible: false, servicios: [] }))
    expect(el.textContent).toContain('Prometheus no respondió')
    expect(el.textContent).not.toContain('Ningún servicio publicó')
  })

  it('inválido: 500 o 403 → error, sin filas inventadas', async () => {
    const el = await montar((h) => h.expectOne(URL).flush({}, { status: 500, statusText: 'x' }))
    expect(el.querySelector('[role="alert"]')).toBeTruthy()
    expect(el.textContent).not.toContain('aportes')
  })

  it('accesibilidad: sin violaciones serias con datos', async () => {
    const el = await montar((h) => h.expectOne(URL).flush(SALIDA))
    expect(await axe(el)).toHaveNoViolations()
  })
})
