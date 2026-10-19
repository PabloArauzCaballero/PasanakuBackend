import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { Tooltip } from './tooltip'

@Component({
  imports: [Tooltip],
  template: `<ap-tooltip texto="Busca en el nombre y en la descripción."><button type="button" aria-label="Ayuda">?</button></ap-tooltip>`,
})
class Anfitrion {}

describe('Tooltip', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))

  function montar() {
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    const host = el.querySelector('ap-tooltip') as HTMLElement
    return { fixture, host, globo: host.querySelector('[role="tooltip"]') as HTMLElement, boton: host.querySelector('button') as HTMLButtonElement }
  }

  it('correcto: el globo existe siempre, vinculado por aria-describedby, y se muestra al enfocar', () => {
    const { fixture, host, globo, boton } = montar()
    expect(globo.textContent?.trim()).toBe('Busca en el nombre y en la descripción.')
    expect(boton.closest('[aria-describedby]')?.getAttribute('aria-describedby')).toBe(globo.id)
    expect(host.classList.contains('visible')).toBe(false)
    host.dispatchEvent(new FocusEvent('focusin'))
    fixture.detectChanges()
    expect(host.classList.contains('visible')).toBe(true)
  })

  it('correcto: Escape lo cierra aunque el foco siga adentro; al salir y volver aparece de nuevo', () => {
    const { fixture, host } = montar()
    host.dispatchEvent(new FocusEvent('focusin'))
    host.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    fixture.detectChanges()
    expect(host.classList.contains('visible')).toBe(false)
    host.dispatchEvent(new MouseEvent('mouseenter'))
    fixture.detectChanges()
    expect(host.classList.contains('visible'), 'descartado hasta que el puntero se vaya').toBe(false)
    host.dispatchEvent(new MouseEvent('mouseleave'))
    host.dispatchEvent(new MouseEvent('mouseenter'))
    fixture.detectChanges()
    expect(host.classList.contains('visible')).toBe(true)
  })
})
