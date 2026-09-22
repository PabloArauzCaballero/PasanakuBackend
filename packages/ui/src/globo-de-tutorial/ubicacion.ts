import type { RecuadroResaltado } from '../foco-de-tutorial/foco-de-tutorial'

export type LadoDelGlobo = 'arriba' | 'abajo' | 'izquierda' | 'derecha' | 'centro'

export interface Medida {
  readonly ancho: number
  readonly alto: number
}

export interface Ubicacion {
  readonly izquierda: number
  readonly arriba: number
  readonly lado: LadoDelGlobo
}

/** Aire entre el elemento y el globo, y entre el globo y el borde de la ventana. */
const SEPARACION = 14
const MARGEN = 12

/**
 * **Dónde plantar el globo.** Función pura: entra el recuadro del elemento, el lado
 * preferido y el tamaño de la ventana; sale una posición. Sin DOM, así se prueba con
 * números y se sabe por qué quedó donde quedó.
 *
 * El lado preferido se respeta si entra. Si no entra, se prueba el opuesto, y si
 * ninguno entra el globo va centrado: **antes tapar el elemento que dejar el texto
 * cortado fuera de la pantalla**, porque un texto que no se lee no enseña nada.
 */
export function ubicar(recuadro: RecuadroResaltado | null, preferido: LadoDelGlobo, ventana: Medida, globo: Medida): Ubicacion {
  if (recuadro === null || preferido === 'centro') return centrado(ventana, globo)
  const candidatos: LadoDelGlobo[] = [preferido, opuesto(preferido), ...OTROS]
  for (const lado of candidatos) {
    const intento = plantar(recuadro, lado, globo)
    if (entra(intento, ventana, globo)) return { ...intento, izquierda: acotar(intento.izquierda, ventana.ancho, globo.ancho), arriba: acotar(intento.arriba, ventana.alto, globo.alto) }
  }
  return centrado(ventana, globo)
}

const OTROS: LadoDelGlobo[] = ['abajo', 'arriba', 'derecha', 'izquierda']

function plantar(r: RecuadroResaltado, lado: LadoDelGlobo, globo: Medida): Ubicacion {
  const centroX = r.x + r.ancho / 2 - globo.ancho / 2
  const centroY = r.y + r.alto / 2 - globo.alto / 2
  switch (lado) {
    case 'arriba':
      return { izquierda: centroX, arriba: r.y - globo.alto - SEPARACION, lado }
    case 'abajo':
      return { izquierda: centroX, arriba: r.y + r.alto + SEPARACION, lado }
    case 'izquierda':
      return { izquierda: r.x - globo.ancho - SEPARACION, arriba: centroY, lado }
    default:
      return { izquierda: r.x + r.ancho + SEPARACION, arriba: centroY, lado: 'derecha' }
  }
}

/**
 * ¿Entra de ese lado **sin pisar el elemento**?
 *
 * Arriba y abajo se juzgan por lo vertical: el centrado horizontal después se recorta
 * contra el borde y eso nunca lo hace pisar. De costado es al revés. Mezclar las dos
 * medidas era el error: un globo que sobresalía media pantalla por la izquierda pasaba
 * el examen, después se recortaba contra el borde y terminaba encima de lo que estaba
 * señalando.
 */
function entra(u: Ubicacion, ventana: Medida, globo: Medida): boolean {
  const cabeVertical = u.arriba >= MARGEN && u.arriba + globo.alto <= ventana.alto - MARGEN
  const cabeHorizontal = u.izquierda >= MARGEN && u.izquierda + globo.ancho <= ventana.ancho - MARGEN
  return u.lado === 'arriba' || u.lado === 'abajo' ? cabeVertical : cabeHorizontal
}

function centrado(ventana: Medida, globo: Medida): Ubicacion {
  return { izquierda: Math.max(MARGEN, (ventana.ancho - globo.ancho) / 2), arriba: Math.max(MARGEN, (ventana.alto - globo.alto) / 2), lado: 'centro' }
}

function acotar(valor: number, disponible: number, propio: number): number {
  return Math.min(Math.max(valor, MARGEN), Math.max(MARGEN, disponible - propio - MARGEN))
}

function opuesto(lado: LadoDelGlobo): LadoDelGlobo {
  switch (lado) {
    case 'arriba':
      return 'abajo'
    case 'abajo':
      return 'arriba'
    case 'izquierda':
      return 'derecha'
    case 'derecha':
      return 'izquierda'
    default:
      return 'centro'
  }
}
