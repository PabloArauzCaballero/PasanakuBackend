import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, switchMap, throwError } from 'rxjs'
import { HttpClient } from '@angular/common/http'
import { GATEWAY } from './gateway'
import { Sesion } from './sesion'

/** Marca la petición de refresco y el reintento, para no entrar en bucle. */
export const YA_REINTENTADA = new HttpContextToken<boolean>(() => false)

/**
 * Bearer desde memoria; ante un `401`, **un** refresco y **un** reintento. Si vuelve a
 * fallar, sesión cerrada global. Reintentar en bucle contra un token muerto es un
 * backoffice trabado.
 */
export const sesionInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(Sesion)
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  const token = sesion.token()
  const conToken = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req

  return next(conToken).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || req.context.get(YA_REINTENTADA)) {
        return throwError(() => error)
      }
      return http
        .post<{ acceso: string; permisos?: string[]; rol?: string }>(`${gateway}/sesion/refrescar`, {}, {
          withCredentials: true,
          context: req.context.set(YA_REINTENTADA, true),
        })
        .pipe(
          switchMap((nueva) => {
            sesion.abrir(nueva.acceso, nueva.permisos ?? sesion.permisos(), nueva.rol ?? sesion.rol() ?? '')
            const reintento = req.clone({
              setHeaders: { Authorization: `Bearer ${nueva.acceso}` },
              context: req.context.set(YA_REINTENTADA, true),
            })
            return next(reintento)
          }),
          catchError((segundo: unknown) => {
            sesion.cerrar()
            return throwError(() => segundo)
          }),
        )
    }),
  )
}
