import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { type PermisoCatalogo, PermisoCatalogoNivelRiesgoEnum } from 'clientes/angular/identidad'
import { GATEWAY, GW } from '../../../nucleo/gateway'
import { PantallaPermisosInternos } from './pantalla-permisos-internos'

const PERMISOS_FIXTURE: PermisoCatalogo[] = [
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
]

describe('PantallaPermisosInternos · accesibilidad', () => {
  it('sin violaciones WCAG serias en la matriz de permisos', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaPermisosInternos)
    fixture.detectChanges()

    http.expectOne(`${GW}/permisos`).flush(PERMISOS_FIXTURE)
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
