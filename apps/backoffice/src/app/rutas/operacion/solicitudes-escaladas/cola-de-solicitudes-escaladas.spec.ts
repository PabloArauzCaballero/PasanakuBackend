import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
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

async function montar(permiso: 'con-permiso' | 'sin-permiso') {
  const fixture = TestBed.createComponent(ColaDeSolicitudesEscaladas)
  TestBed.inject(Sesion).abrir('t', permiso === 'con-permiso' ? ['SOLICITUD_INGRESO_VER', 'SOLICITUD_INGRESO_RESOLVER'] : ['SOLICITUD_INGRESO_VER'], 'operador')
  fixture.detectChanges()
  return fixture
}

/**
 * El orden por plazo del `cargador` está probado de forma aislada, sin CDK, en
 * `dominio/d15-solicitudes-escaladas.spec.ts`. Estas pruebas cubren lo propio de la
 * pantalla: la guarda de permiso y las reglas del diálogo (dato concreto delante,
 * motivo obligatorio para rechazar).
 */
describe('ColaDeSolicitudesEscaladas', () => {
  let http: HttpTestingController

  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('segregación de funciones: sin SOLICITUD_INGRESO_RESOLVER la guarda que monta las acciones da false', async () => {
    const fixture = await montar('sin-permiso')
    http.expectOne(URL).flush([SOLICITUD])
    await fixture.whenStable()
    expect(fixture.componentInstance['puedeResolver']()).toBe(false)
  })

  it('con SOLICITUD_INGRESO_RESOLVER la guarda da true, y aceptar abre el diálogo con el nombre y el grupo delante', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([SOLICITUD])
    await fixture.whenStable()
    expect(fixture.componentInstance['puedeResolver']()).toBe(true)
    fixture.componentInstance.abrir(SOLICITUD, 'ACEPTAR')
    fixture.detectChanges()
    const dialogo = fixture.nativeElement.querySelector('ap-dialogo')
    expect(dialogo.textContent).toContain('Marcelo Rojas')
    expect(dialogo.textContent).toContain('PSK-0042')
  })

  it('rechazar exige motivo escrito: confirmar en blanco no cierra el diálogo', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([SOLICITUD])
    await fixture.whenStable()
    fixture.componentInstance.abrir(SOLICITUD, 'RECHAZAR')
    fixture.componentInstance.confirmar()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(true)
    expect(fixture.componentInstance['motivoVacio']()).toBe(true)
  })

  it('rechazar con motivo escrito sí cierra el diálogo', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([SOLICITUD])
    await fixture.whenStable()
    fixture.componentInstance.abrir(SOLICITUD, 'RECHAZAR')
    fixture.componentInstance['motivo'].set('No cumple el mínimo de aportes en fecha')
    fixture.componentInstance.confirmar()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(false)
  })
})
