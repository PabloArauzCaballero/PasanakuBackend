import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ES_FUENTE_SIMULADA, provideFuentesDeSistemas } from './proveedor-fuentes'
import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { GATEWAY } from '../../../nucleo/gateway'
import { PUERTO_DESPLIEGUES, PUERTO_SERVICIOS } from './puertos'

/**
 * Este archivo es el que se compila en `production` (nunca reemplazado por
 * `fileReplacements`, ver `angular.json`): en los dos modos, sin excepción, nunca entrega
 * datos de ejemplo. Servicios lee su contrato real (auditoria, desde 2026-10-27); los
 * puertos sin contrato entregan la fuente no disponible. `proveedor-fuentes.demo.spec.ts` prueba la otra mitad
 * (H2.S2.M1: "spec del proveedor en los dos modos").
 */
describe('provideFuentesDeSistemas (versión de producción) · nunca datos de ejemplo', () => {
  it('en modo producción: un puerto sin contrato rechaza y ES_FUENTE_SIMULADA es false', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'https://gw.test' }, ...provideFuentesDeSistemas('produccion')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(false)
    await expect(TestBed.inject(PUERTO_DESPLIEGUES).obtener()).rejects.toMatchObject({ tipo: 'fuente-no-disponible' })
  })

  it('en modo producción: servicios pide la salud real a auditoria, no un simulado', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'https://gw.test' }, ...provideFuentesDeSistemas('produccion')] })
    const promesa = TestBed.inject(PUERTO_SERVICIOS).obtener()
    TestBed.inject(HttpTestingController).expectOne('https://gw.test/indicadores/operacion/servicios').flush({ consultadoEn: '2026-10-27T16:00:00Z', servicios: [] })
    await expect(promesa).resolves.toEqual([])
  })

  it('incluso pidiendo modo demo, esta versión no tiene simulado y sigue rechazando', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'https://gw.test' }, ...provideFuentesDeSistemas('demo')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(false)
    await expect(TestBed.inject(PUERTO_DESPLIEGUES).obtener()).rejects.toMatchObject({ tipo: 'fuente-no-disponible' })
  })
})
