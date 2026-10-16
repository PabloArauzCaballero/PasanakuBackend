import type { HttpTestingController } from '@angular/common/http/testing'
import type { Type } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { PantallaDeCierres } from '../cierres/pantalla-de-cierres'
import { PantallaDeConciliaciones } from '../custodia/pantalla-de-conciliaciones'
import { BandejaDeDesembolsos } from '../desembolsos/bandeja-de-desembolsos'
import { RespuestaAReclamo } from '../reclamos/respuesta-a-reclamo'
import { BandejaDeRetiros } from '../retiros/bandeja-de-retiros'
import { ListadoDeReversos } from '../reversos/listado-de-reversos'
import type { ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { configurar, GW, montar, responder } from './bandeja.apoyo'

/** Accesibilidad (axe) de las pantallas nuevas del lote L1, con la página del ejemplo generado del contrato cargada. */
const CASOS: [string, Type<unknown>, string[], string, string, string][] = [
  ['retiros', BandejaDeRetiros, ['RETIRO_APROBAR'], '/billetera/retiros', 'nucleo-financiero', 'listarRetirosEnRevision'],
  ['custodia (Tesorería)', PantallaDeConciliaciones, ['AUDITORIA_LEER', 'TESORERIA'], '/custodia/conciliaciones', 'nucleo-financiero', 'listarConciliaciones'],
  ['cierres (Contabilidad)', PantallaDeCierres, ['AUDITORIA_LEER', 'CONTABILIDAD'], '/contabilidad/cierres-diarios', 'nucleo-financiero', 'listarCierresDiarios'],
  ['reversos', ListadoDeReversos, ['AUDITORIA_LEER'], '/contabilidad/reversos', 'nucleo-financiero', 'listarReversos'],
  ['desembolsos', BandejaDeDesembolsos, ['ENTREGA_EJECUTAR'], '/desembolsos', 'entregas', 'listarDesembolsos'],
]

describe('Bandejas del lote L1 · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))

  it.each(CASOS)('%s: sin violaciones con la página cargada', async (_n, componente, permisos, ruta, servicio, operacion) => {
    const f = await montar(componente, permisos)
    await responder(f, http.expectOne((r) => r.url === `${GW}${ruta}`), ejemploDe(servicio, operacion, 'ok').cuerpo as object)
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })

  it.each(CASOS)('%s: sin violaciones con el error del servidor', async (_n, componente, permisos, ruta) => {
    const f = await montar(componente, permisos)
    await responder(f, http.expectOne((r) => r.url === `${GW}${ruta}`), { codigo: 'AP-CU00-00', mensaje: 'x', trazaId: 't' }, 422)
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })

  it('respuesta a un reclamo: sin violaciones con la ficha y los errores de campo a la vista', async () => {
    const reclamo = (ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as { items: ReclamoEnBandeja[] }).items[0]!
    const f = TestBed.createComponent(RespuestaAReclamo)
    f.componentRef.setInput('reclamo', reclamo)
    f.detectChanges()
    await responder(f, http.expectOne(`${GW}/reclamos/${reclamo.reclamoId}`), ejemploDe('cumplimiento', 'consultarReclamo', 'ok').cuerpo as object)
    ;(f.componentInstance as unknown as { enviar: () => void }).enviar()
    f.detectChanges()
    expect(await axe(f.nativeElement)).toHaveNoViolations()
  })
})
