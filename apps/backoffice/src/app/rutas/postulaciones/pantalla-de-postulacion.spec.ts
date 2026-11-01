import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../nucleo/idempotencia.interceptor'
import type { ExpedienteDeHabilitacion } from './dominio/cu90-postulaciones'
import { PantallaDePostulacion } from './pantalla-de-postulacion'

/**
 * **Prueba de UI contra HTTP simulado** (HttpTestingController): ejercita la pantalla y su cliente, NO prueba
 * integración con `organizador`. Los cuerpos salen de `packages/simulado/ejemplos/organizador/*.json` (sintéticos,
 * generados del contrato).
 */
const ID = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const GW = 'http://gw/api/v1'
const URL_EXPEDIENTE = `${GW}/organizadores/postulaciones/${ID}/expediente`
const URL_RESOLUCION = `${GW}/organizadores/postulaciones/${ID}/resolucion`
const URL_REVISION = `${GW}/organizadores/postulaciones/${ID}/revision`

const BASE = ejemploDe('organizador', 'consultarExpedienteDeHabilitacion', 'ok').cuerpo as ExpedienteDeHabilitacion
const DECISION = ejemploDe('organizador', 'resolverHabilitacion', 'ok').cuerpo as object
const expediente = (estado: string, acciones: string[], revision = 0): ExpedienteDeHabilitacion =>
  ({ solicitud: { ...BASE.solicitud, solicitudId: ID, estado, acciones, revision }, decisiones: [] }) as unknown as ExpedienteDeHabilitacion

