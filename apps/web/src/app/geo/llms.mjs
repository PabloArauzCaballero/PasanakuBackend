// Genera llms.txt y llms-full.txt (F11.2, GEO) a partir de las MISMAS páginas indexables
// que produce scripts/contenido.mjs desde contenido/**/*.md. Plano y sin dependencias
// (igual que geo/robots.mjs y seo/sitemap.mjs), porque lo corre Node directo, sin TS.
//
// Regla que no se negocia (ficha F11, gate propio): llms.txt NO expone ninguna ruta con
// datos de terceros. Reusa RUTAS_EXCLUIDAS_SITEMAP-equivalente acá mismo, porque las
// páginas con datos de terceros (`/verificar/*`, `/publico/*`) ya vienen sin `seo` ni
// frontmatter en `contenido/` — pero la exclusión se declara también acá, en el generador,
// como defensa en profundidad (el mismo patrón que seo/sitemap.mjs).
export const RUTAS_EXCLUIDAS_LLMS = ['/verificar/', '/publico/', '/catalogo', '/api/']

/** true si la ruta nunca puede entrar a llms.txt/llms-full.txt, sin importar el frontmatter. */
export function rutaExcluidaDeLlms(ruta) {
  return RUTAS_EXCLUIDAS_LLMS.some((prefijo) => ruta === prefijo || ruta.startsWith(prefijo))
}

/**
 * Agrupación editorial de las páginas indexables para llms.txt (ficha F11.2). Cada ruta
 * declarada acá cae en su categoría; una ruta indexable que no está en esta lista cae en
 * "Más páginas" — así un carril de contenido nuevo no rompe el generador en silencio.
 */
const CATEGORIAS = [
  { titulo: 'Qué es', rutas: ['/', '/como-funciona', '/preguntas'] },
  { titulo: 'Dinero y costos', rutas: ['/tarifas', '/seguridad'] },
  { titulo: 'Derechos y reclamos', rutas: ['/reclamos', '/privacidad', '/contrato-de-adhesion'] },
  { titulo: 'Estado regulatorio', rutas: ['/legal/estado-regulatorio'] },
  { titulo: 'Opcional', rutas: ['/transparencia', '/descargar'] },
]

/** @param {{ruta: string, indexable: boolean}[]} paginas */
function indexablesSinTerceros(paginas) {
  return paginas.filter((p) => p.indexable && !rutaExcluidaDeLlms(p.ruta))
}

/**
 * @param {{ruta: string, titulo: string, bajada: string, indexable: boolean, actualizado: string}[]} paginas
 * @param {string} base
 */
export function generarLlmsTxt(paginas, base) {
  const disponibles = indexablesSinTerceros(paginas)
  const porRuta = new Map(disponibles.map((p) => [p.ruta, p]))
  const lineas = [
    '# AportaYa',
    '',
    '> Billetera móvil boliviana que digitaliza el pasanaku: ahorro rotativo comunitario',
    '> donde un grupo aporta por períodos y cada participante recibe el fondo en su turno.',
    '> Opera en Bolivia, en bolivianos (Bs).',
    '',
  ]
  const usadas = new Set()
  for (const categoria of CATEGORIAS) {
    const paginasDeCategoria = categoria.rutas.map((r) => porRuta.get(r)).filter((p) => p !== undefined)
    if (paginasDeCategoria.length === 0) continue
    lineas.push(`## ${categoria.titulo}`, '')
    for (const p of paginasDeCategoria) {
      usadas.add(p.ruta)
      const rutaMd = p.ruta === '/' ? '/index.md' : `${p.ruta}.md`
      lineas.push(`- [${p.titulo}](${base}${rutaMd}): ${p.bajada}`)
    }
    lineas.push('')
  }
  const resto = disponibles.filter((p) => !usadas.has(p.ruta))
  if (resto.length > 0) {
    lineas.push('## Más páginas', '')
    for (const p of resto) {
      const rutaMd = p.ruta === '/' ? '/index.md' : `${p.ruta}.md`
      lineas.push(`- [${p.titulo}](${base}${rutaMd}): ${p.bajada}`)
    }
    lineas.push('')
  }
  return `${lineas.join('\n').trimEnd()}\n`
}

/**
 * @param {{ruta: string, titulo: string, bajada: string, indexable: boolean, actualizado: string, markdown: string}[]} paginas
 * @param {string} base
 */
export function generarLlmsFullTxt(paginas, base) {
  const disponibles = indexablesSinTerceros(paginas)
  const bloques = disponibles.map((p) => {
    const rutaMd = p.ruta === '/' ? '/index.md' : `${p.ruta}.md`
    return `# ${p.titulo}\n\nFuente: ${base}${rutaMd}\nActualizado: ${p.actualizado}\n\n${p.markdown}`
  })
  return `${bloques.join('\n\n---\n\n')}\n`
}
