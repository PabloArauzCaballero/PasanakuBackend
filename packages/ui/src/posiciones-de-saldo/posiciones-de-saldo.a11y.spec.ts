import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { PosicionesDeSaldo, type PosicionesParaVista } from './posiciones-de-saldo'

const BASE: PosicionesParaVista = { moneda: 'BOB', gastable: '840.00', comprometidoEnPozos: '400.00', disponible: '1240.00', retenido: '60.00', totalDeLaCuenta: '1300.00', separado: true, advertencias: [] }
const CASOS: [string, PosicionesParaVista][] = [
  ['separado', BASE],
  ['sin separar', { ...BASE, gastable: null, comprometidoEnPozos: null, separado: false, totalDeLaCuenta: '1300.00' }],
  ['comprometido mayor que el disponible', { ...BASE, gastable: '0.00', comprometidoEnPozos: '1240.00', advertencias: ['comprometido-excede-disponible'] }],
]

describe('ap-posiciones-de-saldo · accesibilidad', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] }))
  for (const tema of ['light', 'dark'] as const) {
    for (const [nombre, posiciones] of CASOS) {
      it(`sin violaciones: ${nombre}, tema ${tema}`, async () => {
        document.documentElement.setAttribute('data-theme', tema)
        const fixture = TestBed.createComponent(PosicionesDeSaldo)
        fixture.componentRef.setInput('posiciones', posiciones)
        fixture.detectChanges()
        expect(await axe(fixture.nativeElement, { rules: { 'color-contrast': { enabled: false } } })).toHaveNoViolations()
      })
    }
  }
})
