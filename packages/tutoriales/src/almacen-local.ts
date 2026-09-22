import { Injectable, inject } from '@angular/core'
import { Observable, of, throwError } from 'rxjs'
import { IDENTIDAD_DE_TUTORIALES } from './identidad'
import type { AlmacenDeProgreso } from './almacen'
import type { ProgresoDeTutorial } from './tipos'

/** Una clave por operador: dos personas en la misma máquina no comparten avance. */
const PREFIJO = 'aportaya.tutoriales.'

/**
 * **El respaldo local del progreso.** Guarda en `localStorage`, bajo una clave derivada
 * de quién está mirando ([[IdentidadDeQuienAprende]]).
 *
 * Acá **no va ningún token** (regla de `sesion.ts`: el acceso vive solo en memoria) ni
 * el identificador del operador en claro: la clave lleva una huella corta y no
 * reversible del sujeto del JWT. Lo que se guarda es qué tutorial se hizo y por qué
 * paso se iba — nada de eso es un dato personal, pero saber *quién* sí lo sería.
 *
 * Es el adaptador por omisión mientras el backend no tenga dónde guardar esto. Su
 * límite es real y está declarado: el avance no viaja a otra máquina.
 */
@Injectable()
export class AlmacenLocal implements AlmacenDeProgreso {
  private readonly identidad = inject(IDENTIDAD_DE_TUTORIALES)

  leer(): Observable<readonly ProgresoDeTutorial[]> {
    return of(this.leerCrudo())
  }

  guardar(progreso: ProgresoDeTutorial): Observable<ProgresoDeTutorial> {
    const resto = this.leerCrudo().filter((p) => p.tutorialId !== progreso.tutorialId)
    return this.escribir([...resto, progreso], progreso)
  }

  reiniciar(tutorialId: string): Observable<void> {
    const resto = this.leerCrudo().filter((p) => p.tutorialId !== tutorialId)
    return this.escribir(resto, undefined)
  }

  private escribir<T>(filas: readonly ProgresoDeTutorial[], devolver: T): Observable<T> {
    const almacen = almacenDelNavegador()
    if (almacen === null) return throwError(() => new Error(SIN_ALMACEN))
    try {
      almacen.setItem(this.clave(), JSON.stringify(filas))
    } catch {
      // Cuota llena o almacenamiento bloqueado por política del navegador. No se
      // traga: el motor lo muestra como «no pudimos guardar tu avance» y sigue
      // funcionando en memoria, que es peor que guardar pero mejor que trabarse.
      return throwError(() => new Error(SIN_ALMACEN))
    }
    return of(devolver)
  }

  private leerCrudo(): readonly ProgresoDeTutorial[] {
    const almacen = almacenDelNavegador()
    if (almacen === null) return []
    const crudo = almacen.getItem(this.clave())
    if (crudo === null) return []
    try {
      const leido: unknown = JSON.parse(crudo)
      return Array.isArray(leido) ? leido.filter(esProgreso) : []
    } catch {
      // Alguien editó la clave a mano o quedó a medias. Se descarta lo ilegible y se
      // empieza de nuevo: el progreso de un tutorial no vale una pantalla rota.
      return []
    }
  }

  private clave(): string {
    return PREFIJO + huella(this.identidad.sujeto() ?? 'anonimo')
  }
}

export const SIN_ALMACEN = 'almacenamiento-no-disponible'

/** `localStorage` no existe en SSR ni en algunos modos privados. */
function almacenDelNavegador(): Storage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage
  } catch {
    // Acceder a `localStorage` LANZA cuando el navegador bloquea las cookies de sitio.
    return null
  }
}

/**
 * Huella corta y estable (FNV-1a de 32 bits) del sujeto de la sesión. No es criptografía
 * —no lo pretende— pero evita escribir el identificador del operador en el disco de una
 * máquina compartida, que es exactamente lo que se quería evitar.
 */
export function huella(sujeto: string): string {
  let h = 0x811c9dc5
  for (let i = 0; i < sujeto.length; i++) {
    h ^= sujeto.charCodeAt(i)
    h = Math.imul(h, 0x01000193) >>> 0
  }
  return h.toString(36)
}

function esProgreso(fila: unknown): fila is ProgresoDeTutorial {
  if (fila === null || typeof fila !== 'object') return false
  const f = fila as Record<string, unknown>
  return typeof f['tutorialId'] === 'string' && typeof f['version'] === 'string' && typeof f['estado'] === 'string' && typeof f['indice'] === 'number'
}
