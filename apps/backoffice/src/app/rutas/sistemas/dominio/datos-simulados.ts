/**
 * HUECO DECLARADO: no existe un contrato OpenAPI real de "indicadores"/"tablero" para
 * el backoffice de sistemas — se buscó en `servicios/erp/src/main/resources/openapi/
 * erp.yaml` y en `docs/CasosDeUso/CU-98*.md` y ninguno expone estado de servicios, SLO,
 * despliegues, base, respaldos, proveedores, outbox, webhooks o accesos como API. Se
 * declaró el hueco en `planes/informes/carril-B5.md` §Huecos, pedido al carril dueño de
 * un futuro servicio "observabilidad"/"plataforma" (no asignado en `planes/16`).
 *
 * Mientras ese contrato no exista, estas pantallas se arman con datos de ejemplo fijos
 * (el mismo criterio que un `ejemploDe(...)` de `@aportaya/simulado`, pero sin un
 * `openapi/` del que generarlos: no se inventa una forma de servicio que nadie declaró).
 * Cuando el contrato exista, esta función se reemplaza por `httpResource` como hace
 * `cu13-consultar-saldo.ts` en operación — el resto de la pantalla no cambia.
 */

export type EstadoServicio = {
  id: string
  nombre: string
  estado: 'operativo' | 'degradado' | 'caido'
  disponibilidad30d: string
  presupuestoErrorRestante: string
  ultimaInterrupcion: string
}

export const serviciosSimulados: EstadoServicio[] = [
  { id: 'nucleo-financiero', nombre: 'nucleo-financiero', estado: 'operativo', disponibilidad30d: '99.95%', presupuestoErrorRestante: '68%', ultimaInterrupcion: '2026-07-02' },
  { id: 'erp', nombre: 'erp', estado: 'operativo', disponibilidad30d: '99.98%', presupuestoErrorRestante: '90%', ultimaInterrupcion: '2026-05-11' },
  { id: 'cumplimiento', nombre: 'cumplimiento', estado: 'degradado', disponibilidad30d: '99.40%', presupuestoErrorRestante: '12%', ultimaInterrupcion: '2026-09-08' },
  { id: 'notificaciones', nombre: 'notificaciones', estado: 'operativo', disponibilidad30d: '99.90%', presupuestoErrorRestante: '55%', ultimaInterrupcion: '2026-08-01' },
]

export type Despliegue = {
  id: string
  servicio: string
  version: string
  desplegadoEl: string
  desplegadoPor: string
  estado: 'exitoso' | 'en curso' | 'revertido'
}

export const desplieguesSimulados: Despliegue[] = [
  { id: 'd-401', servicio: 'nucleo-financiero', version: '2026.9.3', desplegadoEl: '2026-09-10 14:20', desplegadoPor: 'ci-bot', estado: 'exitoso' },
  { id: 'd-400', servicio: 'cumplimiento', version: '2026.9.2', desplegadoEl: '2026-09-08 09:05', desplegadoPor: 'ci-bot', estado: 'revertido' },
]

export type Interruptor = {
  id: string
  nombre: string
  tocaDinero: boolean
  activo: boolean
}

export const interruptoresSimulados: Interruptor[] = [
  { id: 'i-1', nombre: 'Habilitar retiros extraordinarios', tocaDinero: true, activo: false },
  { id: 'i-2', nombre: 'Mostrar banner de mantenimiento', tocaDinero: false, activo: false },
  { id: 'i-3', nombre: 'Congelar nuevas aperturas de grupo', tocaDinero: false, activo: true },
]

/**
 * Un interruptor que toca dinero exige dos personas distintas: quien lo pide no puede
 * ser quien lo confirma. `puedeConfirmar` es la regla pura; la pantalla la envuelve en
 * un diálogo de doble confirmación simulado (no hay todavía un flujo real de "otra
 * sesión" en el backoffice — se simula pidiendo el correo de quien confirma).
 */
export function puedeConfirmar(solicitanteEmail: string, confirmanteEmail: string): boolean {
  const s = solicitanteEmail.trim().toLowerCase()
  const c = confirmanteEmail.trim().toLowerCase()
  return c.length > 0 && c !== s
}

export type Migracion = { id: string; nombre: string; aplicadaEl: string | null; estado: 'aplicada' | 'pendiente' }

