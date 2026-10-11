import { Injectable } from '@angular/core'

/** Lo que el contrato exige al secreto (`CanjeInvitacion.token`): 64 caracteres hexadecimales. */
const FORMATO = /^[0-9a-f]{64}$/

export type LecturaDeEnlace =
  /** El enlace no trae secreto. */
  | { estado: 'sin-token' }
  /** Trae algo en `#t=` o `?token=` que no tiene la forma de un secreto. */
  | { estado: 'invalido'; enConsulta: boolean }
  | { estado: 'listo'; token: string; enConsulta: boolean }

/**
 * Lee el secreto de un enlace de invitación.
 *
 * El enlace correcto lleva el secreto en el **fragmento** (`#t=…`): el navegador no lo envía al
 * servidor, así que no queda en registros de acceso ni en proxies. Un secreto en la consulta
 * (`?token=…`) sí viaja al servidor; se acepta para no dejar a nadie afuera, pero se informa
 * (`enConsulta`) y se retira igual de la dirección.
 */
export function leerEnlace(hash: string, search: string): LecturaDeEnlace {
  const deFragmento = new URLSearchParams(hash.replace(/^#/, '')).get('t')
  const deConsulta = new URLSearchParams(search).get('token') ?? new URLSearchParams(search).get('t')
  const crudo = deFragmento ?? deConsulta
  if (crudo === null) return { estado: 'sin-token' }
  const enConsulta = deFragmento === null
  const token = crudo.trim().toLowerCase()
  return FORMATO.test(token) ? { estado: 'listo', token, enConsulta } : { estado: 'invalido', enConsulta }
}

/**
 * Deja en la barra de direcciones y en el historial SOLO la ruta: sin fragmento y sin consulta.
 * `replaceState` reemplaza la entrada actual (no agrega otra), así «atrás» no devuelve el secreto.
 */
export function retirarSecretoDeLaUrl(ventana: Pick<Window, 'history' | 'location'>): void {
  ventana.history.replaceState(ventana.history.state, '', ventana.location.pathname)
}

/**
 * El secreto vive SOLO en memoria, el tiempo que dura la pestaña, y se entrega una vez.
 * Nunca en `localStorage`/`sessionStorage`/cookies, nunca en un signal que se pinte, nunca en un log.
 */
@Injectable({ providedIn: 'root' })
export class SecretoDeInvitacionEnMemoria {
  #token: string | null = null

  guardar(token: string): void {
    this.#token = token
  }

  /** Entrega el secreto y lo olvida: un canje lo consume una sola vez. */
  tomar(): string | null {
    const t = this.#token
    this.#token = null
    return t
  }

  hay(): boolean {
    return this.#token !== null
  }
}
