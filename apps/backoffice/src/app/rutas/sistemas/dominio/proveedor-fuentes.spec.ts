import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ES_FUENTE_SIMULADA, provideFuentesDeSistemas } from './proveedor-fuentes'
import { PUERTO_SERVICIOS } from './puertos'

/**
 * Este archivo es el que se compila en `production` (nunca reemplazado por
 * `fileReplacements`, ver `angular.json`): en los dos modos, sin excepción, entrega
 * la fuente no disponible. `proveedor-fuentes.demo.spec.ts` prueba la otra mitad
 * (H2.S2.M1: "spec del proveedor en los dos modos").
 */
describe('provideFuentesDeSistemas (versión de producción) · siempre fuente no disponible', () => {
  it('en modo producción: el puerto rechaza y ES_FUENTE_SIMULADA es false', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...provideFuentesDeSistemas('produccion')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(false)
    await expect(TestBed.inject(PUERTO_SERVICIOS).obtener()).rejects.toMatchObject({ tipo: 'fuente-no-disponible' })
  })

  it('incluso pidiendo modo demo, esta versión no tiene simulado y sigue rechazando', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...provideFuentesDeSistemas('demo')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(false)
    await expect(TestBed.inject(PUERTO_SERVICIOS).obtener()).rejects.toMatchObject({ tipo: 'fuente-no-disponible' })
  })
})
