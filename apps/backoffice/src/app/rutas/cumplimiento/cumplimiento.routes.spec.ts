import { Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { describe, expect, it } from 'vitest'
import { Sesion } from '../../nucleo/sesion'
import { rutasCumplimiento } from './cumplimiento.routes'

@Component({ template: '' })
class Tablero {}

/**
 * Un listado con su `canMatch` (p. ej. `casos`, CUMPLIMIENTO_CASOS) no puede interceptar por prefijo la ruta de su detalle
 * (`casos/:casoId`): sin `pathMatch: 'full'`, la guarda del listado corría sobre `casos/x` y mandaba al tablero antes de probar
 * la ruta del detalle (lo encontró `e2e/caso-de-cumplimiento.e2e.ts` en la regresión del carril B).
 */
async function navegar(url: string, permisos: string[]): Promise<string> {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([{ path: 'cumplimiento', children: rutasCumplimiento }, { path: 'tablero', component: Tablero }]),
    ],
  })
  TestBed.inject(Sesion).abrir('t', permisos, 'r')
  const router = TestBed.inject(Router)
  await router.navigateByUrl(url)
  return router.url
}

describe('rutasCumplimiento · el listado no tapa la ruta de su detalle', () => {
  it.each([
    ['/cumplimiento/casos/caso-1', ['VERIFICACION_RESOLVER']],
    ['/cumplimiento/alertas/alerta-1', ['VERIFICACION_RESOLVER']],
    ['/cumplimiento/actas/acta-1', ['VERIFICACION_RESOLVER']],
  ])('%s se abre sin el permiso del listado', async (url, permisos) => {
    expect(await navegar(url, permisos)).toBe(url)
  })

  it('el listado sin su permiso sigue redirigiendo al tablero', async () => {
    expect(await navegar('/cumplimiento/casos', ['VERIFICACION_RESOLVER'])).toBe('/tablero')
  })
})

describe('rutasCumplimiento · bloqueos de autoridad (CU-17)', () => {
  it.each([['BLOQUEO_AUTORIDAD_REGISTRAR'], ['BLOQUEO_AUTORIDAD_LEVANTAR']])('con %s se entra a la bandeja', async (permiso) => {
    expect(await navegar('/cumplimiento/bloqueos', [permiso])).toBe('/cumplimiento/bloqueos')
  })

  it('sin ninguno de los dos códigos de su listado, al tablero', async () => {
    expect(await navegar('/cumplimiento/bloqueos', ['CUMPLIMIENTO_CASOS'])).toBe('/tablero')
  })
})

describe('rutasCumplimiento · participantes (CU-134)', () => {
  it('con PARTICIPANTES_LEER se entra al directorio', async () => {
    expect(await navegar('/cumplimiento/participantes', ['PARTICIPANTES_LEER'])).toBe('/cumplimiento/participantes')
  })

  it('con PARTICIPANTES_LEER se entra a la ficha', async () => {
    expect(await navegar('/cumplimiento/participantes/u-1', ['PARTICIPANTES_LEER'])).toBe('/cumplimiento/participantes/u-1')
  })

  it('sin PARTICIPANTES_LEER, al tablero', async () => {
    expect(await navegar('/cumplimiento/participantes', ['CUMPLIMIENTO_CASOS'])).toBe('/tablero')
  })
})
