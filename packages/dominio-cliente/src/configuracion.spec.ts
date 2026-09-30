import { describe, expect, it } from 'vitest'
import { validarConfiguracion } from './configuracion'

const ORIGEN = 'https://backoffice.aportaya.bo'

describe('validarConfiguracion · casos válidos', () => {
  it('ruta relativa en producción', () => {
    expect(validarConfiguracion('/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: true, url: '/api/v1' })
  })

  it('mismo origen con TLS', () => {
    expect(validarConfiguracion(`${ORIGEN}/api/v1`, { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: true,
      url: `${ORIGEN}/api/v1`,
    })
  })

  it('el simulado (Prism) solo se acepta en desarrollo', () => {
    expect(validarConfiguracion('http://localhost:4010/api/v1', { modo: 'desarrollo', origenPropio: 'http://localhost:4200' })).toEqual({
      valida: true,
      url: 'http://localhost:4010/api/v1',
    })
  })

  it('un host ajeno explícitamente permitido por infra', () => {
    const r = validarConfiguracion('https://otro.aportaya.bo/api/v2', {
      modo: 'produccion',
      origenPropio: ORIGEN,
      hostsPermitidos: ['otro.aportaya.bo'],
    })
    expect(r).toEqual({ valida: true, url: 'https://otro.aportaya.bo/api/v2' })
  })
})

describe('validarConfiguracion · casos límite', () => {
  it('normaliza el puerto: origen propio con puerto', () => {
    const origen = 'https://backoffice.aportaya.bo:8443'
    expect(validarConfiguracion(`${origen}/api/v1`, { modo: 'produccion', origenPropio: origen })).toEqual({
      valida: true,
      url: `${origen}/api/v1`,
    })
  })

  it('quita la barra final antes de exigir la versión (ruta relativa)', () => {
    expect(validarConfiguracion('/api/v1/', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: true, url: '/api/v1' })
  })

  it('quita la barra final antes de exigir la versión (URL absoluta)', () => {
    expect(validarConfiguracion(`${ORIGEN}/api/v1/`, { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: true,
      url: `${ORIGEN}/api/v1`,
    })
  })

  it('normaliza mayúsculas en el host antes de comparar el origen', () => {
    expect(validarConfiguracion('https://BACKOFFICE.APORTAYA.BO/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: true,
      url: 'https://BACKOFFICE.APORTAYA.BO/api/v1',
    })
  })

  it('normaliza espacios en la lista de hosts permitidos', () => {
    const r = validarConfiguracion('https://otro.aportaya.bo/api/v1', {
      modo: 'produccion',
      origenPropio: ORIGEN,
      hostsPermitidos: ['  otro.aportaya.bo  ', ' OTRO2.aportaya.bo'],
    })
    expect(r.valida).toBe(true)
  })
})

describe('validarConfiguracion · casos inválidos, cada uno con motivo distinto', () => {
  it('vacía', () => {
    expect(validarConfiguracion('', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: false, motivo: 'vacia' })
    expect(validarConfiguracion(null, { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: false, motivo: 'vacia' })
    expect(validarConfiguracion(undefined, { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: false, motivo: 'vacia' })
    expect(validarConfiguracion('   ', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: false, motivo: 'vacia' })
  })

  it('esquema no soportado', () => {
    expect(validarConfiguracion('ftp://backoffice.aportaya.bo/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'esquema-no-soportado',
    })
  })

  it('sin TLS en producción', () => {
    expect(validarConfiguracion('http://backoffice.aportaya.bo/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'sin-tls-en-produccion',
    })
  })

  it('host ajeno', () => {
    expect(validarConfiguracion('https://evil.example.com/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'host-ajeno',
    })
  })

  it('sin la ruta versionada', () => {
    expect(validarConfiguracion(`${ORIGEN}/api`, { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'sin-ruta-versionada',
    })
    expect(validarConfiguracion('/api', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({ valida: false, motivo: 'sin-ruta-versionada' })
  })

  it('esquema de script', () => {
    expect(validarConfiguracion('javascript:alert(1)', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'esquema-no-soportado',
    })
  })

  it('la forma "//otro-host/..." nunca se acepta, ni siquiera con host permitido', () => {
    expect(
      validarConfiguracion('//evil.example.com/api/v1', { modo: 'produccion', origenPropio: ORIGEN, hostsPermitidos: ['evil.example.com'] }),
    ).toEqual({ valida: false, motivo: 'esquema-relativo-a-protocolo' })
  })

  it('localhost nunca se acepta en producción', () => {
    expect(validarConfiguracion('http://localhost:4010/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'localhost-no-permitido',
    })
  })

  it('127.0.0.1 nunca se acepta en producción', () => {
    expect(validarConfiguracion('https://127.0.0.1/api/v1', { modo: 'produccion', origenPropio: ORIGEN })).toEqual({
      valida: false,
      motivo: 'localhost-no-permitido',
    })
  })

  it('ninguna llamada lanza: siempre devuelve un resultado', () => {
    expect(() => validarConfiguracion('::::no-es-una-url', { modo: 'produccion', origenPropio: ORIGEN })).not.toThrow()
    expect(validarConfiguracion('::::no-es-una-url', { modo: 'produccion', origenPropio: ORIGEN }).valida).toBe(false)
  })
})
