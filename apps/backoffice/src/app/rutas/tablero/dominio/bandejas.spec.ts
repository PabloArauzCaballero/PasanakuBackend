import { HttpClient, provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { BANDEJAS, FUERA_DE_CONTRATO, leerBandeja, traducirError, type DefinicionDeBandeja } from './bandejas'

const GW = 'http://gw/api/v1'
const bandeja = (id: DefinicionDeBandeja['id']) => BANDEJAS.find((b) => b.id === id)!

describe('leerBandeja · primera página con limite=1, nunca un total', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('correcto: una fila en la primera página → «pendientes», pidiendo solo limite=1 y sin filtros', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('retiros'))
    const req = http.expectOne((r) => r.url === `${GW}/billetera/retiros`)
    expect(req.request.method).toBe('GET')
    expect(req.request.params.keys()).toEqual(['limite'])
    expect(req.request.params.get('limite')).toBe('1')
    req.flush({ items: [{ ordenRetiroId: 'x' }], proximoFecha: '2026-10-16T10:00:00Z', proximoId: 'y' })
    expect(await promesa).toEqual({ tipo: 'pendientes' })
    http.verify()
  })

  it('límite: página vacía → «al día»', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('casos'))
    http.expectOne(`${GW}/cumplimiento/casos?limite=1`).flush({ items: [], proximoPlazo: null, proximoId: null })
    expect(await promesa).toEqual({ tipo: 'al-dia' })
  })

  it('límite: desembolsos sin cursor en la última página (el contrato los omite) también es «al día»', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('desembolsos'))
    http.expectOne(`${GW}/desembolsos?limite=1`).flush({ items: [] })
    expect(await promesa).toEqual({ tipo: 'al-dia' })
  })

  it('inválido: 403 → error «No tenés acceso a esto.», nunca «al día»', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('alertas'))
    http.expectOne(`${GW}/cumplimiento/alertas?limite=1`).flush({ codigo: 'AP-SEG-01', mensaje: 'crudo', trazaId: 't-1' }, { status: 403, statusText: 'Forbidden' })
    expect(await promesa).toEqual({ tipo: 'error', error: { mensaje: 'No tenés acceso a esto.', trazaId: 't-1' } })
  })

  it('inválido: 503 → error genérico, sin el mensaje crudo del backend', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('reclamos'))
    http.expectOne(`${GW}/reclamos?limite=1`).flush({ mensaje: 'stacktrace' }, { status: 503, statusText: 'Unavailable' })
    const lectura = await promesa
    expect(lectura.tipo).toBe('error')
    expect(JSON.stringify(lectura)).not.toContain('stacktrace')
  })

  it('inválido: 200 sin `items` (fuera de contrato) → error, no «al día»', async () => {
    const promesa = leerBandeja(cliente, GW, bandeja('reclamos'))
    http.expectOne(`${GW}/reclamos?limite=1`).flush({ total: 3 })
    expect(await promesa).toEqual({ tipo: 'error', error: { mensaje: FUERA_DE_CONTRATO } })
  })
})

describe('BANDEJAS · rutas y permisos del contrato', () => {
  it('cada bandeja apunta a su operación y permiso del YAML', () => {
    expect(BANDEJAS.map((b) => [b.id, b.recurso, b.permisos.join('|')])).toEqual([
      ['retiros', '/billetera/retiros', 'RETIRO_APROBAR'],
      ['reclamos', '/reclamos', 'RECLAMO_VER'],
      ['alertas', '/cumplimiento/alertas', 'CUMPLIMIENTO_ALERTAS'],
      ['casos', '/cumplimiento/casos', 'CUMPLIMIENTO_CASOS'],
      ['desembolsos', '/desembolsos', 'ENTREGA_EJECUTAR|AUDITORIA_LEER'],
    ])
  })

  it('ninguna definición promete una cantidad', () => {
    for (const b of BANDEJAS) expect(`${b.titulo} ${b.definicion}`).not.toMatch(/\d/)
  })
})

describe('traducirError', () => {
  it('respeta el error ya traducido por el interceptor', () => {
    expect(traducirError({ mensaje: 'Tu sesión venció. Volvé a ingresar.', trazaId: 't' })).toEqual({ mensaje: 'Tu sesión venció. Volvé a ingresar.', trazaId: 't' })
  })
  it('algo que no es un error HTTP se dice genérico', () => {
    expect(traducirError(new Error('x')).mensaje).toBe('Algo salió mal de nuestro lado. Probá de nuevo en un momento.')
  })
})
