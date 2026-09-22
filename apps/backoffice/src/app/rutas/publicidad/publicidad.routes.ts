import { Routes } from '@angular/router'
import { requierePermiso } from '../../nucleo/permisos'

/**
 * Las rutas del dominio publicidad. **Este archivo lo posee el carril B4** (CU-110–114);
 * el shell solo lo carga por `loadChildren` (`app.routes.ts`, sin tocar).
 *
 * Segregación de funciones en campañas (gate del carril, mismo patrón que
 * desembolsos/compras del resto del backoffice): `campanas` (gestión, crear) exige
 * `PUBLICIDAD_ANUNCIANTES`; `campanas/:campanaId/aprobacion` (aprobar/rechazar) exige
 * `PUBLICIDAD_APROBAR_CAMPANA`. El `canMatch` decide **antes** de montar el componente
 * — quien no tiene el permiso de aprobar nunca ve ni carga el panel de aprobación.
 */
export const rutasPublicidad: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'anunciantes' },
  {
    path: 'anunciantes',
    canMatch: [requierePermiso('PUBLICIDAD_ANUNCIANTES')],
    loadComponent: () => import('./anunciantes/pantalla-de-anunciantes').then((m) => m.PantallaDeAnunciantes),
    title: 'Anunciantes · AportaYa',
  },
  {
    path: 'campanas',
    canMatch: [requierePermiso('PUBLICIDAD_ANUNCIANTES')],
    loadComponent: () => import('./campanas/pantalla-de-campanas').then((m) => m.PantallaDeCampanas),
    title: 'Campañas · AportaYa',
  },
  {
    path: 'campanas/:campanaId/aprobacion',
    canMatch: [requierePermiso('PUBLICIDAD_APROBAR_CAMPANA')],
    loadComponent: () => import('./campanas/panel-de-aprobacion').then((m) => m.PanelDeAprobacionDeCampana),
    title: 'Aprobar campaña · AportaYa',
  },
  {
    path: 'moderacion',
    canMatch: [requierePermiso('PUBLICIDAD_MODERAR')],
    loadComponent: () => import('./moderacion/cola-de-moderacion').then((m) => m.ColaDeModeracion),
    title: 'Moderación de piezas creativas · AportaYa',
  },
  {
    path: 'desempeno',
    canMatch: [requierePermiso('PUBLICIDAD_ANUNCIANTES')],
    loadComponent: () => import('./desempeno/pantalla-de-desempeno').then((m) => m.PantallaDeDesempeno),
    title: 'Desempeño publicitario · AportaYa',
  },
  {
    path: 'liquidacion',
    canMatch: [requierePermiso('PUBLICIDAD_ANUNCIANTES')],
    loadComponent: () => import('./liquidacion/pantalla-de-liquidacion').then((m) => m.PantallaDeLiquidacion),
    title: 'Liquidación y facturación · AportaYa',
  },
]
