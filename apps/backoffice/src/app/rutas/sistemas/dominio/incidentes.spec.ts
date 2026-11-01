import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { IncidenteOperativo, PaginaDeIncidentesOperativos } from 'clientes/angular/auditoria'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeIncidentes, puertoIncidentes, TOPE_DE_INCIDENTES } from './incidentes'

const GW = 'https://gateway.test/api/v1'
const URL = `${GW}/indicadores/operacion/incidentes?pagina=1&limite=${TOPE_DE_INCIDENTES}`

// Sintético: la forma de `IncidenteOperativo` (auditoria.yaml).
const inc = (codigo: string, severidad: string, estado: string): IncidenteOperativo =>
  ({ id: `id-${codigo}`, codigo, titulo: 'Latencia alta', severidad, sistemaAfectado: 'aportes', estado, detectadoEn: '2026-10-20T14:00:00Z', impactoUsuarios: 3 }) as IncidenteOperativo
const pagina = (items: IncidenteOperativo[]): PaginaDeIncidentesOperativos => ({ items, pagina: 1, limite: TOPE_DE_INCIDENTES, total: items.length, totalPaginas: items.length ? 1 : 0 })

describe('incidentes · Sistemas › Incidentes contra GET /indicadores/operacion/incidentes', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
  })

  it('correcto: severidad y etapa en palabras; detectado y mitigado siguen abiertos, post mortem y resuelto cerrados', () => {
    const filas = filasDeIncidentes(pagina([inc('I-1', 'SEV1', 'DETECTADO'), inc('I-2', 'SEV2', 'MITIGADO'), inc('I-3', 'SEV3', 'POST_MORTEM'), inc('I-4', 'SEV2', 'RESUELTO')]))
    expect(filas.map((f) => [f.severidad, f.estado, f.etapa])).toEqual([
      ['sev1', 'abierto', 'Detectado'],
      ['sev2', 'abierto', 'Mitigado'],
      ['sev3', 'cerrado', 'Post mortem'],
      ['sev2', 'cerrado', 'Resuelto'],
    ])
    expect(filas[0]).toMatchObject({ titulo: 'I-1 · Latencia alta', sistema: 'aportes', abiertoElIso: '2026-10-20T14:00:00Z' })
  })

  it('límite: sin incidentes, ninguna fila; un valor nuevo del contrato no rompe: se trata como abierto y crítico', () => {
    expect(filasDeIncidentes(pagina([]))).toEqual([])
    const [nuevo] = filasDeIncidentes(pagina([inc('I-9', 'SEV0', 'ESCALADO')]))
    expect(nuevo).toMatchObject({ severidad: 'sev1', estado: 'abierto', etapa: 'ESCALADO' })
  })

  it('correcto e inválido: el puerto pide los más recientes; con 403 o 500 rechaza y no inventa filas', async () => {
    const http = () => TestBed.inject(HttpTestingController)
    const ok = TestBed.runInInjectionContext(() => puertoIncidentes()).obtener()
    http().expectOne(URL).flush(pagina([inc('I-1', 'SEV1', 'DETECTADO')]))
    expect((await ok).length).toBe(1)
    for (const status of [403, 500]) {
      const mal = TestBed.runInInjectionContext(() => puertoIncidentes()).obtener()
      http().expectOne(URL).flush({}, { status, statusText: 'x' })
      await expect(mal).rejects.toBeTruthy()
    }
  })
})
