import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { DialogoDeConfirmacion } from './dialogo-de-confirmacion'

describe('DialogoDeConfirmacion', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar(frase?: string) {
    const fixture = TestBed.createComponent(DialogoDeConfirmacion)
    fixture.componentRef.setInput('titulo', 'Guardar ficha de la tabla')
    fixture.componentRef.setInput('descripcion', 'Se actualizará la ficha de aportes.pago (3 campos).')
    fixture.componentRef.setInput('textoDeConfirmar', 'Guardar ficha')
    if (frase) fixture.componentRef.setInput('fraseDeConfirmacion', frase)
    let confirmaciones = 0
    fixture.componentInstance.confirmar.subscribe(() => confirmaciones++)
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    const confirmar = Array.from(el.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes('Guardar ficha')) as HTMLButtonElement
    return { fixture, el, confirmar, confirmaciones: () => confirmaciones }
  }

  it('correcto: título, descripción y confirmar habilitado cuando no hay frase', () => {
    const { el, confirmar, confirmaciones } = montar()
    expect(el.querySelector('h2')?.textContent).toBe('Guardar ficha de la tabla')
    expect(el.textContent).toContain('Se actualizará la ficha de aportes.pago (3 campos).')
    expect(confirmar.disabled).toBe(false)
    confirmar.click()
    expect(confirmaciones()).toBe(1)
  })

  it('límite: con frase, confirmar queda deshabilitado hasta escribirla exactamente (los espacios de más no cuentan)', () => {
    const { fixture, el, confirmar } = montar('RETIRAR')
    expect(confirmar.disabled).toBe(true)
    const input = el.querySelector('ap-campo input') as HTMLInputElement
    input.value = 'retirar'
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(confirmar.disabled).toBe(true)
    input.value = ' RETIRAR '
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(confirmar.disabled).toBe(false)
  })

  it('inválido: al reabrir, la frase escrita antes ya no vale', () => {
    const { fixture, el, confirmar } = montar('RETIRAR')
    const input = el.querySelector('ap-campo input') as HTMLInputElement
    input.value = 'RETIRAR'
    input.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    expect(confirmar.disabled).toBe(false)
    fixture.componentInstance.abierto.set(false)
    fixture.detectChanges()
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    expect(fixture.componentInstance.escrito()).toBe('')
    expect(confirmar.disabled).toBe(true)
  })
})
