import { HttpClient } from '@angular/common/http'
import { PendingTasks, computed, effect, inject, signal, untracked } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Listado } from '@aportaya/ui/listado/crear-listado'
import { mensajeDe, type ErrorTraducido } from '../../../nucleo/errores'
import { GATEWAY } from '../../../nucleo/gateway'
import { textosPublicidad } from '../textos'
import { aPaginaPorCursor, parametrosDeListado, type CursorPublicidad, type FiltroAParametros, type PaginaPublicidad } from './contrato-publicidad'

/** El tope del contrato (`limite` 1–100): cada lote trae lo máximo que el servidor acepta, nunca «todo». */
export const TAMANO_DE_LOTE = 100

/**
 * **Los listados de publicidad, por lotes** (2026-10-26). El contrato pagina por cursor, no trae total ni acepta búsqueda
 * de texto: por eso el listado pide de a {@link TAMANO_DE_LOTE} filas (el máximo del contrato) y el kit `crearListado`
 * busca, filtra, ordena, pagina y exporta **lo cargado**. Si el servidor dijo que hay más (`proximoFecha` + `proximoId`),
 * la vista ofrece «Cargar 100 más», que pide el lote siguiente con el cursor que devolvió el anterior y lo agrega. Nunca
 * se baja la colección entera de una vez ni se calcula un cursor.
 *
 * El filtro de estado sí lo acepta el contrato: cambiarlo (`recargar`) vuelve a pedir desde el primer lote, porque un
 * cursor solo vale para la consulta que lo produjo. Una respuesta atrasada de una consulta vieja se descarta.
 */
export type LotesDePublicidad<T> = ReturnType<typeof lotesPorCursor<T>>

export function lotesPorCursor<T>(ruta: string, filtro: FiltroAParametros) {
  const http = inject(HttpClient)
  const url = `${inject(GATEWAY)}${ruta}`
  const tareas = inject(PendingTasks)
  const filas = signal<T[]>([])
  const carga = signal<CargaDeListado>({ tipo: 'cargando' })
  const siguiente = signal<CursorPublicidad | null>(null)
  const cargandoMas = signal(false)
  const errorDeMas = signal<string | null>(null)
  let filtros: Readonly<Record<string, string>> = {}
  let consulta = 0

  async function pedir(cursor: CursorPublicidad | null): Promise<{ items: T[]; siguiente: CursorPublicidad | null }> {
    const params = parametrosDeListado({ limite: TAMANO_DE_LOTE, cursor, filtros }, filtro)
    return aPaginaPorCursor(await firstValueFrom(http.get<PaginaPublicidad<T>>(url, { params })))
  }

  async function primerLote(): Promise<void> {
    const id = ++consulta
    carga.set({ tipo: 'cargando' })
    errorDeMas.set(null)
    try {
      const lote = await pedir(null)
      if (id !== consulta) return // respuesta de una consulta vieja: no pisa la vigente
      filas.set(lote.items)
      siguiente.set(lote.siguiente)
      carga.set({ tipo: 'lista' })
    } catch (e) {
      if (id !== consulta) return
      const t = traducirError(e)
      filas.set([])
      siguiente.set(null)
      carga.set({ tipo: 'error', mensaje: t.mensaje, idPeticion: t.trazaId })
    }
  }

  async function loteSiguiente(): Promise<void> {
    const cursor = siguiente()
    if (!cursor || cargandoMas()) return
    const id = consulta
    cargandoMas.set(true)
    errorDeMas.set(null)
    try {
      const lote = await pedir(cursor)
      if (id !== consulta) return
      filas.update((actuales) => [...actuales, ...lote.items])
      siguiente.set(lote.siguiente)
    } catch (e) {
      // Lo ya cargado se queda: falló el lote nuevo, no el listado.
      if (id === consulta) errorDeMas.set(traducirError(e).mensaje)
    } finally {
      if (id === consulta) cargandoMas.set(false)
    }
  }

  return {
    filas: filas.asReadonly(),
    carga: carga.asReadonly(),
    hayMas: computed(() => siguiente() !== null),
    cargandoMas: cargandoMas.asReadonly(),
    errorDeMas: errorDeMas.asReadonly(),
    /** Vuelve a pedir desde el primer lote; con `nuevos`, con esos filtros del contrato. */
    recargar(nuevos?: Readonly<Record<string, string>>): void {
      if (nuevos) filtros = nuevos
      void tareas.run(primerLote)
    },
    cargarMas(): void {
      void tareas.run(loteSiguiente)
    },
  }
}

/**
 * `erroresInterceptor` ya entrega el error traducido, con su `trazaId`; acá solo se cambian los códigos de publicidad que
 * el catálogo de `nucleo/errores.ts` todavía no conoce. Nunca el cuerpo crudo del backend.
 */
export function traducirError(e: unknown): ErrorTraducido {
  if (e && typeof e === 'object' && typeof (e as ErrorTraducido).mensaje === 'string') {
    const t = e as ErrorTraducido
    const propio = t.codigo ? textosPublicidad.errores[t.codigo] : undefined
    return propio ? { ...t, mensaje: propio } : t
  }
  return { mensaje: mensajeDe(undefined, 500) }
}

/**
 * Ata el selector `nombre` del listado (el de estado) a la consulta del servidor: cada vez que la persona elige otro
 * valor, se vuelve a pedir el primer lote con ese filtro. El primer disparo del efecto es la carga inicial. El selector
 * sigue filtrando también lo cargado, así que nunca se ve una fila de otro estado mientras llega el lote nuevo.
 */
export function filtroEnElServidor<T>(listado: Listado<T>, lotes: Pick<LotesDePublicidad<T>, 'recargar'>, nombre = 'estado'): void {
  const elegido = computed(() => listado.selectores().find((s) => s.nombre === nombre)?.valor ?? '')
  effect(() => {
    const valor = elegido()
    untracked(() => lotes.recargar(valor ? { [nombre]: valor } : {}))
  })
}
