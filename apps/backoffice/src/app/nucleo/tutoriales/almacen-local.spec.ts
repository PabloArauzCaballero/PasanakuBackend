import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { firstValueFrom } from 'rxjs'
import { Sesion } from '../sesion'
import { AlmacenLocal, huella } from '@aportaya/tutoriales/almacen-local'
import type { ProgresoDeTutorial } from '@aportaya/tutoriales/tipos'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

const FILA: ProgresoDeTutorial = {
  tutorialId: 'intro',
  version: '1.0.0',
  estado: 'en-progreso',
  pasoId: 'p2',
  indice: 1,
  iniciadoEn: '2026-09-17T12:00:00.000Z',
  terminadoEn: null,
  ultimaInteraccion: '2026-09-17T12:01:00.000Z',
  repeticiones: 0,
}

function montar(sujeto: string): AlmacenLocal {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(),
      identidadDelOperador(), AlmacenLocal] })
  TestBed.inject(Sesion).abrir('token', [], 'rol', sujeto)
  return TestBed.inject(AlmacenLocal)
}

describe('AlmacenLocal', () => {
  beforeEach(() => localStorage.clear())

  it('guarda y devuelve lo guardado', async () => {
    const almacen = montar('op-1')
    await firstValueFrom(almacen.guardar(FILA))
    expect(await firstValueFrom(almacen.leer())).toEqual([FILA])
  })

  it('guardar dos veces el mismo tutorial deja una sola fila, la última', async () => {
    const almacen = montar('op-1')
    await firstValueFrom(almacen.guardar(FILA))
    await firstValueFrom(almacen.guardar({ ...FILA, indice: 3, pasoId: 'p4' }))
    const filas = await firstValueFrom(almacen.leer())
    expect(filas).toHaveLength(1)
    expect(filas[0]?.indice).toBe(3)
  })

  it('reiniciar borra la fila de ese tutorial y deja el resto', async () => {
    const almacen = montar('op-1')
    await firstValueFrom(almacen.guardar(FILA))
    await firstValueFrom(almacen.guardar({ ...FILA, tutorialId: 'otro' }))
    await firstValueFrom(almacen.reiniciar('intro'))
    expect((await firstValueFrom(almacen.leer())).map((p) => p.tutorialId)).toEqual(['otro'])
  })

  it('dos operadores en la misma máquina no comparten avance', async () => {
    const primero = montar('op-1')
    await firstValueFrom(primero.guardar(FILA))
    const segundo = montar('op-2')
    expect(await firstValueFrom(segundo.leer())).toEqual([])
  })

  it('la clave no lleva el identificador del operador en claro', async () => {
    const almacen = montar('9f2c1e4a-0000-4000-8000-000000000001')
    await firstValueFrom(almacen.guardar(FILA))
    const claves = Object.keys(localStorage)
    expect(claves.some((c) => c.includes('9f2c1e4a'))).toBe(false)
    expect(claves.some((c) => c.endsWith(huella('9f2c1e4a-0000-4000-8000-000000000001')))).toBe(true)
  })

  it('un contenido ilegible no rompe la pantalla: se empieza de cero', async () => {
    const almacen = montar('op-1')
    localStorage.setItem(`aportaya.tutoriales.${huella('op-1')}`, '{no es json')
    expect(await firstValueFrom(almacen.leer())).toEqual([])
  })

  it('descarta filas que no tienen forma de progreso', async () => {
    const almacen = montar('op-1')
    localStorage.setItem(`aportaya.tutoriales.${huella('op-1')}`, JSON.stringify([FILA, { basura: true }]))
    expect(await firstValueFrom(almacen.leer())).toEqual([FILA])
  })
})
