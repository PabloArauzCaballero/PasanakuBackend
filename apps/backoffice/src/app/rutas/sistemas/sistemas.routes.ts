import { Routes } from '@angular/router'
import { soloRolesDeSistemas } from './guardia-rol-sistemas'

/**
 * Las rutas del dominio sistemas (F8.D). **Este archivo lo posee el carril B5**; el
 * shell (`app.routes.ts`) solo lo carga por `loadChildren` y no cambia.
 *
 * Backoffice aparte del financiero: `canMatch: [soloRolesDeSistemas]` en la ruta padre
 * corta ANTES de cargar cualquier chunk si el rol no es PLATAFORMA ni SEGURIDAD (delta
 * D-2, doble barrera de interfaz — la de servidor la simula cada pantalla).
 */
export const rutasSistemas: Routes = [
  {
    path: '',
    canMatch: [soloRolesDeSistemas],
    loadComponent: () => import('./menu/shell-sistemas').then((m) => m.ShellSistemas),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'servicios' },
      { path: 'servicios', loadComponent: () => import('./servicios/pantalla-servicios').then((m) => m.PantallaServicios), title: 'Servicios y SLO · Sistemas · AportaYa' },
      { path: 'despliegues', loadComponent: () => import('./despliegues/pantalla-despliegues').then((m) => m.PantallaDespliegues), title: 'Despliegues e interruptores · Sistemas · AportaYa' },
      { path: 'base-de-datos', loadComponent: () => import('./base-datos/pantalla-base-datos').then((m) => m.PantallaBaseDeDatos), title: 'Base y migraciones · Sistemas · AportaYa' },
      { path: 'respaldos', loadComponent: () => import('./respaldos/pantalla-respaldos').then((m) => m.PantallaRespaldos), title: 'Respaldos · Sistemas · AportaYa' },
      { path: 'proveedores', loadComponent: () => import('./proveedores/pantalla-proveedores').then((m) => m.PantallaProveedores), title: 'Proveedores · Sistemas · AportaYa' },
      { path: 'outbox', loadComponent: () => import('./outbox/pantalla-outbox').then((m) => m.PantallaOutbox), title: 'Outbox y descartados · Sistemas · AportaYa' },
      { path: 'webhooks', loadComponent: () => import('./webhooks/pantalla-webhooks').then((m) => m.PantallaWebhooks), title: 'Webhooks · Sistemas · AportaYa' },
      { path: 'accesos', loadComponent: () => import('./accesos/pantalla-accesos').then((m) => m.PantallaAccesos), title: 'Accesos · Sistemas · AportaYa' },
      { path: 'incidentes', loadComponent: () => import('./incidentes/pantalla-incidentes').then((m) => m.PantallaIncidentes), title: 'Incidentes · Sistemas · AportaYa' },
    ],
  },
]
