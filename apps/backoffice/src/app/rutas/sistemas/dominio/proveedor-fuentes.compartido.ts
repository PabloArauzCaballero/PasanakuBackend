import { InjectionToken, type Provider } from '@angular/core'
import { adaptadorFuenteNoDisponible } from './adaptador-fuente-no-disponible'
import {
  PUERTO_ACCESOS,
  PUERTO_BASE_DE_DATOS,
  PUERTO_DESPLIEGUES,
  PUERTO_INCIDENTES,
  PUERTO_OUTBOX,
  PUERTO_PROVEEDORES,
  PUERTO_RESPALDOS,
  PUERTO_SERVICIOS,
  PUERTO_WEBHOOKS,
} from './puertos'

/** El modo de DATOS de las pantallas de sistemas — ortogonal al modo de RED de `nucleo/gateway.ts`. */
export type ModoFuentesDeSistemas = 'produccion' | 'demo'

/** Si la fuente activa es la simulada: lo que decide si `banner-datos-de-ejemplo` se pinta. */
export const ES_FUENTE_SIMULADA = new InjectionToken<boolean>('aportaya.sistemas.esFuenteSimulada')

/** Los nueve puertos con la fuente no disponible — el único resultado posible en producción. */
export function proveedoresFuenteNoDisponible(): Provider[] {
  return [
    { provide: PUERTO_SERVICIOS, useValue: { obtener: adaptadorFuenteNoDisponible.servicios } },
    { provide: PUERTO_DESPLIEGUES, useValue: { obtener: adaptadorFuenteNoDisponible.despliegues } },
    { provide: PUERTO_BASE_DE_DATOS, useValue: { obtener: adaptadorFuenteNoDisponible.baseDeDatos } },
    { provide: PUERTO_RESPALDOS, useValue: { obtener: adaptadorFuenteNoDisponible.respaldos } },
    { provide: PUERTO_PROVEEDORES, useValue: { obtener: adaptadorFuenteNoDisponible.proveedores } },
    { provide: PUERTO_OUTBOX, useValue: { obtener: adaptadorFuenteNoDisponible.outbox } },
    { provide: PUERTO_WEBHOOKS, useValue: { obtener: adaptadorFuenteNoDisponible.webhooks } },
    { provide: PUERTO_ACCESOS, useValue: { obtener: adaptadorFuenteNoDisponible.accesos } },
    { provide: PUERTO_INCIDENTES, useValue: { obtener: adaptadorFuenteNoDisponible.incidentes } },
    { provide: ES_FUENTE_SIMULADA, useValue: false },
  ]
}

/**
 * El modo de datos se decide por una etiqueta explícita del HTML — **nunca** solo por
 * `isDevMode()` (Q-J3): eso dejaría la decisión de mostrar datos de ejemplo en manos
 * del compilador, no de quien despliega. El backoffice es una SPA sin servidor propio
 * (a diferencia de `apps/web`): no hay variable de entorno que leer en el navegador,
 * solo la etiqueta que NGINX inyecta (mismo mecanismo que `aportaya-gateway`).
 */
export function detectarModoFuentesDeSistemas(): ModoFuentesDeSistemas {
  const meta = typeof document !== 'undefined' ? (document.querySelector('meta[name="aportaya-demo"]')?.getAttribute('content') ?? null) : null
  return meta === 'true' ? 'demo' : 'produccion'
}
