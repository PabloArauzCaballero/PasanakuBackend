import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { LevantarBloqueo } from './levantar-bloqueo'

/** Datos sintéticos (regla 97.6). */
const BLOQUEO_ID = '0b10c0e0-0000-4000-8000-000000000001'
const URL = `${GW}/billetera/bloqueos/${BLOQUEO_ID}/levantamiento`

function abrir() {
  const bloqueo = { bloqueoId: BLOQUEO_ID, autoridad: 'FISCALIA', tipoOrden: 'RETENCION', alcance: 'TOTAL', estado: 'VIGENTE', puedeLevantar: true, recibidoEn: '2026-11-20T14:00:00Z' }
  const montaje = montar(LevantarBloqueo, ['BLOQUEO_AUTORIDAD_LEVANTAR'], 'SUPERVISOR_CUMPLIMIENTO', { bloqueo })
  const c = montaje.fixture.componentInstance
  c.abierto.set(true)
  montaje.fixture.detectChanges()
  const emitido = vi.fn()
  c.levanto.subscribe(emitido)
  return { ...montaje, c, emitido }
}

describe('LevantarBloqueo · CU-17 levantarBloqueoPorAutoridad', () => {
  it('correcto: POST sin cuerpo (quien levanta es la sesión), con Idempotency-Key y botón bloqueado durante el envío', async () => {
    const { fixture, http, c, emitido } = abrir()
    expect(textoDe(fixture)).toContain('Levantar el bloqueo: Retención de Fiscalía')
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(URL)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toBeNull()
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    expect(emitido).not.toHaveBeenCalled()
    pedido.flush({ bloqueoId: BLOQUEO_ID, estado: 'LEVANTADO' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ bloqueoId: BLOQUEO_ID, estado: 'LEVANTADO' })
    expect(c.abierto()).toBe(false)
    http.verify()
  })

  it('límite: un segundo «confirmar» mientras viaja no manda otro pedido (sin doble envío)', async () => {
    const { http, c } = abrir()
    void c.enviar()
    void c.enviar()
    const pedidos = http.match(URL)
    expect(pedidos).toHaveLength(1)
    pedidos[0]!.flush({ bloqueoId: BLOQUEO_ID, estado: 'LEVANTADO' })
  })

  it('inválido: 422 AP-CU17-04 (misma persona) explica que lo levanta otra persona; el diálogo sigue abierto', async () => {
    const { fixture, http, c, emitido } = abrir()
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU17-04')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Quien registró el bloqueo no puede levantarlo')
    expect(textoDe(fixture)).not.toContain('texto del servidor')
    expect(c.abierto()).toBe(true)
    expect(emitido).not.toHaveBeenCalled()
  })

  it('inválido: 422 AP-CU17-05 (ya no vigente) pide actualizar la lista', async () => {
    const { fixture, http, c } = abrir()
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU17-05')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('ya no está vigente')
    expect(textoDe(fixture)).toContain('Actualizá la lista')
  })
})
