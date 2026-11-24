import { Routes } from '@angular/router'
import { requiereAlgunPermiso } from './guardia'

/**
 * Las rutas de configuración (carril B · L4: CU-34, CU-80, CU-83, CU-58 y CU-132). El shell las carga por
 * `loadChildren` detrás de `ver:configuracion`; cada una vuelve a exigir el permiso que declara su contrato.
 */
export const rutasConfiguracion: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pantalla-de-configuracion').then((m) => m.PantallaDeConfiguracion),
    title: 'Configuración · AportaYa',
  },
  {
    path: 'tarifario',
    loadComponent: () => import('./tarifario/pantalla-de-tarifario').then((m) => m.PantallaDeTarifario),
    title: 'Tarifario · AportaYa',
    canMatch: [requiereAlgunPermiso('TARIFARIO_PUBLICAR', 'AUDITORIA_LEER')],
  },
  {
    path: 'parametros-financieros',
    loadComponent: () => import('./parametros/pantalla-de-parametros-financieros').then((m) => m.PantallaDeParametrosFinancieros),
    title: 'Parámetros financieros · AportaYa',
    canMatch: [requiereAlgunPermiso('TARIFARIO_ADMINISTRAR')],
  },
  {
    path: 'mensajeria',
    loadComponent: () => import('./mensajeria/pantalla-de-mensajeria').then((m) => m.PantallaDeMensajeria),
    title: 'Mensajería · AportaYa',
    canMatch: [requiereAlgunPermiso('AUDITORIA_LEER')],
  },
  {
    path: 'reportes',
    loadComponent: () => import('./reportes/pantalla-de-reportes').then((m) => m.PantallaDeReportes),
    title: 'Reportes · AportaYa',
    canMatch: [requiereAlgunPermiso('AUDITORIA_LEER')],
  },
]
