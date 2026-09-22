import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CuentaPorCobrar } from '../dominio/cu104-cobros'
import { FichaDeCobro } from './ficha-de-cobro'

/**
 * H4.S1.M2 — casos de prueba reales del formulario que `Dialogo` NO conoce (el contenedor
 * es el único dueño del borrador, ver entregables/contrato-formulario.md).
 * H4.S2.M1/M2 — el mismo archivo ya usa `<ap-dialogo>`: esta suite prueba la política de
 * descarte recién cableada (`hayCambiosSinGuardar`) y la rama conservadora de Q-L1.
 */
const CUENTA: CuentaPorCobrar = {
  cuentaPorCobrarId: 'cxc-1',
  origenTipo: 'GRUPO',
  origenId: 'g-1',
  terceroRazonSocial: 'Tercero de prueba',
  fechaVencimiento: '2026-10-01',
  estado: 'PENDIENTE',
  monto: { monto: '500.00', moneda: 'BOB' },
  cobrado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '500.00', moneda: 'BOB' },
}
const OTRA_CUENTA: CuentaPorCobrar = { ...CUENTA, cuentaPorCobrarId: 'cxc-2', terceroRazonSocial: 'Otro tercero' }
const URL = 'http://gw/api/v1/erp/cuentas-por-cobrar/cxc-1/cobros'

function montar(cuenta: CuentaPorCobrar = CUENTA) {
  const fixture = TestBed.createComponent(FichaDeCobro)
  fixture.componentRef.setInput('cuenta', cuenta)
  fixture.detectChanges()
  return fixture
}

function escribirMonto(fixture: ReturnType<typeof montar>, texto: string) {
  const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
  input.value = texto
  input.dispatchEvent(new Event('input'))
  fixture.detectChanges()
}

function elegirForma(fixture: ReturnType<typeof montar>, valor: string) {
  const radio = fixture.nativeElement.querySelector(`input[type="radio"][value="${valor}"]`) as HTMLInputElement
  radio.checked = true
  radio.dispatchEvent(new Event('change'))
  fixture.detectChanges()
}

describe('FichaDeCobro · el contenedor es el único dueño del borrador (H4)', () => {
  let http: HttpTestingController
  let confirmSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
    confirmSpy = vi.spyOn(window, 'confirm')
  })
  afterEach(() => {
    http.verify()
    confirmSpy.mockRestore()
  })

  it('valores iniciales: monto vacío, forma en TRANSFERENCIA, y cerrar sin tocar nada no pregunta (sin cambios sin guardar)', () => {
    const fixture = montar()
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    expect(input.value).toBe('')
    expect((fixture.nativeElement.querySelector('input[type="radio"][value="TRANSFERENCIA"]') as HTMLInputElement).checked).toBe(true)
    const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    caja.dispatchEvent(new Event('cancel', { cancelable: true }))
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('campo tocado/modificado: escribir un monto marca hayCambiosSinGuardar, y eso alcanza al Dialogo (Escape ahora pregunta)', () => {
    const fixture = montar()
    escribirMonto(fixture, '50')
    const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    caja.dispatchEvent(new Event('cancel', { cancelable: true }))
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
  })

  it('cambiar solo la forma de cobro (sin tocar monto) también cuenta como cambio sin guardar', () => {
    const fixture = montar()
    elegirForma(fixture, 'QR')
    const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    caja.dispatchEvent(new Event('cancel', { cancelable: true }))
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
  })

  it('validación: un monto que excede el saldo pendiente se marca y no se envía', () => {
    const fixture = montar()
    escribirMonto(fixture, '999')
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('no puede exceder el saldo pendiente')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    http.expectNone(URL)
  })

  it('envío: monto válido dispara el POST real y, al responder, cierra y emite `cerrada`', () => {
    const fixture = montar()
    let cerrada = false
    fixture.componentRef.instance.cerrada.subscribe(() => (cerrada = true))
    escribirMonto(fixture, '100')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    const req = http.expectOne(URL)
    expect(req.request.body).toEqual({ monto: '100', formaCobro: 'TRANSFERENCIA' })
    req.flush(CUENTA)
    fixture.detectChanges()
    expect(cerrada).toBe(true)
  })

  it('doble envío bloqueado: dos clics rápidos antes de que responda el servidor generan UN solo POST', () => {
    const fixture = montar()
    escribirMonto(fixture, '100')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    botonConfirmar.click()
    fixture.detectChanges()
    http.expectOne(URL).flush(CUENTA)
  })

  it('error remoto: conserva el borrador (monto, forma) y no cierra el diálogo', () => {
    const fixture = montar()
    escribirMonto(fixture, '100')
    elegirForma(fixture, 'QR')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    http.expectOne(URL).flush({ mensaje: 'error de servidor' }, { status: 500, statusText: 'x' })
    fixture.detectChanges()
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    expect(input.value).toBe('100')
    expect((fixture.nativeElement.querySelector('input[type="radio"][value="QR"]') as HTMLInputElement).checked).toBe(true)
    const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    expect(caja.hasAttribute('open')).toBe(true)
  })

  it('cancelar sin cambios: cierra directo y emite `cerrada`, sin preguntar', () => {
    const fixture = montar()
    let cerrada = false
    fixture.componentRef.instance.cerrada.subscribe(() => (cerrada = true))
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(cerrada).toBe(true)
  })

  it('cancelar con cambios: pregunta antes (política de descarte cableada end-to-end)', () => {
    confirmSpy.mockReturnValue(false)
    const fixture = montar()
    escribirMonto(fixture, '50')
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    expect(caja.hasAttribute('open')).toBe(true)
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    expect(input.value).toBe('50')
  })

  it('reapertura: una ficha nueva para otra cuenta arranca con borrador limpio (no hereda el de la anterior)', () => {
    const primera = montar()
    escribirMonto(primera, '100')
    const segunda = montar(OTRA_CUENTA)
    expect((segunda.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('')
    const botonCancelar = segunda.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    segunda.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('Q-L1, rama conservadora: si la cuenta cambia mientras hay un borrador sucio, no se pierde el borrador y no se puede confirmar contra la cuenta vieja', () => {
    const fixture = montar()
    escribirMonto(fixture, '100')
    fixture.componentRef.setInput('cuenta', OTRA_CUENTA)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('cambió mientras editabas')
    expect((fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('100')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    http.expectNone('http://gw/api/v1/erp/cuentas-por-cobrar/cxc-2/cobros')
  })
})
