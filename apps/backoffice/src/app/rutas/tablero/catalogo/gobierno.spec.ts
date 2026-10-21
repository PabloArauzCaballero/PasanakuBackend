import { describe, expect, it } from 'vitest'
import { ColumnaDelCatalogoSensibilidadEnum as S, EstadoDeRevision, EstadoDeTabla, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { tabla } from './catalogo-sintetico'
import { entradaDe, gobiernoDe, tieneDatosPersonales } from './gobierno'

const con = (t: TablaDelCatalogo, estado: EstadoDeTabla, revision: EstadoDeRevision): TablaDelCatalogo => ({
  ...t,
  gobierno: { estado, estadoRevision: revision, criticaParaAuditoria: false, version: 1 },
})

const ACTIVA = tabla('a', 'aportes')
const RETIRO = con(tabla('b', 'grupos', { modulo: '02', nombreModulo: 'Grupos' }), EstadoDeTabla.CandidataARetiro, EstadoDeRevision.Aprobada)
const PERSONAL = tabla('c', 'identidad', {
  columnas: [{ nombre: 'correo', tipo: 'VARCHAR', nulable: false, pk: false, sistema: 's', sensibilidad: S.Personal }],
})

/** CU-130 · la ficha por omisión y el cuerpo del PUT, sin pantalla (filtros y cifras los calcula el servidor). */
describe('gobierno del catálogo', () => {
  it('correcto: sin ficha vale activa y detectada; un secreto cuenta como dato personal', () => {
    expect(gobiernoDe(ACTIVA)).toMatchObject({ estado: 'ACTIVA', estadoRevision: 'DETECTADA', version: 0 })
    expect(gobiernoDe(RETIRO).estado).toBe('CANDIDATA_A_RETIRO')
    expect(tieneDatosPersonales(PERSONAL)).toBe(true)
    expect(tieneDatosPersonales(ACTIVA)).toBe(false)
  })

  it('inválido: el cuerpo no manda cadenas vacías ni espacios (el contrato exige mínimo 1)', () => {
    const entrada = entradaDe(
      { estado: 'ACTIVA', estadoRevision: 'APROBADA', responsable: '   ', politicaRetencionId: '', criticaParaAuditoria: true, notaRevision: ' revisada ' },
      4,
    )
    expect(entrada).toEqual({ estado: 'ACTIVA', estadoRevision: 'APROBADA', criticaParaAuditoria: true, version: 4, notaRevision: 'revisada' })
  })
})
