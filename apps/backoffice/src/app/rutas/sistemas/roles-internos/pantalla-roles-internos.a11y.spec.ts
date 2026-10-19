import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaRolesInternos } from './pantalla-roles-internos'

describe('PantallaRolesInternos · accesibilidad', () => {
  it('sin violaciones WCAG serias en catálogo de roles', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaRolesInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/roles').flush([
      {
        id: 'r-1',
        codigo: 'RISK_ANALYST',
        nombre: 'Analista de Riesgo',
        ambito: 'PLATAFORMA',
        departamento: 'RIESGOS',
        descripcion: 'Monitoreo de score y alertas de morosidad',
        esSistema: true,
        permisos: ['ALERTAS_RIESGO_LEER'],
      },
    ])
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
