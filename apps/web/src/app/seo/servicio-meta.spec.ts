import { describe, expect, it } from 'vitest'
import { espejoMarkdownDe } from './servicio-meta'

describe('espejoMarkdownDe · el patrón del espejo Markdown (F10 ↔ F11)', () => {
  it('la portada va a /index.md, igual que scripts/contenido.mjs', () => {
    expect(espejoMarkdownDe('/')).toBe('/index.md')
  })

  it('cualquier otra ruta pierde la barra inicial y gana .md', () => {
    expect(espejoMarkdownDe('/como-funciona')).toBe('/como-funciona.md')
    expect(espejoMarkdownDe('/legal/estado-regulatorio')).toBe('/legal/estado-regulatorio.md')
  })
})
