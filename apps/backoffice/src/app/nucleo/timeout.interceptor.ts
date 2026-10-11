import { HttpInterceptorFn } from '@angular/common/http'
import { catchError, throwError, timeout, TimeoutError } from 'rxjs'
import { mensajeDeTiempoAgotado, type ErrorTraducido } from './errores'

/**
 * H12.S1.M3 · ninguna llamada espera para siempre. Una lectura que no responde deja la pantalla en «cargando» sin salida;
 * una escritura sin respuesta deja a la persona sin saber si pasó. Al vencer el plazo la petición se cancela en el navegador y
 * sale un error ya traducido con `tiempoAgotado`: para una escritura significa «sin confirmar» (pudo haber llegado), nunca «falló».
 *
 * Los plazos son una decisión de este cliente, no un contrato: el gateway tiene el suyo y esto solo evita colgarse más allá.
 * Escritura más larga que lectura porque puede esperar un asiento del libro.
 */
export const PLAZO_DE_LECTURA_MS = 15_000
export const PLAZO_DE_ESCRITURA_MS = 30_000

export const timeoutInterceptor: HttpInterceptorFn = (req, next) => {
  const escritura = req.method !== 'GET' && req.method !== 'HEAD'
  return next(req).pipe(
    timeout(escritura ? PLAZO_DE_ESCRITURA_MS : PLAZO_DE_LECTURA_MS),
    catchError((error: unknown) => {
      if (!(error instanceof TimeoutError)) return throwError(() => error)
      const traducido: ErrorTraducido = { mensaje: mensajeDeTiempoAgotado(escritura), estado: 0, sinConexion: false, tiempoAgotado: true }
      return throwError(() => traducido)
    }),
  )
}
