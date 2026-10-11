import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { contextoIdempotente, olvidarIntentos, VIGENCIA_DE_CLAVE_MS } from './idempotencia.interceptor'
import { cobrar, COBRO, fallo, GW, montar } from './cadena-de-red.apoyo'
import { Sesion } from './sesion'

/**
 * H12.S1.M3 · la cadena de red REAL (`INTERCEPTORES_DE_RED`, la misma que usa la aplicación), no cada interceptor suelto:
 * lo que importa es cómo se combinan. Todo con `HttpTestingController` (sin servidor): prueba el cliente, no el gateway.
 */
const corte = () => new ProgressEvent('error')

describe('cadena de red · sesión', () => {
  beforeEach(() => olvidarIntentos())

  it('un 401 en la aplicación real dispara UN refresco y el reintento trae la respuesta (el error no se traduce antes de tiempo)', async () => {
    const { http, backend } = montar()
    TestBed.inject(Sesion).abrir('viejo', [], 'oficial')
    const promesa = firstValueFrom(http.get(`${GW}/recurso`))
    backend.expectOne(`${GW}/recurso`).flush({ codigo: 'AP-X', mensaje: 'crudo' }, { status: 401, statusText: 'Unauthorized' })
    backend.expectOne(`${GW}/sesion/refrescar`).flush({ acceso: 'nuevo', permisos: [], rol: 'oficial' })
    const reintento = backend.expectOne(`${GW}/recurso`)
    expect(reintento.request.headers.get('Authorization')).toBe('Bearer nuevo')
    reintento.flush({ ok: true })
    await expect(promesa).resolves.toEqual({ ok: true })
  })

  it('si el refresco también falla, la pantalla ve «Tu sesión venció» ya traducido, con la traza de la petición, y la sesión se cierra', async () => {
    const { http, backend } = montar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('viejo', [], 'oficial')
    const promesa = fallo(firstValueFrom(http.get(`${GW}/recurso`)))
    const primera = backend.expectOne(`${GW}/recurso`)
    primera.flush({}, { status: 401, statusText: 'Unauthorized' })
    backend.expectOne(`${GW}/sesion/refrescar`).flush({}, { status: 401, statusText: 'Unauthorized' })
    const error = await promesa
    expect(error?.mensaje).toBe('Tu sesión venció. Volvé a ingresar.')
    expect(error?.trazaId).toBe(primera.request.headers.get('x-request-id'))
    expect(sesion.abierta()).toBe(false)
  })
})

describe('cadena de red · idempotencia por contenido (éxito no confirmado)', () => {
  beforeEach(() => olvidarIntentos())

  it('tras un corte de red el reintento del MISMO cobro sale con la MISMA clave; tras el éxito, un cobro igual es otra operación', async () => {
    const { http, backend } = montar()
    const p1 = fallo(firstValueFrom(cobrar(http)))
    const r1 = backend.expectOne(COBRO)
    const clave = r1.request.headers.get('Idempotency-Key')
    expect(clave).toMatch(/^[0-9a-f-]{36}$/)
    r1.error(corte(), { status: 0 })
    expect((await p1)?.sinConexion).toBe(true)

    const p2 = firstValueFrom(cobrar(http))
    const r2 = backend.expectOne(COBRO)
    expect(r2.request.headers.get('Idempotency-Key'), 'el reintento reutiliza la clave').toBe(clave)
    r2.flush({ ok: true })
    await p2

    const p3 = firstValueFrom(cobrar(http))
    const r3 = backend.expectOne(COBRO)
    expect(r3.request.headers.get('Idempotency-Key'), 'confirmada la anterior, es otra operación').not.toBe(clave)
    r3.flush({ ok: true })
    await p3
  })

  it('un contenido distinto es otra operación: otra clave', async () => {
    const { http, backend } = montar()
    const pa = fallo(firstValueFrom(cobrar(http, '100.00')))
    const a = backend.expectOne(COBRO)
    const claveA = a.request.headers.get('Idempotency-Key')
    a.error(corte(), { status: 0 })
    await pa
    const pb = fallo(firstValueFrom(cobrar(http, '100.01')))
    const b = backend.expectOne(COBRO)
    expect(b.request.headers.get('Idempotency-Key')).not.toBe(claveA)
    b.error(corte(), { status: 0 })
    await pb
  })

  it('un 5xx tampoco confirma nada: la clave se conserva; un rechazo definitivo (422) la libera', async () => {
    const { http, backend } = montar()
    const p1 = fallo(firstValueFrom(cobrar(http)))
    const r1 = backend.expectOne(COBRO)
    const clave = r1.request.headers.get('Idempotency-Key')
    r1.flush({}, { status: 503, statusText: 'Service Unavailable' })
    await p1
    const p2 = fallo(firstValueFrom(cobrar(http)))
    const r2 = backend.expectOne(COBRO)
    expect(r2.request.headers.get('Idempotency-Key')).toBe(clave)
    r2.flush({ codigo: 'AP-CU13-01' }, { status: 422, statusText: 'Unprocessable Entity' })
    await p2
    const p3 = fallo(firstValueFrom(cobrar(http)))
    const r3 = backend.expectOne(COBRO)
    expect(r3.request.headers.get('Idempotency-Key'), 'rechazada de verdad, el próximo intento es nuevo').not.toBe(clave)
    r3.flush({}, { status: 503, statusText: 'x' })
    await p3
  })

  it('pasada la vigencia sin respuesta, repetir el contenido se toma como una operación nueva', async () => {
    const { http, backend } = montar()
    const ahora = Date.now()
    const reloj = vi.spyOn(Date, 'now')
    reloj.mockReturnValue(ahora)
    const p1 = fallo(firstValueFrom(cobrar(http)))
    const r1 = backend.expectOne(COBRO)
    const clave = r1.request.headers.get('Idempotency-Key')
    r1.error(corte(), { status: 0 })
    await p1
    reloj.mockReturnValue(ahora + VIGENCIA_DE_CLAVE_MS + 1)
    const p2 = fallo(firstValueFrom(cobrar(http)))
    const r2 = backend.expectOne(COBRO)
    expect(r2.request.headers.get('Idempotency-Key')).not.toBe(clave)
    r2.error(corte(), { status: 0 })
    await p2
    reloj.mockRestore()
  })

  it('el orden de las propiedades del cuerpo no cambia la clave: es el mismo contenido', async () => {
    const { http, backend } = montar()
    const p1 = fallo(firstValueFrom(http.post(COBRO, { a: 1, b: { x: '1', y: '2' } }, { context: contextoIdempotente() })))
    const r1 = backend.expectOne(COBRO)
    const clave = r1.request.headers.get('Idempotency-Key')
    r1.error(corte(), { status: 0 })
    await p1
    const p2 = fallo(firstValueFrom(http.post(COBRO, { b: { y: '2', x: '1' }, a: 1 }, { context: contextoIdempotente() })))
    const r2 = backend.expectOne(COBRO)
    expect(r2.request.headers.get('Idempotency-Key')).toBe(clave)
    r2.error(corte(), { status: 0 })
    await p2
  })
})
