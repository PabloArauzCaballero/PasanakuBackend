import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../nucleo/idempotencia.interceptor'
import type { DecisionDeIngreso } from './dominio/cu68-admision'
import { FormularioDeResolucion } from './formulario-de-resolucion'

/**
 * **Prueba de UI contra HTTP simulado** (H12.S1.M3): envío, idempotencia y recuperación ante red perdida
 * del formulario de resolución. No prueba integración con `grupos`: Prism (sin estado, sin cortes) no sabe
 * producir un timeout, por eso la red perdida se simula acá.
 */
const SOLICITUD = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const GW = 'http://gw/api/v1'
const URL_HISTORIAL = `${GW}/grupos/solicitudes/${SOLICITUD}/decisiones`
const URL_RESOLVER = `${GW}/grupos/solicitudes/${SOLICITUD}/resoluciones`
const [PROPUESTA, RESOLUCION] = ejemploDe('grupos', 'historialAdmision', 'ok').cuerpo as [DecisionDeIngreso, DecisionDeIngreso]

describe('FormularioDeResolucion · [UI contra HTTP simulado] H12.S1.M3', () => {
  let http: HttpTestingController
  let registradas: number
  let desactualizadas: number

  beforeEach(() => {
    registradas = 0
    desactualizadas = 0
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function montar(): ComponentFixture<FormularioDeResolucion> {
    const f = TestBed.createComponent(FormularioDeResolucion)
    f.componentRef.setInput('solicitudId', SOLICITUD)
    f.componentRef.setInput('propuesta', PROPUESTA)
    f.componentRef.setInput('revision', PROPUESTA.revision)
    f.componentInstance.registrada.subscribe(() => registradas++)
    f.componentInstance.desactualizada.subscribe(() => desactualizadas++)
    f.detectChanges()
    return f
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const boton = (f: ComponentFixture<unknown>, nombre: string) =>
    Array.from((f.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => (b.textContent ?? '').includes(nombre))
  const escribir = (f: ComponentFixture<FormularioDeResolucion>, motivo: string, decision = 'ACEPTAR') => {
    f.componentInstance['decision'].set(decision)
    f.componentInstance['motivo'].set(motivo)
    f.detectChanges()
  }
  const sinRespuesta = (peticion: { error: (e: ProgressEvent, o: { status: number }) => void }) => peticion.error(new ProgressEvent('error'), { status: 0 })

  it('sin motivo ni resultado: un error junto a cada campo y ninguna petición', () => {
    const f = montar()
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('Elegí si aceptás o rechazás')
    expect(texto(f)).toContain('Escribí el motivo')
    http.expectNone(URL_RESOLVER)
  })

  it('un motivo de más de 1000 caracteres se rechaza junto al campo, sin pedir nada', () => {
    const f = montar()
    escribir(f, 'x'.repeat(1001))
    f.componentInstance['enviar']()
    f.detectChanges()
    expect(texto(f)).toContain('hasta 1000 caracteres')
    http.expectNone(URL_RESOLVER)
  })

  it('resolver manda UNA petición con la propuesta vigente, la revisión esperada y la clave de idempotencia; el doble clic no duplica', () => {
    const f = montar()
    escribir(f, 'Evidencia suficiente.')
    f.componentInstance['enviar']()
    f.componentInstance['enviar']()
    const llamadas = http.match(URL_RESOLVER)
    expect(llamadas.length, 'el doble clic no puede mandar dos peticiones').toBe(1)
    const peticion = llamadas[0]!
    expect(peticion.request.body).toEqual({ decision: 'ACEPTAR', motivo: 'Evidencia suficiente.', revisionEsperada: PROPUESTA.revision, propuestaId: PROPUESTA.id })
    expect(peticion.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    peticion.flush(RESOLUCION)
    f.detectChanges()
    expect(registradas).toBe(1)
    expect(texto(f)).toContain('Resolución registrada')
  })

  it('respuesta perdida (sin conexión): queda «sin confirmar», bloquea el formulario, conserva lo escrito y no reenvía solo', () => {
    const f = montar()
    escribir(f, 'Motivo que no se pierde.')
    f.componentInstance['enviar']()
    sinRespuesta(http.expectOne(URL_RESOLVER))
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar si tu resolución quedó registrada')
    expect(boton(f, 'Verificar si se registró')).toBeDefined()
    expect(f.componentInstance['motivo']()).toBe('Motivo que no se pierde.')
    expect((f.nativeElement as HTMLElement).querySelector('fieldset')?.disabled).toBe(true)
    f.componentInstance['enviar']()
    http.expectNone(URL_RESOLVER)
  })

  it('sin confirmar → verificar encuentra la resolución registrada: queda confirmada y NO se vuelve a enviar', () => {
    const f = montar()
    escribir(f, RESOLUCION.motivo, RESOLUCION.decision)
    f.componentInstance['enviar']()
    sinRespuesta(http.expectOne(URL_RESOLVER))
    f.componentInstance['verificar']()
    http.expectOne(URL_HISTORIAL).flush([PROPUESTA, RESOLUCION])
    f.detectChanges()
    expect(texto(f)).toContain('Resolución registrada')
    expect(registradas).toBe(1)
    http.expectNone(URL_RESOLVER)
  })

  it('la verificación compara contra lo ENVIADO, no contra lo que quedó escrito después', () => {
    const f = montar()
    escribir(f, RESOLUCION.motivo, RESOLUCION.decision)
    f.componentInstance['enviar']()
    sinRespuesta(http.expectOne(URL_RESOLVER))
    f.componentInstance['motivo'].set('Otra cosa distinta')
    f.componentInstance['verificar']()
    http.expectOne(URL_HISTORIAL).flush([PROPUESTA, RESOLUCION])
    expect(registradas).toBe(1)
    expect(desactualizadas).toBe(0)
  })

  it('sin confirmar → verificar no la encuentra → reintentar reutiliza LA MISMA clave de idempotencia', () => {
    const f = montar()
    escribir(f, 'Un motivo')
    f.componentInstance['enviar']()
    const primera = http.expectOne(URL_RESOLVER)
    const clave = primera.request.headers.get('Idempotency-Key')
    sinRespuesta(primera)
    f.componentInstance['verificar']()
    http.expectOne(URL_HISTORIAL).flush([PROPUESTA])
    f.detectChanges()
    expect(texto(f)).toContain('La resolución no se registró')
    f.componentInstance['enviar']()
    const segunda = http.expectOne(URL_RESOLVER)
    expect(segunda.request.headers.get('Idempotency-Key')).toBe(clave)
    segunda.flush(RESOLUCION)
    expect(registradas).toBe(1)
  })

  it('si otra persona resolvió mientras tanto: lo dice y pide actualizar el expediente', () => {
    const f = montar()
    escribir(f, 'Mi motivo', 'RECHAZAR')
    f.componentInstance['enviar']()
    sinRespuesta(http.expectOne(URL_RESOLVER))
    f.componentInstance['verificar']()
    http.expectOne(URL_HISTORIAL).flush([PROPUESTA, { ...RESOLUCION, decision: 'ACEPTAR', motivo: 'De otra persona' }])
    f.detectChanges()
    expect(texto(f)).toContain('Otra persona ya resolvió esta solicitud')
    expect(desactualizadas).toBe(1)
    expect(registradas).toBe(0)
  })

  it('si cambia el motivo entre intentos, la clave cambia (no se reutiliza para otro contenido)', () => {
    const f = montar()
    escribir(f, 'Motivo A')
    f.componentInstance['enviar']()
    const a = http.expectOne(URL_RESOLVER)
    const claveA = a.request.headers.get('Idempotency-Key')
    sinRespuesta(a)
    f.componentInstance['verificar']()
    http.expectOne(URL_HISTORIAL).flush([PROPUESTA])
    f.componentInstance['motivo'].set('Motivo B')
    f.componentInstance['enviar']()
    const b = http.expectOne(URL_RESOLVER)
    expect(b.request.headers.get('Idempotency-Key')).not.toBe(claveA)
    b.flush(RESOLUCION)
  })

  it('422 del servidor: mensaje claro, avisa que cambió, conserva lo escrito y pide actualizar el expediente', () => {
    const f = montar()
    escribir(f, 'Conservar este texto')
    f.componentInstance['enviar']()
    http.expectOne(URL_RESOLVER).flush({ codigo: 'AP-CU68-06', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('Actualizamos el expediente')
    expect(texto(f)).toContain('Actualizá el expediente y revisá la última decisión')
    expect(f.componentInstance['motivo']()).toBe('Conservar este texto')
    expect(desactualizadas).toBe(1)
  })
})
