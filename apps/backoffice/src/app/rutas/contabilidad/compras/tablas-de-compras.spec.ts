import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import type { OrdenDeCompra } from '../dominio/cu102-compras'
import type { FacturaDeProveedor } from '../dominio/cu103-facturas'
import { configurarListado, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { TablaDeFacturas } from './tabla-de-facturas'
import { TablaDeOrdenes } from './tabla-de-ordenes'

/** H1.S3.M2 · CU-102 y CU-103 sobre los listados por cursor reales. Datos sintéticos con la forma de `erp.yaml`. */
const ORDENES = `${GW}/erp/ordenes-de-compra?limite=50`
const FACTURAS = `${GW}/erp/facturas-de-proveedor?limite=50`

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
  fechaVencimiento: '2026-10-02',
  estado: 'APROBADA',
  monto: { monto: '800.00', moneda: 'BOB' },
  montoPagado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '800.00', moneda: 'BOB' },
  aprobadaPorLaSesion: false,
  asientoContableId: null,
  ...p,
})

describe('TablaDeOrdenes · CU-102 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_COMPRAS'])))

  it('correcto · una orden recibida en parte se nombra y es facturable; sin centro de costo lo dice', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden()], proximoAntesDe: null, proximoAntesDeId: null } })
    expect(textoDe(f)).toContain('Recibida en parte')
    expect(textoDe(f)).toContain('Sin centro de costo')
    expect(f.nativeElement.querySelector('.si')?.textContent).toContain('Sí')
  })

  it('límite · una orden en borrador no es facturable y el motivo está a la vista', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [orden({ estado: 'BORRADOR' })] } })
    expect(textoDe(f)).toContain('hay que aprobarla antes de poder facturarla')
  })

  it('límite · vacío', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { cuerpo: { items: [] } })
    expect(textoDe(f)).toContain('No hay órdenes de compra con estos filtros.')
  })

  it('inválido · 422 AP-CU102-04 se dice con el texto del dominio', async () => {
    const f = await montarListado(http, TablaDeOrdenes, ORDENES, { status: 422, cuerpo: { codigo: 'AP-CU102-04', mensaje: 'x', trazaId: 'tz' } })
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })
})

describe('TablaDeFacturas · CU-103 por cursor', () => {
  let http: HttpTestingController

  it('correcto · con CONTABILIDAD_ERP_PAGAR y sin haberla aprobado, ofrece pagar', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura()] } })
    expect(textoDe(f)).toContain('Registrar el pago')
  })

  it('límite · aprobadaPorLaSesion (lo resuelve el servidor): no ofrece pagar, dice por qué (R-CTB-05)', async () => {
    http = configurarListado(['CONTABILIDAD_ERP_PAGAR'])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { cuerpo: { items: [factura({ aprobadaPorLaSesion: true })] } })
    expect(textoDe(f)).not.toContain('Registrar el pago')
    expect(textoDe(f)).toContain('no puede autorizar su pago')
  })

  it('inválido · 403: alerta traducida, sin cuerpo del backend', async () => {
    http = configurarListado([])
    const f = await montarListado(http, TablaDeFacturas, FACTURAS, { status: 403, cuerpo: { mensaje: 'interno', trazaId: 'tz-403' } })
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(textoDe(f)).not.toContain('interno')
  })
})
