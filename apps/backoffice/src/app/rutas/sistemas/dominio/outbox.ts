import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { SalidaOutboxPorServicio } from 'clientes/angular/auditoria'
import type { Tono } from '@aportaya/ui/tono/tono'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * Sistemas › Outbox por servicio: lo que el relevo de cada servicio publica en Prometheus
 * (`GET /indicadores/operacion/outbox`, `consultarOutboxPorServicio`). auditoria no lee el esquema de otro servicio
 * (regla 98.1), así que no hay lista de mensajes: hay cuántos esperan, cuántos fallaron y cuántos salieron.
 */
export type FilaDeOutbox = {
  id: string
  servicio: string
  pendientes: string
  fallidos: string
  esperaMasLarga: string
  publicados24h: string
  errores24h: string
  estado: string
  tono: Tono
  pendientesN: number
  fallidosN: number
}

const SIN_DATO = '—'
const n = (v: number | undefined) => (v === undefined ? SIN_DATO : v.toLocaleString('es-BO'))

/** «42 s», «3 min», «2 h 5 min»: la espera del evento más viejo, legible. */
export function espera(segundos: number | undefined): string {
  if (segundos === undefined) return SIN_DATO
  if (segundos < 60) return `${segundos} s`
  const min = Math.floor(segundos / 60)
  if (min < 60) return `${min} min`
  const h = Math.floor(min / 60)
  return min % 60 ? `${h} h ${min % 60} min` : `${h} h`
}

/** Un evento que espera más de cinco minutos ya no es ritmo normal del relevo (su backoff tope es PT5M). */
export const ESPERA_ANORMAL_S = 300

export function filasDeOutbox(salida: SalidaOutboxPorServicio): FilaDeOutbox[] {
  return salida.servicios.map((s) => {
    const conFallidos = (s.fallidos ?? 0) > 0
    const atrasado = (s.edadMasViejoSegundos ?? 0) > ESPERA_ANORMAL_S
    const sinDato = s.pendientes === undefined && s.fallidos === undefined
    const [estado, tono]: [string, Tono] = conFallidos ? ['Con fallidos', 'error'] : atrasado ? ['Atrasado', 'aviso'] : sinDato ? ['Sin dato', 'neutro'] : ['Al día', 'ok']
    return {
      id: s.servicio,
      servicio: s.servicio,
      pendientes: n(s.pendientes),
      fallidos: n(s.fallidos),
      esperaMasLarga: espera(s.edadMasViejoSegundos),
      publicados24h: n(s.publicados24h),
      errores24h: n(s.erroresDePublicacion24h),
      estado,
      tono,
      pendientesN: s.pendientes ?? 0,
      fallidosN: s.fallidos ?? 0,
    }
  })
}

export function recursoOutbox() {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaOutboxPorServicio>(() => `${gateway}/indicadores/operacion/outbox`)
}
