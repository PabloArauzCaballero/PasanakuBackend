import { describe, expect, it } from 'vitest'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { documentoEnPalabras, estadoEnPalabras, fotosEnNumeros, impedimentosParaAprobar, tiempoEnCola, vencimientoEnPalabras } from './cu02-presentacion'

const base = {
  verificacionId: 'v-1',
  usuarioId: 'u-1',
  nombreCompleto: 'Persona Sintética',
  telefono: '+59170000001',
  estado: 'EN_REVISION',
  iniciadaEn: '2026-10-20T10:00:00Z',
  fotos: ['ANVERSO', 'REVERSO', 'SELFIE', 'PERFIL_IZQUIERDO', 'PERFIL_DERECHO'],
  fechaExpiracionDocumento: '2030-01-01',
  documentoVigente: true,
} as ExpedienteEnRevision

describe('cu02-presentacion', () => {
  it('correcto: el código del contrato se dice en palabras', () => {
    expect(estadoEnPalabras('EN_REVISION')).toBe('En revisión')
    expect(documentoEnPalabras('CI SC')).toEqual({ tipo: 'Cédula de identidad', lugar: 'Santa Cruz', texto: 'Cédula de identidad · Santa Cruz' })
    expect(documentoEnPalabras('PASAPORTE').texto).toBe('Pasaporte')
    expect(fotosEnNumeros(base)).toBe('5 de 5')
    expect(vencimientoEnPalabras(base)).toBe('Vigente')
    expect(impedimentosParaAprobar(base)).toEqual([])
  })

  it('límite: sin documento se dice; un código desconocido se muestra tal cual en vez de inventarle un nombre', () => {
    expect(documentoEnPalabras(undefined).texto).toBe('Sin documento cargado')
    expect(documentoEnPalabras('  ').texto).toBe('Sin documento cargado')
    expect(documentoEnPalabras('DNI XX').texto).toBe('DNI · XX')
    expect(estadoEnPalabras('OTRO')).toBe('OTRO')
  })

  it('inválido: lo que impide aprobar se enumera entero, en el orden del servidor', () => {
    const e = { ...base, fotos: ['ANVERSO'], fechaExpiracionDocumento: '2020-01-01', documentoVigente: false } as ExpedienteEnRevision
    expect(fotosEnNumeros(e)).toBe('1 de 5')
    expect(impedimentosParaAprobar(e)).toEqual([
      'Faltan 4 fotos: Reverso del documento, Selfie de frente, Perfil izquierdo, Perfil derecho.',
      'El documento está vencido según el servidor.',
    ])
    expect(impedimentosParaAprobar({ ...base, fechaExpiracionDocumento: undefined, documentoVigente: false })).toEqual(['El documento no tiene fecha de vencimiento: no hay forma de comprobar que esté vigente.'])
  })

  it('tiempo en cola: abierto cuenta hasta ahora, resuelto hasta su resolución, y una fecha imposible no inventa nada', () => {
    const ahora = new Date('2026-10-23T10:00:00Z')
    expect(tiempoEnCola(base, ahora)).toBe('3 días')
    expect(tiempoEnCola({ ...base, resueltaEn: '2026-10-20T15:00:00Z' }, ahora)).toBe('5 horas')
    expect(tiempoEnCola({ ...base, resueltaEn: '2026-10-20T10:30:00Z' }, ahora)).toBe('30 minutos')
    expect(tiempoEnCola({ ...base, iniciadaEn: 'basura' }, ahora)).toBe('Sin dato')
  })
})