describe('PantallaDePostulacion · [UI contra HTTP simulado] CU-90', () => {
  let http: HttpTestingController

  beforeEach(() => {
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

  async function montar(cuerpo: ExpedienteDeHabilitacion): Promise<ComponentFixture<PantallaDePostulacion>> {
    const f = TestBed.createComponent(PantallaDePostulacion)
    f.componentRef.setInput('solicitudId', ID)
    f.detectChanges()
    http.expectOne(URL_EXPEDIENTE).flush(cuerpo)
    await f.whenStable()
    f.detectChanges()
    return f
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  function formulario(f: ComponentFixture<unknown>) {
    return f.debugElement.query((d) => d.name === 'ap-formulario-de-habilitacion')?.componentInstance
  }
  function decidir(f: ComponentFixture<unknown>, decision: string, motivo: string) {
    const c = formulario(f)
    c['decision'].set(decision)
    c['motivo'].set(motivo)
    c['enviar']()
    f.detectChanges()
    return c
  }
  async function recargar(f: ComponentFixture<unknown>, cuerpo: ExpedienteDeHabilitacion) {
    f.detectChanges()
    TestBed.tick()
    http.expectOne(URL_EXPEDIENTE).flush(cuerpo)
    await f.whenStable()
    f.detectChanges()
  }

  it('correcto: una postulación PENDIENTE se aprueba con el cuerpo exacto del contrato y clave, y se confirma', async () => {
    const f = await montar(expediente('PENDIENTE', ['RESOLVER'], 0))
    expect(texto(f)).toContain('Tu resolución')
    decidir(f, 'APROBAR', 'Cumple los requisitos.')
    const pedido = http.expectOne(URL_RESOLUCION)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toEqual({ decision: 'APROBAR', motivo: 'Cumple los requisitos.', revisionEsperada: 0 })
    expect(pedido.request.headers.get('Idempotency-Key')).toBeTruthy()
    pedido.flush({ ...DECISION, nueva: true })
    await recargar(f, expediente('APROBADA', ['REVISAR'], 1))
    expect(texto(f)).toContain('Decisión registrada.')
    expect(texto(f)).toContain('Revisar la habilitación')
  })

  it('correcto: una habilitación aprobada se revoca por la ruta de revisión', async () => {
    const f = await montar(expediente('APROBADA', ['REVISAR'], 3))
    decidir(f, 'REVOCAR', 'Incumplió el reglamento.')
    const pedido = http.expectOne(URL_REVISION)
    expect(pedido.request.body).toEqual({ decision: 'REVOCAR', motivo: 'Incumplió el reglamento.', revisionEsperada: 3 })
    pedido.flush({ ...DECISION, fase: 'REVISION', decision: 'REVOCAR', nueva: true })
  })

  it('límite: motivo vacío o de 1001 caracteres no se manda; un reintento que el servidor reconoce se informa como ya registrado', async () => {
    const f = await montar(expediente('PENDIENTE', ['RESOLVER'], 0))
    const c = decidir(f, 'RECHAZAR', '   ')
    expect(c['errorMotivo']()).toContain('Escribí el motivo')
    decidir(f, 'RECHAZAR', 'x'.repeat(1001))
    expect(c['errorMotivo']()).toContain('hasta 1000')
    http.expectNone(URL_RESOLUCION)
    decidir(f, 'RECHAZAR', 'x'.repeat(1000))
    http.expectOne(URL_RESOLUCION).flush({ ...DECISION, decision: 'RECHAZAR', nueva: false })
    await recargar(f, expediente('RECHAZADA', [], 1))
    expect(texto(f)).toContain('ya estaba registrada')
  })

  it('correcto: una decisión que creó al organizador enlaza a su habilitación', async () => {
    const ORG = 'org-sintetico-1'
    const f = await montar({
      ...expediente('APROBADA', ['REVISAR'], 1),
      decisiones: [{ ...(DECISION as object), organizadorId: ORG, nueva: true }],
    } as unknown as ExpedienteDeHabilitacion)
    const enlace = (f.nativeElement as HTMLElement).querySelector('a.habilitacion')
    expect(enlace?.textContent).toContain('Ver la habilitación del organizador')
    expect(enlace?.getAttribute('href')).toBe(`/cumplimiento/organizadores/${ORG}/habilitacion`)
  })

  it('límite: sin acciones declaradas no se ofrece ningún formulario', async () => {
    const f = await montar(expediente('RECHAZADA', [], 1))
    expect(formulario(f)).toBeUndefined()
    expect(texto(f)).toContain('No hay nada que decidir')
  })

  it('inválido: la autoaprobación (AP-CU90-04) se explica en español, sin el texto crudo, y conserva lo escrito', async () => {
    const f = await montar(expediente('PENDIENTE', ['RESOLVER'], 0))
    const c = decidir(f, 'APROBAR', 'Me apruebo.')
    http.expectOne(URL_RESOLUCION).flush({ codigo: 'AP-CU90-04', mensaje: 'crudo del backend', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No podés resolver tu propia postulación')
    expect(texto(f)).not.toContain('crudo del backend')
    expect(c['motivo']()).toBe('Me apruebo.')
  })

  it('inválido: si otra persona decidió (AP-CU90-08) vuelve a pedir el expediente', async () => {
    const f = await montar(expediente('PENDIENTE', ['RESOLVER'], 0))
    decidir(f, 'APROBAR', 'Cumple.')
    http.expectOne(URL_RESOLUCION).flush({ codigo: 'AP-CU90-08', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'x' })
    await recargar(f, expediente('APROBADA', ['REVISAR'], 1))
    expect(texto(f)).toContain('Revisar la habilitación')
  })

  it('inválido: sin respuesta (503) no se da por perdida; reenviar lo mismo reusa la misma clave', async () => {
    const f = await montar(expediente('PENDIENTE', ['RESOLVER'], 0))
    decidir(f, 'APROBAR', 'Cumple.')
    const primero = http.expectOne(URL_RESOLUCION)
    const clave = primero.request.headers.get('Idempotency-Key')
    primero.flush({}, { status: 503, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar')
    formulario(f)['enviar']()
    const segundo = http.expectOne(URL_RESOLUCION)
    expect(segundo.request.headers.get('Idempotency-Key')).toBe(clave)
    segundo.flush({ ...DECISION, nueva: false })
  })
})
