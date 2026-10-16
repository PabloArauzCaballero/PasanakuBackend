import { HttpClient, provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { cargarEnvios, cargarPlantillas } from './cu80-cu83-mensajeria'

const GW = 'http://gw/api/v1'
const pedido = (pagina: number) => ({ pagina, tamano: 10, orden: null, filtros: {} })

describe('mensajería · listarPlantillas y listarEnvios por cursor (antesDe, antesDeId)', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('correcto: plantillas en /notificaciones/plantillas; la página 2 lleva el cursor de la 1', async () => {
    const cargador = cargarPlantillas(cliente, GW)
    const p1 = cargador(pedido(1))
    http.expectOne(`${GW}/notificaciones/plantillas?limite=10`).flush({ items: [{ versionId: 'v-2' }], proximoAntesDe: '2026-10-01T00:00:00Z', proximoAntesDeId: 'v-2' })
    await p1
    const p2 = cargador(pedido(2))
    http.expectOne(`${GW}/notificaciones/plantillas?limite=10&antesDe=2026-10-01T00:00:00Z&antesDeId=v-2`).flush({ items: [{ versionId: 'v-1' }] })
    expect((await p2).filas).toEqual([{ versionId: 'v-1' }])
    http.verify()
  })

  it('límite: envíos sin cursor de vuelta es la última página (total = filas vistas)', async () => {
    const promesa = cargarEnvios(cliente, GW)(pedido(1))
    http.expectOne(`${GW}/notificaciones/envios?limite=10`).flush({ items: [{ envioId: 'e-1' }] })
    expect(await promesa).toEqual({ filas: [{ envioId: 'e-1' }], total: 1 })
  })

  it('inválido: 403 rechaza con el error HTTP (lo traduce el listado)', async () => {
    const promesa = cargarEnvios(cliente, GW)(pedido(1))
    http.expectOne(`${GW}/notificaciones/envios?limite=10`).flush({ codigo: 'AP-SEG-01' }, { status: 403, statusText: 'Forbidden' })
    await expect(promesa).rejects.toMatchObject({ status: 403 })
  })
})
