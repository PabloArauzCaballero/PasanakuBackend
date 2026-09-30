import { InjectionToken } from '@angular/core'

/**
 * Un puerto por pantalla de `sistemas/` (H2.S1.M1 / H4.S1.M1). Cada pantalla inyecta
 * su puerto — nunca importa datos directamente — y lo resuelve con `resource()`. Quién
 * implementa el puerto lo decide `proveedor-fuentes.ts`: la fuente simulada
 * (`@aportaya/simulado/backoffice-sistemas`, solo en desarrollo o demo) o la fuente no
 * disponible (`adaptador-fuente-no-disponible.ts`, la que se usa en producción
 * mientras no exista el contrato real — ver el hueco declarado más abajo).
 *
 * HUECO DECLARADO (heredado de la antigua `datos-simulados.ts`): no existe un contrato
 * OpenAPI real de "indicadores"/"tablero" para el backoffice de sistemas — buscado en
 * `servicios/erp/src/main/resources/openapi/erp.yaml` y en `docs/CasosDeUso/CU-98*.md`,
 * ninguno expone esto como API. Ver `entregables/brecha-observabilidad.md` para la
 * propuesta, marcada como propuesta y no como contrato (Q-J2).
 */

// ---- Tipos de dominio (la forma que hoy espera cada pantalla) ----------------------

export type EstadoServicio = {
  id: string
  nombre: string
  estado: 'operativo' | 'degradado' | 'caido'
  disponibilidad30d: string
  presupuestoErrorRestante: string
  ultimaInterrupcion: string
}

export type Despliegue = {
  id: string
  servicio: string
  version: string
  desplegadoEl: string
  desplegadoPor: string
  estado: 'exitoso' | 'en curso' | 'revertido'
}

export type Interruptor = { id: string; nombre: string; tocaDinero: boolean; activo: boolean }

export type DespliguesEInterruptores = { despliegues: Despliegue[]; interruptores: Interruptor[] }

export type Migracion = { id: string; nombre: string; aplicadaEl: string | null; estado: 'aplicada' | 'pendiente' }

export type Respaldo = { id: string; origen: string; tomadoEl: string; ultimaRestauracionProbadaEl: string | null }

export type Proveedor = { id: string; nombre: string; categoria: string; costoRealUltimoPeriodo: string; moneda: string }

export type MensajeOutbox = { id: string; tipo: string; creadoEl: string; estado: 'pendiente' | 'entregado' }
export type MensajeDescartado = { id: string; tipo: string; motivo: string; descartadoEl: string }
export type OutboxYDescartados = { mensajes: MensajeOutbox[]; descartados: MensajeDescartado[] }

export type Webhook = { id: string; direccion: 'entrante' | 'saliente'; origen: string; ultimoEstado: 'ok' | 'error'; ultimaEntregaEl: string }

export type Acceso = { id: string; persona: string; rol: string; ambito: string; otorgadoEl: string }

export type Incidente = { id: string; titulo: string; severidad: 'sev1' | 'sev2' | 'sev3'; estado: 'abierto' | 'cerrado'; abiertoEl: string }

// ---- El error de dominio que emite la fuente no disponible -------------------------

/** Lo que rechaza cualquier puerto cuando la fuente real no está disponible: tipado, nunca un número inventado. */
export type ErrorFuenteNoDisponible = {
  readonly tipo: 'fuente-no-disponible'
  readonly mensaje: string
  readonly trazaId?: string
}

export function errorFuenteNoDisponible(nombreDeRecurso: string): ErrorFuenteNoDisponible {
  return {
    tipo: 'fuente-no-disponible',
    mensaje: `No pudimos traer «${nombreDeRecurso}»: todavía no existe un origen de datos real para esta pantalla.`,
  }
}

// ---- Los nueve puertos ---------------------------------------------------------------

export interface PuertoServicios {
  obtener(): Promise<EstadoServicio[]>
}
export const PUERTO_SERVICIOS = new InjectionToken<PuertoServicios>('aportaya.sistemas.puertoServicios')

export interface PuertoDespliegues {
  obtener(): Promise<DespliguesEInterruptores>
}
export const PUERTO_DESPLIEGUES = new InjectionToken<PuertoDespliegues>('aportaya.sistemas.puertoDespliegues')

export interface PuertoBaseDeDatos {
  obtener(): Promise<Migracion[]>
}
export const PUERTO_BASE_DE_DATOS = new InjectionToken<PuertoBaseDeDatos>('aportaya.sistemas.puertoBaseDeDatos')

export interface PuertoRespaldos {
  obtener(): Promise<Respaldo[]>
}
export const PUERTO_RESPALDOS = new InjectionToken<PuertoRespaldos>('aportaya.sistemas.puertoRespaldos')

export interface PuertoProveedores {
  obtener(): Promise<Proveedor[]>
}
export const PUERTO_PROVEEDORES = new InjectionToken<PuertoProveedores>('aportaya.sistemas.puertoProveedores')

export interface PuertoOutbox {
  obtener(): Promise<OutboxYDescartados>
}
export const PUERTO_OUTBOX = new InjectionToken<PuertoOutbox>('aportaya.sistemas.puertoOutbox')

export interface PuertoWebhooks {
  obtener(): Promise<Webhook[]>
}
export const PUERTO_WEBHOOKS = new InjectionToken<PuertoWebhooks>('aportaya.sistemas.puertoWebhooks')

export interface PuertoAccesos {
  obtener(): Promise<Acceso[]>
}
export const PUERTO_ACCESOS = new InjectionToken<PuertoAccesos>('aportaya.sistemas.puertoAccesos')

export interface PuertoIncidentes {
  obtener(): Promise<Incidente[]>
}
export const PUERTO_INCIDENTES = new InjectionToken<PuertoIncidentes>('aportaya.sistemas.puertoIncidentes')

/** Los nueve tokens juntos: para que `proveedor-fuentes.ts` no pueda olvidarse de ninguno. */
export const TODOS_LOS_PUERTOS_DE_SISTEMAS = [
  PUERTO_SERVICIOS,
  PUERTO_DESPLIEGUES,
  PUERTO_BASE_DE_DATOS,
  PUERTO_RESPALDOS,
  PUERTO_PROVEEDORES,
  PUERTO_OUTBOX,
  PUERTO_WEBHOOKS,
  PUERTO_ACCESOS,
  PUERTO_INCIDENTES,
] as const
