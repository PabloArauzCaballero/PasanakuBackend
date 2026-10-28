import type { HttpTestingController } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, PAGINA_VACIA_ASC, responder, texto } from '../comun/bandeja.apoyo'
import { BandejaDeRetiros } from './bandeja-de-retiros'
import type { RetiroEnRevision } from './dominio/cu11-retiros-en-revision'

/**
 * **UI contra HTTP simulado** (no prueba integración con `nucleo-financiero`). Las filas salen del ejemplo generado
 * del contrato (`packages/simulado`, sintético) y de variaciones suyas. Tres niveles: aceptado, límite y error.
 */
const URL = `${GW}/billetera/retiros`
const OK = ejemploDe('nucleo-financiero', 'listarRetirosEnRevision', 'ok').cuerpo as { items: RetiroEnRevision[]; proximoFecha: string; proximoId: string }
const FILA = OK.items[0]!
const PROPIA: RetiroEnRevision = { ...FILA, ordenRetiroId: '11111111-1111-4111-8111-111111111111', puedeDecidir: false, mfaVerificado: false }

describe('BandejaDeRetiros · [UI contra HTTP simulado] CU-11', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const pedido = () => http.expectOne((r) => r.url === URL)
  const l = (f: { componentInstance: BandejaDeRetiros }) => f.componentInstance['l']

  it('aceptado: pide 10 sin cursor, pinta la página con su monto y moneda, y Siguiente pide con (proximoFecha, proximoId)', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    const primera = pedido()
    expect(primera.request.params.get('limite')).toBe('10')
    expect(primera.request.params.has('despuesDeFecha')).toBe(false)
    await responder(f, primera, OK)
    expect(filasDe<RetiroEnRevision>(f)).toEqual(OK.items)
    expect(texto(f)).toContain('Bs')
    boton(f, 'Siguiente')!.click()
    const segunda = pedido()
    expect(segunda.request.params.get('despuesDeFecha')).toBe(OK.proximoFecha)
    expect(segunda.request.params.get('despuesDeId')).toBe(OK.proximoId)
    await responder(f, segunda, PAGINA_VACIA_ASC)
  })

  it('aceptado: «Revisar y decidir» abre retiros/:ordenId con la fila; una orden propia no ofrece el botón', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), { ...OK, items: [FILA, PROPIA] })
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    const botones = [...(f.nativeElement as HTMLElement).querySelectorAll('button[apAccionDeFila]')]
    expect(botones).toHaveLength(1)
    ;(botones[0] as HTMLButtonElement).click()
    expect(navegar).toHaveBeenCalledWith([FILA.ordenRetiroId], expect.objectContaining({ state: { retiro: FILA } }))
    expect(texto(f)).toContain('La pediste vos')
  })

  it('aceptado: el buscador y los filtros locales trabajan sobre la página cargada sin volver a pedir', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), { ...OK, items: [FILA, PROPIA] })
    l(f).filtrar('segundoFactor', 'Sin segundo factor')
    await asentar(f)
    expect(filasDe<RetiroEnRevision>(f)).toEqual([PROPIA])
    l(f).limpiar()
    l(f).buscar(FILA.ordenRetiroId.slice(0, 8))
    await asentar(f)
    expect(filasDe<RetiroEnRevision>(f)).toEqual([FILA])
    http.expectNone((r) => r.url === URL)
  })

  it('límite: bandeja vacía ⇒ lo explica, sin tabla ni error', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), PAGINA_VACIA_ASC)
    expect(texto(f)).toContain('No hay retiros esperando una segunda firma.')
    expect(filasDe(f)).toBeNull()
  })

  it('límite: búsqueda sin coincidencias ⇒ vacío por filtro con «Quitar filtros», que vuelve a mostrar la página', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), OK)
    l(f).buscar('no-existe-nada')
    await asentar(f)
    expect(texto(f)).toContain('Ningún registro de esta página coincide')
    boton(f, 'Quitar filtros')!.click()
    await asentar(f)
    expect(filasDe<RetiroEnRevision>(f)).toEqual(OK.items)
  })

  it('error: 403 ⇒ «sin acceso», sin tabla ni mensaje crudo', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), { codigo: 'AP-GW-403', mensaje: 'crudo', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(f)).toBeNull()
    expect(texto(f)).not.toContain('crudo')
  })

  it('error: 500 ⇒ error accionable con código de seguimiento; «Volver a intentar» repite el pedido', async () => {
    const f = await montar(BandejaDeRetiros, ['RETIRO_APROBAR'])
    await responder(f, pedido(), { codigo: 'AP-GW-500', mensaje: 'crudo del backend', trazaId: 'traza-1' }, 500)
    expect(texto(f)).toContain('Retiros en revisión: no se pudo cargar la información.')
    expect(texto(f)).toContain('traza-1')
    expect(texto(f)).not.toContain('crudo del backend')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, pedido(), OK)
    expect(filasDe<RetiroEnRevision>(f)).toEqual(OK.items)
  })
})
