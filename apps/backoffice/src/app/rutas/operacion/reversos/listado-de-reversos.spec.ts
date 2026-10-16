import type { HttpTestingController } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, GW, montar, responder, tablaDe, texto } from '../comun/bandeja.apoyo'
import type { ReversoEnLista } from './dominio/cu14-reversos'
import { ListadoDeReversos } from './listado-de-reversos'

/** **UI contra HTTP simulado** de `listarReversos` (CU-14), solo lectura. Página del ejemplo generado del contrato (sintética). */
const URL = `${GW}/contabilidad/reversos`
const OK = ejemploDe('nucleo-financiero', 'listarReversos', 'ok').cuerpo as { items: ReversoEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }

describe('ListadoDeReversos · [UI contra HTTP simulado] CU-14', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)

  it('correcto: sin filtro no manda estados; la segunda página va con antesDe/antesDeId; no hay acciones de escritura', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    const tabla = tablaDe<ReversoEnLista>(f)!
    expect(tabla.filas()).toEqual(OK.items)
    expect(f.nativeElement.querySelector('ap-dialogo, form')).toBeNull()
    tabla.pagina.set(2)
    f.detectChanges()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, { items: [], proximoAntesDe: null, proximoAntesDeId: null })
  })

  it('límite: con ?filtro=SOLICITADO,AUTORIZADO viaja una sola clave estados en CSV; vacío por filtro lo explica', async () => {
    await TestBed.inject(Router).navigateByUrl('/?filtro=SOLICITADO,AUTORIZADO')
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    const req = listado()
    expect(req.request.params.getAll('estados')).toEqual(['SOLICITADO,AUTORIZADO'])
    await responder(f, req, { items: [], proximoAntesDe: null, proximoAntesDeId: null })
    expect(texto(f)).toContain('No hay reversos en estos estados.')
    expect(texto(f)).toContain('Quitar los filtros')
  })

  it('inválido: 403 ⇒ sin acceso y sin tabla; 422 AP-CU14-04 ⇒ error traducido', async () => {
    const f = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(tablaDe(f)).toBeNull()
    const g = await montar(ListadoDeReversos, ['AUDITORIA_LEER'])
    await responder(g, listado(), { codigo: 'AP-CU14-04', mensaje: 'crudo', trazaId: 't' }, 422)
    expect(texto(g)).toContain('No pudimos traer la bandeja.')
    expect(texto(g)).not.toContain('crudo')
  })
})
