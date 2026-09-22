import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { cerrarUnPeriodo, mirarLosServicios } from './administracion'
import { resolverUnExpediente } from './cumplimiento'
import { atenderUnReclamo, mirarUnaBilletera } from './operacion'
import { introduccionALaPlataforma, navegacionPrincipal, tuSesionYTuRol, usarElCentroDeAyuda } from './primeros-pasos'
import { crearUnaCampana, tablasYFiltros } from './publicidad'

/**
 * **El catálogo completo.** Agregar un tutorial es: escribir su constante en el archivo
 * de su módulo y sumarla a este arreglo. Nada más — ni el motor, ni el registro, ni el
 * centro de ayuda saben que existe.
 *
 * El orden es el orden en que se ofrecen cuando nada los separa: primero lo que sirve
 * a cualquiera, después cada módulo.
 */
export const CATALOGO_DE_TUTORIALES: readonly TutorialDefinicion[] = [
  introduccionALaPlataforma,
  navegacionPrincipal,
  tuSesionYTuRol,
  usarElCentroDeAyuda,
  mirarUnaBilletera,
  atenderUnReclamo,
  resolverUnExpediente,
  crearUnaCampana,
  tablasYFiltros,
  cerrarUnPeriodo,
  mirarLosServicios,
]
