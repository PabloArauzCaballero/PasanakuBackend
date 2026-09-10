import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import tokens from '../tokens.json'

/**
 * **La prueba que impide inventar.**
 *
 * Los tokens no se comparan contra lo que alguien recuerda: se leen de
 * `docs/Views/Sistema-Diseno/estilos.css`, que es la fuente de verdad de la bóveda, y se
 * exige que coincidan valor por valor. Un tono inventado acá se propaga a los tres
 * productos y ya no se saca. Portada del `packages/ui` anterior sin cambiar ningún valor.
 */
const CSS_DE_LA_BOVEDA = resolve(__dirname, '../../../docs/Views/Sistema-Diseno/estilos.css')

function bloque(css: string, selector: string): Record<string, string> {
  const inicio = css.indexOf(selector)
  if (inicio === -1) throw new Error(`la bóveda ya no declara el bloque ${selector}`)
  const abre = css.indexOf('{', inicio)
  const cierra = css.indexOf('}', abre)
  const declaraciones: Record<string, string> = {}
  for (const linea of css.slice(abre + 1, cierra).split(';')) {
    const corte = linea.indexOf(':')
    if (corte === -1) continue
    const nombre = linea.slice(0, corte).trim()
    if (!nombre.startsWith('--')) continue
    declaraciones[nombre] = linea.slice(corte + 1).trim()
  }
  return declaraciones
}

function normalizar(valor: string): string {
  const corto = /^#([0-9a-f])([0-9a-f])([0-9a-f])$/i.exec(valor)
  if (corto) return `#${corto[1]}${corto[1]}${corto[2]}${corto[2]}${corto[3]}${corto[3]}`.toLowerCase()
  return /^#[0-9a-f]{6}$/i.test(valor) ? valor.toLowerCase() : valor
}

const css = readFileSync(CSS_DE_LA_BOVEDA, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')
const raiz = bloque(css, ':root{')
const escuro = { ...raiz, ...bloque(css, ':root[data-theme="dark"]') }

function valorDe(declaraciones: Record<string, string>, nombre: string): string {
  let valor = declaraciones[nombre]
  if (valor === undefined) throw new Error(`la bóveda no declara ${nombre}`)
  for (let salto = 0; salto < 8; salto += 1) {
    const referencia = /^var\((--[a-z0-9-]+)\)$/i.exec(valor)
    if (!referencia) break
    const siguiente = declaraciones[referencia[1]!]
    if (siguiente === undefined) throw new Error(`${nombre} apunta a ${referencia[1]}, que no existe`)
    valor = siguiente
  }
  return normalizar(valor)
}

const color = tokens.primitivas.color as Record<string, string>
/** Un rol que nombra una primitiva se resuelve a su hex; lo demás queda tal cual. */
const resolver = (v: string) => color[v] ?? v

function comparar(declaraciones: Record<string, string>, pares: Record<string, string | number>, sufijo = '') {
  for (const [variable, mio] of Object.entries(pares)) {
    expect(valorDe(declaraciones, variable), `${variable} divergió de la bóveda`).toBe(normalizar(`${mio}${sufijo}`))
  }
}

const MAPA_DE_ROLES: Record<string, string> = {
  bg: '--bg', surface: '--surface', surface2: '--surface-2', text: '--text', text2: '--text-2', text3: '--text-3',
  border: '--border', brand: '--brand', brandInk: '--brand-ink', brandTexto: '--brand-texto', accent: '--accent',
  accentInk: '--accent-ink', accentTexto: '--accent-texto', field: '--field', fieldBorder: '--field-border',
  verdeSolido: '--verde-solido', sobreVerdeSolido: '--sobre-verde-solido', okBg: '--okbg', okTexto: '--ok-texto',
  warnBg: '--warnbg', avisoTexto: '--aviso-texto', errBg: '--errbg', errTexto: '--err-texto', infoBg: '--infobg',
  infoTexto: '--info-texto', sombra1: '--sh-1', sombra2: '--sh-2', sombra3: '--sh-3',
}

describe('los tokens salen de la bóveda, no de la memoria de nadie', () => {
  it('la paleta entera', () => {
    comparar(raiz, Object.fromEntries(Object.entries(color).map(([k, v]) => [`--${k}`, v])))
  })

  it('las escalas', () => {
    comparar(raiz, Object.fromEntries(Object.entries(tokens.primitivas.espacio).map(([k, v]) => [`--${k}`, v])), 'px')
    comparar(raiz, Object.fromEntries(Object.entries(tokens.primitivas.radio).map(([k, v]) => [`--r-${k}`, v])), 'px')
    comparar(raiz, {
      '--font-d': tokens.primitivas.fuente.display,
      '--font-b': tokens.primitivas.fuente.cuerpo,
      '--mono': tokens.primitivas.fuente.mono,
    })
  })

  for (const [tema, declaraciones] of [['claro', raiz], ['oscuro', escuro]] as const) {
    it(`los roles del tema ${tema}`, () => {
      const roles = tokens.roles[tema] as Record<string, string>
      comparar(
        declaraciones,
        Object.fromEntries(Object.entries(MAPA_DE_ROLES).map(([rol, variable]) => [variable, resolver(roles[rol]!)])),
      )
    })
  }

  it('el rojo sólido sale de `.btn-danger`, que es donde la bóveda lo escribe', () => {
    const regla = /\.btn-danger\{background:(#[0-9a-f]{3,6});color:(#[0-9a-f]{3,6}|\w+)\}/i.exec(css)
    expect(regla, 'la bóveda ya no declara .btn-danger').not.toBeNull()
    expect(normalizar(regla![1]!)).toBe(normalizar(resolver(tokens.roles.claro.rojoSolido)))
    expect(normalizar(regla![2]!)).toBe(normalizar(resolver(tokens.roles.claro.sobreRojoSolido)))
    expect(tokens.roles.oscuro.rojoSolido).toBe(tokens.roles.claro.rojoSolido)
  })

  it('todo rol de un tema existe en el otro, y ninguno inventa un hex donde hay primitiva', () => {
    expect(Object.keys(tokens.roles.oscuro).sort()).toEqual(Object.keys(tokens.roles.claro).sort())
    const hexes = new Set(Object.values(color).map((v) => v.toLowerCase()))
    for (const tema of ['claro', 'oscuro'] as const) {
      for (const [rol, valor] of Object.entries(tokens.roles[tema])) {
        if (/^#/.test(valor) && hexes.has(valor.toLowerCase())) {
          throw new Error(`roles.${tema}.${rol} escribe ${valor} a mano: nombrá la primitiva`)
        }
      }
    }
  })

  it('el área táctil no baja de la pauta', () => {
    expect(tokens.primitivas.tactil.minimoWeb).toBeGreaterThanOrEqual(44)
    expect(tokens.primitivas.tactil.minimoMovil).toBeGreaterThanOrEqual(48)
  })
})
