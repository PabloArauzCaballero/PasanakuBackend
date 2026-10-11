import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { olvidarIntentos } from './idempotencia.interceptor'
import { PLAZO_DE_ESCRITURA_MS, PLAZO_DE_LECTURA_MS } from './timeout.interceptor'
import { cobrar, COBRO, fallo, GW, montar } from './cadena-de-red.apoyo'

/** H12.S1.M3 · plazos de espera sobre la cadena real (ver `cadena-de-red.spec.ts` para el montaje compartido). */
describe('cadena de red · tiempo de espera', () => {
  beforeEach(() => {
    olvidarIntentos()
    vi.useFakeTimers()
  })
  afterEach(() => vi.useRealTimers())

  it('una escritura sin respuesta vence a los 30 s como «sin confirmar»: estado 0, tiempoAgotado, mensaje que prohíbe darla por fallida', async () => {
    const { http, backend } = montar()
    const p = fallo(firstValueFrom(cobrar(http)))
    backend.expectOne(COBRO)
    await vi.advanceTimersByTimeAsync(PLAZO_DE_ESCRITURA_MS - 1)
    await vi.advanceTimersByTimeAsync(2)
    const e = await p
    expect(e).toMatchObject({ estado: 0, tiempoAgotado: true, sinConexion: false })
    expect(e?.mensaje).toContain('no sabemos si la operación quedó registrada')
  })

  it('tras ese vencimiento, el reintento del mismo cobro conserva la clave: no se duplica', async () => {
    const { http, backend } = montar()
    const p1 = fallo(firstValueFrom(cobrar(http)))
    const r1 = backend.expectOne(COBRO)
    const clave = r1.request.headers.get('Idempotency-Key')
    await vi.advanceTimersByTimeAsync(PLAZO_DE_ESCRITURA_MS + 1)
    await p1
    const p2 = firstValueFrom(cobrar(http))
    const r2 = backend.expectOne(COBRO)
    expect(r2.request.headers.get('Idempotency-Key')).toBe(clave)
    r2.flush({ ok: true })
    await p2
  })

  it('una lectura vence a los 15 s con un mensaje de lectura, no de escritura', async () => {
    const { http, backend } = montar()
    const p = fallo(firstValueFrom(http.get(`${GW}/recurso`)))
    backend.expectOne(`${GW}/recurso`)
    await vi.advanceTimersByTimeAsync(PLAZO_DE_LECTURA_MS + 1)
    const e = await p
    expect(e).toMatchObject({ estado: 0, tiempoAgotado: true })
    expect(e?.mensaje).toBe('La respuesta tardó demasiado. Probá de nuevo en un momento.')
  })

  it('una respuesta a tiempo no se toca', async () => {
    const { http, backend } = montar()
    const p = firstValueFrom(http.get(`${GW}/recurso`))
    backend.expectOne(`${GW}/recurso`).flush({ ok: 1 })
    await vi.advanceTimersByTimeAsync(PLAZO_DE_ESCRITURA_MS * 2)
    await expect(p).resolves.toEqual({ ok: 1 })
  })
})
