import type {
  AccesoSimulado,
  DespliegueSimulado,
  EstadoServicioSimulado,
  IncidenteSimulado,
  InterruptorSimulado,
  MensajeDescartadoSimulado,
  MensajeOutboxSimulado,
  MigracionSimulada,
  ProveedorSimulado,
  RespaldoSimulado,
  WebhookSimulado,
} from './tipos'

/**
 * Los mismos nueve arrays que antes vivían en `apps/backoffice/.../dominio/datos-
 * simulados.ts`, movidos acá sin cambiar un solo valor (H2.S1.M2): el hallazgo no era
 * el contenido, era que nueve pantallas los importaban directo y por eso entraban al
 * bundle de producción. Acá adentro es exactamente donde deben vivir: un paquete que
 * la rama de producción del backoffice nunca importa (ver `proveedor-fuentes.ts`).
 */

export const serviciosSimulados: EstadoServicioSimulado[] = [
  { id: 'nucleo-financiero', nombre: 'nucleo-financiero', estado: 'operativo', disponibilidad30d: '99.95%', presupuestoErrorRestante: '68%', ultimaInterrupcion: '2026-07-02' },
  { id: 'erp', nombre: 'erp', estado: 'operativo', disponibilidad30d: '99.98%', presupuestoErrorRestante: '90%', ultimaInterrupcion: '2026-05-11' },
  { id: 'cumplimiento', nombre: 'cumplimiento', estado: 'degradado', disponibilidad30d: '99.40%', presupuestoErrorRestante: '12%', ultimaInterrupcion: '2026-09-08' },
  { id: 'notificaciones', nombre: 'notificaciones', estado: 'operativo', disponibilidad30d: '99.90%', presupuestoErrorRestante: '55%', ultimaInterrupcion: '2026-08-01' },
]

export const desplieguesSimulados: DespliegueSimulado[] = [
  { id: 'd-401', servicio: 'nucleo-financiero', version: '2026.9.3', desplegadoEl: '2026-09-10 14:20', desplegadoPor: 'ci-bot', estado: 'exitoso' },
  { id: 'd-400', servicio: 'cumplimiento', version: '2026.9.2', desplegadoEl: '2026-09-08 09:05', desplegadoPor: 'ci-bot', estado: 'revertido' },
]

export const interruptoresSimulados: InterruptorSimulado[] = [
  { id: 'i-1', nombre: 'Habilitar retiros extraordinarios', tocaDinero: true, activo: false },
  { id: 'i-2', nombre: 'Mostrar banner de mantenimiento', tocaDinero: false, activo: false },
  { id: 'i-3', nombre: 'Congelar nuevas aperturas de grupo', tocaDinero: false, activo: true },
]

export const migracionesSimuladas: MigracionSimulada[] = [
  { id: 'v221', nombre: 'v221__indice_movimientos.sql', aplicadaEl: '2026-09-01 03:10', estado: 'aplicada' },
  { id: 'v222', nombre: 'v222__columna_riesgo_grupo.sql', aplicadaEl: null, estado: 'pendiente' },
]

export const respaldosSimulados: RespaldoSimulado[] = [
  { id: 'r-1', origen: 'base-nucleo-financiero', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: '2026-08-29' },
  { id: 'r-2', origen: 'base-cumplimiento', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: '2026-06-15' },
  { id: 'r-3', origen: 'base-erp', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: null },
]

export const proveedoresSimulados: ProveedorSimulado[] = [
  { id: 'p-1', nombre: 'Pasarela QR', categoria: 'pagos', costoRealUltimoPeriodo: '1240.50', moneda: 'BOB' },
  { id: 'p-2', nombre: 'SMS/WhatsApp', categoria: 'mensajería', costoRealUltimoPeriodo: '312.00', moneda: 'BOB' },
]

export const outboxSimulado: MensajeOutboxSimulado[] = [
  { id: 'o-1', tipo: 'EventoAporteRegistrado', creadoEl: '2026-09-11 08:00', estado: 'entregado' },
  { id: 'o-2', tipo: 'EventoAlertaTemprana', creadoEl: '2026-09-11 08:05', estado: 'pendiente' },
]

export const descartadosSimulados: MensajeDescartadoSimulado[] = [
  { id: 'x-1', tipo: 'EventoWebhookProveedor', motivo: 'Firma inválida: la clave del proveedor rotó y no se actualizó el secreto.', descartadoEl: '2026-09-10 22:14' },
  { id: 'x-2', tipo: 'EventoNotificacionSMS', motivo: 'Excedió 5 reintentos: el proveedor devolvió 500 en cada intento.', descartadoEl: '2026-09-09 11:02' },
]

export const webhooksSimulados: WebhookSimulado[] = [
  { id: 'w-1', direccion: 'entrante', origen: 'pasarela-qr', ultimoEstado: 'ok', ultimaEntregaEl: '2026-09-11 07:58' },
  { id: 'w-2', direccion: 'saliente', origen: 'notificaciones', ultimoEstado: 'error', ultimaEntregaEl: '2026-09-10 19:40' },
]

export const accesosSimulados: AccesoSimulado[] = [
  { id: 'a-1', persona: 'j.perez@aportaya.bo', rol: 'PLATAFORMA', ambito: 'backoffice-sistemas', otorgadoEl: '2026-03-01' },
  { id: 'a-2', persona: 'm.rios@aportaya.bo', rol: 'SEGURIDAD', ambito: 'backoffice-sistemas', otorgadoEl: '2026-05-14' },
]

export const incidentesSimulados: IncidenteSimulado[] = [
  { id: 'inc-1', titulo: 'Latencia alta en cumplimiento', severidad: 'sev2', estado: 'abierto', abiertoEl: '2026-09-08 09:10' },
  { id: 'inc-2', titulo: 'Webhook de pasarela QR caído 12 min', severidad: 'sev3', estado: 'cerrado', abiertoEl: '2026-08-20 15:30' },
]
