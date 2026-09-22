import { provideRouter, Router } from '@angular/router'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { Sesion } from './sesion'
import { requierePermiso, requiereSesion } from './permisos'

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

describe('requiereSesion · canMatch', () => {
  function segmentos(ruta: string) {
    return ruta
      .split('/')
      .filter(Boolean)
      .map((path) => ({ path }) as never)
  }

  it('en AUTHENTICATED, la ruta monta', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    const sesion = TestBed.inject(Sesion)
    sesion.abrir('t', [], 'oficial')
    const resultado = await TestBed.runInInjectionContext(() => firstValueFrom(requiereSesion()({} as never, segmentos('/tablero'), {} as never) as any))
    expect(resultado).toBe(true)
  })

  it('en ANONYMOUS, redirige a /ingreso preservando la ruta pedida en volverA', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.anonima()
    const router = TestBed.inject(Router)
    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(requiereSesion()({} as never, segmentos('/operacion/reclamos'), {} as never) as any),
    )
    expect(resultado).toEqual(router.createUrlTree(['/ingreso'], { queryParams: { volverA: '/operacion/reclamos' } }))
  })

  it('en ERROR, redirige a /arranque — no a un login silencioso', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()
    sesion.fallo()
    const router = TestBed.inject(Router)
    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(requiereSesion()({} as never, segmentos('/cumplimiento/verificaciones'), {} as never) as any),
    )
    expect(resultado).toEqual(router.createUrlTree(['/arranque'], { queryParams: { volverA: '/cumplimiento/verificaciones' } }))
  })

  it('en RESTORING no emite hasta que el estado se resuelve', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([])] })
    const sesion = TestBed.inject(Sesion)
    sesion.restaurando()

    let emitio = false
    const promesa = TestBed.runInInjectionContext(() =>
      firstValueFrom(requiereSesion()({} as never, segmentos('/tablero'), {} as never) as any),
    ).then((v) => {
      emitio = true
      return v
    })

    await Promise.resolve()
    expect(emitio).toBe(false)

    sesion.abrir('t', [], 'oficial')
    const resultado = await promesa
    expect(emitio).toBe(true)
    expect(resultado).toBe(true)
  })
})
