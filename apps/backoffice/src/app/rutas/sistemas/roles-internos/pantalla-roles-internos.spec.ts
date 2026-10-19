import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { RolCatalogo } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaRolesInternos } from './pantalla-roles-internos'

const ROLES_EJEMPLO: RolCatalogo[] = [
  {
    id: 'r-1',
    codigo: 'RISK_ANALYST',
    nombre: 'Analista de Riesgo',
    ambito: 'PLATAFORMA',
    departamento: 'RIESGOS',
    descripcion: 'Monitoreo de score y alertas de morosidad',
    esSistema: true,
    permisos: ['ALERTAS_RIESGO_LEER', 'SCORE_CONSULTAR'],
  },
  {
    id: 'r-2',
    codigo: 'OPERATIONS_MANAGER',
    nombre: 'Gerente de Operaciones',
    ambito: 'PLATAFORMA',
    departamento: 'OPERACIONES',
    descripcion: 'Supervisión de conciliación y cierres diarios',
    esSistema: true,
    permisos: ['CIERRES_DIARIOS_GESTIONAR'],
  },
]

describe('PantallaRolesInternos', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la lista de roles por departamento', async () => {
    const fixture = TestBed.createComponent(PantallaRolesInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/identidad/roles').flush(ROLES_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('RISK_ANALYST')
    expect(texto).toContain('Analista de Riesgo')
    expect(texto).toContain('OPERATIONS_MANAGER')
    expect(texto).toContain('Gerente de Operaciones')
  })
})
