import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ReclamoEnBandeja } from '../dominio/cu52-reclamos'
import { BandejaDeReclamos } from './bandeja-de-reclamos'

/**
 * **UI contra HTTP simulado** (no prueba integración con `cumplimiento`). Filas del ejemplo generado del contrato
 * (sintético). Cubre la guarda de RECLAMO_ATENDER, vacío, 403, 422, y que tras responder (200) se anuncia y se recarga.
 */
const GW = 'http://gw/api/v1'
const OK = ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as { items: ReclamoEnBandeja[] }
const FILA = { ...OK.items[0]!, estado: 'EN_ANALISIS', montoReclamado: null } as ReclamoEnBandeja
const FICHA = ejemploDe('cumplimiento', 'consultarReclamo', 'ok').cuerpo as object
const listado = (r: { url: string; method: string }) => r.method === 'GET' && r.url === `${GW}/reclamos`

describe('BandejaDeReclamos · [UI contra HTTP simulado] CU-52', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function montar(permisos: string[]): Promise<ComponentFixture<BandejaDeReclamos>> {
    TestBed.inject(Sesion).abrir('t', permisos, 'operador')
    const f = TestBed.createComponent(BandejaDeReclamos)
    f.detectChanges()
    await f.whenStable()
    return f
  }
  async function responder(f: ComponentFixture<unknown>, p: TestRequest, cuerpo: object, estado = 200) {
    p.flush(cuerpo, { status: estado, statusText: 'x' })
    await new Promise((r) => setTimeout(r, 0))
    await f.whenStable()
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('correcto: la primera página va sin estados (el servidor trae los abiertos) y la tabla recibe las filas tal cual', async () => {
    const f = await montar(['RECLAMO_VER'])
    const req = http.expectOne(listado)
    expect(req.request.params.has('estados')).toBe(false)
    await responder(f, req, OK)
    const tabla = f.debugElement.query((d) => d.componentInstance instanceof TablaDeDatosVirtualizada).componentInstance as TablaDeDatosVirtualizada<ReclamoEnBandeja>
    expect(tabla.filas()).toEqual(OK.items)
  })

  it('segregación: sin RECLAMO_ATENDER no se abre la respuesta ni se pide la ficha', async () => {
    const f = await montar(['RECLAMO_VER'])
    await responder(f, http.expectOne(listado), OK)
    f.componentInstance['abrir'](FILA)
    f.detectChanges()
    expect(f.nativeElement.querySelector('ap-respuesta-a-reclamo')).toBeNull()
    http.expectNone(`${GW}/reclamos/${FILA.reclamoId}`)
  })

  it('límite: un reclamo cerrado no se responde aunque se tenga el permiso', async () => {
    const f = await montar(['RECLAMO_VER', 'RECLAMO_ATENDER'])
    await responder(f, http.expectOne(listado), OK)
    f.componentInstance['abrir']({ ...FILA, estado: 'CERRADO' } as ReclamoEnBandeja)
    f.detectChanges()
    expect(f.nativeElement.querySelector('ap-respuesta-a-reclamo')).toBeNull()
  })

  it('límite: bandeja vacía explicada', async () => {
    const f = await montar(['RECLAMO_VER'])
    await responder(f, http.expectOne(listado), { items: [], proximoLimite: null, proximoId: null })
    expect(texto(f)).toContain('No hay reclamos en estos estados.')
  })

  it('inválido: 403 ⇒ sin acceso y sin tabla; 422 AP-CU52-07 ⇒ error traducido', async () => {
    let f = await montar(['RECLAMO_VER'])
    await responder(f, http.expectOne(listado), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(f.nativeElement.querySelector('ap-tabla-de-datos-virtualizada')).toBeNull()
    const g = await montar(['RECLAMO_VER'])
    await responder(g, http.expectOne(listado), { codigo: 'AP-CU52-07', mensaje: 'crudo', trazaId: 'traza-7' }, 422)
    f = g
    expect(texto(f)).toContain('No pudimos traer la bandeja.')
    expect(texto(f)).not.toContain('crudo')
  })

  it('responder: con el 200 se anuncia el cierre (no antes) y la bandeja se vuelve a pedir', async () => {
    const f = await montar(['RECLAMO_VER', 'RECLAMO_ATENDER'])
    await responder(f, http.expectOne(listado), OK)
    f.componentInstance['abrir'](FILA)
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
    await responder(f, http.expectOne(listado), OK)
  })
})
