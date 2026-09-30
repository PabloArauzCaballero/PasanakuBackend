import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, vi } from 'vitest'
import { ES_FUENTE_SIMULADA, provideFuentesDeSistemas } from './proveedor-fuentes.demo'
import { PUERTO_SERVICIOS } from './puertos'

vi.mock('@aportaya/simulado/backoffice-sistemas', () => ({
  adaptadorSimuladoSistemas: {
    servicios: () => Promise.resolve([{ id: 'x', nombre: 'x', estado: 'operativo', disponibilidad30d: '99.9%', presupuestoErrorRestante: '1%', ultimaInterrupcion: '-' }]),
  },
}))

describe('provideFuentesDeSistemas (versión demo) · elige por modo explícito, no por isDevMode()', () => {
  it('en modo demo: el puerto resuelve con datos y ES_FUENTE_SIMULADA es true', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...provideFuentesDeSistemas('demo')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(true)
    const datos = await TestBed.inject(PUERTO_SERVICIOS).obtener()
    expect(datos).toHaveLength(1)
  })

  it('en modo producción (aunque este archivo tenga el simulado disponible): fuente no disponible', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), ...provideFuentesDeSistemas('produccion')] })
    expect(TestBed.inject(ES_FUENTE_SIMULADA)).toBe(false)
    await expect(TestBed.inject(PUERTO_SERVICIOS).obtener()).rejects.toMatchObject({ tipo: 'fuente-no-disponible' })
  })
})
