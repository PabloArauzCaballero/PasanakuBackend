import { computed, signal, type Signal } from '@angular/core'
import type { Selector } from '../barra-de-filtros/barra-de-filtros'
import type { Opcion } from '../lista-de-opciones/lista-de-opciones'
import type { Orden } from '../tabla-de-datos/tabla-de-datos'

/**
 * Un criterio de filtro del listado. Sin `opciones`, se arman con los valores distintos que traen las filas
 * (ordenados), así un filtro nunca ofrece un valor que no existe ni esconde uno que sí.
 */
export type FiltroDeListado<T> = {
  nombre: string
  etiqueta: string
  valor: (fila: T) => string | null | undefined
  opciones?: readonly Opcion[]
  /** Cómo se lee cada valor en el desplegable cuando las opciones salen de los datos. */
  texto?: (valor: string) => string
  ayuda?: string
  textoTodos?: string
}

export type ConfigDeListado<T> = {
  filas: Signal<readonly T[]>
  /** Los campos en los que busca el buscador: claves o funciones que devuelven el texto que se ve. */
  camposDeBusqueda: readonly (keyof T | ((fila: T) => unknown))[]
  filtros?: readonly FiltroDeListado<T>[]
  porPagina?: number
  opcionesPorPagina?: readonly number[]
  ordenInicial?: Orden | null
  /** El valor con el que se ordena una columna; sin él, `fila[clave]`. */
  valorParaOrden?: (fila: T, clave: string) => unknown
}

/** Minúsculas y sin acentos: «José» y «jose» son la misma búsqueda. */
export function normalizar(texto: unknown): string {
  return String(texto ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}

const NUMERICO = /^-?\d+(\.\d+)?$/

/** Orden determinista: números (incluidos importes en texto) como números, el resto como texto en español. */
export function comparar(a: unknown, b: unknown): number {
  if (a === b) return 0
  if (a === null || a === undefined || a === '') return 1
  if (b === null || b === undefined || b === '') return -1
  const na = typeof a === 'number' ? a : NUMERICO.test(String(a)) ? Number(a) : NaN
  const nb = typeof b === 'number' ? b : NUMERICO.test(String(b)) ? Number(b) : NaN
  if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
  return String(a).localeCompare(String(b), 'es', { sensitivity: 'base', numeric: true })
}

/**
 * El estado de un listado en el navegador, con señales: búsqueda multicampo (cada palabra tiene que aparecer en
 * algún campo), N filtros, orden por columna, página y filas por página. Cambiar la búsqueda, un filtro o el
 * tamaño vuelve a la página 1; si el filtro achica el total, la página se acota a la última que existe.
 *
 * Es para colecciones que ya están en el navegador. Un listado paginado **en el servidor** manda la búsqueda y
 * los filtros en la consulta y usa `ap-barra-de-filtros` + `ap-paginacion` directo.
 */
export function crearListado<T>(config: ConfigDeListado<T>) {
  const opcionesPorPagina = config.opcionesPorPagina ?? [10, 20, 50, 100]
  const busqueda = signal('')
  const valores = signal<Record<string, string>>({})
  const orden = signal<Orden | null>(config.ordenInicial ?? null)
  const pagina = signal(1)
  const porPagina = signal(config.porPagina ?? opcionesPorPagina[0] ?? 10)
  const filtros = config.filtros ?? []
  const extractores = config.camposDeBusqueda.map((c) => (typeof c === 'function' ? c : (f: T) => f[c as keyof T]))
  const valorParaOrden = config.valorParaOrden ?? ((f: T, clave: string) => (f as Record<string, unknown>)[clave])

  const filtradas = computed<T[]>(() => {
    const palabras = normalizar(busqueda()).split(/\s+/).filter(Boolean)
    const activos = filtros.filter((f) => (valores()[f.nombre] ?? '') !== '')
    return config.filas().filter((fila) => {
      if (activos.some((f) => (f.valor(fila) ?? '') !== valores()[f.nombre])) return false
      if (palabras.length === 0) return true
      const texto = extractores.map((x) => normalizar(x(fila))).join(' ')
      return palabras.every((p) => texto.includes(p))
    })
  })
  const ordenadas = computed<T[]>(() => {
    const o = orden()
    if (!o) return filtradas()
    const signo = o.sentido === 'asc' ? 1 : -1
    // `map` con índice: el desempate por posición original hace el orden estable entre recargas.
    return filtradas()
      .map((f, i) => ({ f, i }))
      .sort((a, b) => signo * comparar(valorParaOrden(a.f, o.clave), valorParaOrden(b.f, o.clave)) || a.i - b.i)
      .map((x) => x.f)
  })
  const total = computed(() => filtradas().length)
  const totalDePaginas = computed(() => Math.max(1, Math.ceil(total() / porPagina())))
  const paginaAcotada = computed(() => Math.min(Math.max(1, pagina()), totalDePaginas()))
  const visibles = computed<T[]>(() => ordenadas().slice((paginaAcotada() - 1) * porPagina(), paginaAcotada() * porPagina()))
  const hayCriterios = computed(() => busqueda() !== '' || Object.values(valores()).some((v) => v !== ''))
  const selectores = computed<Selector[]>(() =>
    filtros.map((f) => ({
      nombre: f.nombre,
      etiqueta: f.etiqueta,
      valor: valores()[f.nombre] ?? '',
      ayuda: f.ayuda,
      textoTodos: f.textoTodos,
      opciones:
        f.opciones ??
        [...new Set(config.filas().map((fila) => f.valor(fila) ?? '').filter((v) => v !== ''))]
          .sort((a, b) => comparar(a, b))
          .map((v) => ({ valor: v, texto: f.texto ? f.texto(v) : v })),
    })),
  )

  return {
    /** Todas las filas, antes de buscar y filtrar. */
    filas: config.filas,
    busqueda: busqueda.asReadonly(),
    orden,
    pagina: paginaAcotada,
    porPagina: porPagina.asReadonly(),
    opcionesPorPagina,
    filtradas: ordenadas,
    visibles,
    total,
    totalDePaginas,
    selectores,
    hayCriterios,
    buscar(texto: string): void {
      busqueda.set(texto)
      pagina.set(1)
    },
    filtrar(nombre: string, valor: string): void {
      valores.update((v) => ({ ...v, [nombre]: valor }))
      pagina.set(1)
    },
    limpiar(): void {
      busqueda.set('')
      valores.set({})
      pagina.set(1)
    },
    irAPagina(n: number): void {
      pagina.set(Math.min(Math.max(1, Math.trunc(n) || 1), totalDePaginas()))
    },
    cambiarPorPagina(n: number): void {
      if (!Number.isFinite(n) || n <= 0) return
      porPagina.set(n)
      pagina.set(1)
    },
  }
}

export type Listado<T> = ReturnType<typeof crearListado<T>>
