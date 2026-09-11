import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import type { AlertaDeRiesgo } from '../dominio/cu97-alertas'
import { PantallaDeAlertas } from './pantalla-de-alertas'

const ALERTA: AlertaDeRiesgo = {
  id: 'alerta-a11y',
  grupoId: 'grupo-1',
  hechos: [{ nombre: 'Aporte tardío', explicacion: 'El aporte de agosto se registró 6 días después del turno.' }],
  estimaciones: [{ nombre: 'Puntaje de riesgo', explicacion: 'El modelo calcula un puntaje alto para este grupo según sus factores guardados.' }],
  generadaEn: '2026-09-10T08:00:00-04:00',
}

describe('PantallaDeAlertas · accesibilidad', () => {
  it('sin violaciones serias', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(PantallaDeAlertas)
    fixture.componentRef.setInput('alerta', ALERTA)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
