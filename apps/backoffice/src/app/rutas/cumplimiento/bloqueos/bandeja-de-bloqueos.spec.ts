import { describe, expect, it } from 'vitest'
import type { BloqueoEnLista } from 'clientes/angular/nucleo-financiero/model/bloqueoEnLista'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, boton, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { BandejaDeBloqueos } from './bandeja-de-bloqueos'

/** Datos sintéticos (regla 97.6): identificadores de prueba, sin personas ni cuentas reales. */
const URL = `${GW}/billetera/bloqueos`
const PARCIAL = {
  bloqueoId: '0b10c0e0-0000-4000-8000-000000000001',
  cuentaBilleteraId: '0b10c0e0-0000-4000-8000-0000000000c1',
  autoridad: 'JUZGADO',
  tipoOrden: 'EMBARGO',
  alcance: 'PARCIAL',
  estado: 'VIGENTE',
  montoBloqueado: { monto: '1240.50', moneda: 'BOB' },
  registradoPor: '0b10c0e0-0000-4000-8000-0000000000a1',
  levantadoPor: null,
  recibidoEn: '2026-11-20T14:00:00Z',
  venceEn: null,
  levantadoEn: null,
  puedeLevantar: true,
}
const TOTAL_PROPIO = { ...PARCIAL, bloqueoId: '0b10c0e0-0000-4000-8000-000000000002', autoridad: 'UIF', tipoOrden: 'CONGELAMIENTO', alcance: 'TOTAL', montoBloqueado: undefined, puedeLevantar: false }
const LEVANTAR_PARCIAL = '[aria-label^="Levantar el bloqueo Embargo de Juzgado"]'

describe('BandejaDeBloqueos · CU-17 listarBloqueosPorAutoridad', () => {
  it('correcto: primera página sin cursor; la segunda viaja con el par (antesDe, antesDeId) que dio el servidor', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    const primero = http.expectOne((r) => r.url === URL)
    expect(primero.request.params.has('antesDe')).toBe(false)
    expect(primero.request.params.has('antesDeId')).toBe(false)
    primero.flush({ items: [PARCIAL], proximoAntesDe: PARCIAL.recibidoEn, proximoAntesDeId: PARCIAL.bloqueoId })
    await asentar(fixture)
    expect(filasDe<BloqueoEnLista>(fixture).map((b) => b.bloqueoId)).toEqual([PARCIAL.bloqueoId])

    boton(fixture, 'Siguiente')!.click()
    await asentar(fixture)
    const segundo = http.expectOne((r) => r.url === URL)
    expect(segundo.request.params.get('antesDe')).toBe(PARCIAL.recibidoEn)
    expect(segundo.request.params.get('antesDeId')).toBe(PARCIAL.bloqueoId)
    segundo.flush({ items: [], proximoAntesDe: null, proximoAntesDeId: null })
    await asentar(fixture)
    http.verify()
  })

  it('correcto: el filtro de estado viaja como `estado` (el único que admite el contrato)', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    bandejaDe<Bandeja<BloqueoEnLista>>(fixture).filtrar('estado', 'VIGENTE')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estado')).toEqual(['VIGENTE'])
    expect(pedido.request.params.keys().sort()).toEqual(['estado', 'limite'])
    pedido.flush({ items: [] })
    http.verify()
  })

  it('correcto: el monto parcial se muestra con su moneda desde la cadena exacta; el total dice «Todo el saldo»', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [PARCIAL, TOTAL_PROPIO] })
    await asentar(fixture)
    const montos = [...(fixture.nativeElement as HTMLElement).querySelectorAll('ap-monto')]
    expect(montos).toHaveLength(1)
    expect(montos[0]!.textContent).toContain('1.240,50')
    expect(textoDe(fixture)).toContain('Todo el saldo')
    expect(textoDe(fixture)).toContain('Sin fecha de fin')
  })

  it('límite: vacío orienta (por qué está vacío y cuándo aparece un bloqueo)', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay bloqueos en este estado.')
    expect(textoDe(fixture)).toContain('cuando se registra la orden de una autoridad')
  })

  it('inválido: 403 → «sin acceso», sin tabla', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(fixture)).toEqual([])
  })

  it('inválido: 422 AP-CU17-06 (cursor a medias) muestra el filtro inválido con su código de seguimiento', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU17-06')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
    expect(textoDe(fixture)).toContain('traza-123')
    expect(textoDe(fixture)).not.toContain('texto del servidor')
  })
})

describe('BandejaDeBloqueos · acción «Levantar» (cuatro ojos)', () => {
  it('con BLOQUEO_AUTORIDAD_LEVANTAR, «Levantar» aparece solo en la fila con puedeLevantar=true', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_LEVANTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [PARCIAL, TOTAL_PROPIO] })
    await asentar(fixture)
    const el = fixture.nativeElement as HTMLElement
    expect(el.querySelectorAll('[aria-label^="Levantar el bloqueo"]')).toHaveLength(1)
    expect(el.querySelector(LEVANTAR_PARCIAL)).not.toBeNull()
  })

  it('sin BLOQUEO_AUTORIDAD_LEVANTAR no se ofrece levantar, aunque el servidor diga puedeLevantar', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_REGISTRAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [PARCIAL] })
    await asentar(fixture)
    expect((fixture.nativeElement as HTMLElement).querySelector('[aria-label^="Levantar el bloqueo"]')).toBeNull()
  })

  it('levantar abre la confirmación; al 200 avisa y recarga la bandeja', async () => {
    const { fixture, http } = montar(BandejaDeBloqueos, ['BLOQUEO_AUTORIDAD_LEVANTAR'])
    http.expectOne((r) => r.url === URL).flush({ items: [PARCIAL] })
    await asentar(fixture)
    ;((fixture.nativeElement as HTMLElement).querySelector(LEVANTAR_PARCIAL) as HTMLButtonElement).click()
    fixture.detectChanges()
    expect(textoDe(fixture)).toContain('Levantar el bloqueo: Embargo de Juzgado')
    expect(textoDe(fixture)).toContain('distinto de quien lo registró')

    const dialogo = fixture.debugElement.query((d) => d.name === 'ap-levantar-bloqueo').componentInstance as { enviar(): Promise<void> }
    void dialogo.enviar()
    http.expectOne(`${URL}/${PARCIAL.bloqueoId}/levantamiento`).flush({ bloqueoId: PARCIAL.bloqueoId, estado: 'LEVANTADO' })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Bloqueo levantado.')
    http.expectOne((r) => r.url === URL).flush({ items: [{ ...PARCIAL, estado: 'LEVANTADO', puedeLevantar: false }] })
    await asentar(fixture)
    expect((fixture.nativeElement as HTMLElement).querySelector(LEVANTAR_PARCIAL)).toBeNull()
    http.verify()
  })
})
