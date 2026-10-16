import { describe, expect, it, vi } from 'vitest'
import { asentar, botonConfirmar, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { AprobarYEnviar } from './aprobar-y-enviar'
import { PrepararReporte } from './preparar-reporte'

const REPORTE = { reporteId: 'r-1', periodo: '2026-09', estado: 'GENERADO' }
const SALIDA = { reporteId: 'r-1', cantidadRegistros: 12, reporteEnCero: false, hashArchivo: 'abc', fueraDePlazo: false }

function preparar() {
  const m = montar(PrepararReporte, ['CUMPLIMIENTO_REPORTAR'])
  m.fixture.componentInstance.abierto.set(true)
  m.fixture.detectChanges()
  const emitido = vi.fn()
  m.fixture.componentInstance.preparo.subscribe(emitido)
  return { ...m, c: m.fixture.componentInstance, emitido }
}

function aprobar() {
  const m = montar(AprobarYEnviar, ['CUMPLIMIENTO_REPORTAR'], 'OFICIAL_CUMPLIMIENTO', { reporte: REPORTE })
  m.fixture.componentInstance.abierto.set(true)
  m.fixture.detectChanges()
  const emitido = vi.fn()
  m.fixture.componentInstance.envio.subscribe(emitido)
  return { ...m, c: m.fixture.componentInstance, emitido }
}

describe('PrepararReporte · CU-43 generarReporteRegulatorio', () => {
  it('correcto: cuerpo exacto + Idempotency-Key + botón bloqueado; 201 es «preparado»', async () => {
    const { fixture, http, c, emitido } = preparar()
    c['periodo'].set(' 2026-09 ')
    c['catalogoCodigo'].set(' UIF-MENSUAL ')
    c['formulario'].set('ROG-01')
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(`${GW}/uif/reportes`)
    expect(pedido.request.body).toEqual({ periodo: '2026-09', catalogoCodigo: 'UIF-MENSUAL', formulario: 'ROG-01' })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    pedido.flush(SALIDA, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida: SALIDA, yaExistia: false })
  })

  it('límite: 200 es un reintento (ya estaba preparado); período 2026-13 no sale', async () => {
    const { fixture, http, c, emitido } = preparar()
    c['periodo'].set('2026-13')
    c['catalogoCodigo'].set('UIF')
    c['formulario'].set('PCC-01')
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(`${GW}/uif/reportes`)
    expect(textoDe(fixture)).toContain('año-mes')
    c['periodo'].set('2026-12')
    void c.enviar()
    http.expectOne(`${GW}/uif/reportes`).flush(SALIDA, { status: 200, statusText: 'OK' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida: SALIDA, yaExistia: true })
  })

  it('inválido: 422 AP-CU43-01 sin éxito y con el texto propio', async () => {
    const { fixture, http, c, emitido } = preparar()
    c['periodo'].set('2026-09')
    c['catalogoCodigo'].set('X')
    c['formulario'].set('PCC-01')
    void c.enviar()
    rechazar(http.expectOne(`${GW}/uif/reportes`), 422, 'AP-CU43-01')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(textoDe(fixture)).toContain('No hay catálogo activo con ese código')
  })
})

describe('AprobarYEnviar · CU-43 aprobarYEnviarReporte', () => {
  it('correcto: el período sale de la fila; cuerpo exacto con constancia; botón bloqueado; Idempotency-Key', async () => {
    const { fixture, http, c, emitido } = aprobar()
    c['catalogoCodigo'].set('UIF-MENSUAL')
    c['canal'].set('PORTAL_WEB')
    c['numeroConstancia'].set(' C-0099 ')
    c['aceptado'].set(true)
    c['reintentos'].set('1')
    void c.enviar()
    fixture.detectChanges()
    const pedido = http.expectOne(`${GW}/uif/reportes/envios`)
    expect(pedido.request.body).toEqual({ periodo: '2026-09', catalogoCodigo: 'UIF-MENSUAL', canal: 'PORTAL_WEB', numeroConstancia: 'C-0099', aceptadoPorElOrganismo: true, reintentos: 1 })
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(botonConfirmar(fixture).disabled).toBe(true)
    expect(emitido).not.toHaveBeenCalled()
    pedido.flush({ ...SALIDA, fueraDePlazo: true }, { status: 201, statusText: 'Created' })
    await asentar(fixture)
    expect(emitido).toHaveBeenCalledWith({ salida: { ...SALIDA, fueraDePlazo: true }, yaExistia: false })
  })

  it('límite: sin constancia no se manda el campo; reintentos 0 es válido', async () => {
    const { http, c } = aprobar()
    c['catalogoCodigo'].set('UIF')
    c['canal'].set('CORREO')
    void c.enviar()
    expect(http.expectOne(`${GW}/uif/reportes/envios`).request.body).toEqual({ periodo: '2026-09', catalogoCodigo: 'UIF', canal: 'CORREO', aceptadoPorElOrganismo: false, reintentos: 0 })
  })

  it('inválido: 422 AP-CU43-02 (quien aprueba generó) no dice «aprobado»', async () => {
    const { fixture, http, c, emitido } = aprobar()
    c['catalogoCodigo'].set('UIF')
    c['canal'].set('CORREO')
    void c.enviar()
    rechazar(http.expectOne(`${GW}/uif/reportes/envios`), 422, 'AP-CU43-02')
    await asentar(fixture)
    expect(emitido).not.toHaveBeenCalled()
    expect(c.abierto()).toBe(true)
    expect(textoDe(fixture)).toContain('Quien aprueba no puede ser quien generó el reporte')
    expect(botonConfirmar(fixture).disabled).toBe(false)
  })

  it('inválido: sin canal o reintentos negativos no sale', async () => {
    const { fixture, http, c } = aprobar()
    c['catalogoCodigo'].set('UIF')
    c['reintentos'].set('-1')
    await c.enviar()
    fixture.detectChanges()
    http.expectNone(`${GW}/uif/reportes/envios`)
    expect(textoDe(fixture)).toContain('Elegí el canal')
    expect(textoDe(fixture)).toContain('número entero')
  })
})
