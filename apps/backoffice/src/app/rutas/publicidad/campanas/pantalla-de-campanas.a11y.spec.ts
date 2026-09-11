import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeCampanas } from './pantalla-de-campanas'
import type { Campana } from '../dominio/cu111-campanas'
import { SalidaCampanaEstadoEnum } from 'clientes/angular/publicidad/model/salidaCampana'

const URL = 'http://gw/api/v1/publicidad/campanas'
const CAMPANA: Campana = {
  campanaPublicitariaId: 'c-1',
  estado: SalidaCampanaEstadoEnum.EnRevision,
  presupuestoTotal: '500.00',
  nombre: 'Lanzamiento',
  moneda: 'BOB',
  fechaInicio: '2026-09-01T00:00:00-04:00',
}

describe('PantallaDeCampanas · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la lista de campañas cargada, lado gestión', async () => {
    TestBed.inject(Sesion).abrir('t', ['PUBLICIDAD_ANUNCIANTES'], 'operador')
    const fixture = TestBed.createComponent(PantallaDeCampanas)
    fixture.detectChanges()
    http.expectOne(URL).flush([CAMPANA])
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
