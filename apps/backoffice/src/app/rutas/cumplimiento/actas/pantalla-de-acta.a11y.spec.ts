import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import type { ActaDeComite } from '../dominio/cu94-acta'
import { PantallaDeActa } from './pantalla-de-acta'

const ACTA: ActaDeComite = {
  id: 'acta-a11y',
  comiteId: 'comite-cumplimiento',
  quorumMinimo: 3,
  composicionRequerida: ['OFICIAL_CUMPLIMIENTO', 'GERENCIA', 'DIRECTORIO'],
  asistentes: [
    { integranteId: 'u1', rol: 'OFICIAL_CUMPLIMIENTO', voto: 'A_FAVOR' },
    { integranteId: 'u2', rol: 'GERENCIA', voto: 'ABSTENCION', motivoAbstencion: 'Tiene interés directo en el asunto' },
  ],
}

describe('PantallaDeActa · accesibilidad', () => {
  it('sin violaciones serias, incluso con el botón de cerrar deshabilitado por falta de quórum', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(PantallaDeActa)
    fixture.componentRef.setInput('acta', ACTA)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
