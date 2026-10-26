import { HttpClient } from '@angular/common/http'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { montar } from '../compartido/pruebas.apoyo'
import { Indisponible } from '../compartido/indisponible'
import { FORMA_H9, pedirPagina } from './paginas'

const URL = 'http://gw/api/v1/uif/reportes'

function preparar() {
  const { http } = montar(Indisponible, [], 'OPERADOR', { titulo: 't', proposito: 'p', motivo: 'm' })
  return { backend: http, pedir: pedirPagina<{ id: string }>(TestBed.inject(HttpClient), URL, FORMA_H9, ['estados']) }
}

describe('pedirPagina · familia H9 (antesDe / antesDeId, estados con explode: true)', () => {
  it('correcto: estados repetidos, cursor completo y próximo par devuelto como cursor', async () => {
    const { backend, pedir } = preparar()
    const promesa = pedir({ limite: 25, cursor: { fecha: '2026-09-30', id: 'r-9' }, filtros: { estados: 'GENERADO,ENVIADO', otro: 'x' } })
    const pedido = backend.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['GENERADO', 'ENVIADO'])
    expect(pedido.request.params.get('antesDe')).toBe('2026-09-30')
    expect(pedido.request.params.get('antesDeId')).toBe('r-9')
    // Lista blanca: una clave de filtro que el contrato no declara no viaja.
    expect(pedido.request.params.has('otro')).toBe(false)
    pedido.flush({ items: [{ id: 'r-8' }], proximoAntesDe: '2026-08-31', proximoAntesDeId: 'r-8' })
    expect(await promesa).toEqual({ items: [{ id: 'r-8' }], siguiente: { fecha: '2026-08-31', id: 'r-8' } })
  })

  it('límite: sin próximo par, no hay siguiente página', async () => {
    const { backend, pedir } = preparar()
    const promesa = pedir({ limite: 25, cursor: null, filtros: {} })
    const pedido = backend.expectOne((r) => r.url === URL)
    expect(pedido.request.params.has('antesDe')).toBe(false)
    expect(pedido.request.params.has('estados')).toBe(false)
    pedido.flush({ items: [] })
    expect(await promesa).toEqual({ items: [], siguiente: null })
  })

  it('inválido: un cursor a medias del servidor (solo la fecha) no se usa: nunca se manda medio cursor', async () => {
    const { backend, pedir } = preparar()
    const promesa = pedir({ limite: 25, cursor: null, filtros: {} })
    backend.expectOne((r) => r.url === URL).flush({ items: [{ id: 'r-1' }], proximoAntesDe: '2026-08-31' })
    expect((await promesa).siguiente).toBeNull()
  })

  it('correcto (familia H5, explode: false): varios valores viajan en UNA clave con comas', async () => {
    const { backend } = preparar()
    const forma = { parametroFecha: 'despuesDeFecha', parametroId: 'despuesDeId', campoFecha: 'proximoFecha', campoId: 'proximoId', multiplesConComa: true }
    const pedir = pedirPagina<{ id: string }>(TestBed.inject(HttpClient), URL, forma, ['estados'])
    const promesa = pedir({ limite: 25, cursor: null, filtros: { estados: 'ABIERTA,ESCALADA' } })
    const pedido = backend.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['ABIERTA,ESCALADA'])
    pedido.flush({ items: [] })
    await promesa
  })
})