export const migracionesSimuladas: Migracion[] = [
  { id: 'v221', nombre: 'v221__indice_movimientos.sql', aplicadaEl: '2026-09-01 03:10', estado: 'aplicada' },
  { id: 'v222', nombre: 'v222__columna_riesgo_grupo.sql', aplicadaEl: null, estado: 'pendiente' },
]

export type Respaldo = {
  id: string
  origen: string
  tomadoEl: string
  ultimaRestauracionProbadaEl: string | null
}

export const respaldosSimulados: Respaldo[] = [
  { id: 'r-1', origen: 'base-nucleo-financiero', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: '2026-08-29' },
  { id: 'r-2', origen: 'base-cumplimiento', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: '2026-06-15' },
  { id: 'r-3', origen: 'base-erp', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: null },
]

/** Una restauración vencida es la que se probó hace más de 30 días, o nunca se probó. */
export function restauracionVencida(respaldo: Respaldo, hoy: Date = new Date()): boolean {
  if (!respaldo.ultimaRestauracionProbadaEl) return true
  const probada = new Date(respaldo.ultimaRestauracionProbadaEl)
  const dias = (hoy.getTime() - probada.getTime()) / (1000 * 60 * 60 * 24)
  return dias > 30
}

export type Proveedor = { id: string; nombre: string; categoria: string; costoRealUltimoPeriodo: string; moneda: string }

export const proveedoresSimulados: Proveedor[] = [
  { id: 'p-1', nombre: 'Pasarela QR', categoria: 'pagos', costoRealUltimoPeriodo: '1240.50', moneda: 'BOB' },
  { id: 'p-2', nombre: 'SMS/WhatsApp', categoria: 'mensajería', costoRealUltimoPeriodo: '312.00', moneda: 'BOB' },
]

export type MensajeOutbox = { id: string; tipo: string; creadoEl: string; estado: 'pendiente' | 'entregado' }
export type MensajeDescartado = { id: string; tipo: string; motivo: string; descartadoEl: string }

export const outboxSimulado: MensajeOutbox[] = [
  { id: 'o-1', tipo: 'EventoAporteRegistrado', creadoEl: '2026-09-11 08:00', estado: 'entregado' },
  { id: 'o-2', tipo: 'EventoAlertaTemprana', creadoEl: '2026-09-11 08:05', estado: 'pendiente' },
]

export const descartadosSimulados: MensajeDescartado[] = [
  { id: 'x-1', tipo: 'EventoWebhookProveedor', motivo: 'Firma inválida: la clave del proveedor rotó y no se actualizó el secreto.', descartadoEl: '2026-09-10 22:14' },
  { id: 'x-2', tipo: 'EventoNotificacionSMS', motivo: 'Excedió 5 reintentos: el proveedor devolvió 500 en cada intento.', descartadoEl: '2026-09-09 11:02' },
]

export type Webhook = { id: string; direccion: 'entrante' | 'saliente'; origen: string; ultimoEstado: 'ok' | 'error'; ultimaEntregaEl: string }

export const webhooksSimulados: Webhook[] = [
  { id: 'w-1', direccion: 'entrante', origen: 'pasarela-qr', ultimoEstado: 'ok', ultimaEntregaEl: '2026-09-11 07:58' },
  { id: 'w-2', direccion: 'saliente', origen: 'notificaciones', ultimoEstado: 'error', ultimaEntregaEl: '2026-09-10 19:40' },
]

export type Acceso = { id: string; persona: string; rol: string; ambito: string; otorgadoEl: string }

export const accesosSimulados: Acceso[] = [
  { id: 'a-1', persona: 'j.perez@aportaya.bo', rol: 'PLATAFORMA', ambito: 'backoffice-sistemas', otorgadoEl: '2026-03-01' },
  { id: 'a-2', persona: 'm.rios@aportaya.bo', rol: 'SEGURIDAD', ambito: 'backoffice-sistemas', otorgadoEl: '2026-05-14' },
]

export type Incidente = { id: string; titulo: string; severidad: 'sev1' | 'sev2' | 'sev3'; estado: 'abierto' | 'cerrado'; abiertoEl: string }

export const incidentesSimulados: Incidente[] = [
  { id: 'inc-1', titulo: 'Latencia alta en cumplimiento', severidad: 'sev2', estado: 'abierto', abiertoEl: '2026-09-08 09:10' },
  { id: 'inc-2', titulo: 'Webhook de pasarela QR caído 12 min', severidad: 'sev3', estado: 'cerrado', abiertoEl: '2026-08-20 15:30' },
]
