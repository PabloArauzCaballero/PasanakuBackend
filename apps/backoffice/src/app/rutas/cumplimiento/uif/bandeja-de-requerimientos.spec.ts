import { describe, expect, it } from 'vitest'
import type { RequerimientoEnLista } from 'clientes/angular/cumplimiento'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { asentar, bandejaDe, filasDe, GW, montar, rechazar, textoDe } from '../compartido/pruebas.apoyo'
import { BandejaDeRequerimientos } from './bandeja-de-requerimientos'

const URL = `${GW}/uif/requerimientos`
const REQ = {
  requerimientoId: '6b0c1d00-0000-4000-8000-000000000001',
  autoridad: 'FISCALIA',
  numeroOficio: 'OF-2026-114',
  fechaRecepcion: '2026-10-01T13:00:00Z',
  plazoRespuesta: '2026-10-15T13:00:00Z',
  estado: 'RECIBIDO',
  usuarioAfectadoId: '1a2b3c4d-0000-4000-8000-000000000099',
  conBloqueoDeSaldo: true,
}

describe('BandejaDeRequerimientos · CU-45 listarRequerimientosDeAutoridad', () => {
  it('correcto: filas del contrato; la persona alcanzada no se muestra; acciones solo para el rol OFICIAL_CUMPLIMIENTO', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, ['AUDITORIA_LEER'], 'OFICIAL_CUMPLIMIENTO')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.get('limite')).toBe('25')
    pedido.flush({ items: [REQ], proximoAntesDe: REQ.fechaRecepcion, proximoAntesDeId: REQ.requerimientoId })
    await asentar(fixture)
    expect(filasDe<RequerimientoEnLista>(fixture)[0]?.numeroOficio).toBe('OF-2026-114')
    expect(textoDe(fixture)).not.toContain(REQ.usuarioAfectadoId)
    expect(textoDe(fixture)).toContain('Registrar un requerimiento')
    http.verify()
  })

  it('con AUDITORIA_LEER sin el rol, se lee pero no se registra ni se responde', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    http.expectOne((r) => r.url === URL).flush({ items: [REQ] })
    await asentar(fixture)
    expect(textoDe(fixture)).not.toContain('Registrar un requerimiento')
    expect(fixture.nativeElement.querySelector('ap-responder-requerimiento')).toBeNull()
  })

  it('límite: vacío con su porqué; el filtro de estado viaja al servidor', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    http.expectOne((r) => r.url === URL).flush({ items: [] })
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No hay requerimientos en estos estados.')
    bandejaDe<Bandeja<RequerimientoEnLista>>(fixture).filtrar('estados', 'VENCIDO')
    const pedido = http.expectOne((r) => r.url === URL)
    expect(pedido.request.params.getAll('estados')).toEqual(['VENCIDO'])
    pedido.flush({ items: [] })
  })

  it('inválido: 403 → «sin acceso»', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    rechazar(http.expectOne((r) => r.url === URL), 403)
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('No tenés acceso a esta bandeja.')
  })

  it('inválido: 422 AP-CU45-05 → filtro inválido', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, ['AUDITORIA_LEER'], 'AUDITOR_INTERNO')
    rechazar(http.expectOne((r) => r.url === URL), 422, 'AP-CU45-05')
    await asentar(fixture)
    expect(textoDe(fixture)).toContain('El filtro de la lista no es válido')
  })

  it('acción de fila «Responder»: abre la respuesta del oficio; un oficio respondido no la ofrece', async () => {
    const { fixture, http } = montar(BandejaDeRequerimientos, [], 'OFICIAL_CUMPLIMIENTO')
    http.expectOne((r) => r.url === URL).flush({ items: [REQ, { ...REQ, requerimientoId: 'r-2', numeroOficio: 'OF-2026-115', estado: 'RESPONDIDO' }] })
    await asentar(fixture)
    expect(fixture.nativeElement.querySelector('[aria-label="Responder el oficio OF-2026-115"]')).toBeNull()
    ;(fixture.nativeElement.querySelector('[aria-label="Responder el oficio OF-2026-114"]') as HTMLButtonElement).click()
    fixture.detectChanges()
    expect(fixture.componentInstance['elegido']()?.numeroOficio).toBe('OF-2026-114')
    expect(textoDe(fixture)).not.toMatch(/Editar|Eliminar/)
  })
})
