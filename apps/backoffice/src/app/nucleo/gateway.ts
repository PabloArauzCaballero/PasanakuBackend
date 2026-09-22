import { InjectionToken } from '@angular/core'

/** Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. */
export const GATEWAY = new InjectionToken<string>('aportaya.gateway')

/** En desarrollo, Prism (`yarn dev:mock`). En producción, la que NGINX inyecte. */
export function gatewayPorDefecto(): string {
  const meta = typeof document !== 'undefined' ? document.querySelector('meta[name="aportaya-gateway"]') : null
  return meta?.getAttribute('content') ?? 'http://localhost:4010/api/v1'
}
