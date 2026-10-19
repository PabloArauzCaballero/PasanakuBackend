import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../nucleo/gateway'

/**
 * CU-04 · cerrar la sesión. Contrato real: `DELETE /sesion` (`cerrarSesion`), que revoca
 * la sesión del token y toda su familia de refresh. Es idempotente y no recibe ningún
 * identificador del cliente: no hay forma de cerrar la sesión de otro.
 */
export function crearCerrarSesion(): () => Observable<unknown> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return () => http.delete(`${gateway}/sesion`)
}

/** `ADMIN_PLATAFORMA` → «Admin plataforma». El código crudo del rol no es para personas. */
export function rolLegible(codigo: string): string {
  const texto = codigo.toLowerCase().replace(/_/g, ' ').replace('admin ', 'administración de ').trim()
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}
