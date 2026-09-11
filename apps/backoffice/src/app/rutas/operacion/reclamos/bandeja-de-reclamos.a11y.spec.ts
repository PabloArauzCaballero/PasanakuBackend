import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { BandejaDeReclamos } from './bandeja-de-reclamos'
import type { ReclamoDeBandeja } from '../dominio/cu52-reclamos'

const URL = 'http://gw/api/v1/reclamos'
const RECLAMO: ReclamoDeBandeja = {
  reclamoId: 'r-1',
  codigo: 'REC-2026-08-0157',
  categoria: 'COMISION',
  canalIngreso: 'APP',
  fechaIngreso: '2026-08-01T10:00:00-04:00',
  plazoRespuesta: '2026-08-06T10:00:00-04:00',
  diasHabilesPlazo: 5,
  estado: 'EN_ANALISIS',
  responsableId: 'op-1',
  responsableNombre: 'Ana',
}

describe('BandejaDeReclamos · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la bandeja cargada', async () => {
    TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER', 'RECLAMO_ATENDER'], 'operador')
    const fixture = TestBed.createComponent(BandejaDeReclamos)
    fixture.detectChanges()
    http.expectOne(URL).flush([RECLAMO])
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
