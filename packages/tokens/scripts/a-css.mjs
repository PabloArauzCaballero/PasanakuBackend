#!/usr/bin/env node
// Emite generado/tokens.css desde tokens.json. No se versiona ni se edita.
import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { RAIZ, ROLES, TIPOS, aFontCss, aKebab, resolverRol, tokens } from './comun.mjs'

const { color, espacio, radio, fuente, tactil, borde } = tokens.primitivas
const linea = (nombre, valor) => `  --${nombre}: ${valor};`

const primitivas = [
  '  /* Paleta — los tonos crudos. Un componente no los pide: pide un rol. */',
  ...Object.entries(color)
    .filter(([k]) => !['ok', 'okbg', 'warn', 'warnbg', 'err', 'errbg', 'info', 'infobg'].includes(k))
    .map(([k, v]) => linea(k, v)),
  '',
  '  /* Escalas */',
  ...Object.entries(espacio).map(([k, v]) => linea(k, `${v}px`)),
  ...Object.entries(radio).map(([k, v]) => linea(`r-${k}`, `${v}px`)),
  linea('area-tactil', `${tactil.minimoWeb}px`),
  ...Object.entries(borde).map(([k, v]) => linea(`borde-${k}`, `${v}px`)),
  linea('font-d', fuente.display),
  linea('font-b', fuente.cuerpo),
  linea('mono', fuente.mono),
  '',
  '  /* Tipografía — un rol por uso. El tracking va aparte: el `font` abreviado no lo lleva. */',
  ...TIPOS.flatMap(([nombre, t]) => [
    linea(`t-${aKebab(nombre)}`, aFontCss(t)),
    linea(`t-${aKebab(nombre)}-track`, `${t.track}em`),
  ]),
]

const roles = (tema) => ROLES.map((rol) => linea(aKebab(rol), resolverRol(tokens.roles[tema][rol])))

const css = `/* GENERADO por packages/tokens/scripts/a-css.mjs desde tokens.json — no editar a mano. */

:root {
${primitivas.join('\n')}

  /* Roles del tema claro. El oscuro redefine estos y ningún componente. */
${roles('claro').join('\n')}
}

/* El tema del sistema, salvo que se haya elegido claro a mano. */
@media (prefers-color-scheme: dark) {
  :root:not([data-theme='light']) {
${roles('oscuro').map((l) => `  ${l}`).join('\n')}
  }
}

:root[data-theme='light'] {
${roles('claro').join('\n')}
}

:root[data-theme='dark'] {
${roles('oscuro').join('\n')}
}
`
mkdirSync(resolve(RAIZ, 'generado'), { recursive: true })
writeFileSync(resolve(RAIZ, 'generado/tokens.css'), css, 'utf8')
process.stdout.write(`generado/tokens.css · ${css.split('\n').length} lineas\n`)
