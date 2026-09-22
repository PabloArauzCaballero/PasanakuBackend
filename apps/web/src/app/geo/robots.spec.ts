import { describe, expect, it } from 'vitest'
import { BUSCADORES_PERMITIDOS, ENTRENAMIENTO_BLOQUEADO, RUTAS_NO_INDEXABLES, robotsTxt } from './robots'

describe('robots.txt · búsqueda sí, entrenamiento no (ADR-042)', () => {
  const texto = robotsTxt('https://aportaya.bo')

  it('los agentes de búsqueda pasan', () => {
    for (const agente of BUSCADORES_PERMITIDOS) expect(texto).toContain(`User-agent: ${agente}`)
    expect(texto).toContain('Allow: /')
  })

  it('los agentes de entrenamiento no pasan, y ninguno queda con Allow', () => {
    for (const agente of ENTRENAMIENTO_BLOQUEADO) {
      const bloque = texto.split(`User-agent: ${agente}`)[1]?.split('User-agent: *')[0] ?? ''
      expect(bloque).toContain('Disallow: /')
      expect(bloque).not.toContain('Allow: /')
    }
  })

  it('las rutas con datos de personas quedan fuera para todos', () => {
    const paraTodos = texto.split('User-agent: *')[1] ?? ''
    for (const ruta of RUTAS_NO_INDEXABLES) expect(paraTodos).toContain(`Disallow: ${ruta}`)
    expect(ENTRENAMIENTO_BLOQUEADO.length).toBeGreaterThanOrEqual(4)
    expect(texto).toContain('Sitemap: https://aportaya.bo/sitemap.xml')
  })
})
