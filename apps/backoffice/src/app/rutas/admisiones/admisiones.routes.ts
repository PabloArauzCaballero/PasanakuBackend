import { Routes } from '@angular/router'

/**
 * Admisión humana de grupos (CU-68). Sección propia, con su permiso (`ver:admisiones` →
 * `ADMIN_PLATAFORMA`): resolver quién entra a un grupo no es cumplimiento ni operación, y abrirle
 * la sección de cumplimiento a quien resuelve admisiones le mostraría acciones que el servidor
 * le rechaza. Cada solicitud tiene su id en la URL para poder pegar el enlace en un aviso.
 */
export const rutasAdmisiones: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./entrada-de-admision').then((m) => m.EntradaDeAdmision),
    title: 'Solicitudes de ingreso · AportaYa',
  },
  {
    path: ':solicitudId',
    loadComponent: () => import('./pantalla-de-admision').then((m) => m.PantallaDeAdmision),
    title: 'Solicitud de ingreso · AportaYa',
  },
]
