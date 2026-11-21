import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { asentar, GW, montar, rechazar } from '../compartido/pruebas.apoyo'
import { BandejaDeBloqueos } from './bandeja-de-bloqueos'

/** WCAG 2.2 AA automático (axe) sobre la bandeja de bloqueos (CU-17) en sus estados. Datos sintéticos. */
const URL = `${GW}/billetera/bloqueos`
const VIGENTE = {
  bloqueoId: '0b10c0e0-0000-4000-8000-000000000001',
  cuentaBilleteraId: '0b10c0e0-0000-4000-8000-0000000000c1',
  autoridad: 'JUZGADO',
  tipoOrden: 'EMBARGO',
  alcance: 'PARCIAL',
  estado: 'VIGENTE',
  montoBloqueado: { monto: '1240.50', moneda: 'BOB' },
  recibidoEn: '2026-11-20T14:00:00Z',
  puedeLevantar: true,
}

describe('bandeja de bloqueos · accesibilidad', () => {
  it('con datos, la acción «Levantar» y su diálogo abierto, sin violaciones', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_LEVANTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [VIGENTE, { ...VIGENTE, bloqueoId: '0b10c0e0-0000-4000-8000-000000000002', alcance: 'TOTAL', montoBloqueado: undefined, puedeLevantar: false }] })
    await asentar(fixture)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
    fixture.componentInstance['levantar'](VIGENTE as never)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('vacía, sin violaciones', async () => {
    const vacia = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    vacia.http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(vacia.fixture)
    expect(await axe(vacia.fixture.nativeElement)).toHaveNoViolations()
  })

  it('en error (422 AP-CU17-06), sin violaciones', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU17-06')
    await asentar(fixture)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
