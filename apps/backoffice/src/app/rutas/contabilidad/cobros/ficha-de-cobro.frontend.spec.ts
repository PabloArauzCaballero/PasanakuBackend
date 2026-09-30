import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { FichaDeCobro } from './ficha-de-cobro'
import type { CuentaPorCobrar } from '../dominio/cu104-cobros'

const CUENTA: CuentaPorCobrar = {
  cuentaPorCobrarId: 'c-1',
  origenTipo: 'GRUPO',
  origenId: 'g-1',
  terceroRazonSocial: 'Ana Pérez',
  fechaVencimiento: '2026-09-30',
  estado: 'PENDIENTE',
  monto: { monto: '500.00', moneda: 'BOB' },
  cobrado: { monto: '0.00', moneda: 'BOB' },
  saldoPendiente: { monto: '500.00', moneda: 'BOB' },
}

function montar() {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: 'http://gw/api/v1' },
    ],
  })
  const fixture = TestBed.createComponent(FichaDeCobro)
  fixture.componentRef.setInput('cuenta', CUENTA)
  fixture.detectChanges()
  return fixture
}

/**
 * Migración al diálogo canónico (H4.S2.M1). Cubre el kill-test del encargo: un borrador
 * sucio (monto tipeado) protegido igual por las tres rutas de cierre, y el error de
 * guardado que conserva lo cargado. El foco atrapado/restaurado real (nativo de
 * `showModal`) no se prueba acá —jsdom no lo implementa— sino en E2E.
 */
describe('FichaDeCobro · CU-104, migrado a ap-dialogo con política de descarte', () => {
  let http: HttpTestingController

  beforeEach(() => {
    vi.spyOn(window, 'confirm')
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('borrador limpio: cerrar por el botón cancelar no pregunta nada', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    const botonCancelar = fixture.nativeElement.querySelectorAll('.acciones button')[0] as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(window.confirm).not.toHaveBeenCalled()
    expect(fixture.componentInstance['abierto']()).toBe(false)
    http.verify()
  })

  it('borrador sucio (monto tipeado): cerrar por el botón pregunta, y si se elige quedarse, no se pierde', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    ;(window.confirm as ReturnType<typeof vi.spyOn>).mockReturnValue(false)
    const campoMonto = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    campoMonto.value = '100'
    campoMonto.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(fixture.componentInstance['sucio']()).toBe(true)

    const botonCancelar = fixture.nativeElement.querySelectorAll('.acciones button')[0] as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()

    expect(window.confirm).toHaveBeenCalledOnce()
    expect(fixture.componentInstance['abierto']()).toBe(true)
    expect(fixture.componentInstance['monto']()).toBe('100')
    http.verify()
  })

  it('borrador sucio: Escape (evento nativo `cancel`) pasa por la MISMA política que el botón', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    ;(window.confirm as ReturnType<typeof vi.spyOn>).mockReturnValue(false)
    const campoMonto = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    campoMonto.value = '250'
    campoMonto.dispatchEvent(new Event('input'))
    fixture.detectChanges()

    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const evento = new Event('cancel', { cancelable: true })
    dialog.dispatchEvent(evento)

    expect(evento.defaultPrevented).toBe(true)
    expect(window.confirm).toHaveBeenCalledOnce()
    expect(fixture.componentInstance['abierto']()).toBe(true)
    http.verify()
  })

  it('borrador sucio: clic en el fondo pasa por la MISMA política — las tres rutas, un solo predicado', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    ;(window.confirm as ReturnType<typeof vi.spyOn>).mockReturnValue(true)
    const campoMonto = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    campoMonto.value = '250'
    campoMonto.dispatchEvent(new Event('input'))
    fixture.detectChanges()

    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const clickEnFondo = new MouseEvent('click')
    Object.defineProperty(clickEnFondo, 'target', { value: dialog })
    dialog.dispatchEvent(clickEnFondo)
    fixture.detectChanges()

    expect(window.confirm).toHaveBeenCalledOnce()
    expect(fixture.componentInstance['abierto']()).toBe(false)
    http.verify()
  })

  it('cambiar la forma de cobro (aunque el monto siga vacío) también ensucia el borrador', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    expect(fixture.componentInstance['sucio']()).toBe(false)
    fixture.componentInstance['forma'].set('QR')
    fixture.detectChanges()
    expect(fixture.componentInstance['sucio']()).toBe(true)
    http.verify()
  })

  it('error de guardado: conserva el monto y la forma cargados, no limpia el borrador', () => {
    const fixture = montar()
    http = TestBed.inject(HttpTestingController)
    const campoMonto = fixture.nativeElement.querySelector('ap-campo-monto input') as HTMLInputElement
    campoMonto.value = '250'
    campoMonto.dispatchEvent(new Event('input'))
    fixture.detectChanges()

    const botonConfirmar = fixture.nativeElement.querySelectorAll('.acciones button')[1] as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    http.expectOne(`http://gw/api/v1/erp/cuentas-por-cobrar/${CUENTA.cuentaPorCobrarId}/cobros`).flush(
      { codigo: 'AP-CU104-02', mensaje: 'tecnico', trazaId: 't' },
      { status: 409, statusText: 'Conflict' },
    )
    fixture.detectChanges()

    expect(fixture.componentInstance['monto']()).toBe('250')
    expect(fixture.componentInstance['forma']()).toBe('TRANSFERENCIA')
    expect(fixture.componentInstance['abierto']()).toBe(true)
    expect(fixture.componentInstance['enviando']()).toBe(false)
  })
})
