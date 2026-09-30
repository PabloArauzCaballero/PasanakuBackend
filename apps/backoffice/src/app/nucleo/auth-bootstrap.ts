import { HttpErrorResponse } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, firstValueFrom, map, of, timeout } from 'rxjs'
import { RefrescoDeSesion } from './refresco-de-sesion'
import { Sesion } from './sesion'

/** Cuánto se espera la respuesta del refresco antes de rendirse con `ERROR`. */
export const TIMEOUT_ARRANQUE_MS = 5000

type Resultado = { ok: true; acceso: string; permisos: string[]; rol: string } | { ok: false; error: unknown }

/**
 * Intenta el refresco con la cookie de sesión. Reutiliza `RefrescoDeSesion` (mismo
 * single-flight que `sesionInterceptor`, H2) para que un `401` que llegara de otro lado
 * mientras esto corre no dispare un segundo `POST /sesion/refrescar`; la diferencia con H2
 * es la semántica de fallo: acá un `401` (cookie inválida o ausente) es `ANONYMOUS`, no un
 * error — recién un error real de `identidad` (5xx, red, timeout) pasa a `ERROR`.
 *
 * Toma `sesion`/`refresco` por parámetro (no `inject()` propio) para poder llamarse tanto
 * desde `provideAppInitializer` (H3.S2) como desde el botón «Reintentar» de
 * `RestaurandoSesion` (H3.S3) — un método de componente NO es contexto de inyección.
 */
export async function restaurarSesion(sesion: Sesion, refresco: RefrescoDeSesion): Promise<void> {
  sesion.restaurando()

  const resultado = await firstValueFrom(
    refresco.refrescar().pipe(
      timeout(TIMEOUT_ARRANQUE_MS),
      map((nueva): Resultado => ({ ok: true, acceso: nueva.acceso, permisos: nueva.permisos ?? [], rol: nueva.rol ?? '' })),
      catchError((error: unknown) => of<Resultado>({ ok: false, error })),
    ),
  )

  if (resultado.ok) {
    sesion.abrir(resultado.acceso, resultado.permisos, resultado.rol)
    return
  }

  if (resultado.error instanceof HttpErrorResponse && resultado.error.status === 401) {
    sesion.anonima()
    return
  }

  sesion.fallo()
}

/** `provideAppInitializer(inicializarSesion())`: antes de montar cualquier ruta. */
export function inicializarSesion(): () => Promise<void> {
  return () => {
    const sesion = inject(Sesion)
    const refresco = inject(RefrescoDeSesion)
    return restaurarSesion(sesion, refresco)
  }
}
