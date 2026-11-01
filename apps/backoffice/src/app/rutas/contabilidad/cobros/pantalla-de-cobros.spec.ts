import { HttpTestingController } from '@angular/common/http/testing'
import { By } from '@angular/platform-browser'
import { beforeEach, describe, expect, it } from 'vitest'
import type { CuentaPorCobrar } from '../dominio/cu104-cobros'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { FichaDeCobro } from './ficha-de-cobro'
import { PantallaDeCobros } from './pantalla-de-cobros'

/**
 * H5.S1.M4 · CU-104 sobre el listado por cursor, contra `HttpTestingController` (doble declarado, regla 65). Datos
 * sintéticos con la forma de `CuentaPorCobrarEnLista`. Tres niveles y la acción de fila.
 */
const URL = `${GW}/erp/cuentas-por-cobrar?limite=10`

const cuenta = (p: Partial<CuentaPorCobrar> = {}): CuentaPorCobrar => ({
  cuentaPorCobrarId: 'cxc-1',
  origenTipo: 'FACTURA_PUBLICIDAD',
  origenId: 'fp-1',
  terceroComercialId: 't-1',
  terceroRazonSocial: 'Anunciante del Valle SA',
  fechaVencimiento: '2026-11-01',
  estado: 'PENDIENTE',
  monto: { monto: '300.00', moneda: 'BOB' },
  cobrado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '300.00', moneda: 'BOB' },
  ...p,
})

const botonDe = (el: Element | null, texto: string) => Array.from(el?.querySelectorAll('button') ?? []).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement | undefined
type ConListado = { listado: { filtrar(n: string, v: string): void; buscar(t: string): void } }

describe('PantallaDeCobros · CU-104 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_COBRAR'])))

  it('aceptado · el origen se nombra en español y se ofrece cobrar como acción de fila con ícono', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { cuerpo: { items: [cuenta()] } })
    expect(textoDe(f)).toContain('Factura de publicidad')
    const cobrar = botonDe(f.nativeElement.querySelector('tbody'), 'Registrar el cobro')!
    expect(cobrar.querySelector('ap-icono')).not.toBeNull()
    expect(cobrar.getAttribute('aria-label')).toBe('Registrar el cobro de Anunciante del Valle SA')
  })

  it('aceptado · el origen filtra la página; el tercero se encuentra por búsqueda sin acentos', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, {
      cuerpo: { items: [cuenta(), cuenta({ cuentaPorCobrarId: 'cxc-2', origenTipo: 'VENTA_SERVICIO', terceroRazonSocial: 'Cooperativa Ñuñoa' })] },
    })
    const p = f.componentInstance as unknown as ConListado
    p.listado.filtrar('origen', 'VENTA_SERVICIO')
    await esperar(f)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    p.listado.filtrar('origen', '')
    p.listado.buscar('nunoa')
    await esperar(f)
    expect(textoDe(f)).toContain('Cooperativa Ñuñoa')
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    http.expectNone(() => true)
  })

  it('aceptado · el cobro confirmado por el servidor recarga la lista y lo dice', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { cuerpo: { items: [cuenta()] } })
    botonDe(f.nativeElement.querySelector('tbody'), 'Registrar el cobro')!.click()
    await esperar(f)
    f.debugElement.query(By.directive(FichaDeCobro)).componentInstance.cobrado.emit()
    await esperar(f)
    http.expectOne(URL).flush({ items: [cuenta({ estado: 'COBRADA', saldoPendiente: { monto: '0.00', moneda: 'BOB' } })] })
    await esperar(f)
    await esperar(f)
    expect(textoDe(f)).toContain('El cobro de Anunciante del Valle SA quedó registrado')
    expect(botonDe(f.nativeElement.querySelector('tbody'), 'Registrar el cobro')).toBeUndefined()
  })

  it('límite · una cuenta sin tercero (nulable en el contrato) lo dice; una incobrable no ofrece cobrar y cuenta en la cifra', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, {
      cuerpo: { items: [cuenta({ terceroComercialId: null, terceroRazonSocial: null, estado: 'INCOBRABLE' })] },
    })
    expect(textoDe(f)).toContain('Sin tercero registrado')
    expect(botonDe(f.nativeElement.querySelector('tbody'), 'Registrar el cobro')).toBeUndefined()
    expect(textoDe(f)).toContain('Cuenta declarada incobrable')
    const kpis = Array.from(f.nativeElement.querySelectorAll('ap-tarjeta-kpi .valor') as NodeListOf<HTMLElement>).map((k) => k.textContent?.trim())
    expect(kpis).toEqual(['1', '0', '1'])
  })

  it('límite · vacío sin datos', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { cuerpo: { items: [] } })
    expect(textoDe(f)).toContain('No hay cuentas por cobrar con estos filtros.')
    expect(f.nativeElement.querySelector('ap-paginacion')).toBeNull()
  })

  it('error · 422 AP-CU104-04 con el texto del dominio y reintento', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { status: 422, cuerpo: { codigo: 'AP-CU104-04', mensaje: 'x', trazaId: 'tz' } })
    expect(f.nativeElement.querySelector('ap-estado-de-error[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
    botonDe(f.nativeElement.querySelector('ap-estado-de-error'), 'Volver a intentar')!.click()
    await esperar(f)
    http.expectOne(URL).flush({ items: [cuenta()] })
    await esperar(f)
    await esperar(f)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
  })
})
