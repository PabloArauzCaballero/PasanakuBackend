import { httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type { EventoBitacora, PaginaDeBitacora } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'

export type { EventoBitacora, PaginaDeBitacora }

export function bitacora(filtros: {
  entidad: Signal<string>
  accion: Signal<string>
  antesDe?: Signal<string | null>
  antesDeId?: Signal<string | null>
  limite?: Signal<number>
}) {
  const gateway = inject(GATEWAY)
  return httpResource<PaginaDeBitacora>(() => {
    const params = new URLSearchParams()
    const ent = filtros.entidad().trim()
    const acc = filtros.accion().trim()
    if (ent && ent !== 'TODAS') params.set('entidad', ent)
    if (acc && acc !== 'TODAS') params.set('accion', acc)
    if (filtros.limite) params.set('limite', String(filtros.limite()))
    const ad = filtros.antesDe?.()
    const adId = filtros.antesDeId?.()
    if (ad) params.set('antesDe', ad)
    if (adId) params.set('antesDeId', adId)
    const q = params.toString()
    const base = `${gateway}/auditoria/bitacora`
    return q ? `${base}?${q}` : base
  })
}

export function eventoBitacora(id: Signal<string | null>) {
  const gateway = inject(GATEWAY)
  return httpResource<EventoBitacora>(() => {
    const idVal = id()
    return idVal ? `${gateway}/auditoria/bitacora/${idVal}` : undefined
  })
}
