import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { alcanza, PERMISOS_POR_SECCION } from './secciones'
import { Sesion } from './sesion'

/** Un JWT sin firma válida: el backoffice solo lee los claims para mostrar u ocultar. */
function tokenCon(claims: Record<string, unknown>): string {
  const b64 = (o: unknown) => btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${b64({ alg: 'RS256' })}.${b64(claims)}.firma`
}

describe('alcanza · de permiso real a sección', () => {
  it('sin sesión no abre nada, ni el tablero', () => {
    expect(alcanza('ver:tablero', ['VERIFICACION_RESOLVER'], false)).toBe(false)
  })

  it('el tablero lo abre cualquier sesión', () => {
    expect(alcanza('ver:tablero', [], true)).toBe(true)
  })

  it('un permiso real de la sección la abre', () => {
    expect(alcanza('ver:cumplimiento', ['VERIFICACION_RESOLVER'], true)).toBe(true)
    expect(alcanza('ver:contabilidad', ['CONTABILIDAD_ERP_REPORTES'], true)).toBe(true)
  })

  it('un permiso de OTRA sección no la abre', () => {
    expect(alcanza('ver:contabilidad', ['VERIFICACION_RESOLVER'], true)).toBe(false)
  })

  it('una sección nueva sin traducir queda cerrada, nunca abierta por omisión', () => {
    expect(alcanza('ver:seccion-nueva', ['ACCESOS_ADMINISTRAR'], true)).toBe(false)
  })

  it('el administrador de plataforma del catálogo ve tablero, cumplimiento y sistemas, y nada de dinero', () => {
    const admin = ['CATALOGO_EDITAR', 'ACCESOS_ADMINISTRAR', 'TARIFARIO_PUBLICAR', 'AUDITORIA_LEER', 'SEGURIDAD_ACCESO_RESTABLECER', 'SEGURIDAD_FACTOR_REINSCRIBIR', 'VERIFICACION_RESOLVER']
    const abiertas = ['ver:tablero', ...Object.keys(PERMISOS_POR_SECCION)].filter((s) => alcanza(s, admin, true))
    expect(abiertas).toEqual(['ver:tablero', 'ver:cumplimiento', 'ver:sistemas'])
  })
})

describe('Sesion.abrirConToken', () => {
  it('lee permisos y rol de los claims y abre la sesión', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const sesion = TestBed.inject(Sesion)
    sesion.abrirConToken(tokenCon({ permisos: ['VERIFICACION_RESOLVER'], rol: 'BACKOFFICE' }))
    expect(sesion.abierta()).toBe(true)
    expect(sesion.rol()).toBe('BACKOFFICE')
    expect(sesion.puede('ver:cumplimiento')).toBe(true)
    expect(sesion.puede('ver:publicidad')).toBe(false)
  })

  it('un token ilegible abre la sesión sin permisos, no rompe la pantalla', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const sesion = TestBed.inject(Sesion)
    sesion.abrirConToken('no-es-un-jwt')
    expect(sesion.permisos()).toEqual([])
    expect(sesion.puede('ver:cumplimiento')).toBe(false)
  })
})
