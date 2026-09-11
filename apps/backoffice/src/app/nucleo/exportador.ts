import { HttpClient } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { Observable } from 'rxjs'
import { GATEWAY } from './gateway'
import { Sesion } from './sesion'

export type FormatoDeExportacion = 'csv' | 'xlsx'

export type PedidoDeExportacion = {
  recurso: string
  formato: FormatoDeExportacion
  filtros: Record<string, string>
  orden?: { clave: string; sentido: 'asc' | 'desc' } | null
}

export type ExportacionSolicitada = {
  id: string
  estado: 'pendiente' | 'lista' | 'vencida'
  caducaIso: string
  topeDeDescargasRestante: number
}

/**
 * CU-58, y nada más: **nunca** un `CSV.stringify` acá. Este servicio pide al backend que
 * arme el archivo con la sesión de quien exporta — la huella y el tope de descargas los
 * decide el servidor, el cliente solo muestra lo que el servidor devuelve.
 *
 * `permisoRequerido()` es el permiso que una pantalla consulta para decidir si **muestra**
 * el botón; el rechazo real, si igual se pide sin permiso, lo da el `403` del gateway.
 */
@Injectable({ providedIn: 'root' })
export class Exportador {
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)
  private readonly sesion = inject(Sesion)

  permisoRequerido(recurso: string): string {
    return `exportar:${recurso}`
  }

  puedeExportar(recurso: string): boolean {
    return this.sesion.puede(this.permisoRequerido(recurso))
  }

  solicitar(pedido: PedidoDeExportacion): Observable<ExportacionSolicitada> {
    return this.http.post<ExportacionSolicitada>(`${this.gateway}/extraccion/exportaciones`, pedido)
  }
}
