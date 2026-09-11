import { Routes } from '@angular/router'
import { migaDePan, organizacion, paginaWeb, sitioWeb } from './seo/json-ld'
import type { MetaDeRuta } from './seo/servicio-meta'

/**
 * `data.seo` es de `ServicioMeta` (F10, `apps/web/src/app/seo/`). Cada página le agrega
 * su bloque acá — no se reescribe la página para esto. `/catalogo` no lleva `seo`: está en
 * `RUTAS_NO_INDEXABLES` (ADR-042) porque es la herramienta interna del sistema de diseño,
 * no contenido del sitio público.
 */
const seoInicio: MetaDeRuta = {
  titulo: 'AportaYa · el pasanaku de siempre, sin el cuaderno',
  tituloCompleto: true,
  descripcion: 'AportaYa organiza el pasanaku de toda la vida con turnos, plazos y acuerdos claros, sin reemplazar la confianza del grupo.',
  jsonLd: [organizacion(), sitioWeb(), paginaWeb({ titulo: 'AportaYa', descripcion: 'El pasanaku de siempre, sin el cuaderno.', ruta: '/' })],
}

const seoPlazos: MetaDeRuta = {
  titulo: 'Calcular un plazo hábil',
  descripcion: 'Calculá un plazo en días hábiles bolivianos: fines de semana y feriados quedan afuera, igual que en el pasanaku.',
  jsonLd: [
    paginaWeb({ titulo: 'Calcular un plazo hábil', descripcion: 'Calculadora de plazos hábiles.', ruta: '/plazos' }),
    migaDePan([
      { nombre: 'Inicio', ruta: '/' },
      { nombre: 'Plazos', ruta: '/plazos' },
    ]),
  ],
}

/**
 * Una carpeta por página en `paginas/`. Lo indexable se prerenderiza; lo vivo va en
 * servidor (`app.routes.server.ts`). Las quince rutas de `planes/14` F9.1, más
 * `/plazos` (CU-59, ya scaffoldeado por F0-W antes de este carril).
 */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio), title: 'AportaYa · el pasanaku de siempre, sin el cuaderno', data: { seo: seoInicio } },
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
  { path: 'plazos', loadComponent: () => import('./paginas/plazos/plazos').then((m) => m.Plazos), title: 'Calcular un plazo hábil · AportaYa', data: { seo: seoPlazos } },
]
