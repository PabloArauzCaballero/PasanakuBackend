import { Injectable, computed, inject } from '@angular/core'
import { normalizar } from '../rutas/tablero/catalogo/dominio'
import { CatalogoDeDatosRemoto } from '../rutas/tablero/catalogo/dominio'
import { MENU } from './menu'
import { Sesion } from './sesion'

/** Un resultado del buscador global: a dónde lleva y cómo se describe. */
export type Resultado = { tipo: 'pantalla' | 'tabla'; texto: string; detalle: string; ruta: string }

/**
 * El índice del buscador global (`⌘K`), como el del portal de Atlas: pantallas del menú (solo las
 * que el permiso habilita) y, si quien busca puede ver el catálogo, las tablas de la base. Todo es
 * local: ni un dato de personas ni una petición por tecla.
 */
@Injectable({ providedIn: 'root' })
export class IndiceDeBusqueda {
  private readonly sesion = inject(Sesion)
  private readonly catalogo = inject(CatalogoDeDatosRemoto)

  private readonly pantallas = computed<Resultado[]>(() =>
    MENU.flatMap((g) => g.entradas)
      .filter((e) => this.sesion.puede(e.permiso))
      .flatMap((e) => [
        { tipo: 'pantalla' as const, texto: e.texto, detalle: g(e.texto), ruta: e.destino },
        ...(e.hijos ?? []).map((h) => ({ tipo: 'pantalla' as const, texto: h.texto, detalle: e.texto, ruta: h.ruta ? `${e.destino}/${h.ruta}` : e.destino })),
      ]),
  )

  private readonly tablas = computed<Resultado[]>(() => {
    if (!this.sesion.puede('ver:catalogo') || !this.catalogo.recurso.hasValue()) return []
    return (this.catalogo.recurso.value()?.tablas ?? []).map((t) => ({
      tipo: 'tabla' as const,
      texto: t.tabla,
      detalle: `${t.esquema} · ${t.nombreModulo}`,
      ruta: `/tablero/catalogo-de-datos/${t.tabla}`,
    }))
  })

  /** Hasta `maximo` resultados: primero las pantallas, después las tablas; coincide por texto y detalle, sin acentos. */
  buscar(consulta: string, maximo = 8): Resultado[] {
    const q = normalizar(consulta)
    if (q.length < 2) return []
    const coincide = (r: Resultado) => normalizar(r.texto).includes(q) || normalizar(r.detalle).includes(q) || normalizar(r.texto.replaceAll('_', ' ')).includes(q)
    return [...this.pantallas().filter(coincide), ...this.tablas().filter(coincide)].slice(0, maximo)
  }
}

const g = (texto: string) => (texto === 'Inicio' ? 'Tablero' : 'Sección')
