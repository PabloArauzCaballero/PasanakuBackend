import {
  errorFuenteNoDisponible,
  type Acceso,
  type DespliguesEInterruptores,
  type EstadoServicio,
  type Incidente,
  type Migracion,
  type OutboxYDescartados,
  type Proveedor,
  type Respaldo,
  type Webhook,
} from './puertos'

/**
 * La fuente que se usa en producción mientras no exista el contrato real (H2.S1.M4):
 * cada puerto rechaza con `ErrorFuenteNoDisponible`, nunca con datos inventados. La
 * pantalla lo pinta con `EstadoDePantalla` como el estado de error accionable que pide
 * el kill-test — "la fuente no está disponible", nunca un `99.95 %` de ejemplo.
 *
 * Nueve fábricas, una por puerto, cada una con el nombre correcto del recurso en el
 * mensaje de error — un objeto y no una sola clase porque cada puerto tiene una firma
 * de retorno distinta (`obtener(): Promise<T>` con un `T` diferente cada vez).
 */
export const adaptadorFuenteNoDisponible = {
  servicios: (): Promise<EstadoServicio[]> => Promise.reject(errorFuenteNoDisponible()),
  despliegues: (): Promise<DespliguesEInterruptores> => Promise.reject(errorFuenteNoDisponible()),
  baseDeDatos: (): Promise<Migracion[]> => Promise.reject(errorFuenteNoDisponible()),
  respaldos: (): Promise<Respaldo[]> => Promise.reject(errorFuenteNoDisponible()),
  proveedores: (): Promise<Proveedor[]> => Promise.reject(errorFuenteNoDisponible()),
  outbox: (): Promise<OutboxYDescartados> => Promise.reject(errorFuenteNoDisponible()),
  webhooks: (): Promise<Webhook[]> => Promise.reject(errorFuenteNoDisponible()),
  accesos: (): Promise<Acceso[]> => Promise.reject(errorFuenteNoDisponible()),
  incidentes: (): Promise<Incidente[]> => Promise.reject(errorFuenteNoDisponible()),
} as const
