import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { calcularUnPlazo, cotizarUnaComision, verificarSinCuenta } from './herramientas'
import { comoFunciona, queEsAportaYa } from './primeros-pasos'
import { usarElCentro } from './centro'

/**
 * **El catálogo del sitio público.** Agregar un tutorial es escribir su constante en el
 * archivo de su tema y sumarla acá. Ni el motor ni el centro se enteran.
 */
export const CATALOGO_DEL_SITIO: readonly TutorialDefinicion[] = [
  queEsAportaYa,
  comoFunciona,
  cotizarUnaComision,
  calcularUnPlazo,
  verificarSinCuenta,
  usarElCentro,
]
