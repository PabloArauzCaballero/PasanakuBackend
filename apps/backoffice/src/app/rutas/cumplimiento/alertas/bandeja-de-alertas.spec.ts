import { By } from '@angular/platform-browser'
import { describe, expect, it } from 'vitest'
import { aCsv, aJson } from '@aportaya/ui/exportar-listado/exportar'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import type { AlertaEnBandeja } from 'clientes/angular/cumplimiento'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, boton, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { BandejaDeAlertas } from './bandeja-de-alertas'

const URL = `${GW}/cumplimiento/alertas`
const ALERTA = {
  alertaId: '0b9b2a61-0000-4000-8000-000000000001',
  reglaCodigo: 'R-FRAC-01',
  tipologia: 'Fraccionamiento',
  severidad: 'ALTA',
  estado: 'ABIERTA',
  detectadaEn: '2026-10-10T14:00:00Z',
  asignadaA: null,
  casoId: null,
  cerradaEn: null,
}
const OTRA = { ...ALERTA, alertaId: '0b9b2a61-0000-4000-8000-000000000002', reglaCodigo: 'R-PEP-02', tipologia: 'Persona expuesta', severidad: 'CRITICA', casoId: 'c-9' }
const bandeja = (f: Parameters<typeof bandejaDe>[0]) => bandejaDe<Bandeja<AlertaEnBandeja>>(f)

