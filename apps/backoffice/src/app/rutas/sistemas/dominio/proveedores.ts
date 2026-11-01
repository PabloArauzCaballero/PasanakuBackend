import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { ProveedorDePagoResumen } from 'clientes/angular/aportes'
import type { ProveedorDeMensajeriaResumen } from 'clientes/angular/notificaciones'
import type { CostoDeProveedor } from 'clientes/angular/tarifas'
import { GATEWAY } from '../../../nucleo/gateway'
import type { Proveedor, PuertoProveedores } from './puertos'

const TIPO: Readonly<Record<string, string>> = { BANCO: 'Banco', BILLETERA: 'Billetera', PASARELA: 'Pasarela' }

/** El último mes cerrado en La Paz, `AAAA-MM`: el 3 de noviembre es «2026-10». */
export function periodoAnterior(ahora: Date): string {
  const [anio, mes] = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/La_Paz', year: 'numeric', month: '2-digit' })
    .format(ahora)
    .split('-')
    .map(Number) as [number, number]
  const a = mes === 1 ? anio - 1 : anio
  const m = mes === 1 ? 12 : mes - 1
  return `${a}-${String(m).padStart(2, '0')}`
}

/**
 * Sistemas › Proveedores con los datos REALES de sus dueños: los de pago (aportes, `listarProveedoresDePago`), los de
 * mensajería (notificaciones, `listarProveedoresDeMensajeria`) y el costo del último mes cerrado (tarifas,
 * `listarCostosDeProveedor`). Tarifas solo registra costos de proveedores de pago (su FK apunta a
 * `aportes.proveedor_pago`), así que la mensajería queda sin costo. Un proveedor con costo en dos monedas sale en dos
 * filas: las monedas no se suman (regla 91.1.2). `costos` vacío o ausente deja el costo vacío, nunca un cero.
 */
export function filasDeProveedores(
  pagos: readonly ProveedorDePagoResumen[],
  mensajeria: readonly ProveedorDeMensajeriaResumen[],
  costos: readonly CostoDeProveedor[] | undefined,
  periodo: string,
): Proveedor[] {
  const porProveedor = new Map<string, CostoDeProveedor[]>()
  for (const c of costos ?? []) porProveedor.set(c.proveedorId, [...(porProveedor.get(c.proveedorId) ?? []), c])
  const activo = (a: boolean) => (a ? 'Activo' : 'Inactivo')
  return [
    ...pagos.flatMap((p) => {
      const base = { nombre: `${p.nombre} (${p.codigo})`, categoria: `Pagos · ${TIPO[p.tipo] ?? p.tipo}`, activo: activo(p.activo), periodo }
      const suyos = porProveedor.get(p.id) ?? []
      if (!suyos.length) return [{ id: p.id, ...base, costoRealUltimoPeriodo: '', moneda: '' }]
      return suyos.map((c) => ({ id: `${p.id}:${c.moneda}`, ...base, costoRealUltimoPeriodo: c.costoTotal, moneda: c.moneda }))
    }),
    ...mensajeria.map((m) => ({
      id: m.id,
      nombre: `${m.nombre} (${m.codigo})`,
      categoria: `Mensajería · ${m.canales}`,
      activo: activo(m.activo),
      periodo,
      costoRealUltimoPeriodo: '',
      moneda: '',
    })),
  ]
}

/**
 * Proveedores de pago y de mensajería son el listado: si alguno falla, el puerto rechaza. El costo es complemento:
 * si tarifas falla, los proveedores se ven igual sin costo.
 */
export function puertoProveedores(ahora: () => Date = () => new Date()): PuertoProveedores {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    obtener: async () => {
      const periodo = periodoAnterior(ahora())
      const [pagos, mensajeria, costos] = await Promise.all([
        firstValueFrom(http.get<ProveedorDePagoResumen[]>(`${gateway}/pagos/proveedores`)),
        firstValueFrom(http.get<ProveedorDeMensajeriaResumen[]>(`${gateway}/notificaciones/proveedores`)),
        firstValueFrom(http.get<CostoDeProveedor[]>(`${gateway}/tarifas/costos-de-proveedor?periodo=${periodo}`)).catch(() => undefined),
      ])
      return filasDeProveedores(pagos, mensajeria, costos, periodo)
    },
  }
}
