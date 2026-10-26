import { computed, effect, inject, signal, untracked } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { HttpErrorResponse } from '@angular/common/http'
import { crearListado, type FiltroDeListado } from '@aportaya/ui/listado/crear-listado'
import type { CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import type { Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { mensajeDe } from '../../../nucleo/errores'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'

/**
 * **El estado de un listado paginado por cursor en el servidor** (2026-10-26), para las bandejas del ERP (CU-101 a
 * CU-105) y de Configuración (CU-34, CU-58, CU-80/83). Ninguno de esos contratos acepta búsqueda de texto ni orden por
 * columna, y ninguno da un total: por eso
 *
 * - la **página** la pide el servidor con `limite` (filas por página) y el cursor que guarda `cargadorPorCursor`
 *   (`nucleo/tabla/cursor.ts`): «Ir a la página N» recorre desde la última conocida, nunca calcula un cursor;
 * - el filtro **del servidor** (`estado` → `estados` del contrato) viaja en la consulta y reinicia la página;
 * - el **buscador** y los demás filtros se aplican sobre la página cargada (`crearListado` de `@aportaya/ui`), y la
 *   exportación CSV/JSON baja lo filtrado de esa página. La pantalla lo dice en la ayuda del buscador.
 *
 * Nada se baja entero para paginar en el navegador. La respuesta vieja de una consulta que ya cambió se descarta.
 */
export type FiltroDelServidor = { nombre: string; etiqueta: string; opciones: readonly Opcion[]; ayuda?: string; inicial?: string }

/**
 * El primer valor de `?filtro=A,B` (la forma que dejaba `BarraDeFiltros` en la URL), si es uno de los permitidos: el
 * enlace de una bandeja filtrada pegado en un expediente sigue abriendo filtrado. Un código pegado a mano no viaja.
 */
export function filtroInicialDeLaUrl(opciones: readonly Opcion[]): string {
  const elegido = inject(ActivatedRoute, { optional: true })?.snapshot.queryParamMap.get('filtro')?.split(',')[0] ?? ''
  return opciones.some((o) => o.valor === elegido) ? elegido : ''
}

export type ConfigDeListadoPorCursor<T> = {
  cargador: CargadorDePagina<T>
  camposDeBusqueda: readonly (keyof T | ((fila: T) => unknown))[]
  filtros?: readonly FiltroDeListado<T>[]
  servidor?: FiltroDelServidor
  porPagina?: number
  opcionesPorPagina?: readonly number[]
  /** Códigos `AP-…` que el catálogo de `nucleo/errores.ts` todavía no traduce. */
  textosDeError?: Readonly<Record<string, string>>
}

/** Las etiquetas de un enum del contrato como opciones de un selector (el valor es el código, el texto la etiqueta). */
export const opcionesDe = (etiquetas: Readonly<Record<string, string>>): Opcion[] => Object.entries(etiquetas).map(([valor, texto]) => ({ valor, texto }))

type ErrorLeido ={ mensaje: string; trazaId?: string; codigo?: string }

/** Lo que llega ya traducido por `erroresInterceptor`, o se traduce acá con el mismo catálogo. Nunca el cuerpo crudo. */
export function leerError(e: unknown, textos: Readonly<Record<string, string>> = {}): ErrorLeido {
  let leido: ErrorLeido
  if (e instanceof HttpErrorResponse) {
    const cuerpo = (e.error ?? {}) as Partial<ErrorLeido>
    leido = { mensaje: mensajeDe(cuerpo.codigo, e.status), trazaId: cuerpo.trazaId, codigo: cuerpo.codigo }
  } else if (e && typeof e === 'object' && typeof (e as ErrorLeido).mensaje === 'string') leido = e as ErrorLeido
  else leido = { mensaje: mensajeDe(undefined, 500) }
  const propio = leido.codigo ? textos[leido.codigo] : undefined
  return propio ? { ...leido, mensaje: propio } : leido
}

export function crearListadoPorCursor<T>(config: ConfigDeListadoPorCursor<T>) {
  const opcionesPorPagina = config.opcionesPorPagina ?? [10, 20, 50, 100]
  const pagina = signal(1)
  const porPagina = signal(config.porPagina ?? opcionesPorPagina[0] ?? 10)
  const valorDelServidor = signal(config.servidor?.inicial ?? '')
  const filas = signal<T[]>([])
  const total = signal(0)
  const carga = signal<CargaDeListado>({ tipo: 'cargando' })
  const vuelta = signal(0)
  // La página entera ya está filtrada por el servidor: el kit no vuelve a paginarla (100 = el tope de `limite`).
  const local = crearListado<T>({ filas, camposDeBusqueda: config.camposDeBusqueda, filtros: config.filtros, porPagina: 100, opcionesPorPagina: [100] })
  let pedido = 0

  effect(() => {
    const consulta = { pagina: pagina(), tamano: porPagina(), orden: null, filtros: valorDelServidor() ? { [config.servidor!.nombre]: valorDelServidor() } : {} }
    vuelta()
    const este = ++pedido
    untracked(() => carga.set({ tipo: 'cargando' }))
    config.cargador(consulta).then(
      (respuesta) => {
        if (este !== pedido) return
        filas.set(respuesta.filas)
        total.set(respuesta.total)
        carga.set({ tipo: 'lista' })
      },
      (e: unknown) => {
        if (este !== pedido) return
        const leido = leerError(e, config.textosDeError)
        filas.set([])
        carga.set({ tipo: 'error', mensaje: leido.mensaje, idPeticion: leido.trazaId })
      },
    )
  })

  const selectores = computed<Selector[]>(() => {
    const s = config.servidor
    const propio: Selector[] = s ? [{ nombre: s.nombre, etiqueta: s.etiqueta, opciones: s.opciones, valor: valorDelServidor(), ayuda: s.ayuda }] : []
    return [...propio, ...local.selectores()]
  })

  return {
    local,
    filas: filas.asReadonly(),
    /** Lo que se ve y lo que se exporta: la página cargada, buscada y filtrada. */
    visibles: local.filtradas,
    carga: carga.asReadonly(),
    pagina: pagina.asReadonly(),
    porPagina: porPagina.asReadonly(),
    total: total.asReadonly(),
    opcionesPorPagina,
    selectores,
    hayFiltroDelServidor: computed(() => valorDelServidor() !== ''),
    hayCriterios: computed(() => valorDelServidor() !== '' || local.hayCriterios()),
    buscar: (texto: string) => local.buscar(texto),
    filtrar(nombre: string, valor: string): void {
      if (config.servidor && nombre === config.servidor.nombre) {
        valorDelServidor.set(valor)
        pagina.set(1)
      } else local.filtrar(nombre, valor)
    },
    limpiar(): void {
      local.limpiar()
      if (valorDelServidor() !== '') {
        valorDelServidor.set('')
        pagina.set(1)
      }
    },
    irAPagina(n: number): void {
      pagina.set(Math.max(1, Math.trunc(n) || 1))
    },
    cambiarPorPagina(n: number): void {
      if (!Number.isFinite(n) || n <= 0) return
      porPagina.set(n)
      pagina.set(1)
    },
    /** Vuelve a pedir la página vigente: tras un error, o después de una mutación que cambió las filas. */
    recargar(): void {
      vuelta.update((v) => v + 1)
    },
  }
}

export type ListadoPorCursor<T> = ReturnType<typeof crearListadoPorCursor<T>>
