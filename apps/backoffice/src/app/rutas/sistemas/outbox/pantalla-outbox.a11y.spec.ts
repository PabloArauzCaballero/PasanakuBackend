import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { PUERTO_OUTBOX } from '../dominio/puertos'
import { PantallaOutbox } from './pantalla-outbox'

const DATOS = {
  mensajes: [{ id: 'o-1', tipo: 'EventoX', creadoEl: '2026-09-11', estado: 'entregado' as const }],
  descartados: [{ id: 'x-1', tipo: 'EventoY', motivo: 'motivo de ejemplo', descartadoEl: '2026-09-11' }],
}

describe('PantallaOutbox · accesibilidad', () => {
  it('sin violaciones serias, incluida la cola de descartados', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), { provide: PUERTO_OUTBOX, useValue: { obtener: () => Promise.resolve(DATOS) } }],
    })
    const fixture = TestBed.createComponent(PantallaOutbox)
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
