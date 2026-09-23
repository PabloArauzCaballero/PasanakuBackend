import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { PUERTO_RESPALDOS } from '../dominio/puertos'
import { PantallaRespaldos } from './pantalla-respaldos'

const DATOS = [{ id: 'r-1', origen: 'base-x', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: null }]

describe('PantallaRespaldos · accesibilidad', () => {
  it('sin violaciones serias, incluida la marca de vencida', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), { provide: PUERTO_RESPALDOS, useValue: { obtener: () => Promise.resolve(DATOS) } }],
    })
    const fixture = TestBed.createComponent(PantallaRespaldos)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
