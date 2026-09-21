import { Routes } from '@angular/router'

/**
 * Las rutas del centro de tutoriales. Cuelga del shell como cualquier otro dominio y se
 * enchufa en `app.routes.ts` con un solo `loadChildren`.
 *
 * No lleva `canMatch` de permiso: `ver:ayuda` lo tiene toda sesión abierta (ver
 * `nucleo/secciones.ts`). Lo que se filtra por rol es el catálogo, no la puerta.
 */
export const rutasAyuda: Routes = [
  {
    path: '',
    loadComponent: () => import('@aportaya/tutoriales/centro/pantalla-centro-de-ayuda').then((m) => m.PantallaCentroDeAyuda),
    title: 'Centro de tutoriales · AportaYa',
  },
]
