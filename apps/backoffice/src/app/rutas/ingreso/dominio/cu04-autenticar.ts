import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { EntradaAutenticacion, SalidaAutenticacion } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * CU-04 · Autenticar con MFA. Contrato real: `POST /sesiones` (`autenticar`).
 *
 * Sin clave de idempotencia **a propósito**, como dice el contrato: cada intento es un
 * hecho distinto y queda en `intento_autenticacion`, incluidos los fallidos.
 *
 * Se llama en un campo de la clase (contexto de inyección) y devuelve la función que de
 * verdad dispara el POST; la red no vive en la vista.
 */
export function crearAutenticar(): (entrada: EntradaAutenticacion) => Observable<SalidaAutenticacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaAutenticacion>(`${gateway}/sesiones`, entrada)
}

/**
 * La huella del navegador para `huellaDispositivo`. Aleatoria por pestaña: el backoffice
 * no persiste nada del operador en el navegador (ni `localStorage` ni cookies propias).
 *
 * `crypto.getRandomValues` y no `crypto.randomUUID`: el segundo solo existe en contexto
 * seguro, y un entorno servido por HTTP plano no lo es — la pantalla moría al cargar.
 */
export function huellaDeEstaPestana(): string {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return 'web-' + Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}
