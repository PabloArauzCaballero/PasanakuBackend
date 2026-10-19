import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  PermisoCatalogoNivelRiesgoEnum,
  type PermisoCatalogo,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaPermisosInternos } from './pantalla-permisos-internos'

const PERMISOS_EJEMPLO: PermisoCatalogo[] = [
  {
    id: 'p-1',
    codigo: 'ALERTAS_RIESGO_LEER',
    recurso: 'ALERTAS_RIESGO',
    accion: 'LEER',
    nivelRiesgo: PermisoCatalogoNivelRiesgoEnum.Medio,
    exigeMotivo: false,
    requiereMfa: false,
    descripcion: 'Consultar alertas tempranas de riesgo de mora',
  },
  {
    id: 'p-2',
    codigo: 'CIERRES_DIARIOS_GESTIONAR',
    recurso: 'CIERRES_DIARIOS',
    accion: 'GESTIONAR',
    nivelRiesgo: PermisoCatalogoNivelRiesgoEnum.Critico,
    exigeMotivo: true,
    requiereMfa: true,
    descripcion: 'Ejecutar o ajustar cierre contable y compensación diaria',
  },
]

describe('PantallaPermisosInternos', () => {
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

  it('renderiza la matriz de permisos con su nivel de riesgo y exigencia de motivo', async () => {
    const fixture = TestBed.createComponent(PantallaPermisosInternos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/identidad/permisos').flush(PERMISOS_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('ALERTAS_RIESGO_LEER')
    expect(texto).toContain('CIERRES_DIARIOS_GESTIONAR')
    expect(texto).toContain('CRITICO')
    expect(texto).toContain('Exige motivo')
  })
})
