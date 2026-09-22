import { TestBed } from '@angular/core/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { afterEach, describe, expect, it } from 'vitest'
import { CONFIGURACION_GATEWAY, GATEWAY, gatewayPorDefecto, provideGateway, resolverGateway } from './gateway'

function ponerMeta(contenido: string | null): void {
  document.querySelectorAll('meta[name="aportaya-gateway"]').forEach((m) => m.remove())
  if (contenido === null) return
  const meta = document.createElement('meta')
  meta.setAttribute('name', 'aportaya-gateway')
  meta.setAttribute('content', contenido)
  document.head.appendChild(meta)
}

describe('resolverGateway', () => {
  afterEach(() => ponerMeta(null))

  it('sin la etiqueta y en producción: inválida, nunca localhost', () => {
    ponerMeta(null)
    const r = resolverGateway('produccion', { origenPropio: 'https://backoffice.aportaya.bo' })
    expect(r.valida).toBe(false)
    expect(JSON.stringify(r)).not.toContain('localhost')
  })

  it('con la etiqueta de mismo origen y producción: válida', () => {
    ponerMeta('/api/v1')
    const r = resolverGateway('produccion', { origenPropio: 'https://backoffice.aportaya.bo' })
    expect(r).toEqual({ valida: true, url: '/api/v1' })
  })

  it('con la etiqueta apuntando a otro host en producción: inválida', () => {
    ponerMeta('https://otra-cosa.example.com/api/v1')
    const r = resolverGateway('produccion', { origenPropio: 'https://backoffice.aportaya.bo' })
    expect(r.valida).toBe(false)
  })

  it('en desarrollo, sin etiqueta, sigue sin caer a localhost por sí sola (vacía es inválida, no un valor por omisión)', () => {
    ponerMeta(null)
    const r = resolverGateway('desarrollo', { origenPropio: 'http://localhost:4200' })
    expect(r.valida).toBe(false)
    expect(r.valida === false && r.motivo).toBe('vacia')
  })
})

describe('provideGateway · TestBed, en los dos modos', () => {
  afterEach(() => ponerMeta(null))

  it('en producción sin etiqueta: GATEWAY queda vacío, nunca localhost', () => {
    ponerMeta(null)
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...provideGateway('produccion', { origenPropio: 'https://backoffice.aportaya.bo' })],
    })
    expect(TestBed.inject(GATEWAY)).toBe('')
    expect(TestBed.inject(CONFIGURACION_GATEWAY).valida).toBe(false)
  })

  it('en producción con etiqueta válida (ruta relativa): GATEWAY resuelve la ruta', () => {
    ponerMeta('/api/v1')
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...provideGateway('produccion', { origenPropio: 'https://backoffice.aportaya.bo' })],
    })
    expect(TestBed.inject(GATEWAY)).toBe('/api/v1')
    expect(TestBed.inject(CONFIGURACION_GATEWAY).valida).toBe(true)
  })

  it('en desarrollo con Prism: GATEWAY resuelve localhost porque el modo lo permite explícitamente', () => {
    ponerMeta('http://localhost:4010/api/v1')
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...provideGateway('desarrollo', { origenPropio: 'http://localhost:4200' })],
    })
    expect(TestBed.inject(GATEWAY)).toBe('http://localhost:4010/api/v1')
  })
})

describe('gatewayPorDefecto', () => {
  afterEach(() => ponerMeta(null))

  it('sin etiqueta, nunca devuelve una URL que contenga "localhost"', () => {
    ponerMeta(null)
    expect(gatewayPorDefecto()).not.toContain('localhost')
  })

  it('con una etiqueta válida de mismo origen, la usa', () => {
    ponerMeta('/api/v1')
    expect(gatewayPorDefecto()).toBe('/api/v1')
  })
})
