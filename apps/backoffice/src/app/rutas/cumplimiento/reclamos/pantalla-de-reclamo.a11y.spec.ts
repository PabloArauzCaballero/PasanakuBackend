import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ServicioBorrador } from '../../../nucleo/borrador'
import type { ReclamoDelConsumidor } from '../dominio/cu52-reclamo'
import { PantallaDeReclamo } from './pantalla-de-reclamo'

class ServicioBorradorEnMemoria {
  async guardar(): Promise<void> {}
  async leer(): Promise<undefined> {
    return undefined
  }
  async borrar(): Promise<void> {}
}

const RECLAMO: ReclamoDelConsumidor = {
  id: 'reclamo-a11y',
  registradoEn: '2026-09-01T10:00:00-04:00',
  plazoVenceEn: '2026-09-15T10:00:00-04:00',
  norma: 'ASFI · Reglamento de atención al consumidor financiero',
  respuesta: null,
}

describe('PantallaDeReclamo · accesibilidad', () => {
  it('sin violaciones serias', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ServicioBorrador, useValue: new ServicioBorradorEnMemoria() }] })
    const fixture = TestBed.createComponent(PantallaDeReclamo)
    fixture.componentRef.setInput('reclamo', RECLAMO)
    fixture.componentRef.setInput('ahoraIso', '2026-09-11T10:00:00-04:00')
    fixture.detectChanges()
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
