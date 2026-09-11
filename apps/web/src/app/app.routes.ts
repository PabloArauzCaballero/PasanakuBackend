import { Routes } from '@angular/router'
import { migaDePan, organizacion, paginaWeb, preguntasFrecuentes, sitioWeb } from './seo/json-ld'
import type { MetaDeRuta } from './seo/servicio-meta'

/**
 * `data.seo` es de `ServicioMeta` (F10, `apps/web/src/app/seo/`). Cada página le agrega
 * su bloque acá — no se reescribe la página para esto. Título y descripción salen del
 * `descripcion`/`titulo` real del frontmatter en `contenido/**\/*.md` (lo que la página ya
 * renderiza), no de un texto genérico inventado para SEO.
 *
 * `/catalogo` no lleva `seo`: está en `RUTAS_NO_INDEXABLES` (ADR-042), es la herramienta
 * interna del sistema de diseño. `verificar/:codigo`, `publico/grupos/:codigo` y
 * `publico/sorteos/:id` **tampoco llevan `seo`**: son datos de terceros por diseño
 * (invariante 9 · `RUTAS_NO_INDEXABLES`), y además ya van `noindex,nofollow` por
 * `X-Robots-Tag` desde `app.routes.server.ts`. `ServicioMeta` refuerza eso mismo del lado
 * del cliente — dos capas, no una, porque acá es donde el SEO y la protección de datos se
 * pelean y gana la protección.
 */
/** El primer segmento de cualquier miga de pan del sitio: siempre "Inicio" → "/". */
const INICIO = { nombre: 'Inicio', ruta: '/' }

const seoInicio: MetaDeRuta = {
  titulo: 'AportaYa · el pasanaku de siempre, sin el cuaderno',
  tituloCompleto: true,
  descripcion: 'AportaYa organiza el pasanaku de toda la vida con turnos, plazos y acuerdos claros, sin reemplazar la confianza del grupo.',
  actualizado: '2026-09-09',
  jsonLd: [organizacion(), sitioWeb(), paginaWeb({ titulo: 'AportaYa', descripcion: 'El pasanaku de siempre, sin el cuaderno.', ruta: '/' })],
}

const seoComoFunciona: MetaDeRuta = {
  titulo: 'Cómo funciona AportaYa',
  descripcion: 'Cómo funciona el pasanaku digital de AportaYa: grupos, aportes, turnos por sorteo verificable y entrega del fondo.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Cómo funciona AportaYa', descripcion: 'El pasanaku digital, paso a paso.', ruta: '/como-funciona', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Cómo funciona', ruta: '/como-funciona' }]),
  ],
}

const seoSeguridad: MetaDeRuta = {
  titulo: 'Seguridad y custodia de tu dinero',
  descripcion: 'Cómo custodia AportaYa el dinero de los grupos: partida doble, encaje y trazabilidad de cada movimiento.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Seguridad y custodia de tu dinero', descripcion: 'Qué pasa con tu plata desde que la aportás hasta que la recibís.', ruta: '/seguridad', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Seguridad', ruta: '/seguridad' }]),
  ],
}

const seoTarifas: MetaDeRuta = {
  titulo: 'Tarifas y comisiones',
  descripcion: 'Cotizá cuánto cobra AportaYa por una operación, con impuestos incluidos, antes de hacerla. El tarifario está sujeto a preaviso cuando sube (CU-34).',
  jsonLd: [
    paginaWeb({ titulo: 'Tarifas y comisiones', descripcion: 'Cotizador de comisiones de AportaYa.', ruta: '/tarifas' }),
    migaDePan([INICIO, { nombre: 'Tarifas', ruta: '/tarifas' }]),
  ],
}

const seoContratoDeAdhesion: MetaDeRuta = {
  titulo: 'Contrato de adhesión',
  descripcion: 'Contrato de adhesión de la cuenta de billetera de AportaYa: condiciones vigentes, con versión y fecha.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Contrato de adhesión', descripcion: 'Las condiciones que aceptás al abrir tu billetera.', ruta: '/contrato-de-adhesion', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Contrato de adhesión', ruta: '/contrato-de-adhesion' }]),
  ],
}

