import { HttpContext, HttpContextToken, HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http'
import { tap } from 'rxjs'

/**
 * La clave viaja por el contexto de la petición: la genera el formulario al abrirse
 * (`claveDeIdempotencia()`) y se reenvía igual en el reintento. Nunca se regenera acá.
 */
export const CLAVE_IDEMPOTENCIA = new HttpContextToken<string | null>(() => null)

/**
 * H12.S1.M3 · la clave la gobierna el CONTENIDO de la operación (método + URL + cuerpo), no cada llamada.
 *
 * Antes cada operación de dinero generaba una clave nueva por intento («nueva por intento»): si la respuesta se perdía
 * (timeout, corte, 5xx) y la persona volvía a pulsar, el reintento salía con OTRA clave y el servidor lo trataba como
 * una operación distinta —cobrar, pagar o cerrar dos veces—. Con esta marca el interceptor reutiliza la misma clave para
 * la misma operación hasta que haya una respuesta DEFINITIVA, y recién entonces la olvida.
 */
export const CLAVE_POR_CONTENIDO = new HttpContextToken<boolean>(() => false)

/** Contexto para una operación con efecto cuya clave la gobierna su contenido. */
export const contextoIdempotente = (): HttpContext => new HttpContext().set(CLAVE_POR_CONTENIDO, true)

/** Se llama UNA vez por formulario, al abrirlo. El contrato la exige con forma UUID. */
export function claveDeIdempotencia(): string {
  return crypto.randomUUID()
}

/**
 * Cuánto se recuerda una clave sin respuesta definitiva. Pasado ese tiempo, repetir el mismo contenido se toma como una
 * operación nueva a propósito (dos cobros iguales, dos días distintos): la persona ya tuvo tiempo de verificar.
 * Es una decisión de este cliente, no un contrato del servidor: el servidor decide por su cuenta cuánto conserva la clave.
 */
export const VIGENCIA_DE_CLAVE_MS = 15 * 60_000

type Intento = { clave: string; desde: number }
const intentos = new Map<string, Intento>()

/** Para pruebas: olvida todo lo recordado. */
export function olvidarIntentos(): void {
  intentos.clear()
}

function canonico(valor: unknown): string {
  if (valor === null || typeof valor !== 'object') return JSON.stringify(valor ?? null)
  if (Array.isArray(valor)) return `[${valor.map(canonico).join(',')}]`
  const objeto = valor as Record<string, unknown>
  return `{${Object.keys(objeto)
    .sort()
    .map((k) => `${JSON.stringify(k)}:${canonico(objeto[k])}`)
    .join(',')}}`
}

/** ¿La respuesta cierra el intento? Un rechazo del servidor (4xx) es definitivo; no saber qué pasó (0, 5xx, 408, 425, 429) no lo es. */
function esDefinitiva(estado: number): boolean {
  return estado >= 400 && estado < 500 && ![408, 425, 429].includes(estado)
}

export const idempotenciaInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.context.get(CLAVE_POR_CONTENIDO)) {
    const huella = `${req.method} ${req.urlWithParams} ${canonico(req.body)}`
    const ahora = Date.now()
    let intento = intentos.get(huella)
    if (!intento || ahora - intento.desde > VIGENCIA_DE_CLAVE_MS) {
      intento = { clave: claveDeIdempotencia(), desde: ahora }
      intentos.set(huella, intento)
    }
    return next(req.clone({ setHeaders: { 'Idempotency-Key': intento.clave } })).pipe(
      tap({
        next: (evento) => {
          if (evento instanceof HttpResponse) intentos.delete(huella)
        },
        error: (error: unknown) => {
          // El interceptor de errores corre más adentro y entrega el error ya traducido (`estado`); sin él, el HttpErrorResponse crudo.
          const estado = error instanceof HttpErrorResponse ? error.status : ((error as { estado?: number } | null)?.estado ?? 0)
          if (esDefinitiva(estado)) intentos.delete(huella)
        },
      }),
    )
  }
  const clave = req.context.get(CLAVE_IDEMPOTENCIA)
  return next(clave ? req.clone({ setHeaders: { 'Idempotency-Key': clave } }) : req)
}
