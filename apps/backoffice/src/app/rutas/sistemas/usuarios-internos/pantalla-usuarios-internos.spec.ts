import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { UsuarioInternoResumen } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaUsuariosInternos } from './pantalla-usuarios-internos'

const USUARIOS_EJEMPLO: UsuarioInternoResumen[] = [
  {
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
    estado: 'ACTIVO',
    tieneSegundoFactor: true,
    debeCambiarContrasena: false,
  },
]

describe('PantallaUsuariosInternos', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la lista de usuarios internos y sus datos clave', async () => {
    const fixture = TestBed.createComponent(PantallaUsuariosInternos)
    fixture.detectChanges()

    const req = http.expectOne('http://gw/api/v1/identidad/usuarios-internos')
    req.flush(USUARIOS_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('carlos.mamani@aportaya.bo')
    expect(texto).toContain('Carlos')
    expect(texto).toContain('Mamani')
    expect(texto).toContain('RIESGOS')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('RISK_ANALYST')
  })

  it('muestra estado vacío si el backend devuelve lista vacía', async () => {
    const fixture = TestBed.createComponent(PantallaUsuariosInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/identidad/usuarios-internos').flush([])
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('No se encontraron usuarios internos registrados')
  })
})
