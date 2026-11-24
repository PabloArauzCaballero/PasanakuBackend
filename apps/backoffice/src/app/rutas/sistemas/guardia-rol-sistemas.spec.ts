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
  it('rol real del token (OPERADOR) con el código de catálogo ADMIN_PLATAFORMA en permisos entra', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', ['ADMIN_PLATAFORMA', 'ACCESOS_ADMINISTRAR'], 'OPERADOR')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).toBe(true)
  })

  it('rol real del token (OPERADOR) con RESPONSABLE_SEGURIDAD en permisos entra', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', ['RESPONSABLE_SEGURIDAD'], 'OPERADOR')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).toBe(true)
  })

  it('OPERADOR con un permiso de sistemas pero sin rol de sistemas (p. ej. CONTABILIDAD) sigue bloqueado', () => {
    configurar()
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('token', ['ACCESOS_ADMINISTRAR', 'CONTADOR'], 'OPERADOR')
    const resultado = TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, {} as never, {} as never))
    expect(resultado).toBeInstanceOf(UrlTree)
  })

  describe('pantallas de «Personal y accesos» (GU-09): las abre el permiso, no el rol de Sistemas', () => {
    const ir = (primero: string) => TestBed.runInInjectionContext(() => soloRolesDeSistemas({} as never, [{ path: primero }] as never, {} as never))

    it('correcto: INTERNAL_IDENTITY_ADMIN (rol OPERADOR, ACCESOS_ADMINISTRAR) abre usuarios-internos, roles y accesos', () => {
      configurar()
      TestBed.inject(Sesion).abrir('token', ['INTERNAL_IDENTITY_ADMIN', 'ACCESOS_ADMINISTRAR', 'USUARIOS_INTERNOS_GESTIONAR'], 'OPERADOR')
      for (const p of ['usuarios-internos', 'roles-internos', 'permisos-internos', 'accesos']) expect(ir(p), p).toBe(true)
    })

    it('límite: con solo USUARIOS_INTERNOS_LEER abre usuarios-internos y el informe, pero no roles ni permisos', () => {
      configurar()
      TestBed.inject(Sesion).abrir('token', ['USUARIOS_INTERNOS_LEER'], 'OPERADOR')
      expect(ir('usuarios-internos')).toBe(true)
      expect(ir('accesos')).toBe(true)
      expect(ir('roles-internos')).not.toBe(true)
      expect(ir('permisos-internos')).not.toBe(true)
    })

    it('inválido: RESPONSABLE_SEGURIDAD entra a Sistemas pero no a Personal y accesos (recibiría 403)', () => {
      configurar()
      TestBed.inject(Sesion).abrir('token', ['RESPONSABLE_SEGURIDAD', 'SEGURIDAD_ACCESO_RESTABLECER'], 'OPERADOR')
      expect(ir('servicios')).toBe(true)
      for (const p of ['usuarios-internos', 'roles-internos', 'permisos-internos', 'accesos']) expect(ir(p), p).toBeInstanceOf(UrlTree)
    })

    it('inválido: ni siquiera el rol PLATAFORMA entra sin el permiso real (la interfaz sigue al servidor)', () => {
      configurar()
      TestBed.inject(Sesion).abrir('token', [], 'PLATAFORMA')
      expect(ir('usuarios-internos')).toBeInstanceOf(UrlTree)
    })
  })
})
