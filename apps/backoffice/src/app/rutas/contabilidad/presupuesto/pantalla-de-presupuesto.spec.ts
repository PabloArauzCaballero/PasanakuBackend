import { HttpTestingController } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { Router } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import type { PartidaPresupuestaria } from '../dominio/cu101-presupuestos'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { PantallaDePresupuesto } from './pantalla-de-presupuesto'

/**
 * H5.S1.M1 · CU-101 sobre el listado por cursor (`crearListadoPorCursor`), contra `HttpTestingController` (doble
 * declarado del gateway, regla 65). Datos sintéticos con la forma de `PartidaEnLista`. Tres niveles.
 */
const URL = `${GW}/erp/partidas-presupuestarias?limite=10`

function partida(parcial: Partial<PartidaPresupuestaria> = {}): PartidaPresupuestaria {
  return {
    partidaId: 'pp-1',
    presupuestoId: 'p-1',
    presupuestoNombre: 'Operación 2026',
    estado: 'APROBADO',
    centroCostoId: 'cc-1',
    centroCostoNombre: 'Tecnología',
    cuentaContableId: 'cta-1',
    periodoContableId: 'per-3',
    anio: 2026,
    mes: 3,
    fechaInicioPeriodo: '2026-03-01',
    montoPresupuestado: { monto: '1000.00', moneda: 'BOB' },
    montoEjecutado: { monto: '1250.75', moneda: 'BOB' },
    ...parcial,
  }
}

const dos = () => ({
  items: [partida(), partida({ partidaId: 'pp-2', centroCostoNombre: 'Marketing', estado: 'BORRADOR', montoEjecutado: { monto: '10.00', moneda: 'BOB' } })],
  proximoAntesDe: null,
  proximoAntesDeId: null,
})

describe('PantallaDePresupuesto · CU-101 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_PRESUPUESTO'])))

  it('aceptado · nombra el período en español, no la cuenta contable (que es de nucleo-financiero), y resalta la desviación', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { cuerpo: dos() })
    expect(textoDe(f)).toContain('marzo 2026')
    expect(textoDe(f)).toContain('Operación 2026')
    expect(textoDe(f)).not.toContain('cta-1')
    expect(f.nativeElement.querySelectorAll('.desviacion.sobre')).toHaveLength(1)
  })

  it('aceptado · cifras de la página: partidas, sobre lo presupuestado y aprobadas (conteos, no sumas)', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { cuerpo: dos() })
    const kpis = Array.from(f.nativeElement.querySelectorAll('ap-tarjeta-kpi .valor') as NodeListOf<HTMLElement>).map((k) => k.textContent?.trim())
    expect(kpis).toEqual(['2', '1', '1'])
  })

  it('aceptado · el estado viaja al servidor como `estados`; centro de costo filtra la página cargada', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { cuerpo: dos() })
    const pantalla = f.componentInstance as unknown as { listado: { filtrar(n: string, v: string): void } }
    pantalla.listado.filtrar('centro', 'Marketing')
    await esperar(f)
    http.expectNone(() => true)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    pantalla.listado.filtrar('estado', 'APROBADO')
    await esperar(f)
    http.expectOne(`${URL}&estados=APROBADO`).flush({ items: [partida()] })
    await esperar(f)
    await esperar(f)
    expect(textoDe(f)).toContain('Tecnología')
  })

  it('aceptado · el enlace `?filtro=APROBADO` de un expediente abre la bandeja ya filtrada', async () => {
    await TestBed.inject(Router).navigateByUrl('/?filtro=APROBADO')
    const f = await montarListado(http, PantallaDePresupuesto, `${URL}&estados=APROBADO`, { cuerpo: { items: [partida()] } })
    expect(textoDe(f)).toContain('Tecnología')
  })

  it('aceptado · no ofrece ordenar por columna ni acciones de fila: el orden lo fija el servidor y no hay operación por partida', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { cuerpo: dos() })
    expect(f.nativeElement.querySelectorAll('th button')).toHaveLength(0)
    expect(f.nativeElement.querySelector('ap-acciones-de-fila')).toBeNull()
    expect(textoDe(f)).toContain('Descargar CSV')
  })

  it('límite · sin partidas lo dice y explica cuándo aparecen', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { cuerpo: { items: [], proximoAntesDe: null, proximoAntesDeId: null } })
    expect(textoDe(f)).toContain('No hay partidas presupuestarias')
    expect(textoDe(f)).toContain('cuando el Directorio aprueba')
  })

  it('error · 403: mensaje traducido con su código de seguimiento, sin el cuerpo del backend', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { status: 403, cuerpo: { mensaje: 'detalle interno', trazaId: 'tz-9' } })
    const alerta = f.nativeElement.querySelector('ap-estado-de-error[role="alert"]') as HTMLElement
    expect(alerta.textContent).toContain('No tenés acceso a esto.')
    expect(alerta.textContent).toContain('tz-9')
    expect(textoDe(f)).not.toContain('detalle interno')
  })

  it('error · 422 AP-CU101-04: filtro inválido con el texto del dominio, y «Volver a intentar» repite el pedido', async () => {
    const f = await montarListado(http, PantallaDePresupuesto, URL, { status: 422, cuerpo: { codigo: 'AP-CU101-04', mensaje: 'interno', trazaId: 'tz-4' } })
    expect(f.nativeElement.querySelector('ap-estado-de-error')?.textContent).toContain('El filtro de la lista no es válido')
    ;(f.nativeElement.querySelector('ap-estado-de-error button') as HTMLButtonElement).click()
    await esperar(f)
    http.expectOne(URL).flush(dos())
    await esperar(f)
    await esperar(f)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(2)
  })
})
