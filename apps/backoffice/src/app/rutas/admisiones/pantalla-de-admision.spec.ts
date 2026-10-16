import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../nucleo/sesion'
import type { DecisionDeIngreso } from './dominio/cu68-admision'
import { PantallaDeAdmision } from './pantalla-de-admision'

/**
 * **Prueba de UI contra HTTP simulado** (HttpTestingController): ejercita la pantalla y su cliente, NO
 * prueba integración con `grupos`. Los cuerpos de éxito salen de `packages/simulado/ejemplos/grupos/*.json`
 * (los mismos que sirve Prism). El envío y la recuperación de red están en `formulario-de-resolucion.spec.ts`.
 */
const SOLICITUD = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const GW = 'http://gw/api/v1'
const URL_HISTORIAL = `${GW}/grupos/solicitudes/${SOLICITUD}/decisiones`
const URL_RESOLVER = `${GW}/grupos/solicitudes/${SOLICITUD}/resoluciones`

const ejemplo = (escenario: 'ok' | 'vacio') => ejemploDe('grupos', 'historialAdmision', escenario).cuerpo as DecisionDeIngreso[]
const PROPUESTA = ejemplo('ok')[0] as DecisionDeIngreso
const RESOLUCION = ejemplo('ok')[1] as DecisionDeIngreso

describe('PantallaDeAdmision · [UI contra HTTP simulado] H12.S1.M1', () => {
  let http: HttpTestingController
  let sesion: Sesion

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    sesion = TestBed.inject(Sesion)
  })

  function montar(permisos: string[] = ['ADMIN_PLATAFORMA'], sujeto = 'bo-1'): ComponentFixture<PantallaDeAdmision> {
    sesion.abrir('t', permisos, 'oficial', sujeto)
    const f = TestBed.createComponent(PantallaDeAdmision)
    f.componentRef.setInput('solicitudId', SOLICITUD)
    f.detectChanges()
    return f
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const boton = (f: ComponentFixture<unknown>, nombre: string) =>
    Array.from((f.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => (b.textContent ?? '').includes(nombre))
  async function cargar(f: ComponentFixture<unknown>, cuerpo: unknown = [PROPUESTA]) {
    http.expectOne(URL_HISTORIAL).flush(cuerpo as object)
    await f.whenStable()
    f.detectChanges()
  }
  /** `reload()` de un httpResource pide en el siguiente ciclo, no en el mismo tick. */
  async function recargar(f: ComponentFixture<unknown>, cuerpo: unknown) {
    f.detectChanges()
    TestBed.tick() // corre el efecto que dispara el pedido; whenStable() esperaría para siempre un pedido sin responder
    http.expectOne(URL_HISTORIAL).flush(cuerpo as object)
    await f.whenStable()
    f.detectChanges()
  }

  it('cargando: avisa que está trayendo el expediente, sin formulario', () => {
    const f = montar()
    expect((f.nativeElement as HTMLElement).querySelector('[role="status"]')?.getAttribute('aria-label')).toContain('Cargando')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
    http.expectOne(URL_HISTORIAL).flush([])
  })

  it('con datos: la evidencia del algoritmo es una recomendación, y se ve la propuesta del administrador con el formulario', async () => {
    const f = montar()
    await cargar(f)
    expect(texto(f)).toContain('Evidencia del algoritmo')
    expect(texto(f)).toContain('Es una recomendación')
    expect(texto(f)).toContain('Propuesta del administrador')
    expect(texto(f)).toContain(PROPUESTA.motivo)
    expect(boton(f, 'Resolver solicitud')).toBeDefined()
  })

  it('vacío: dice por qué no hay nada que resolver y no ofrece resolver', async () => {
    const f = montar()
    await cargar(f, ejemplo('vacio'))
    expect(texto(f)).toContain('todavía no propuso una decisión')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('error: mensaje en español accionable, sin el código ni el texto crudo del backend', async () => {
    const f = montar()
    http.expectOne(URL_HISTORIAL).flush({ codigo: 'AP-CU68-06', mensaje: 'crudo del backend', trazaId: 'traza-1' }, { status: 422, statusText: 'x' })
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('Actualizá el expediente')
    expect(texto(f)).not.toContain('crudo del backend')
    expect(boton(f, 'Volver a intentar')).toBeDefined()
  })

  it('sin el permiso de resolver ve el expediente pero ninguna acción de resolución (roles sin acciones ajenas)', async () => {
    const f = montar(['CUMPLIMIENTO_CASOS'])
    await cargar(f)
    expect(texto(f)).toContain(PROPUESTA.motivo)
    expect(texto(f)).toContain('Podés ver el expediente')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
    expect((f.nativeElement as HTMLElement).querySelector('input[type="radio"]')).toBeNull()
  })

  it('si la propuesta la hizo la misma persona, no se le ofrece resolver', async () => {
    const f = montar(['ADMIN_PLATAFORMA'], PROPUESTA.actorId)
    await cargar(f)
    expect(texto(f)).toContain('La resolución la tiene que dar otra persona')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('una solicitud ya resuelta se muestra cerrada, sin formulario, y avisa que la persona todavía no es integrante', async () => {
    const f = montar()
    await cargar(f, [PROPUESTA, RESOLUCION])
    expect(texto(f)).toContain('Resolución de backoffice')
    expect(texto(f)).toContain('La solicitud ya está resuelta')
    expect(texto(f)).toContain('todavía no es integrante')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('al resolver, vuelve a pedir el historial y muestra la solicitud cerrada con la confirmación', async () => {
    const f = montar()
    await cargar(f)
    const formulario = f.debugElement.query((d) => d.name === 'ap-formulario-de-resolucion').componentInstance
    formulario['decision'].set('ACEPTAR')
    formulario['motivo'].set('Evidencia suficiente.')
    formulario['enviar']()
    http.expectOne(URL_RESOLVER).flush(RESOLUCION)
    await recargar(f, [PROPUESTA, RESOLUCION])
    expect(texto(f)).toContain('Resolución registrada. La solicitud quedó cerrada.')
    expect(boton(f, 'Resolver solicitud')).toBeUndefined()
  })

  it('si el servidor dice que la solicitud cambió, vuelve a pedir el historial y conserva lo escrito', async () => {
    const f = montar()
    await cargar(f)
    const formulario = f.debugElement.query((d) => d.name === 'ap-formulario-de-resolucion').componentInstance
    formulario['decision'].set('RECHAZAR')
    formulario['motivo'].set('Conservar este texto')
    formulario['enviar']()
    http.expectOne(URL_RESOLVER).flush({ codigo: 'AP-CU68-06', mensaje: 'x', trazaId: 't' }, { status: 422, statusText: 'x' })
    await recargar(f, [PROPUESTA, { ...PROPUESTA, id: 'nueva-propuesta', revision: PROPUESTA.revision + 1 }])
    expect(texto(f)).toContain('Actualizamos el expediente')
    expect(formulario['motivo']()).toBe('Conservar este texto')
  })
})
