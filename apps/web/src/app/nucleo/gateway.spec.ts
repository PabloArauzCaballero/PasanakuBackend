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

describe('resolverGateway · sin la etiqueta, nunca cae a localhost', () => {
  afterEach(() => ponerMeta(null))

  it('en producción, sin etiqueta: inválida', () => {
    ponerMeta(null)
    const r = resolverGateway('produccion', { origenPropio: 'https://aportaya.bo' })
    expect(r.valida).toBe(false)
  })

  it('gatewayPorDefecto() nunca devuelve una URL con "localhost"', () => {
    ponerMeta(null)
    expect(gatewayPorDefecto()).not.toContain('localhost')
  })
})

describe('resolverGateway · mismo resultado con la ruta relativa, la forma que server.ts inyecta', () => {
  afterEach(() => ponerMeta(null))

  it('la ruta relativa que server.ts agrega en despliegue ("/api/v1") es válida en producción', () => {
    ponerMeta('/api/v1')
    const r = resolverGateway('produccion', { origenPropio: 'https://aportaya.bo' })
    expect(r).toEqual({ valida: true, url: '/api/v1' })
  })

  it('la misma ruta relativa, pasada por la variable de entorno que lee el servidor de SSR, da el mismo resultado', () => {
    // `resolverGateway` en plataforma servidor lee `process.env.APORTAYA_GATEWAY` antes
    // que la etiqueta (no hay `document` en el primer render). En este test, que corre
    // bajo jsdom, se fija la variable para comprobar que el validador es el mismo y
    // produce el mismo resultado para el mismo valor — la invariante de H3.S2.M2.
    process.env['APORTAYA_GATEWAY'] = '/api/v1'
    try {
      const r = resolverGateway('produccion', { origenPropio: 'https://aportaya.bo' })
      expect(r).toEqual({ valida: true, url: '/api/v1' })
    } finally {
      delete process.env['APORTAYA_GATEWAY']
    }
  })
})

describe('provideGateway · TestBed', () => {
  afterEach(() => ponerMeta(null))

  it('en producción sin etiqueta: GATEWAY vacío, nunca localhost', () => {
    ponerMeta(null)
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...provideGateway('produccion', { origenPropio: 'https://aportaya.bo' })],
    })
    expect(TestBed.inject(GATEWAY)).toBe('')
    expect(TestBed.inject(CONFIGURACION_GATEWAY).valida).toBe(false)
  })

  it('en producción con etiqueta válida: GATEWAY resuelve la ruta', () => {
    ponerMeta('/api/v1')
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), ...provideGateway('produccion', { origenPropio: 'https://aportaya.bo' })],
    })
    expect(TestBed.inject(GATEWAY)).toBe('/api/v1')
  })
})
