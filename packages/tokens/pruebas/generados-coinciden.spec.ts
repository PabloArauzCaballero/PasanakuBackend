import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import tokens from '../tokens.json'

/**
 * Los dos generados salen del mismo JSON y tienen que exponer exactamente los mismos
 * roles. Si un día `a-dart.mjs` se queda atrás de `a-css.mjs`, la app y la web dejan de
 * hablar el mismo idioma sin que nadie lo vea en un `git diff`.
 */
const RAIZ = resolve(__dirname, '..')
execFileSync('node', [resolve(RAIZ, 'scripts/a-css.mjs')], { stdio: 'ignore' })
execFileSync('node', [resolve(RAIZ, 'scripts/a-dart.mjs')], { stdio: 'ignore' })
const css = readFileSync(resolve(RAIZ, 'generado/tokens.css'), 'utf8')
const dart = readFileSync(resolve(RAIZ, 'generado/tokens.dart'), 'utf8')
const roles = Object.keys(tokens.roles.claro)
const aKebab = (n: string) => n.replace(/[A-Z]/g, (l) => `-${l.toLowerCase()}`).replace(/([a-z])(\d)/g, '$1-$2')

describe('tokens.css y tokens.dart', () => {
  it('declaran los mismos roles', () => {
    for (const rol of roles) {
      expect(css, `--${aKebab(rol)} falta en el CSS`).toContain(`--${aKebab(rol)}:`)
      expect(dart, `${rol} falta en el Dart`).toMatch(new RegExp(`final (Color|BoxShadow) ${rol};`))
    }
  })

  it('el Dart lleva los dos temas y ningún hex que no esté en tokens.json', () => {
    expect(dart).toContain('static const claro = Tokens(')
    expect(dart).toContain('static const oscuro = Tokens(')
    const hexesDelJson = new Set(
      [...Object.values(tokens.primitivas.color), ...Object.values(tokens.roles.claro), ...Object.values(tokens.roles.oscuro)]
        .filter((v) => /^#/.test(v))
        .map((v) => v.slice(1).toUpperCase()),
    )
    for (const m of dart.matchAll(/Color\(0xFF([0-9A-F]{6})\)/g)) {
      expect(hexesDelJson.has(m[1]!), `el Dart inventó ${m[1]}`).toBe(true)
    }
  })

  it('el CSS lleva el tema del sistema y los dos forzados', () => {
    expect(css).toContain("@media (prefers-color-scheme: dark)")
    expect(css).toContain(":root[data-theme='light']")
    expect(css).toContain(":root[data-theme='dark']")
  })
})
