import { HttpTestingController } from '@angular/common/http/testing'
import { By } from '@angular/platform-browser'
import { beforeEach, describe, expect, it } from 'vitest'
import { CLAVE_POR_CONTENIDO } from '../../../nucleo/idempotencia.interceptor'
import type { OrdenDeCompra } from '../dominio/cu102-compras'
import type { FacturaDeProveedor } from '../dominio/cu103-facturas'
import { configurarListado, esperar, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { FichaDeFactura } from './ficha-de-factura'
import { TablaDeFacturas } from './tabla-de-facturas'
import { TablaDeOrdenes } from './tabla-de-ordenes'

/**
 * H5.S1.M2 / H5.S1.M3 · CU-102 y CU-103 sobre el listado por cursor, contra `HttpTestingController` (doble declarado
 * del gateway, regla 65). Datos sintéticos con la forma de `erp.yaml`. Tres niveles por tabla, más la acción de fila.
 */
const ORDENES = `${GW}/erp/ordenes-de-compra?limite=10`
const FACTURAS = `${GW}/erp/facturas-de-proveedor?limite=10`

const orden = (p: Partial<OrdenDeCompra> = {}): OrdenDeCompra => ({
  ordenCompraId: 'o-1',
  numero: 'OC-0001',
  terceroComercialId: 't-1',
  terceroRazonSocial: 'Proveedor Andino SRL',
  centroCostoId: null,
  centroCostoNombre: null,
  estado: 'RECIBIDA_PARCIAL',
  monto: { monto: '800.00', moneda: 'BOB' },
  creadaEn: '2026-09-01T12:00:00Z',
  ...p,
})

const factura = (p: Partial<FacturaDeProveedor> = {}): FacturaDeProveedor => ({
  facturaProveedorId: 'f-1',
  numeroFactura: 'F-0009',
  terceroComercialId: 't-1',
  terceroRazonSocial: 'Proveedor Andino SRL',
  ordenCompraId: 'o-1',
  fechaEmision: '2026-09-02',
  fechaVencimiento: '2099-10-02',
  estado: 'APROBADA',
  monto: { monto: '800.00', moneda: 'BOB' },
  montoPagado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '800.00', moneda: 'BOB' },
  aprobadaPorLaSesion: false,
  asientoContableId: null,
  ...p,
})

const botonDe = (el: HTMLElement, texto: string) => Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes(texto)) as HTMLButtonElement | undefined

describe('TablaDeOrdenes · CU-102 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_COMPRAS'])))

  it('aceptado · una orden recibida en parte se nombra y es facturable; sin centro de costo lo dice', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden()], proximoAntesDe: null, proximoAntesDeId: null } })
    expect(textoDe(f)).toContain('Recibida en parte')
    expect(textoDe(f)).toContain('Sin centro de costo')
    expect(f.nativeElement.querySelector('.si')?.textContent).toContain('Sí')
    expect(botonDe(f.nativeElement, 'Aprobar')).toBeUndefined()
  })

  it('aceptado · «Aprobar» sobre una orden en borrador pide la confirmación, dispara el POST con clave de idempotencia y recarga', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden({ estado: 'BORRADOR' })] } })
    expect(textoDe(f)).toContain('hay que aprobarla antes de poder facturarla')
    botonDe(f.nativeElement, 'Aprobar')!.click()
    await esperar(f)
    botonDe(document.body, 'Aprobar la orden')!.click()
    await esperar(f)
    const post = http.expectOne(`${GW}/erp/ordenes-de-compra/o-1/aprobacion`)
    expect(post.request.method).toBe('POST')
    expect(post.request.context.get(CLAVE_POR_CONTENIDO)).toBe(true)
    post.flush({ ordenCompraId: 'o-1', estado: 'APROBADA', monto: '800.00' })
    await esperar(f)
    http.expectOne(ORDENES).flush({ items: [orden({ estado: 'APROBADA' })] })
    await esperar(f)
    await esperar(f)
    expect(textoDe(f)).toContain('La orden OC-0001 quedó aprobada')
    expect(textoDe(f)).toContain('Aprobada')
  })

  it('límite · vacío', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [] } })
    expect(textoDe(f)).toContain('No hay órdenes de compra con estos filtros.')
  })

  it('error · 422 AP-CU102-04 se dice con el texto del dominio', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { status: 422, cuerpo: { codigo: 'AP-CU102-04', mensaje: 'x', trazaId: 'tz' } })
    expect(f.nativeElement.querySelector('ap-estado-de-error[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })

  it('error · la aprobación rechazada se dice traducida, sin recargar, y el botón no queda trabado', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden({ estado: 'BORRADOR' })] } })
    botonDe(f.nativeElement, 'Aprobar')!.click()
    await esperar(f)
    botonDe(document.body, 'Aprobar la orden')!.click()
    await esperar(f)
    http.expectOne(`${GW}/erp/ordenes-de-compra/o-1/aprobacion`).flush({ codigo: 'X', mensaje: 'interno', trazaId: 'tz-5' }, { status: 500, statusText: 'x' })
    await esperar(f)
    http.expectNone(ORDENES)
    expect(textoDe(f)).not.toContain('interno')
    expect(botonDe(f.nativeElement, 'Aprobar')!.disabled).toBe(false)
  })
})

