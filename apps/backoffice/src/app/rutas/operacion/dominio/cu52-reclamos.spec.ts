import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { pedirReclamos, sePuedeResponder, type ReclamoEnBandeja } from './cu52-reclamos'

/**
 * El adaptador de `listarBandejaDeReclamos` contra el contrato (`cumplimiento.yaml`): el orden por límite vigente lo
 * hace el SERVIDOR (antes se ordenaba en memoria sobre una lista completa que el contrato nunca publicó); acá se prueba
 * que el cliente no reordena, que `estados` viaja como CSV y que el cursor es el que devolvió el servidor.
 */
const GW = 'http://gw/api/v1'
const OK = ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as { items: ReclamoEnBandeja[]; proximoLimite: string; proximoId: string }
const FILA = OK.items[0]!

describe('pedirReclamos · adaptador de la bandeja (CU-52)', () => {
  let http: HttpTestingController
  let cliente: HttpClient
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('correcto: devuelve las filas en el orden del servidor y el cursor (proximoLimite, proximoId)', async () => {
    const segunda = { ...FILA, reclamoId: 'otro', codigo: 'REC-B', limiteVigente: '2020-01-01T00:00:00Z' }
    const promesa = pedirReclamos(cliente, GW)({ limite: 25, cursor: null, filtros: {} })
    const req = http.expectOne((r) => r.url === `${GW}/reclamos`)
    expect(req.request.params.keys().sort()).toEqual(['limite'])
    req.flush({ ...OK, items: [FILA, segunda] })
    const pagina = await promesa
    expect(pagina.items.map((r) => r.codigo)).toEqual([FILA.codigo, 'REC-B'])
    expect(pagina.siguiente).toEqual({ a: OK.proximoLimite, b: OK.proximoId })
  })

  it('correcto: los estados elegidos viajan como una sola clave CSV y el cursor como despuesDeLimite/despuesDeId', async () => {
    const promesa = pedirReclamos(cliente, GW)({ limite: 10, cursor: { a: '2026-08-06T14:00:00Z', b: FILA.reclamoId }, filtros: { estados: 'ELEVADO,CERRADO' } })
    const req = http.expectOne((r) => r.url === `${GW}/reclamos`)
    expect(req.request.params.getAll('estados')).toEqual(['ELEVADO,CERRADO'])
    expect(req.request.params.get('despuesDeLimite')).toBe('2026-08-06T14:00:00Z')
    expect(req.request.params.get('despuesDeId')).toBe(FILA.reclamoId)
    req.flush({ items: [], proximoLimite: null, proximoId: null })
    expect((await promesa).siguiente).toBeNull()
  })

  it('límite: página vacía o cursor nulo ⇒ no hay próxima', async () => {
    const promesa = pedirReclamos(cliente, GW)({ limite: 25, cursor: null, filtros: {} })
    http.expectOne((r) => r.url === `${GW}/reclamos`).flush({ items: [], proximoLimite: OK.proximoLimite, proximoId: OK.proximoId })
    expect(await promesa).toEqual({ items: [], siguiente: null })
  })

  it('inválido: el 422 AP-CU52-07 sale traducido (con código y estado), no crudo', async () => {
    const promesa = pedirReclamos(cliente, GW)({ limite: 25, cursor: null, filtros: {} })
    http.expectOne((r) => r.url === `${GW}/reclamos`).flush({ codigo: 'AP-CU52-07', mensaje: 'crudo', trazaId: 't' }, { status: 422, statusText: 'x' })
    await expect(promesa).rejects.toMatchObject({ codigo: 'AP-CU52-07', estado: 422 })
  })

  it('un reclamo cerrado o respondido no se responde; uno ingresado o elevado sí', () => {
    expect(sePuedeResponder({ ...FILA, estado: 'CERRADO' } as ReclamoEnBandeja)).toBe(false)
    expect(sePuedeResponder({ ...FILA, estado: 'RESPONDIDO' } as ReclamoEnBandeja)).toBe(false)
    expect(sePuedeResponder({ ...FILA, estado: 'INGRESADO' } as ReclamoEnBandeja)).toBe(true)
    expect(sePuedeResponder({ ...FILA, estado: 'ELEVADO' } as ReclamoEnBandeja)).toBe(true)
  })
})
