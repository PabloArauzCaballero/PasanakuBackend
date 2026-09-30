import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { PUERTO_SERVICIOS } from '../dominio/puertos'
import { PantallaServicios } from './pantalla-servicios'

const DATOS = [{ id: 's-1', nombre: 'nucleo-financiero', estado: 'operativo' as const, disponibilidad30d: '99.9%', presupuestoErrorRestante: '50%', ultimaInterrupcion: '2026-01-01' }]

describe('PantallaServicios · accesibilidad', () => {
  it('sin violaciones serias', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), { provide: PUERTO_SERVICIOS, useValue: { obtener: () => Promise.resolve(DATOS) } }],
    })
    const fixture = TestBed.createComponent(PantallaServicios)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
