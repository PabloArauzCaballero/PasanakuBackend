import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, PAGINA_VACIA_DESC, responder, texto } from '../comun/bandeja.apoyo'
import type { CierreDiarioEnLista, SalidaCierreDiario } from './dominio/cu51-cierres-diarios'
import { FormularioDeCierre } from './formulario-de-cierre'
import { PantallaDeCierres } from './pantalla-de-cierres'

/** **UI contra HTTP simulado** de `listarCierresDiarios` (CU-51). Ejemplo generado del contrato (sintético). */
const URL = `${GW}/contabilidad/cierres-diarios`
const OK = ejemploDe('nucleo-financiero', 'listarCierresDiarios', 'ok').cuerpo as { items: CierreDiarioEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }
const FILA = OK.items[0]!
const OBSERVADO: CierreDiarioEnLista = { ...FILA, cierreId: '44444444-4444-4444-8444-444444444444', cuadrado: false }

describe('PantallaDeCierres · [UI contra HTTP simulado] CU-51', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)
  const l = (f: { componentInstance: PantallaDeCierres }) => f.componentInstance['l']

  it('aceptado: página por cursor (antesDe/antesDeId); los importes sin moneda lo dicen; sin Editar ni Eliminar', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(filasDe<CierreDiarioEnLista>(f)).toEqual(OK.items)
    expect(texto(f)).toContain('· sin moneda')
    expect(boton(f, 'Eliminar')).toBeUndefined()
    boton(f, 'Siguiente')!.click()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, PAGINA_VACIA_DESC)
  })

  it('aceptado: Contabilidad abre el formulario desde «Cerrar un día» (con los prerrequisitos) y un cierre vuelve a pedir el historial', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER', 'CONTABILIDAD'])
    await responder(f, listado(), OK)
    expect(f.nativeElement.querySelector('ap-formulario-de-cierre')).toBeNull()
    boton(f, 'Cerrar un día')!.click()
    await asentar(f)
    expect(texto(f)).toContain('Solo se cierra un día ya terminado en La Paz')
    const formulario = f.debugElement.query((d) => d.componentInstance instanceof FormularioDeCierre).componentInstance as FormularioDeCierre
    formulario.cerrado.emit({} as SalidaCierreDiario)
    await responder(f, listado(), OK)
    boton(f, 'Cerrar el formulario')!.click()
    await asentar(f)
    expect(f.nativeElement.querySelector('ap-formulario-de-cierre')).toBeNull()
  })

  it('aceptado: el filtro de resultado deja solo los días con observaciones', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(f, listado(), { ...OK, items: [FILA, OBSERVADO] })
    l(f).filtrar('resultado', 'Con observaciones')
    await asentar(f)
    expect(filasDe<CierreDiarioEnLista>(f)).toEqual(FILA.cuadrado ? [OBSERVADO] : [FILA, OBSERVADO])
  })

  it('límite: sin el rol CONTABILIDAD no hay botón ni formulario (lo explica) y la lista vacía se explica', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(f, listado(), PAGINA_VACIA_DESC)
    expect(boton(f, 'Cerrar un día')).toBeUndefined()
    expect(texto(f)).toContain('Cerrar un día es de Contabilidad')
    expect(texto(f)).toContain('Todavía no hay días cerrados.')
  })

  it('error: 403 ⇒ sin acceso y sin tabla; 422 AP-CU51-05 ⇒ error accionable, sin el crudo, y se reintenta', async () => {
    const f = await montar(PantallaDeCierres, ['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(f)).toBeNull()
    l(f).recargar()
    await responder(f, listado(), { codigo: 'AP-CU51-05', mensaje: 'crudo', trazaId: 't51' }, 422)
    expect(texto(f)).toContain('Historial de cierres: no se pudo cargar la información.')
    expect(texto(f)).not.toContain('crudo')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, listado(), OK)
    expect(filasDe<CierreDiarioEnLista>(f)).toEqual(OK.items)
  })
})
