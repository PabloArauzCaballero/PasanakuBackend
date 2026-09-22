import type { Provider } from '@angular/core'
import { ALMACEN_DE_PROGRESO } from '@aportaya/tutoriales/almacen'
import { AlmacenLocal } from '@aportaya/tutoriales/almacen-local'
import { BITACORA_DE_TUTORIALES, BitacoraEnMemoria } from '@aportaya/tutoriales/bitacora'
import { IDENTIDAD_DE_TUTORIALES, type IdentidadDeQuienAprende } from '@aportaya/tutoriales/identidad'
import { PRESENTACION_DEL_CENTRO } from '@aportaya/tutoriales/centro/textos'
import { CARGADORES_DE_TUTORIALES, RUTAS_DEL_PRODUCTO } from '@aportaya/tutoriales/registro'
import { Sesion } from '../../../nucleo/sesion'

/**
 * Las rutas raíz del backoffice, contra las que se valida que ningún tutorial apunte a
 * una pantalla que no existe. Sale de `app.routes.ts`; la prueba del catálogo falla si
 * alguna vez dejan de coincidir.
 */
export const RUTAS_CONOCIDAS: readonly string[] = ['/tablero', '/ingreso', '/operacion', '/cumplimiento', '/sistemas', '/contabilidad', '/publicidad', '/ayuda']

/**
 * Todo lo que hay que enchufar para que los tutoriales funcionen. Un solo lugar, para
 * que encender el almacén remoto el día que exista el endpoint sea cambiar una línea.
 *
 * **Este archivo es lo único de tutoriales que viaja en el arranque**, y a propósito no
 * importa ningún tutorial: el `import()` de abajo se resuelve recién cuando alguien
 * pide ayuda.
 */
export function proveerTutoriales(): Provider[] {
  return [
    { provide: CARGADORES_DE_TUTORIALES, useValue: () => import('./catalogo').then((m) => m.CATALOGO_DE_TUTORIALES), multi: true },
    { provide: RUTAS_DEL_PRODUCTO, useValue: RUTAS_CONOCIDAS },
    // Cambiar a `AlmacenRemoto` cuando `identidad` exponga el progreso (ver la
    // documentación del motor, §11). Ningún componente se entera.
    { provide: ALMACEN_DE_PROGRESO, useClass: AlmacenLocal },
    { provide: BITACORA_DE_TUTORIALES, useClass: BitacoraEnMemoria },
    identidadDelOperador(),
    {
      provide: PRESENTACION_DEL_CENTRO,
      useValue: {
        titulo: 'Centro de tutoriales',
        proposito: 'Aprender a usar el backoffice haciéndolo, sobre las pantallas de verdad. Ningún tutorial toca datos ni confirma operaciones.',
      },
    },
  ]
}

/**
 * **Quién aprende, para el motor: el operador con sesión abierta.** Es el único lugar
 * donde el backoffice se distingue del sitio público, que provee un visitante anónimo
 * contra el mismo token. Se exporta aparte porque las pruebas lo necesitan tal cual.
 */
export function identidadDelOperador(): Provider {
  return {
    provide: IDENTIDAD_DE_TUTORIALES,
    useFactory: (sesion: Sesion): IdentidadDeQuienAprende => ({
      sujeto: () => sesion.sujeto(),
      puede: (permiso) => sesion.puede(permiso),
    }),
    deps: [Sesion],
  }
}
