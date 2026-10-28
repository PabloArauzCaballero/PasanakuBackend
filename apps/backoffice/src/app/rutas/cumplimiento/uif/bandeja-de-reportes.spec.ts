import { describe, expect, it } from 'vitest'
import type { ReporteRegulatorioEnLista } from 'clientes/angular/cumplimiento'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { BandejaDeReportes } from './bandeja-de-reportes'

const URL = `${GW}/uif/reportes`
const REPORTE = {
  reporteId: '3f2a1b00-0000-4000-8000-000000000001',
  catalogoReporteId: '3f2a1b00-0000-4000-8000-0000000000c1',
  periodo: '2026-09',
  fechaCorte: '2026-09-30',
  fechaLimite: '2026-10-10',
  estado: 'GENERADO',
  cantidadRegistros: 12,
  reporteEnCero: false,
  montoTotal: '15000.00',
}

describe('BandejaDeReportes · CU-43 listarReportesRegulatorios', () => {
  it('correcto: filas del contrato; el estado viaja al servidor y un filtro nuevo vuelve a la primera página (sin antesDe)', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['CUMPLIMIENTO_REPORTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [REPORTE], proximoAntesDe: '2026-09-30', proximoAntesDeId: REPORTE.reporteId })
    await asentar(fixture)
    expect(filasDe<ReporteRegulatorioEnLista>(fixture)[0]?.periodo).toBe('2026-09')
    // El monto viaja sin moneda: la pantalla lo declara en vez de inventarle una.
    expect(textoDe(fixture)).toContain('Monto total (moneda sin especificar)')

    bandejaDe<Bandeja<ReporteRegulatorioEnLista>>(fixture).filtrar('estados', 'APROBADO')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['APROBADO'])
    expect(pedido.request.params.has('antesDe')).toBe(false)
    pedido.flush({ items: [] })
    http.verify()
  })

  it('límite: vacío con su porqué', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay reportes en estos estados.')
  })

  it('inválido: 403 → «sin acceso»', async () => {
    const uno = montar(BandejaDeReportes, ['CUMPLIMIENTO_REPORTAR'])
    rechazar(uno.http.expectOne((r) => r.url === URL), 403)
    await asentar(uno.fixture)
    expect(textoDe(uno.fixture)).toContain('No tenés acceso a esta bandeja.')
  })

  it('inválido: 422 AP-CU43-05 → filtro inválido', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['CUMPLIMIENTO_REPORTAR'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU43-05')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
  })

  it('con solo AUDITORIA_LEER se lee pero no se prepara ni se aprueba', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    http.expectOne((r) => r.url === URL).flush({ items: [REPORTE] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('Preparar un reporte')
    expect(fixture.nativeElement.querySelector('ap-aprobar-y-enviar')).toBeNull()
  })

  it('con CUMPLIMIENTO_REPORTAR, la acción de fila «Aprobar» abre la confirmación con la consecuencia exacta del período elegido', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['CUMPLIMIENTO_REPORTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [REPORTE] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Preparar un reporte')
    ;(fixture.nativeElement.querySelector('[aria-label="Aprobar y registrar envío del período 2026-09"]') as HTMLButtonElement).click()
    fixture.detectChanges()
    const texto = textoDe(fixture)
    expect(texto).toContain('Aprobar y registrar el envío de 2026-09')
    expect(texto).toContain('El sistema no transmite el archivo')
  })

  it('un reporte que no está GENERADO (p. ej. ENVIADO) no ofrece aprobar, y nada ofrece Editar ni Eliminar', async () => {
    const { fixture, http } = montar(BandejaDeReportes, ['CUMPLIMIENTO_REPORTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [{ ...REPORTE, estado: 'ENVIADO' }] })
    await asentar(fixture)
    expect(fixture.nativeElement.querySelector('[aria-label^="Aprobar y registrar envío"]')).toBeNull()
    expect(textoDe(fixture)).not.toMatch(/Editar|Eliminar/)
    expect(textoDe(fixture)).toContain('Enviado')
  })
})
