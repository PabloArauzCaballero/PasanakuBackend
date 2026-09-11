import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { ColaDeSolicitudesEscaladas } from './cola-de-solicitudes-escaladas'
import type { SolicitudEscalada } from '../dominio/d15-solicitudes-escaladas'

const URL = 'http://gw/api/v1/grupos/solicitudes-escaladas'

const VENCE_PRIMERO: SolicitudEscalada = {
  solicitudId: 's-1',
  grupoId: 'g-1',
  grupoCodigo: 'PSK-0042',
  usuarioId: 'u-1',
  usuarioNombre: 'Marcelo Rojas',
  canal: 'QR',
  puntajeCompatibilidad: 712,
  factores: [
    { motivo: '2 pasanakus completos', aFavor: true },
    { motivo: '14 aportes en fecha', aFavor: true },
  ],
  fechaLimiteOrganizador: '2026-09-12T10:00:00-04:00',
  escaladaEn: '2026-09-10T10:00:00-04:00',
  estado: 'ESCALADA',
}

const VENCE_DESPUES: SolicitudEscalada = { ...VENCE_PRIMERO, solicitudId: 's-2', usuarioNombre: 'Julia Pérez', fechaLimiteOrganizador: '2026-09-20T10:00:00-04:00' }

async function montar(permiso: 'con-permiso' | 'sin-permiso') {
  const fixture = TestBed.createComponent(ColaDeSolicitudesEscaladas)
  TestBed.inject(Sesion).abrir('t', permiso === 'con-permiso' ? ['SOLICITUD_INGRESO_VER', 'SOLICITUD_INGRESO_RESOLVER'] : ['SOLICITUD_INGRESO_VER'], 'operador')
  fixture.detectChanges()
  return fixture
}

describe('ColaDeSolicitudesEscaladas', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('ordena la cola por el plazo del organizador: el que vence antes va primero', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([VENCE_DESPUES, VENCE_PRIMERO])
    await fixture.whenStable()
    const nombres = [...fixture.nativeElement.querySelectorAll('li strong')].map((e: HTMLElement) => e.textContent)
    expect(nombres).toEqual(['Marcelo Rojas', 'Julia Pérez'])
  })

  it('el puntaje llega descompuesto en factores, nunca como número suelto', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([VENCE_PRIMERO])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('2 pasanakus completos')
    expect(fixture.nativeElement.textContent).not.toContain('712')
  })

  it('segregación de funciones: sin SOLICITUD_INGRESO_RESOLVER no aparecen las acciones', async () => {
    const fixture = await montar('sin-permiso')
    http.expectOne(URL).flush([VENCE_PRIMERO])
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelectorAll('.acciones').length).toBe(0)
  })

  it('rechazar exige motivo escrito: no deja confirmar en blanco', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([VENCE_PRIMERO])
    await fixture.whenStable()
    const botones = [...fixture.nativeElement.querySelectorAll('.acciones ap-boton button')] as HTMLButtonElement[]
    botones[1]!.click() // Rechazar solicitud
    fixture.detectChanges()
    const confirmar = fixture.nativeElement.querySelector('ap-dialogo .acciones ap-boton:last-child button, dialog .acciones ap-boton:last-child button') as HTMLButtonElement
    confirmar.click()
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Escribí el motivo')
    http.expectNone(URL + '/s-1/resolucion')
  })

  it('aceptar es idempotente en la interfaz: el diálogo dice la acción exacta con el nombre y el grupo', async () => {
    const fixture = await montar('con-permiso')
    http.expectOne(URL).flush([VENCE_PRIMERO])
    await fixture.whenStable()
    const botones = [...fixture.nativeElement.querySelectorAll('.acciones ap-boton button')] as HTMLButtonElement[]
    botones[0]!.click() // Aceptar solicitud
    fixture.detectChanges()
    const dialogo = fixture.nativeElement.querySelector('ap-dialogo')
    expect(dialogo.textContent).toContain('Marcelo Rojas')
    expect(dialogo.textContent).toContain('PSK-0042')
  })
})
