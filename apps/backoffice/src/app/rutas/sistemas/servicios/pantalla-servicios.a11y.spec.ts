import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { PantallaServicios } from './pantalla-servicios'

describe('PantallaServicios · accesibilidad', () => {
  it('sin violaciones serias', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    const fixture = TestBed.createComponent(PantallaServicios)
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .expectOne('http://gw/api/v1/indicadores/operacion/servicios')
      .flush({ consultadoEn: '2026-10-28T19:04:05Z', servicios: [{ servicio: 'identidad', estado: 'ARRIBA', latenciaMs: 12 }] })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
