import type { ErrorTraducido } from '../../../nucleo/errores'

/**
 * A qué campo pertenece un error de negocio de `/cuenta/*` y qué se le dice a la persona. El texto del backend no se
 * muestra nunca (95.2.3): cada código del contrato tiene su mensaje accionable acá. `sinConfirmar` marca el «no sabemos
 * si llegó» (sin conexión, plazo vencido, 5xx): lo escrito se conserva y reintentar es seguro.
 */
export type CampoDeCuenta = 'claveNueva' | 'claveActual' | 'codigo'
export type ErrorDeCuenta = {
  mensaje: string
  campo?: CampoDeCuenta
  /** El token no sirve (vencido, usado, falso u otro propósito): el contrato no distingue y esta capa tampoco. */
  tokenInvalido?: boolean
  /** El enrolamiento pendiente ya no existe: hay que generar el QR de nuevo. */
  factorPerdido?: boolean
  sinConfirmar?: boolean
  trazaId?: string
}

export const MENSAJE_TOKEN_INVALIDO =
  'Este enlace ya no sirve: pudo vencer, haberse usado o no ser válido. Pedí una invitación nueva a quien administra los accesos, o recuperá tu acceso.'

const POR_CODIGO: Readonly<Record<string, Omit<ErrorDeCuenta, 'mensaje'> & { mensaje: string }>> = {
  'AP-CU09-01': { campo: 'claveActual', mensaje: 'La clave actual no coincide. Revisala y probá de nuevo.' },
  'AP-CU09-02': { campo: 'claveNueva', mensaje: 'Esa clave no sirve: usá una más larga y que no contenga tu teléfono ni tu documento.' },
  'AP-CU09-03': { campo: 'claveNueva', mensaje: 'Esa clave ya la usaste antes. Elegí una distinta de las anteriores.' },
  'AP-CU09-04': { tokenInvalido: true, mensaje: MENSAJE_TOKEN_INVALIDO },
  'AP-CU09-09': { mensaje: 'No podés decidir tu propia solicitud. La tiene que decidir otra persona con este mismo permiso.' },
  'AP-CU09-11': { mensaje: 'Esa solicitud ya no existe. Actualizá la bandeja.' },
  'AP-CU09-12': { mensaje: 'Esa solicitud ya fue decidida por otra persona. Actualizá la bandeja para ver cómo quedó.' },
  // El token NO se consume: la invitación sigue vigente y se puede reintentar con el código siguiente.
  'AP-CU04-04': { campo: 'codigo', mensaje: 'Ese código no coincide o ya venció. Esperá el siguiente de tu aplicación y probá de nuevo: tu invitación sigue vigente.' },
  'AP-CU04-08': { factorPerdido: true, mensaje: 'El código QR que generaste ya no sirve. Generá uno nuevo y volvé a escanearlo.' },
}

const SIN_CONFIRMAR = 'No pudimos confirmar si se completó. Lo que escribiste sigue acá: probá de nuevo en un momento.'
const GENERICO = 'Algo salió mal de nuestro lado. Lo que escribiste sigue acá: probá de nuevo en un momento.'

export function errorDeCuenta(e: ErrorTraducido): ErrorDeCuenta {
  const conocido = e.codigo ? POR_CODIGO[e.codigo] : undefined
  if (conocido) return { ...conocido, trazaId: e.trazaId }
  const estado = e.estado ?? 0
  if (e.sinConexion || e.tiempoAgotado || estado === 0 || estado >= 500) return { mensaje: SIN_CONFIRMAR, sinConfirmar: true, trazaId: e.trazaId }
  if (estado === 429) return { mensaje: 'Hiciste demasiados intentos seguidos. Esperá unos minutos antes de volver a probar.', trazaId: e.trazaId }
  if (estado === 403) return { mensaje: 'No tenés permiso para hacer esto.', trazaId: e.trazaId }
  return { mensaje: GENERICO, trazaId: e.trazaId }
}
