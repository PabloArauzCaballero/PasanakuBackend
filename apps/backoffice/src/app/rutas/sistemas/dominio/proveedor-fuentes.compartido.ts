import { InjectionToken, type Provider } from '@angular/core'
import { adaptadorFuenteNoDisponible } from './adaptador-fuente-no-disponible'
import { puertoAccesos } from './accesos'
import { puertoIncidentes } from './incidentes'
import { puertoProveedores } from './proveedores'
import { puertoSaludDeServicios } from './salud-de-servicios'
import { puertoWebhooks } from './webhooks'
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

/** Los puertos de producción: servicios contra su contrato real; los otros ocho, con la fuente no disponible. */
export function proveedoresFuenteNoDisponible(): Provider[] {
  return [
    // La salud de servicios sí tiene contrato real (auditoria): el resto de los puertos todavía no.
    { provide: PUERTO_SERVICIOS, useFactory: puertoSaludDeServicios },
    { provide: PUERTO_DESPLIEGUES, useValue: { obtener: adaptadorFuenteNoDisponible.despliegues } },
    { provide: PUERTO_BASE_DE_DATOS, useValue: { obtener: adaptadorFuenteNoDisponible.baseDeDatos } },
    { provide: PUERTO_RESPALDOS, useValue: { obtener: adaptadorFuenteNoDisponible.respaldos } },
    // Proveedores: aportes y notificaciones publican los suyos, tarifas el costo del último mes cerrado.
    { provide: PUERTO_PROVEEDORES, useFactory: () => puertoProveedores() },
    { provide: PUERTO_OUTBOX, useValue: { obtener: adaptadorFuenteNoDisponible.outbox } },
    // Webhooks: los de las pasarelas (aportes) y los avisos de entrega de mensajería (notificaciones).
    { provide: PUERTO_WEBHOOKS, useFactory: puertoWebhooks },
    // Revisión de accesos: identidad ya publica los usuarios internos con sus roles (`listarUsuariosInternos`).
    { provide: PUERTO_ACCESOS, useFactory: puertoAccesos },
    // Incidentes: auditoria publica `incidente_operativo` (`listarIncidentesOperativos`).
    { provide: PUERTO_INCIDENTES, useFactory: puertoIncidentes },
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
