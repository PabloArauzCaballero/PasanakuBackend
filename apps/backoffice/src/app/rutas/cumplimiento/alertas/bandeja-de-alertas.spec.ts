import { describe, expect, it } from 'vitest'
import type { AlertaEnBandeja } from 'clientes/angular/cumplimiento'
import { asentar, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
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

describe('BandejaDeAlertas · CU-44 listarBandejaDeAlertas', () => {
  it('correcto: pide la primera página sin cursor y entrega las filas del contrato a la tabla', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.method).toBe('GET')
    expect(pedido.request.params.get('limite')).toBe('25')
    expect(pedido.request.params.has('despuesDeFecha')).toBe(false)
    pedido.flush({ items: [ALERTA], proximoFecha: '2026-10-10T14:00:00Z', proximoId: ALERTA.alertaId })
    await asentar(fixture)
    expect(filasDe<AlertaEnBandeja>(fixture).map((a) => a.alertaId)).toEqual([ALERTA.alertaId])
    expect(textoDe(fixture)).not.toContain('No hay alertas')
    http.verify()
  })

  it('correcto: los filtros viajan como el contrato los pide (explode: false → una clave con comas)', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    fixture.componentInstance['estados'].set(['ABIERTA', 'ESCALADA'])
    fixture.componentInstance['severidades'].set(['CRITICA'])
    fixture.detectChanges()
    await asentar(fixture)
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['ABIERTA,ESCALADA'])
    expect(pedido.request.params.get('severidades')).toBe('CRITICA')
    pedido.flush({ items: [] })
    await asentar(fixture)
    // Vacío POR FILTRO: lo dice y ofrece quitarlo.
    expect(textoDe(fixture)).toContain('Quitar los filtros')
    http.verify()
  })

  it('límite: bandeja vacía muestra el porqué, no una tabla muda', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [], proximoFecha: null, proximoId: null })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay alertas en estos estados.')
    http.verify()
  })

  it('límite: última página (próximo nulo) no inventa una página más', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA], proximoFecha: null, proximoId: null })
    await asentar(fixture)
    const tabla = fixture.debugElement.query((d) => d.name === 'ap-tabla-de-datos-virtualizada').componentInstance
    expect(tabla.total()).toBe(1)
    http.verify()
  })

  it('inválido: 403 muestra «sin acceso» y oculta la tabla', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
    expect((fixture.nativeElement as HTMLElement).querySelector('[hidden] ap-tabla-de-datos-virtualizada')).not.toBeNull()
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
    ;(fixture.nativeElement.querySelector('ap-estado-de-bandeja ap-boton button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await asentar(fixture)
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('No pudimos cargar')
    http.verify()
  })

  it('sin CUMPLIMIENTO_CASOS no se ofrece abrir un caso (la tabla no es seleccionable)', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('Abrir caso')
    expect(fixture.nativeElement.querySelector('ap-abrir-caso')).toBeNull()
    http.verify()
  })

  it('con CUMPLIMIENTO_CASOS, el botón de abrir caso espera una selección de 1 a 50 alertas', async () => {
    const { fixture, http } = montar(BandejaDeAlertas, ['CUMPLIMIENTO_ALERTAS', 'CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [ALERTA] })
    await asentar(fixture)
    const boton = () => fixture.nativeElement.querySelector('header ap-boton button') as HTMLButtonElement
    expect(boton().disabled).toBe(true)
    fixture.componentInstance['elegidas'].set([ALERTA.alertaId])
    fixture.detectChanges()
    expect(boton().disabled).toBe(false)
    expect(boton().textContent).toContain('Abrir caso con 1 alerta')
    fixture.componentInstance['elegidas'].set(Array.from({ length: 51 }, (_, i) => `a-${i}`))
    fixture.detectChanges()
    expect(boton().disabled).toBe(true)
    http.verify()
  })
})
