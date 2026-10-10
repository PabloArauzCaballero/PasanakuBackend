import { existsSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * H4.S1.M6 · lado cliente: **un QR bancario no se afirma interoperable ni se procesa como interno**.
 *
 * Qué se comprueba, en tres capas (tipos, rutas, mensajes), sin red y sin backend:
 *  1. TIPOS: el contrato de `nucleo-financiero` solo conoce la clase `INTERNO`; no existe un tipo de respuesta
 *     «interoperable» que un cliente pueda llegar a pintar. El rechazo `AP-CU12-10` está declarado en `leerQr` y `pagarQr`.
 *  2. RUTAS: el único recurso de QR que hay en el contrato es `/billetera/qr*`; el contenido viaja en el cuerpo, nunca en la URL.
 *  3. MENSAJES: el cliente móvil (Dart) y el simulado traducen `AP-CU12-10` con un texto que dice que NO está habilitado,
 *     distinto del de «QR inválido» (`AP-CU12-07`), y que no promete que se pueda pagar.
 *
 * No cubierto: ejecutar los widgets de Flutter (no está en el guardián de RAM); la prueba Dart hermana vive en
 * `apps/movil/test/unidad/errores_qr_test.dart` y queda sin ejecutar acá.
 */
const RAIZ = resolve(__dirname, '../../..')
const GENERADO = resolve(__dirname, '../generado')
const EJEMPLOS = resolve(__dirname, '../ejemplos/nucleo-financiero')

type Esquema = { properties?: Record<string, { enum?: string[] }> }
const contrato = JSON.parse(readFileSync(join(GENERADO, 'nucleo-financiero.json'), 'utf8')) as {
  paths: Record<string, Record<string, { operationId?: string; description?: string; responses?: Record<string, unknown> }>>
  components: { schemas: Record<string, Esquema> }
}
const ejemplo = (op: string) =>
  JSON.parse(readFileSync(join(EJEMPLOS, `${op}.json`), 'utf8')) as { escenarios: Record<string, { estado: number; cuerpo: Record<string, unknown> }> }

describe('QR interno vs interoperable · tipos', () => {
  it('la lectura de un QR solo puede responder la clase INTERNO', () => {
    expect(contrato.components.schemas['SalidaQrLeido']?.properties?.['clase']?.enum).toEqual(['INTERNO'])
  })

  it('ningún esquema de nucleo-financiero define una clase o un canal interoperable que el cliente pueda pintar', () => {
    const nombres = Object.keys(contrato.components.schemas).filter((n) => /interoper/i.test(n))
    expect(nombres).toEqual([])
    const enums = Object.values(contrato.components.schemas).flatMap((e) => Object.values(e.properties ?? {}).flatMap((p) => p.enum ?? []))
    expect(enums.filter((v) => /INTEROPER/i.test(v))).toEqual([])
  })

  it('los ejemplos de lectura y emisión del simulado nunca devuelven otra clase que INTERNO', () => {
    for (const op of ['leerQr', 'emitirQr']) {
      for (const [nombre, e] of Object.entries(ejemplo(op).escenarios)) {
        if (e.estado >= 200 && e.estado < 300 && 'clase' in e.cuerpo) expect(e.cuerpo['clase'], `${op}/${nombre}`).toBe('INTERNO')
      }
    }
  })
})

describe('QR interno vs interoperable · rutas', () => {
  const rutasQr = Object.keys(contrato.paths).filter((r) => /qr/i.test(r)).sort()

  it('las únicas rutas de QR son emisión, lectura y pago, bajo /billetera/qr', () => {
    expect(rutasQr).toEqual(['/billetera/qr', '/billetera/qr/lecturas', '/billetera/qr/pagos'])
  })

  it('el contenido del QR viaja en el cuerpo: ninguna ruta de QR lleva el contenido como parámetro de la URL', () => {
    expect(rutasQr.filter((r) => /\{/.test(r))).toEqual([])
  })

  it('leerQr y pagarQr declaran el rechazo AP-CU12-10 (QR_INTEROPERABLE_NO_HABILITADO)', () => {
    const yaml = readFileSync(join(GENERADO, 'nucleo-financiero.yaml'), 'utf8')
    const bloque = (op: string) => yaml.slice(yaml.indexOf(`operationId: ${op}`), yaml.indexOf(`operationId: ${op}`) + 2500)
    expect(bloque('leerQr')).toContain('AP-CU12-10')
    expect(bloque('pagarQr')).toMatch(/AP-CU12-07\.\.10|AP-CU12-10/)
    expect(bloque('leerQr')).toContain('no se afirma interoperabilidad')
  })

  it('el ejemplo de rechazo de leerQr es el del QR interoperable no habilitado', () => {
    expect(ejemplo('leerQr').escenarios['rechazo']?.cuerpo['codigo']).toBe('AP-CU12-10')
    expect(ejemplo('leerQr').escenarios['rechazo']?.estado).toBe(422)
  })
})

describe('QR interno vs interoperable · mensajes del cliente', () => {
  const dart = join(RAIZ, 'apps/movil/lib/dominio/errores.dart')
  const texto = (codigo: string): string => {
    const fuente = readFileSync(dart, 'utf8')
    const i = fuente.indexOf(`'${codigo}'`)
    if (i < 0) return ''
    const fin = fuente.indexOf('\n  \'AP-', i + 1)
    return fuente.slice(i, fin < 0 ? undefined : fin).replace(/\s+/g, ' ')
  }

  it('el cliente móvil traduce los cuatro rechazos del QR, cada uno con su texto', () => {
    expect(existsSync(dart)).toBe(true)
    const textos = ['07', '08', '09', '10'].map((n) => texto(`AP-CU12-${n}`))
    for (const t of textos) expect(t.length).toBeGreaterThan(20)
    expect(new Set(textos).size).toBe(4)
  })

  it('AP-CU12-10 dice que el QR interoperable todavía no se acepta; no lo llama inválido ni promete pagarlo', () => {
    const t = texto('AP-CU12-10').toLowerCase()
    expect(t).toContain('interoperable')
    expect(t).toContain('todavía no')
    expect(t).not.toContain('no es válido')
    expect(t).not.toContain('podés pagar este qr')
  })
})
