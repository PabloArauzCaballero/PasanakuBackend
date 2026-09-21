import { Component, provideZonelessChangeDetection, type EnvironmentProviders, type Provider } from '@angular/core'
import { provideRouter } from '@angular/router'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { ATRIBUTO } from '@aportaya/tutoriales/objetivo'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'
import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { identidadDelOperador } from '../../rutas/ayuda/catalogo/proveer'

/**
 * El andamiaje que comparten las pruebas del motor y del anfitrión: dos pantallas de
 * mentira, el catálogo que se le pase y los cuatro proveedores del motor.
 *
 * Vive fuera de un `.spec.ts` porque lo usan varios, y con el nombre de lo que hace.
 * No entra en el paquete de la aplicación: nadie de `src/app` lo importa.
 */
@Component({ template: 'tablero' })
export class PantallaDePrueba {}

@Component({ template: 'operacion' })
export class OtraPantallaDePrueba {}

export function proveedoresDeTutoriales(catalogo: readonly TutorialDefinicion[]): (Provider | EnvironmentProviders)[] {
  return [
    provideZonelessChangeDetection(),
    provideRouter([
      { path: 'tablero', component: PantallaDePrueba },
      { path: 'operacion', component: OtraPantallaDePrueba },
    ]),
    { provide: CARGADORES_DE_TUTORIALES, useValue: () => Promise.resolve(catalogo), multi: true },
    { provide: RUTAS_DEL_PRODUCTO, useValue: ['/tablero', '/operacion'] },
    { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
    { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
    identidadDelOperador(),
  ]
}

/** Pone en el documento un elemento que el tutorial pueda señalar. */
export function sembrarObjetivo(id: string): HTMLElement {
  const div = document.createElement('div')
  div.setAttribute(ATRIBUTO, id)
  document.body.appendChild(div)
  return div
}

const paso = (id: string, titulo: string, extra: Partial<TutorialDefinicion['pasos'][number]> = {}) => ({ id, titulo, descripcion: 'd', ...extra })

export const TRES_PASOS: TutorialDefinicion = {
  id: 'tres',
  version: '1.0.0',
  titulo: 'Tres pasos',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/tablero',
  dificultad: 'inicial',
  pasos: [paso('p1', 'Primero esto', { objetivo: 'uno' }), paso('p2', 'Después esto', { objetivo: 'dos' }), paso('p3', 'Y listo', { objetivo: 'tres' })],
}

export const CON_ACCION: TutorialDefinicion = {
  ...TRES_PASOS,
  id: 'con-accion',
  pasos: [paso('p1', 'Uno', { objetivo: 'uno', accion: { tipo: 'clic' }, ayudaSiFalla: 'Pulsá el botón' }), paso('p2', 'Dos', { objetivo: 'dos' })],
}

export const OTRA_RUTA: TutorialDefinicion = {
  ...TRES_PASOS,
  id: 'viaja',
  pasos: [paso('p1', 'Uno', { objetivo: 'uno', ruta: '/tablero' }), paso('p2', 'Dos', { objetivo: 'dos', ruta: '/operacion' })],
}

export const SIN_OBJETIVO: TutorialDefinicion = {
  ...TRES_PASOS,
  id: 'fantasma',
  pasos: [paso('p1', 'Uno', { objetivo: 'no-existe-en-ningun-lado', esperaMs: 20 })],
}

export const TARDIO: TutorialDefinicion = {
  ...TRES_PASOS,
  id: 'tardio',
  pasos: [paso('p1', 'Uno', { objetivo: 'llega-despues', esperaMs: 2000 })],
}

export const CATALOGO_DE_PRUEBA = [TRES_PASOS, CON_ACCION, OTRA_RUTA, SIN_OBJETIVO, TARDIO]
