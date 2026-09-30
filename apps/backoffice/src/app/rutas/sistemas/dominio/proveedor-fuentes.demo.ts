import type { Provider } from '@angular/core'
import { ES_FUENTE_SIMULADA, proveedoresFuenteNoDisponible, type ModoFuentesDeSistemas } from './proveedor-fuentes.compartido'
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

export { ES_FUENTE_SIMULADA, detectarModoFuentesDeSistemas, type ModoFuentesDeSistemas } from './proveedor-fuentes.compartido'

type ModuloSimulado = typeof import('@aportaya/simulado/backoffice-sistemas')

/** Envuelve cada `obtener()` en un import dinámico: el paquete de simulado se carga recién cuando una pantalla lo pide. */
function conFuenteSimulada<T>(leer: (modulo: ModuloSimulado) => Promise<T>): { obtener: () => Promise<T> } {
  return { obtener: async () => leer(await import('@aportaya/simulado/backoffice-sistemas')) }
}

/**
 * H2.S2.M2 — la versión CON datos de ejemplo. Reemplaza a `proveedor-fuentes.ts` por
 * `fileReplacements` (`angular.json`) SOLO en las configuraciones `development` y
 * `demo` — nunca en `production` (ver ese archivo).
 *
 * Con `modo !== 'demo'` se comporta exactamente igual que la versión segura: la
 * bandera de datos, no el `fileReplacements`, es lo que en desarrollo decide si se ve
 * el simulado o el error — así un desarrollador puede probar el estado de error sin
 * cambiar de build.
 */
export function provideFuentesDeSistemas(modo: ModoFuentesDeSistemas): Provider[] {
  if (modo !== 'demo') return proveedoresFuenteNoDisponible()
  return [
    { provide: PUERTO_SERVICIOS, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.servicios()) },
    { provide: PUERTO_DESPLIEGUES, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.despliegues()) },
    { provide: PUERTO_BASE_DE_DATOS, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.baseDeDatos()) },
    { provide: PUERTO_RESPALDOS, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.respaldos()) },
    { provide: PUERTO_PROVEEDORES, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.proveedores()) },
    { provide: PUERTO_OUTBOX, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.outbox()) },
    { provide: PUERTO_WEBHOOKS, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.webhooks()) },
    { provide: PUERTO_ACCESOS, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.accesos()) },
    { provide: PUERTO_INCIDENTES, useValue: conFuenteSimulada((m) => m.adaptadorSimuladoSistemas.incidentes()) },
    { provide: ES_FUENTE_SIMULADA, useValue: true },
  ]
}
