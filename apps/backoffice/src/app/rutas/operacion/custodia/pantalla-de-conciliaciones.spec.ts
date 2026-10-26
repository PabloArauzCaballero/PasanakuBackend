import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, PAGINA_VACIA_DESC, responder, texto } from '../comun/bandeja.apoyo'
import type { ConciliacionEnLista, SalidaConciliacionCustodia } from './dominio/cu50-conciliaciones'
import { FormularioDeConciliacion } from './formulario-de-conciliacion'
import { PantallaDeConciliaciones } from './pantalla-de-conciliaciones'

/** **UI contra HTTP simulado** de `listarConciliaciones` (CU-50). Página del ejemplo generado del contrato (sintética). */
const URL = `${GW}/custodia/conciliaciones`
const OK = ejemploDe('nucleo-financiero', 'listarConciliaciones', 'ok').cuerpo as { items: ConciliacionEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }
const FILA = OK.items[0]!
const OTRA: ConciliacionEnLista = { ...FILA, conciliacionId: '55555555-5555-4555-8555-555555555555', cuentaCustodiaId: '66666666-6666-4666-8666-666666666666', estado: 'EN_ANALISIS' as ConciliacionEnLista['estado'] }

describe('PantallaDeConciliaciones · [UI contra HTTP simulado] CU-50', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)
  const l = (f: { componentInstance: PantallaDeConciliaciones }) => f.componentInstance['l']

  it('aceptado: primera página sin cursor y la segunda con antesDe/antesDeId; se ve la cuenta de custodia de cada fila', async () => {
    const f = await montar(PantallaDeConciliaciones, ['AUDITORIA_LEER'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(filasDe<ConciliacionEnLista>(f)).toEqual(OK.items)
    expect(texto(f)).toContain(FILA.cuentaCustodiaId)
    boton(f, 'Siguiente')!.click()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, PAGINA_VACIA_DESC)
  })

  it('aceptado: Tesorería abre el formulario con «Registrar conciliación»; registrar vuelve a pedir el historial', async () => {
    const f = await montar(PantallaDeConciliaciones, ['AUDITORIA_LEER', 'TESORERIA'])
    await responder(f, listado(), OK)
    boton(f, 'Registrar conciliación')!.click()
    await asentar(f)
    const formulario = f.debugElement.query((d) => d.componentInstance instanceof FormularioDeConciliacion).componentInstance as FormularioDeConciliacion
    formulario.registrada.emit({} as SalidaConciliacionCustodia)
    await responder(f, listado(), OK)
    expect(filasDe<ConciliacionEnLista>(f)).toEqual(OK.items)
  })

  it('aceptado: buscar por cuenta de custodia y filtrar por estado trabajan sobre la página', async () => {
    const f = await montar(PantallaDeConciliaciones, ['AUDITORIA_LEER'])
    await responder(f, listado(), { ...OK, items: [FILA, OTRA] })
    l(f).buscar('66666666')
    await asentar(f)
    expect(filasDe<ConciliacionEnLista>(f)).toEqual([OTRA])
    l(f).limpiar()
    l(f).filtrar('estado', 'EN_ANALISIS')
    await asentar(f)
    expect(filasDe<ConciliacionEnLista>(f)).toEqual([OTRA])
  })

  it('límite: sin el rol TESORERIA no hay botón ni formulario (lo explica) y la bandeja vacía se explica', async () => {
    const f = await montar(PantallaDeConciliaciones, ['AUDITORIA_LEER'])
    await responder(f, listado(), PAGINA_VACIA_DESC)
    expect(boton(f, 'Registrar conciliación')).toBeUndefined()
    expect(texto(f)).toContain('Registrar una conciliación es de Tesorería')
    expect(texto(f)).toContain('Todavía no hay conciliaciones registradas.')
  })

  it('error: 403 ⇒ sin acceso y sin tabla; 422 AP-CU50-04 ⇒ error accionable, sin el crudo, y reintento', async () => {
    const f = await montar(PantallaDeConciliaciones, ['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(f)).toBeNull()
    l(f).recargar()
    await responder(f, listado(), { codigo: 'AP-CU50-04', mensaje: 'crudo', trazaId: 'traza-50' }, 422)
    expect(texto(f)).toContain('No se pudo cargar historial de conciliaciones.')
    expect(texto(f)).not.toContain('crudo')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, listado(), OK)
    expect(filasDe<ConciliacionEnLista>(f)).toEqual(OK.items)
  })
})
