import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { FichaDeExpediente } from './ficha-de-expediente'

/** Un expediente sintético al que le faltan fotos y fecha: el caso con más avisos en pantalla. */
const EXPEDIENTE = {
  verificacionId: 'v-a11y',
  usuarioId: 'u-a11y',
  nombreCompleto: 'Persona Sintética',
  telefono: '+59170000009',
  documento: 'CI LP',
  estado: 'EN_REVISION',
  iniciadaEn: '2026-10-20T14:00:00Z',
  documentoVigente: false,
  fotos: ['ANVERSO', 'SELFIE'],
} as unknown as ExpedienteEnRevision

describe('FichaDeExpediente · accesibilidad', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'cumplimiento/verificaciones/:verificacionId', component: FichaDeExpediente }], withComponentInputBinding()),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
  })

  for (const vista of ['resumen', 'fotos', 'decision', 'como-funciona']) {
    it(`sin violaciones serias en la pestaña «${vista}»`, async () => {
      const arnes = await RouterTestingHarness.create(`/cumplimiento/verificaciones/v-a11y?vista=${vista}`)
      TestBed.inject(ApplicationRef).tick()
      TestBed.inject(HttpTestingController).expectOne('http://gw/api/v1/identidad/verificaciones').flush([EXPEDIENTE])
      await arnes.fixture.whenStable()
      arnes.fixture.detectChanges()
      expect(await axe(arnes.routeNativeElement as HTMLElement)).toHaveNoViolations()
    })
  }
})
