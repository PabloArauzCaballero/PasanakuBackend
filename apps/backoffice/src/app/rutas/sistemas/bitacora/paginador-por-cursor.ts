import { computed, signal } from '@angular/core'

/** El cursor del contrato (`auditoria.yaml`): la página siguiente empieza antes de este evento. */
export type Cursor = { antesDe: string; antesDeId: string }

/** Lo que el paginador necesita de cada respuesta: el cursor de la página siguiente (si hay) y el total aproximado. */
export type RespuestaPaginada = { proximoAntesDe?: string; proximoAntesDeId?: string; totalAproximado?: number; items: readonly unknown[] }

/**
 * Paginación **en el servidor** por cursor descendente, con la misma experiencia que el kit (filas por página, ir a
 * la página N, Anterior/Siguiente). El contrato no permite saltar a una página: «Ir a N» recorre las intermedias pidiendo
 * cada cursor y se detiene si el servidor ya no trae más. Los cursores ya conocidos se guardan, así volver atrás no
 * repite el recorrido. Cambiar un filtro o el tamaño vuelve a la página 1.
 */
export function crearPaginadorPorCursor(opciones: { porPagina?: number; opcionesPorPagina?: readonly number[] } = {}) {
  const opcionesPorPagina = opciones.opcionesPorPagina ?? [10, 25, 50, 100]
  // cursores[k] es el cursor con el que se pide la página k + 1; la primera va sin cursor.
  const cursores = signal<(Cursor | null)[]>([null])
  const pagina = signal(1)
  const destino = signal(1)
  const porPagina = signal(opciones.porPagina ?? 25)
  const total = signal<number | undefined>(undefined)
  const ultimaRecibida = signal(0)
  const cursorActual = computed(() => cursores()[pagina() - 1] ?? null)

  function reiniciar(): void {
    cursores.set([null])
    pagina.set(1)
    destino.set(1)
    ultimaRecibida.set(0)
  }

  return {
    pagina: pagina.asReadonly(),
    destino: destino.asReadonly(),
    porPagina: porPagina.asReadonly(),
    opcionesPorPagina,
    antesDe: computed(() => cursorActual()?.antesDe ?? null),
    antesDeId: computed(() => cursorActual()?.antesDeId ?? null),
    /** Las páginas que se pueden pedir ya: las recorridas más la siguiente si el servidor dio su cursor. */
    paginasConocidas: computed(() => cursores().length),
    /** Para `ap-paginacion`: con total aproximado, el real; sin él, lo conocido (crece a medida que se avanza). */
    totalDeFilas: computed(() => total() ?? cursores().length * porPagina()),
    conTotal: computed(() => total() !== undefined),
    /** Registra la respuesta de la página vigente y, si se pidió una página más adelante, avanza un paso. */
    recibir(r: RespuestaPaginada): void {
      total.set(r.totalAproximado)
      const p = pagina()
      ultimaRecibida.set(p)
      if (r.proximoAntesDe && r.proximoAntesDeId && cursores().length === p) cursores.update((c) => [...c, { antesDe: r.proximoAntesDe!, antesDeId: r.proximoAntesDeId! }])
      if (destino() > p) {
        if (cursores().length > p) pagina.set(p + 1)
        else destino.set(p) // el servidor ya no tiene más páginas: el recorrido se detiene en la última
      }
    },
    irAPagina(n: number): void {
      const pedida = Math.max(1, Math.trunc(n) || 1)
      const conocidas = cursores().length
      // Ya se recibió la última página conocida y no trajo cursor siguiente: no hay a dónde avanzar.
      destino.set(pedida > conocidas && pagina() === conocidas && ultimaRecibida() === conocidas ? conocidas : pedida)
      // Hacia atrás o a una página con cursor conocido se va directo; más adelante, `recibir` recorre.
      pagina.set(Math.min(pedida, conocidas))
    },
    cambiarPorPagina(n: number): void {
      if (!Number.isFinite(n) || n <= 0) return
      porPagina.set(n)
      reiniciar()
    },
    reiniciar,
  }
}

export type PaginadorPorCursor = ReturnType<typeof crearPaginadorPorCursor>
