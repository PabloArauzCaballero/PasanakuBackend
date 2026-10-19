import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ActivatedRoute, provideRouter } from '@angular/router'
import { CatalogoOperativoDetalleEstadoEnum } from 'clientes/angular/auditoria'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaCatalogoOperativoDetalle } from './pantalla-catalogo-operativo-detalle'

describe('PantallaCatalogoOperativoDetalle · accesibilidad', () => {
  it('sin violaciones WCAG en ficha de catálogo y elementos', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (clave: string) => (clave === 'codigo' ? 'UMBRALES_UIF' : null),
              },
            },
          },
        },
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const http = TestBed.inject(HttpTestingController)
    const fixture = TestBed.createComponent(PantallaCatalogoOperativoDetalle)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF').flush({
      codigo: 'UMBRALES_UIF',
      nombre: 'Umbrales UIF',
      dueno: 'Cumplimiento Normativo',
      version: '2.0.0',
      totalElementos: 1,
      elementosActivos: 1,
      vigenteDesde: '2026-03-01',
      estado: CatalogoOperativoDetalleEstadoEnum.Vigente,
      descripcion: 'Límites normativos.',
      tablaFisica: 'cumplimiento.parametro_uif',
      baseNormativa: 'UIF Res. Adm. 045/2024',
    })
    http
      .expectOne((req) => req.url.startsWith('http://gw/api/v1/auditoria/catalogos/UMBRALES_UIF/elementos'))
      .flush([
        {
          id: 'elem-01',
          catalogoCodigo: 'UMBRALES_UIF',
          codigo: 'PCC_01_SIMPLIFICADA',
          nombre: 'Debida Diligencia Simplificada',
          descripcion: 'Monto máximo acumulado.',
          activo: true,
          orden: 1,
          actualizadoEn: '2026-10-18T10:00:00Z',
        },
      ])
    await fixture.whenStable()
    fixture.detectChanges()

    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
