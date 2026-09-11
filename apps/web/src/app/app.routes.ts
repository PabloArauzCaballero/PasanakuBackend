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

/** Una carpeta por página en `paginas/`. Lo indexable se prerenderiza; lo vivo va en servidor. */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio), title: 'AportaYa · el pasanaku de siempre, sin el cuaderno', data: { seo: seoInicio } },
  { path: 'plazos', loadComponent: () => import('./paginas/plazos/plazos').then((m) => m.Plazos), title: 'Calcular un plazo hábil · AportaYa', data: { seo: seoPlazos } },
  { path: 'catalogo', loadComponent: () => import('./paginas/catalogo/catalogo').then((m) => m.PaginaCatalogo), title: 'Catálogo de diseño · AportaYa' },
]
