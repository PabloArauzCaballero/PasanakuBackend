import { describe, expect, it, vi } from 'vitest'
import type { CasoEnBandeja } from 'clientes/angular/cumplimiento'
import { Router } from '@angular/router'
import { TestBed } from '@angular/core/testing'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, boton, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
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

    boton(fixture, 'Siguiente')!.click()
    await asentar(fixture)
    const segundo = http.expectOne((r) => r.url === URL)
    expect(segundo.request.params.get('despuesDePlazo')).toBe(CASO.plazoLimite)
    expect(segundo.request.params.get('despuesDeId')).toBe(CASO.casoId)
    segundo.flush({ items: [], proximoPlazo: null, proximoId: null })
    await asentar(fixture)
    http.verify()
  })

  it('correcto: el filtro de estado viaja al servidor como una sola clave (explode: false)', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    bandejaDe<Bandeja<CasoEnBandeja>>(fixture).filtrar('estados', 'CERRADO')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['CERRADO'])
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

  it('la acción de fila «Decidir» abre el diálogo sobre ese caso; nunca ofrece avisar al cliente', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    http.expectOne((r) => r.url === URL).flush({ items: [CASO] })
    await asentar(fixture)
    ;(fixture.nativeElement.querySelector('[aria-label="Decidir el caso LFT-2026-0007"]') as HTMLButtonElement).click()
    fixture.detectChanges()
    expect(textoDe(fixture)).toContain('Decidir el caso LFT-2026-0007')
    expect(textoDe(fixture)).toContain('Nunca se notifica al titular')
    expect(textoDe(fixture).toLowerCase()).not.toMatch(/avisar al cliente|notificar al cliente/)
    http.verify()
  })

  it('acciones de fila: «Ver» lleva a la ficha; un caso cerrado o ya decidido no ofrece «Decidir»; nunca Editar ni Eliminar', async () => {
    const { fixture, http } = montar(BandejaDeCasos, ['CUMPLIMIENTO_CASOS'])
    const cerrado = { ...CASO, casoId: 'c-2', codigo: 'LFT-2026-0008', estado: 'CERRADO', decision: 'DESCARTAR' }
    http.expectOne((r) => r.url === URL).flush({ items: [CASO, cerrado] })
    await asentar(fixture)
    expect(fixture.nativeElement.querySelector('[aria-label="Decidir el caso LFT-2026-0008"]')).toBeNull()
    expect(textoDe(fixture)).not.toMatch(/Editar|Eliminar/)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    ;(fixture.nativeElement.querySelector('[aria-label="Ver caso LFT-2026-0007"]') as HTMLButtonElement).click()
    expect(navegar).toHaveBeenCalledWith(['/cumplimiento/casos', CASO.casoId])
    http.verify()
  })

  it('sin CUMPLIMIENTO_CASOS no hay «Decidir» (el servidor lo rechazaría igual)', async () => {
    const { fixture, http } = montar(BandejaDeCasos, [])
    http.expectOne((r) => r.url === URL).flush({ items: [CASO] })
    await asentar(fixture)
    expect(fixture.nativeElement.querySelector('[aria-label="Decidir el caso LFT-2026-0007"]')).toBeNull()
    expect(fixture.nativeElement.querySelector('ap-decidir-caso')).toBeNull()
  })
})
