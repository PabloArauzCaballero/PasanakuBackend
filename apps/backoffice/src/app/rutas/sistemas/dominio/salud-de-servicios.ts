import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { firstValueFrom } from 'rxjs'
import type { SaludDeServicio, SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import { decimal } from '../../tablero/dominio/indicadores'
import type { EstadoServicio, PuertoServicios } from './puertos'

/** Lo que todavía no se midió (o un servicio que nunca se interrumpió): se dice, no se inventa. */
export const SIN_DATO = '—'

// Por texto y con import solo de tipos: el cliente generado lo arma el host al construir, y depender de un valor
// suyo (el enum) es lo único que podría romper la imagen si el cliente del host va atrás del contrato.
const ESTADO: Readonly<Record<string, EstadoServicio['estado']>> = { ARRIBA: 'operativo', DEGRADADO: 'degradado', CAIDO: 'caido' }

/**
 * La salud REAL de cada servicio para Sistemas › Servicios: `auditoria` pregunta el readiness de cada uno ahora
 * (`GET /indicadores/operacion/servicios`, la misma fuente que Operación › Estado). Hasta 2026-10-27 esta pantalla
 * usaba la fuente «no disponible» aunque el endpoint ya existía, y en TEST mostraba siempre el error.
 *
 * Desde 2026-10-29 auditoria mide el readiness cada minuto y el contrato trae, por servicio, la disponibilidad de los
 * últimos 30 días, el presupuesto de error contra `objetivoSlo` (99,5 %) y la última interrupción. Un servicio sin
 * nada medido todavía, o que nunca se interrumpió, queda en «—»: no es 100 % ni «nunca», es que no hay dato.
 */
export function filasDeSalud(salida: SalidaSaludDeServicios): EstadoServicio[] {
  return salida.servicios.map((s) => ({
    id: s.servicio,
    nombre: s.servicio,
    estado: ESTADO[s.estado as SaludDeServicio['estado']] ?? 'caido',
    disponibilidad30d: s.disponibilidad30d ? `${decimal(s.disponibilidad30d)} %` : SIN_DATO,
    presupuestoErrorRestante: s.presupuestoErrorRestante ? `${decimal(s.presupuestoErrorRestante)} %` : SIN_DATO,
    ultimaInterrupcion: s.ultimaInterrupcion ? formatearFecha(s.ultimaInterrupcion) : SIN_DATO,
    ...(s.ultimaInterrupcion ? { ultimaInterrupcionIso: s.ultimaInterrupcion } : {}),
  }))
}

/** El puerto de servicios contra el gateway. Un error de red llega traducido por el interceptor (mensaje y trazaId). */
export function puertoSaludDeServicios(): PuertoServicios {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { obtener: async () => filasDeSalud(await firstValueFrom(http.get<SalidaSaludDeServicios>(`${gateway}/indicadores/operacion/servicios`))) }
}
