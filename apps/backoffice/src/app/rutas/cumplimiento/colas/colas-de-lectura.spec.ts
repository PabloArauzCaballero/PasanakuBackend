import { describe, expect, it } from 'vitest'
import { By } from '@angular/platform-browser'
import { aCsv } from '@aportaya/ui/exportar-listado/exportar'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { PantallaDeActas } from './pantalla-de-actas'
import { PantallaDeEventos } from './pantalla-de-eventos'
import { PantallaDeIncidentes } from './pantalla-de-incidentes'

const ACTA = { actaId: 'ac-1', comiteGobiernoId: 'cg-1', numero: 'ACTA-2026-03', fecha: '2026-09-15', cumpleQuorum: true, hashDocumento: 'f'.repeat(64) }
const DINERO = (monto: string) => ({ monto, moneda: 'BOB' })
const EVENTO = {
  eventoId: 'ev-1', codigo: 'RO-2026-01', categoria: 'FRAUDE_EXTERNO', factor: 'PERSONAS', lineaNegocio: 'PASANAKU',
  fechaOcurrencia: '2026-09-01T12:00:00Z', fechaDeteccion: '2026-09-02T12:00:00Z',
  perdidaBruta: DINERO('1500.00'), recuperacion: DINERO('500.00'), perdidaNeta: DINERO('1000.00'), estado: 'EN_ANALISIS', reportadoCentral: false,
}
const INCIDENTE = {
  incidenteId: 'in-1', codigo: 'INC-2026-04', tipo: 'ACCESO_NO_AUTORIZADO', severidad: 'ALTA', datosPersonalesAfectados: true, usuariosAfectados: 3,
  detectadoEn: '2026-10-01T12:00:00Z', plazoReporte: '2026-10-02T12:00:00Z', estado: 'EN_CONTENCION',
}

describe('PantallaDeActas · CU-94 listarActasDeComite', () => {
  const URL = `${GW}/cumplimiento/comites/actas`
  it('correcto: lista sin filtros de estado (el contrato no los tiene) y sin ofrecer sesionar', async () => {
    const { fixture, http } = montar(PantallaDeActas, ['AUDITORIA_LEER'])
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.has('estados')).toBe(false)
    pedido.flush({ items: [ACTA], proximoAntesDe: '2026-09-15', proximoAntesDeId: 'ac-1' })
    await asentar(fixture)
    expect(filasDe<typeof ACTA>(fixture)[0]?.numero).toBe('ACTA-2026-03')
    expect(textoDe(fixture)).toContain('Sesionar el comité desde acá no está habilitado')
    expect(fixture.nativeElement.querySelector('ap-chips-de-filtro')).toBeNull()
    http.verify()
  })
  it('límite: vacío', async () => {
    const { fixture, http } = montar(PantallaDeActas, ['AUDITORIA_LEER'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Todavía no hay actas de comité registradas.')
  })
  it('inválido: 403 → «sin acceso»', async () => {
    const { fixture, http } = montar(PantallaDeActas, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
  })
  it('inválido: 422 AP-CU94-07', async () => {
    const uno = montar(PantallaDeActas, ['AUDITORIA_LEER'])
    rechazar(uno.http.expectOne((r) => r.url === URL), 422, 'AP-CU94-07')
    await asentar(uno.fixture)
    expect(textoDe(uno.fixture)).toContain('El filtro de la lista no es válido')
  })
})

describe('PantallaDeEventos · CU-54 listarEventosDeRiesgo', () => {
  const URL = `${GW}/cumplimiento/riesgos/eventos`
  it('correcto: pérdidas con su moneda vía ap-monto (también en el archivo); el estado viaja al servidor', async () => {
    const { fixture, http } = montar(PantallaDeEventos, ['AUDITORIA_LEER'], 'RESPONSABLE_RIESGOS')
    http.expectOne((r) => r.url === URL).flush({ items: [EVENTO] })
    await asentar(fixture)
    const pantalla = fixture.componentInstance as unknown as { celda: (e: typeof EVENTO, c: string) => unknown }
    expect(pantalla.celda(EVENTO, 'perdidaNeta')).toEqual({ monto: '1000.00', moneda: 'BOB' })
    expect(pantalla.celda(EVENTO, 'estado')).toBe('En análisis')
    const exportar = fixture.debugElement.query(By.directive(ExportarListado)).componentInstance as ExportarListado<typeof EVENTO>
    expect(aCsv(exportar.columnas(), exportar.filas())).toContain('1000.00 BOB')
    bandejaDe<Bandeja<typeof EVENTO>>(fixture).filtrar('estados', 'CERRADO')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['CERRADO'])
    pedido.flush({ items: [] })
    http.verify()
  })
  it('inválido: 403 → sin acceso', async () => {
    const { fixture, http } = montar(PantallaDeEventos, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
  })
  it('inválido: 422 AP-CU54-04', async () => {
    const { fixture, http } = montar(PantallaDeEventos, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU54-04')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
  })
})

describe('PantallaDeIncidentes · CU-55 listarIncidentesDeSeguridad', () => {
  const URL = `${GW}/cumplimiento/seguridad/incidentes`
  it('correcto: plazos guardados; sin reportar se dice', async () => {
    const { fixture, http } = montar(PantallaDeIncidentes, ['AUDITORIA_LEER'], 'RESPONSABLE_SEGURIDAD')
    http.expectOne((r) => r.url === URL).flush({ items: [INCIDENTE], proximoAntesDe: INCIDENTE.detectadoEn, proximoAntesDeId: 'in-1' })
    await asentar(fixture)
    expect(filasDe<typeof INCIDENTE>(fixture)).toHaveLength(1)
    const pantalla = fixture.componentInstance as unknown as { celda: (i: typeof INCIDENTE, c: string) => unknown }
    expect(pantalla.celda(INCIDENTE, 'reportadoAlOrganismoEn')).toBe('Sin reportar')
    expect(pantalla.celda(INCIDENTE, 'plazoReporte')).toBe('2 oct 2026, 08:00 (La Paz)')
  })
  it('límite: vacío', async () => {
    const vacio = montar(PantallaDeIncidentes, ['AUDITORIA_LEER'])
    vacio.http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(vacio.fixture)
    expect(textoDe(vacio.fixture)).toContain('No hay incidentes en estos estados.')
  })
  it('inválido: 403 → «sin acceso»', async () => {
    const { fixture, http } = montar(PantallaDeIncidentes, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
  })
  it('inválido: 422 AP-CU55-04', async () => {
    const { fixture, http } = montar(PantallaDeIncidentes, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU55-04')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
  })
})
