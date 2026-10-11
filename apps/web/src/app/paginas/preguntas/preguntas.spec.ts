import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { Preguntas } from './preguntas'

/**
 * H12.S1.M2 · en la web pública el saldo no existe (no hay sesión de participantes): lo que la web puede
 * hacer es dejar claras las etiquetas. Esta prueba fija que la pregunta sobre qué es gastable está publicada
 * y nombra las tres posiciones con las mismas palabras que la billetera.
 */
describe('Preguntas · saldos y posiciones', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] }))

  it('explica que no todo lo de la billetera es gastable y nombra gastable, comprometido en pozos y retenido', () => {
    const fixture = TestBed.createComponent(Preguntas)
    fixture.detectChanges()
    const el = fixture.nativeElement as HTMLElement
    const titulos = [...el.querySelectorAll('h2')].map((h) => h.textContent?.trim())
    expect(titulos).toContain('¿Todo lo que veo en mi billetera lo puedo gastar?')
    const texto = el.textContent ?? ''
    for (const palabra of ['gastable', 'comprometido en pozos', 'retenido', 'Lo que invertís tampoco forma parte de ese saldo']) expect(texto).toContain(palabra)
  })
})