describe('TablaDeOrdenes · CU-102 sin permiso de compras', () => {
  it('límite · con solo CONTABILIDAD_ERP_REPORTES, una orden en borrador no ofrece aprobar', async () => {
    const http = configurarListado(['CONTABILIDAD_ERP_REPORTES'])
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden({ estado: 'BORRADOR' })] } })
    expect(botonDe(f.nativeElement, 'Aprobar')).toBeUndefined()
  })
})

describe('TablaDeFacturas · CU-103 por cursor', () => {
  let http: HttpTestingController

  it('aceptado · con CONTABILIDAD_ERP_PAGAR y sin haberla aprobado, ofrece pagar como acción de fila con ícono', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura()] } })
    const pagar = botonDe(f.nativeElement, 'Registrar el pago')!
    expect(pagar.querySelector('ap-icono')).not.toBeNull()
    expect(pagar.getAttribute('aria-label')).toBe('Registrar el pago de la factura F-0009')
  })

  it('aceptado · el pago confirmado por el servidor recarga la lista y lo dice', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura()] } })
    botonDe(f.nativeElement, 'Registrar el pago')!.click()
    await esperar(f)
    f.debugElement.query(By.directive(FichaDeFactura)).componentInstance.pagada.emit()
    await esperar(f)
    http.expectOne(FACTURAS).flush({ items: [factura({ estado: 'PAGADA', saldoPendiente: { monto: '0.00', moneda: 'BOB' } })] })
    await esperar(f)
    await esperar(f)
    expect(textoDe(f)).toContain('El pago de la factura F-0009 quedó registrado')
    expect(botonDe(f.nativeElement.querySelector('tbody'), 'Registrar el pago')).toBeUndefined()
  })

  it('límite · aprobadaPorLaSesion (lo resuelve el servidor): no ofrece pagar, dice por qué (R-CTB-05)', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura({ aprobadaPorLaSesion: true })] } })
    expect(botonDe(f.nativeElement, 'Registrar el pago')).toBeUndefined()
    expect(textoDe(f)).toContain('no puede autorizar su pago')
  })

  it('límite · una factura vencida con saldo se marca y el filtro «Vencida con saldo» la aísla en la página', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura(), factura({ facturaProveedorId: 'f-2', numeroFactura: 'F-0010', fechaVencimiento: '2020-01-01' })] } })
    const chips = Array.from(f.nativeElement.querySelectorAll('tbody ap-chip-estado') as NodeListOf<HTMLElement>).map((c) => c.textContent?.trim())
    expect(chips.filter((c) => c === 'Vencida')).toHaveLength(1)
    ;(f.componentInstance as unknown as { listado: { filtrar(n: string, v: string): void } }).listado.filtrar('vencimiento', 'VENCIDA')
    await esperar(f)
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1)
    expect(textoDe(f)).toContain('F-0010')
  })

  it('error · 403: alerta traducida, sin cuerpo del backend', async () => {
    http = configurarListado([])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { status: 403, cuerpo: { mensaje: 'interno', trazaId: 'tz-403' } })
    expect(f.nativeElement.querySelector('ap-estado-de-error[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(textoDe(f)).not.toContain('interno')
  })
})
