import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { INTERCEPTORES_DE_RED } from '../../../nucleo/interceptores'
import { olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import type { CuentaPorCobrar } from '../dominio/cu104-cobros'
import { FichaDeCobro } from './ficha-de-cobro'

/**
 * H12.S1.M3 · el cobro, con la cadena de red real: si la respuesta se pierde la pantalla lo DICE (antes tragaba el error
 * en silencio), conserva lo escrito, no cierra, y el reintento del mismo cobro sale con la MISMA clave de idempotencia.
 */
const CUENTA: CuentaPorCobrar = {
  cuentaPorCobrarId: 'cxc-1',
  origenTipo: 'OTRO',
  origenId: 'g-1',
  terceroRazonSocial: 'Tercero de prueba',
  fechaVencimiento: '2026-10-01',
  estado: 'PENDIENTE',
  monto: { monto: '500.00', moneda: 'BOB' },
  cobrado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '500.00', moneda: 'BOB' },
}
const URL = 'http://gw/api/v1/erp/cuentas-por-cobrar/cxc-1/cobros'

describe('FichaDeCobro · éxito no confirmado', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors(INTERCEPTORES_DE_RED)), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function pulsarConfirmar(fixture: ReturnType<typeof TestBed.createComponent<FichaDeCobro>>) {
    const botones = fixture.nativeElement.querySelectorAll('ap-boton') as NodeListOf<HTMLElement>
    ;(botones[1]!.querySelector('button') as HTMLButtonElement).click()
    fixture.detectChanges()
  }

  it('corte de red: avisa que no se pudo confirmar, conserva el monto, deja el diálogo abierto y reintenta con la misma clave', async () => {
    const fixture = TestBed.createComponent(FichaDeCobro)
    fixture.componentRef.setInput('cuenta', CUENTA)
    fixture.detectChanges()
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    input.value = '100'
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    let cerrada = false
    fixture.componentInstance.cerrada.subscribe(() => (cerrada = true))

    pulsarConfirmar(fixture)
    const primera = http.expectOne(URL)
    const clave = primera.request.headers.get('Idempotency-Key')
    primera.error(new ProgressEvent('error'), { status: 0 })
    await fixture.whenStable()
    fixture.detectChanges()

    expect(fixture.nativeElement.querySelector('ap-error-de-envio [role="alert"]')?.textContent).toContain('No pudimos confirmar si se registró')
    expect(cerrada).toBe(false)
    expect((fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement).value).toBe('100')

    pulsarConfirmar(fixture)
    const segunda = http.expectOne(URL)
    expect(segunda.request.headers.get('Idempotency-Key'), 'mismo cobro, misma clave: no se duplica').toBe(clave)
    segunda.flush({})
    await fixture.whenStable()
    expect(cerrada).toBe(true)
  })

  it('rechazo del servidor (422): dice que NO se completó y no promete que repetir no duplica', async () => {
    const fixture = TestBed.createComponent(FichaDeCobro)
    fixture.componentRef.setInput('cuenta', CUENTA)
    fixture.detectChanges()
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    input.value = '100'
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    pulsarConfirmar(fixture)
    http.expectOne(URL).flush({ codigo: 'AP-CU104-02', mensaje: 'tecnico', trazaId: 'tz-1' }, { status: 422, statusText: 'Unprocessable Entity' })
    await fixture.whenStable()
    fixture.detectChanges()
    const texto = fixture.nativeElement.querySelector('ap-error-de-envio')?.textContent ?? ''
    expect(texto).toContain('No se pudo completar')
    expect(texto).toContain('Código de seguimiento: tz-1')
    expect(texto).not.toContain('tecnico')
    expect(texto).not.toContain('no se duplica')
  })

  it('el monto escrito en formato es-BO sale como cadena del contrato, y el exceso del saldo se valida con ese valor', async () => {
    const fixture = TestBed.createComponent(FichaDeCobro)
    fixture.componentRef.setInput('cuenta', CUENTA)
    fixture.detectChanges()
    const input = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    const escribir = (t: string) => {
      input.value = t
      input.dispatchEvent(new Event('input'))
      fixture.detectChanges()
    }
    const errorDelCampo = () => fixture.nativeElement.querySelector('ap-campo-monto [role="alert"]')?.textContent ?? ''
    escribir('1.240,50')
    expect(errorDelCampo()).toContain('no puede exceder el saldo pendiente')
    escribir('250,5')
    expect(errorDelCampo()).toBe('')
    pulsarConfirmar(fixture)
    const req = http.expectOne(URL)
    expect(req.request.body).toEqual({ monto: '250.50', formaCobro: 'TRANSFERENCIA' })
    req.flush({})
  })
})
