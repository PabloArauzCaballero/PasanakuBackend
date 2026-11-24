import type { VersionDeParametro } from '../dominio/cu132-parametros'
import type { Tono } from '@aportaya/ui/tono/tono'

export type CampoDePropuesta = 'valor' | 'nivel' | 'moneda' | 'vigenteDesde' | 'fuente' | 'referencia' | 'fecha'
export type Borrador = Record<CampoDePropuesta, string>
export type Historial = Record<string, VersionDeParametro[]>
export type ErroresDeCampo = Partial<Record<CampoDePropuesta, string>>

export const BORRADOR_VACIO: Borrador = {
  valor: '',
  nivel: '',
  moneda: '',
  vigenteDesde: '',
  fuente: '',
  referencia: '',
  fecha: '',
}
export const VALOR_DECIMAL = /^\d{1,10}(\.\d{1,6})?$/
export const TONO: Record<VersionDeParametro['estado'], Tono> = { PROPUESTA: 'aviso', APROBADA: 'ok', RECHAZADA: 'neutro' }

type TextosDeValidacion = { valorInvalido: string; nivelInvalido: string; monedaRequerida: string; campoRequerido: string; fechaInvalida: string }
type FormaDelParametro = { porNivel: boolean; unidad?: string | null }

/** Las validaciones locales de una propuesta, campo por campo. El servidor las repite: esto solo evita el viaje. */
export function validarBorrador(p: FormaDelParametro, b: Borrador, t: TextosDeValidacion): ErroresDeCampo {
  const errores: ErroresDeCampo = {}
  if (!VALOR_DECIMAL.test(b['valor'].trim())) errores['valor'] = t.valorInvalido
  if (p.porNivel && (!b['nivel'].trim() || b['nivel'].trim().length > 30)) errores['nivel'] = t.nivelInvalido
  if (p.unidad === 'MONTO' && !['BOB', 'USD'].includes(b['moneda'])) errores['moneda'] = t.monedaRequerida
  if (!b['vigenteDesde'] || Number.isNaN(Date.parse(b['vigenteDesde']))) errores['vigenteDesde'] = t.campoRequerido
  if (!b['fuente'].trim() || b['fuente'].trim().length > 200) errores['fuente'] = t.campoRequerido
  if (!b['referencia'].trim() || b['referencia'].trim().length > 300) errores['referencia'] = t.campoRequerido
  if (!/^\d{4}-\d{2}-\d{2}$/.test(b['fecha']) || Number.isNaN(Date.parse(`${b['fecha']}T00:00:00Z`))) errores['fecha'] = t.fechaInvalida
  return errores
}
