import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import type { FacturaDeProveedor } from '../dominio/cu103-facturas'
import { FichaDeFactura } from './ficha-de-factura'

/** Ver `ficha-de-cobro.spec.ts` (H4.S1.M2, H4.S2.M1/M2): el mismo contrato de borrador, el mismo cableado de descarte, sobre el segundo modal real elegido. */
const FACTURA: FacturaDeProveedor = {
  facturaProveedorId: 'fac-1',
  numeroFactura: 'F-0001',
  terceroRazonSocial: 'Proveedor de prueba',
  ordenCompraId: null,
  fechaEmision: '2026-09-01',
  fechaVencimiento: '2026-10-01',
  estado: 'APROBADA',
  monto: { monto: '500.00', moneda: 'BOB' },
  montoPagado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '500.00', moneda: 'BOB' },
  aprobadaPor: 'otra-cuenta',
  aprobadaPorLaSesion: false,
  asientoContableId: null,
}
const OTRA_FACTURA: FacturaDeProveedor = { ...FACTURA, facturaProveedorId: 'fac-2', numeroFactura: 'F-0002' }
const URL = 'http://gw/api/v1/erp/facturas-de-proveedor/fac-1/pagos'

function montar(factura: FacturaDeProveedor = FACTURA) {
  const fixture = TestBed.createComponent(FichaDeFactura)
  fixture.componentRef.setInput('factura', factura)
  fixture.detectChanges()
  return fixture
}

function escribirMonto(fixture: ReturnType<typeof montar>, texto: string) {
  const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
  input.value = texto
  input.dispatchEvent(new Event('input'))
  fixture.detectChanges()
}

describe('FichaDeFactura · el contenedor es el único dueño del borrador (H4)', () => {
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
    expect((fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('')
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('validación: un monto que excede el saldo pendiente se marca y no se envía', () => {
    const fixture = montar()
    escribirMonto(fixture, '999')
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
    expect(req.request.body).toEqual({ monto: '100', moneda: 'BOB', formaPago: 'TRANSFERENCIA' })
    req.flush(FACTURA)
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
    http.expectOne(URL).flush(FACTURA)
  })

  it('error remoto: conserva el borrador y no cierra el diálogo', () => {
    const fixture = montar()
    escribirMonto(fixture, '100')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    http.expectOne(URL).flush({ mensaje: 'error de servidor' }, { status: 500, statusText: 'x' })
    fixture.detectChanges()
    expect((fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('100')
    expect((fixture.nativeElement.querySelector('dialog') as HTMLDialogElement).hasAttribute('open')).toBe(true)
  })

  it('cancelar con cambios: pregunta antes de descartar (política única cableada end-to-end)', () => {
    confirmSpy.mockReturnValue(false)
    const fixture = montar()
    escribirMonto(fixture, '50')
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    expect((fixture.nativeElement.querySelector('dialog') as HTMLDialogElement).hasAttribute('open')).toBe(true)
  })

  it('reapertura: una ficha nueva para otra factura arranca con borrador limpio', () => {
    const primera = montar()
    escribirMonto(primera, '100')
    const segunda = montar(OTRA_FACTURA)
    expect((segunda.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('')
    const botonCancelar = segunda.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    segunda.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('Q-L1, rama conservadora: si la factura cambia mientras hay un borrador sucio, se bloquea el envío contra la factura vieja', () => {
    const fixture = montar()
    escribirMonto(fixture, '100')
    fixture.componentRef.setInput('factura', OTRA_FACTURA)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('cambió mientras editabas')
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    http.expectNone('http://gw/api/v1/erp/facturas-de-proveedor/fac-2/pagos')
  })
})
