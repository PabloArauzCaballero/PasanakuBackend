import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom, type Observable } from 'rxjs'
import type {
  AlertaEnBandeja,
  CasoEnBandeja,
  EntradaCasoLft,
  EntradaDecisionCasoLft,
  FichaDeCasoLft,
  SalidaCasoLft,
  SalidaDecisionCasoLft,
} from 'clientes/angular/cumplimiento'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { pedirPagina, type FormaDePagina } from './paginas'

/**
 * CU-44 · bandejas de alertas y casos LFT y sus dos acciones (`cumplimiento.yaml`:
 * `listarBandejaDeAlertas`, `listarBandejaDeCasos`, `abrirCasoLft`, `decidirCasoLft`) y la ficha del caso
 * (`obtenerCasoLft`). Deber de reserva: las bandejas no traen persona, monto ni detalle; la ficha trae la alerta de
 * origen y el titular SOLO enmascarado (`****` + cuatro caracteres del seudónimo), y esta capa no pide más.
 */
const FORMA_ALERTAS: FormaDePagina = { parametroFecha: 'despuesDeFecha', parametroId: 'despuesDeId', campoFecha: 'proximoFecha', campoId: 'proximoId', multiplesConComa: true }
const FORMA_CASOS: FormaDePagina = { parametroFecha: 'despuesDePlazo', parametroId: 'despuesDeId', campoFecha: 'proximoPlazo', campoId: 'proximoId', multiplesConComa: true }

export function cargadorDeAlertas(): CargadorDePagina<AlertaEnBandeja> {
  const url = `${inject(GATEWAY)}/cumplimiento/alertas`
  return cargadorPorCursor(pedirPagina<AlertaEnBandeja>(inject(HttpClient), url, FORMA_ALERTAS, ['estados', 'severidades']))
}

export function cargadorDeCasos(): CargadorDePagina<CasoEnBandeja> {
  const url = `${inject(GATEWAY)}/cumplimiento/casos`
  return cargadorPorCursor(pedirPagina<CasoEnBandeja>(inject(HttpClient), url, FORMA_CASOS, ['estados']))
}

export type { FichaDeCasoLft }

/** La ficha de un caso (`GET /cumplimiento/casos/{casoId}`, CUMPLIMIENTO_CASOS, igual que la bandeja). */
export function crearConsultarCaso(): (casoId: string) => Observable<FichaDeCasoLft> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (casoId) => http.get<FichaDeCasoLft>(`${gateway}/cumplimiento/casos/${encodeURIComponent(casoId)}`)
}

/** Las dos escrituras. La clave de idempotencia la gobierna el contenido (`contextoIdempotente`). */
export function accionesDeCasos() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    abrir: (entrada: EntradaCasoLft) =>
      firstValueFrom(http.post<SalidaCasoLft>(`${gateway}/cumplimiento/casos`, entrada, { context: contextoIdempotente() })),
    decidir: (casoId: string, entrada: EntradaDecisionCasoLft) =>
      firstValueFrom(
        http.post<SalidaDecisionCasoLft>(`${gateway}/cumplimiento/casos/${encodeURIComponent(casoId)}/decision`, entrada, {
          context: contextoIdempotente(),
        }),
      ),
  }
}
