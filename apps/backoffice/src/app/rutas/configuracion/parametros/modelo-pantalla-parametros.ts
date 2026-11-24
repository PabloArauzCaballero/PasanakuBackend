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
