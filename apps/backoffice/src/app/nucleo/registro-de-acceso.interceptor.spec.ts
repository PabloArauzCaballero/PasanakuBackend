import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { Avisos } from '@aportaya/ui/toast/toast'
import { GATEWAY } from './gateway'
import { ACCESO_A_DATOS, REGISTRO_DE_ACCESO_HABILITADO, registroDeAccesoInterceptor } from './registro-de-acceso.interceptor'

const GATEWAY_TEST = 'https://gateway.test/api/v1'

function configurar(habilitado: boolean) {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([registroDeAccesoInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GATEWAY_TEST },
      { provide: REGISTRO_DE_ACCESO_HABILITADO, useValue: habilitado },
    ],
  })
}

describe('registroDeAccesoInterceptor · bandera apagada (default)', () => {
  it('con la bandera apagada, no manda nada aunque la petición esté marcada', async () => {
    configurar(false)
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = firstValueFrom(http.get('/expedientes/1', { context: new HttpContext().set(ACCESO_A_DATOS, { recurso: 'expediente', id: '1' }) }))
    backend.expectOne('/expedientes/1').flush({ ok: true })
    await promesa

    backend.expectNone(`${GATEWAY_TEST}/extraccion/accesos`)
    backend.verify()
  })
})

describe('registroDeAccesoInterceptor · con la bandera prendida', () => {
  beforeEach(() => configurar(true))

  it('nivel correcto: el registro sale después del éxito, con recurso/id/correlación, sin query en la ruta', async () => {
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = firstValueFrom(
      http.get('/expedientes/42?tab=fotos', {
        context: new HttpContext().set(ACCESO_A_DATOS, { recurso: 'expediente', id: '42' }),
        headers: { 'x-request-id': 'corr-123' },
      }),
    )
    backend.expectOne('/expedientes/42?tab=fotos').flush({ ok: true })
    await promesa

    const registro = backend.expectOne(`${GATEWAY_TEST}/extraccion/accesos`)
    expect(registro.request.body).toEqual({ recurso: 'expediente', id: '42', rutaId: '/expedientes/42' })
    expect(registro.request.headers.get('x-request-id')).toBe('corr-123')
    registro.flush({})
    backend.verify()
  })

  it('nivel límite: una lectura sin marca no registra nada', async () => {
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)

    const promesa = firstValueFrom(http.get('/tablero'))
    backend.expectOne('/tablero').flush({ ok: true })
    await promesa

    backend.expectNone(`${GATEWAY_TEST}/extraccion/accesos`)
    backend.verify()
  })

  it('nivel límite: dos lecturas iguales seguidas registran dos veces', async () => {
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)
    const marca = new HttpContext().set(ACCESO_A_DATOS, { recurso: 'expediente', id: '7' })
    let registrosVistos = 0

    for (let i = 0; i < 2; i++) {
      const promesa = firstValueFrom(http.get('/expedientes/7', { context: marca }))
      backend.expectOne('/expedientes/7').flush({ ok: true })
      await promesa
      backend.expectOne(`${GATEWAY_TEST}/extraccion/accesos`).flush({})
      registrosVistos++
    }

    expect(registrosVistos).toBe(2)
    backend.verify()
  })

  it('nivel inválido: si el registro falla, se reporta a consola y se avisa en pantalla — la lectura ya se mostró', async () => {
    const http = TestBed.inject(HttpClient)
    const backend = TestBed.inject(HttpTestingController)
    const avisos = TestBed.inject(Avisos)
    const mostrar = vi.spyOn(avisos, 'mostrar')
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    const promesa = firstValueFrom(http.get('/expedientes/9', { context: new HttpContext().set(ACCESO_A_DATOS, { recurso: 'expediente', id: '9' }) }))
    backend.expectOne('/expedientes/9').flush({ ok: true })
    const resultado = await promesa
    expect(resultado).toEqual({ ok: true })

    const registro = backend.expectOne(`${GATEWAY_TEST}/extraccion/accesos`)
    registro.flush(null, { status: 404, statusText: 'Not Found' })

    expect(mostrar).toHaveBeenCalledTimes(1)
    expect(mostrar.mock.calls[0]?.[1]).toBe('error')
    expect(consoleError).toHaveBeenCalledTimes(1)
    consoleError.mockRestore()
    backend.verify()
  })
})
