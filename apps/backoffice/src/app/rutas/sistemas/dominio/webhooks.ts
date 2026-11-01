import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { firstValueFrom } from 'rxjs'
import type { WebhookRecibido } from 'clientes/angular/aportes'
import type { EventoDeEntregaRecibido } from 'clientes/angular/notificaciones'
import { GATEWAY } from '../../../nucleo/gateway'
import type { PuertoWebhooks, Webhook } from './puertos'

/** Cuántos se piden a cada servicio: los más recientes. */
export const TOPE_DE_WEBHOOKS = 100

// Estado en palabras y si cuenta como error. Por texto, con import solo de tipos (como `salud-de-servicios.ts`).
const DE_PAGOS: Readonly<Record<string, [string, Webhook['ultimoEstado']]>> = {
  PROCESADO: ['Procesado', 'ok'],
  DUPLICADO: ['Duplicado (ya procesado)', 'ok'],
  RECIBIDO: ['Recibido, sin procesar', 'ok'],
  FALLIDO: ['Falló al procesar', 'error'],
  DESCARTADO: ['Descartado', 'error'],
}
const DE_MENSAJERIA: Readonly<Record<string, [string, Webhook['ultimoEstado']]>> = {
  ENVIADO: ['Enviado', 'ok'],
  ENTREGADO: ['Entregado', 'ok'],
  LEIDO: ['Leído', 'ok'],
  FALLIDO: ['Falló', 'error'],
  RECHAZADO: ['Rechazado', 'error'],
  EXPIRADO: ['Expiró', 'error'],
}

/**
 * Sistemas › Webhooks con lo que de verdad llegó: los webhooks de las pasarelas (aportes, `listarWebhooksRecibidos`)
 * y los avisos de entrega de los proveedores de mensajería (notificaciones, `listarEventosDeEntregaRecibidos`).
 * Todos son entrantes. Un webhook de pago con la firma inválida es un error aunque se haya descartado bien: es lo
 * primero que hay que mirar. Un estado nuevo del contrato se muestra tal cual y cuenta como error.
 */
export function filasDeWebhooks(pagos: readonly WebhookRecibido[], mensajeria: readonly EventoDeEntregaRecibido[]): Webhook[] {
  const filas: Webhook[] = [
    ...pagos.map((w) => {
      const [detalle, estado] = DE_PAGOS[w.estado] ?? [w.estado, 'error' as const]
      return {
        id: `pagos:${w.id}`,
        direccion: 'entrante' as const,
        origen: `Pasarela ${w.proveedorCodigo} · ${w.evento}`,
        ultimoEstado: w.firmaValida ? estado : ('error' as const),
        detalle: w.firmaValida ? detalle : `${detalle} · firma inválida`,
        ultimaEntregaEl: formatearFecha(w.recibidoEn),
        ultimaEntregaIso: w.recibidoEn,
      }
    }),
    ...mensajeria.map((e) => {
      const [detalle, estado] = DE_MENSAJERIA[e.tipoEvento] ?? [e.tipoEvento, 'error' as const]
      return {
        id: `mensajeria:${e.id}`,
        direccion: 'entrante' as const,
        origen: `Mensajería ${e.proveedorCodigo} · ${e.canal}`,
        ultimoEstado: estado,
        detalle: e.codigoError ? `${detalle} · código ${e.codigoError}` : detalle,
        ultimaEntregaEl: formatearFecha(e.recibidoEn),
        ultimaEntregaIso: e.recibidoEn,
      }
    }),
  ]
  return filas.sort((a, b) => (b.ultimaEntregaIso ?? '').localeCompare(a.ultimaEntregaIso ?? '') || a.id.localeCompare(b.id))
}

/**
 * Las dos fuentes se piden en paralelo. Si cualquiera falla, el puerto rechaza: una lista con la mitad de los
 * webhooks, sin decir que falta la otra mitad, se leería como «no llegó nada» de ese lado.
 */
export function puertoWebhooks(): PuertoWebhooks {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    obtener: async () => {
      const [pagos, mensajeria] = await Promise.all([
        firstValueFrom(http.get<WebhookRecibido[]>(`${gateway}/pagos/webhooks-recibidos?limite=${TOPE_DE_WEBHOOKS}`)),
        firstValueFrom(http.get<EventoDeEntregaRecibido[]>(`${gateway}/notificaciones/webhooks-recibidos?limite=${TOPE_DE_WEBHOOKS}`)),
      ])
      return filasDeWebhooks(pagos, mensajeria)
    },
  }
}
