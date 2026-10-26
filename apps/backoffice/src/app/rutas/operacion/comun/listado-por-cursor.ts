import { computed, signal } from '@angular/core'
import type { Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { crearListado, type FiltroDeListado } from '@aportaya/ui/listado/crear-listado'
import type { Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { cargadorPorCursor, type PedirPorCursor } from '../../../nucleo/tabla/cursor'

/**
 * Un criterio que viaja **al servidor** (el contrato lo acepta como parámetro, p. ej. `estados`): cambiarlo vuelve a
 * pedir la primera página. Las opciones salen del enum del contrato, no de la página cargada.
 */
export type FiltroDeServidor = { nombre: string; etiqueta: string; opciones: readonly Opcion[]; ayuda?: string; textoTodos?: string }

export type ConfigPorCursor<T, C> = {
  pedir: PedirPorCursor<T, C>
  /** Busca dentro de la página cargada: ningún listado de operación acepta búsqueda en el contrato. */
  camposDeBusqueda: readonly (keyof T | ((fila: T) => unknown))[]
  filtrosLocales?: readonly FiltroDeListado<T>[]
  filtrosDeServidor?: readonly FiltroDeServidor[]
  porPagina?: number
}

export type EstadoPorCursor = 'cargando' | 'lista' | 'error' | 'denegado'

const OPCIONES_POR_PAGINA = [10, 20, 50, 100] as const

/**
 * **El listado de las bandejas de operación (paginadas por cursor en el servidor), 2026-10-26.** Junta dos cosas:
 *
 * - la página del servidor: filas por página, ir a la página N, Anterior/Siguiente y los filtros que el contrato acepta
 *   (`filtrosDeServidor`) viajan en la consulta; los cursores los guarda `cargadorPorCursor` y nunca se calculan;
 * - lo que se hace en el navegador sobre **esa página**: buscador multicampo y filtros locales con `crearListado`, y
 *   la descarga CSV/JSON de lo filtrado de la página (se dice así en la ayuda del buscador).
 *
 * El total del servidor no existe en el contrato: la paginación conoce las páginas vistas y una más si el servidor dijo
 * que hay próxima. Una respuesta vieja (la persona cambió de página antes de que llegara) se descarta.
 */
export function crearListadoPorCursor<T, C>(config: ConfigPorCursor<T, C>) {
  const cargador = cargadorPorCursor(config.pedir)
  const filas = signal<readonly T[]>([])
  const estado = signal<EstadoPorCursor>('cargando')
  const error = signal<ErrorTraducido | null>(null)
  const pagina = signal(1)
  // 10 = la primera opción: `ap-paginacion` muestra la primera opción del selector al montar (ver «Pedidos» en franjas/b.md).
  const porPagina = signal(config.porPagina ?? 10)
  const totalConocido = signal(0)
  const deServidor = signal<Record<string, string>>({})
  const definiciones = config.filtrosDeServidor ?? []
  // La página del servidor es la unidad: el listado local nunca la vuelve a partir (100 = el tamaño máximo).
  const local = crearListado<T>({ filas, camposDeBusqueda: config.camposDeBusqueda, filtros: config.filtrosLocales, porPagina: 100, opcionesPorPagina: [100] })
  let turno = 0

  async function cargar(): Promise<void> {
    const mio = ++turno
    estado.set('cargando')
    error.set(null)
    const filtros = Object.fromEntries(Object.entries(deServidor()).filter(([, v]) => v !== ''))
    try {
      const r = await cargador({ pagina: pagina(), tamano: porPagina(), orden: null, filtros })
      if (mio !== turno) return
      filas.set(r.filas)
      totalConocido.set(r.total)
      estado.set('lista')
    } catch (e) {
      if (mio !== turno) return
      const traducido = (e ?? {}) as ErrorTraducido
      filas.set([])
      error.set(traducido)
      estado.set(traducido.estado === 403 ? 'denegado' : 'error')
    }
  }

  const selectores = computed<Selector[]>(() => [
    ...definiciones.map((d) => ({ nombre: d.nombre, etiqueta: d.etiqueta, opciones: d.opciones, valor: deServidor()[d.nombre] ?? '', ayuda: d.ayuda, textoTodos: d.textoTodos })),
    ...local.selectores(),
  ])
  const hayCriteriosDeServidor = computed(() => Object.values(deServidor()).some((v) => v !== ''))
  const hayCriterios = computed(() => hayCriteriosDeServidor() || local.hayCriterios())
  const esDeServidor = (nombre: string) => definiciones.some((d) => d.nombre === nombre)

  void cargar()

  return {
    local,
    estado: estado.asReadonly(),
    error: error.asReadonly(),
    pagina: pagina.asReadonly(),
    porPagina: porPagina.asReadonly(),
    opcionesPorPagina: OPCIONES_POR_PAGINA,
    /** Filas vistas + una página más si el servidor dijo que hay próxima: lo que `ap-paginacion` necesita para ofrecer avanzar. */
    totalConocido: totalConocido.asReadonly(),
    selectores,
    hayCriterios,
    hayCriteriosDeServidor,
    recargar: (): void => void cargar(),
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
    buscar: (texto: string): void => local.buscar(texto),
    filtrar(nombre: string, valor: string): void {
      if (!esDeServidor(nombre)) return local.filtrar(nombre, valor)
      deServidor.update((v) => ({ ...v, [nombre]: valor }))
      pagina.set(1)
      void cargar()
    },
    limpiar(): void {
      local.limpiar()
      if (!hayCriteriosDeServidor()) return
      deServidor.set({})
      pagina.set(1)
      void cargar()
    },
  }
}

export type ListadoPorCursor<T> = ReturnType<typeof crearListadoPorCursor<T, unknown>>
