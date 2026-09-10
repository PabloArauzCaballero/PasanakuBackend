import { Component } from '@angular/core'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import vectores from '@aportaya/tokens/vectores/monto.json'
import { describe, expect, it } from 'vitest'
import { Monto } from './monto'

@Component({ imports: [Monto], template: `<ap-monto monto="1240.00" moneda="BOB" etiqueta="Saldo disponible" />` })
class Anfitrion {}

describe('ap-monto', () => {
  it('formatea como la maqueta y se anuncia con su concepto', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(Anfitrion)
    await fixture.whenStable()
    const el = fixture.nativeElement.querySelector('ap-monto') as HTMLElement
    expect(el.textContent).toBe('Bs 1.240,00')
    expect(el.getAttribute('aria-label')).toBe('Saldo disponible: Bs 1.240,00')
  })

  it('pasa los mismos vectores que el Monto de Flutter', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(Monto)
    for (const v of vectores.slice(0, 500)) {
      fixture.componentRef.setInput('monto', v.monto)
      fixture.componentRef.setInput('moneda', v.moneda)
      await fixture.whenStable()
      expect(fixture.nativeElement.textContent).toBe(v.esperado)
    }
  })
})
