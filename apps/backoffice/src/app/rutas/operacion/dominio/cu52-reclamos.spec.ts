import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { HttpClient } from '@angular/common/http'
import { beforeEach, describe, expect, it } from 'vitest'
import { cargarReclamos, type ReclamoDeBandeja } from './cu52-reclamos'

const RECLAMO_QUE_VENCE_PRIMERO: ReclamoDeBandeja = {
  reclamoId: 'r-1',
  codigo: 'REC-2026-08-0157',
  categoria: 'COMISION',
  canalIngreso: 'APP',
  fechaIngreso: '2026-08-01T10:00:00-04:00',
  plazoRespuesta: '2026-08-06T10:00:00-04:00',
  diasHabilesPlazo: 5,
  estado: 'EN_ANALISIS',
  responsableId: 'op-1',
  responsableNombre: 'Ana',
}

const RECLAMO_QUE_VENCE_DESPUES: ReclamoDeBandeja = { ...RECLAMO_QUE_VENCE_PRIMERO, reclamoId: 'r-2', codigo: 'REC-2026-08-0158', estado: 'INGRESADO', plazoRespuesta: '2026-08-20T10:00:00-04:00' }

describe('cargarReclamos · el cargador de la tabla', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('ordena por vencimiento cuando no se pide un orden explícito: el que vence antes va primero', async () => {
    const cargador = cargarReclamos(cliente, 'http://gw/api/v1')
    const promesa = cargador({ pagina: 1, tamano: 50, orden: null, filtros: {} })
    http.expectOne('http://gw/api/v1/reclamos').flush([RECLAMO_QUE_VENCE_DESPUES, RECLAMO_QUE_VENCE_PRIMERO])
    const pagina = await promesa
    expect(pagina.filas.map((r) => r.codigo)).toEqual(['REC-2026-08-0157', 'REC-2026-08-0158'])
    expect(pagina.total).toBe(2)
  })

  it('filtra por estado cuando la URL lo trae', async () => {
    const cargador = cargarReclamos(cliente, 'http://gw/api/v1')
    const promesa = cargador({ pagina: 1, tamano: 50, orden: null, filtros: { estado: 'EN_ANALISIS' } })
    http.expectOne('http://gw/api/v1/reclamos').flush([RECLAMO_QUE_VENCE_DESPUES, RECLAMO_QUE_VENCE_PRIMERO])
    const pagina = await promesa
    expect(pagina.filas).toHaveLength(1)
    expect(pagina.filas[0]?.estado).toBe('EN_ANALISIS')
  })

  it('bandeja vacía: cero filas, sin romper', async () => {
    const cargador = cargarReclamos(cliente, 'http://gw/api/v1')
    const promesa = cargador({ pagina: 1, tamano: 50, orden: null, filtros: {} })
    http.expectOne('http://gw/api/v1/reclamos').flush([])
    const pagina = await promesa
    expect(pagina.total).toBe(0)
  })
})
