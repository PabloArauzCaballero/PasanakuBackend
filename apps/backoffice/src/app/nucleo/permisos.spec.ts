import { provideRouter, Router } from '@angular/router'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { Sesion } from './sesion'
import { requierePermiso } from './permisos'

describe('requierePermiso · canMatch', () => {
  it('con el permiso, la ruta monta', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    TestBed.runInInjectionContext(() => {
      TestBed.inject(Sesion).abrir('t', ['ver:operacion'], 'oficial')
      const resultado = requierePermiso('ver:operacion')({} as never, [], {} as never)
      expect(resultado).toBe(true)
    })
  })

  it('sin el permiso, redirige — no deja pasar en silencio', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    TestBed.runInInjectionContext(() => {
      TestBed.inject(Sesion).abrir('t', [], 'consulta')
      const resultado = requierePermiso('ver:operacion')({} as never, [], {} as never)
      const router = TestBed.inject(Router)
      expect(resultado).toEqual(router.parseUrl('/tablero'))
    })
  })
})
