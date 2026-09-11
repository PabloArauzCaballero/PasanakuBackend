import { Routes } from '@angular/router'

/**
 * Una carpeta por página en `paginas/`. Lo indexable se prerenderiza; lo vivo va en
 * servidor (`app.routes.server.ts`). Las quince rutas de `planes/14` F9.1, más
 * `/plazos` (CU-59, ya scaffoldeado por F0-W antes de este carril).
 */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio), title: 'AportaYa · el pasanaku de siempre, sin el cuaderno' },
  { path: 'como-funciona', loadComponent: () => import('./paginas/como-funciona/como-funciona').then((m) => m.ComoFunciona), title: 'Cómo funciona · AportaYa' },
  { path: 'seguridad', loadComponent: () => import('./paginas/seguridad/seguridad').then((m) => m.Seguridad), title: 'Seguridad y custodia · AportaYa' },
  { path: 'tarifas', loadComponent: () => import('./paginas/tarifas/tarifas').then((m) => m.Tarifas), title: 'Tarifas y comisiones · AportaYa' },
  { path: 'contrato-de-adhesion', loadComponent: () => import('./paginas/contrato-de-adhesion/contrato-de-adhesion').then((m) => m.ContratoDeAdhesion), title: 'Contrato de adhesión · AportaYa' },
  { path: 'reclamos', loadComponent: () => import('./paginas/reclamos/reclamos').then((m) => m.Reclamos), title: 'Reclamos · AportaYa' },
  { path: 'privacidad', loadComponent: () => import('./paginas/privacidad/privacidad').then((m) => m.Privacidad), title: 'Privacidad · AportaYa' },
  { path: 'transparencia', loadComponent: () => import('./paginas/transparencia/transparencia').then((m) => m.Transparencia), title: 'Transparencia · AportaYa' },
  { path: 'preguntas', loadComponent: () => import('./paginas/preguntas/preguntas').then((m) => m.Preguntas), title: 'Preguntas frecuentes · AportaYa' },
  { path: 'legal/estado-regulatorio', loadComponent: () => import('./paginas/legal-estado-regulatorio/legal-estado-regulatorio').then((m) => m.LegalEstadoRegulatorio), title: 'Estado regulatorio · AportaYa' },
  { path: 'descargar', loadComponent: () => import('./paginas/descargar/descargar').then((m) => m.Descargar), title: 'Descargar la app · AportaYa' },
  { path: 'verificar/:codigo', loadComponent: () => import('./paginas/verificar/verificar').then((m) => m.Verificar), title: 'Verificar certificado · AportaYa' },
  { path: 'publico/grupos/:codigo', loadComponent: () => import('./paginas/publico-grupos/grupo-transparencia').then((m) => m.GrupoTransparencia), title: 'Verificar cadena de transparencia · AportaYa' },
  { path: 'publico/sorteos/:id', loadComponent: () => import('./paginas/publico-sorteos/sorteo-verificacion').then((m) => m.SorteoVerificacion), title: 'Verificar sorteo · AportaYa' },
  { path: 'catalogo', loadComponent: () => import('./paginas/catalogo/catalogo').then((m) => m.PaginaCatalogo), title: 'Catálogo de diseño · AportaYa' },
  { path: 'plazos', loadComponent: () => import('./paginas/plazos/plazos').then((m) => m.Plazos), title: 'Calcular un plazo hábil · AportaYa' },
]
