import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ReciboDeConciliacion, type ReciboConciliacionVm } from './recibo-de-conciliacion'

const RECIBO_OK: ReciboConciliacionVm = {
  conciliacionId: 'recibo-123e4567-e89b-12d3-a456-426614174000',
  estado: 'CUADRADA',
  saldoEmitido: { monto: '50000.00', moneda: 'BOB' },
  diferencia: { monto: '0.00', moneda: 'BOB' },
  ratioCobertura: '1.000000',
  cumpleEncaje: true,
}

const RECIBO_DESCUADRADO: ReciboConciliacionVm = {
  conciliacionId: 'recibo-987fcba9-e89b-12d3-a456-426614174000',
  estado: 'DESCUADRADA',
  saldoEmitido: { monto: '50000.00', moneda: 'BOB' },
  diferencia: { monto: '-1250.00', moneda: 'BOB' },
  ratioCobertura: '0.975000',
  cumpleEncaje: false,
}

describe('ReciboDeConciliacion · [Componente Dumb]', () => {
  function montar(recibo: ReciboConciliacionVm, permitirOtro = true): ComponentFixture<ReciboDeConciliacion> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(ReciboDeConciliacion)
    fixture.componentRef.setInput('recibo', recibo)
    fixture.componentRef.setInput('permitirOtro', permitirOtro)
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('muestra los datos de conciliación cuadrada con tono de éxito', () => {
    const f = montar(RECIBO_OK)
    expect(texto(f)).toContain(RECIBO_OK.conciliacionId)
    expect(texto(f)).toContain('Cuadrada')
    expect(texto(f)).toContain('Cuadra:')
    expect(texto(f)).toContain('Cumple el encaje')
    expect(texto(f)).toContain('1.000000')
    const alerta = f.nativeElement.querySelector('ap-alerta')
    expect(alerta).not.toBeNull()
  })

  it('muestra descuadre y alerta cuando no cumple el encaje', () => {
    const f = montar(RECIBO_DESCUADRADO)
    expect(texto(f)).toContain(RECIBO_DESCUADRADO.conciliacionId)
    expect(texto(f)).toContain('Descuadrada')
    expect(texto(f)).toContain('Descuadre:')
    expect(texto(f)).toContain('No cumple el encaje')
    expect(texto(f)).toContain('0.975000')
  })

  it('emite el evento otra al pulsar el botón de registrar otra', () => {
    const f = montar(RECIBO_OK, true)
    let emitido = false
    f.componentInstance.otra.subscribe(() => {
      emitido = true
    })
    const boton = f.nativeElement.querySelector('button')
    expect(boton).not.toBeNull()
    boton?.click()
    expect(emitido).toBe(true)
  })

  it('oculta el botón si permitirOtro es false', () => {
    const f = montar(RECIBO_OK, false)
    const boton = f.nativeElement.querySelector('button')
    expect(boton).toBeNull()
  })
})
