import { HttpClient, httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type {
  CatalogoOperativoDetalle,
  CatalogoOperativoResumen,
  ElementoCatalogoOperativo,
  EntradaActualizarElementoCatalogo,
} from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'

export type {
  CatalogoOperativoDetalle,
  CatalogoOperativoResumen,
  ElementoCatalogoOperativo,
  EntradaActualizarElementoCatalogo,
}

export function catalogosOperativos() {
  const gateway = inject(GATEWAY)
  return httpResource<CatalogoOperativoResumen[]>(() => `${gateway}/auditoria/catalogos`)
}

export function catalogoOperativo(codigo: Signal<string | null>) {
  const gateway = inject(GATEWAY)
  return httpResource<CatalogoOperativoDetalle>(() => {
    const cod = codigo()
    return cod ? `${gateway}/auditoria/catalogos/${cod}` : undefined
  })
}

export function elementosCatalogo(codigo: Signal<string | null>, activo: Signal<boolean | null>) {
  const gateway = inject(GATEWAY)
  return httpResource<ElementoCatalogoOperativo[]>(() => {
    const cod = codigo()
    if (!cod) return undefined
    const act = activo()
    const params = new URLSearchParams()
    if (act !== null) params.set('activo', String(act))
    const q = params.toString()
    const base = `${gateway}/auditoria/catalogos/${cod}/elementos`
    return q ? `${base}?${q}` : base
  })
}

export function actualizadorElementoCatalogo() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (codigo: string, id: string, entrada: EntradaActualizarElementoCatalogo) =>
    http.patch<ElementoCatalogoOperativo>(
      `${gateway}/auditoria/catalogos/${codigo}/elementos/${id}`,
      entrada,
    )
}
