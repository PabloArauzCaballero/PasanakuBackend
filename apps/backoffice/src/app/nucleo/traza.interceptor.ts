import { HttpInterceptorFn } from '@angular/common/http'

/** `x-request-id` en cada petición: sin esto «me falló el reverso» no se puede seguir. */
export const trazaInterceptor: HttpInterceptorFn = (req, next) =>
  next(req.headers.has('x-request-id') ? req : req.clone({ setHeaders: { 'x-request-id': nuevaTraza() } }))

export function nuevaTraza(): string {
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}
