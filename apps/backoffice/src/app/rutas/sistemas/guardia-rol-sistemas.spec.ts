import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, UrlTree } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../nucleo/sesion'
import { soloRolesDeSistemas } from './guardia-rol-sistemas'

/**
 * GATE del carril B5: "un rol financiero no ve este backoffice". Se prueba simulando
 * el rol y verificando que `canMatch` lo bloquea (barrera de interfaz). La segunda
 * barrera —el 403 del servidor simulado— la prueba cada pantalla contra su endpoint.
 */
describe('soloRolesDeSistemas · barrera de interfaz', () => {
  function configurar() {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
  }

  it('un rol financiero (ORGANIZADOR) es bloqueado y redirigido, nunca entra', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', [], 'ORGANIZADOR')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).not.toBe(true)
    expect(resultado).toBeInstanceOf(UrlTree)
  })

  it('sin sesión (rol nulo) es bloqueado', () => {
    configurar()
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).not.toBe(true)
  })

  it('rol PLATAFORMA entra', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', [], 'PLATAFORMA')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).toBe(true)
  })

  it('rol SEGURIDAD entra', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', [], 'SEGURIDAD')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).toBe(true)
  })
})
