import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import type { ErrorTraducido } from './errores'
import { GATEWAY } from './gateway'
import { contextoIdempotente } from './idempotencia.interceptor'
import { INTERCEPTORES_DE_RED } from './interceptores'

/** Montaje compartido de las pruebas de la cadena de red real (`INTERCEPTORES_DE_RED`). */
export const GW = 'https://gw.test/api/v1'
export const COBRO = `${GW}/erp/cuentas-por-cobrar/c1/cobros`

export function montar() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors(INTERCEPTORES_DE_RED)), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
  })
  return { http: TestBed.inject(HttpClient), backend: TestBed.inject(HttpTestingController) }
}
export const cobrar = (http: HttpClient, monto = '100.00') => http.post(COBRO, { monto, formaCobro: 'QR' }, { context: contextoIdempotente() })
export const fallo = (p: Promise<unknown>) =>
  p.then(
    () => null,
    (e: ErrorTraducido) => e,
  )
