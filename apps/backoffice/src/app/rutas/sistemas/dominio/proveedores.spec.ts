import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { ProveedorDePagoResumen } from 'clientes/angular/aportes'
import type { ProveedorDeMensajeriaResumen } from 'clientes/angular/notificaciones'
import type { CostoDeProveedor } from 'clientes/angular/tarifas'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeProveedores, periodoAnterior, puertoProveedores } from './proveedores'

const GW = 'https://gateway.test/api/v1'

// Sintético: la forma de los contratos de aportes, notificaciones y tarifas.
const qr = { id: 'p-qr', codigo: 'QRB', nombre: 'Pasarela QR', tipo: 'PASARELA', activo: true, soportaWebhook: true, prioridad: 1 } as ProveedorDePagoResumen
const sms = { id: 'm-sms', codigo: 'SMSB', nombre: 'SMS Bolivia', canales: 'SMS', activo: false, prioridad: 1, saludPorcentaje: '97.50' } as ProveedorDeMensajeriaResumen
const costo = (moneda: string, costoTotal: string) => ({ proveedorId: 'p-qr', periodo: '2026-09', moneda, costoTotal, operaciones: 2 }) as CostoDeProveedor

describe('proveedores · Sistemas › Proveedores contra aportes, notificaciones y tarifas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
  })

  it('correcto: el costo exacto va con su proveedor de pago; dos monedas son dos filas; la mensajería no tiene costo', () => {
    const filas = filasDeProveedores([qr], [sms], [costo('BOB', '0.30'), costo('USD', '1.50')], '2026-09')
    expect(filas.map((f) => [f.nombre, f.costoRealUltimoPeriodo, f.moneda, f.activo])).toEqual([
      ['Pasarela QR (QRB)', '0.30', 'BOB', 'Activo'],
      ['Pasarela QR (QRB)', '1.50', 'USD', 'Activo'],
      ['SMS Bolivia (SMSB)', '', '', 'Inactivo'],
    ])
    expect(new Set(filas.map((f) => f.id)).size).toBe(3)
    expect(filas[0]).toMatchObject({ categoria: 'Pagos · Pasarela', periodo: '2026-09' })
  })

  it('límite: sin costos (o si tarifas falló) el costo queda vacío, nunca cero; el período es el mes cerrado de La Paz', () => {
    expect(filasDeProveedores([qr], [], undefined, '2026-09')[0]).toMatchObject({ costoRealUltimoPeriodo: '', moneda: '' })
    expect(periodoAnterior(new Date('2026-11-01T03:00:00Z'))).toBe('2026-09') // 31-10 23:00 en La Paz
    expect(periodoAnterior(new Date('2026-01-15T12:00:00Z'))).toBe('2025-12')
  })

  it('inválido: si falla un listado el puerto rechaza; si falla tarifas, se ven los proveedores sin costo', async () => {
    const http = () => TestBed.inject(HttpTestingController)
    const ahora = () => new Date('2026-10-15T12:00:00Z')
    const sinCosto = TestBed.runInInjectionContext(() => puertoProveedores(ahora)).obtener()
    http().expectOne(`${GW}/pagos/proveedores`).flush([qr])
    http().expectOne(`${GW}/notificaciones/proveedores`).flush([])
    http().expectOne(`${GW}/tarifas/costos-de-proveedor?periodo=2026-09`).flush({}, { status: 503, statusText: 'x' })
    expect((await sinCosto)[0]).toMatchObject({ costoRealUltimoPeriodo: '' })
    const mal = TestBed.runInInjectionContext(() => puertoProveedores(ahora)).obtener()
    http().expectOne(`${GW}/pagos/proveedores`).flush({}, { status: 403, statusText: 'x' })
    http().expectOne(`${GW}/notificaciones/proveedores`).flush([])
    http().expectOne(`${GW}/tarifas/costos-de-proveedor?periodo=2026-09`).flush([])
    await expect(mal).rejects.toBeTruthy()
  })
})
