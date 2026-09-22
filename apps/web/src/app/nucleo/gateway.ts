import { InjectionToken } from '@angular/core'

/** Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. */
export const GATEWAY = new InjectionToken<string>('aportaya.gateway')

/** En desarrollo, Prism (`yarn dev:mock`). En el servidor de SSR, la variable de entorno. */
export function gatewayPorDefecto(): string {
  const deEntorno = typeof process !== 'undefined' ? process.env?.['APORTAYA_GATEWAY'] : undefined
  const meta = typeof document !== 'undefined' ? document.querySelector('meta[name="aportaya-gateway"]') : null
  return deEntorno ?? meta?.getAttribute('content') ?? 'http://localhost:4010/api/v1'
}
