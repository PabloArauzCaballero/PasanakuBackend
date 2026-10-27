import { HttpClient } from '@angular/common/http'
import { Injectable, computed, inject, signal } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { normalizar, SIN_FILTROS, urlDeTablas, type PaginaDeTablasDelCatalogo } from '../rutas/tablero/catalogo/dominio'
import { GATEWAY } from './gateway'
import { entradasVisibles, hojas, pantallasVisibles } from './navegacion'
import { Sesion } from './sesion'

/** Un resultado del buscador global: a dónde lleva y cómo se describe. */
export type Resultado = { tipo: 'pantalla' | 'tabla'; texto: string; detalle: string; ruta: string }

/**
 * El índice del buscador global (`⌘K`), como el del portal de Atlas: pantallas del menú (solo las
 * que el permiso habilita) y, si quien busca puede ver el catálogo, las tablas de la base. Ni un dato
 * de personas ni una petición por tecla: los nombres de las tablas se piden una sola vez, la primera
 * vez que alguien busca (antes se bajaba el catálogo entero, ~2 MB, al abrir cualquier pantalla).
 */
@Injectable({ providedIn: 'root' })
export class IndiceDeBusqueda {
  private readonly sesion = inject(Sesion)
  private readonly http = inject(HttpClient)
  private readonly gateway = inject(GATEWAY)
  private readonly tablas = signal<Resultado[]>([])
  private pedidas = false

  // Las mismas pantallas que el menú le muestra a esta sesión: un resultado que rebota al tablero es un botón roto.
  private readonly pantallas = computed<Resultado[]>(() =>
    entradasVisibles(this.sesion)
      .flatMap((grupo) => grupo.entradas)
      .flatMap((e) => [
        { tipo: 'pantalla' as const, texto: e.texto, detalle: g(e.texto), ruta: e.destino },
        ...pantallasVisibles(e, this.sesion)
          .flatMap((p) => hojas(p).map((h) => ({ h, p })))
          .map(({ h, p }) => ({ tipo: 'pantalla' as const, texto: h.texto, detalle: p.grupo ? `${e.texto} · ${p.texto}` : e.texto, ruta: h.ruta })),
      ]),
  )

  /** Los nombres de todas las tablas, de a cien por página, una sola vez. Si falla, el buscador sigue con las pantallas. */
  private async cargarTablas(): Promise<void> {
    const encontradas: Resultado[] = []
    try {
      for (let pagina = 1; ; pagina++) {
        const p = await firstValueFrom(this.http.get<PaginaDeTablasDelCatalogo>(urlDeTablas(this.gateway, SIN_FILTROS, pagina, 100)))
        encontradas.push(...p.items.map((t) => ({ tipo: 'tabla' as const, texto: t.tabla, detalle: `${t.esquema} · ${t.nombreModulo}`, ruta: `/tablero/catalogo-de-datos/${t.esquema}/${t.tabla}` })))
        if (pagina >= p.totalPaginas) break
      }
      this.tablas.set(encontradas)
    } catch {
      this.pedidas = false
    }
  }

  /** Hasta `maximo` resultados: primero las pantallas, después las tablas; coincide por texto y detalle, sin acentos. */
  buscar(consulta: string, maximo = 8): Resultado[] {
    const q = normalizar(consulta)
    if (q.length < 2) return []
    if (!this.pedidas && this.sesion.puede('ver:catalogo')) {
      this.pedidas = true
      void this.cargarTablas()
    }
    const coincide = (r: Resultado) => normalizar(r.texto).includes(q) || normalizar(r.detalle).includes(q) || normalizar(r.texto.replaceAll('_', ' ')).includes(q)
    // Una sección y su primera pestaña comparten ruta: se muestra una sola vez (la lista se identifica por ruta).
    const vistas = new Set<string>()
    return [...this.pantallas().filter(coincide), ...this.tablas().filter(coincide)].filter((r) => !vistas.has(r.ruta) && vistas.add(r.ruta)).slice(0, maximo)
  }
}

const g = (texto: string) => (texto === 'Inicio' ? 'Tablero' : 'Sección')
