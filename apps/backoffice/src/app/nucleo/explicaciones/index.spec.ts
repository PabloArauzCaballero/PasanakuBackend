import { describe, expect, it } from 'vitest'
import { coincidePrefijo, resolverExplicacion } from './index'

describe('Explicaciones de vistas y módulos', () => {
  it('coincidePrefijo reconoce rutas fijas y con parámetros dinámicos', () => {
    expect(coincidePrefijo('/operacion/retiros', '/operacion/retiros')).toBe(true)
    expect(coincidePrefijo('/operacion/retiros/ord-999', '/operacion/retiros/[ordenId]')).toBe(true)
    expect(coincidePrefijo('/tablero/catalogo-de-datos/usuarios', '/tablero/catalogo-de-datos/[tabla]')).toBe(true)
    expect(coincidePrefijo('/tablero/catalogo-de-datos/usuarios/ficha', '/tablero/catalogo-de-datos/[tabla]/ficha')).toBe(true)
    expect(coincidePrefijo('/cumplimiento/casos/caso-abc', '/cumplimiento/casos/[casoId]')).toBe(true)
    expect(coincidePrefijo('/sistemas/usuarios-internos/usr-1', '/sistemas/usuarios-internos/[id]')).toBe(true)
  })

  it('resolverExplicacion encuentra el módulo y la vista correcta para Inicio / Tablero', () => {
    const res = resolverExplicacion('/tablero')
    expect(res).not.toBeNull()
    expect(res?.modulo.modulo).toBe('Inicio y Tableros')
    expect(res?.modulo.sistemas).toContain('PostgreSQL')
    expect(res?.modulo.negocio).toContain('cabina de control')
    expect(res?.vista?.negocio).toContain('bienvenida y pulso del negocio')
  })

  it('resolverExplicacion encuentra la vista de conciliación de custodia', () => {
    const res = resolverExplicacion('/operacion/custodia')
    expect(res).not.toBeNull()
    expect(res?.modulo.modulo).toBe('Operación')
    expect(res?.vista?.negocio).toContain('prueba máxima de honestidad')
    expect(res?.vista?.sistemas).toContain('CU-50')
  })

  it('resolverExplicacion encuentra rutas dinámicas como retiros por ID', () => {
    const res = resolverExplicacion('/operacion/retiros/ret-12345')
    expect(res).not.toBeNull()
    expect(res?.modulo.modulo).toBe('Operación')
    expect(res?.vista?.negocio).toContain('cuatro ojos')
    expect(res?.vista?.sistemas).toContain('segunda firma')
  })

  it('resolverExplicacion resuelve vistas de cumplimiento normativo (KYC y reportes UIF)', () => {
    const kyc = resolverExplicacion('/cumplimiento/verificaciones')
    expect(kyc?.modulo.modulo).toBe('Cumplimiento')
    expect(kyc?.vista?.negocio).toContain('Verificación de que cada persona sea realmente quien dice ser')
    expect(kyc?.vista?.sistemas).toContain('CU-02')

    const uif = resolverExplicacion('/cumplimiento/uif/reportes')
    expect(uif?.vista?.sistemas).toContain('PCC-01')
    expect(uif?.vista?.negocio).toContain('Unidad de Investigaciones Financieras')
  })

  it('resolverExplicacion resuelve vistas de sistemas (operaciones, flujos, base de datos)', () => {
    const ops = resolverExplicacion('/sistemas/operaciones')
    expect(ops?.modulo.modulo).toBe('Sistemas')
    expect(ops?.vista?.negocio).toContain('inventario oficial')
    expect(ops?.vista?.sistemas).toContain('endpoints')

    const flujos = resolverExplicacion('/sistemas/flujos')
    expect(flujos?.vista?.negocio).toContain('mapa visual')
  })

  it('resolverExplicacion resuelve contabilidad y publicidad', () => {
    const erp = resolverExplicacion('/contabilidad/compras')
    expect(erp?.modulo.modulo).toBe('Contabilidad ERP')
    expect(erp?.vista?.negocio).toContain('proveedores')

    const pub = resolverExplicacion('/publicidad/campanas')
    expect(pub?.modulo.modulo).toBe('Publicidad y Alianzas')
    expect(pub?.vista?.negocio).toContain('promociones y beneficios')
  })
})
