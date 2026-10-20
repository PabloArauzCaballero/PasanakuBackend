import { Injectable, computed, signal } from '@angular/core'
import datosOperaciones from './operaciones.json'
import type { FiltrosOperaciones, MetricasOperaciones, OperacionDelSistema } from './tipos'

@Injectable({ providedIn: 'root' })
export class ServicioOperaciones {
  private readonly todas = signal<readonly OperacionDelSistema[]>(datosOperaciones as OperacionDelSistema[])
  readonly operaciones = this.todas.asReadonly()

  readonly metricas = computed<MetricasOperaciones>(() => {
    const ops = this.todas()
    return {
      total: ops.length,
      mutaciones: ops.filter((o) => o.metodo !== 'GET').length,
      lecturas: ops.filter((o) => o.metodo === 'GET').length,
      criticas: ops.filter((o) => o.riesgo === 'CRITICO' || o.riesgo === 'ALTO').length,
      idempotentes: ops.filter((o) => o.idempotencia).length,
    }
  })

  filtrar(filtros: FiltrosOperaciones): OperacionDelSistema[] {
    const q = filtros.q.trim().toLowerCase()
    return this.todas().filter((op) => {
      if (filtros.servicio && op.servicio !== filtros.servicio) return false
      if (filtros.metodo && op.metodo !== filtros.metodo) return false
      if (filtros.riesgo && op.riesgo !== filtros.riesgo) return false
      if (filtros.flujo && op.flujo.id !== filtros.flujo) return false
      if (!q) return true

      return (
        op.ruta.toLowerCase().includes(q) ||
        op.operationId.toLowerCase().includes(q) ||
        op.proposito.toLowerCase().includes(q) ||
        op.modulo.toLowerCase().includes(q) ||
        op.tablasImpactadas.some((t) => t.tabla.toLowerCase().includes(q)) ||
        (op.dtoEntrada?.nombre ?? '').toLowerCase().includes(q)
      )
    })
  }

  buscarPorId(id: string): OperacionDelSistema | undefined {
    return this.todas().find((op) => op.id === id || op.operationId === id)
  }

  buscarPorTabla(tabla: string): OperacionDelSistema[] {
    const nombre = tabla.toLowerCase()
    return this.todas().filter((op) => op.tablasImpactadas.some((t) => t.tabla.toLowerCase() === nombre))
  }

  serviciosUnicos(): string[] {
    return Array.from(new Set(this.todas().map((o) => o.servicio))).sort()
  }

  flujosUnicos(): { id: string; nombre: string }[] {
    const mapa = new Map<string, string>()
    for (const op of this.todas()) {
      mapa.set(op.flujo.id, op.flujo.nombre)
    }
    return Array.from(mapa.entries()).map(([id, nombre]) => ({ id, nombre }))
  }
}
