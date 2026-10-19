import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { CajonLateral } from './cajon-lateral'

@Component({
  imports: [CajonLateral],
  template: `
    <button type="button" id="abridor" (click)="abierto.set(true)">Revisar</button>
    <ap-cajon-lateral titulo="Revisar columna monto" [(abierto)]="abierto" (cerrado)="cerrados = cerrados + 1">
      <p>Decisión</p>
      <button acciones type="button">Guardar</button>
    </ap-cajon-lateral>
  `,
})
class Anfitrion {
  readonly abierto = signal(false)
  cerrados = 0
}

describe('CajonLateral', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar() {
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    return { fixture, el, dialogo: el.querySelector('dialog') as HTMLDialogElement, anfitrion: fixture.componentInstance }
  }

  it('correcto: cerrado no está abierto; al abrir muestra título, contenido y acciones, con nombre accesible', async () => {
    const { fixture, el, dialogo } = montar()
    expect(dialogo.open).toBe(false)
    ;(el.querySelector('#abridor') as HTMLButtonElement).click()
    fixture.detectChanges()
    await fixture.whenStable()
    expect(dialogo.open).toBe(true)
    expect(dialogo.getAttribute('aria-labelledby')).toBe(dialogo.querySelector('h2')?.id)
    expect(dialogo.querySelector('h2')?.textContent).toBe('Revisar columna monto')
    expect(dialogo.querySelector('footer button')?.textContent).toBe('Guardar')
    expect(dialogo.querySelector('.cerrar')?.getAttribute('aria-label')).toBe('Cerrar el panel')
  })

  it('correcto: el botón de cerrar y Escape cierran, avisan y devuelven el foco a quien abrió', async () => {
    const { fixture, el, dialogo, anfitrion } = montar()
    const abridor = el.querySelector('#abridor') as HTMLButtonElement
    abridor.focus()
    abridor.click()
    fixture.detectChanges()
    await fixture.whenStable()
    ;(dialogo.querySelector('.cerrar') as HTMLButtonElement).click()
    fixture.detectChanges()
    await fixture.whenStable()
    expect(dialogo.open).toBe(false)
    expect(anfitrion.cerrados).toBe(1)
    expect(document.activeElement).toBe(abridor)

    abridor.click()
    fixture.detectChanges()
    await fixture.whenStable()
    const cancelar = new Event('cancel', { cancelable: true })
    dialogo.dispatchEvent(cancelar)
    fixture.detectChanges()
    await fixture.whenStable()
    expect(cancelar.defaultPrevented, 'Escape pasa por la misma puerta de cierre').toBe(true)
    expect(dialogo.open).toBe(false)
    expect(anfitrion.cerrados).toBe(2)
  })
})