const seoReclamos: MetaDeRuta = {
  titulo: 'Reclamos',
  descripcion: 'Punto de reclamo de AportaYa: plazo de respuesta de 5 días hábiles (prorrogable a 10) y segunda instancia ante el supervisor.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Reclamos', descripcion: 'Cómo presentar un reclamo y en cuánto tiempo se responde.', ruta: '/reclamos', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Reclamos', ruta: '/reclamos' }]),
  ],
}

const seoPrivacidad: MetaDeRuta = {
  titulo: 'Privacidad y tratamiento de datos',
  descripcion: 'Política de privacidad de AportaYa: tratamiento de datos personales y cómo ejercer tus derechos.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Privacidad y tratamiento de datos', descripcion: 'Qué datos usamos, para qué, y cómo ejercer tus derechos.', ruta: '/privacidad', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Privacidad', ruta: '/privacidad' }]),
  ],
}

const seoTransparencia: MetaDeRuta = {
  titulo: 'Transparencia: cómo se sella y se verifica',
  descripcion: 'Cómo AportaYa hace verificable el sorteo de turnos y la historia de cada grupo: compromiso, revelación y bloques encadenados por hash.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Transparencia', descripcion: 'El sorteo y la historia de cada grupo se pueden comprobar por tu cuenta.', ruta: '/transparencia', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Transparencia', ruta: '/transparencia' }]),
  ],
}

const seoPreguntas: MetaDeRuta = {
  titulo: 'Preguntas frecuentes',
  descripcion: 'Preguntas frecuentes sobre AportaYa: qué es, qué cobra, qué pasa si alguien no aporta y cómo se verifica el sorteo.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Preguntas frecuentes', descripcion: 'Lo que más preguntan antes de unirse a un grupo.', ruta: '/preguntas', actualizado: '2026-09-09' }),
    preguntasFrecuentes([
      { pregunta: '¿AportaYa presta dinero o paga intereses?', respuesta: 'No. Un pasanaku es ahorro rotativo: el dinero que recibís en tu turno es el que aportó el grupo, no un préstamo ni una inversión con rendimiento.' },
      { pregunta: '¿Cuánto cobra AportaYa?', respuesta: 'Una comisión por operación, que se muestra completa —con impuestos incluidos— antes de que confirmes cualquier movimiento.' },
      { pregunta: '¿Qué pasa si alguien del grupo no aporta?', respuesta: 'Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado, con un proceso de descargo antes de cualquier sanción.' },
      { pregunta: '¿Cómo sé que el sorteo de turnos no está arreglado?', respuesta: 'El sorteo usa un algoritmo público en dos fases —compromiso y revelación— y cada resultado se puede verificar de forma independiente.' },
      { pregunta: '¿AportaYa está regulado por ASFI?', respuesta: 'Tenemos una solicitud de licencia en trámite ante la ASFI.' },
      { pregunta: '¿Qué hago si tengo un reclamo?', respuesta: 'Lo presentás desde la app o por los canales publicados en Reclamos, con un plazo de respuesta garantizado.' },
    ]),
  ],
}

const seoEstadoRegulatorio: MetaDeRuta = {
  titulo: 'Estado regulatorio',
  descripcion: 'Estado regulatorio de AportaYa: solicitud de licencia en trámite ante la ASFI (Res. ASFI/540/2025).',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Estado regulatorio', descripcion: 'El estado real del trámite de licencia de AportaYa ante la ASFI.', ruta: '/legal/estado-regulatorio', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Legal', ruta: '/legal/estado-regulatorio' }, { nombre: 'Estado regulatorio', ruta: '/legal/estado-regulatorio' }]),
  ],
}

const seoDescargar: MetaDeRuta = {
  titulo: 'Descargar la app',
  descripcion: 'Dónde descargar la app de AportaYa.',
  actualizado: '2026-09-09',
  jsonLd: [
    paginaWeb({ titulo: 'Descargar la app', descripcion: 'AportaYa está disponible para Android. iOS llega en una etapa posterior.', ruta: '/descargar', actualizado: '2026-09-09' }),
    migaDePan([INICIO, { nombre: 'Descargar', ruta: '/descargar' }]),
  ],
}

