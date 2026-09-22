import { HttpClient, HttpContext, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'
import type { SalidaConsumo } from 'clientes/angular/publicidad/model/salidaConsumo'
import type { EntradaLiquidacion } from 'clientes/angular/publicidad/model/entradaLiquidacion'
import type { SalidaLiquidacion } from 'clientes/angular/publicidad/model/salidaLiquidacion'

/**
 * CU-113 (desempeño) y CU-114 (liquidación) comparten esta única función de carga a
 * propósito: es el gate central del carril («el desempeño mostrado cuadra con lo
 * facturado en CU-114» — `planes/18` ficha F14). `GET
 * /publicidad/cuentas/{cuentaId}/consumo` es lo que la propia definición del contrato
 * dice que Contabilidad mira antes de liquidar («es lo que mira Contabilidad antes de
 * correr la liquidación»), así que es también lo correcto para mostrarle al operador
 * en el panel de desempeño: **un solo origen de dato para las dos pantallas**, nunca
 * dos cálculos separados. `pantalla-de-desempeno.ts` y `pantalla-de-liquidacion.ts`
 * importan las dos exclusivamente de acá.
 */
export function recursoDeConsumo(cuentaId: () => string | undefined, periodo: () => string | undefined) {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaConsumo>(() => (cuentaId() && periodo() ? `${gateway}/publicidad/cuentas/${cuentaId()}/consumo?periodo=${periodo()}` : undefined))
}

export async function consultarConsumo(http: HttpClient, gateway: string, cuentaId: string, periodo: string): Promise<SalidaConsumo> {
  return firstValueFrom(http.get<SalidaConsumo>(`${gateway}/publicidad/cuentas/${cuentaId}/consumo`, { params: { periodo } }))
}

export async function liquidarPeriodo(http: HttpClient, gateway: string, cuentaId: string, entrada: EntradaLiquidacion): Promise<SalidaLiquidacion> {
  return firstValueFrom(
    http.post<SalidaLiquidacion>(`${gateway}/publicidad/cuentas/${cuentaId}/liquidaciones`, entrada, {
      context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()),
    }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/`; la pantalla de liquidación llama a esto en contexto de inyección. */
export function accionesDeLiquidacion() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { liquidar: (cuentaId: string, entrada: EntradaLiquidacion) => liquidarPeriodo(http, gateway, cuentaId, entrada) }
}
