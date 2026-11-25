import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { of, throwError, type Observable } from 'rxjs'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { DialogoDeActo } from './dialogo-de-acto'

const GW = 'http://gw/api/v1'
const EVIDENCIA = 'e'.repeat(40)

@Component({
  selector: 'ap-anfitrion',
  imports: [DialogoDeActo],
  template: `<ap-dialogo-de-acto titulo="Suspender" textoDeConfirmar="Suspender" efecto="Se revocan todos sus roles." [accion]="accion" [(abierto)]="abierto" (completado)="hecho.set($event)" />`,
})
class Anfitrion {
  abierto = signal(true)
  hecho = signal<unknown>(null)
  accion: (evidencia: string, motivo: string) => Observable<unknown> = () => of('ok')
}

describe('DialogoDeActo · motivo + segundo factor + error por campo', () => {
  let backend: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }] })
    backend = TestBed.inject(HttpTestingController)
  })
  afterEach(() => backend.verify())

  async function abrir(accion?: Anfitrion['accion']) {
    const f = TestBed.createComponent(Anfitrion)
    if (accion) f.componentInstance.accion = accion
    f.detectChanges()
    await f.whenStable()
    f.detectChanges()
    const el = f.nativeElement as HTMLElement
    return { f, el }
  }
  const escribirMotivo = (el: HTMLElement, texto: string) => {
    const t = el.querySelector('textarea') as HTMLTextAreaElement
    t.value = texto
    t.dispatchEvent(new Event('input'))
  }
  const escribirCodigo = (el: HTMLElement, codigo: string) => {
    const i = el.querySelector('ap-campo-otp input') as HTMLInputElement
    i.value = codigo
    i.dispatchEvent(new Event('input'))
  }
  const confirmar = (el: HTMLElement) => (Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Suspender') as HTMLButtonElement)
  const responderStepUp = () => {
    backend.expectOne(`${GW}/sesiones/desafios`).flush({ desafioId: 'd1', expiraEn: '2026-11-25T10:05:00Z' })
    backend.expectOne(`${GW}/sesiones/desafios/d1/verificacion`).flush({ evidencia: EVIDENCIA, expiraEn: '2026-11-25T10:05:00Z', jti: 'j1' })
  }

  it('correcto: dice el efecto; no deja confirmar sin motivo ni código; con ambos hace el step-up y llama a la acción con la evidencia y el motivo', async () => {
    const accion = vi.fn(() => of('hecho'))
    const { f, el } = await abrir(accion)
    expect(el.textContent).toContain('Se revocan todos sus roles.')
    expect(confirmar(el).disabled).toBe(true)
    escribirMotivo(el, 'Renuncia presentada el lunes')
    f.detectChanges()
    expect(confirmar(el).disabled).toBe(true)
    escribirCodigo(el, '123456')
    f.detectChanges()
    expect(confirmar(el).disabled).toBe(false)
    confirmar(el).click()
    responderStepUp()
    await f.whenStable()
    expect(accion).toHaveBeenCalledWith(EVIDENCIA, 'Renuncia presentada el lunes')
    expect(f.componentInstance.hecho()).toBe('hecho')
    expect(f.componentInstance.abierto()).toBe(false)
  })

  it('límite: motivo de 9 caracteres o código de 5 dígitos no habilitan; un doble clic mientras viaja no envía dos veces', async () => {
    const accion = vi.fn(() => of('ok'))
    const { f, el } = await abrir(accion)
    escribirMotivo(el, '123456789')
    escribirCodigo(el, '123456')
    f.detectChanges()
    expect(confirmar(el).disabled).toBe(true)
    escribirMotivo(el, '1234567890')
    escribirCodigo(el, '12345')
    f.detectChanges()
    expect(confirmar(el).disabled).toBe(true)
    escribirCodigo(el, '123456')
    f.detectChanges()
    confirmar(el).click()
    confirmar(el).click()
    backend.expectOne(`${GW}/sesiones/desafios`)
    backend.expectNone(`${GW}/sesiones/desafios/d1/verificacion`)
    f.detectChanges()
    expect(Array.from(el.querySelectorAll('button')).some((b) => b.textContent?.includes('Confirmando…') && b.disabled)).toBe(true)
  })

  it('inválido: código equivocado (AP-CU04-04) → el error queda en el campo del código, lo escrito se conserva y el código se pide de nuevo', async () => {
    const accion = vi.fn(() => of('ok'))
    const { f, el } = await abrir(accion)
    escribirMotivo(el, 'Renuncia presentada el lunes')
    escribirCodigo(el, '000000')
    f.detectChanges()
    confirmar(el).click()
    backend.expectOne(`${GW}/sesiones/desafios`).flush({ desafioId: 'd1', expiraEn: 'x' })
    backend.expectOne(`${GW}/sesiones/desafios/d1/verificacion`).flush({ codigo: 'AP-CU04-04', mensaje: 'CRUDO' }, { status: 422, statusText: 'x' })
    await f.whenStable()
    f.detectChanges()
    expect(accion).not.toHaveBeenCalled()
    expect(el.querySelector('ap-campo-otp [role="alert"]')?.textContent).toContain('Ese código no sirve')
    expect(el.textContent).not.toContain('CRUDO')
    expect((el.querySelector('textarea') as HTMLTextAreaElement).value).toBe('Renuncia presentada el lunes')
    expect(f.componentInstance.abierto()).toBe(true)
    expect(confirmar(el).disabled).toBe(true)
  })

  it('inválido: un rechazo del servidor (AP-CU08-06) se muestra traducido y accionable, sin cerrar; un motivo rechazado (AP-CU08-11) va a su campo', async () => {
    let n = 0
    const { f, el } = await abrir(() => throwError(() => ({ codigo: n++ === 0 ? 'AP-CU08-06' : 'AP-CU08-11', estado: 422, mensaje: 'CRUDO' })))
    escribirMotivo(el, 'Renuncia presentada el lunes')
    escribirCodigo(el, '123456')
    f.detectChanges()
    confirmar(el).click()
    responderStepUp()
    await f.whenStable()
    f.detectChanges()
    expect(el.querySelector('ap-error-de-envio')?.textContent).toContain('sin ningún administrador de accesos')
    expect(el.textContent).not.toContain('CRUDO')
    escribirCodigo(el, '654321')
    f.detectChanges()
    confirmar(el).click()
    responderStepUp()
    await f.whenStable()
    f.detectChanges()
    expect(el.querySelector('ap-area-de-texto [role="alert"]')?.textContent).toContain('Escribí el motivo')
    expect(f.componentInstance.abierto()).toBe(true)
  })

  it('límite: sin respuesta del servidor (no se sabe si se registró) conserva la evidencia: reintentar no pide otro código ni abre otro desafío', async () => {
    let intento = 0
    const accion = vi.fn(() => (intento++ === 0 ? throwError(() => ({ estado: 0, sinConexion: true, mensaje: 'Sin conexión' })) : of('ok')))
    const { f, el } = await abrir(accion)
    escribirMotivo(el, 'Renuncia presentada el lunes')
    escribirCodigo(el, '123456')
    f.detectChanges()
    confirmar(el).click()
    responderStepUp()
    await f.whenStable()
    f.detectChanges()
    expect(el.textContent).toContain('No pudimos confirmar si se registró')
    expect(confirmar(el).disabled).toBe(false)
    confirmar(el).click()
    backend.expectNone(`${GW}/sesiones/desafios`)
    await f.whenStable()
    expect(accion).toHaveBeenCalledTimes(2)
    expect(accion).toHaveBeenLastCalledWith(EVIDENCIA, 'Renuncia presentada el lunes')
    expect(f.componentInstance.abierto()).toBe(false)
  })
})
