import { Routes } from '@angular/router'

/**
 * Postulaciones a organizador (CU-90). Sección propia con su permiso (`ver:postulaciones` → `ADMIN_PLATAFORMA`, el
 * mismo que exige `organizador` en cada ruta). Cada postulación tiene su id en la URL para poder pegar el enlace.
 */
export const rutasPostulaciones: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./bandeja-de-postulaciones').then((m) => m.BandejaDePostulaciones),
    title: 'Postulaciones a organizador · AportaYa',
  },
  {
    path: ':solicitudId',
    loadComponent: () => import('./pantalla-de-postulacion').then((m) => m.PantallaDePostulacion),
    title: 'Postulación a organizador · AportaYa',
  },
]
