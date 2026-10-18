import { describe, expect, it } from 'vitest'
import { ColumnaDelCatalogoSensibilidadEnum as S, EstadoDeRevision, EstadoDeTabla, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { tabla } from './catalogo-sintetico'
import { aplicarGobierno, entradaDe, metricas, modulos } from './gobierno'

const con = (t: TablaDelCatalogo, estado: EstadoDeTabla, revision: EstadoDeRevision): TablaDelCatalogo => ({
  ...t,
  gobierno: { estado, estadoRevision: revision, criticaParaAuditoria: false, version: 1 },
})

const ACTIVA = tabla('a', 'aportes')
const RETIRO = con(tabla('b', 'grupos', { modulo: '02', nombreModulo: 'Grupos' }), EstadoDeTabla.CandidataARetiro, EstadoDeRevision.Aprobada)
const PERSONAL = tabla('c', 'identidad', {
  columnas: [{ nombre: 'correo', tipo: 'VARCHAR', nulable: false, pk: false, sistema: 's', sensibilidad: S.Personal }],
})

/** CU-130 · filtros, cifras y el cuerpo del PUT, sin pantalla. */
describe('gobierno del catálogo', () => {
  it('correcto: sin ficha cuenta como activa y sin revisar; las cifras salen de todo el catálogo', () => {
    expect(metricas([ACTIVA, RETIRO, PERSONAL])).toEqual({ total: 3, activas: 2, aRevisar: 2, conDatosPersonales: 1, haciaElRetiro: 1 })
  })

  it('límite: filtrar por estado, revisión y módulo a la vez; vacío es «todos»', () => {
    const todas = [ACTIVA, RETIRO, PERSONAL]
    expect(aplicarGobierno(todas, { estado: '', revision: '', modulo: '' })).toHaveLength(3)
    expect(aplicarGobierno(todas, { estado: 'CANDIDATA_A_RETIRO', revision: 'APROBADA', modulo: '02' }).map((t) => t.tabla)).toEqual(['b'])
    expect(aplicarGobierno(todas, { estado: 'CANDIDATA_A_RETIRO', revision: '', modulo: '03' })).toEqual([])
    expect(modulos(todas).map((m) => m.texto)).toEqual(['02 · Grupos', '03 · Aportes'])
  })

  it('inválido: el cuerpo no manda cadenas vacías ni espacios (el contrato exige mínimo 1)', () => {
    const entrada = entradaDe(
      { estado: 'ACTIVA', estadoRevision: 'APROBADA', responsable: '   ', politicaRetencionId: '', criticaParaAuditoria: true, notaRevision: ' revisada ' },
      4,
    )
    expect(entrada).toEqual({ estado: 'ACTIVA', estadoRevision: 'APROBADA', criticaParaAuditoria: true, version: 4, notaRevision: 'revisada' })
  })
})
