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
import { ColaDeSolicitudesEscaladas } from './cola-de-solicitudes-escaladas'
import type { SolicitudEscalada } from '../dominio/d15-solicitudes-escaladas'

const URL = 'http://gw/api/v1/grupos/solicitudes-escaladas'
const SOLICITUD: SolicitudEscalada = {
  solicitudId: 's-1',
  grupoId: 'g-1',
  grupoCodigo: 'PSK-0042',
  usuarioId: 'u-1',
  usuarioNombre: 'Marcelo Rojas',
  canal: 'QR',
  puntajeCompatibilidad: 712,
  factores: [{ motivo: '2 pasanakus completos', aFavor: true }],
  fechaLimiteOrganizador: '2026-09-12T10:00:00-04:00',
  escaladaEn: '2026-09-10T10:00:00-04:00',
  estado: 'ESCALADA',
}

describe('ColaDeSolicitudesEscaladas · accesibilidad', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('sin violaciones serias con la cola cargada', async () => {
    TestBed.inject(Sesion).abrir('t', ['SOLICITUD_INGRESO_VER', 'SOLICITUD_INGRESO_RESOLVER'], 'operador')
    const fixture = TestBed.createComponent(ColaDeSolicitudesEscaladas)
    fixture.detectChanges()
    http.expectOne(URL).flush([SOLICITUD])
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
