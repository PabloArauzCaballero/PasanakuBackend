import { Routes } from '@angular/router'

/**
 * **El enchufe por dominio.** Lo escribe el shell (F6) UNA vez y se congela: cada
 * carril de pantallas llena el `<dominio>.routes.ts` de su directorio y este archivo no
 * cambia. La prueba `enchufe-de-rutas.spec.ts` lo verifica agregando una ruta.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'operacion' },
  { path: 'operacion', loadChildren: () => import('./rutas/operacion/operacion.routes').then((m) => m.rutasOperacion) },
  { path: 'cumplimiento', loadChildren: () => import('./rutas/cumplimiento/cumplimiento.routes').then((m) => m.rutasCumplimiento) },
  { path: 'sistemas', loadChildren: () => import('./rutas/sistemas/sistemas.routes').then((m) => m.rutasSistemas) },
  { path: 'contabilidad', loadChildren: () => import('./rutas/contabilidad/contabilidad.routes').then((m) => m.rutasContabilidad) },
  { path: 'publicidad', loadChildren: () => import('./rutas/publicidad/publicidad.routes').then((m) => m.rutasPublicidad) },
]
