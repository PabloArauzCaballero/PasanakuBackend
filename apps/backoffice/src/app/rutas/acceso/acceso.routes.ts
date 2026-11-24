import { Routes } from '@angular/router'
import { requierePermiso } from '../../nucleo/permisos'

/**
 * GU-C · Las rutas **con sesión** de «acceso» (cuelgan del shell, bajo `/acceso`). Las dos públicas —`/acceso/invitacion` y
 * `/acceso/recuperar`— viven fuera del shell, en `app.routes.ts`, porque sin sesión no hay menú que mostrar.
 *
 * - `cambiar-clave`: cualquier sesión abierta; el servidor resuelve el titular por el token.
 * - `recuperaciones`: la guardia oculta, el servidor decide por el mismo permiso (`SEGURIDAD_ACCESO_RESTABLECER`).
 */
export const rutasAcceso: Routes = [
  {
    path: 'cambiar-clave',
    loadComponent: () => import('./cambiar-clave/pantalla-de-cambiar-clave').then((m) => m.PantallaDeCambiarClave),
    title: 'Cambiar mi clave · AportaYa',
  },
  {
    path: 'recuperaciones',
    canMatch: [requierePermiso('SEGURIDAD_ACCESO_RESTABLECER')],
    loadComponent: () => import('./recuperaciones/pantalla-de-recuperaciones').then((m) => m.PantallaDeRecuperaciones),
    title: 'Recuperaciones de acceso · AportaYa',
  },
]
