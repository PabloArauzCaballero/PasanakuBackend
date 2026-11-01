import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { WebhookRecibido } from 'clientes/angular/aportes'
import type { EventoDeEntregaRecibido } from 'clientes/angular/notificaciones'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeWebhooks, puertoWebhooks, TOPE_DE_WEBHOOKS } from './webhooks'

const GW = 'https://gateway.test/api/v1'
const PAGOS = `${GW}/pagos/webhooks-recibidos?limite=${TOPE_DE_WEBHOOKS}`
const MENSAJES = `${GW}/notificaciones/webhooks-recibidos?limite=${TOPE_DE_WEBHOOKS}`

// Sintético: la forma de los contratos de aportes y notificaciones.
const pago = (id: string, estado: string, recibidoEn: string, firmaValida = true) =>
  ({ id, proveedorCodigo: 'QRB', evento: 'pago.confirmado', estado, firmaValida, recibidoEn, intentos: 1 }) as WebhookRecibido
const acuse = (id: string, tipoEvento: string, recibidoEn: string, codigoError?: string) =>
  ({ id, proveedorCodigo: 'SMSB', canal: 'SMS', tipoEvento, recibidoEn, fechaHoraProveedor: recibidoEn, ...(codigoError ? { codigoError } : {}) }) as EventoDeEntregaRecibido

describe('webhooks · Sistemas › Webhooks contra aportes y notificaciones', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
  })

  it('correcto: une las dos fuentes, del más reciente al más viejo, con su estado en palabras', () => {
    const filas = filasDeWebhooks([pago('p1', 'PROCESADO', '2026-10-29T10:00:00Z')], [acuse('m1', 'FALLIDO', '2026-10-29T11:00:00Z', '131026')])
    expect(filas.map((f) => [f.id, f.ultimoEstado, f.detalle])).toEqual([
      ['mensajeria:m1', 'error', 'Falló · código 131026'],
      ['pagos:p1', 'ok', 'Procesado'],
    ])
    expect(filas.every((f) => f.direccion === 'entrante')).toBe(true)
    expect(filas[1]!.origen).toBe('Pasarela QRB · pago.confirmado')
  })

  it('límite: firma inválida es error aunque se haya descartado; un estado nuevo cuenta como error; sin nada, vacío', () => {
    const [f] = filasDeWebhooks([pago('p1', 'DESCARTADO', '2026-10-29T10:00:00Z', false)], [])
    expect(f).toMatchObject({ ultimoEstado: 'error', detalle: 'Descartado · firma inválida' })
    const [nuevo] = filasDeWebhooks([], [acuse('m1', 'REBOTADO', '2026-10-29T10:00:00Z')])
    expect(nuevo).toMatchObject({ ultimoEstado: 'error', detalle: 'REBOTADO' })
    expect(filasDeWebhooks([], [])).toEqual([])
  })

  it('inválido: si una fuente falla, el puerto rechaza (no muestra media lista como si fuera toda)', async () => {
    const http = () => TestBed.inject(HttpTestingController)
    const ok = TestBed.runInInjectionContext(() => puertoWebhooks()).obtener()
    http().expectOne(PAGOS).flush([pago('p1', 'PROCESADO', '2026-10-29T10:00:00Z')])
    http().expectOne(MENSAJES).flush([])
    expect((await ok).length).toBe(1)
    const mal = TestBed.runInInjectionContext(() => puertoWebhooks()).obtener()
    http().expectOne(PAGOS).flush([])
    http().expectOne(MENSAJES).flush({}, { status: 500, statusText: 'x' })
    await expect(mal).rejects.toBeTruthy()
  })
})
