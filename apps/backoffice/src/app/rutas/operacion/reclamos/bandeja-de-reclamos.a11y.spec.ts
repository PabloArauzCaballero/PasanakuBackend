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
import { BandejaDeReclamos } from './bandeja-de-reclamos'
import { ejemploDe } from '@aportaya/simulado'

const URL = 'http://gw/api/v1/reclamos'
/** Página del ejemplo generado del contrato (`listarBandejaDeReclamos`, sintético). */
const PAGINA = ejemploDe('cumplimiento', 'listarBandejaDeReclamos', 'ok').cuerpo as object

describe('BandejaDeReclamos · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la bandeja cargada', async () => {
    TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER', 'RECLAMO_ATENDER'], 'operador')
    const fixture = TestBed.createComponent(BandejaDeReclamos)
    fixture.detectChanges()
    http.expectOne((r) => r.url === URL).flush(PAGINA)
    await new Promise((r) => setTimeout(r, 0))
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
