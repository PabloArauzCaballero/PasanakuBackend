import { describe, expect, it } from 'vitest'
import { ConfianzaDeRevision, DecisionDeRevision, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { OBLIGACION } from './catalogo-sintetico'
import { entradaDeRevision, faltaEnRevision, filtrarColumnas, metricasDeColumnas } from './revision'

const CON_REVISION: TablaDelCatalogo = {
  ...OBLIGACION,
  columnas: OBLIGACION.columnas.map((c, i) => (i === 1 ? { ...c, revision: { decision: DecisionDeRevision.Aprobada, confianza: ConfianzaDeRevision.Alta, version: 2 } } : c)),
}

/** CU-131 · cifras, filtros y el cuerpo del PUT, sin pantalla. */
describe('revisión de columnas', () => {
  it('correcto: las cifras cuentan columnas, personales, financieras, descritas y aprobadas', () => {
    expect(metricasDeColumnas(CON_REVISION)).toEqual({ columnas: 3, personales: 0, financieras: 1, descritas: 2, aprobadas: 1 })
  })

  it('límite: filtrar por «sin revisar», por sensibilidad y por texto sin acentos', () => {
    expect(filtrarColumnas(CON_REVISION, { q: '', sensibilidad: '', revision: 'sinRevisar' }).map((c) => c.nombre)).toEqual(['id', 'grupo_id'])
    expect(filtrarColumnas(CON_REVISION, { q: '', sensibilidad: 'FINANCIERO', revision: '' }).map((c) => c.nombre)).toEqual(['monto_esperado'])
    expect(filtrarColumnas(CON_REVISION, { q: 'PACTÓ', sensibilidad: '', revision: '' }).map((c) => c.nombre)).toEqual(['monto_esperado'])
  })

  it('inválido: rechazar sin motivo falta; el cuerpo no manda confianza ni motivo vacíos', () => {
    expect(faltaEnRevision({ decision: 'RECHAZADA', confianza: '', motivo: '  ' })).toEqual({ motivo: 'Para rechazar hay que decir por qué.' })
    expect(faltaEnRevision({ decision: '', confianza: '', motivo: '' })).toEqual({ decision: 'Elegí una decisión.' })
    expect(entradaDeRevision({ decision: 'APROBADA', confianza: '', motivo: ' ' }, 2)).toEqual({ decision: 'APROBADA', version: 2 })
    expect(entradaDeRevision({ decision: 'RECHAZADA', confianza: 'BAJA', motivo: ' raro ' }, 0)).toEqual({ decision: 'RECHAZADA', version: 0, confianza: 'BAJA', motivo: 'raro' })
  })
})
