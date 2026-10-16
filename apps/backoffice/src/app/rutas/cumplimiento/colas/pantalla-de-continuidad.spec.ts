import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { PantallaDeContinuidad } from './pantalla-de-continuidad'
import { RegistrarPrueba } from './registrar-prueba'

const URL = `${GW}/cumplimiento/continuidad/pruebas`
const PLAN = '2d4f6a80-0000-4000-8000-000000000001'
const QUIEN = '2d4f6a80-0000-4000-8000-0000000000e1'
const PRUEBA = { pruebaId: 'p-1', planContinuidadId: PLAN, ejecutadaPor: QUIEN, tipo: 'PARCIAL', fecha: '2026-09-20', rtoMinutos: 45, rpoMinutos: 5, resultado: 'EXITOSA' }

describe('PantallaDeContinuidad · CU-56 listarPruebasDeContinuidad', () => {
  it('correcto: lista sin estados; registrar solo para el rol RESPONSABLE_RIESGOS', async () => {
    const { fixture, http } = montar(PantallaDeContinuidad, ['AUDITORIA_LEER'], 'RESPONSABLE_RIESGOS')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.has('estados')).toBe(false)
    pedido.flush({ items: [PRUEBA] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Registrar una prueba')
    http.verify()
  })
  it('con AUDITORIA_LEER sin el rol, no se ofrece registrar', async () => {
    const { fixture, http } = montar(PantallaDeContinuidad, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('Registrar una prueba')
    expect(textoDe(fixture)).toContain('Todavía no hay pruebas de continuidad registradas.')
  })
  it('inválido: 403 → «sin acceso»', async () => {
    const { fixture, http } = montar(PantallaDeContinuidad, ['AUDITORIA_LEER'])
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
  })
  it('inválido: 422 AP-CU56-04', async () => {
    const uno = montar(PantallaDeContinuidad, ['AUDITORIA_LEER'])
    rechazar(uno.http.expectOne((r) => r.url === URL), 422, 'AP-CU56-04')
    await asentar(uno.fixture)
    expect(textoDe(uno.fixture)).toContain('El filtro de la lista no es válido')
  })
})

describe('RegistrarPrueba · CU-56 registrarPruebaDeContinuidad', () => {
  function registrar() {
    const m = montar(RegistrarPrueba, ['AUDITORIA_LEER'], 'RESPONSABLE_RIESGOS')
    const c = m.fixture.componentInstance
    c.abierto.set(true)
    m.fixture.detectChanges()
    const emitido = vi.fn()
    c.registro.subscribe(emitido)
    const llenar = (extra: Record<string, unknown> = {}) =>
      c['d'].set({ planContinuidadId: PLAN, tipo: 'PARCIAL', fecha: '2026-09-20', ejecutadaPor: QUIEN, actaComiteId: '', rto: '45', rpo: '5', evidenciaUrl: '', impactoEnClientes: false, ...extra })
    return { ...m, c, emitido, llenar }
  }

  it('correcto: cuerpo exacto (sin pérdida: el contrato no le da moneda), Idempotency-Key y botón bloqueado', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar()
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(URL)
    expect(pedido.request.body).toEqual({ planContinuidadId: PLAN, tipo: 'PARCIAL', fecha: '2026-09-20', ejecutadaPor: QUIEN, rtoObtenido: 45, rpoObtenido: 5, impactoEnClientes: false })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    const salida = { pruebaId: 'p-2', rtoObtenido: 45, rpoObtenido: 5, resultado: 'FALLIDA', planAccionId: 'pa-1' }
    pedido.flush(salida, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida, yaExistia: false })
  })

  it('límite: RTO 0 es válido; RTO decimal o negativo no sale', async () => {
    const { http, c, llenar } = registrar()
    llenar({ rto: '1.5' })
    await c.enviar()
    llenar({ rto: '-1' })
    await c.enviar()
    http.expectNone(URL)
    llenar({ rto: '0' })
    void c.enviar()
    expect(http.expectOne(URL).request.body.rtoObtenido).toBe(0)
  })

  it('inválido: 422 AP-CU56-01 se marca en el acta y no hay éxito', async () => {
    const { fixture, http, c, emitido, llenar } = registrar()
    llenar()
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU56-01')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(c['errores']().actaComiteId).toContain('exige el acta del comité')
  })
})
