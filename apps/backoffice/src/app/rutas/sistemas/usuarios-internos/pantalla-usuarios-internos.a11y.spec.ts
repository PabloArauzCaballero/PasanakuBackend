import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaUsuariosInternos } from './pantalla-usuarios-internos'

describe('PantallaUsuariosInternos · accesibilidad', () => {
  it('sin violaciones WCAG serias', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaUsuariosInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/identidad/usuarios-internos').flush([
      {
        id: 'u-1',
        usuarioId: 'usr-1',
        codigoPublico: 'USR-001',
        nombres: 'Carlos',
        apellidos: 'Mamani',
        correo: 'carlos.mamani@aportaya.bo',
        departamento: 'RIESGOS',
        cargo: 'Analista de Riesgo',
        roles: ['RISK_ANALYST'],
        estado: 'ACTIVO',
        tieneSegundoFactor: true,
        debeCambiarContrasena: false,
      },
    ])
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
