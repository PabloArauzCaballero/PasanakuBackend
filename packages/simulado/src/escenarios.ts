/**
 * Los escenarios que toda pantalla tiene que saber mostrar, y cómo se eligen.
 *
 * En el simulado el escenario se elige por petición con la cabecera `Prefer`: el ejemplo va
 * por nombre y, cuando el escenario responde con otro código que el 2xx, también el
 * código (`Prefer: code=401, example=rechazo`). En las pruebas de componente y widget
 * se elige cargando el ejemplo con ese nombre del archivo
 * `ejemplos/<servicio>/<operationId>.json`. Es el mismo JSON en los dos casos: lo que ve
 * la pantalla en desarrollo es lo que prueba el test.
 */
export const ESCENARIOS = ['ok', 'vacio', 'aceptado', 'intermitente', 'rechazo', 'adverso'] as const
export type Escenario = (typeof ESCENARIOS)[number]

/** La cabecera que entiende el simulado. `estado` sale del archivo de ejemplos. */
export function cabeceraDeEscenario(escenario: Escenario, estado?: number): Record<string, string> {
  const partes = [`example=${escenario}`]
  if (estado !== undefined && (estado < 200 || estado >= 300)) partes.unshift(`code=${estado}`)
  return { Prefer: partes.join(', ') }
}
