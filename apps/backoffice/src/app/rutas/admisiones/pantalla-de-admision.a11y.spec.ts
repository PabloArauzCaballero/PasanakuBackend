import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { Sesion } from '../../nucleo/sesion'
import { EntradaDeAdmision } from './entrada-de-admision'
import { PantallaDeAdmision } from './pantalla-de-admision'

const SOLICITUD = '9f1c2b7e-3a54-4d0e-8a61-5b7d2c9e4f10'
const URL = `http://gw/api/v1/grupos/solicitudes/${SOLICITUD}/decisiones`

describe('PantallaDeAdmision · accesibilidad (axe, [UI contra HTTP simulado])', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  for (const [nombre, permisos, respuesta] of [
    ['con propuesta y formulario de resolución', ['ADMIN_PLATAFORMA'], () => http.expectOne(URL).flush(ejemploDe('grupos', 'historialAdmision', 'ok').cuerpo as object[])],
    ['vacío', ['ADMIN_PLATAFORMA'], () => http.expectOne(URL).flush([])],
    ['error', ['ADMIN_PLATAFORMA'], () => http.expectOne(URL).flush({ codigo: 'AP-CU68-06', mensaje: '', trazaId: 't' }, { status: 422, statusText: 'x' })],
    ['solo lectura (sin permiso de resolver)', ['CUMPLIMIENTO_CASOS'], () => http.expectOne(URL).flush(ejemploDe('grupos', 'historialAdmision', 'ok').cuerpo as object[])],
  ] as const) {
    it(`sin violaciones en el estado ${nombre}`, async () => {
      TestBed.inject(Sesion).abrir('t', permisos, 'oficial', 'bo-1')
      const fixture = TestBed.createComponent(PantallaDeAdmision)
      fixture.componentRef.setInput('solicitudId', SOLICITUD)
      fixture.detectChanges()
      respuesta()
      await fixture.whenStable()
      fixture.detectChanges()
      expect(await axe(fixture.nativeElement)).toHaveNoViolations()
    })
  }

  it('sin violaciones en la puerta de entrada (con su error de campo)', async () => {
    const fixture = TestBed.createComponent(EntradaDeAdmision)
    fixture.detectChanges()
    fixture.componentInstance['abrir']()
    fixture.detectChanges()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
