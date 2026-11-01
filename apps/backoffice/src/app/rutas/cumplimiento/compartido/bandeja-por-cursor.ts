import { computed, signal } from '@angular/core'
import type { Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { normalizar } from '@aportaya/ui/listado/crear-listado'
import type { Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { traducirError } from './errores-de-cumplimiento'

/** Un filtro que viaja al servidor (`estados`, `severidades`): un valor por vez; vacío = lo que el servidor da sin filtro. */
export type FiltroDeServidor = { nombre: string; etiqueta: string; opciones: readonly Opcion[]; textoTodos?: string; ayuda?: string }

export type CargaDeBandeja<T> =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; filas: readonly T[]; total: number }
  | { tipo: 'error'; error: ErrorTraducido }
  | { tipo: 'denegado'; error: ErrorTraducido }

export type ConfigDeBandeja<T> = {
  cargador: CargadorDePagina<T>
  /** Dónde busca el buscador: el contrato no admite búsqueda, así que se busca en la página cargada. */
  camposDeBusqueda: readonly (keyof T | ((fila: T) => unknown))[]
  filtros?: readonly FiltroDeServidor[]
  porPagina?: number
  opcionesPorPagina?: readonly number[]
}

/**
 * **Una bandeja de cumplimiento paginada en el servidor por cursor** (reemplaza a `observarBandeja` + la tabla
 * virtualizada, 2026-10-26). El contrato pagina con cursor y no trae total: los filtros y las filas por página viajan
 * en la consulta, la página N se alcanza con los cursores que guarda `cargadorPorCursor` y el total que ve la
 * paginación es «lo ya visto + una página más si el servidor dijo que hay más» (nunca un total inventado).
 *
 * El buscador busca **en la página cargada** (ningún listado de `cumplimiento.yaml` acepta búsqueda) y la descarga
 * baja esa página filtrada; la vista lo dice en la ayuda. Un rechazo se vuelve estado visible —403 es «sin acceso»,
 * el resto error con reintento y código de seguimiento— y solo la ÚLTIMA carga pedida escribe: una respuesta atrasada
 * no pisa a una más nueva.
 */
export function crearBandeja<T>(config: ConfigDeBandeja<T>) {
  const opcionesPorPagina = config.opcionesPorPagina ?? [10, 25, 50, 100]
  const carga = signal<CargaDeBandeja<T>>({ tipo: 'cargando' })
  const pagina = signal(1)
  const porPagina = signal(config.porPagina ?? 25)
  const valores = signal<Record<string, string>>({})
  const busqueda = signal('')
  const filtros = config.filtros ?? []
  const extractores = config.camposDeBusqueda.map((c) => (typeof c === 'function' ? c : (f: T) => f[c as keyof T]))
  let ultima = 0

  async function cargar(): Promise<void> {
    const esta = ++ultima
    carga.set({ tipo: 'cargando' })
    const filtrosActivos = Object.fromEntries(Object.entries(valores()).filter(([, v]) => v !== ''))
    try {
      const r = await config.cargador({ pagina: pagina(), tamano: porPagina(), orden: null, filtros: filtrosActivos })
      if (esta === ultima) carga.set({ tipo: 'lista', filas: r.filas, total: r.total })
    } catch (e) {
      if (esta !== ultima) return
      const error = traducirError(e)
      carga.set(error.estado === 403 ? { tipo: 'denegado', error } : { tipo: 'error', error })
    }
  }

  const filas = computed<readonly T[]>(() => {
    const c = carga()
    return c.tipo === 'lista' ? c.filas : []
  })
  const filtradas = computed<T[]>(() => {
    const palabras = normalizar(busqueda()).split(/\s+/).filter(Boolean)
    if (palabras.length === 0) return [...filas()]
    return filas().filter((f) => {
      const texto = extractores.map((x) => normalizar(x(f))).join(' ')
      return palabras.every((p) => texto.includes(p))
    })
  })
  const total = computed(() => {
    const c = carga()
    return c.tipo === 'lista' ? c.total : 0
  })
  const hayMas = computed(() => total() > (pagina() - 1) * porPagina() + filas().length)
  const filtrosDeServidor = computed(() => Object.values(valores()).some((v) => v !== ''))
  const hayCriterios = computed(() => busqueda() !== '' || filtrosDeServidor())
  const selectores = computed<Selector[]>(() =>
    filtros.map((f) => ({ nombre: f.nombre, etiqueta: f.etiqueta, valor: valores()[f.nombre] ?? '', opciones: f.opciones, textoTodos: f.textoTodos, ayuda: f.ayuda })),
  )

  void cargar()

  return {
    carga: carga.asReadonly(),
    pagina: pagina.asReadonly(),
    porPagina: porPagina.asReadonly(),
    opcionesPorPagina,
    busqueda: busqueda.asReadonly(),
    filas,
    filtradas,
    total,
    hayMas,
    hayCriterios,
    filtrosDeServidor,
    selectores,
    buscar(texto: string): void {
      busqueda.set(texto)
    },
    filtrar(nombre: string, valor: string): void {
      valores.update((v) => ({ ...v, [nombre]: valor }))
      pagina.set(1)
      void cargar()
    },
    limpiar(): void {
      const recargar = filtrosDeServidor()
      busqueda.set('')
      valores.set({})
      if (recargar) {
        pagina.set(1)
        void cargar()
      }
    },
    irAPagina(n: number): void {
      const destino = Math.max(1, Math.trunc(n) || 1)
      if (destino === pagina()) return
      pagina.set(destino)
      void cargar()
    },
    cambiarPorPagina(n: number): void {
      if (!Number.isFinite(n) || n <= 0 || n === porPagina()) return
      porPagina.set(n)
      pagina.set(1)
      void cargar()
    },
    /** Reintentar, o refrescar después de una acción: la misma página con los mismos filtros. */
    recargar: (): Promise<void> => cargar(),
  }
}

export type Bandeja<T> = ReturnType<typeof crearBandeja<T>>
