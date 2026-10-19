import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { AbrirCaso } from './abrir-caso'

const URL = `${GW}/cumplimiento/casos`
const A1 = '0b9b2a61-0000-4000-8000-000000000001'
const A2 = '0b9b2a61-0000-4000-8000-000000000002'
const ANALISTA = '7c1d9e40-0000-4000-8000-0000000000aa'

function abrir(alertas: string[] = [A1, A2]) {
  const montaje = montar(AbrirCaso, ['CUMPLIMIENTO_CASOS'], 'OFICIAL_CUMPLIMIENTO', { alertaIds: alertas })
  const c = montaje.fixture.componentInstance
  c.abierto.set(true)
  montaje.fixture.detectChanges()
  const emitido = vi.fn()
  c.abrio.subscribe(emitido)
  return { ...montaje, c, emitido }
}

describe('AbrirCaso · CU-44 abrirCasoLft', () => {
  it('correcto: cuerpo exacto del contrato, con Idempotency-Key, y el botón bloqueado mientras se envía', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['analistaId'].set(` ${ANALISTA} `)
    c['resumen'].set('  Operaciones fraccionadas en la misma semana.  ')
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(URL)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toEqual({ alertaIds: [A1, A2], analistaId: ANALISTA, resumen: 'Operaciones fraccionadas en la misma semana.' })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    expect(emitido).not.toHaveBeenCalled()
    pedido.flush({ casoId: 'c-1', estado: 'ABIERTO', plazoLimite: '2026-11-10T14:00:00Z' }, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ casoId: 'c-1', estado: 'ABIERTO', plazoLimite: '2026-11-10T14:00:00Z' })
    expect(c.abierto()).toBe(false)
    http.verify()
  })

  it('límite: 50 alertas se envían; 51 no salen de la pantalla', async () => {
    const cincuenta = Array.from({ length: 50 }, (_, i) => `0b9b2a61-0000-4000-8000-${String(i).padStart(12, '0')}`)
    const { fixture, http, c } = abrir(cincuenta)
    c['analistaId'].set(ANALISTA)
    c['resumen'].set('Resumen')
    void c.enviar()
    const pedido = http.expectOne(URL)
    expect(pedido.request.body.alertaIds).toHaveLength(50)
    pedido.flush({ casoId: 'c-1', estado: 'ABIERTO', plazoLimite: '2026-11-10T14:00:00Z' }, { status: 201, statusText: 'Created' })
    await asentar(fixture)

    fixture.componentRef.setInput('alertaIds', [...cincuenta, A1])
    c.abierto.set(true)
    c['analistaId'].set(ANALISTA)
    c['resumen'].set('Resumen')
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(URL)
    expect(textoDe(fixture)).toContain('hasta 50 alertas')
  })

  it('inválido (cliente): analista que no es UUID y resumen vacío no se envían; el error queda en su campo', async () => {
    const { fixture, http, c } = abrir()
    c['analistaId'].set('juan')
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(URL)
    const texto = textoDe(fixture)
    expect(texto).toContain('formato UUID')
    expect(texto).toContain('Escribí el resumen del caso.')
  })

  it('inválido: 422 AP-CU44-08 no dice «abierto», marca la selección y deja el formulario como estaba', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['analistaId'].set(ANALISTA)
    c['resumen'].set('Resumen')
    void c.enviar()
    rechazar(http.expectOne(URL), 422, 'AP-CU44-08')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(c.abierto()).toBe(true)
    expect(c['resumen']()).toBe('Resumen')
    expect(textoDe(fixture)).toContain('es de otro titular o ya está en otro caso')
    expect(botonConfirmar(fixture).disabled).toBe(false)
  })

  it('inválido: 403 se muestra como error del envío, sin éxito', async () => {
    const { fixture, http, c, emitido } = abrir()
    c['analistaId'].set(ANALISTA)
    c['resumen'].set('Resumen')
    void c.enviar()
    rechazar(http.expectOne(URL), 403)
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(textoDe(fixture)).toContain('No tenés acceso a esto.')
  })
})
