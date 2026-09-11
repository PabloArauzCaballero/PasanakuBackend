/**
 * Constructores de JSON-LD (schema.org) para el sitio público.
 *
 * Regla que no se negocia (ficha F10, planes/18): **nunca** `Review`, `AggregateRating`
 * ni `FinancialService`. AportaYa no es una entidad financiera regulada a ojos de un
 * buscador y no se le atribuyen reseñas que no existen. `pruebas-cu`/`glosario-dominio`
 * comparten el mismo espíritu: no se declara lo que no se puede sostener.
 *
 * Ningún tipo de acá expone un dato que la ruta no exponga ya de por sí — si mañana se
 * agrega un tipo nuevo, tiene que salir de un campo que la página ya renderiza.
 */

/** Tipos de schema.org explícitamente prohibidos en este sitio. Ver `sitio.spec.ts`. */
export const TIPOS_PROHIBIDOS = ['Review', 'AggregateRating', 'FinancialService'] as const

export type Organizacion = {
  '@context': 'https://schema.org'
  '@type': 'Organization'
  name: string
  url: string
  logo?: string
}

export type SitioWeb = {
  '@context': 'https://schema.org'
  '@type': 'WebSite'
  name: string
  url: string
}

export type PaginaWeb = {
  '@context': 'https://schema.org'
  '@type': 'WebPage'
  name: string
  description: string
  url: string
  dateModified?: string
}

export type MigaDePan = {
  '@context': 'https://schema.org'
  '@type': 'BreadcrumbList'
  itemListElement: { '@type': 'ListItem'; position: number; name: string; item: string }[]
}

export type JsonLd = Organizacion | SitioWeb | PaginaWeb | MigaDePan

const BASE = 'https://aportaya.bo'

export function organizacion(): Organizacion {
  return { '@context': 'https://schema.org', '@type': 'Organization', name: 'AportaYa', url: BASE }
}

export function sitioWeb(): SitioWeb {
  return { '@context': 'https://schema.org', '@type': 'WebSite', name: 'AportaYa', url: BASE }
}

export function paginaWeb(datos: { titulo: string; descripcion: string; ruta: string; actualizado?: string }): PaginaWeb {
  return {
    '@context': 'https://schema.org',
    '@type': 'WebPage',
    name: datos.titulo,
    description: datos.descripcion,
    url: `${BASE}${datos.ruta}`,
    ...(datos.actualizado ? { dateModified: datos.actualizado } : {}),
  }
}

export function migaDePan(segmentos: { nombre: string; ruta: string }[]): MigaDePan {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: segmentos.map((s, i) => ({ '@type': 'ListItem', position: i + 1, name: s.nombre, item: `${BASE}${s.ruta}` })),
  }
}

/** Verificación en tiempo de ejecución: se usa en pruebas, nunca en producción silenciosamente. */
export function sinTiposProhibidos(json: unknown): boolean {
  const texto = JSON.stringify(json)
  return !TIPOS_PROHIBIDOS.some((t) => texto.includes(`"@type":"${t}"`) || texto.includes(`"@type": "${t}"`))
}
