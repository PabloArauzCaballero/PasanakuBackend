import { httpResource, HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'

/**
 * D-15 · «F7 suma la vista de solicitudes escaladas cuando el organizador deja vencer
 * el plazo» (`planes/20` §D-15). La solicitud nace en CU-68 (`solicitud_ingreso`,
 * `PENDIENTE`) y el organizador tiene 48 horas para resolverla.
 *
 * **Supuesto declarado**: ni `servicios/grupos/openapi/grupos.yaml` ni CU-68 publican
 * todavía el endpoint de escalamiento (`GET /grupos/solicitudes-escaladas`) — CU-68 solo
 * define `POST /grupos/:id/solicitudes` y la resolución del organizador. La forma de
 * abajo sale de los campos que CU-68 ya declara para `solicitud_ingreso`
 * (`solicitudId`, `puntajeCompatibilidad` con su desglose, `fechaLimite`) más el dato
 * que este escalamiento necesariamente agrega: desde cuándo está vencida y a quién
 * (el equipo de operación) le toca resolver en su lugar. Se pide a `grupos` (backend)
 * la operación; el nombre de ruta puede cambiar cuando exista el contrato real.
 */
export type FactorDePuntaje = { motivo: string; aFavor: boolean }

export type SolicitudEscalada = {
  solicitudId: string
  grupoId: string
  grupoCodigo: string
  usuarioId: string
  usuarioNombre: string
  canal: 'QR' | 'CODIGO'
  puntajeCompatibilidad: number
  factores: FactorDePuntaje[]
  fechaLimiteOrganizador: string
  escaladaEn: string
  estado: 'ESCALADA' | 'RESUELTA'
}

export function colaDeSolicitudesEscaladas() {
  const gateway = inject(GATEWAY)
  return httpResource<SolicitudEscalada[]>(() => `${gateway}/grupos/solicitudes-escaladas`)
}

export function ordenadasPorVencimiento(solicitudes: readonly SolicitudEscalada[]): SolicitudEscalada[] {
  return [...solicitudes].sort((a, b) => new Date(a.fechaLimiteOrganizador).getTime() - new Date(b.fechaLimiteOrganizador).getTime())
}

/**
 * El `cargador` de `TablaDeDatosVirtualizada` (`nucleo/tabla/`, del shell) — misma
 * salvedad que `cargarReclamos`: pagina/ordena en el adaptador porque el endpoint
 * asumido todavía no lo hace del lado del servidor.
 */
export function cargarSolicitudesEscaladas(http: HttpClient, gateway: string): CargadorDePagina<SolicitudEscalada> {
  return async (pedido) => {
    const todas = await firstValueFrom(http.get<SolicitudEscalada[]>(`${gateway}/grupos/solicitudes-escaladas`))
    const ordenadas = pedido.orden
      ? [...todas].sort((a, b) => {
          const clave = pedido.orden!.clave as keyof SolicitudEscalada
          const [va, vb] = [String(a[clave] ?? ''), String(b[clave] ?? '')]
          return pedido.orden!.sentido === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
        })
      : ordenadasPorVencimiento(todas)
    const inicio = (pedido.pagina - 1) * pedido.tamano
    const pagina: PaginaServidor<SolicitudEscalada> = { filas: ordenadas.slice(inicio, inicio + pedido.tamano), total: ordenadas.length }
    return pagina
  }
}

/** Ídem `cargadorDeReclamos`: fábrica en contexto de inyección, sin `HttpClient` en `rutas/`. */
export function cargadorDeSolicitudesEscaladas(): CargadorDePagina<SolicitudEscalada> {
  return cargarSolicitudesEscaladas(inject(HttpClient), inject(GATEWAY))
}
