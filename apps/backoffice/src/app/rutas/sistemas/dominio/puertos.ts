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
  /** El instante ISO de la última interrupción, para ordenar por fecha y no por el texto formateado. */
  ultimaInterrupcionIso?: string
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

export type Respaldo = { id: string; origen: string; tomadoEl: string; ultimaRestauracionProbadaEl: string | null }

/** Un proveedor externo. Sin costo registrado en el período, `costoRealUltimoPeriodo` y `moneda` quedan vacíos (la vista dice «—»). */
export type Proveedor = { id: string; nombre: string; categoria: string; costoRealUltimoPeriodo: string; moneda: string; activo?: string; periodo?: string }

/** Un webhook recibido. `detalle` es el estado tal como lo registró el servicio dueño (procesado, duplicado, rechazado…). */
export type Webhook = { id: string; direccion: 'entrante' | 'saliente'; origen: string; ultimoEstado: 'ok' | 'error'; ultimaEntregaEl: string; detalle?: string; ultimaEntregaIso?: string }

/**
 * Un acceso = una persona interna con un rol. `estado` y `segundoFactor` los trae identidad (`UsuarioInternoResumen`);
 * `otorgadoEl` no lo publica ningún contrato todavía y el adaptador real lo deja en «—».
 */
export type Acceso = { id: string; persona: string; rol: string; ambito: string; otorgadoEl: string; estado?: string; segundoFactor?: string }

/**
 * Un incidente. `estado` resume si sigue afectando (abierto) o ya se restableció (cerrado); `etapa` y `sistema` los
 * trae el adaptador real (`auditoria.incidente_operativo`): Detectado, Mitigado, Post mortem o Resuelto.
 */
export type Incidente = { id: string; titulo: string; severidad: 'sev1' | 'sev2' | 'sev3'; estado: 'abierto' | 'cerrado'; abiertoEl: string; etapa?: string; sistema?: string; abiertoElIso?: string }

// ---- El error de dominio que emite la fuente no disponible -------------------------

/** Lo que rechaza cualquier puerto cuando la fuente real no está disponible: tipado, nunca un número inventado. */
export type ErrorFuenteNoDisponible = {
  readonly tipo: 'fuente-no-disponible'
  readonly mensaje: string
  readonly trazaId?: string
}

/** Con `motivo`, la pantalla dice qué falta en concreto (ver `entregables/brecha-observabilidad.md`). */
export function errorFuenteNoDisponible(motivo?: string): ErrorFuenteNoDisponible {
  return {
    tipo: 'fuente-no-disponible',
    mensaje: motivo ?? 'Esta información va a aparecer acá cuando el sistema que la registra esté conectado. No es una falla y no hace falta hacer nada.',
  }
}

// ---- Los siete puertos ---------------------------------------------------------------

export interface PuertoServicios {
  obtener(): Promise<EstadoServicio[]>
}
export const PUERTO_SERVICIOS = new InjectionToken<PuertoServicios>('aportaya.sistemas.puertoServicios')

export interface PuertoDespliegues {
  obtener(): Promise<DespliguesEInterruptores>
}
export const PUERTO_DESPLIEGUES = new InjectionToken<PuertoDespliegues>('aportaya.sistemas.puertoDespliegues')

export interface PuertoRespaldos {
  obtener(): Promise<Respaldo[]>
}
export const PUERTO_RESPALDOS = new InjectionToken<PuertoRespaldos>('aportaya.sistemas.puertoRespaldos')

export interface PuertoProveedores {
  obtener(): Promise<Proveedor[]>
}
export const PUERTO_PROVEEDORES = new InjectionToken<PuertoProveedores>('aportaya.sistemas.puertoProveedores')

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

/** Los siete tokens juntos: para que `proveedor-fuentes.ts` no pueda olvidarse de ninguno. */
export const TODOS_LOS_PUERTOS_DE_SISTEMAS = [
  PUERTO_SERVICIOS,
  PUERTO_DESPLIEGUES,
  PUERTO_RESPALDOS,
  PUERTO_PROVEEDORES,
  PUERTO_WEBHOOKS,
  PUERTO_ACCESOS,
  PUERTO_INCIDENTES,
] as const
