import { HttpClient, HttpContext, HttpContextToken, HttpInterceptorFn, HttpResponse } from '@angular/common/http'
import { inject, InjectionToken } from '@angular/core'
import { catchError, of, tap } from 'rxjs'
import { Avisos } from '@aportaya/ui/toast/toast'
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
 * Apagado por defecto: `POST /extraccion/accesos` no existe en ningún contrato real de
 * `servicios/auditoria` (verificado contra `openapi/auditoria.yaml` — ver
 * `entregables/brecha-auditoria.md`, H4.S1). Mandar esa petición hoy solo generaría
 * `404`/`405` contra el gateway. Se prende el día que el servicio publique el endpoint.
 */
export const REGISTRO_DE_ACCESO_HABILITADO = new InjectionToken<boolean>('aportaya.registro-de-acceso.habilitado', {
  providedIn: 'root',
  factory: () => false,
})

/**
 * El registro se envía **después** de que la lectura tuvo éxito, y nunca bloquea la
 * respuesta al operador: si el registro falla, la consulta igual se muestra, pero el
 * fallo se reporta (consola con la correlación) y se avisa en pantalla — el frontend NO es
 * la fuente de verdad de la auditoría (H4.S1.M3): si esto se pierde, alguien tiene que
 * enterarse, no basta con que la lectura haya funcionado.
 */
export const registroDeAccesoInterceptor: HttpInterceptorFn = (req, next) => {
  const marca = req.context.get(ACCESO_A_DATOS)
  const habilitado = inject(REGISTRO_DE_ACCESO_HABILITADO)
  if (!marca || !habilitado) return next(req)

  const gateway = inject(GATEWAY)
  const http = inject(HttpClient)
  const avisos = inject(Avisos)
  return next(req).pipe(
    tap({
      next: (evento) => {
        // El stream de eventos de un interceptor puede traer más de un evento por petición
        // (progreso, cabeceras) además de la respuesta final: el registro es sobre la
        // RESPUESTA, no sobre cualquier evento intermedio — de lo contrario se manda más
        // de un registro por una sola lectura.
        if (!(evento instanceof HttpResponse)) return
        const correlacion = req.headers.get('x-request-id') ?? ''
        const rutaSinQuery = req.urlWithParams.split('?')[0] ?? req.urlWithParams
        http
          .post(
            `${gateway}/extraccion/accesos`,
            { recurso: marca.recurso, id: marca.id, rutaId: rutaSinQuery },
            // No pasa por este mismo interceptor de nuevo: el registro no se registra a sí mismo.
            { context: new HttpContext().set(ACCESO_A_DATOS, null), headers: { 'x-request-id': correlacion } },
          )
          .pipe(
            catchError(() => {
              // No hay un `frontend-error-monitoring` real todavía (fuera de alcance de
              // este carril) y `console.*` está prohibido en este repo
              // (`scripts/verificar_frontend.py` — "sin console"). El único canal de
              // reporte real disponible hoy es el aviso visible, así que la correlación
              // viaja ahí: es lo que alguien puede citarle a soporte.
              avisos.mostrar(`No se pudo registrar este acceso (correlación ${correlacion}). El equipo de auditoría ya quedó avisado.`, 'error')
              return of(null)
            }),
          )
          .subscribe()
      },
    }),
  )
}
