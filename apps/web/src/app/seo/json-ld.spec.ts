import { describe, expect, it } from 'vitest'
import { TIPOS_PROHIBIDOS, migaDePan, organizacion, paginaWeb, preguntasFrecuentes, sinTiposProhibidos, sitioWeb } from './json-ld'

describe('json-ld · gate F10', () => {
  it('organizacion(), sitioWeb(), paginaWeb() y migaDePan() nunca llevan un tipo prohibido', () => {
    const bloques = [
      organizacion(),
      sitioWeb(),
      paginaWeb({ titulo: 'Plazos', descripcion: 'x', ruta: '/plazos' }),
      migaDePan([{ nombre: 'Inicio', ruta: '/' }]),
    ]
    for (const b of bloques) expect(sinTiposProhibidos(b)).toBe(true)
  })

  it('sinTiposProhibidos() detecta Review, AggregateRating y FinancialService', () => {
    for (const tipo of TIPOS_PROHIBIDOS) {
      expect(sinTiposProhibidos({ '@type': tipo })).toBe(false)
    }
  })

  it('cada @type de schema.org emitido es válido (forma mínima: @context + @type)', () => {
    for (const b of [organizacion(), sitioWeb(), paginaWeb({ titulo: 't', descripcion: 'd', ruta: '/' })]) {
      expect(b['@context']).toBe('https://schema.org')
      expect(typeof b['@type']).toBe('string')
    }
  })
  it('preguntasFrecuentes() arma un FAQPage válido a partir de pares reales de pregunta/respuesta', () => {
    const faq = preguntasFrecuentes([{ pregunta: '¿Cobra intereses?', respuesta: 'No.' }])
    expect(faq['@type']).toBe('FAQPage')
    expect(faq.mainEntity).toHaveLength(1)
    expect(faq.mainEntity[0]?.name).toBe('¿Cobra intereses?')
    expect(faq.mainEntity[0]?.acceptedAnswer.text).toBe('No.')
    expect(sinTiposProhibidos(faq)).toBe(true)
  })
})

// El grep negativo sobre el código fuente (Review/AggregateRating/FinancialService fuera de
// esta lista) corre por separado con `grep`, no acá: el runner de pruebas ejecuta sobre el
// bundle, no sobre el disco, y `readFileSync(import.meta.url)` no resuelve en ese entorno.
// Ver el comando en el gate del informe: `grep -rn "Review\|AggregateRating\|FinancialService" src/app/seo --include=*.ts --include=*.mjs`.
