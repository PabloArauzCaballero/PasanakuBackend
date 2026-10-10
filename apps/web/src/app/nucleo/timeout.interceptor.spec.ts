import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { erroresInterceptor } from './errores.interceptor'
import { PLAZO_DE_LECTURA_MS, timeoutInterceptor } from './timeout.interceptor'

describe('timeoutInterceptor (sitio público)', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor, timeoutInterceptor])), provideHttpClientTesting()] })
  })
  afterEach(() => vi.useRealTimers())

  it('una lectura sin respuesta vence a los 15 s con un error traducido y accionable, no queda cargando para siempre', async () => {
    const p = firstValueFrom(TestBed.inject(HttpClient).get('/x')).then(() => null, (e: unknown) => e)
    TestBed.inject(HttpTestingController).expectOne('/x')
    await vi.advanceTimersByTimeAsync(PLAZO_DE_LECTURA_MS + 1)
    expect(await p).toMatchObject({ estado: 0, tiempoAgotado: true, mensaje: 'La respuesta tardó demasiado. Probá de nuevo en un momento.' })
  })

  it('una respuesta a tiempo pasa intacta', async () => {
    const p = firstValueFrom(TestBed.inject(HttpClient).get('/x'))
    TestBed.inject(HttpTestingController).expectOne('/x').flush({ ok: 1 })
    await vi.advanceTimersByTimeAsync(PLAZO_DE_LECTURA_MS * 2)
    await expect(p).resolves.toEqual({ ok: 1 })
  })

  it('un 503 sigue saliendo traducido por el interceptor de errores, no como timeout', async () => {
    const p = firstValueFrom(TestBed.inject(HttpClient).get('/x')).then(() => null, (e: unknown) => e)
    TestBed.inject(HttpTestingController).expectOne('/x').flush({}, { status: 503, statusText: 'x' })
    expect(await p).toMatchObject({ estado: 503 })
  })
})
