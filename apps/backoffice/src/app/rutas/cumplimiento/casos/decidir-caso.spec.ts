import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { DecisionDeCaso } from '../dominio/cu44-validacion'
import { DecidirCaso } from './decidir-caso'

const CASO_ID = '5a1e0c22-0000-4000-8000-000000000001'
const URL = `${GW}/cumplimiento/casos/${CASO_ID}/decision`
const ROS = '9e8d7c6b-0000-4000-8000-000000000123'
const CONCLUSION = 'Las operaciones tienen respaldo documental suficiente.'

function abrir() {
  const caso = { casoId: CASO_ID, codigo: 'LFT-2026-0007', estado: 'EN_ANALISIS' }
  const montaje = montar(DecidirCaso, ['CUMPLIMIENTO_CASOS'], 'OFICIAL_CUMPLIMIENTO', { caso })
  const c = montaje.fixture.componentInstance
  c.abierto.set(true)
  montaje.fixture.detectChanges()
  const emitido = vi.fn()
  c.decidio.subscribe(emitido)
  return { ...montaje, c, emitido }
}

describe('DecidirCaso · CU-44 decidirCasoLft', () => {
  it('correcto: cuerpo exacto SIN revisor (es la sesión), con Idempotency-Key y botón bloqueado durante el envío', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['decision'].set(DecisionDeCaso.Descartar)
    c['conclusion'].set(`  ${CONCLUSION}  `)
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(URL)
    expect(pedido.request.body).toEqual({ decision: 'DESCARTAR', conclusion: CONCLUSION })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    expect(emitido).not.toHaveBeenCalled()
    const salida = { casoId: CASO_ID, decision: 'DESCARTAR', revisadoPor: 'u-2', alertasCerradas: 2, fueraDePlazo: false, reporteSospechosoId: null }
    pedido.flush(salida)
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith(salida)
    http.verify()
  })

  it('correcto: REPORTAR viaja con el ROS radicado', async () => {
    const { http, c } = abrir()
    c['decision'].set(DecisionDeCaso.Reportar)
    c['conclusion'].set(CONCLUSION)
    c['reporteSospechosoId'].set(ROS)
    void c.enviar()
    expect(http.expectOne(URL).request.body).toEqual({ decision: 'REPORTAR', conclusion: CONCLUSION, reporteSospechosoId: ROS })
  })

  it('límite: conclusión de 19 caracteres no sale; de 20 sí; REPORTAR sin ROS no sale', async () => {
    const { fixture, http, c } = abrir()
    c['decision'].set(DecisionDeCaso.MonitoreoReforzado)
    c['conclusion'].set('a'.repeat(19))
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(URL)
    expect(textoDe(fixture)).toContain('al menos 20 caracteres')

    c['decision'].set(DecisionDeCaso.Reportar)
    c['conclusion'].set('a'.repeat(20))
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(URL)
    expect(textoDe(fixture)).toContain('el identificador del ROS')

    c['decision'].set(DecisionDeCaso.TerminarRelacion)
    void c.enviar()
    expect(http.expectOne(URL).request.body).toEqual({ decision: 'TERMINAR_RELACION', conclusion: 'a'.repeat(20) })
  })

  it('inválido: 422 AP-CU44-02 (revisor = analista) no dice «decidido» y deja el formulario', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['decision'].set(DecisionDeCaso.Descartar)
    c['conclusion'].set(CONCLUSION)
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU44-02')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(c.abierto()).toBe(true)
    expect(c['conclusion']()).toBe(CONCLUSION)
    expect(textoDe(fixture)).toContain('quien analizó el caso no puede decidirlo')
    expect(botonConfirmar(fixture).disabled).toBe(false)
  })

  it('inválido: AP-CU44-03 se muestra en el campo del ROS; AP-CU44-07 como error general', async () => {
    const { fixture, http, c } = abrir()
    c['decision'].set(DecisionDeCaso.Reportar)
    c['conclusion'].set(CONCLUSION)
    c['reporteSospechosoId'].set(ROS)
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU44-03')
    await asentar(fixture)
    expect(c['errores']().reporteSospechosoId).toContain('tiene que estar radicado')

    c['decision'].set(DecisionDeCaso.Descartar)
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU44-07')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('Este caso ya tiene otra decisión')
  })

  it('inválido: 403 sin éxito', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['decision'].set(DecisionDeCaso.Descartar)
    c['conclusion'].set(CONCLUSION)
    void c.enviar()
    rechazar(http.expectOne(URL), 403)
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(textoDe(fixture)).toContain('No tenés acceso a esto.')
  })
})
