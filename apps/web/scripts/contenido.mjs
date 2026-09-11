#!/usr/bin/env node
// Procesa contenido/**/*.md ANTES de `ng build`: produce src/generado/contenido.json (lo que
// las paginas renderizan en prerender), public/<ruta>.md (espejo para modelos, F11),
// public/robots.txt (ADR-042) y la lista de rutas indexables para el sitemap (F10).
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { marked } from 'marked'
import { parse } from 'yaml'
import { robotsTxt } from '../src/app/geo/robots.mjs'

const AQUI = dirname(fileURLToPath(import.meta.url))
const RAIZ = join(AQUI, '..')
const CONTENIDO = join(RAIZ, 'contenido')
const GENERADO = join(RAIZ, 'src/generado')
const PUBLIC = join(RAIZ, 'public')
const OBLIGATORIOS = ['ruta', 'titulo', 'bajada', 'descripcion', 'indexable', 'actualizado']

function leer(archivo) {
  const texto = readFileSync(archivo, 'utf8')
  const m = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/.exec(texto)
  if (!m) throw new Error(`${archivo}: sin frontmatter`)
  const meta = parse(m[1])
  for (const campo of OBLIGATORIOS) {
    if (meta[campo] === undefined) throw new Error(`${archivo}: falta '${campo}' en el frontmatter`)
  }
  if (!(meta.actualizado instanceof Date) && !/^\d{4}-\d{2}-\d{2}$/.test(String(meta.actualizado))) {
    throw new Error(`${archivo}: 'actualizado' tiene que ser una fecha AAAA-MM-DD`)
  }
  const actualizado = meta.actualizado instanceof Date ? meta.actualizado.toISOString().slice(0, 10) : String(meta.actualizado)
  return { ...meta, actualizado, markdown: m[2].trim(), html: marked.parse(m[2]) }
}

mkdirSync(GENERADO, { recursive: true })
mkdirSync(PUBLIC, { recursive: true })
const paginas = {}
for (const carpeta of readdirSync(CONTENIDO)) {
  for (const archivo of readdirSync(join(CONTENIDO, carpeta)).filter((a) => a.endsWith('.md'))) {
    const pagina = leer(join(CONTENIDO, carpeta, archivo))
    const clave = pagina.ruta === '/' ? 'inicio' : pagina.ruta.replace(/^\//, '').replace(/\//g, '__')
    paginas[clave] = pagina
    if (pagina.indexable) {
      // El espejo Markdown: el mismo contenido sin la capa de presentacion (F11).
      const espejo = pagina.ruta === '/' ? 'index.md' : `${pagina.ruta.replace(/^\//, '')}.md`
      mkdirSync(dirname(join(PUBLIC, espejo)), { recursive: true })
      writeFileSync(join(PUBLIC, espejo), `# ${pagina.titulo}\n\n> ${pagina.bajada}\n\n${pagina.markdown}\n\nActualizado: ${pagina.actualizado}\n`, 'utf8')
    }
  }
}
writeFileSync(join(GENERADO, 'contenido.json'), `${JSON.stringify({ paginas }, null, 2)}\n`, 'utf8')
writeFileSync(join(PUBLIC, 'robots.txt'), robotsTxt('https://aportaya.bo'), 'utf8')
const indexables = Object.values(paginas).filter((p) => p.indexable).map((p) => ({ ruta: p.ruta, actualizado: p.actualizado }))
writeFileSync(join(GENERADO, 'rutas-indexables.json'), `${JSON.stringify(indexables, null, 2)}\n`, 'utf8')
console.log(`contenido: ${Object.keys(paginas).length} páginas · ${indexables.length} indexables · robots.txt generado`)
