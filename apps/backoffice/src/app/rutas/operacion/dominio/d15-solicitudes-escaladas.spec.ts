import { provideHttpClient, HttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { cargarSolicitudesEscaladas, type SolicitudEscalada } from './d15-solicitudes-escaladas'

const VENCE_PRIMERO: SolicitudEscalada = {
  solicitudId: 's-1',
  grupoId: 'g-1',
  grupoCodigo: 'PSK-0042',
  usuarioId: 'u-1',
  usuarioNombre: 'Marcelo Rojas',
  canal: 'QR',
  puntajeCompatibilidad: 712,
  factores: [{ motivo: '2 pasanakus completos', aFavor: true }],
  fechaLimiteOrganizador: '2026-09-12T10:00:00-04:00',
  escaladaEn: '2026-09-10T10:00:00-04:00',
  estado: 'ESCALADA',
}

const VENCE_DESPUES: SolicitudEscalada = { ...VENCE_PRIMERO, solicitudId: 's-2', usuarioNombre: 'Julia Pérez', fechaLimiteOrganizador: '2026-09-20T10:00:00-04:00' }

describe('cargarSolicitudesEscaladas · el cargador de la tabla', () => {
  let http: HttpTestingController
  let cliente: HttpClient

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })
    http = TestBed.inject(HttpTestingController)
    cliente = TestBed.inject(HttpClient)
  })

  it('ordena por el plazo del organizador cuando no se pide un orden explícito: el que vence antes va primero', async () => {
    const cargador = cargarSolicitudesEscaladas(cliente, 'http://gw/api/v1')
    const promesa = cargador({ pagina: 1, tamano: 50, orden: null, filtros: {} })
    http.expectOne('http://gw/api/v1/grupos/solicitudes-escaladas').flush([VENCE_DESPUES, VENCE_PRIMERO])
    const pagina = await promesa
    expect(pagina.filas.map((s) => s.usuarioNombre)).toEqual(['Marcelo Rojas', 'Julia Pérez'])
  })

  it('el puntaje llega con sus factores, nunca solo el número', async () => {
    const cargador = cargarSolicitudesEscaladas(cliente, 'http://gw/api/v1')
    const promesa = cargador({ pagina: 1, tamano: 50, orden: null, filtros: {} })
    http.expectOne('http://gw/api/v1/grupos/solicitudes-escaladas').flush([VENCE_PRIMERO])
    const pagina = await promesa
    expect(pagina.filas[0]?.factores).toEqual([{ motivo: '2 pasanakus completos', aFavor: true }])
  })
})
