import { Injectable, signal } from '@angular/core'

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

  puede(permiso: string): boolean {
    return this.permisos().includes(permiso)
  }
}
