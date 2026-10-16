import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, beforeEach } from 'vitest'
import { Sesion } from './sesion'

function crear(): Sesion {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  return TestBed.inject(Sesion)
}

describe('Sesion · máquina de estados', () => {
  let sesion: Sesion

  beforeEach(() => {
    sesion = crear()
  })

  it('arranca en UNKNOWN, con abierta() en false', () => {
    expect(sesion.estado()).toBe('UNKNOWN')
    expect(sesion.abierta()).toBe(false)
  })

  it('restaurando(): UNKNOWN → RESTORING', () => {
    sesion.restaurando()
    expect(sesion.estado()).toBe('RESTORING')
  })

  it('abrir() desde RESTORING → AUTHENTICATED, y abierta() pasa a true', () => {
    sesion.restaurando()
    sesion.abrir('t', ['x'], 'oficial')
    expect(sesion.estado()).toBe('AUTHENTICATED')
    expect(sesion.abierta()).toBe(true)
  })

  it('anonima() desde RESTORING → ANONYMOUS, y abierta() sigue en false', () => {
    sesion.restaurando()
    sesion.anonima()
    expect(sesion.estado()).toBe('ANONYMOUS')
    expect(sesion.abierta()).toBe(false)
  })

  it('fallo() desde RESTORING → ERROR', () => {
    sesion.restaurando()
    sesion.fallo()
    expect(sesion.estado()).toBe('ERROR')
  })

  it('cerrar(): AUTHENTICATED → ANONYMOUS', () => {
    sesion.restaurando()
    sesion.abrir('t', [], 'oficial')
    sesion.cerrar()
    expect(sesion.estado()).toBe('ANONYMOUS')
    expect(sesion.abierta()).toBe(false)
  })

  it('restaurando() desde ERROR → RESTORING (reintento)', () => {
    sesion.restaurando()
    sesion.fallo()
    sesion.restaurando()
    expect(sesion.estado()).toBe('RESTORING')
  })

  it('no se puede pasar de UNKNOWN a ANONYMOUS sin restaurar', () => {
    expect(() => sesion.anonima()).toThrow(/transición de arranque inválida/)
    expect(sesion.estado()).toBe('UNKNOWN')
  })
})
