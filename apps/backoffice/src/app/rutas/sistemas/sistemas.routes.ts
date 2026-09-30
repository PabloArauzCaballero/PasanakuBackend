import { Routes } from '@angular/router'
import { detectarModoFuentesDeSistemas, provideFuentesDeSistemas } from './dominio/proveedor-fuentes'
import { soloRolesDeSistemas } from './guardia-rol-sistemas'

/**
 * Las rutas del dominio sistemas (F8.D). **Este archivo lo posee el carril B5**; el
 * shell (`app.routes.ts`, congelado por F6) solo lo carga por `loadChildren`, ya detrás
 * de `canMatch: [requierePermiso('ver:sistemas')]` puesto en la ruta padre `sistemas`.
 *
 * Acá se agrega una SEGUNDA barrera, más estricta que un permiso suelto: `canMatch: [
 * soloRolesDeSistemas]` exige además que el `rol` de la sesión sea PLATAFORMA o
 * SEGURIDAD — nunca un rol financiero, aunque por error tuviera el permiso
 * `ver:sistemas`. Ninguna pantalla de acá carga su chunk si cualquiera de las dos
 * barreras corta (delta D-2, doble barrera de interfaz — la de servidor la simula cada
 * pantalla, ver el hueco de contrato en `planes/informes/carril-B5.md`).
 *
 * `providers` en la ruta padre (H2.S2.M1): acá, y no en `app.config.ts`, es donde se
 * resuelve la fuente de las nueve pantallas — este archivo es de este carril,
 * `app.config.ts` no lo es. `provideFuentesDeSistemas` viene de
 * `dominio/proveedor-fuentes` (la versión SEGURA, la que compila `production`);
 * `fileReplacements` (`angular.json`) la cambia por `proveedor-fuentes.demo.ts` solo en
 * las configuraciones `development` y `demo`.
 */
export const rutasSistemas: Routes = [
  {
    path: '',
    canMatch: [soloRolesDeSistemas],
    providers: [...provideFuentesDeSistemas(detectarModoFuentesDeSistemas())],
    loadComponent: () => import('../../layout/sistemas/shell-sistemas').then((m) => m.ShellSistemas),
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
