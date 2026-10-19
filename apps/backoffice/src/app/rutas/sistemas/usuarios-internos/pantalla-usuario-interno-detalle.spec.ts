import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { UsuarioInternoDetalle } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaUsuarioInternoDetalle } from './pantalla-usuario-interno-detalle'

const USUARIO_DETALLE: UsuarioInternoDetalle = {
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
  permisosEfectivos: ['ALERTAS_RIESGO_LEER', 'SCORE_CONSULTAR'],
  estado: 'ACTIVO',
  tieneSegundoFactor: true,
  debeCambiarContrasena: false,
}

describe('PantallaUsuarioInternoDetalle', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'u-1' } } },
        },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la ficha con datos de organización, roles y permisos efectivos', async () => {
    const fixture = TestBed.createComponent(PantallaUsuarioInternoDetalle)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/usuarios-internos/u-1').flush(USUARIO_DETALLE)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('Carlos Mamani')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('USR-001')
    expect(texto).toContain('RISK_ANALYST')
    expect(texto).toContain('ALERTAS_RIESGO_LEER')
  })
})
