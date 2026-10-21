import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ConfirmacionDeConciliacion, type ConfirmacionConciliacionVm } from './confirmacion-de-conciliacion'

const PENDIENTE: ConfirmacionConciliacionVm = {
  cuentaCustodiaId: '66d81a5c-e974-4f02-8705-39aa2504c995',
  fecha: '2026-10-15',
  saldoCustodia: { monto: '1250000.50', moneda: 'BOB' },
  saldoEnTransito: { monto: '0.00', moneda: 'BOB' },
}

describe('ConfirmacionDeConciliacion · [Componente Dumb]', () => {
  function montar(pendiente: ConfirmacionConciliacionVm | null = PENDIENTE, cargando = false): ComponentFixture<ConfirmacionDeConciliacion> {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    })
    const fixture = TestBed.createComponent(ConfirmacionDeConciliacion)
    fixture.componentRef.setInput('pendiente', pendiente)
    fixture.componentRef.setInput('cargando', cargando)
    fixture.detectChanges()
    return fixture
  }

  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('no muestra ningún diálogo si pendiente es null', () => {
    const f = montar(null)
    expect(f.nativeElement.querySelector('ap-dialogo')).toBeNull()
  })

  it('muestra el diálogo con consecuencia exacta y saldos cuando hay pendiente', () => {
    const f = montar(PENDIENTE)
    const contenido = texto(f)
    expect(contenido).toContain(PENDIENTE.cuentaCustodiaId)
    expect(contenido).toContain('15 oct 2026')
    expect(contenido).toContain('El saldo emitido lo calcula el servidor')
    expect(contenido).toContain('Extracto')
    expect(contenido).toContain('En tránsito')
  })

  it('emite el evento confirmar con la entrada exacta', () => {
    const f = montar(PENDIENTE)
    let recibido: ConfirmacionConciliacionVm | null = null
    f.componentInstance.confirmar.subscribe((val) => {
      recibido = val
    })
    const dialogo = f.nativeElement.querySelector('ap-dialogo')
    expect(dialogo).not.toBeNull()
    f.componentInstance.confirmar.emit(PENDIENTE)
    expect(recibido).toEqual(PENDIENTE)
  })

  it('emite el evento cancelar al pulsar cancelar', () => {
    const f = montar(PENDIENTE)
    let cancelado = false
    f.componentInstance.cancelar.subscribe(() => {
      cancelado = true
    })
    f.componentInstance.cancelar.emit()
    expect(cancelado).toBe(true)
  })
})
