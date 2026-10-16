import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { CargadorDePagina, PedidoDePagina } from '../../../nucleo/tabla/tipos'
import { aPaginaPorCursor } from './contrato-erp'
import { cargadorDePartidas } from './cu101-presupuestos'
import { cargadorDeOrdenes } from './cu102-compras'
import { cargadorDeFacturas } from './cu103-facturas'
import { cargadorDeCuentas } from './cu104-cobros'
import { cargadorDeActivos } from './cu105-activos'

/**
 * H1.S3.M2 · Los cinco listados del ERP por cursor (`erp.yaml`, carril A · A6), contra `HttpTestingController` (doble
 * declarado, regla 65). Por cada adaptador: correcto (ruta, límite, `estados`), cursor (la página 2 va con
 * `antesDe` + `antesDeId` de la 1), límite (vacío y última página con total exacto) e inválido (403 y el 422
 * `FILTRO_DE_LISTADO_INVALIDO` de ese CU, que llega con su código). Datos sintéticos.
 */
const GW = 'http://gw/api/v1'

const LISTADOS: { cu: string; ruta: string; codigo422: string; crear: () => CargadorDePagina<unknown>; estado: string }[] = [
  { cu: 'CU-101 partidas', ruta: '/erp/partidas-presupuestarias', codigo422: 'AP-CU101-04', crear: cargadorDePartidas, estado: 'APROBADO' },
  { cu: 'CU-102 órdenes', ruta: '/erp/ordenes-de-compra', codigo422: 'AP-CU102-04', crear: cargadorDeOrdenes, estado: 'RECIBIDA_PARCIAL' },
  { cu: 'CU-103 facturas', ruta: '/erp/facturas-de-proveedor', codigo422: 'AP-CU103-06', crear: cargadorDeFacturas, estado: 'PAGADA_PARCIAL' },
  { cu: 'CU-104 cuentas por cobrar', ruta: '/erp/cuentas-por-cobrar', codigo422: 'AP-CU104-04', crear: cargadorDeCuentas, estado: 'INCOBRABLE' },
  { cu: 'CU-105 activos', ruta: '/erp/activos', codigo422: 'AP-CU105-04', crear: cargadorDeActivos, estado: 'DADO_DE_BAJA' },
]

const pedido = (pagina: number, filtros: Record<string, string> = {}): PedidoDePagina => ({ pagina, tamano: 2, orden: null, filtros })

describe.each(LISTADOS)('$cu · GET $ruta por cursor', ({ ruta, codigo422, crear, estado }) => {
  let http: HttpTestingController
  let cargar: CargadorDePagina<unknown>

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    cargar = TestBed.runInInjectionContext(crear)
  })

  const flushTras = async (url: string, cuerpo: object) => {
    await Promise.resolve()
    http.expectOne(url).flush(cuerpo)
  }

  it('correcto · la primera página va sin cursor, con el límite de la tabla y el filtro como `estados`', async () => {
    const promesa = cargar(pedido(1, { estado }))
    await flushTras(`${GW}${ruta}?limite=2&estados=${estado}`, { items: [{ id: 1 }, { id: 2 }], proximoAntesDe: '2026-03-01', proximoAntesDeId: 'u-2' })
    // Hay próxima: la tabla ofrece una página más, sin inventar un total.
    expect(await promesa).toEqual({ filas: [{ id: 1 }, { id: 2 }], total: 4 })
  })

  it('cursor · la página 2 lleva antesDe + antesDeId tal como los devolvió la 1; en la última el total es exacto', async () => {
    const primera = cargar(pedido(1))
    await flushTras(`${GW}${ruta}?limite=2`, { items: [{ id: 1 }, { id: 2 }], proximoAntesDe: '2026-03-01T10:00:00Z', proximoAntesDeId: 'u-2' })
    await primera
    const segunda = cargar(pedido(2))
    await flushTras(`${GW}${ruta}?limite=2&antesDe=2026-03-01T10:00:00Z&antesDeId=u-2`, { items: [{ id: 3 }], proximoAntesDe: null, proximoAntesDeId: null })
    expect(await segunda).toEqual({ filas: [{ id: 3 }], total: 3 })
  })

  it('límite · página vacía: sin filas y sin página siguiente', async () => {
    const promesa = cargar(pedido(1))
    await flushTras(`${GW}${ruta}?limite=2`, { items: [] })
    expect(await promesa).toEqual({ filas: [], total: 0 })
  })

  it('inválido · 403: el error llega traducido, nunca el cuerpo del backend', async () => {
    const promesa = cargar(pedido(1)).catch((e: ErrorTraducido) => e)
    await Promise.resolve()
    http.expectOne(`${GW}${ruta}?limite=2`).flush({ mensaje: 'interno' }, { status: 403, statusText: 'Forbidden' })
    expect(await promesa).toMatchObject({ estado: 403, mensaje: 'No tenés acceso a esto.' })
  })

  it(`inválido · 422 ${codigo422}: llega con su código para que la pantalla lo diga`, async () => {
    const promesa = cargar(pedido(1, { estado: 'INEXISTENTE' })).catch((e: ErrorTraducido) => e)
    await Promise.resolve()
    http
      .expectOne(`${GW}${ruta}?limite=2&estados=INEXISTENTE`)
      .flush({ codigo: codigo422, mensaje: 'interno', trazaId: 'tz-422' }, { status: 422, statusText: 'Unprocessable Entity' })
    expect(await promesa).toMatchObject({ estado: 422, codigo: codigo422, trazaId: 'tz-422' })
  })
})

describe('aPaginaPorCursor · el cursor va entero o no va', () => {
  it('con las dos mitades hay siguiente', () => {
    expect(aPaginaPorCursor({ items: [1], proximoAntesDe: '2026-01-01', proximoAntesDeId: 'u' }).siguiente).toEqual({ antesDe: '2026-01-01', antesDeId: 'u' })
  })

  it('con una sola mitad se trata como fin: pedir con un cursor a medias sería un 422 seguro', () => {
    expect(aPaginaPorCursor({ items: [1], proximoAntesDe: '2026-01-01', proximoAntesDeId: null }).siguiente).toBeNull()
    expect(aPaginaPorCursor({ items: [1], proximoAntesDe: null, proximoAntesDeId: 'u' }).siguiente).toBeNull()
  })

  it('sin campos de cursor (el contrato los declara opcionales) también es fin', () => {
    expect(aPaginaPorCursor({ items: [] })).toEqual({ items: [], siguiente: null })
  })
})
