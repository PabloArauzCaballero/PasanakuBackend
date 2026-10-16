import { HttpClient, provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { cargarDefiniciones, cargarEjecuciones } from './cu58-reportes'

const GW = 'http://gw/api/v1'
const pedido = (pagina: number, filtros: Record<string, string> = {}) => ({ pagina, tamano: 5, orden: null, filtros })

describe('reportes · listarDefinicionesDeReporte (despuesDe) y listarEjecucionesDeReporte (antesDe)', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('correcto: el catálogo pagina por nombre con despuesDe/despuesDeId', async () => {
    const cargador = cargarDefiniciones(cliente, GW)
    const p1 = cargador(pedido(1))
    http.expectOne(`${GW}/reportes?limite=5`).flush({ items: [{ definicionId: 'd-1' }], proximoDespuesDe: 'Alfa', proximoDespuesDeId: 'd-1' })
    await p1
    const p2 = cargador(pedido(2))
    http.expectOne(`${GW}/reportes?limite=5&despuesDe=Alfa&despuesDeId=d-1`).flush({ items: [] })
    expect((await p2).filas).toEqual([])
    http.verify()
  })

  it('límite: el historial manda los estados elegidos repetidos y termina sin cursor', async () => {
    const promesa = cargarEjecuciones(cliente, GW)(pedido(1, { estado: 'FALLIDA,EXPIRADA' }))
    const req = http.expectOne((r) => r.url === `${GW}/reportes/ejecuciones`)
    expect(req.request.params.getAll('estados')).toEqual(['FALLIDA', 'EXPIRADA'])
    req.flush({ items: [] })
    expect(await promesa).toEqual({ filas: [], total: 0 })
  })

  it('inválido: un cursor a medias (solo proximoAntesDe) no se usa: es la última página', async () => {
    const cargador = cargarEjecuciones(cliente, GW)
    const promesa = cargador(pedido(1))
    http.expectOne(`${GW}/reportes/ejecuciones?limite=5`).flush({ items: [{ ejecucionId: 'x' }], proximoAntesDe: '2026-10-01T00:00:00Z' })
    expect((await promesa).total).toBe(1)
  })
})
