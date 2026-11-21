/**
 * Lo que una persona escribe en un campo de importe → la cadena del contrato (`1240.50`).
 *
 * Es la **referencia** de los dos mundos, igual que `formatear.ts`: de acá salen los
 * vectores de `vectores/monto-escrito.json` que el `CampoMonto` de Angular y el de Flutter
 * reproducen caso por caso (F1.S4.M1 · L6 DINERO-002).
 *
 * ## Por qué existe
 *
 * El teclado numérico del teléfono muchas veces solo ofrece el punto. El parser viejo de la
 * app trataba todo punto como separador de miles y lo borraba: «12.50» viajaba como
 * `1250.00`, cien veces más. En dinero, un error así no se disculpa.
 *
 * ## La regla (formato regional es-BO: miles con punto, centavos con coma)
 *
 * - Con coma: la coma es la decimal (0 a 2 dígitos detrás); la parte entera va sin
 *   separadores o con puntos en grupos de tres. «1.250,50» → `1250.50`.
 * - Sin coma y con un solo punto seguido de 0 a 2 dígitos al final: ese punto es la
 *   decimal. «12.50» → `12.50`, «12.5» → `12.50`.
 * - Sin coma y con puntos en grupos de tres: son miles. «1.240» → `1240.00`. El campo
 *   muestra el importe interpretado al vuelo para que la persona lo vea antes de enviar.
 * - Cualquier otra cosa es ambigua y se rechaza (`null`): «1.240.50», «1,250.50» (orden
 *   inverso al regional), dos comas, signo, letras.
 *
 * Nunca se convierte a número: se separan enteros y centavos como texto.
 */

/** Parte entera válida: sin separadores, o con puntos de miles en grupos de tres. */
const ENTERO = /^(\d{1,3}(\.\d{3})+|\d+)$/

export function interpretarMontoEscrito(escrito: string): string | null {
  const texto = escrito.replace(/\s/g, '')
  if (!/^[\d.,]+$/.test(texto)) return null

  let entero: string
  let centavos: string
  const partesPorComa = texto.split(',')
  if (partesPorComa.length > 2) return null

  if (partesPorComa.length === 2) {
    const [antes = '', despues = ''] = partesPorComa
    if (!ENTERO.test(antes) || !/^\d{0,2}$/.test(despues)) return null
    entero = antes.replace(/\./g, '')
    centavos = despues
  } else if (/^\d+\.\d{0,2}$/.test(texto)) {
    const [antes = '', despues = ''] = texto.split('.')
    entero = antes
    centavos = despues
  } else if (ENTERO.test(texto)) {
    entero = texto.replace(/\./g, '')
    centavos = ''
  } else {
    return null
  }

  return `${entero.replace(/^0+(?=\d)/, '')}.${centavos.padEnd(2, '0')}`
}
