import { Routes } from '@angular/router'

/**
 * La guía interactiva del sitio. Es una página más —estática, indexable— y el recorrido
 * se hace sobre las páginas de verdad, no sobre una maqueta.
 */
export const rutasDeTutoriales: Routes = [
  {
    path: '',
    loadComponent: () => import('@aportaya/tutoriales/centro/pantalla-centro-de-ayuda').then((m) => m.PantallaCentroDeAyuda),
    title: 'Guía interactiva · AportaYa',
  },
]
