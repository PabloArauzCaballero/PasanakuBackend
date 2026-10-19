import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import type { Tono } from '@aportaya/ui/tono/tono'
import { GATEWAY } from '../../../../nucleo/gateway'

export type Nivel = 'N1' | 'N2' | 'N3'
export type ServicioDeArquitectura = { servicio: string; nivel: Nivel; replicas: number; porQue: string }

/** Una fila de la pantalla: la arquitectura (constante) más lo que contestó el servicio (en vivo). */
export type FilaDeEstado = ServicioDeArquitectura & { salud: string; tono: Tono; latencia: string }

/**
 * Lo que es ARQUITECTURA y no cambia con el estado: el nivel de criticidad de cada servicio (ADR-037 — «el nivel no
 * lo elige el dueño del servicio, lo impone su peor dependiente») y las réplicas que EXIGE en producción. No es cuántas
 * corren: en TEST corre una por servicio. La salud ya NO vive acá: la consulta `auditoria` en el momento
 * (`GET /indicadores/operacion/servicios`, ver `recursoSaludDeServicios`). Antes era la constante «arriba» para los
 * catorce, que decía que todo andaba aunque un servicio estuviera caído.
 */
export const SERVICIOS: readonly ServicioDeArquitectura[] = [
  { servicio: 'identidad', nivel: 'N1', replicas: 4, porQue: 'Autenticar y autorizar: sin esto nadie entra' },
  { servicio: 'nucleo-financiero', nivel: 'N1', replicas: 4, porQue: 'El libro contable y la billetera: sin esto no se mueve plata' },
  { servicio: 'aportes', nivel: 'N1', replicas: 4, porQue: 'Cobrar el aporte, el flujo de todos los períodos' },
  { servicio: 'tarifas', nivel: 'N1', replicas: 4, porQue: '«¿Cuánto es la comisión?» está en el camino del cobro' },
  { servicio: 'grupos', nivel: 'N2', replicas: 3, porQue: 'Gobernanza: se atrasa un acuerdo, no se pierde plata' },
  { servicio: 'entregas', nivel: 'N2', replicas: 3, porQue: 'La entrega tiene fecha, no instante: tolera minutos' },
  { servicio: 'garantia', nivel: 'N2', replicas: 3, porQue: 'La cobertura se aplica en el barrido, no en línea' },
  { servicio: 'notificaciones', nivel: 'N2', replicas: 3, porQue: 'El outbox retiene: la caída se vuelve atraso, no aviso perdido' },
  { servicio: 'organizador', nivel: 'N2', replicas: 3, porQue: 'Habilitación y automatización: fuera del camino crítico' },
  { servicio: 'cumplimiento', nivel: 'N3', replicas: 2, porQue: 'Los límites se leen del catálogo; monitorear llega por evento' },
  { servicio: 'auditoria', nivel: 'N3', replicas: 2, porQue: 'Consultas y exportes: se difieren sin consecuencia' },
  { servicio: 'transparencia', nivel: 'N3', replicas: 2, porQue: 'Reputación y certificados: nadie afuera nota diez minutos' },
  { servicio: 'erp', nivel: 'N3', replicas: 2, porQue: 'Contabilidad de gestión: trabaja sobre períodos cerrados' },
  { servicio: 'publicidad', nivel: 'N3', replicas: 2, porQue: 'Lo primero que se apaga en degradación controlada' },
] as const

export const NIVELES: readonly { valor: Nivel; texto: string }[] = [
  { valor: 'N1', texto: 'N1 · crítico' },
  { valor: 'N2', texto: 'N2 · esencial' },
  { valor: 'N3', texto: 'N3 · diferible' },
]

const EN_PALABRAS: Record<string, { salud: string; tono: Tono }> = {
  ARRIBA: { salud: 'arriba', tono: 'ok' },
  DEGRADADO: { salud: 'con problemas', tono: 'aviso' },
  CAIDO: { salud: 'no responde', tono: 'error' },
}

/** Por qué no hay salud que mostrar: nunca se rellena con «arriba». */
export type SinSalud = 'consultando' | 'sinPermiso' | 'error'
const SIN_SALUD: Record<SinSalud, { salud: string; tono: Tono }> = {
  consultando: { salud: 'consultando…', tono: 'neutro' },
  sinPermiso: { salud: 'sin permiso', tono: 'neutro' },
  error: { salud: 'sin dato', tono: 'neutro' },
}

export function filasConSalud(salida: SalidaSaludDeServicios | undefined, sinSalud: SinSalud | undefined): FilaDeEstado[] {
  const porServicio = new Map((salida?.servicios ?? []).map((s) => [s.servicio, s]))
  return SERVICIOS.map((s) => {
    const r = porServicio.get(s.servicio)
    const texto = sinSalud ? SIN_SALUD[sinSalud] : r ? EN_PALABRAS[r.estado]! : { salud: 'sin dato', tono: 'neutro' as Tono }
    const latencia = !sinSalud && r?.latenciaMs !== undefined ? `${r.latenciaMs} ms` : '—'
    return { ...s, ...texto, latencia }
  })
}

/** «15:04:05»: la hora de La Paz en que `auditoria` preguntó. */
export function horaDeConsulta(iso: string): string {
  return new Intl.DateTimeFormat('es-BO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false, timeZone: 'America/La_Paz' }).format(new Date(iso))
}

/** La salud de cada servicio, preguntada ahora por `auditoria` (o hace menos de 10 s). */
export function recursoSaludDeServicios() {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaSaludDeServicios>(() => `${gateway}/indicadores/operacion/servicios`)
}