describe('BandejaDeAlertas · CU-44 listarBandejaDeAlertas', () => {
  it('correcto: pide la primera página sin cursor y con 25 filas, y entrega las filas del contrato a la tabla', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.method).toBe('GET')
    expect(pedido.request.params.get('limite')).toBe('25')
    expect(pedido.request.params.has('despuesDeFecha')).toBe(false)
    pedido.flush({ items: [ALERTA], proximoFecha: '2026-10-10T14:00:00Z', proximoId: ALERTA.alertaId })
    await asentar(fixture)
    expect(filasDe<AlertaEnBandeja>(fixture).map((a) => a.alertaId)).toEqual([ALERTA.alertaId])
    expect(textoDe(fixture)).toContain('1 alertas en la página 1 · hay más')
    http.verify()
  })

  it('correcto: estado y severidad viajan al servidor (una clave cada uno) y vuelven a la página 1', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    bandeja(fixture).filtrar('estados', 'ESCALADA')
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    bandeja(fixture).filtrar('severidades', 'CRITICA')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['ESCALADA'])
    expect(pedido.request.params.get('severidades')).toBe('CRITICA')
    pedido.flush({ items: [] })
    await asentar(fixture)
    // Vacío POR FILTRO: lo dice y ofrece quitarlo, que vuelve a pedir sin filtros.
    expect(textoDe(fixture)).toContain('Ningún registro coincide')
    boton(fixture, 'Quitar filtros')!.click()
    const sinFiltro = http.expectOne((r) => r.url === URL)
    expect(sinFiltro.request.params.has('estados')).toBe(false)
    sinFiltro.flush({ items: [ALERTA] })
    http.verify()
  })

  it('correcto: el buscador busca por regla, tipología y severidad en la página cargada, sin pedir otra vez', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA, OTRA] })
    await asentar(fixture)
    bandeja(fixture).buscar('critica')
    fixture.detectChanges()
    expect(filasDe<AlertaEnBandeja>(fixture).map((a) => a.reglaCodigo)).toEqual(['R-PEP-02'])
    bandeja(fixture).buscar('fraccionamiento')
    fixture.detectChanges()
    expect(filasDe<AlertaEnBandeja>(fixture).map((a) => a.reglaCodigo)).toEqual(['R-FRAC-01'])
    http.verify()
  })

  it('sin el permiso de exportación del recurso no se ofrece la descarga (regla 90.2)', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    expect(fixture.debugElement.query(By.directive(ExportarListado))).toBeNull()
  })

  it('correcto: CSV y JSON bajan lo filtrado con los valores como se ven y sin identificadores internos', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS', 'exportar:cumplimiento-alertas'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA, OTRA] })
    await asentar(fixture)
    bandeja(fixture).buscar('R-PEP')
    fixture.detectChanges()
    const exportar = fixture.debugElement.query(By.directive(ExportarListado)).componentInstance as ExportarListado<AlertaEnBandeja>
    const csv = aCsv(exportar.columnas(), exportar.filas())
    expect(csv).toContain('Regla,Tipología,Severidad,Estado,Detectada,Caso')
    expect(csv).toContain('R-PEP-02,Persona expuesta,Crítica,Abierta')
    expect(csv).not.toContain('R-FRAC-01')
    expect(csv).not.toContain(OTRA.alertaId)
    expect(JSON.parse(aJson(exportar.columnas(), exportar.filas()))).toEqual([expect.objectContaining({ reglaCodigo: 'R-PEP-02', severidad: 'Crítica', caso: 'En un caso' })])
    http.verify()
  })

  it('límite: bandeja vacía muestra el porqué, no una tabla muda', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [], proximoFecha: null, proximoId: null })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay alertas en estos estados.')
    expect(filasDe(fixture)).toEqual([])
    http.verify()
  })

  it('límite: última página (próximo nulo) no ofrece Siguiente', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA], proximoFecha: null, proximoId: null })
    await asentar(fixture)
    expect(boton(fixture, 'Siguiente')!.disabled).toBe(true)
    expect(textoDe(fixture)).not.toContain('hay más')
    http.verify()
  })

  it('inválido: 403 muestra «sin acceso» y no dibuja tabla, buscador ni descarga', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
    expect(fixture.nativeElement.querySelector('ap-tabla-de-datos')).toBeNull()
    expect(fixture.nativeElement.querySelector('ap-exportar-listado')).toBeNull()
    http.verify()
  })

  it('inválido: 422 AP-CU44-05 muestra un error accionable con reintento, sin el texto crudo del servidor', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU44-05')
    await asentar(fixture)
    const texto = textoDe(fixture)
    expect(texto).toContain('El filtro de la lista no es válido')
    expect(texto).toContain('traza-123')
    expect(texto).not.toContain('texto del servidor')
    boton(fixture, 'Volver a intentar')!.click()
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('No se pudo cargar')
    http.verify()
  })

  it('sin CUMPLIMIENTO_CASOS no se ofrece abrir un caso (ni botón, ni selección, ni acción de fila)', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('Abrir caso')
    expect(fixture.nativeElement.querySelector('ap-abrir-caso')).toBeNull()
    expect(fixture.nativeElement.querySelector('ap-casilla')).toBeNull()
    http.verify()
  })

  it('con CUMPLIMIENTO_CASOS, el botón de abrir caso espera una selección de 1 a 50 alertas', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS', 'CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    const principal = () => fixture.nativeElement.querySelector('ap-cabecera-de-pagina ap-boton button') as HTMLButtonElement
    expect(principal().disabled).toBe(true)
    fixture.componentInstance['elegidas'].set([ALERTA.alertaId])
    fixture.detectChanges()
    expect(principal().disabled).toBe(false)
    expect(principal().textContent).toContain('Abrir caso con 1 alerta')
    fixture.componentInstance['elegidas'].set(Array.from({ length: 51 }, (_, i) => `a-${i}`))
    fixture.detectChanges()
    expect(principal().disabled).toBe(true)
    http.verify()
  })

  it('la acción de fila «Abrir caso» abre el diálogo con esa sola alerta; una alerta ya en un caso no la ofrece', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS', 'CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA, OTRA] })
    await asentar(fixture)
    expect(fixture.nativeElement.querySelector(`[aria-label="Abrir caso con la alerta R-PEP-02"]`)).toBeNull()
    ;(fixture.nativeElement.querySelector(`[aria-label="Abrir caso con la alerta R-FRAC-01"]`) as HTMLButtonElement).click()
    fixture.detectChanges()
    expect(fixture.componentInstance['paraAbrir']()).toEqual([ALERTA.alertaId])
    expect(textoDe(fixture)).toContain('Se agrupan 1 alerta en un caso nuevo')
    http.verify()
  })
})
