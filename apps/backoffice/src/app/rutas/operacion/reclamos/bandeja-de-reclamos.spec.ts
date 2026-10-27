import type { HttpTestingController } from '@angular/common/http/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { asentar, boton, configurar, filasDe, GW, montar, responder, texto } from '../comun/bandeja.apoyo'
import type { ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { BandejaDeReclamos } from './bandeja-de-reclamos'

/**
 * **UI contra HTTP simulado** de la bandeja de reclamos (CU-52). Filas: el ejemplo generado del contrato (sintético) y
 * variaciones suyas. Tres niveles: aceptado (página, filtros, responder), límite (vacío, cerrado, sin permiso) y error.
 */
const URL = `${GW}/reclamos`
const OK = ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as { items: ReclamoEnBandeja[]; proximoLimite: string; proximoId: string }
const FILA = { ...OK.items[0]!, estado: 'EN_ANALISIS', montoReclamado: null } as ReclamoEnBandeja
const CERRADO = { ...FILA, reclamoId: '77777777-7777-4777-8777-777777777777', codigo: 'RC-CERRADO', estado: 'CERRADO', canalIngreso: 'TELEFONO' } as ReclamoEnBandeja
const FICHA = ejemploDe('cumplimiento', 'consultarReclamo', 'ok').cuerpo as object
const VACIA = { items: [], proximoLimite: null, proximoId: null }

describe('BandejaDeReclamos · [UI contra HTTP simulado] CU-52', () => {
  let http: HttpTestingController
  beforeEach(() => (http = configurar()))
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)
  const l = (f: { componentInstance: BandejaDeReclamos }) => f.componentInstance['l']
  const responderEn = (f: { nativeElement: HTMLElement }) => [...f.nativeElement.querySelectorAll<HTMLButtonElement>('button[apAccionDeFila]')]

  it('aceptado: la primera página va sin estados (el servidor trae los abiertos) y la tabla recibe las filas tal cual', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER'])
    const req = listado()
    expect(req.request.params.has('estados')).toBe(false)
    expect(req.request.params.get('limite')).toBe('10')
    await responder(f, req, OK)
    expect(filasDe<ReclamoEnBandeja>(f)).toEqual(OK.items)
  })

  it('aceptado: el estado viaja al servidor; canal y búsqueda trabajan sobre la página; Siguiente usa (despuesDeLimite, despuesDeId)', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER'])
    await responder(f, listado(), OK)
    l(f).filtrar('estados', 'CERRADO')
    const req = listado()
    expect(req.request.params.getAll('estados')).toEqual(['CERRADO'])
    await responder(f, req, { ...OK, items: [FILA, CERRADO] })
    l(f).filtrar('canal', 'TELEFONO')
    await asentar(f)
    expect(filasDe<ReclamoEnBandeja>(f)).toEqual([CERRADO])
    l(f).filtrar('canal', '')
    l(f).buscar('rc-cerrado')
    await asentar(f)
    expect(filasDe<ReclamoEnBandeja>(f)).toEqual([CERRADO])
    boton(f, 'Siguiente')!.click()
    const segunda = listado()
    expect(segunda.request.params.get('despuesDeLimite')).toBe(OK.proximoLimite)
    expect(segunda.request.params.get('despuesDeId')).toBe(OK.proximoId)
    await responder(f, segunda, VACIA)
  })

  it('aceptado: «Responder» pide la ficha; con el 200 del POST se anuncia el cierre (no antes) y la bandeja se vuelve a pedir', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER', 'RECLAMO_ATENDER'])
    await responder(f, listado(), { ...OK, items: [FILA] })
    responderEn(f)[0]!.click()
    f.detectChanges()
    await responder(f, http.expectOne(`${GW}/reclamos/${FILA.reclamoId}`), FICHA)
    const hija = f.debugElement.query((d) => d.name === 'ap-respuesta-a-reclamo').componentInstance
    hija['resultado'].set('DESFAVORABLE')
    hija['texto'].set('No corresponde: la comisión figura en el tarifario vigente.')
    hija['enviar']()
    f.detectChanges()
    expect(texto(f)).not.toContain('Respuesta registrada')
    const post = http.expectOne(`${GW}/reclamos/${FILA.reclamoId}/respuesta`)
    expect(post.request.body).toEqual({ resultado: 'DESFAVORABLE', respuesta: 'No corresponde: la comisión figura en el tarifario vigente.' })
    await responder(f, post, ejemploDe('cumplimiento', 'responderReclamo', 'ok').cuerpo as object)
    expect(texto(f)).toContain(`Respuesta registrada: el reclamo ${FILA.codigo} quedó cerrado.`)
    await responder(f, listado(), OK)
  })

  it('límite: sin RECLAMO_ATENDER no hay «Responder» ni se pide la ficha; un reclamo cerrado tampoco se responde', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER'])
    await responder(f, listado(), { ...OK, items: [FILA] })
    expect(responderEn(f)).toHaveLength(0)
    expect(texto(f)).toContain('Lo responde quien atiende reclamos')
    f.componentInstance['abrir'](FILA)
    f.detectChanges()
    expect(f.nativeElement.querySelector('ap-respuesta-a-reclamo')).toBeNull()
    http.expectNone(`${GW}/reclamos/${FILA.reclamoId}`)
    const g = await montar(BandejaDeReclamos, ['RECLAMO_VER', 'RECLAMO_ATENDER'])
    await responder(g, listado(), { ...OK, items: [CERRADO] })
    expect(responderEn(g)).toHaveLength(0)
    expect(texto(g)).toContain('Ya respondido')
  })

  it('límite: bandeja vacía explicada', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER'])
    await responder(f, listado(), VACIA)
    expect(texto(f)).toContain('No hay reclamos en estos estados.')
  })

  it('error: 403 ⇒ sin acceso y sin tabla; 422 AP-CU52-07 ⇒ error accionable sin el crudo y «Volver a intentar»', async () => {
    const f = await montar(BandejaDeReclamos, ['RECLAMO_VER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(filasDe(f)).toBeNull()
    l(f).recargar()
    await responder(f, listado(), { codigo: 'AP-CU52-07', mensaje: 'crudo', trazaId: 'traza-7' }, 422)
    expect(texto(f)).toContain('No se pudo cargar reclamos.')
    expect(texto(f)).toContain('traza-7')
    expect(texto(f)).not.toContain('crudo')
    boton(f, 'Volver a intentar')!.click()
    await responder(f, listado(), OK)
    expect(filasDe<ReclamoEnBandeja>(f)).toEqual(OK.items)
  })
})
