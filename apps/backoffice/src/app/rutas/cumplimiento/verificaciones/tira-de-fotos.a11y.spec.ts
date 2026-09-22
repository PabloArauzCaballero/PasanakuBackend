import { provideZonelessChangeDetection } from '@angular/core'
import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { TiraDeFotos } from './tira-de-fotos'

/** Un expediente al que le falta el reverso: el caso que hay que poder leer sin abrir nada. */
const EXPEDIENTE = {
  verificacionId: 'v-a11y',
  usuarioId: '9f2c1e4a-0000-4000-8000-000000000001',
  nombreCompleto: 'Marcelo Rojas',
  documento: 'CI 4821993 SC',
  estado: ExpedienteEnRevisionEstadoEnum.EnRevision,
  iniciadaEn: '2026-09-17T10:00:00-04:00',
  fotos: ['ANVERSO', 'SELFIE'],
} as unknown as ExpedienteEnRevision

describe('TiraDeFotos · accesibilidad', () => {
  it('sin violaciones serias, con una cara faltante', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const fixture = TestBed.createComponent(TiraDeFotos)
    fixture.componentRef.setInput('expediente', EXPEDIENTE)
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
