import type { EstadoDeProgreso, ProgresoDeTutorial, TutorialDefinicion } from './tipos'

/**
 * **Las cuentas del avance, sin estado y sin IO.** Todo lo que el centro de ayuda
 * muestra —porcentajes, qué se puede continuar, qué caducó por versión— sale de acá,
 * así se puede probar con un arreglo literal y sin navegador.
 */

/** El progreso recién nacido de un tutorial que alguien acaba de abrir. */
export function progresoInicial(t: TutorialDefinicion, ahora: string, repeticiones = 0): ProgresoDeTutorial {
  return {
    tutorialId: t.id,
    version: t.version,
    estado: 'en-progreso',
    pasoId: t.pasos[0]?.id ?? null,
    indice: 0,
    iniciadoEn: ahora,
    terminadoEn: null,
    ultimaInteraccion: ahora,
    repeticiones,
  }
}

/** Mueve el progreso a un paso. No decide nada: lo decide el motor. */
export function enPaso(previo: ProgresoDeTutorial, indice: number, pasoId: string | null, ahora: string): ProgresoDeTutorial {
  return { ...previo, estado: 'en-progreso', indice, pasoId, ultimaInteraccion: ahora }
}

/** Cierra el progreso con un desenlace. Completar suma una repetición. */
export function cerrado(previo: ProgresoDeTutorial, estado: Extract<EstadoDeProgreso, 'completado' | 'omitido'>, ahora: string): ProgresoDeTutorial {
  return {
    ...previo,
    estado,
    terminadoEn: ahora,
    ultimaInteraccion: ahora,
    repeticiones: estado === 'completado' ? previo.repeticiones + 1 : previo.repeticiones,
  }
}

/**
 * **Cambió la versión del tutorial: lo que había ya no vale.**
 *
 * El progreso viejo no se borra —se sigue sabiendo que esa persona lo hizo alguna vez,
 * y `repeticiones` viaja— pero deja de contar como completado: el tutorial vuelve a
 * ofrecerse. Es la estrategia conservadora a propósito: mostrar de más un tutorial que
 * cambió molesta; esconder un cambio que importa deja a alguien operando con
 * instrucciones viejas.
 */
export function vigente(p: ProgresoDeTutorial | undefined, t: TutorialDefinicion): boolean {
  return p !== undefined && p.version === t.version
}

/** El estado con el que se pinta la tarjeta, ya con la versión tenida en cuenta. */
export function estadoDe(p: ProgresoDeTutorial | undefined, t: TutorialDefinicion): EstadoDeProgreso {
  if (p === undefined) return 'pendiente'
  if (!vigente(p, t)) return 'pendiente'
  return p.estado
}

/**
 * Se puede continuar lo que quedó a medias en la versión que está corriendo, **lo haya
 * dejado a medias o lo haya abandonado a propósito**.
 *
 * Omitido también cuenta: al salir, el diálogo promete «guardamos por qué paso ibas y
 * podés retomarlo». Si después la tarjeta solo ofreciera empezar de cero, esa promesa
 * sería mentira. Quien quiera arrancar de nuevo tiene «Reiniciar».
 */
export function sePuedeContinuar(p: ProgresoDeTutorial | undefined, t: TutorialDefinicion): boolean {
  const estado = estadoDe(p, t)
  return (estado === 'en-progreso' || estado === 'omitido') && (p?.indice ?? 0) > 0
}

/** Cuánto del tutorial recorrió esta persona, de 0 a 1. */
export function fraccionDe(p: ProgresoDeTutorial | undefined, t: TutorialDefinicion): number {
  const estado = estadoDe(p, t)
  if (estado === 'completado') return 1
  if (t.pasos.length === 0 || p === undefined || estado === 'pendiente') return 0
  return Math.min(1, Math.max(0, p.indice / t.pasos.length))
}

/**
 * El avance general: tutoriales completados sobre tutoriales disponibles. Lo que se
 * omitió NO cuenta como hecho —omitir es decir «esto no lo necesito», no «ya lo sé»—
 * pero tampoco se esconde: la tarjeta sigue ahí para repetirlo.
 */
export function avanceGeneral(tutoriales: readonly TutorialDefinicion[], progresos: ReadonlyMap<string, ProgresoDeTutorial>): number {
  if (tutoriales.length === 0) return 0
  const hechos = tutoriales.filter((t) => estadoDe(progresos.get(t.id), t) === 'completado').length
  return hechos / tutoriales.length
}

/** Los requisitos que todavía le faltan a quien mira esta tarjeta. */
export function requisitosPendientes(
  t: TutorialDefinicion,
  porId: ReadonlyMap<string, TutorialDefinicion>,
  progresos: ReadonlyMap<string, ProgresoDeTutorial>,
): TutorialDefinicion[] {
  return (t.requisitos ?? [])
    .map((id) => porId.get(id))
    .filter((r): r is TutorialDefinicion => r !== undefined)
    .filter((r) => estadoDe(progresos.get(r.id), r) !== 'completado')
}
