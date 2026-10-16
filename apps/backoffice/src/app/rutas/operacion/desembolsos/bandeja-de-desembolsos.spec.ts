import type { HttpTestingController } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, GW, montar, responder, tablaDe, texto } from '../comun/bandeja.apoyo'
import { BandejaDeDesembolsos } from './bandeja-de-desembolsos'
import type { DesembolsoEnLista } from './dominio/cu28-desembolsos'

/** **UI contra HTTP simulado** de `listarDesembolsos` (CU-28, entregas). Página del ejemplo generado del contrato (sintética). */
const URL = `${GW}/desembolsos`
const OK = ejemploDe('entregas', 'listarDesembolsos', 'ok').cuerpo as { items: DesembolsoEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }

describe('BandejaDeDesembolsos · [UI contra HTTP simulado] CU-28', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)

  it('correcto: por omisión no manda estados (el servidor trae las excepciones) y avanza con antesDe/antesDeId', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    const tabla = tablaDe<DesembolsoEnLista>(f)!
    expect(tabla.filas()).toEqual(OK.items)
    expect(f.componentInstance['COLUMNAS'].map((c) => c.clave)).not.toContain('cuentaDestino')
    tabla.pagina.set(2)
    f.detectChanges()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, { items: [] })
  })

  it('límite: con filtro, estados va repetido (explode: true), uno por valor; la última página sin cursor no ofrece más', async () => {
    await TestBed.inject(Router).navigateByUrl('/?filtro=ACREDITADA,CANCELADA')
    const f = await montar(BandejaDeDesembolsos, ['AUDITORIA_LEER'])
    const req = listado()
    expect(req.request.params.getAll('estados')).toEqual(['ACREDITADA', 'CANCELADA'])
    await responder(f, req, { items: OK.items })
    expect(tablaDe(f)!.total()).toBe(OK.items.length)
  })

  it('límite: vacío explicado', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(f, listado(), { items: [] })
    expect(texto(f)).toContain('No hay desembolsos en estos estados.')
  })

  it('inválido: 403 ⇒ sin acceso y sin tabla; 422 AP-CU28-07 ⇒ error traducido', async () => {
    const f = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(tablaDe(f)).toBeNull()
    const g = await montar(BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'])
    await responder(g, listado(), { codigo: 'AP-CU28-07', mensaje: 'crudo', trazaId: 't' }, 422)
    expect(texto(g)).toContain('No pudimos traer la bandeja.')
    expect(texto(g)).not.toContain('crudo')
  })
})
