import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http'
import { catchError, throwError } from 'rxjs'
import { mensajeDe, type ErrorTraducido } from './errores'

/**
 * Todo error sale de acá ya traducido: la pantalla nunca ve el mensaje del backend ni
 * el código crudo. Un error de red (estado 0) no llegó al backend y no lleva traza.
 */
export const erroresInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error)
      const cuerpo = (error.error ?? {}) as Partial<{ codigo: string; trazaId: string }>
      const traducido: ErrorTraducido = {
        mensaje: mensajeDe(cuerpo.codigo, error.status),
        codigo: cuerpo.codigo,
        estado: error.status,
        sinConexion: error.status === 0,
        trazaId: error.status === 0 ? undefined : cuerpo.trazaId ?? req.headers.get('x-request-id') ?? undefined,
      }
      return throwError(() => traducido)
    }),
  )
