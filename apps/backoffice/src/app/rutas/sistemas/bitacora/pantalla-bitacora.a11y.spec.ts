import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaBitacora } from './pantalla-bitacora'

describe('PantallaBitacora · accesibilidad', () => {
  it('sin violaciones WCAG en registro del sistema', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaBitacora)
    fixture.detectChanges()

    http.expectOne((req) => req.url.startsWith('http://gw/api/v1/auditoria/bitacora')).flush({
      items: [
        {
          id: 'b17a0000-0000-4000-8000-000000000001',
          secuencia: 1,
          entidad: 'USUARIO',
          entidadId: 'e17a0000-0000-4000-8000-000000000001',
          accion: 'CREACION',
          actorRol: 'ADMIN_PLATAFORMA',
          origen: 'PANEL_ADMIN',
          correlationId: 'c17a0000-0000-4000-8000-000000000001',
          hashRegistro: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          fechaHora: '2026-10-18T14:30:00Z',
        },
      ],
      totalAproximado: 1,
    })
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
