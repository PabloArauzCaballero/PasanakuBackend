import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CuentaDeServicio } from '../dominio/gobierno-de-roles'
import { GW_DE_PRUEBA } from '../compartido/pruebas-http.apoyo'
import { estabilizar, texto } from '../compartido/pruebas.apoyo'
import { PantallaCuentasDeServicio } from './pantalla-cuentas-de-servicio'

const CUENTAS: CuentaDeServicio[] = [
  { id: 'c-1', usuarioId: 'u-1', rolCodigo: 'SERVICIO_ENTREGAS', nombre: 'Servicio de entregas', estado: 'ACTIVA', venceEn: '2027-01-15T10:30:00Z', creadaEn: '2026-11-01T10:00:00Z', credencialRotadaEn: '2026-11-01T10:00:00Z' },
  { id: 'c-2', usuarioId: 'u-2', rolCodigo: 'SERVICIO_GRUPOS', nombre: 'Servicio de grupos', estado: 'REVOCADA', venceEn: '2026-12-15T10:30:00Z', creadaEn: '2026-10-01T10:00:00Z', credencialRotadaEn: '2026-10-01T10:00:00Z', revocadaEn: '2026-11-10T10:00:00Z' },
]

function montar() {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW_DE_PRUEBA }] })
  const http = TestBed.inject(HttpTestingController)
  const f = TestBed.createComponent(PantallaCuentasDeServicio)
  f.detectChanges()
  return { f, http }
}

describe('PantallaCuentasDeServicio (doble del gateway, regla 65)', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify())

  it('con datos: lista las cuentas, y solo las activas ofrecen rotar y revocar', async () => {
    const { f, http } = montar()
    http.expectOne(`${GW_DE_PRUEBA}/cuentas-servicio`).flush(CUENTAS)
    const el = await estabilizar(f)
    expect(texto(el)).toContain('Servicio de entregas')
    expect(texto(el)).toContain('SERVICIO_GRUPOS')
    const botones = Array.from(el.querySelectorAll('tbody button')).map((b) => texto(b))
    expect(botones).toEqual(['Rotar clave', 'Revocar'])
  })

  it('vacío: explica por qué y qué hacer', async () => {
    const { f, http } = montar()
    http.expectOne(`${GW_DE_PRUEBA}/cuentas-servicio`).flush([])
    const el = await estabilizar(f)
    expect(texto(el)).toContain('Todavía no hay cuentas de servicio registradas')
    expect(el.querySelector('table')).toBeNull()
  })

  it('error: dice que algo falló y ofrece reintentar, sin mostrar el texto crudo del servidor', async () => {
    const { f, http } = montar()
    http.expectOne(`${GW_DE_PRUEBA}/cuentas-servicio`).flush({ mensaje: 'java.lang.NullPointerException' }, { status: 500, statusText: 'x' })
    const el = await estabilizar(f)
    expect(texto(el)).not.toContain('NullPointerException')
    expect(texto(el)).toContain('Reintentar')
  })

  it('la credencial entregada no está en el listado ni en la página al cargar', async () => {
    const { f, http } = montar()
    http.expectOne(`${GW_DE_PRUEBA}/cuentas-servicio`).flush(CUENTAS)
    const el = await estabilizar(f)
    expect(texto(el)).not.toContain('no se vuelve a mostrar')
  })
})
