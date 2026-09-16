import { Routes } from '@angular/router'
import { ShellFinanciero } from './layout/shell-financiero'
import { requierePermiso, requiereSesion } from './nucleo/permisos'

/**
 * **El enchufe por dominio.** Lo escribe el shell (F6) UNA vez y se congela: cada
 * carril de pantallas llena el `<dominio>.routes.ts` de su directorio y este archivo no
 * cambia. La prueba `enchufe-de-rutas.spec.ts` lo verifica agregando una ruta.
 *
 * Todo cuelga de `ShellFinanciero` (menú + cabecera). `sistemas` reutiliza el mismo
 * shell hasta que `B5` (F8.D) dibuje `layout/sistemas/` — ese directorio ya existe,
 * vacío, y cambiarlo no toca este archivo.
 */
export const routes: Routes = [
  // CU-04 · Fuera del shell: sin sesión no hay menú que mostrar.
  {
    path: 'ingreso',
    loadComponent: () => import('./rutas/ingreso/pantalla-de-ingreso').then((m) => m.PantallaDeIngreso),
    title: 'Ingresar · AportaYa',
  },
  {
    path: '',
    component: ShellFinanciero,
    canMatch: [requiereSesion()],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tablero' },
      { path: 'tablero', loadComponent: () => import('./rutas/tablero/tablero').then((m) => m.Tablero), title: 'Tablero · AportaYa' },
      {
        path: 'operacion',
        canMatch: [requierePermiso('ver:operacion')],
        loadChildren: () => import('./rutas/operacion/operacion.routes').then((m) => m.rutasOperacion),
      },
      {
        path: 'cumplimiento',
        canMatch: [requierePermiso('ver:cumplimiento')],
        loadChildren: () => import('./rutas/cumplimiento/cumplimiento.routes').then((m) => m.rutasCumplimiento),
      },
      {
        path: 'sistemas',
        canMatch: [requierePermiso('ver:sistemas')],
        loadChildren: () => import('./rutas/sistemas/sistemas.routes').then((m) => m.rutasSistemas),
      },
      {
        path: 'contabilidad',
        canMatch: [requierePermiso('ver:contabilidad')],
        loadChildren: () => import('./rutas/contabilidad/contabilidad.routes').then((m) => m.rutasContabilidad),
      },
      {
        path: 'publicidad',
        canMatch: [requierePermiso('ver:publicidad')],
        loadChildren: () => import('./rutas/publicidad/publicidad.routes').then((m) => m.rutasPublicidad),
      },
    ],
  },
]
