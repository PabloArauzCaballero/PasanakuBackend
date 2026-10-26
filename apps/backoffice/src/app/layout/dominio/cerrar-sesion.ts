import { HttpClient, HttpContext } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../nucleo/gateway'
import { YA_REINTENTADA } from '../../nucleo/sesion.interceptor'

/**
 * CU-04 · cerrar la sesión. Contrato real: `DELETE /sesion` (`cerrarSesion`), que revoca
 * la sesión del token y toda su familia de refresh. Es idempotente y no recibe ningún
 * identificador del cliente: no hay forma de cerrar la sesión de otro.
 *
 * Marcada `YA_REINTENTADA`: un `401` al salir quiere decir que la sesión ya no vale, y refrescarla solo para cerrarla
 * emitía un token nuevo antes de revocarlo (el recorrido en navegador mostraba `DELETE 401` + `refrescar 401`).
 */
export function crearCerrarSesion(): () => Observable<unknown> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return () => http.delete(`${gateway}/sesion`, { context: new HttpContext().set(YA_REINTENTADA, true) })
}

/** `ADMIN_PLATAFORMA` → «Admin plataforma». El código crudo del rol no es para personas. */
export function rolLegible(codigo: string): string {
  const texto = codigo.toLowerCase().replace(/_/g, ' ').replace('admin ', 'administración de ').trim()
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}
