import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { RegistrarRequerimiento } from './registrar-requerimiento'
import { ResponderRequerimiento } from './responder-requerimiento'

const HASH = 'a'.repeat(64)
const REQ = { requerimientoId: '6b0c1d00-0000-4000-8000-000000000001', numeroOficio: 'OF-2026-114', estado: 'RECIBIDO' }

function registrar() {
  const m = montar(RegistrarRequerimiento, ['AUDITORIA_LEER'], 'OFICIAL_CUMPLIMIENTO')
  const c = m.fixture.componentInstance
  c.abierto.set(true)
  m.fixture.detectChanges()
  const emitido = vi.fn()
  c.registro.subscribe(emitido)
  const llenar = (extra: Record<string, unknown> = {}) =>
    c['d'].set({ autoridad: 'FISCALIA', numeroOficio: ' OF-2026-114 ', plazoLocal: '2026-10-20T17:30', usuarioAfectadoId: '', alcance: 'Movimientos de septiembre', documentoUrl: 'minio://oficios/of-114.pdf', hashDocumento: HASH, exigeBloqueoDeSaldo: true, ...extra })
  return { ...m, c, emitido, llenar }
}

describe('RegistrarRequerimiento · CU-45 registrarRequerimientoDeAutoridad', () => {
  it('correcto: cuerpo exacto, plazo en hora de La Paz (−04:00), sin persona si no se cargó, con Idempotency-Key y botón bloqueado', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar()
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(`${GW}/uif/requerimientos`)
    expect(pedido.request.body).toEqual({
      autoridad: 'FISCALIA',
      numeroOficio: 'OF-2026-114',
      plazoRespuesta: '2026-10-20T17:30:00-04:00',
      alcance: 'Movimientos de septiembre',
      documentoUrl: 'minio://oficios/of-114.pdf',
      hashDocumento: HASH,
      exigeBloqueoDeSaldo: true,
    })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    pedido.flush({ requerimientoId: REQ.requerimientoId }, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida: { requerimientoId: REQ.requerimientoId }, yaExistia: false })
  })

  it('límite: hash de 63 caracteres o en mayúscula no sale; 200 es el mismo oficio ya registrado', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar({ hashDocumento: 'a'.repeat(63) })
    await c.enviar()
    llenar({ hashDocumento: 'A'.repeat(64) })
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(`${GW}/uif/requerimientos`)
    expect(textoDe(fixture)).toContain('64 caracteres hexadecimales en minúscula')
    llenar()
    void c.enviar()
    http.expectOne(`${GW}/uif/requerimientos`).flush({ requerimientoId: REQ.requerimientoId }, { status: 200, statusText: 'OK' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida: { requerimientoId: REQ.requerimientoId }, yaExistia: true })
  })

  it('inválido: 422 AP-CU45-03 se marca en el alcance y no hay éxito', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar()
    void c.enviar()
    rechazar(http.expectOne(`${GW}/uif/requerimientos`), 422, 'AP-CU45-03')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(c['errores']().alcance).toContain('pedí una aclaración')
    expect(c['d']().numeroOficio).toBe(' OF-2026-114 ')
  })

  it('inválido: 403 sin éxito', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar()
    void c.enviar()
    rechazar(http.expectOne(`${GW}/uif/requerimientos`), 403)
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(textoDe(fixture)).toContain('No tenés acceso a esto.')
  })
})

describe('ResponderRequerimiento · CU-45 responderRequerimientoDeAutoridad', () => {
  function responder() {
    const m = montar(ResponderRequerimiento, ['AUDITORIA_LEER'], 'OFICIAL_CUMPLIMIENTO', { requerimiento: REQ })
    const c = m.fixture.componentInstance
    c.abierto.set(true)
    m.fixture.detectChanges()
    const emitido = vi.fn()
    c.respondio.subscribe(emitido)
    return { ...m, c, emitido }
  }
  const URL = `${GW}/uif/requerimientos/${REQ.requerimientoId}/respuesta`

  it('correcto: cuerpo exacto (solo el enlace: plazo y oficio son los registrados), Idempotency-Key, botón bloqueado', async () => {
    const { fixture, http, c, emitido } = responder()
    c['respuestaUrl'].set(' minio://respuestas/of-114.pdf ')
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(URL)
    expect(pedido.request.body).toEqual({ respuestaUrl: 'minio://respuestas/of-114.pdf' })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    pedido.flush({ requerimientoId: REQ.requerimientoId }, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledTimes(1)
  })

  it('límite e inválido: vacío no sale; 422 AP-CU45-01 sin éxito', async () => {
    const { fixture, http, c, emitido } = responder()
    await c.enviar()
    http.expectNone(URL)
    c['respuestaUrl'].set('minio://r.pdf')
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU45-01')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(textoDe(fixture)).toContain('Ese número de oficio ya fue registrado.')
  })
})
