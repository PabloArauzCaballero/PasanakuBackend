import { describe, expect, it } from 'vitest'
import type { CasoEnBandeja } from 'clientes/angular/cumplimiento'
import { asentar, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { BandejaDeCasos } from './bandeja-de-casos'

const URL = `${GW}/cumplimiento/casos`
const CASO = {
  casoId: '5a1e0c22-0000-4000-8000-000000000001',
  codigo: 'LFT-2026-0007',
  origen: 'ALERTA',
  estado: 'EN_ANALISIS',
  prioridad: 'ALTA',
  analistaId: '7c1d9e40-0000-4000-8000-0000000000aa',
  revisadoPor: null,
  abiertoEn: '2026-10-01T12:00:00Z',
  plazoLimite: '2026-10-31T12:00:00Z',
  vencido: false,
  decision: null,
  cerradoEn: null,
}

describe('BandejaDeCasos · CU-44 listarBandejaDeCasos', () => {
  it('correcto: primera página sin cursor; la segunda se pide con el par (despuesDePlazo, despuesDeId) que dio el servidor', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    const primero = http.expectOne((r) => r.url === URL)
    expect(primero.request.params.has('despuesDePlazo')).toBe(false)
    primero.flush({ items: [CASO], proximoPlazo: CASO.plazoLimite, proximoId: CASO.casoId })
    await asentar(fixture)
    expect(filasDe<CasoEnBandeja>(fixture).map((c) => c.codigo)).toEqual(['LFT-2026-0007'])

    const tabla = fixture.debugElement.query((d) => d.name === 'ap-tabla-de-datos-virtualizada').componentInstance
    tabla.pagina.set(2)
    fixture.detectChanges()
    await asentar(fixture)
    const segundo = http.expectOne((r) => r.url === URL)
    expect(segundo.request.params.get('despuesDePlazo')).toBe(CASO.plazoLimite)
    expect(segundo.request.params.get('despuesDeId')).toBe(CASO.casoId)
    segundo.flush({ items: [], proximoPlazo: null, proximoId: null })
    await asentar(fixture)
    http.verify()
  })

  it('correcto: filtro de estado con explode: false', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    fixture.componentInstance['estados'].set(['ABIERTO', 'CERRADO'])
    fixture.detectChanges()
    await asentar(fixture)
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.get('estados')).toBe('ABIERTO,CERRADO')
    pedido.flush({ items: [] })
    http.verify()
  })

  it('límite: vacío explica que sin filtros se ven los no cerrados', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay casos en estos estados.')
    expect(textoDe(fixture)).toContain('los que no están cerrados')
    http.verify()
  })

  it('inválido: 403 → «sin acceso», sin tabla', async () => {
    const uno = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    rechazar(uno.http.expectOne((r) => r.url === URL), 403)
    await asentar(uno.fixture)
    expect(textoDe(uno.fixture)).toContain('No tenés acceso a esta bandeja.')
  })

  it('inválido: 422 AP-CU44-05 muestra el filtro inválido con su código de seguimiento', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU44-05')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
    expect(textoDe(fixture)).toContain('traza-123')
  })

  it('decidir abre el diálogo sobre el caso elegido; nunca ofrece avisar al cliente', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [CASO] })
    await asentar(fixture)
    fixture.componentInstance['decidir'](CASO as unknown as CasoEnBandeja)
    fixture.detectChanges()
    expect(textoDe(fixture)).toContain('Decidir el caso LFT-2026-0007')
    expect(textoDe(fixture)).toContain('Nunca se notifica al titular')
    expect(textoDe(fixture).toLowerCase()).not.toMatch(/avisar al cliente|notificar al cliente/)
    http.verify()
  })
})
