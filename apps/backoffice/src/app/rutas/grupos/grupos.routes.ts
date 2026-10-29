import { Routes } from '@angular/router'

/**
 * Consulta de grupos del backoffice (solo lectura). Sección propia con su permiso (`ver:grupos` →
 * `ADMIN_PLATAFORMA` o `AUDITORIA_LEER`, los mismos que exige `grupos` en `/grupos/backoffice`).
 */
export const rutasGrupos: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./listado-de-grupos').then((m) => m.ListadoDeGrupos), title: 'Grupos · AportaYa' },
  { path: ':grupoId', loadComponent: () => import('./detalle-de-grupo').then((m) => m.DetalleDeGrupo), title: 'Grupo · AportaYa' },
]
