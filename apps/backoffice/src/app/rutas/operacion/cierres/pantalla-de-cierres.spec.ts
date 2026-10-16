import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { configurar, GW, montar, responder, tablaDe, texto } from '../comun/bandeja.apoyo'
import type { CierreDiarioEnLista } from './dominio/cu51-cierres-diarios'
import { PantallaDeCierres } from './pantalla-de-cierres'

/** **UI contra HTTP simulado** de `listarCierresDiarios` (CU-51). Página del ejemplo generado del contrato (sintética). */
const URL = `${GW}/contabilidad/cierres-diarios`
const OK = ejemploDe('nucleo-financiero', 'listarCierresDiarios', 'ok').cuerpo as { items: CierreDiarioEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }

describe('PantallaDeCierres · [UI contra HTTP simulado] CU-51', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)

  it('correcto: página por cursor (antesDe/antesDeId); Contabilidad ve el formulario con los prerrequisitos', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER', 'CONTABILIDAD'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(texto(f)).toContain('Solo se cierra un día ya terminado en La Paz')
    const tabla = tablaDe<CierreDiarioEnLista>(f)!
    expect(tabla.filas()).toEqual(OK.items)
    tabla.pagina.set(2)
    f.detectChanges()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, { items: [] })
  })

  it('límite: sin el rol CONTABILIDAD no hay formulario y la lista vacía se explica', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(f, listado(), { items: [], proximoAntesDe: null, proximoAntesDeId: null })
    expect(f.nativeElement.querySelector('ap-formulario-de-cierre')).toBeNull()
    expect(texto(f)).toContain('Cerrar un día es de Contabilidad')
    expect(texto(f)).toContain('Todavía no hay días cerrados.')
  })

  it('inválido: 403 ⇒ sin acceso y sin tabla; 422 AP-CU51-05 ⇒ error traducido', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(tablaDe(f)).toBeNull()
    const g = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(g, listado(), { codigo: 'AP-CU51-05', mensaje: 'crudo', trazaId: 't' }, 422)
    expect(texto(g)).toContain('No pudimos traer la bandeja.')
    expect(texto(g)).not.toContain('crudo')
  })
})
