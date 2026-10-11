import type { HttpInterceptorFn } from '@angular/common/http'
import { erroresInterceptor } from './errores.interceptor'
import { idempotenciaInterceptor } from './idempotencia.interceptor'
import { registroDeAccesoInterceptor } from './registro-de-acceso.interceptor'
import { sesionInterceptor } from './sesion.interceptor'
import { timeoutInterceptor } from './timeout.interceptor'
import { trazaInterceptor } from './traza.interceptor'

/**
 * La cadena de red del backoffice, en el orden en que se atraviesa HACIA el servidor (la respuesta vuelve en el orden inverso).
 * Un solo lugar, para que la prueba de la cadena ejercite EXACTAMENTE la que corre en producción.
 *
 * - `traza` primero: el identificador viaja en la petición que ven todos los de adentro.
 * - `errores` inmediatamente después, o sea más AFUERA que `sesion`: traduce el error final que ve la pantalla. Antes iba el último
 *   (pegado al servidor) y convertía el 401 en un objeto traducido ANTES de que `sesion` lo viera, y `sesion` solo refresca ante un
 *   `HttpErrorResponse`: el refresco de sesión por 401 nunca se disparaba en la aplicación real.
 * - `idempotencia` y `timeout`: la clave por contenido se decide antes de plazo; el plazo vencido sale como error traducido.
 */
export const INTERCEPTORES_DE_RED: HttpInterceptorFn[] = [
  trazaInterceptor,
  erroresInterceptor,
  sesionInterceptor,
  idempotenciaInterceptor,
  timeoutInterceptor,
  registroDeAccesoInterceptor,
]
