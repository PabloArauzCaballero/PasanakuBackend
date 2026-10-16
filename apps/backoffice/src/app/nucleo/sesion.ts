import { computed, Injectable, signal } from '@angular/core'
import { alcanza } from './secciones'

/**
 * `UNKNOWN` es el único estado de arranque: todavía no se intentó el refresco. `RESTORING`
 * es mientras `AuthBootstrap` (H3.S2) espera esa respuesta. Desde ahí se resuelve a
 * `AUTHENTICATED`, `ANONYMOUS` o `ERROR`. Un login manual (`abrir`) o un logout (`cerrar`)
 * valen desde cualquier estado: son eventos autoritativos del servidor, no parte de la
 * secuencia de arranque.
 */
export type EstadoSesion = 'UNKNOWN' | 'RESTORING' | 'AUTHENTICATED' | 'ANONYMOUS' | 'ERROR'

/** A qué estados puede pasar `restaurando`/`anonima`/`fallo` desde cada estado actual. */
const TRANSICIONES_DE_ARRANQUE: Readonly<Record<EstadoSesion, ReadonlySet<EstadoSesion>>> = {
  UNKNOWN: new Set(['RESTORING']),
  RESTORING: new Set(['ANONYMOUS', 'ERROR']),
  ANONYMOUS: new Set(['RESTORING']),
  ERROR: new Set(['RESTORING']),
  AUTHENTICATED: new Set([]),
}

/**
 * El token del operador vive SOLO en memoria; el refresco, en cookie HttpOnly que el
 * gateway maneja. Un token en `localStorage` lo lee cualquier script que llegue a la
 * página, y el backoffice mira expedientes con datos de personas.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly acceso = signal<string | null>(null)
  readonly permisos = signal<readonly string[]>([])
  readonly rol = signal<string | null>(null)
  /**
   * El `sub` del token: quién es, para el servidor. No se muestra en pantalla; lo usa
   * `AlmacenLocal` para que dos operadores en la misma máquina no compartan el avance
   * de los tutoriales, y viaja siempre por una huella, nunca en claro.
   */
  readonly sujeto = signal<string | null>(null)

  private readonly estadoInterno = signal<EstadoSesion>('UNKNOWN')
  readonly estado = this.estadoInterno.asReadonly()

  /** Hay operador con sesión: es lo que separa el login del resto del backoffice. */
  readonly abierta = computed(() => this.estadoInterno() === 'AUTHENTICATED')

  token(): string | null {
    return this.acceso()
  }

  /** Login (manual o por refresco exitoso): vale desde cualquier estado. */
  abrir(acceso: string, permisos: readonly string[], rol: string, sujeto: string | null = null): void {
    this.acceso.set(acceso)
    this.permisos.set(permisos)
    this.rol.set(rol)
    this.sujeto.set(sujeto)
    this.estadoInterno.set('AUTHENTICATED')
  }

  /** Logout: vale desde cualquier estado. */
  cerrar(): void {
    this.acceso.set(null)
    this.permisos.set([])
    this.rol.set(null)
    this.sujeto.set(null)
    this.estadoInterno.set('ANONYMOUS')
  }

  /** `AuthBootstrap` empieza a intentar el refresco. Rechaza si ya hay un intento en curso. */
  restaurando(): void {
    this.transicionarDeArranque('RESTORING')
  }

  /** `AuthBootstrap` concluyó: la cookie de refresco no vale (401) o no había ninguna. */
  anonima(): void {
    this.transicionarDeArranque('ANONYMOUS')
  }

  /** `AuthBootstrap` no pudo determinar el estado (servicio caído, timeout). */
  fallo(): void {
    this.transicionarDeArranque('ERROR')
  }

  private transicionarDeArranque(destino: EstadoSesion): void {
    const actual = this.estadoInterno()
    if (!TRANSICIONES_DE_ARRANQUE[actual].has(destino)) {
      throw new Error(`Sesion: transición de arranque inválida ${actual} → ${destino}`)
    }
    this.estadoInterno.set(destino)
  }

  /**
   * Abre la sesión con el token que devuelve `POST /sesiones` (CU-04). Los permisos y el
   * rol se leen de sus claims **solo para mostrar u ocultar**: la firma la verifica el
   * gateway en cada petición, y un token manipulado aquí no abre nada del lado servidor.
   */
  abrirConToken(tokenAcceso: string): void {
    const claims = leerClaims(tokenAcceso)
    const permisos = Array.isArray(claims['permisos']) ? (claims['permisos'] as unknown[]).filter((p): p is string => typeof p === 'string') : []
    const rol = typeof claims['rol'] === 'string' ? claims['rol'] : ''
    const sujeto = typeof claims['sub'] === 'string' ? claims['sub'] : null
    this.abrir(tokenAcceso, permisos, rol, sujeto)
  }

  puede(permiso: string): boolean {
    return alcanza(permiso, this.permisos(), this.abierta())
  }
}

/** El payload de un JWT, sin verificar la firma (eso es trabajo del servidor). */
function leerClaims(token: string): Record<string, unknown> {
  const payload = token.split('.')[1]
  if (!payload) return {}
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=')
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes))
    return claims !== null && typeof claims === 'object' ? (claims as Record<string, unknown>) : {}
  } catch {
    return {}
  }
}
