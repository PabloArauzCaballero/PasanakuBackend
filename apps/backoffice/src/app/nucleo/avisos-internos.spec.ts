import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import {
  AvisoInternoTipoEnum,
  type AvisoInterno,
  type PaginaDeAvisosInternos,
  type SalidaMarcarTodosLeidos,
} from 'clientes/angular/notificaciones'
import { GATEWAY } from './gateway'
import { AvisosInternosServicio } from './avisos-internos'

describe('AvisosInternosServicio', () => {
  let servicio: AvisosInternosServicio
  let http: HttpTestingController

  const aviso1: AvisoInterno = {
    id: '11111111-1111-4111-8111-111111111111',
    titulo: 'Alerta de mora',
    mensaje: 'Grupo #12 con mora alta',
    tipo: AvisoInternoTipoEnum.Alerta,
    creadoEn: '2026-10-19T10:00:00Z',
  }

  const aviso2: AvisoInterno = {
    id: '22222222-2222-4222-8222-222222222222',
    titulo: 'Despliegue completado',
    mensaje: 'Versión 2.4 lista',
    tipo: AvisoInternoTipoEnum.Despliegue,
    leidoEn: '2026-10-19T10:05:00Z',
    creadoEn: '2026-10-19T09:00:00Z',
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AvisosInternosServicio,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'https://api.test' },
      ],
    })
    servicio = TestBed.inject(AvisosInternosServicio)
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => {
    http.verify()
  })

  it('cargar() recupera la lista de avisos y actualiza totalNoLeidos', () => {
    const respuesta: PaginaDeAvisosInternos = {
      totalNoLeidos: 1,
      elementos: [aviso1, aviso2],
    }

    servicio.cargar(true, 10).subscribe()

    const req = http.expectOne('https://api.test/notificaciones/avisos-internos?soloNoLeidos=true&limite=10')
    expect(req.request.method).toBe('GET')
    req.flush(respuesta)

    expect(servicio.avisos()).toEqual([aviso1, aviso2])
    expect(servicio.noLeidos()).toBe(1)
    expect(servicio.cargando()).toBe(false)
  })

  it('marcarLeido() marca el aviso y decrementa noLeidos', () => {
    // Primero cargamos un aviso no leído
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    expect(servicio.noLeidos()).toBe(1)

    const avisoLeido: AvisoInterno = { ...aviso1, leidoEn: '2026-10-19T10:10:00Z' }
    servicio.marcarLeido(aviso1.id).subscribe()

    const req = http.expectOne(`https://api.test/notificaciones/avisos-internos/${aviso1.id}/leido`)
    expect(req.request.method).toBe('POST')
    req.flush(avisoLeido)

    expect(servicio.noLeidos()).toBe(0)
    expect(servicio.avisos()[0]?.leidoEn).toBe('2026-10-19T10:10:00Z')
  })

  it('marcarTodosLeidos() pone totalNoLeidos en cero', () => {
    servicio.cargar().subscribe()
    http.expectOne((r) => r.url.includes('/notificaciones/avisos-internos')).flush({
      totalNoLeidos: 1,
      elementos: [aviso1],
    })
    expect(servicio.noLeidos()).toBe(1)

    const salida: SalidaMarcarTodosLeidos = { modificados: 1 }
    servicio.marcarTodosLeidos().subscribe()

    const req = http.expectOne('https://api.test/notificaciones/avisos-internos/marcar-todos-leidos')
    expect(req.request.method).toBe('POST')
    req.flush(salida)

    expect(servicio.noLeidos()).toBe(0)
    expect(servicio.avisos()[0]?.leidoEn).toBeDefined()
  })
})
