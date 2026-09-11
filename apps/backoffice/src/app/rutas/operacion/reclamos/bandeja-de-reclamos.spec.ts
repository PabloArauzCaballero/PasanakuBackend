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
import { BandejaDeReclamos } from './bandeja-de-reclamos'
import type { ReclamoDeBandeja } from '../dominio/cu52-reclamos'

const URL = 'http://gw/api/v1/reclamos'

const RECLAMO: ReclamoDeBandeja = {
  reclamoId: 'r-1',
  codigo: 'REC-2026-08-0157',
  categoria: 'COMISION',
  canalIngreso: 'APP',
  montoReclamado: { monto: '120.00', moneda: 'BOB' },
  fechaIngreso: '2026-08-01T10:00:00-04:00',
  plazoRespuesta: '2026-08-06T10:00:00-04:00',
  diasHabilesPlazo: 5,
  estado: 'EN_ANALISIS',
  responsableId: 'op-1',
  responsableNombre: 'Ana',
}

async function montar(rol: 'sin-permiso' | 'RECLAMO_ATENDER') {
  const fixture = TestBed.createComponent(BandejaDeReclamos)
  if (rol === 'RECLAMO_ATENDER') TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER', 'RECLAMO_ATENDER'], 'operador')
  else TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER'], 'lector')
  fixture.detectChanges()
  return fixture
}

/**
 * El orden por vencimiento y el filtro por estado del `cargador` están probados de
 * forma aislada, sin CDK de por medio, en `dominio/cu52-reclamos.spec.ts`. Estas
 * pruebas cubren lo que es propio de la pantalla: la guarda de permiso y el diálogo de
 * confirmación con el dato concreto delante — ninguna de las dos depende de que el
 * `cdk-virtual-scroll-viewport` mida y renderice filas, que en jsdom no ocurre (mismo
 * criterio que ya usa `nucleo/tabla/tabla-de-datos-virtualizada.spec.ts`).
 */
describe('BandejaDeReclamos', () => {
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

  it('segregación de funciones: sin RECLAMO_ATENDER la guarda que monta la acción da false', async () => {
    const fixture = await montar('sin-permiso')
    http.expectOne(URL).flush([RECLAMO])
    await fixture.whenStable()
    expect(fixture.componentInstance['puedeAtender']()).toBe(false)
  })

  it('con RECLAMO_ATENDER la guarda da true, y el diálogo de responder muestra el código concreto delante, no un "¿estás seguro?" genérico', async () => {
    const fixture = await montar('RECLAMO_ATENDER')
    http.expectOne(URL).flush([RECLAMO])
    await fixture.whenStable()
    expect(fixture.componentInstance['puedeAtender']()).toBe(true)
    fixture.componentInstance.abrirConfirmacion(RECLAMO)
    fixture.detectChanges()
    const dialogo = fixture.nativeElement.querySelector('ap-dialogo')
    expect(dialogo.textContent).toContain('REC-2026-08-0157')
  })

  it('confirmar cierra el diálogo', async () => {
    const fixture = await montar('RECLAMO_ATENDER')
    http.expectOne(URL).flush([RECLAMO])
    await fixture.whenStable()
    fixture.componentInstance.abrirConfirmacion(RECLAMO)
    fixture.detectChanges()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(true)
    fixture.componentInstance.confirmarRespuesta()
    expect(fixture.componentInstance['dialogoAbierto']()).toBe(false)
  })
})
