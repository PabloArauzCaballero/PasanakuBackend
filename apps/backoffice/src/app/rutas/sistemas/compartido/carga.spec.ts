import { ApplicationRef, provideZonelessChangeDetection, resource } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { cargaDe, filasDe, mensajeDeError } from './carga'

function montar<T>(loader: () => Promise<T>) {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  return TestBed.runInInjectionContext(() => {
    const r = resource({ loader })
    return { r, carga: cargaDe(r), filas: filasDe<number, T>(r, (v) => v as unknown as readonly number[]) }
  })
}

describe('compartido/carga · el recurso traducido al estado del listado', () => {
  it('aceptado: con valor, `lista` y las filas', async () => {
    const { carga, filas } = montar(() => Promise.resolve([1, 2, 3]))
    expect(carga().tipo).toBe('cargando')
    await TestBed.inject(ApplicationRef).whenStable()
    expect(carga()).toEqual({ tipo: 'lista' })
    expect(filas()).toEqual([1, 2, 3])
  })

  it('error: el mensaje traducido y el código de seguimiento como idPeticion; sin filas', async () => {
    const { carga, filas } = montar(() => Promise.reject({ mensaje: 'No tenés acceso.', trazaId: 'traza-9' }))
    await TestBed.inject(ApplicationRef).whenStable()
    expect(carga()).toEqual({ tipo: 'error', mensaje: 'No tenés acceso.', idPeticion: 'traza-9' })
    expect(filas()).toEqual([])
  })

  it('límite: un error sin forma conocida nunca muestra el crudo', () => {
    expect(mensajeDeError(new Error('ECONNRESET 10.0.0.3')).mensaje).not.toContain('ECONNRESET')
    expect(mensajeDeError({ mensaje: 5 }).mensaje).toContain('Probá de nuevo')
  })
})

