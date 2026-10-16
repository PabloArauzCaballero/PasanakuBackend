import { HttpClient, provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { cargarTarifarios, type TarifarioEnLista } from './cu34-tarifarios'
import { EstadoDeListado, FUERA_DE_CONTRATO } from './listado'

const GW = 'http://gw/api/v1'
const URL = `${GW}/tarifas/tarifarios`
// Ejemplo sintético con la forma de `TarifarioEnLista` (tarifas.yaml); no es un tarifario real.
const T1 = { tarifarioId: 't-2', codigo: 'TAR-EJEMPLO', version: 2, nombre: 'Ejemplo', estado: 'VIGENTE', monedaBase: 'BOB', vigenteDesde: '2026-09-01T04:00:00Z', diasPreaviso: 30 } as TarifarioEnLista
const pedido = (pagina: number, filtros: Record<string, string> = {}) => ({ pagina, tamano: 25, orden: null, filtros })

describe('cargarTarifarios · listarTarifarios por cursor (antesDe, antesDeId)', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('correcto: la página 2 se pide con el cursor que devolvió la 1; el total no se inventa', async () => {
    const cargador = cargarTarifarios(cliente, GW)
    const p1 = cargador(pedido(1))
    const r1 = http.expectOne((r) => r.url === URL)
    expect(r1.request.params.get('limite')).toBe('25')
    expect(r1.request.params.has('antesDe')).toBe(false)
    r1.flush({ items: [T1], proximoAntesDe: '2026-09-01T04:00:00Z', proximoAntesDeId: 't-2' })
    expect((await p1).total).toBe(26) // una fila vista + una página más porque el servidor dijo que hay

    const p2 = cargador(pedido(2))
    const r2 = http.expectOne((r) => r.url === URL)
    expect(r2.request.params.get('antesDe')).toBe('2026-09-01T04:00:00Z')
    expect(r2.request.params.get('antesDeId')).toBe('t-2')
    r2.flush({ items: [{ ...T1, tarifarioId: 't-1', version: 1 }] })
    const pagina2 = await p2
    expect(pagina2.filas.map((f) => f.tarifarioId)).toEqual(['t-1'])
    expect(pagina2.total).toBe(26)
    http.verify()
  })

  it('límite: el filtro de estado viaja como `estados` repetido; vacío es cero filas', async () => {
    const promesa = cargarTarifarios(cliente, GW)(pedido(1, { estado: 'VIGENTE,EN_PREAVISO' }))
    const req = http.expectOne((r) => r.url === URL)
    expect(req.request.params.getAll('estados')).toEqual(['VIGENTE', 'EN_PREAVISO'])
    req.flush({ items: [] })
    expect(await promesa).toEqual({ filas: [], total: 0 })
  })

  it('inválido: 403 y respuesta sin `items` dejan el listado en error, sin filas', async () => {
    const estado = new EstadoDeListado()
    const cargador = estado.envolver(cargarTarifarios(cliente, GW))
    const p403 = cargador(pedido(1))
    http.expectOne((r) => r.url === URL).flush({ codigo: 'AP-SEG-01', trazaId: 'tr-1' }, { status: 403, statusText: 'Forbidden' })
    expect(await p403).toEqual({ filas: [], total: 0 })
    expect(estado.error()).toEqual({ mensaje: 'No tenés acceso a esto.', trazaId: 'tr-1' })

    estado.reintentar()
    const cargador2 = estado.envolver(cargarTarifarios(cliente, GW))
    const pMal = cargador2(pedido(1))
    http.expectOne((r) => r.url === URL).flush({ filas: [T1] })
    await pMal
    expect(estado.error()?.mensaje).toBe(FUERA_DE_CONTRATO)
  })
})
