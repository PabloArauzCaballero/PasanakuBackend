import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ComprobanteDeCierre, type ComprobanteCierreVm } from './comprobante-de-cierre'

const COMPROBANTE_OK: ComprobanteCierreVm = {
  cierreId: 'cierre-12345678-abcd-1234-abcd-1234567890ab',
  fecha: '2026-10-15',
  cuadrado: true,
  yaExistia: false,
  excepcionesAbiertas: 0,
}

const COMPROBANTE_OBSERVADO: ComprobanteCierreVm = {
  cierreId: 'cierre-87654321-abcd-1234-abcd-1234567890ab',
  fecha: '2026-10-14',
  cuadrado: false,
  yaExistia: true,
  excepcionesAbiertas: 2,
  motivoDelDescuadre: 'Sin conciliación de custodia del día',
}

describe('ComprobanteDeCierre · [Componente Dumb]', () => {
  function montar(comprobante: ComprobanteCierreVm, permitirOtro = true): ComponentFixture<ComprobanteDeCierre> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(ComprobanteDeCierre)
    fixture.componentRef.setInput('comprobante', comprobante)
    fixture.componentRef.setInput('permitirOtro', permitirOtro)
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('muestra el comprobante de un cierre cuadrado exitoso', () => {
    const f = montar(COMPROBANTE_OK)
    expect(texto(f)).toContain(COMPROBANTE_OK.cierreId)
    expect(texto(f)).toContain('15 oct 2026')
    expect(texto(f)).toContain('Cuadrado')
    expect(texto(f)).toContain('0')
    expect(f.nativeElement.querySelector('ap-alerta')).not.toBeNull()
  })

  it('muestra observaciones y aviso cuando ya existía y está descuadrado', () => {
    const f = montar(COMPROBANTE_OBSERVADO)
    expect(texto(f)).toContain(COMPROBANTE_OBSERVADO.cierreId)
    expect(texto(f)).toContain('Ese día ya estaba cerrado')
    expect(texto(f)).toContain('Con observaciones')
    expect(texto(f)).toContain('Sin conciliación de custodia del día')
  })

  it('emite el evento otro al pulsar el botón de otro cierre', () => {
    const f = montar(COMPROBANTE_OK, true)
    let emitido = false
    f.componentInstance.otro.subscribe(() => {
      emitido = true
    })
    const boton = f.nativeElement.querySelector('button')
    boton?.click()
    expect(emitido).toBe(true)
  })
})
