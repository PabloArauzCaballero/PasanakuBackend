import { HttpClient, HttpContext } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { EntradaAutenticacion, SalidaAutenticacion } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { YA_REINTENTADA } from '../../../nucleo/sesion.interceptor'

/**
 * CU-04 · Autenticar con MFA. Contrato real: `POST /sesiones` (`autenticar`).
 *
 * Sin clave de idempotencia **a propósito**, como dice el contrato: cada intento es un
 * hecho distinto y queda en `intento_autenticacion`, incluidos los fallidos.
 *
 * Se llama en un campo de la clase (contexto de inyección) y devuelve la función que de
 * verdad dispara el POST; la red no vive en la vista.
 *
 * Va marcada `YA_REINTENTADA`: el `401` del ingreso es «credenciales que no coinciden», no un token vencido. Sin la
 * marca, el interceptor de sesión intentaba un refresco con la cookie de una sesión anterior en medio del login.
 */
export function crearAutenticar(): (entrada: EntradaAutenticacion) => Observable<SalidaAutenticacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaAutenticacion>(`${gateway}/sesiones`, entrada, { context: new HttpContext().set(YA_REINTENTADA, true) })
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
