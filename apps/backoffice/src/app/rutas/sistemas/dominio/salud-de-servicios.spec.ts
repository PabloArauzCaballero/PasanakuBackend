import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { SaludDeServicioEstadoEnum as Salud } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeSalud, puertoSaludDeServicios, SIN_DATO } from './salud-de-servicios'

const GW = 'https://gateway.test/api/v1'

describe('salud de servicios · Sistemas › Servicios contra GET /indicadores/operacion/servicios', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
  })

  it('correcto: ARRIBA, DEGRADADO y CAIDO pasan a operativo, degradado y caído; lo que el contrato no trae queda en «—»', () => {
    const filas = filasDeSalud({
      consultadoEn: '2026-10-27T16:00:00Z',
      servicios: [
        { servicio: 'aportes', estado: Salud.Arriba, latenciaMs: 12 },
        { servicio: 'garantia', estado: Salud.Degradado, latenciaMs: 40 },
        { servicio: 'entregas', estado: Salud.Caido },
      ],
    })
    expect(filas.map((f) => [f.nombre, f.estado])).toEqual([['aportes', 'operativo'], ['garantia', 'degradado'], ['entregas', 'caido']])
    expect(filas[0]).toMatchObject({ disponibilidad30d: SIN_DATO, presupuestoErrorRestante: SIN_DATO, ultimaInterrupcion: SIN_DATO })
  })

  it('límite: sin servicios en la respuesta, la pantalla recibe una lista vacía (su vacío, no un error)', async () => {
    const puerto = TestBed.runInInjectionContext(puertoSaludDeServicios)
    const promesa = puerto.obtener()
    TestBed.inject(HttpTestingController).expectOne(`${GW}/indicadores/operacion/servicios`).flush({ consultadoEn: '2026-10-27T16:00:00Z', servicios: [] })
    await expect(promesa).resolves.toEqual([])
  })

  it('inválido: un 403 del gateway rechaza la promesa (la pantalla muestra su error con reintento)', async () => {
    const puerto = TestBed.runInInjectionContext(puertoSaludDeServicios)
    const promesa = puerto.obtener()
    TestBed.inject(HttpTestingController).expectOne(`${GW}/indicadores/operacion/servicios`).flush({}, { status: 403, statusText: 'Forbidden' })
    await expect(promesa).rejects.toBeTruthy()
  })
})
