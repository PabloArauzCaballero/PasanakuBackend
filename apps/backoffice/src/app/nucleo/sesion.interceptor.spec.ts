import { HttpClient, HttpContext, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, beforeEach } from 'vitest'
import { firstValueFrom, forkJoin, lastValueFrom } from 'rxjs'
import { Sesion } from './sesion'
import { sesionInterceptor, YA_REINTENTADA } from './sesion.interceptor'
import { GATEWAY } from './gateway'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

function configurar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([sesionInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GATEWAY_TEST },
    ],
  })
}

describe('sesionInterceptor · un 401 → un refresh → un reintento', () => {
  beforeEach(() => configurar())

  it('con un token vencido, un 401 dispara un refresh y el reintento trae la respuesta', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = firstValueFrom(http.get('/recurso'))

    const primera = backend.expectOne('/recurso')
    primera.flush(null, { status: 401, statusText: 'Unauthorized' })

    const refresco = backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`)
    refresco.flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })

    const reintento = backend.expectOne('/recurso')
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer nuevo')
    reintento.flush({ ok: true })

    await expect(promesa).resolves.toEqual({ ok: true })
    backend.verify()
  })
})

describe('sesionInterceptor · diez 401 concurrentes → exactamente un refresh', () => {
  beforeEach(() => configurar())

  it('diez peticiones con token vencido comparten un único refresh', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const N = 10
    const promesa = firstValueFrom(forkJoin(Array.from({ length: N }, (_, i) => http.get(`/recurso-${i}`))))

    for (let i = 0; i < N; i++) {
      backend.expectOne(`/recurso-${i}`).flush(null, { status: 401, statusText: 'Unauthorized' })
    }

    const refrescosEnVuelo = backend.match(`${GATEWAY_TEST}/sesion/refrescar`)
    expect(refrescosEnVuelo.length, `se esperaba 1 refresh, salieron ${refrescosEnVuelo.length}`).toBe(1)
    const unicoRefresco = refrescosEnVuelo.at(0)
    if (!unicoRefresco) throw new Error('no salió ningún refresh')
    unicoRefresco.flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })

    for (let i = 0; i < N; i++) {
      backend.expectOne(`/recurso-${i}`).flush({ i })
    }

    await promesa
    backend.verify()
  })
})

describe('sesionInterceptor · caso límite: refresh en vuelo no dispara otro', () => {
  beforeEach(() => configurar())

  it('cinco peticiones que llegan mientras el refresco está en vuelo comparten el mismo resultado', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const primeraTanda = firstValueFrom(http.get('/recurso-0'))
    backend.expectOne('/recurso-0').flush(null, { status: 401, statusText: 'Unauthorized' })
    // El refresco ya está en vuelo (todavía no respondió) cuando llegan cinco más.
    const segundaTanda = firstValueFrom(forkJoin(Array.from({ length: 5 }, (_, i) => http.get(`/recurso-tardio-${i}`))))
    for (let i = 0; i < 5; i++) {
      backend.expectOne(`/recurso-tardio-${i}`).flush(null, { status: 401, statusText: 'Unauthorized' })
    }

    const refrescos = backend.match(`${GATEWAY_TEST}/sesion/refrescar`)
    expect(refrescos.length, `se esperaba 1 refresh, salieron ${refrescos.length}`).toBe(1)
    refrescos.at(0)?.flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })

    backend.expectOne('/recurso-0').flush({ ok: 0 })
    for (let i = 0; i < 5; i++) {
      backend.expectOne(`/recurso-tardio-${i}`).flush({ ok: i })
    }

    await primeraTanda
    await segundaTanda
    backend.verify()
  })
})

describe('sesionInterceptor · caso error: el refresco falla', () => {
  beforeEach(() => configurar())

  it('cierra la sesión una sola vez y las N peticiones fallan con su error original', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const N = 4
    const promesas = Array.from({ length: N }, (_, i) => firstValueFrom(http.get(`/recurso-${i}`)).catch((e: unknown) => e))
    for (let i = 0; i < N; i++) {
      backend.expectOne(`/recurso-${i}`).flush(null, { status: 401, statusText: 'Unauthorized' })
    }

    const refrescos = backend.match(`${GATEWAY_TEST}/sesion/refrescar`)
    expect(refrescos.length).toBe(1)
    refrescos.at(0)?.flush(null, { status: 500, statusText: 'Error interno' })

    const resultados = await Promise.all(promesas)
    for (const r of resultados) {
      expect(r).toBeInstanceOf(HttpErrorResponse)
      expect((r as HttpErrorResponse).status).toBe(401)
    }
    expect(sesion.abierta()).toBe(false)
    backend.verify()
  })
})

describe('sesionInterceptor · caso rotación: el reintento usa el token nuevo', () => {
  beforeEach(() => configurar())

  it('el header del reintento es exactamente el acceso devuelto por el refresco', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = firstValueFrom(http.get('/recurso'))
    backend.expectOne('/recurso').flush(null, { status: 401, statusText: 'Unauthorized' })
    backend.expectOne(`${GATEWAY_TEST}/sesion/refrescar`).flush({ acceso: 'rotado-xyz', permisos: [], rol: 'oficial' })
    const reintento = backend.expectOne('/recurso')
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer rotado-xyz')
    reintento.flush({ ok: true })
    await promesa
    backend.verify()
  })
})

describe('sesionInterceptor · protección de bucle: 401 tras reintento no refresca de nuevo', () => {
  beforeEach(() => configurar())

  it('una petición marcada como ya reintentada que recibe 401 propaga el error sin pedir otro refresh', async () => {
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = lastValueFrom(http.get('/recurso', { context: new HttpContext().set(YA_REINTENTADA, true) })).catch((e: unknown) => e)
    const peticion = backend.expectOne('/recurso')
    peticion.flush(null, { status: 401, statusText: 'Unauthorized' })

    const resultado = await promesa
    expect(resultado).toBeInstanceOf(HttpErrorResponse)
    backend.expectNone(`${GATEWAY_TEST}/sesion/refrescar`)
    backend.verify()
  })
})
