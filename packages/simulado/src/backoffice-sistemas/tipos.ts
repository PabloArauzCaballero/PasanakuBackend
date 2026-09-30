/**
 * Tipos SINTÉTICOS — no son un contrato: son la forma que hoy inventan las nueve
 * pantallas de `apps/backoffice/rutas/sistemas/` mientras no exista un contrato real
 * de "indicadores"/"tablero" (ver `entregables/brecha-observabilidad.md` en el
 * repo raíz). El sufijo `Simulado` marca cada tipo como dato de ejemplo, nunca como
 * dato real — la regla de `seed-data-catalogs`: un dato inventado presentado como
 * real contamina; uno declarado como sintético, no.
 *
 * Este paquete no depende de `apps/backoffice` (las apps dependen de `packages/`, no
 * al revés): los tipos de dominio "reales" que consume cada puerto viven en
 * `apps/backoffice/src/app/rutas/sistemas/dominio/puertos.ts` y tienen la MISMA forma
 * estructural que estos — es intencional, no un descuido: el adaptador simulado
 * (`adaptador.ts`) implementa esos puertos, y TypeScript verifica la forma en el punto
 * de uso, no el nombre del tipo.
 */

export type EstadoServicioSimulado = {
  id: string
  nombre: string
  estado: 'operativo' | 'degradado' | 'caido'
  disponibilidad30d: string
  presupuestoErrorRestante: string
  ultimaInterrupcion: string
}

export type DespliegueSimulado = {
  id: string
  servicio: string
  version: string
  desplegadoEl: string
  desplegadoPor: string
  estado: 'exitoso' | 'en curso' | 'revertido'
}

export type InterruptorSimulado = { id: string; nombre: string; tocaDinero: boolean; activo: boolean }

export type MigracionSimulada = { id: string; nombre: string; aplicadaEl: string | null; estado: 'aplicada' | 'pendiente' }

export type RespaldoSimulado = { id: string; origen: string; tomadoEl: string; ultimaRestauracionProbadaEl: string | null }

export type ProveedorSimulado = { id: string; nombre: string; categoria: string; costoRealUltimoPeriodo: string; moneda: string }

export type MensajeOutboxSimulado = { id: string; tipo: string; creadoEl: string; estado: 'pendiente' | 'entregado' }

export type MensajeDescartadoSimulado = { id: string; tipo: string; motivo: string; descartadoEl: string }

export type WebhookSimulado = {
  id: string
  direccion: 'entrante' | 'saliente'
  origen: string
  ultimoEstado: 'ok' | 'error'
  ultimaEntregaEl: string
}

export type AccesoSimulado = { id: string; persona: string; rol: string; ambito: string; otorgadoEl: string }

export type IncidenteSimulado = { id: string; titulo: string; severidad: 'sev1' | 'sev2' | 'sev3'; estado: 'abierto' | 'cerrado'; abiertoEl: string }
