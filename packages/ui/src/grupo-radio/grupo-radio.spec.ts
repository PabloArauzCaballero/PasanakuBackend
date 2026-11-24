import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { GrupoRadio } from './grupo-radio'

@Component({
  imports: [GrupoRadio],
  template: `<ap-grupo-radio etiqueta="Causal" [variante]="variante()" [opciones]="[{ valor: 'A', texto: 'Uno', detalle: 'd' }, { valor: 'B', texto: 'Dos' }]" [(elegido)]="elegido" />`,
})
class Anfitrion {
  readonly variante = signal<'lista' | 'tarjetas'>('lista')
  readonly elegido = signal<string | null>(null)
}

function montar(variante: 'lista' | 'tarjetas') {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(Anfitrion)
  fixture.componentInstance.variante.set(variante)
  fixture.detectChanges()
  return fixture
}

describe('ap-grupo-radio', () => {
  it('lista (por omisión): fieldset sin rol agregado, como siempre', () => {
    const fieldset = montar('lista').nativeElement.querySelector('fieldset') as HTMLFieldSetElement
    expect(fieldset.getAttribute('role')).toBeNull()
  })

  it('tarjetas: es un radiogroup nombrado por su leyenda y sigue eligiendo con radios nativos', async () => {
    const fixture = montar('tarjetas')
    const fieldset = fixture.nativeElement.querySelector('fieldset') as HTMLFieldSetElement
    expect(fieldset.getAttribute('role')).toBe('radiogroup')
    const leyenda = fixture.nativeElement.querySelector(`#${fieldset.getAttribute('aria-labelledby')}`) as HTMLElement
    expect(leyenda.textContent?.trim()).toBe('Causal')
    ;(fixture.nativeElement.querySelectorAll('input[type="radio"]')[1] as HTMLInputElement).click()
    await fixture.whenStable()
    expect(fixture.componentInstance.elegido()).toBe('B')
  })
})
