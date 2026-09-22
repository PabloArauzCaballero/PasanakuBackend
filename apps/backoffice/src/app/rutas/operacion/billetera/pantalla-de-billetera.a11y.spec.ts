import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { PantallaDeBilletera } from './pantalla-de-billetera'

const CUENTA = '11111111-1111-4111-8111-111111111111'
const URL = `http://gw/api/v1/billetera/${CUENTA}/saldo`

describe('PantallaDeBilletera · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  for (const [nombre, respuesta] of [
    ['éxito', () => http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)],
    ['error', () => http.expectOne(URL).flush({ codigo: 'AP-CU04-01', mensaje: '', trazaId: 't' }, { status: 401, statusText: 'x' })],
    ['vacío', () => http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'vacio').cuerpo as object)],
  ] as const) {
    it(`sin violaciones serias en el estado ${nombre}`, async () => {
      const fixture = TestBed.createComponent(PantallaDeBilletera)
      fixture.componentRef.setInput('cuentaId', CUENTA)
      fixture.detectChanges()
      respuesta()
      await fixture.whenStable()
      expect(await axe(fixture.nativeElement)).toHaveNoViolations()
    })
  }
})
