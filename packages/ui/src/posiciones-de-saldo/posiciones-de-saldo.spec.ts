import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { PosicionesDeSaldo, type PosicionesParaVista } from './posiciones-de-saldo'

const SEPARADO: PosicionesParaVista = { moneda: 'BOB', gastable: '840.00', comprometidoEnPozos: '400.00', disponible: '1240.00', retenido: '60.00', totalDeLaCuenta: '1300.00', separado: true, advertencias: [] }
const SIN_SEPARAR: PosicionesParaVista = { moneda: 'BOB', gastable: null, comprometidoEnPozos: null, disponible: '1240.00', retenido: '0.00', totalDeLaCuenta: '1240.00', separado: false, advertencias: [] }

@Component({
  imports: [PosicionesDeSaldo],
  template: `<ap-posiciones-de-saldo [posiciones]="p">Lo invertido no forma parte de este saldo.</ap-posiciones-de-saldo>`,
})
class Anfitrion {
  readonly p = SEPARADO
}

function montar(posiciones: PosicionesParaVista, perspectiva: 'titular' | 'soporte' = 'soporte') {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(PosicionesDeSaldo)
  fixture.componentRef.setInput('posiciones', posiciones)
  fixture.componentRef.setInput('perspectiva', perspectiva)
  fixture.detectChanges()
  return fixture.nativeElement as HTMLElement
}

const filas = (el: HTMLElement) => [...el.querySelectorAll('.fila')].map((f) => f.textContent!.replace(/\s+/g, ' ').trim())

describe('ap-posiciones-de-saldo', () => {
  it('muestra cada posición con su cifra y dice con palabras cuál se gasta y cuál no', () => {
    const el = montar(SEPARADO)
    const f = filas(el)
    expect(f).toHaveLength(4)
    expect(f[0]).toContain('Bs 840,00')
    expect(f[0]).toContain('Gastable')
    expect(f[0]).not.toContain('No gastable')
    expect(f[1]).toContain('Comprometido en pozos de grupos')
    expect(f[1]).toContain('Bs 400,00')
    expect(f[1]).toContain('No gastable')
    expect(f[2]).toContain('Retenido por una operación en curso')
    expect(f[2]).toContain('Bs 60,00')
    expect(f[2]).toContain('No gastable')
    expect(f[3]).toContain('Bs 1.300,00')
    expect(f[3]).toContain('Gastable + comprometido + retenido')
  })

  it('cada cifra se anuncia con su concepto: un lector no oye «mil doscientos» a secas', () => {
    const el = montar(SEPARADO)
    const lecturas = [...el.querySelectorAll('ap-monto')].map((m) => m.getAttribute('aria-label'))
    expect(lecturas).toEqual([
      'Gastable por el titular ahora: Bs 840,00',
      'Comprometido en pozos de grupos: Bs 400,00',
      'Retenido por una operación en curso: Bs 60,00',
      'Total de la cuenta en el libro: Bs 1.300,00',
    ])
  })

  it('el vocabulario cambia con la perspectiva: la persona lee «podés», soporte lee «el titular»', () => {
    expect(montar(SEPARADO, 'titular').textContent).toContain('Lo que podés gastar ahora')
    expect(montar(SEPARADO, 'soporte').textContent).toContain('Gastable por el titular ahora')
  })

  it('sin lo comprometido NO inventa una cifra gastable: dice que el disponible está sin separar', () => {
    const el = montar(SIN_SEPARAR)
    expect(el.textContent).toContain('Disponible sin separar')
    expect(el.textContent).toContain('Sin separar')
    expect(el.textContent).not.toMatch(/Gastable por el titular ahora/)
    expect(el.querySelector('.fila--gastable ap-chip-estado')?.textContent).toContain('Sin separar')
    expect(el.textContent).not.toContain('Comprometido en pozos de grupos')
    expect(filas(el)[2]).toContain('Disponible + retenido')
  })

  it('si el libro informa más comprometido que disponible, lo avisa además de mostrar el tope', () => {
    const el = montar({ ...SEPARADO, gastable: '0.00', comprometidoEnPozos: '1240.00', advertencias: ['comprometido-excede-disponible'] })
    expect(el.querySelector('ap-alerta')?.textContent).toContain('más comprometido en pozos que disponible')
    expect(montar({ ...SEPARADO, gastable: '0.00', advertencias: ['comprometido-excede-disponible'] }, 'titular').querySelector('ap-alerta')?.textContent).toContain('por encima de tu disponible')
  })

  it('proyecta el texto de alcance (p. ej. lo invertido no forma parte de este saldo)', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(Anfitrion)
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector('.alcance').textContent).toContain('Lo invertido no forma parte de este saldo')
  })
})
