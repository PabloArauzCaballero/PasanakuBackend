import { HttpInterceptorFn } from '@angular/common/http'
import { catchError, throwError, timeout, TimeoutError } from 'rxjs'
import { mensajeDeTiempoAgotado, type ErrorTraducido } from './errores'

/**
 * H12.S1.M3 · el sitio solo LEE (verificadores y datos públicos): una lectura que no responde dejaba la pantalla en «cargando»
 * sin salida. Vencido el plazo la petición se cancela y sale un error ya traducido con `tiempoAgotado`, que las pantallas muestran
 * con su botón de reintento. Mismo plazo y misma forma que el del backoffice (`apps/backoffice/src/app/nucleo/timeout.interceptor.ts`).
 */
export const PLAZO_DE_LECTURA_MS = 15_000

export const timeoutInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    timeout(PLAZO_DE_LECTURA_MS),
    catchError((error: unknown) => {
      if (!(error instanceof TimeoutError)) return throwError(() => error)
      const traducido: ErrorTraducido = { mensaje: mensajeDeTiempoAgotado(), estado: 0, sinConexion: false, tiempoAgotado: true }
      return throwError(() => traducido)
    }),
  )
