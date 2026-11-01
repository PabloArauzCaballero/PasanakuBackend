import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { firstValueFrom } from 'rxjs'
import type { IncidenteOperativo, PaginaDeIncidentesOperativos } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import type { Incidente, PuertoIncidentes } from './puertos'

/** Cuántos se traen: los más recientes. La pantalla pagina y filtra sobre ellos; si hay más, lo dice el total. */
export const TOPE_DE_INCIDENTES = 100

// Por texto, con import solo de tipos (mismo motivo que `salud-de-servicios.ts`).
const SEVERIDAD: Readonly<Record<string, Incidente['severidad']>> = { SEV1: 'sev1', SEV2: 'sev2', SEV3: 'sev3' }
const ETAPA: Readonly<Record<string, { etapa: string; estado: Incidente['estado'] }>> = {
  DETECTADO: { etapa: 'Detectado', estado: 'abierto' },
  MITIGADO: { etapa: 'Mitigado', estado: 'abierto' },
  // El post mortem se hace con el servicio ya restablecido: para quien opera, el incidente está cerrado.
  POST_MORTEM: { etapa: 'Post mortem', estado: 'cerrado' },
  RESUELTO: { etapa: 'Resuelto', estado: 'cerrado' },
}

/**
 * Sistemas › Incidentes con los incidentes REALES de `auditoria.incidente_operativo`
 * (`GET /indicadores/operacion/incidentes`, del más reciente al más viejo). Una severidad o etapa nueva que el
 * contrato agregue no rompe la vista: se trata como la más alta y se muestra el código.
 */
export function filasDeIncidentes(pagina: PaginaDeIncidentesOperativos): Incidente[] {
  return pagina.items.map((i: IncidenteOperativo) => {
    const etapa = ETAPA[i.estado] ?? { etapa: i.estado, estado: 'abierto' as const }
    return {
      id: i.id,
      titulo: `${i.codigo} · ${i.titulo}`,
      severidad: SEVERIDAD[i.severidad] ?? 'sev1',
      estado: etapa.estado,
      etapa: etapa.etapa,
      sistema: i.sistemaAfectado,
      abiertoEl: formatearFecha(i.detectadoEn),
      abiertoElIso: i.detectadoEn,
    }
  })
}

export function puertoIncidentes(): PuertoIncidentes {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    obtener: async () =>
      filasDeIncidentes(await firstValueFrom(http.get<PaginaDeIncidentesOperativos>(`${gateway}/indicadores/operacion/incidentes?pagina=1&limite=${TOPE_DE_INCIDENTES}`))),
  }
}
