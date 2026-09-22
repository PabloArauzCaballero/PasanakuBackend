import { describe, expect, it } from 'vitest'
import { ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import { carpetaDelExpediente, colaVacia, esperaDecision, POR_DECIDIR, recortar } from './cu02-expedientes'

const USUARIO = '9f2c1e4a-0000-4000-8000-000000000001'

const expediente = (estado: ExpedienteEnRevisionEstadoEnum, verificacionId: string): ExpedienteEnRevision =>
  ({
    verificacionId,
    usuarioId: USUARIO,
    nombreCompleto: 'Marcelo Rojas',
    documento: 'CI 4821993 SC',
    estado,
    iniciadaEn: '2026-09-17T10:00:00-04:00',
    fotos: [],
  }) as unknown as ExpedienteEnRevision

const EN_REVISION = expediente(ExpedienteEnRevisionEstadoEnum.EnRevision, 'v-1')
const PENDIENTE = expediente(ExpedienteEnRevisionEstadoEnum.Pendiente, 'v-2')
const APROBADA = expediente(ExpedienteEnRevisionEstadoEnum.Aprobada, 'v-3')
const RECHAZADA = expediente(ExpedienteEnRevisionEstadoEnum.Rechazada, 'v-4')

describe('la cola por decidir', () => {
  it('espera a una persona todo lo que todavía no se resolvió, en los DOS estados', () => {
    // El alta abre el expediente en EN_REVISION, no en PENDIENTE: una cola que solo
    // mira PENDIENTE decía «no hay nada» con gente esperando su cuenta.
    expect(esperaDecision(EN_REVISION)).toBe(true)
    expect(esperaDecision(PENDIENTE)).toBe(true)
    expect(esperaDecision(APROBADA)).toBe(false)
    expect(esperaDecision(RECHAZADA)).toBe(false)
  })

  it('recorta los resueltos con POR_DECIDIR y no toca nada con un estado del contrato', () => {
    const todos = [EN_REVISION, PENDIENTE, APROBADA, RECHAZADA]
    expect(recortar(todos, POR_DECIDIR).map((e) => e.verificacionId)).toEqual(['v-1', 'v-2'])
    // Con un estado del contrato ya filtró el backend: recortar de nuevo sería filtrar dos veces.
    expect(recortar(todos, ExpedienteEnRevisionEstadoEnum.Aprobada)).toHaveLength(4)
  })

  it('una cola sin nada por decidir está vacía, y eso es una buena noticia', () => {
    expect(colaVacia(recortar([APROBADA, RECHAZADA], POR_DECIDIR))).toBe(true)
    expect(colaVacia(recortar([EN_REVISION], POR_DECIDIR))).toBe(false)
  })
})

describe('la carpeta del expediente', () => {
  it('es una por persona, y las tres fotos de alguien viven ahí', () => {
    expect(carpetaDelExpediente(USUARIO)).toBe(`identidad/${USUARIO}/`)
  })
})
