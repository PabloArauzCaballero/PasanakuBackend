// Genera sitemap.xml a partir de las rutas indexables que produce scripts/contenido.mjs.
// Plano y sin dependencias (igual que geo/robots.mjs), porque lo corre Node directo, sin TS.
//
// Regla que no se negocia (ficha F10): el sitemap NO incluye ninguna ruta /verificar/* ni
// /publico/*. RUTAS_EXCLUIDAS es la fuente única de esa regla — se comparte con
// RUTAS_NO_INDEXABLES de geo/robots.mjs para que las dos listas nunca diverjan en silencio.
export const RUTAS_EXCLUIDAS_SITEMAP = ['/verificar/', '/publico/']

/** true si la ruta nunca puede entrar al sitemap, sin importar lo que diga `indexable`. */
export function rutaExcluidaDelSitemap(ruta) {
  return RUTAS_EXCLUIDAS_SITEMAP.some((prefijo) => ruta === prefijo || ruta.startsWith(prefijo))
}

/**
 * @param {{ruta: string, actualizado: string}[]} rutasIndexables
 * @param {string} base
 */
export function generarSitemap(rutasIndexables, base) {
  const filas = rutasIndexables
    .filter((p) => !rutaExcluidaDelSitemap(p.ruta))
    .map((p) => `  <url>\n    <loc>${base}${p.ruta}</loc>\n    <lastmod>${p.actualizado}</lastmod>\n  </url>`)
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${filas.join('\n')}\n</urlset>\n`
}
