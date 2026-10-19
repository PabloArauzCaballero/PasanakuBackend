import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import type { UsuarioInternoDetalle } from 'clientes/angular/identidad'
import { GATEWAY, GW } from '../../../nucleo/gateway'
import { PantallaUsuarioInternoDetalle } from './pantalla-usuario-interno-detalle'

const DETALLE_FIXTURE: UsuarioInternoDetalle = {
  id: 'u-1',
  usuarioId: 'usr-1',
  codigoPublico: 'USR-001',
  nombres: 'Carlos',
  apellidos: 'Mamani',
  correo: 'carlos.mamani@aportaya.bo',
  telefono: '+59171234567',
  departamento: 'RIESGOS',
  cargo: 'Analista de Riesgo',
  roles: ['RISK_ANALYST'],
  permisosEfectivos: ['ALERTAS_RIESGO_LEER'],
  estado: 'ACTIVO',
  tieneSegundoFactor: true,
  debeCambiarContrasena: false,
}

describe('PantallaUsuarioInternoDetalle · accesibilidad', () => {
  it('sin violaciones WCAG serias en la ficha de detalle', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'u-1' } } },
        },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaUsuarioInternoDetalle)
    fixture.detectChanges()

    http.expectOne(`${GW}/usuarios-internos/u-1`).flush(DETALLE_FIXTURE)
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
