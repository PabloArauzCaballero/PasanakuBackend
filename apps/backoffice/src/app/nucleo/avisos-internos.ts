import { HttpClient } from '@angular/common/http'
import { computed, inject, Injectable, signal } from '@angular/core'
import { tap } from 'rxjs'
import type { AvisoInterno, PaginaDeAvisosInternos, SalidaMarcarTodosLeidos } from 'clientes/angular/notificaciones'
import { GATEWAY } from './gateway'
import { contextoIdempotente } from './idempotencia.interceptor'

/**
 * H5.S0 · Avisos internos para operadores (la campana del backoffice).
 * Maneja la lista de avisos reactiva, el conteo de no leídos y las mutaciones.
 */
@Injectable({ providedIn: 'root' })
export class AvisosInternosServicio {
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)

  private readonly estado = signal<{
    elementos: AvisoInterno[]
    totalNoLeidos: number
    cargando: boolean
    error: string | null
  }>({
    elementos: [],
    totalNoLeidos: 0,
    cargando: false,
    error: null,
  })

  readonly avisos = computed(() => this.estado().elementos)
  readonly noLeidos = computed(() => this.estado().totalNoLeidos)
  readonly cargando = computed(() => this.estado().cargando)
  readonly error = computed(() => this.estado().error)

  cargar(soloNoLeidos = false, limite = 20) {
    this.estado.update((s) => ({ ...s, cargando: true, error: null }))
    const q = new URLSearchParams()
    if (soloNoLeidos) q.set('soloNoLeidos', 'true')
    q.set('limite', String(limite))

    return this.http
      .get<PaginaDeAvisosInternos>(`${this.gateway}/notificaciones/avisos-internos?${q.toString()}`)
      .pipe(
        tap({
          next: (res) => {
            this.estado.set({
              elementos: res.elementos ?? [],
              totalNoLeidos: res.totalNoLeidos ?? 0,
              cargando: false,
              error: null,
            })
          },
          error: () => {
            this.estado.update((s) => ({
              ...s,
              cargando: false,
              error: 'Error al cargar avisos',
            }))
          },
        }),
      )
  }

  marcarLeido(id: string) {
    return this.http
      .post<AvisoInterno>(
        `${this.gateway}/notificaciones/avisos-internos/${id}/leido`,
        {},
        { context: contextoIdempotente() },
      )
      .pipe(
        tap({
          next: (actualizado) => {
            this.estado.update((s) => {
              const prev = s.elementos.find((e) => e.id === id)
              const restaba = prev && !prev.leidoEn ? 1 : 0
              return {
                ...s,
                elementos: s.elementos.map((e) => (e.id === id ? actualizado : e)),
                totalNoLeidos: Math.max(0, s.totalNoLeidos - restaba),
              }
            })
          },
        }),
      )
  }

  marcarTodosLeidos() {
    return this.http
      .post<SalidaMarcarTodosLeidos>(
        `${this.gateway}/notificaciones/avisos-internos/marcar-todos-leidos`,
        {},
        { context: contextoIdempotente() },
      )
      .pipe(
        tap({
          next: () => {
            const ahora = new Date().toISOString()
            this.estado.update((s) => ({
              ...s,
              elementos: s.elementos.map((e) => ({ ...e, leidoEn: e.leidoEn ?? ahora })),
              totalNoLeidos: 0,
            }))
          },
        }),
      )
  }
}
