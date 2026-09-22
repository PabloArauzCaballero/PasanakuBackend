import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { sinLenguajeDeCondena, type AlertaDeRiesgo } from '../dominio/cu97-alertas'
import { PantallaDeAlertas } from './pantalla-de-alertas'

const ALERTA: AlertaDeRiesgo = {
  id: 'alerta-1',
  grupoId: 'grupo-1',
  hechos: [{ nombre: 'Aporte tardío', explicacion: 'El aporte de agosto se registró 6 días después del turno.' }],
  estimaciones: [{ nombre: 'Puntaje de riesgo', explicacion: 'El modelo calcula un puntaje alto para este grupo según sus factores guardados.' }],
  generadaEn: '2026-09-10T08:00:00-04:00',
}

describe('PantallaDeAlertas · hechos vs. estimaciones, nunca una condena', () => {
  it('muestra los hechos y las estimaciones en secciones separadas, con el mismo aviso de que no es un veredicto', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(PantallaDeAlertas)
    fixture.componentRef.setInput('alerta', ALERTA)
    fixture.detectChanges()

    const texto = fixture.nativeElement.textContent as string
    expect(texto).toContain('Hechos registrados')
    expect(texto).toContain('Estimaciones del modelo')
    expect(texto).toContain('Aporte tardío')
    expect(texto).toContain('Puntaje de riesgo')
    expect(texto.toLowerCase()).not.toMatch(/probable|probablemente|va a incumplir|incumplirá/)
  })

  it('la guarda de dominio rechaza cualquier alerta redactada como condena', () => {
    const alertaMalRedactada: AlertaDeRiesgo = {
      ...ALERTA,
      estimaciones: [{ nombre: 'Puntaje', explicacion: 'Este participante probablemente va a incumplir.' }],
    }
    expect(sinLenguajeDeCondena(ALERTA)).toBe(true)
    expect(sinLenguajeDeCondena(alertaMalRedactada)).toBe(false)
  })
})
