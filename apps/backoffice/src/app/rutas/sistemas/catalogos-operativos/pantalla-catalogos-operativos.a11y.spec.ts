import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { CatalogoOperativoResumenEstadoEnum } from 'clientes/angular/auditoria'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaCatalogosOperativos } from './pantalla-catalogos-operativos'

describe('PantallaCatalogosOperativos · accesibilidad', () => {
  it('sin violaciones WCAG en listado de catálogos operativos', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaCatalogosOperativos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/auditoria/catalogos').flush([
      {
        codigo: 'HECHOS_GENERADORES',
        nombre: 'Hechos Generadores',
        dueno: 'Contabilidad y Reglas',
        version: '1.2.0',
        totalElementos: 1,
        elementosActivos: 1,
        vigenteDesde: '2026-01-01',
        estado: CatalogoOperativoResumenEstadoEnum.Vigente,
        descripcion: 'Eventos transaccionales.',
      },
    ])
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
