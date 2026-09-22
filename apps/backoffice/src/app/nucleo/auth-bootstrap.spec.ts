import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { GATEWAY } from './gateway'
import { RefrescoDeSesion } from './refresco-de-sesion'
import { Sesion } from './sesion'
import { inicializarSesion, TIMEOUT_ARRANQUE_MS } from './auth-bootstrap'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GATEWAY_TEST },
    ],
  })
}

describe('inicializarSesion · AuthBootstrap', () => {
  beforeEach(() => configurar())

  it('cookie válida (200) → AUTHENTICATED', async () => {
    const sesion = TestBed.inject(Sesion)
    const backend = TestBed.inject(HttpTestingController)

    const listo = TestBed.runInInjectionContext(() => inicializarSesion()())
    expect(sesion.estado()).toBe('RESTORING')

    backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`).flush({ acceso: 'nuevo', permisos: ['x'], rol: 'oficial' })
    await listo

    expect(sesion.estado()).toBe('AUTHENTICATED')
    expect(sesion.abierta()).toBe(true)
    backend.verify()
  })

  it('cookie inválida (401) → ANONYMOUS, no ERROR', async () => {
    const sesion = TestBed.inject(Sesion)
    const backend = TestBed.inject(HttpTestingController)

    const listo = TestBed.runInInjectionContext(() => inicializarSesion()())
    backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`).flush(null, { status: 401, statusText: 'Unauthorized' })
    await listo

    expect(sesion.estado()).toBe('ANONYMOUS')
    backend.verify()
  })

  it('identidad caído (503) → ERROR, sin redirect implícito', async () => {
    const sesion = TestBed.inject(Sesion)
    const backend = TestBed.inject(HttpTestingController)

    const listo = TestBed.runInInjectionContext(() => inicializarSesion()())
    backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`).flush(null, { status: 503, statusText: 'Service Unavailable' })
    await listo

    expect(sesion.estado()).toBe('ERROR')
    backend.verify()
  })

  it('timeout a los 5000ms exactos → ERROR', async () => {
    vi.useFakeTimers()
    try {
      const sesion = TestBed.inject(Sesion)
      const backend = TestBed.inject(HttpTestingController)

      let resuelto = false
      const listo = TestBed.runInInjectionContext(() => inicializarSesion()()).then(() => {
        resuelto = true
      })
      // La petición queda sin flushear a propósito: lo que se mide es el timeout del cliente.
      backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`)

      await vi.advanceTimersByTimeAsync(TIMEOUT_ARRANQUE_MS - 1)
      expect(resuelto).toBe(false)
      expect(sesion.estado()).toBe('RESTORING')

      await vi.advanceTimersByTimeAsync(1)
      await listo
      expect(resuelto).toBe(true)
      expect(sesion.estado()).toBe('ERROR')
    } finally {
      vi.useRealTimers()
    }
  })

  it('concurrencia: un segundo llamador durante el arranque comparte el mismo refresco (cero POST adicionales)', async () => {
    const sesion = TestBed.inject(Sesion)
    const refresco = TestBed.inject(RefrescoDeSesion)
    const backend = TestBed.inject(HttpTestingController)

    const listo = TestBed.runInInjectionContext(() => inicializarSesion()())
    // "Otra petición" que necesita el mismo refresco mientras el de arranque sigue en vuelo.
    const otroLlamador = TestBed.runInInjectionContext(() => refresco.refrescar())
    let segundoResultado: unknown
    otroLlamador.subscribe({ next: (v) => (segundoResultado = v), error: (e) => (segundoResultado = e) })

    const pendientes = backend.match(`${GATEWAY_TEST}/sesion/refrescar`)
    expect(pendientes.length, `se esperaba 1 POST de refresco, salieron ${pendientes.length}`).toBe(1)
    pendientes[0]?.flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })

    await listo
    expect(sesion.estado()).toBe('AUTHENTICATED')
    expect(segundoResultado).toEqual({ acceso: 'nuevo', permisos: [], rol: 'oficial' })
    backend.verify()
  })
})
