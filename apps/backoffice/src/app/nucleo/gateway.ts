import { InjectionToken, isDevMode, type Provider } from '@angular/core'
import { validarConfiguracion, type ModoGateway, type ResultadoValidacionGateway } from '@aportaya/dominio-cliente/configuracion'

/** Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. */
export const GATEWAY = new InjectionToken<string>('aportaya.gateway')

/**
 * El resultado completo de validar la configuración — no solo la URL, sino también si
 * es válida. `ConfiguracionInvalida` (y quien la cablee, ver `entregables/cableado-
 * app-config.md`) lo inyecta para decidir si arranca el shell o el aviso de bloqueo.
 */
export const CONFIGURACION_GATEWAY = new InjectionToken<ResultadoValidacionGateway>('aportaya.configuracionGateway')

function leerMetaGateway(): string | null {
  if (typeof document === 'undefined') return null
  return document.querySelector('meta[name="aportaya-gateway"]')?.getAttribute('content') ?? null
}

function origenDelDocumento(): string {
  return typeof location !== 'undefined' ? location.origin : ''
}

/**
 * Resuelve y valida el gateway para un `modo` explícito. **Nunca** devuelve `localhost`
 * como valor por omisión: si la configuración es inválida, `valida` es `false` y quien
 * llama decide (mostrar `ConfiguracionInvalida`, no hacer ninguna petición).
 */
export function resolverGateway(modo: ModoGateway, opciones: { origenPropio?: string; hostsPermitidos?: readonly string[] } = {}): ResultadoValidacionGateway {
  const cruda = leerMetaGateway()
  return validarConfiguracion(cruda, {
    modo,
    origenPropio: opciones.origenPropio ?? origenDelDocumento(),
    hostsPermitidos: opciones.hostsPermitidos ?? [],
  })
}

/**
 * En desarrollo, Prism (`yarn dev:mock`). En producción, la que NGINX inyecte — y si
 * falta o es inválida, **vacío**, nunca `http://localhost:4010`: eso, en el navegador
 * de un operador, es su propia máquina (el hallazgo original de este archivo).
 *
 * Firma sin argumentos por compatibilidad: `app.config.ts` (de Richard) sigue
 * llamándola igual que antes; el modo se autodetecta con `isDevMode()` **solo para
 * esta función de compatibilidad** — la decisión explícita y probada vive en
 * `provideGateway(modo)`, más abajo, para quien prefiera pasar el modo a mano.
 */
export function gatewayPorDefecto(): string {
  const modo: ModoGateway = isDevMode() ? 'desarrollo' : 'produccion'
  const resultado = resolverGateway(modo)
  return resultado.valida ? resultado.url : ''
}

/**
 * El par de providers que entrega este carril (H3.S2.M4): probado con `TestBed`, pero
 * **no** cableado en `app.config.ts` — eso es de Richard. Ver la línea exacta en
 * `entregables/cableado-app-config.md`.
 *
 * Da tanto `GATEWAY` (la URL, o `''` si es inválida — nunca localhost) como
 * `CONFIGURACION_GATEWAY` (el resultado completo) para que el shell pueda mostrar
 * `ConfiguracionInvalida` en vez de montar rutas que llamarían a un gateway inválido.
 */
export function provideGateway(modo: ModoGateway, opciones: { origenPropio?: string; hostsPermitidos?: readonly string[] } = {}): Provider[] {
  const resultado = resolverGateway(modo, opciones)
  return [
    { provide: GATEWAY, useValue: resultado.valida ? resultado.url : '' },
    { provide: CONFIGURACION_GATEWAY, useValue: resultado },
  ]
}
