import { describe, expect, it } from 'vitest'
import { paginarEnElAdaptador, filtroPorEstado } from './contrato-erp'
import { admitePago, mismoAprobadorYPagador, motivoParaNoPagar, pagoExcedeElSaldo, vencida, type FacturaDeProveedor } from './cu103-facturas'

function factura(parcial: Partial<FacturaDeProveedor> = {}): FacturaDeProveedor {
  return {
    facturaProveedorId: 'f-1',
    numeroFactura: 'F-0001',
    terceroRazonSocial: 'Imprenta Illimani SRL',
    ordenCompraId: 'o-1',
    fechaEmision: '2026-03-01',
    fechaVencimiento: '2026-03-31',
    estado: 'APROBADA',
    monto: { monto: '1200.00', moneda: 'BOB' },
    montoPagado: { monto: '0.00', moneda: 'BOB' },
    saldoPendiente: { monto: '1200.00', moneda: 'BOB' },
    aprobadaPor: 'op-ana',
    aprobadaPorLaSesion: false,
    asientoContableId: null,
    ...parcial,
  }
}

describe('CU-103 · segregación de funciones sobre el pago (R-CTB-05 · AP-CU103-02)', () => {
  it('quien aprobó la factura no ve el botón de pagar: ve el motivo', () => {
    const motivo = motivoParaNoPagar(factura({ aprobadaPorLaSesion: true }), true)
    expect(motivo).toContain('no puede autorizar su pago')
  })

  it('sin CONTABILIDAD_ERP_PAGAR tampoco se ofrece pagar, y el motivo lo nombra', () => {
    expect(motivoParaNoPagar(factura(), false)).toContain('CONTABILIDAD_ERP_PAGAR')
  })

  it('con permiso y sin haber aprobado, se puede pagar', () => {
    expect(motivoParaNoPagar(factura(), true)).toBeNull()
    expect(mismoAprobadorYPagador(factura())).toBe(false)
  })

  it('una factura anulada no admite pago aunque el operador tenga todo', () => {
    expect(motivoParaNoPagar(factura({ estado: 'ANULADA' }), true)).toContain('anulada')
  })

  it('una factura sin saldo pendiente no admite pago', () => {
    expect(admitePago(factura({ estado: 'PAGADA', saldoPendiente: { monto: '0.00', moneda: 'BOB' } }))).toBe(false)
  })
})

describe('CU-103 · el pago no excede el saldo (AP-CU103-04)', () => {
  it('pagar más que el saldo se bloquea antes de pedirlo', () => {
    expect(pagoExcedeElSaldo(factura(), '1200.01')).toBe(true)
  })

  it('pagar exactamente el saldo es válido', () => {
    expect(pagoExcedeElSaldo(factura(), '1200.00')).toBe(false)
  })

  it('un pago parcial es válido', () => {
    expect(pagoExcedeElSaldo(factura(), '0.01')).toBe(false)
  })
})

describe('CU-103 · la cola de cuentas por pagar', () => {
  it('vencida es la que pasó su fecha y todavía tiene saldo', () => {
    expect(vencida(factura(), '2026-04-01T09:00:00Z')).toBe(true)
    expect(vencida(factura({ saldoPendiente: { monto: '0.00', moneda: 'BOB' } }), '2026-04-01T09:00:00Z')).toBe(false)
  })

  it('el adaptador filtra por estado y pagina sin que la tabla sepa de memoria', () => {
    const todas = [factura({ facturaProveedorId: 'a', estado: 'APROBADA' }), factura({ facturaProveedorId: 'b', estado: 'PAGADA' })]
    const pagina = paginarEnElAdaptador(todas, { pagina: 1, tamano: 50, orden: null, filtros: { estado: 'PAGADA' } }, filtroPorEstado)
    expect(pagina.total).toBe(1)
    expect(pagina.filas[0]?.facturaProveedorId).toBe('b')
  })

  it('ordena por importe comparando centavos, no cadenas: 90 va antes que 1200', () => {
    const todas = [factura({ facturaProveedorId: 'caro', monto: { monto: '1200.00', moneda: 'BOB' } }), factura({ facturaProveedorId: 'barato', monto: { monto: '90.00', moneda: 'BOB' } })]
    const pagina = paginarEnElAdaptador(todas, { pagina: 1, tamano: 50, orden: { clave: 'monto', sentido: 'asc' }, filtros: {} }, filtroPorEstado)
    expect(pagina.filas.map((f) => f.facturaProveedorId)).toEqual(['barato', 'caro'])
  })
})
