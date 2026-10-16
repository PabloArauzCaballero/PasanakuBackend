import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { CargadorDePagina, PedidoDePagina } from '../../../nucleo/tabla/tipos'
import { aPaginaPorCursor } from './contrato-publicidad'
import { cargadorDeAnunciantes } from './cu110-anunciantes'
import { cargadorDeCampanas } from './cu111-campanas'
import { cargadorDePiezasPendientes } from './cu112-moderacion'

/**
 * H1.S3.M3 · Los tres listados de publicidad por cursor (`publicidad.yaml`, carril A · H7) contra `HttpTestingController`
 * (doble declarado, regla 65): correcto, cursor `despuesDeFecha` + `despuesDeId`, límite (vacío, última página) e
 * inválido (403 y el 422 `FILTRO_DE_LISTADO_INVALIDO` de cada CU). Datos sintéticos.
 */
const GW = 'http://gw/api/v1'

const LISTADOS: { cu: string; base: string; codigo422: string; crear: () => CargadorDePagina<unknown>; filtro: Record<string, string> }[] = [
  { cu: 'CU-110 anunciantes', base: `${GW}/publicidad/anunciantes?limite=2`, codigo422: 'AP-CU110-04', crear: cargadorDeAnunciantes, filtro: { estado: 'SUSPENDIDO' } },
  { cu: 'CU-111 campañas', base: `${GW}/publicidad/campanas?limite=2`, codigo422: 'AP-CU111-04', crear: cargadorDeCampanas, filtro: { estado: 'ACTIVA' } },
  { cu: 'CU-112 piezas', base: `${GW}/publicidad/piezas-creativas?limite=2&estadoModeracion=PENDIENTE`, codigo422: 'AP-CU112-04', crear: cargadorDePiezasPendientes, filtro: {} },
]

const pedido = (pagina: number, filtros: Record<string, string> = {}): PedidoDePagina => ({ pagina, tamano: 2, orden: null, filtros })

describe.each(LISTADOS)('$cu · listado por cursor', ({ base, codigo422, crear, filtro }) => {
  let http: HttpTestingController
  let cargar: CargadorDePagina<unknown>

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
    http = TestBed.inject(HttpTestingController)
    cargar = TestBed.runInInjectionContext(crear)
  })

  const responder = async (url: string, cuerpo: object, estado?: number) => {
    await Promise.resolve()
    http.expectOne(url).flush(cuerpo, estado ? { status: estado, statusText: 'x' } : {})
  }

  it('correcto · primera página sin cursor; el filtro de la URL viaja como lo nombra el contrato', async () => {
    const promesa = cargar(pedido(1, filtro))
    const url = filtro['estado'] ? `${base}&estados=${filtro['estado']}` : base
    await responder(url, { items: [{ id: 1 }, { id: 2 }], proximoFecha: '2026-09-01T12:00:00Z', proximoId: 'u-2' })
    expect(await promesa).toEqual({ filas: [{ id: 1 }, { id: 2 }], total: 4 })
  })

  it('cursor · la página 2 lleva despuesDeFecha + despuesDeId de la 1; en la última el total es exacto', async () => {
    const primera = cargar(pedido(1))
    await responder(base, { items: [{ id: 1 }, { id: 2 }], proximoFecha: '2026-09-01T12:00:00Z', proximoId: 'u-2' })
    await primera
    const segunda = cargar(pedido(2))
    await responder(`${base}&despuesDeFecha=2026-09-01T12:00:00Z&despuesDeId=u-2`, { items: [{ id: 3 }], proximoFecha: null, proximoId: null })
    expect(await segunda).toEqual({ filas: [{ id: 3 }], total: 3 })
  })

  it('límite · vacío', async () => {
    const promesa = cargar(pedido(1))
    await responder(base, { items: [] })
    expect(await promesa).toEqual({ filas: [], total: 0 })
  })

  it('inválido · 403 traducido', async () => {
    const promesa = cargar(pedido(1)).catch((e: ErrorTraducido) => e)
    await responder(base, { mensaje: 'interno' }, 403)
    expect(await promesa).toMatchObject({ estado: 403, mensaje: 'No tenés acceso a esto.' })
  })

  it(`inválido · 422 ${codigo422} con su código`, async () => {
    const promesa = cargar(pedido(1)).catch((e: ErrorTraducido) => e)
    await responder(base, { codigo: codigo422, mensaje: 'interno', trazaId: 'tz' }, 422)
    expect(await promesa).toMatchObject({ estado: 422, codigo: codigo422 })
  })
})

describe('aPaginaPorCursor (publicidad) · el cursor va entero o no va', () => {
  it('con una sola mitad es fin, no un pedido a medias', () => {
    expect(aPaginaPorCursor({ items: [1], proximoFecha: '2026-09-01T12:00:00Z', proximoId: null }).siguiente).toBeNull()
    expect(aPaginaPorCursor({ items: [1], proximoFecha: '2026-09-01T12:00:00Z', proximoId: 'u' }).siguiente).toEqual({ despuesDeFecha: '2026-09-01T12:00:00Z', despuesDeId: 'u' })
  })
})
