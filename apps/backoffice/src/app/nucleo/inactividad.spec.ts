import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from './gateway'
import { VigilanteDeInactividad } from './inactividad'
import { Sesion } from './sesion'

const GATEWAY_TEST = 'https://gateway.test/api/v1'
const MINUTO = 60_000

describe('VigilanteDeInactividad (ADR-038 §1, P-8)', () => {
  let vigilante: VigilanteDeInactividad
  let sesion: Sesion
  let http: HttpTestingController

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'setTimeout', 'clearTimeout', 'Date'] })
    vi.setSystemTime(new Date('2026-11-24T15:00:00Z'))
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GATEWAY_TEST }],
    })
    sesion = TestBed.inject(Sesion)
    sesion.abrir('token-1', ['TABLERO_VER'], 'ADMIN_PLATAFORMA', 'sujeto')
    vigilante = TestBed.inject(VigilanteDeInactividad)
    http = TestBed.inject(HttpTestingController)
    TestBed.tick()
    vigilante.iniciar()
  })

  afterEach(() => {
    vigilante.detener()
    vi.useRealTimers()
  })

  it('correcto: sin actividad avisa a los 13 minutos y cierra a los 15', () => {
    vi.advanceTimersByTime(12 * MINUTO + 59_000)
    expect(vigilante.estado()).toBe('ACTIVA')

    vi.advanceTimersByTime(1_000)
    expect(vigilante.estado()).toBe('AVISO')
    expect(vigilante.cierraEn()).toBe(new Date('2026-11-24T15:15:00Z').getTime())

    vi.advanceTimersByTime(2 * MINUTO)
    expect(vigilante.estado()).toBe('CERRADA')
  })

  it('correcto: la actividad durante el aviso lo cierra y reinicia la cuenta', () => {
    vi.advanceTimersByTime(14 * MINUTO)
    expect(vigilante.estado()).toBe('AVISO')

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'a' }))
    expect(vigilante.estado()).toBe('ACTIVA')

    vi.advanceTimersByTime(12 * MINUTO)
    expect(vigilante.estado()).toBe('ACTIVA')
  })

  it('latido: con actividad, a los 5 minutos renueva la sesión una sola vez y adopta el token nuevo', () => {
    vi.advanceTimersByTime(5 * MINUTO)
    document.dispatchEvent(new Event('pointerdown'))
    document.dispatchEvent(new Event('pointerdown'))

    const pedido = http.expectOne(`${GATEWAY_TEST}/sesion/refrescar`)
    expect(pedido.request.method).toBe('POST')
    pedido.flush({ acceso: 'token-2' })
    expect(sesion.token()).toBe('token-2')
    http.verify()
  })

  it('limite: sin actividad no hay latido (el servidor ve la ausencia)', () => {
    vi.advanceTimersByTime(14 * MINUTO)
    http.expectNone(`${GATEWAY_TEST}/sesion/refrescar`)
  })

  it('limite: un equipo suspendido que despierta con la sesión vencida se cierra en la primera revisión', () => {
    vi.setSystemTime(new Date('2026-11-24T15:20:00Z'))
    vigilante.revisar()
    expect(vigilante.estado()).toBe('CERRADA')

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'a' }))
    expect(vigilante.estado()).toBe('CERRADA')
  })

  it('limite: sin sesión abierta no avisa ni cierra nada', () => {
    sesion.cerrar()
    vi.advanceTimersByTime(20 * MINUTO)
    expect(vigilante.estado()).toBe('ACTIVA')
  })

  it('invalido: un latido que falla no cierra la sesión por sí mismo', () => {
    vi.advanceTimersByTime(5 * MINUTO)
    document.dispatchEvent(new Event('wheel'))
    http.expectOne(`${GATEWAY_TEST}/sesion/refrescar`).flush(null, { status: 503, statusText: 'caído' })

    expect(sesion.abierta()).toBe(true)
    expect(vigilante.estado()).toBe('ACTIVA')
  })
})
