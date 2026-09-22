import { Routes } from '@angular/router'

/**
 * Las rutas del dominio cumplimiento. **Este archivo lo posee el carril B2**; el shell
 * solo lo carga por `loadChildren`. Cada pantalla tiene su propio id en la URL, para
 * poder pegar el enlace en un expediente (mismo patrón que `operacion`).
 *
 * `alertas`, `actas` y `reclamos` cuelgan de un contenedor (ver
 * `planes/informes/carril-B2.md` § huecos): `cumplimiento.yaml` no declara ruta HTTP
 * para CU-97, CU-94 ni CU-52/53, así que esos tres contenedores arman datos de
 * ejemplo a partir del id — el día que el contrato exista, solo el contenedor cambia.
 */
export const rutasCumplimiento: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'verificaciones' },
  {
    path: 'verificaciones',
    loadComponent: () => import('./verificaciones/pantalla-de-expedientes').then((m) => m.PantallaDeExpedientes),
    title: 'Verificación de identidad · AportaYa',
  },
  {
    path: 'alertas/:alertaId',
    loadComponent: () => import('./alertas/contenedor-de-alerta').then((m) => m.ContenedorDeAlerta),
    title: 'Alertas de riesgo temprano · AportaYa',
  },
  {
    path: 'casos/:casoId',
    loadComponent: () => import('./casos/pantalla-de-caso').then((m) => m.PantallaDeCaso),
    title: 'Caso de cumplimiento · AportaYa',
  },
  {
    path: 'actas/:actaId',
    loadComponent: () => import('./actas/contenedor-de-acta').then((m) => m.ContenedorDeActa),
    title: 'Acta de comité · AportaYa',
  },
  {
    path: 'reclamos/:reclamoId',
    loadComponent: () => import('./reclamos/contenedor-de-reclamo').then((m) => m.ContenedorDeReclamo),
    title: 'Reclamo del consumidor · AportaYa',
  },
  {
    path: 'organizadores/:organizadorId/habilitacion',
    loadComponent: () => import('./organizadores/pantalla-de-habilitacion').then((m) => m.PantallaDeHabilitacionOrganizador),
    title: 'Habilitación de organizador · AportaYa',
  },
]
