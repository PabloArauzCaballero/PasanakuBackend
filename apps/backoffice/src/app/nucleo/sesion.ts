import { computed, Injectable, signal } from '@angular/core'
import { alcanza } from './secciones'

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

  /** Hay operador con sesión: es lo que separa el login del resto del backoffice. */
  readonly abierta = computed(() => this.acceso() !== null)

  token(): string | null {
    return this.acceso()
  }

  abrir(acceso: string, permisos: readonly string[], rol: string): void {
    this.acceso.set(acceso)
    this.permisos.set(permisos)
    this.rol.set(rol)
  }

  cerrar(): void {
    this.acceso.set(null)
    this.permisos.set([])
    this.rol.set(null)
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
    this.abrir(tokenAcceso, permisos, rol)
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
