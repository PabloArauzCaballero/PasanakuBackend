import type { Movimiento } from '../fila-de-movimiento/fila-de-movimiento'
import type { Turno } from '../riel-de-turnos/riel-de-turnos'
import type { Etapa } from '../escalera-de-etapas/escalera-de-etapas'
import type { Requisito } from '../lista-de-requisitos/lista-de-requisitos'
import type { Factor } from '../panel-de-factores/panel-de-factores'
import type { PasoDeSorteo } from '../verificador-de-sorteo/verificador-de-sorteo'
import type { Filtro } from '../chips-de-filtro/chips-de-filtro'

/** Datos fijos del catálogo. «Hoy» es fijo para que las capturas no cambien con el calendario. */
export const HOY = '2026-09-10T15:00:00Z'

export const MOVIMIENTOS: Movimiento[] = [
  { id: '1', tipo: 'aporte', concepto: 'Aporte al grupo Las Vecinas', monto: '-250.00', moneda: 'BOB', fechaIso: '2026-09-10T13:05:00Z', saldoCorrido: '990.00' },
  { id: '2', tipo: 'recarga', concepto: 'Recarga por QR', monto: '500.00', moneda: 'BOB', fechaIso: '2026-09-10T09:40:00Z' },
  { id: '3', tipo: 'entrega', concepto: 'Entrega del turno 4', monto: '2400.00', moneda: 'BOB', fechaIso: '2026-09-09T18:00:00Z', saldoCorrido: '740.00' },
  { id: '4', tipo: 'mora', concepto: 'Mora por 2 días', monto: '-12.50', moneda: 'BOB', fechaIso: '2026-09-07T12:00:00Z', saldoCorrido: '-1660.00' },
]

export const TURNOS: Turno[] = [
  { numero: 1, nombre: 'Rosa Mamani', estado: 'entregado' },
  { numero: 2, nombre: 'Julio Quispe', estado: 'entregado' },
  { numero: 3, nombre: 'Ana Flores', estado: 'actual' },
  { numero: 4, nombre: 'Vos', estado: 'pendiente', esMio: true },
  { numero: 5, nombre: 'Carlos Choque', estado: 'pendiente' },
]

export const ETAPAS: Etapa[] = [
  { nombre: 'Recordatorio', desdeIso: '2026-09-05T12:00:00Z', descripcion: 'Aviso amable el día del vencimiento.' },
  { nombre: 'Mora', desdeIso: '2026-09-07T12:00:00Z', descripcion: 'Se suma el recargo por día, según el reglamento del grupo.' },
  { nombre: 'Garantía', descripcion: 'A los 10 días se cubre con el fondo de garantía.' },
  { nombre: 'Cobranza', descripcion: 'Pasa a gestión de cobro.' },
]

export const REQUISITOS: Requisito[] = [
  { id: 'ci', nombre: 'Documento de identidad', estado: 'cumplido' },
  { id: 'selfie', nombre: 'Selfie con el documento', estado: 'observado', nota: 'La foto salió movida.', accion: 'Volver a tomar' },
  { id: 'cuenta', nombre: 'Cuenta bancaria', estado: 'pendiente', accion: 'Agregar cuenta' },
]

export const FACTORES: Factor[] = [
  { nombre: 'Historial de pagos', peso: 'aFavor', explicacion: '14 aportes seguidos al día.' },
  { nombre: 'Turno pedido', peso: 'enContra', explicacion: 'Pide el turno 1: es el de más riesgo para el grupo.' },
  { nombre: 'Antigüedad', peso: 'neutro', explicacion: 'Menos de un año en la plataforma.' },
]

export const PASOS_DE_SORTEO: PasoDeSorteo[] = [
  { nombre: 'Semilla comprometida', hash: 'a3f9…c21e', cuandoIso: '2026-09-01T12:00:00Z' },
  { nombre: 'Semilla revelada', hash: '7b40…9d1a', cuandoIso: '2026-09-02T12:00:00Z' },
  { nombre: 'Orden resultante', hash: 'e1d2…44b0', cuandoIso: '2026-09-02T12:00:05Z' },
]

export const FILTROS: Filtro[] = [
  { valor: 'aporte', texto: 'Aportes', icono: 'aporte' },
  { valor: 'entrega', texto: 'Entregas', icono: 'entrega' },
  { valor: 'recarga', texto: 'Recargas', icono: 'recarga' },
  { valor: 'retiro', texto: 'Retiros', icono: 'retiro' },
  { valor: 'mora', texto: 'Moras', icono: 'mora' },
]

export type FilaDeTabla = { id: string; grupo: string; cupos: number; aporte: string; estado: string }

export const FILAS_DE_TABLA: FilaDeTabla[] = [
  { id: 'g1', grupo: 'Las Vecinas', cupos: 12, aporte: '250.00', estado: 'Al día' },
  { id: 'g2', grupo: 'Taller Sur', cupos: 8, aporte: '500.00', estado: 'Atrasado' },
  { id: 'g3', grupo: 'Feria 16', cupos: 10, aporte: '100.00', estado: 'Cerrado' },
]
