import { HttpTestingController } from '@angular/common/http/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import type { CuentaPorCobrar } from '../dominio/cu104-cobros'
import { configurarListado, GW, montarListado, textoDe } from '../listado/listado.apoyo'
import { PantallaDeCobros } from './pantalla-de-cobros'

/** H1.S3.M2 · CU-104 sobre el listado por cursor real. Datos sintéticos con la forma de `CuentaPorCobrarEnLista`. */
const URL = `${GW}/erp/cuentas-por-cobrar?limite=50`

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

describe('PantallaDeCobros · CU-104 por cursor', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurarListado(['CONTABILIDAD_ERP_COBRAR'])))

  it('correcto · el origen se nombra en español y se ofrece cobrar', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { cuerpo: { items: [cuenta()] } })
    expect(textoDe(f)).toContain('Factura de publicidad')
    expect(textoDe(f)).toContain('Registrar el cobro')
  })

  it('límite · una cuenta sin tercero (nulable en el contrato) lo dice; una incobrable no ofrece cobrar', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, {
      cuerpo: { items: [cuenta({ terceroComercialId: null, terceroRazonSocial: null, estado: 'INCOBRABLE' })] },
    })
    expect(textoDe(f)).toContain('Sin tercero registrado')
    expect(textoDe(f)).not.toContain('Registrar el cobro')
    expect(textoDe(f)).toContain('Cuenta declarada incobrable')
  })

  it('inválido · 422 AP-CU104-04', async () => {
    const f = await montarListado(http, PantallaDeCobros, URL, { status: 422, cuerpo: { codigo: 'AP-CU104-04', mensaje: 'x', trazaId: 'tz' } })
    expect(f.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El filtro de la lista no es válido')
  })
})
