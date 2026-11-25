import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { map, switchMap, type Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

/**
 * Segundo factor de un acto de gobierno (step-up, `docs/auditoria-produccion/contratos/step-up-jwt.md`): dos pasos
 * contra identidad —abrir un desafío con propósito `ADMIN` y presentar el código TOTP— que devuelven la **evidencia**
 * (un JWT de un solo uso y de minutos de vida) que la operación lleva como `evidenciaMfa`. El código nunca se guarda ni
 * se registra: viaja una vez y se descarta.
 */
export const LARGO_DEL_CODIGO = 6
export const esCodigoValido = (codigo: string): boolean => new RegExp(`^[0-9]{${LARGO_DEL_CODIGO}}$`).test(codigo)

type SalidaDesafio = { desafioId: string; expiraEn: string }
type SalidaEvidencia = { evidencia: string; expiraEn: string; jti: string }

export function obtenerEvidenciaDeStepUp(http: HttpClient, gateway: string, codigo: string): Observable<string> {
  return http.post<SalidaDesafio>(`${gateway}/sesiones/desafios`, { proposito: 'ADMIN' }, { context: contextoIdempotente() }).pipe(
    switchMap((d) => http.post<SalidaEvidencia>(`${gateway}/sesiones/desafios/${d.desafioId}/verificacion`, { factor: { tipo: 'TOTP', valor: codigo } })),
    map((s) => s.evidencia),
  )
}

/** `(codigo) => evidencia`, listo para inyectar. */
export function pasoDeSeguridad() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (codigo: string) => obtenerEvidenciaDeStepUp(http, gateway, codigo)
}
