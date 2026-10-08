import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { VencimientoDelDocumento } from './vencimiento-del-documento'

/** Un documento vencido: el caso marcado, que tiene que leerse igual de bien. */
const VENCIDO = {
  verificacionId: 'v-a11y',
  usuarioId: '9f2c1e4a-0000-4000-8000-000000000001',
  nombreCompleto: 'Marcelo Rojas',
  documento: 'CI 4821993 SC',
  estado: ExpedienteEnRevisionEstadoEnum.EnRevision,
  iniciadaEn: '2026-09-17T10:00:00-04:00',
  fotos: [],
  fechaExpiracionDocumento: '2026-09-23',
  documentoVigente: false,
} as unknown as ExpedienteEnRevision

describe('VencimientoDelDocumento · accesibilidad', () => {
  it('sin violaciones serias, con el documento vencido', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(VencimientoDelDocumento)
    fixture.componentRef.setInput('expediente', VENCIDO)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
