import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, PAGINA_VACIA_DESC, responder, texto } from '../comun/bandeja.apoyo'
import { BandejaDeDesembolsos } from './bandeja-de-desembolsos'
import type { DesembolsoEnLista } from './dominio/cu28-desembolsos'

/** **UI contra HTTP simulado** de `listarDesembolsos` (CU-28, `entregas.yaml`). Ejemplo generado del contrato (sintético). */
const URL = `${GW}/desembolsos`
const OK = ejemploDe('entregas', 'listarDesembolsos', 'ok').cuerpo as { items: DesembolsoEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }
const FILA = OK.items[0]!
const FALLIDO: DesembolsoEnLista = { ...FILA, ordenId: '33333333-3333-4333-8333-333333333333', ultimoResultado: 'FALLIDO' as DesembolsoEnLista['ultimoResultado'], ultimoCodigoError: 'BNK-51' }

describe('BandejaDeDesembolsos · [UI contra HTTP simulado] CU-28', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const pedido = () => http.expectOne((r) => r.url === URL)
  const l = (f: { componentInstance: BandejaDeDesembolsos }) => f.componentInstance['l']

  it('aceptado: sin estado el servidor decide (excepciones); el monto sale con su moneda; sin acciones de escritura', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    const primera = pedido()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(filasDe<DesembolsoEnLista>(f)).toEqual(OK.items)
    expect(texto(f)).toMatch(/Bs|USD/)
    expect(boton(f, 'Eliminar')).toBeUndefined()
  })

  it('aceptado: el estado viaja como `estados` repetido (explode: true) y Siguiente usa el cursor descendente', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(f, pedido(), OK)
    l(f).filtrar('estados', 'ACREDITADA')
    const req = pedido()
    expect(req.request.params.getAll('estados')).toEqual(['ACREDITADA'])
    await responder(f, req, OK)
    boton(f, 'Siguiente')!.click()
    const segunda = pedido()
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    expect(segunda.request.params.getAll('estados')).toEqual(['ACREDITADA'])
    await responder(f, segunda, PAGINA_VACIA_DESC)
  })

  it('aceptado: el filtro de último resultado y la búsqueda por código del proveedor trabajan sobre la página', async () => {
    const f = await montar(BandejaDeDesembolsos, ['AUDITORIA_LEER'])
    await responder(f, pedido(), { ...OK, items: [FILA, FALLIDO] })
    l(f).buscar('bnk-51')
    await asentar(f)
    expect(filasDe<DesembolsoEnLista>(f)).toEqual([FALLIDO])
  })

  it('límite: sin excepciones ⇒ vacío explicado', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(f, pedido(), PAGINA_VACIA_DESC)
    expect(texto(f)).toContain('No hay desembolsos en estos estados.')
  })

  it('error: 403 ⇒ sin acceso; 500 ⇒ error accionable y «Volver a intentar» repite', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(f, pedido(), { codigo: 'AP-GW-403', mensaje: 'x' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    l(f).recargar()
    await responder(f, pedido(), { codigo: 'AP-GW-500', mensaje: 'crudo', trazaId: 'traza-28' }, 500)
    expect(texto(f)).toContain('traza-28')
    expect(texto(f)).not.toContain('crudo')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, pedido(), OK)
    expect(filasDe<DesembolsoEnLista>(f)).toEqual(OK.items)
  })
})
