import { Routes } from '@angular/router'

/** Una carpeta por página en `paginas/`. Lo indexable se prerenderiza; lo vivo va en servidor. */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio), title: 'AportaYa · el pasanaku de siempre, sin el cuaderno' },
  { path: 'plazos', loadComponent: () => import('./paginas/plazos/plazos').then((m) => m.Plazos), title: 'Calcular un plazo hábil · AportaYa' },
  { path: 'catalogo', loadComponent: () => import('./paginas/catalogo/catalogo').then((m) => m.PaginaCatalogo), title: 'Catálogo de diseño · AportaYa' },
]
