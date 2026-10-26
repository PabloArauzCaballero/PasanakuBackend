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
import { campana, pagina } from '../publicidad.apoyo'

const URL = 'http://gw/api/v1/publicidad/campanas?limite=100&estados=EN_REVISION'

describe('PantallaDeCampanas · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la lista de campañas cargada (gestión + cola de Operaciones)', async () => {
    TestBed.inject(Sesion).abrir('t', ['PUBLICIDAD_ANUNCIANTES', 'PUBLICIDAD_APROBAR_CAMPANA'], 'operador')
    const fixture = TestBed.createComponent(PantallaDeCampanas)
    fixture.detectChanges()
    await new Promise((r) => setTimeout(r, 0))
    http.expectOne(URL).flush(pagina([campana()]))
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
