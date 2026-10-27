import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { SaludDeServicio, SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import type { EstadoServicio, PuertoServicios } from './puertos'

/** Lo que el contrato no publica (todavía no hay SLO ni historial de interrupciones): se dice, no se inventa. */
export const SIN_DATO = '—'

// Por texto y con import solo de tipos: el cliente generado lo arma el host al construir, y depender de un valor
// suyo (el enum) es lo único que podría romper la imagen si el cliente del host va atrás del contrato.
const ESTADO: Readonly<Record<string, EstadoServicio['estado']>> = { ARRIBA: 'operativo', DEGRADADO: 'degradado', CAIDO: 'caido' }

/**
 * La salud REAL de cada servicio para Sistemas › Servicios: `auditoria` pregunta el readiness de cada uno ahora
 * (`GET /indicadores/operacion/servicios`, la misma fuente que Operación › Estado). Hasta 2026-10-27 esta pantalla
 * usaba la fuente «no disponible» aunque el endpoint ya existía, y en TEST mostraba siempre el error.
 *
 * Disponibilidad a 30 días, presupuesto de error y última interrupción no vienen en el contrato: quedan en «—».
 */
export function filasDeSalud(salida: SalidaSaludDeServicios): EstadoServicio[] {
  return salida.servicios.map((s) => ({
    id: s.servicio,
    nombre: s.servicio,
    estado: ESTADO[s.estado as SaludDeServicio['estado']] ?? 'caido',
    disponibilidad30d: SIN_DATO,
    presupuestoErrorRestante: SIN_DATO,
    ultimaInterrupcion: SIN_DATO,
  }))
}

/** El puerto de servicios contra el gateway. Un error de red llega traducido por el interceptor (mensaje y trazaId). */
export function puertoSaludDeServicios(): PuertoServicios {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { obtener: async () => filasDeSalud(await firstValueFrom(http.get<SalidaSaludDeServicios>(`${gateway}/indicadores/operacion/servicios`))) }
}
