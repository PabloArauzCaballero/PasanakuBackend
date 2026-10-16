import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { BandejaDeAlertas } from '../alertas/bandeja-de-alertas'
import { BandejaDeCasos } from '../casos/bandeja-de-casos'
import { PantallaDeContinuidad } from '../colas/pantalla-de-continuidad'
import { ContenedorDeAlerta } from '../alertas/contenedor-de-alerta'
import { RegistrarRequerimiento } from '../uif/registrar-requerimiento'
import { asentar, GW, montar, rechazar } from './pruebas.apoyo'

/** WCAG 2.2 AA automático (axe) sobre las bandejas y formularios nuevos del lote L2, en sus estados distintos. */
describe('bandejas de cumplimiento · accesibilidad', () => {
  it('bandeja de alertas con datos y selección, sin violaciones', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS', 'CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === `${GW}/cumplimiento/alertas`).flush({ items: [{ alertaId: 'a-1', reglaCodigo: 'R-1', tipologia: 'T', severidad: 'ALTA', estado: 'ABIERTA', detectadaEn: '2026-10-10T14:00:00Z' }] })
    await asentar(fixture)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('bandeja de casos en error (422) con el diálogo de decidir abierto, sin violaciones', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    rechazar(http.expectOne((r) => r.url === `${GW}/cumplimiento/casos`), 422, 'AP-CU44-05')
    await asentar(fixture)
    fixture.componentInstance['decidir']({ casoId: 'c-1', codigo: 'LFT-1' } as never)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('cola de continuidad sin permiso (403), sin violaciones', async () => {
    const { fixture, http } = montar(PantallaDeContinuidad, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === `${GW}/cumplimiento/continuidad/pruebas`), 403)
    await asentar(fixture)
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('formulario de requerimiento con errores por campo, sin violaciones', async () => {
    const { fixture } = montar(RegistrarRequerimiento, ['AUDITORIA_LEER'], 'OFICIAL_CUMPLIMIENTO')
    fixture.componentInstance.abierto.set(true)
    await fixture.componentInstance.enviar()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('indisponibilidad honesta (CU-97), sin violaciones', async () => {
    const { fixture } = montar(ContenedorDeAlerta, [], 'OPERADOR', { alertaId: 'x' })
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
