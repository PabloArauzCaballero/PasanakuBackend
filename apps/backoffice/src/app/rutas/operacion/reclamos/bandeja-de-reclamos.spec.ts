import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import { BandejaDeReclamos } from './bandeja-de-reclamos'
import type { ReclamoDeBandeja } from '../dominio/cu52-reclamos'

const URL = 'http://gw/api/v1/reclamos'

const RECLAMO_QUE_VENCE_PRIMERO: ReclamoDeBandeja = {
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

const RECLAMO_QUE_VENCE_DESPUES: ReclamoDeBandeja = {
  ...RECLAMO_QUE_VENCE_PRIMERO,
  reclamoId: 'r-2',
  codigo: 'REC-2026-08-0158',
  plazoRespuesta: '2026-08-20T10:00:00-04:00',
}

async function montar(rol: 'sin-permiso' | 'RECLAMO_ATENDER') {
  const fixture = TestBed.createComponent(BandejaDeReclamos)
  if (rol === 'RECLAMO_ATENDER') TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER', 'RECLAMO_ATENDER'], 'operador')
  else TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER'], 'lector')
  fixture.detectChanges()
  return fixture
}

describe('BandejaDeReclamos', () => {
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

  it('ordena la bandeja por vencimiento: el que vence antes va primero', async () => {
    const fixture = await montar('RECLAMO_ATENDER')
    http.expectOne(URL).flush([RECLAMO_QUE_VENCE_DESPUES, RECLAMO_QUE_VENCE_PRIMERO])
    await fixture.whenStable()
    const codigos = [...fixture.nativeElement.querySelectorAll('li strong')].map((e: HTMLElement) => e.textContent)
    expect(codigos).toEqual(['REC-2026-08-0157', 'REC-2026-08-0158'])
  })

  it('segregación de funciones: sin RECLAMO_ATENDER no aparece el botón de responder', async () => {
    const fixture = await montar('sin-permiso')
    http.expectOne(URL).flush([RECLAMO_QUE_VENCE_PRIMERO])
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('ap-boton')).toBeNull()
  })

  it('con RECLAMO_ATENDER, responder abre un diálogo con el código concreto delante, no un "¿estás seguro?" genérico', async () => {
    const fixture = await montar('RECLAMO_ATENDER')
    http.expectOne(URL).flush([RECLAMO_QUE_VENCE_PRIMERO])
    await fixture.whenStable()
    ;(fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement).click()
    fixture.detectChanges()
    const dialogo = fixture.nativeElement.querySelector('ap-dialogo')
    expect(dialogo.textContent).toContain('REC-2026-08-0157')
  })

  it('bandeja vacía: lo dice, sin lista', async () => {
    const fixture = await montar('RECLAMO_ATENDER')
    http.expectOne(URL).flush([])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('No hay reclamos abiertos.')
  })
})
