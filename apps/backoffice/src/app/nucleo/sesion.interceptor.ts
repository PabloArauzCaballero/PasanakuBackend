import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, switchMap, tap, throwError } from 'rxjs'
import { RefrescoDeSesion } from './refresco-de-sesion'
import { Sesion } from './sesion'

/** Marca la petición de refresco y el reintento, para no entrar en bucle. */
export const YA_REINTENTADA = new HttpContextToken<boolean>(() => false)

/**
 * Bearer desde memoria; ante un `401`, **un** refresco compartido (`RefrescoDeSesion`,
 * single-flight) y **un** reintento por petición. Si el refresco falla, sesión cerrada.
 * Reintentar en bucle contra un token muerto es un backoffice trabado.
 */
export const sesionInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(Sesion)
  const refresco = inject(RefrescoDeSesion)
  const token = sesion.token()
  const conToken = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req

  return next(conToken).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || req.context.get(YA_REINTENTADA)) {
        return throwError(() => error)
      }
      return refresco.refrescar().pipe(
        tap((nueva) => sesion.abrir(nueva.acceso, nueva.permisos ?? sesion.permisos(), nueva.rol ?? sesion.rol() ?? '')),
        // Si el refresco mismo falla (401/5xx/red), la petición ve su 401 ORIGINAL, no el
        // error interno del refresco: quien la llamó no tiene por qué saber que hubo un
        // intento de refresco, y "su error original" es lo que dice el CA del hito.
        catchError(() => {
          sesion.cerrar()
          return throwError(() => error)
        }),
        switchMap((nueva) => {
          const reintento = req.clone({
            setHeaders: { Authorization: `Bearer ${nueva.acceso}` },
            context: req.context.set(YA_REINTENTADA, true),
          })
          return next(reintento)
        }),
      )
    }),
  )
}
