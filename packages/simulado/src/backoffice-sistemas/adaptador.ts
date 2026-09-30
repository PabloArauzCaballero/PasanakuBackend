import {
  accesosSimulados,
  descartadosSimulados,
  desplieguesSimulados,
  incidentesSimulados,
  interruptoresSimulados,
  migracionesSimuladas,
  outboxSimulado,
  proveedoresSimulados,
  respaldosSimulados,
  serviciosSimulados,
  webhooksSimulados,
} from './datos.ts'

/**
 * El adaptador simulado de las nueve pantallas de `sistemas/` (H2.S1.M3): resuelve con
 * los datos de ejemplo tras un retardo **determinista** (nunca un generador
 * pseudoaleatorio no criptográfico: un retardo aleatorio en un test es un test que a
 * veces tarda y a veces no) y nunca falla — para eso está el otro adaptador,
 * `adaptadorFuenteNoDisponible`, del lado de `apps/backoffice`.
 *
 * Solo se importa desde el backoffice por import dinámico y en una rama que el build
 * de producción no incluye (`proveedor-fuentes.demo.ts`, reemplazado por
 * `fileReplacements` — ver `angular.json`): así es como los nueve arrays de arriba
 * nunca llegan al bundle de producción.
 */
const RETARDO_MS = 30

function conRetardo<T>(valor: T): Promise<T> {
  return new Promise((resolver) => setTimeout(() => resolver(valor), RETARDO_MS))
}

export const adaptadorSimuladoSistemas = {
  servicios: () => conRetardo(serviciosSimulados),
  despliegues: () => conRetardo({ despliegues: desplieguesSimulados, interruptores: interruptoresSimulados }),
  baseDeDatos: () => conRetardo(migracionesSimuladas),
  respaldos: () => conRetardo(respaldosSimulados),
  proveedores: () => conRetardo(proveedoresSimulados),
  outbox: () => conRetardo({ mensajes: outboxSimulado, descartados: descartadosSimulados }),
  webhooks: () => conRetardo(webhooksSimulados),
  accesos: () => conRetardo(accesosSimulados),
  incidentes: () => conRetardo(incidentesSimulados),
} as const