const seoPlazos: MetaDeRuta = {
  titulo: 'Calcular un plazo hábil',
  descripcion: 'Calculá un plazo en días hábiles bolivianos: fines de semana y feriados quedan afuera, igual que en el pasanaku.',
  jsonLd: [
    paginaWeb({ titulo: 'Calcular un plazo hábil', descripcion: 'Calculadora de plazos hábiles.', ruta: '/plazos' }),
    migaDePan([INICIO, { nombre: 'Plazos', ruta: '/plazos' }]),
  ],
}

/**
 * Una carpeta por página en `paginas/`. Lo indexable se prerenderiza; lo vivo va en
 * servidor (`app.routes.server.ts`). Las quince rutas de `planes/14` F9.1, más
 * `/plazos` (CU-59, ya scaffoldeado por F0-W antes de este carril).
 */
export const routes: Routes = [
  { path: '', loadComponent: () => import('./paginas/inicio/inicio').then((m) => m.Inicio), title: 'AportaYa · el pasanaku de siempre, sin el cuaderno', data: { seo: seoInicio } },
  { path: 'como-funciona', loadComponent: () => import('./paginas/como-funciona/como-funciona').then((m) => m.ComoFunciona), title: 'Cómo funciona · AportaYa', data: { seo: seoComoFunciona } },
  { path: 'seguridad', loadComponent: () => import('./paginas/seguridad/seguridad').then((m) => m.Seguridad), title: 'Seguridad y custodia · AportaYa', data: { seo: seoSeguridad } },
  { path: 'tarifas', loadComponent: () => import('./paginas/tarifas/tarifas').then((m) => m.Tarifas), title: 'Tarifas y comisiones · AportaYa', data: { seo: seoTarifas } },
  { path: 'contrato-de-adhesion', loadComponent: () => import('./paginas/contrato-de-adhesion/contrato-de-adhesion').then((m) => m.ContratoDeAdhesion), title: 'Contrato de adhesión · AportaYa', data: { seo: seoContratoDeAdhesion } },
  { path: 'reclamos', loadComponent: () => import('./paginas/reclamos/reclamos').then((m) => m.Reclamos), title: 'Reclamos · AportaYa', data: { seo: seoReclamos } },
  { path: 'privacidad', loadComponent: () => import('./paginas/privacidad/privacidad').then((m) => m.Privacidad), title: 'Privacidad · AportaYa', data: { seo: seoPrivacidad } },
  { path: 'transparencia', loadComponent: () => import('./paginas/transparencia/transparencia').then((m) => m.Transparencia), title: 'Transparencia · AportaYa', data: { seo: seoTransparencia } },
  { path: 'preguntas', loadComponent: () => import('./paginas/preguntas/preguntas').then((m) => m.Preguntas), title: 'Preguntas frecuentes · AportaYa', data: { seo: seoPreguntas } },
  { path: 'legal/estado-regulatorio', loadComponent: () => import('./paginas/legal-estado-regulatorio/legal-estado-regulatorio').then((m) => m.LegalEstadoRegulatorio), title: 'Estado regulatorio · AportaYa', data: { seo: seoEstadoRegulatorio } },
  { path: 'descargar', loadComponent: () => import('./paginas/descargar/descargar').then((m) => m.Descargar), title: 'Descargar la app · AportaYa', data: { seo: seoDescargar } },
  // Datos de terceros: sin `data.seo`, y noindex reforzado también en app.routes.server.ts.
  { path: 'verificar/:codigo', loadComponent: () => import('./paginas/verificar/verificar').then((m) => m.Verificar), title: 'Verificar certificado · AportaYa' },
  { path: 'publico/grupos/:codigo', loadComponent: () => import('./paginas/publico-grupos/grupo-transparencia').then((m) => m.GrupoTransparencia), title: 'Verificar cadena de transparencia · AportaYa' },
  { path: 'publico/sorteos/:id', loadComponent: () => import('./paginas/publico-sorteos/sorteo-verificacion').then((m) => m.SorteoVerificacion), title: 'Verificar sorteo · AportaYa' },
  { path: 'catalogo', loadComponent: () => import('./paginas/catalogo/catalogo').then((m) => m.PaginaCatalogo), title: 'Catálogo de diseño · AportaYa' },
  { path: 'plazos', loadComponent: () => import('./paginas/plazos/plazos').then((m) => m.Plazos), title: 'Calcular un plazo hábil · AportaYa', data: { seo: seoPlazos } },
]
