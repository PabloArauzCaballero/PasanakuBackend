import { InjectionToken, isDevMode, type Provider } from '@angular/core'
import { validarConfiguracion, type ModoGateway, type ResultadoValidacionGateway } from '@aportaya/dominio-cliente/configuracion'

/** Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. */
export const GATEWAY = new InjectionToken<string>('aportaya.gateway')

/** El resultado completo de validar la configuración (ver `nucleo/gateway.ts` del backoffice, mismo patrón). */
export const CONFIGURACION_GATEWAY = new InjectionToken<ResultadoValidacionGateway>('aportaya.configuracionGateway')

function leerMetaGateway(): string | null {
  return typeof document !== 'undefined' ? (document.querySelector('meta[name="aportaya-gateway"]')?.getAttribute('content') ?? null) : null
}

function leerEnvGateway(): string | null {
  return typeof process !== 'undefined' ? (process.env?.['APORTAYA_GATEWAY'] ?? null) : null
}

/**
 * El origen propio: en el navegador, `location.origin`. En el servidor de SSR no hay
 * `location` — se usa la URL pública del sitio (`APORTAYA_URL_APP`, la misma variable
 * que ya inyecta `server.ts` en la etiqueta `aportaya-app`; ver ese archivo, que no es
 * de este carril). Sin esa variable, no hay "mismo origen" posible del lado servidor:
 * solo pasará una URL que esté en `hostsPermitidos` (p. ej. el host interno del
 * gateway en la red del contenedor, que infra permite explícitamente).
 */
function origenPropioDelEntorno(): string {
  if (typeof document !== 'undefined' && typeof location !== 'undefined') return location.origin
  return typeof process !== 'undefined' ? (process.env?.['APORTAYA_URL_APP'] ?? '') : ''
}

/**
 * Resuelve y valida el gateway para un `modo` explícito, en el navegador o en el
 * servidor de SSR (isomorfo). **Nunca** devuelve `localhost` por omisión.
 *
 * Orden de lectura, igual en las dos plataformas (invariante de H3.S2.M2): la
 * variable de entorno primero (solo existe en el proceso de Node del servidor de
 * render; en un navegador real `process` no está definido), y la etiqueta del HTML
 * como respaldo. Con las dos ausentes, ambas plataformas llegan al mismo resultado:
 * inválida — nunca un valor por omisión que apunte a algún host.
 */
export function resolverGateway(modo: ModoGateway, opciones: { origenPropio?: string; hostsPermitidos?: readonly string[] } = {}): ResultadoValidacionGateway {
  const cruda = leerEnvGateway() ?? leerMetaGateway()
  return validarConfiguracion(cruda, {
    modo,
    origenPropio: opciones.origenPropio ?? origenPropioDelEntorno(),
    hostsPermitidos: opciones.hostsPermitidos ?? [],
  })
}

/**
 * En desarrollo, Prism. En el servidor de SSR, la variable de entorno; en el
 * navegador, la etiqueta. Si falta o es inválida: **vacío**, nunca
 * `http://localhost:4010` — el hallazgo original de este archivo. Firma sin
 * argumentos por compatibilidad con `app.config.ts` (de Richard); el modo explícito
 * y probado vive en `provideGateway(modo)`.
 */
export function gatewayPorDefecto(): string {
  const modo: ModoGateway = isDevMode() ? 'desarrollo' : 'produccion'
  const resultado = resolverGateway(modo)
  return resultado.valida ? resultado.url : ''
}

/**
 * El par de providers de H3.S2.M2/M4: mismo resultado en plataforma servidor y
 * navegador para la misma configuración de entorno. Probado con `TestBed`, no
 * cableado en `app.config.ts` — ver `entregables/cableado-app-config.md`.
 */
export function provideGateway(modo: ModoGateway, opciones: { origenPropio?: string; hostsPermitidos?: readonly string[] } = {}): Provider[] {
  const resultado = resolverGateway(modo, opciones)
  return [
    { provide: GATEWAY, useValue: resultado.valida ? resultado.url : '' },
    { provide: CONFIGURACION_GATEWAY, useValue: resultado },
  ]
}
