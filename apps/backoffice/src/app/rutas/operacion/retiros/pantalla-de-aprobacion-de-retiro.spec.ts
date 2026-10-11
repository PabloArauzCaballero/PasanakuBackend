import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeAprobacionDeRetiro } from './pantalla-de-aprobacion-de-retiro'

/**
 * **Prueba de UI contra HTTP simulado** (F5 · retiro con doble aprobación, CU-11): no prueba integración con
 * `nucleo-financiero`. Cubre la entrada estricta (sin campos de más), la idempotencia por contenido, el éxito no
 * confirmado y cada rechazo del contrato (422 autoaprobación, 409 ya resuelta, 404, 403).
 */
const ORDEN = '2e4c38bf-2985-4b9d-87c9-332257d1445c'
const GW = 'http://gw/api/v1'
const URL = `${GW}/billetera/retiros/${ORDEN}/aprobacion`
const OK = ejemploDe('nucleo-financiero', 'aprobarRetiro', 'ok').cuerpo as { ordenRetiroId: string; estado: string; aprobadaPor: string }

describe('PantallaDeAprobacionDeRetiro · [UI contra HTTP simulado] F5', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('t', ['RETIRO_APROBAR'], 'TESORERIA', 'tes-1')
  })

  function montar(): ComponentFixture<PantallaDeAprobacionDeRetiro> {
    const f = TestBed.createComponent(PantallaDeAprobacionDeRetiro)
    f.detectChanges()
    return f
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const cargar = (f: ComponentFixture<PantallaDeAprobacionDeRetiro>, orden: string, desenlace: string | null, motivo = '') => {
    f.componentInstance['ordenId'].set(orden)
    f.componentInstance['desenlace'].set(desenlace)
    f.componentInstance['motivo'].set(motivo)
    f.detectChanges()
  }
  const sinRespuesta = (p: { error: (e: ProgressEvent, o: { status: number }) => void }) => p.error(new ProgressEvent('error'), { status: 0 })

  it('muestra qué hace, que no hay listado y el formulario (estado inicial, sin pedir nada)', () => {
    const f = montar()
    expect(texto(f)).toContain('Aprobar o rechazar un retiro')
    expect(texto(f)).toContain('Todavía no hay un listado')
    http.expectNone(URL)
  })

  it('sin identificador ni decisión: un error junto a cada campo y ninguna petición', () => {
    const f = montar()
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('no tiene el formato de una orden de retiro')
    expect(texto(f)).toContain('Elegí si autorizás o rechazás')
    http.expectNone(URL)
  })

  it('un motivo de más de 500 caracteres se rechaza junto al campo, sin pedir nada', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA', 'x'.repeat(501))
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('hasta 500 caracteres')
    http.expectNone(URL)
  })

  it('entrada estricta: sin motivo NO manda la clave «motivo»; con motivo manda solo desenlace y motivo; la clave de idempotencia viaja', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA')
    f.componentInstance['enviar']()
    const sin = http.expectOne(URL)
    expect(Object.keys(sin.request.body as object).sort()).toEqual(['desenlace'])
    expect(sin.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    sin.flush(OK)
    f.detectChanges()
    expect(texto(f)).toContain('la orden quedó autorizada')

    f.componentInstance['otra']()
    cargar(f, ORDEN, 'RECHAZADA', 'Titular no coincide')
    f.componentInstance['enviar']()
    const con = http.expectOne(URL)
    expect(con.request.body).toEqual({ desenlace: 'RECHAZADA', motivo: 'Titular no coincide' })
    con.flush({ ...OK, estado: 'RECHAZADA' })
    f.detectChanges()
    expect(texto(f)).toContain('la orden quedó rechazada')
    expect(texto(f)).toContain('Resuelta por')
  })

  it('el doble clic no duplica: una sola petición mientras la primera está en vuelo', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA')
    f.componentInstance['enviar']()
    f.componentInstance['enviar']()
    expect(http.match(URL).length).toBe(1)
  })

  it('respuesta perdida: queda «sin confirmar», conserva lo escrito y el reintento sale con LA MISMA clave de idempotencia', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA', 'Todo en orden')
    f.componentInstance['enviar']()
    const primera = http.expectOne(URL)
    const clave = primera.request.headers.get('Idempotency-Key')
    sinRespuesta(primera)
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar si tu decisión quedó registrada')
    expect(f.componentInstance['motivo']()).toBe('Todo en orden')
    expect(f.componentInstance['ordenId']()).toBe(ORDEN)
    f.componentInstance['enviar']()
    const segunda = http.expectOne(URL)
    expect(segunda.request.headers.get('Idempotency-Key')).toBe(clave)
    segunda.flush(OK)
    f.detectChanges()
    expect(texto(f)).toContain('quedó autorizada')
  })

  it('rechazo (422 AP-CU11-10): dice que no podés resolver lo que pediste vos y conserva lo escrito', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA', 'x')
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'AP-CU11-10', mensaje: 'crudo del backend', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No podés resolver un retiro que pediste vos')
    expect(texto(f)).not.toContain('crudo del backend')
    expect(f.componentInstance['motivo']()).toBe('x')
  })

  it('rechazo (409): cuenta que otra persona ya la resolvió, sin decir «falló»', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA')
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'AP-GW-409', mensaje: 'x', trazaId: 't' }, { status: 409, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('otra persona la resolvió primero')
  })

  it('rechazo (404): la orden no existe y se puede corregir el identificador', () => {
    const f = montar()
    cargar(f, ORDEN, 'AUTORIZADA')
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'X', mensaje: 'x', trazaId: 't' }, { status: 404, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No encontramos esa orden')
    expect(f.componentInstance['ordenId']()).toBe(ORDEN)
  })

  it('sin el permiso de aprobar: lo dice (el servidor igual rechazaría con 403, que se muestra sin detalle)', () => {
    TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER'], 'SOPORTE', 'sop-1')
    const f = montar()
    expect(texto(f)).toContain('permiso de aprobación')
    cargar(f, ORDEN, 'AUTORIZADA')
    f.componentInstance['enviar']()
    http.expectOne(URL).flush({ codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, { status: 403, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No tenés acceso a esto')
  })
})
