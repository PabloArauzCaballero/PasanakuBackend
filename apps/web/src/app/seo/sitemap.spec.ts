import { describe, expect, it } from 'vitest'
import { RUTAS_EXCLUIDAS_SITEMAP, generarSitemap, rutaExcluidaDelSitemap } from './sitemap'

const BASE = 'https://aportaya.bo'

describe('sitemap', () => {
  it('gate F10: nunca incluye una ruta /verificar/*', () => {
    const rutas = [
      { ruta: '/', actualizado: '2026-01-01' },
      { ruta: '/verificar/plazo', actualizado: '2026-01-01' },
      { ruta: '/verificar/algo/mas', actualizado: '2026-01-01' },
    ]
    const xml = generarSitemap(rutas, BASE)
    expect(xml).not.toContain('/verificar/')
  })

  it('gate F10: nunca incluye una ruta /publico/*', () => {
    const rutas = [
      { ruta: '/', actualizado: '2026-01-01' },
      { ruta: '/publico/reglamento', actualizado: '2026-01-01' },
    ]
    const xml = generarSitemap(rutas, BASE)
    expect(xml).not.toContain('/publico/')
  })

  it('incluye las rutas indexables que sí corresponden', () => {
    const rutas = [{ ruta: '/plazos', actualizado: '2026-02-03' }]
    const xml = generarSitemap(rutas, BASE)
    expect(xml).toContain(`<loc>${BASE}/plazos</loc>`)
    expect(xml).toContain('<lastmod>2026-02-03</lastmod>')
  })

  it('produce XML de sitemap 0.9 válido en su estructura', () => {
    const xml = generarSitemap([{ ruta: '/', actualizado: '2026-01-01' }], BASE)
    expect(xml.startsWith('<?xml version="1.0" encoding="UTF-8"?>')).toBe(true)
    expect(xml).toContain('<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">')
    expect(xml).toContain('</urlset>')
  })

  it('rutaExcluidaDelSitemap detecta ambos prefijos prohibidos', () => {
    expect(rutaExcluidaDelSitemap('/verificar/plazo')).toBe(true)
    expect(rutaExcluidaDelSitemap('/publico/x')).toBe(true)
    expect(rutaExcluidaDelSitemap('/plazos')).toBe(false)
  })

  it('la lista de exclusión no está vacía (defensa contra un vaciado accidental)', () => {
    expect(RUTAS_EXCLUIDAS_SITEMAP.length).toBeGreaterThan(0)
    expect(RUTAS_EXCLUIDAS_SITEMAP).toEqual(expect.arrayContaining(['/verificar/', '/publico/']))
  })
})
