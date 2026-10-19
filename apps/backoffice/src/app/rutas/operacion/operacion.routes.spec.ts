import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, UrlTree, type CanMatchFn } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { Sesion } from '../../nucleo/sesion'
import { rutasOperacion } from './operacion.routes'

/** Cada ruta nueva del lote L1 monta solo con el permiso que su contrato exige (y redirige sin él). */
function pasa(path: string, permisos: string[]): boolean {
  TestBed.inject(Sesion).abrir('t', permisos, 'r')
  const guardas = (rutasOperacion.find((r) => r.path === path)?.canMatch ?? []) as CanMatchFn[]
  expect(guardas.length).toBeGreaterThan(0)
  return guardas.every((g) => TestBed.runInInjectionContext(() => g({}, [], {} as never)) === true)
}

describe('rutasOperacion · permisos del contrato (lote L1)', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] }))

  it.each([
    ['retiros', 'RETIRO_APROBAR'],
    ['retiros/:ordenId', 'RETIRO_APROBAR'],
    ['custodia', 'AUDITORIA_LEER'],
    ['cierres-diarios', 'AUDITORIA_LEER'],
    ['reversos', 'AUDITORIA_LEER'],
    ['desembolsos', 'AUDITORIA_LEER'],
    ['desembolsos', 'ENTREGA_EJECUTAR'],
  ])('%s monta con %s', (path, permiso) => {
    expect(pasa(path, [permiso])).toBe(true)
  })

  it('sin el permiso no monta: redirige al tablero', () => {
    TestBed.inject(Sesion).abrir('t', ['RECLAMO_VER'], 'r')
    const guarda = (rutasOperacion.find((r) => r.path === 'desembolsos')!.canMatch as CanMatchFn[])[0]!
    expect(TestBed.runInInjectionContext(() => guarda({}, [], {} as never))).toBeInstanceOf(UrlTree)
    expect(pasa('custodia', ['RECLAMO_VER'])).toBe(false)
  })
})
