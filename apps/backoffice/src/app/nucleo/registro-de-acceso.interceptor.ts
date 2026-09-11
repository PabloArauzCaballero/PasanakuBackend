import { HttpClient, HttpContext, HttpContextToken, HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, of, tap } from 'rxjs'
import { GATEWAY } from './gateway'

/**
 * Marca una petición como «vista datos personales» (CU-58, `R-SEG-02`). La pantalla que
 * muestra un expediente pone `context: new HttpContext().set(ACCESO_A_DATOS, { recurso, id })`
 * en su petición de lectura; este interceptor no decide **qué** es sensible, solo deja
 * la huella cuando la pantalla lo declaró.
 */
export type AccesoADatos = { recurso: string; id: string } | null
export const ACCESO_A_DATOS = new HttpContextToken<AccesoADatos>(() => null)

/**
 * El registro se envía **después** de que la lectura tuvo éxito, y nunca bloquea la
 * respuesta al operador: si el registro falla, la consulta igual se muestra, pero queda
 * la traza de que el intento de registrar se perdió (consola, en desarrollo).
 */
export const registroDeAccesoInterceptor: HttpInterceptorFn = (req, next) => {
  const marca = req.context.get(ACCESO_A_DATOS)
  if (!marca) return next(req)

  const gateway = inject(GATEWAY)
  const http = inject(HttpClient)
  return next(req).pipe(
    tap({
      next: () => {
        http
          .post(
            `${gateway}/extraccion/accesos`,
            { recurso: marca.recurso, id: marca.id, rutaId: req.urlWithParams },
            // No pasa por este mismo interceptor de nuevo: el registro no se registra a sí mismo.
            { context: new HttpContext().set(ACCESO_A_DATOS, null), headers: { 'x-request-id': req.headers.get('x-request-id') ?? '' } },
          )
          // Si el registro falla, la lectura ya se mostró y no se reintenta desde acá:
          // reintentar en bucle un registro de auditoría no vale bloquear al operador.
          .pipe(catchError(() => of(null)))
          .subscribe()
      },
    }),
  )
}
