import { describe, expect, it } from 'vitest'
import { aCentavos, calcularDesviacion, deCentavos, sobreEjecutada, type PartidaPresupuestaria } from './cu101-presupuestos'
import { admiteCobro, cobroExcedeElSaldo, motivoParaNoCobrar, saldoPendienteDe, type CuentaPorCobrar } from './cu104-cobros'
import { agotado, motivoParaNoDepreciar, type ActivoFijo } from './cu105-activos'

function partida(presupuestado: string, ejecutado: string): PartidaPresupuestaria {
  return {
    partidaId: 'pp-1',
    presupuestoId: 'p-1',
    presupuestoNombre: 'Marketing 2026',
    centroCostoNombre: 'Marketing',
    cuentaContableNombre: 'Publicidad',
    periodoNombre: '3/2026',
    estado: 'APROBADO',
    montoPresupuestado: { monto: presupuestado, moneda: 'BOB' },
    montoEjecutado: { monto: ejecutado, moneda: 'BOB' },
  }
}

describe('CU-101 · la desviación es un hecho, no un rechazo', () => {
  it('ejecutar de más da desviación positiva y marca la partida', () => {
    const p = partida('1000.00', '1250.75')
    expect(calcularDesviacion(p)).toEqual({ monto: '250.75', moneda: 'BOB' })
    expect(sobreEjecutada(p)).toBe(true)
  })

  it('ejecutar de menos da desviación negativa y no marca nada', () => {
    const p = partida('1000.00', '400.25')
    expect(calcularDesviacion(p)).toEqual({ monto: '-599.75', moneda: 'BOB' })
    expect(sobreEjecutada(p)).toBe(false)
  })

  it('ejecutar exactamente lo presupuestado no es sobre-ejecución', () => {
    expect(sobreEjecutada(partida('1000.00', '1000.00'))).toBe(false)
  })
})

describe('Importes en centavos enteros: ningún doble toca un monto (invariante 4)', () => {
  it('ida y vuelta de la cadena del contrato', () => {
    for (const monto of ['0.00', '0.01', '1240.55', '-599.75', '999999.99']) {
      expect(deCentavos(aCentavos(monto))).toBe(monto)
    }
  })

  it('0.1 + 0.2 en centavos da exactamente 0.30', () => {
    expect(deCentavos(aCentavos('0.10') + aCentavos('0.20'))).toBe('0.30')
  })
})

function cuenta(parcial: Partial<CuentaPorCobrar> = {}): CuentaPorCobrar {
  return {
    cuentaPorCobrarId: 'c-1',
    origenTipo: 'FACTURA_PUBLICIDAD',
    origenId: 'fp-1',
    terceroRazonSocial: 'Anunciante SA',
    fechaVencimiento: '2026-03-31',
    estado: 'PENDIENTE',
    monto: { monto: '500.00', moneda: 'BOB' },
    cobrado: { monto: '0.00', moneda: 'BOB' },
    saldoPendiente: { monto: '500.00', moneda: 'BOB' },
    ...parcial,
  }
}

describe('CU-104 · cobrar una cuenta por cobrar', () => {
  it('una cuenta INCOBRABLE no ofrece cobro (AP-CU104-03)', () => {
    expect(admiteCobro(cuenta({ estado: 'INCOBRABLE' }))).toBe(false)
    expect(motivoParaNoCobrar(cuenta({ estado: 'INCOBRABLE' }), true)).toContain('incobrable')
  })

  it('cobrar 800 sobre un saldo de 500 se bloquea antes de pedirlo (AP-CU104-02)', () => {
    expect(cobroExcedeElSaldo(cuenta(), '800.00')).toBe(true)
  })

  it('el saldo pendiente es monto menos cobrado, en centavos enteros', () => {
    expect(saldoPendienteDe(cuenta({ cobrado: { monto: '120.30', moneda: 'BOB' } }))).toEqual({ monto: '379.70', moneda: 'BOB' })
  })

  it('sin CONTABILIDAD_ERP_COBRAR el motivo lo nombra', () => {
    expect(motivoParaNoCobrar(cuenta(), false)).toContain('CONTABILIDAD_ERP_COBRAR')
  })
})

function activo(parcial: Partial<ActivoFijo> = {}): ActivoFijo {
  return {
    activoFijoId: 'a-1',
    codigo: 'AF-001',
    descripcion: 'Servidor de oficina',
    categoriaNombre: 'Equipos de computación',
    vidaUtilMeses: 12,
    fechaAdquisicion: '2026-01-15',
    estado: 'ACTIVO',
    costoAdquisicion: { monto: '12000.00', moneda: 'BOB' },
    valorResidual: { monto: '0.00', moneda: 'BOB' },
    depreciacionAcumulada: { monto: '1000.00', moneda: 'BOB' },
    valorEnLibros: { monto: '11000.00', moneda: 'BOB' },
    corridoEnElPeriodoVigente: false,
    ...parcial,
  }
}

describe('CU-105 · cuándo un activo deja de depreciarse', () => {
  it('agotado cuando el valor en libros llegó al residual (AP-CU105-02)', () => {
    expect(agotado(activo({ valorEnLibros: { monto: '0.00', moneda: 'BOB' } }))).toBe(true)
    expect(motivoParaNoDepreciar(activo({ valorEnLibros: { monto: '0.00', moneda: 'BOB' } }), true)).toContain('residual')
  })

  it('ya corrido en el período no se vuelve a correr (AP-CU105-01)', () => {
    expect(motivoParaNoDepreciar(activo({ corridoEnElPeriodoVigente: true }), true)).toContain('Ya tiene cuota')
  })

  it('dado de baja no se deprecia (AP-CU105-03)', () => {
    expect(motivoParaNoDepreciar(activo({ estado: 'DADO_DE_BAJA' }), true)).toContain('fuera de uso')
  })

  it('en uso, no agotado y sin cuota del mes: se puede depreciar', () => {
    expect(motivoParaNoDepreciar(activo(), true)).toBeNull()
  })
})
