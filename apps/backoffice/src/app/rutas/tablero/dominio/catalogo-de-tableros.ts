/**
 * Tableros por rol · el catálogo de lo que cada tablero espera ver, en su orden. Los códigos son los de las
 * definiciones de CU-98 (`seeders/minimos/23-indicadores-tableros.json`). Un código que no llegó se muestra «sin datos».
 */
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'

/** El ícono de cada grupo de indicadores: la tarjeta y su título lo comparten, así se reconoce el bloque de un vistazo. */
export const ICONO_DE_GRUPO: Readonly<Record<string, NombreDeIcono>> = {
  'Dinero entrante': 'aporte',
  'Dinero saliente': 'retiro',
  'Mora y fondo de garantía': 'mora',
  Ingresos: 'moneda',
  'Turnos y entregas': 'entrega',
  Personas: 'persona',
  Grupos: 'grupo',
  Tráfico: 'actividad',
  Latencia: 'reloj',
}

/** Lo que cada tablero espera ver, en su orden. Un código que no llegó se muestra «sin datos», nunca como cero. */
export type Esperado = { codigo: string; nombre: string; grupo: string }

export const ESPERADOS_FINANCIERO: readonly Esperado[] = [
  { codigo: 'SALDO_CARGADO', nombre: 'Saldo cargado', grupo: 'Dinero entrante' },
  { codigo: 'PENDIENTES_COBRADOS_PROPIO', nombre: 'Pendientes cobrados (pagó tarde)', grupo: 'Dinero entrante' },
  { codigo: 'PENDIENTES_COBRADOS_RECUPERADO', nombre: 'Recuperado de lo cubierto', grupo: 'Dinero entrante' },
  { codigo: 'SALDO_RETIRADO', nombre: 'Saldo retirado', grupo: 'Dinero saliente' },
  { codigo: 'SALDO_INVERTIDO', nombre: 'Saldo invertido en el fondo de inversión', grupo: 'Dinero saliente' },
  { codigo: 'DINERO_EN_MORA', nombre: 'Dinero en mora', grupo: 'Mora y fondo de garantía' },
  { codigo: 'TASA_MORA', nombre: 'Tasa de mora', grupo: 'Mora y fondo de garantía' },
  { codigo: 'IMPAGOS_CUBIERTOS', nombre: 'Impagos cubiertos', grupo: 'Mora y fondo de garantía' },
  { codigo: 'RECUPERACION_CUBIERTO', nombre: 'Recuperación de lo cubierto', grupo: 'Mora y fondo de garantía' },
  { codigo: 'COBERTURA_FONDO', nombre: 'Cobertura del fondo', grupo: 'Mora y fondo de garantía' },
  { codigo: 'INTERESES_MORA_COBRADOS', nombre: 'Intereses por mora cobrados', grupo: 'Ingresos' },
  { codigo: 'COMISIONES_COBRADAS', nombre: 'Comisiones cobradas', grupo: 'Ingresos' },
  { codigo: 'TURNOS_NORMALES', nombre: 'Turnos normales', grupo: 'Turnos y entregas' },
  { codigo: 'TURNOS_CIERRE_CICLO', nombre: 'Turnos que cierran ciclo', grupo: 'Turnos y entregas' },
  { codigo: 'ENTREGAS_PENDIENTES', nombre: 'Entregas pendientes', grupo: 'Turnos y entregas' },
  { codigo: 'TIEMPO_TURNO_DESEMBOLSO', nombre: 'Días del turno al desembolso', grupo: 'Turnos y entregas' },
]

export const ESPERADOS_COMERCIAL: readonly Esperado[] = [
  { codigo: 'USUARIOS_NUEVOS', nombre: 'Usuarios nuevos', grupo: 'Personas' },
  { codigo: 'USUARIOS_ACTIVOS', nombre: 'Usuarios activos', grupo: 'Personas' },
  { codigo: 'RETENCION_60D', nombre: 'Retención a 60 días', grupo: 'Personas' },
  { codigo: 'GRUPOS_NUEVOS', nombre: 'Grupos nuevos', grupo: 'Grupos' },
  { codigo: 'GRUPOS_COMPLETOS', nombre: 'Grupos completos', grupo: 'Grupos' },
  { codigo: 'DIAS_LLENADO_GRUPO', nombre: 'Días hasta llenar un grupo', grupo: 'Grupos' },
  { codigo: 'VOLUMEN_COMPROMETIDO', nombre: 'Volumen comprometido', grupo: 'Grupos' },
]

export const ESPERADOS_SISTEMAS: readonly Esperado[] = [
  { codigo: 'SOLICITUDES_TOTALES', nombre: 'Solicitudes', grupo: 'Tráfico' },
  { codigo: 'SOLICITUDES_EXITOSAS', nombre: 'Solicitudes exitosas', grupo: 'Tráfico' },
  { codigo: 'SOLICITUDES_FALLIDAS_4XX', nombre: 'Rechazadas (4xx)', grupo: 'Tráfico' },
  { codigo: 'SOLICITUDES_FALLIDAS_5XX', nombre: 'Fallidas (5xx)', grupo: 'Tráfico' },
  { codigo: 'TASA_ERROR', nombre: 'Tasa de error', grupo: 'Tráfico' },
  { codigo: 'LATENCIA_P50', nombre: 'Latencia p50', grupo: 'Latencia' },
  { codigo: 'LATENCIA_P95', nombre: 'Latencia p95', grupo: 'Latencia' },
  { codigo: 'LATENCIA_P99', nombre: 'Latencia p99', grupo: 'Latencia' },
  { codigo: 'USUARIOS_ACTIVOS_HOY', nombre: 'Usuarios activos', grupo: 'Personas' },
]

/** Los pasos del embudo comercial, en orden. Cada servicio cuenta sus hechos del período: no es una cohorte. */
export const EMBUDO: readonly Esperado[] = [
  { codigo: 'USUARIOS_NUEVOS', nombre: 'Se registraron', grupo: 'Embudo' },
  { codigo: 'EMBUDO_KYC_APROBADO', nombre: 'Aprobaron su identidad', grupo: 'Embudo' },
  { codigo: 'EMBUDO_EN_GRUPO', nombre: 'Entraron a su primer grupo', grupo: 'Embudo' },
  { codigo: 'EMBUDO_PRIMER_APORTE', nombre: 'Pagaron su primer aporte', grupo: 'Embudo' },
]
