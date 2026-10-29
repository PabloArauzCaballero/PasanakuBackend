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

  it('correcto: ARRIBA, DEGRADADO y CAIDO pasan a operativo, degradado y caído; sin nada medido, las tres columnas quedan en «—»', () => {
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

  it('correcto: lo medido se muestra con coma decimal y la interrupción en hora de La Paz', () => {
    const [fila] = filasDeSalud({
      consultadoEn: '2026-10-29T16:00:00Z',
      objetivoSlo: '99.5',
      servicios: [
        {
          servicio: 'identidad',
          estado: Salud.Arriba,
          disponibilidad30d: '99.90',
          presupuestoErrorRestante: '80.0',
          minutosMedidos30d: 1000,
          ultimaInterrupcion: '2026-10-20T03:04:00Z',
        },
      ],
    })
    expect(fila).toMatchObject({
      disponibilidad30d: '99,9 %',
      presupuestoErrorRestante: '80 %',
      ultimaInterrupcion: '19 oct 2026, 23:04 (La Paz)',
      ultimaInterrupcionIso: '2026-10-20T03:04:00Z',
    })
  })

  it('límite: SLO incumplido muestra el presupuesto negativo; nunca interrumpido queda en «—»', () => {
    const [fila] = filasDeSalud({
      consultadoEn: '2026-10-29T16:00:00Z',
      servicios: [{ servicio: 'erp', estado: Salud.Arriba, disponibilidad30d: '100.00', presupuestoErrorRestante: '-1250.5' }],
    })
    expect(fila).toMatchObject({ disponibilidad30d: '100 %', presupuestoErrorRestante: '-1.250,5 %', ultimaInterrupcion: SIN_DATO })
    expect(fila!.ultimaInterrupcionIso).toBeUndefined()
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
