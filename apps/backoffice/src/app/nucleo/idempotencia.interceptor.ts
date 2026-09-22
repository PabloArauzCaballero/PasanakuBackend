import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http'

/**
 * La clave viaja por el contexto de la petición: la genera el formulario al abrirse
 * (`claveDeIdempotencia()`) y se reenvía igual en el reintento. Nunca se regenera acá.
 */
export const CLAVE_IDEMPOTENCIA = new HttpContextToken<string | null>(() => null)

export const idempotenciaInterceptor: HttpInterceptorFn = (req, next) => {
  const clave = req.context.get(CLAVE_IDEMPOTENCIA)
  return next(clave ? req.clone({ setHeaders: { 'Idempotency-Key': clave } }) : req)
}

/** Se llama UNA vez por formulario, al abrirlo. El contrato la exige con forma UUID. */
export function claveDeIdempotencia(): string {
  return crypto.randomUUID()
}
