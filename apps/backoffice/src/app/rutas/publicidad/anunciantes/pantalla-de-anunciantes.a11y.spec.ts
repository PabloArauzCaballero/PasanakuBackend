import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeAnunciantes } from './pantalla-de-anunciantes'
import { anunciante, pagina } from '../publicidad.apoyo'

const URL = 'http://gw/api/v1/publicidad/anunciantes?limite=100'

describe('PantallaDeAnunciantes · accesibilidad', () => {
  it('sin violaciones serias, con la tabla canónica cargada', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    TestBed.inject(Sesion).abrir('t', ['PUBLICIDAD_ANUNCIANTES'], 'oficial')
    const fixture = TestBed.createComponent(PantallaDeAnunciantes)
    fixture.detectChanges()
    const http = TestBed.inject(HttpTestingController)
    await new Promise((r) => setTimeout(r, 0))
    http.expectOne(URL).flush(pagina([anunciante({ tipo: 'ORGANIZADOR', razonSocialFacturacion: 'Uno SA' }), anunciante({ anuncianteId: 'a-2', limiteGastoMensual: { monto: '900.00', moneda: 'BOB' } })]))
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement as HTMLElement)).toHaveNoViolations()
  })
})
