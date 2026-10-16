import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { configurar, ejercicio, mes, montar, periodos, vigente } from './periodo.apoyo'

describe('PantallaDePeriodo · accesibilidad', () => {
  it('sin violaciones serias con un mes cerrado y otro abierto a la vez, el período de hoy y el selector', async () => {
    const http = configurar()
    const fixture = await montar(http, {
      permisos: ['CONTABILIDAD_ERP_CERRAR', 'CONTABILIDAD_ERP_REPORTES'],
      vigente: vigente(2, 'e-2026'),
      ejercicios: { items: [ejercicio(2026, 'e-2026'), ejercicio(2025, 'e-2025', 'CERRADO')], proximoAntesDeAnio: 2025 },
      periodos: periodos('e-2026', [mes(1, 'CERRADO'), mes(2, 'ABIERTO')]),
    })
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones serias cuando no hay período para hoy (AP-CU100-04)', async () => {
    const http = configurar()
    const fixture = await montar(http, { vigente: { status: 422, cuerpo: { codigo: 'AP-CU100-04', mensaje: 'x', trazaId: 't' } } })
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
