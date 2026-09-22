import { describe, expect, it } from 'vitest'
import { RUTAS_EXCLUIDAS_LLMS, generarLlmsFullTxt, generarLlmsTxt, rutaExcluidaDeLlms } from './llms'

const BASE = 'https://aportaya.bo'

const PAGINAS = [
  { ruta: '/', titulo: 'Inicio', bajada: 'Qué es AportaYa.', indexable: true, actualizado: '2026-09-09', markdown: '## ¿Qué es un pasanaku?\n\nEs ahorro rotativo.' },
  { ruta: '/tarifas', titulo: 'Tarifas', bajada: 'Cuánto cobra AportaYa.', indexable: true, actualizado: '2026-09-09', markdown: '## Tarifario\n\nBs 10.' },
  { ruta: '/catalogo', titulo: 'Catálogo', bajada: 'Interno.', indexable: false, actualizado: '2026-09-09', markdown: 'interno' },
  { ruta: '/verificar/x', titulo: 'Certificado', bajada: 'Dato de un tercero.', indexable: true, actualizado: '2026-09-09', markdown: 'no debería aparecer' },
  { ruta: '/publico/grupos/x', titulo: 'Grupo', bajada: 'Dato de un tercero.', indexable: true, actualizado: '2026-09-09', markdown: 'no debería aparecer' },
]

describe('llms.txt · el índice para modelos (F11.2)', () => {
  const texto = generarLlmsTxt(PAGINAS, BASE)

  it('lista las páginas indexables sin datos de terceros', () => {
    expect(texto).toContain('[Inicio](https://aportaya.bo/index.md)')
    expect(texto).toContain('[Tarifas](https://aportaya.bo/tarifas.md)')
  })

  it('NO expone ninguna ruta con datos de terceros', () => {
    expect(texto).not.toContain('/verificar/')
    expect(texto).not.toContain('/publico/')
    expect(texto).not.toContain('/catalogo')
  })

  it('es determinista: la misma entrada produce el mismo texto', () => {
    expect(generarLlmsTxt(PAGINAS, BASE)).toBe(texto)
  })
})

describe('llms-full.txt · el contenido completo (F11.2)', () => {
  const texto = generarLlmsFullTxt(PAGINAS, BASE)

  it('concatena el markdown de las páginas indexables sin terceros', () => {
    expect(texto).toContain('¿Qué es un pasanaku?')
    expect(texto).toContain('Tarifario')
  })

  it('NO expone ninguna ruta con datos de terceros', () => {
    expect(texto).not.toContain('no debería aparecer')
    expect(texto).not.toContain('/verificar/')
    expect(texto).not.toContain('/publico/')
  })
})

describe('rutaExcluidaDeLlms', () => {
  it('excluye /verificar/, /publico/, /catalogo y /api/', () => {
    for (const prefijo of RUTAS_EXCLUIDAS_LLMS) expect(rutaExcluidaDeLlms(`${prefijo}algo`)).toBe(true)
    expect(rutaExcluidaDeLlms('/tarifas')).toBe(false)
  })
})
