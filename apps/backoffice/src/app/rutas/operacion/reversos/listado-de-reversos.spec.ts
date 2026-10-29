import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, PAGINA_VACIA_DESC, responder, texto } from '../comun/bandeja.apoyo'
import type { ReversoEnLista } from './dominio/cu14-reversos'
import { ListadoDeReversos } from './listado-de-reversos'

/** **UI contra HTTP simulado** de `listarReversos` (CU-14). Página del ejemplo generado del contrato (sintética). */
const URL = `${GW}/contabilidad/reversos`
const OK = ejemploDe('nucleo-financiero', 'listarReversos', 'ok').cuerpo as { items: ReversoEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }
const FILA = OK.items[0]!
const OTRA: ReversoEnLista = { ...FILA, reversoId: '22222222-2222-4222-8222-222222222222', tipo: 'CONTRACARGO' as ReversoEnLista['tipo'], motivo: 'Contracargo del banco emisor por una operación duplicada '.repeat(4) }

describe('ListadoDeReversos · [UI contra HTTP simulado] CU-14', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const pedido = () => http.expectOne((r) => r.url === URL)
  const l = (f: { componentInstance: ListadoDeReversos }) => f.componentInstance['l']

  it('aceptado: sin filtro no manda estados; Siguiente va con antesDe/antesDeId; la fila no ofrece Editar ni Eliminar', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    const primera = pedido()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(filasDe<ReversoEnLista>(f)).toEqual(OK.items)
    expect(boton(f, 'Editar')).toBeUndefined()
    expect(boton(f, 'Eliminar')).toBeUndefined()
    boton(f, 'Siguiente')!.click()
    const segunda = pedido()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, PAGINA_VACIA_DESC)
  })

  it('aceptado: elegir un estado pide al servidor `estados` en una sola clave y vuelve a la página 1', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), OK)
    l(f).filtrar('estados', 'RECHAZADO')
    const req = pedido()
    expect(req.request.params.getAll('estados')).toEqual(['RECHAZADO'])
    expect(req.request.params.has('antesDe')).toBe(false)
    await responder(f, req, OK)
  })

  it('aceptado: el filtro de tipo y la búsqueda por motivo trabajan sobre la página', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), { ...OK, items: [FILA, OTRA] })
    l(f).filtrar('tipo', 'CONTRACARGO')
    await asentar(f)
    expect(filasDe<ReversoEnLista>(f)).toEqual([OTRA])
    l(f).limpiar()
    l(f).buscar('banco emisor')
    await asentar(f)
    expect(filasDe<ReversoEnLista>(f)).toEqual([OTRA])
  })

  it('límite: un estado sin reversos ⇒ vacío por filtro con «Quitar filtros», que vuelve a pedir sin estados', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), OK)
    l(f).filtrar('estados', 'EJECUTADO')
    await responder(f, pedido(), PAGINA_VACIA_DESC)
    expect(texto(f)).toContain('Ningún registro de esta página coincide')
    boton(f, 'Quitar filtros')!.click()
    const req = pedido()
    expect(req.request.params.has('estados')).toBe(false)
    await responder(f, req, OK)
  })

  it('límite: sin reversos ⇒ vacío sin datos con su explicación', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), PAGINA_VACIA_DESC)
    expect(texto(f)).toContain('No hay reversos en estos estados.')
  })

  it('error: 403 ⇒ sin acceso y sin tabla; 422 ⇒ error accionable sin el mensaje crudo, con reintento', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(f)).toBeNull()
    l(f).recargar()
    await responder(f, pedido(), { codigo: 'AP-CU14-04', mensaje: 'crudo', trazaId: 'traza-14' }, 422)
    expect(texto(f)).toContain('Reversos: no se pudo cargar la información.')
    expect(texto(f)).not.toContain('crudo')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, pedido(), OK)
    expect(filasDe<ReversoEnLista>(f)).toEqual(OK.items)
  })
})
