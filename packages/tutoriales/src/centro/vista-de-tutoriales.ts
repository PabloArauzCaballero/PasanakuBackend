import { estadoDe, fraccionDe, requisitosPendientes, sePuedeContinuar } from '../avance'
import type { EstadoDeProgreso, ProgresoDeTutorial, TutorialDefinicion } from '../tipos'

/** Los filtros del centro de ayuda, tal como viven en la dirección. */
export type FiltroDeEstado = 'todos' | 'pendiente' | 'en-progreso' | 'completado' | 'obligatorio'

export interface Filtro {
  readonly texto: string
  readonly estado: FiltroDeEstado
  readonly categoria: string | null
}

/** Una tarjeta, ya resuelta: el tutorial más lo que le pasó a esta persona. */
export interface TutorialEnLista {
  readonly tutorial: TutorialDefinicion
  readonly estado: EstadoDeProgreso
  readonly fraccion: number
  readonly continuable: boolean
  readonly progreso: ProgresoDeTutorial | undefined
  readonly requisitosPendientes: readonly TutorialDefinicion[]
}

/**
 * **Lo que el centro de ayuda muestra, calculado sin pintar nada.**
 *
 * Es una función pura sobre tres entradas —catálogo, avance y filtro— así que el orden
 * de la lista y el texto de cada botón se prueban con arreglos literales, sin montar un
 * componente ni tocar el DOM.
 */
export function componerLista(
  tutoriales: readonly TutorialDefinicion[],
  progresos: ReadonlyMap<string, ProgresoDeTutorial>,
  filtro: Filtro,
): readonly TutorialEnLista[] {
  const porId = new Map(tutoriales.map((t) => [t.id, t]))
  return tutoriales
    .map<TutorialEnLista>((tutorial) => ({
      tutorial,
      estado: estadoDe(progresos.get(tutorial.id), tutorial),
      fraccion: fraccionDe(progresos.get(tutorial.id), tutorial),
      continuable: sePuedeContinuar(progresos.get(tutorial.id), tutorial),
      progreso: progresos.get(tutorial.id),
      requisitosPendientes: requisitosPendientes(tutorial, porId, progresos),
    }))
    .filter((fila) => coincide(fila, filtro))
    .sort(porPrioridad)
}

/**
 * El que conviene hacer ahora: el primero obligatorio o a medias que no tenga
 * requisitos pendientes. `undefined` cuando no queda nada por hacer, que también es una
 * respuesta.
 */
export function recomendado(lista: readonly TutorialEnLista[]): TutorialEnLista | undefined {
  return lista.find((f) => f.estado !== 'completado' && f.requisitosPendientes.length === 0)
}

function coincide(fila: TutorialEnLista, filtro: Filtro): boolean {
  if (filtro.categoria !== null && fila.tutorial.categoria !== filtro.categoria) return false
  if (!coincideEstado(fila, filtro.estado)) return false
  const texto = (filtro.texto ?? '').trim().toLowerCase()
  if (texto.length === 0) return true
  const heno = `${fila.tutorial.titulo} ${fila.tutorial.descripcion} ${fila.tutorial.categoria}`.toLowerCase()
  return texto.split(/\s+/).every((palabra) => heno.includes(palabra))
}

function coincideEstado(fila: TutorialEnLista, estado: FiltroDeEstado): boolean {
  switch (estado) {
    case 'todos':
      return true
    case 'obligatorio':
      return fila.tutorial.obligatorio === true
    case 'pendiente':
      // Omitido cuenta como pendiente: se dejó a medias y sigue sin hacerse.
      return fila.estado === 'pendiente' || fila.estado === 'omitido'
    default:
      return fila.estado === estado
  }
}

/**
 * Primero lo obligatorio, después lo que está a medias, después lo pendiente, y al
 * final lo ya hecho: el orden en el que a uno le sirve encontrarlo.
 */
function porPrioridad(a: TutorialEnLista, b: TutorialEnLista): number {
  return peso(a) - peso(b)
}

function peso(fila: TutorialEnLista): number {
  if (fila.estado === 'completado') return 4
  if (fila.tutorial.obligatorio === true) return 0
  if (fila.estado === 'en-progreso') return 1
  return fila.estado === 'omitido' ? 3 : 2
}
