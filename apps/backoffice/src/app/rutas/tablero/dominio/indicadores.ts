import { httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type { Indicador, SalidaTablero } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * Tableros por rol · el contrato de CU-98 (`publicarTablero`, `GET /indicadores`, auditoria.yaml), con los tipos del
 * cliente generado.
 * Los valores llegan como CADENA decimal: pasarlos a `number` los volvería un doble en el navegador y el número del
 * tablero dejaría de ser el de la tabla. El servidor ya filtra las familias por permiso (TABLERO_FINANCIERO_VER,
 * TABLERO_COMERCIAL_VER, TABLERO_SISTEMAS_VER); acá solo se ordena lo que llegó.
 */
/** Del cliente generado desde auditoria.yaml: si el contrato cambia, esto deja de compilar en vez de divergir. */
export type IndicadorPublicado = Indicador
export type { SalidaTablero }

export type Granularidad = 'dia' | 'mes' | 'anio'

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

/** Los pasos del embudo comercial, en orden. Cada servicio cuenta sus hechos del período: no es una cohorte. */
export const EMBUDO: readonly Esperado[] = [
  { codigo: 'USUARIOS_NUEVOS', nombre: 'Se registraron', grupo: 'Embudo' },
  { codigo: 'EMBUDO_KYC_APROBADO', nombre: 'Aprobaron su identidad', grupo: 'Embudo' },
  { codigo: 'EMBUDO_EN_GRUPO', nombre: 'Entraron a su primer grupo', grupo: 'Embudo' },
  { codigo: 'EMBUDO_PRIMER_APORTE', nombre: 'Pagaron su primer aporte', grupo: 'Embudo' },
]

/**
 * El período que el tablero abre por omisión: el último CERRADO de esa granularidad (ayer, el mes pasado, el año
 * pasado). El día de hoy, el mes y el año en curso todavía no tienen valor guardado (ambigüedad A7 del trabajo).
 */
export function periodoCerrado(hoy: Date, granularidad: Granularidad): string {
  const anio = hoy.getFullYear()
  const mes = hoy.getMonth()
  if (granularidad === 'anio') return String(anio - 1)
  if (granularidad === 'mes') {
    const d = new Date(anio, mes - 1, 1)
    return `${d.getFullYear()}-${dos(d.getMonth() + 1)}`
  }
  const ayer = new Date(anio, mes, hoy.getDate() - 1)
  return `${ayer.getFullYear()}-${dos(ayer.getMonth() + 1)}-${dos(ayer.getDate())}`
}

function dos(n: number): string {
  return String(n).padStart(2, '0')
}

/** Lo que una tarjeta muestra. `estado` decide la rama; nunca hay un número inventado. */
export type Tarjeta = {
  codigo: string
  nombre: string
  grupo: string
  estado: 'valor' | 'suprimido' | 'sinDatos'
  valor?: string
  /** Código ISO de la moneda cuando el indicador es dinero (la unidad es la moneda). */
  moneda?: string
  /** El valor ya con su unidad para lo que no es dinero: «23,33 %», «4,5 días», «2,10 veces», «12». */
  texto?: string
  variacion?: string
  casos?: number
  minimoCasos?: number
}

const MONEDAS = new Set(['BOB', 'USD'])

export function tarjetas(salida: SalidaTablero | undefined, esperados: readonly Esperado[]): Tarjeta[] {
  const porCodigo = new Map((salida?.indicadores ?? []).map((i) => [i.codigo, i]))
  return esperados.map((e) => {
    const i = porCodigo.get(e.codigo)
    if (!i) return { codigo: e.codigo, nombre: e.nombre, grupo: e.grupo, estado: 'sinDatos' }
    if (i.suprimidoPorPrivacidad || i.valor === undefined) {
      return { codigo: e.codigo, nombre: e.nombre, grupo: e.grupo, estado: 'suprimido', minimoCasos: i.minimoCasos }
    }
    const esDinero = MONEDAS.has(i.unidad)
    const exacto = esDinero ? montoExacto(i.valor) : undefined
    return {
      codigo: e.codigo,
      nombre: e.nombre,
      grupo: e.grupo,
      estado: 'valor',
      valor: exacto ?? i.valor,
      // `ap-monto` exige dos decimales; si el valor trae decimales significativos de mas, no se redondea en silencio
      // (regla 91): se muestra como texto con la moneda al lado.
      moneda: exacto ? i.unidad : undefined,
      texto: exacto ? undefined : esDinero ? `${decimal(i.valor)} ${i.unidad}` : conUnidad(i.valor, i.unidad),
      variacion: variacionEnPalabras(i.variacionPeriodoAnterior),
      casos: i.casos,
    }
  })
}

/**
 * `indicador_kpi.valor` es NUMERIC(16,4): un importe llega como «1500.0000». El esquema `Dinero` de `ap-monto` pide
 * exactamente dos decimales. Se recortan SOLO ceros; si sobra un decimal distinto de cero, no hay importe exacto.
 */
export function montoExacto(valor: string): string | undefined {
  const m = /^(-?\d+)(?:\.(\d{0,2})(\d*))?$/.exec(valor)
  if (!m) return undefined
  const [, entero, dos = '', resto = ''] = m
  if (/[1-9]/.test(resto)) return undefined
  return `${entero}.${dos.padEnd(2, '0')}`
}

/** Decimal con coma de Bolivia, sin pasar por `number` para no perder exactitud. */
export function decimal(valor: string): string {
  const [entero, fraccion] = valor.split('.')
  const conMiles = entero!.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  if (!fraccion) return conMiles
  const recortada = fraccion.replace(/0+$/, '')
  return recortada ? `${conMiles},${recortada}` : conMiles
}

export function conUnidad(valor: string, unidad: string): string {
  const n = decimal(valor)
  switch (unidad) {
    case 'PORCENTAJE':
      return `${n} %`
    case 'DIAS':
      return `${n} días`
    case 'VECES':
      return `${n} veces`
    case 'MILISEGUNDOS':
      return `${n} ms`
    default:
      return n
  }
}

/** «12,5 % más que el período anterior» / «sin comparación»: qué pasó, no una flecha. */
export function variacionEnPalabras(variacion: string | undefined): string {
  if (variacion === undefined) return 'Sin comparación con el período anterior'
  const negativa = variacion.startsWith('-')
  const absoluta = decimal(negativa ? variacion.slice(1) : variacion)
  if (/^0*(\.0*)?$/.test(negativa ? variacion.slice(1) : variacion)) return 'Igual que el período anterior'
  return `${absoluta} % ${negativa ? 'menos' : 'más'} que el período anterior`
}

/** Los pasos del embudo con su ancho relativo al primero (0-100). El texto del número va siempre al lado. */
export function embudo(salida: SalidaTablero | undefined): { nombre: string; texto: string; ancho: number }[] {
  const porCodigo = new Map((salida?.indicadores ?? []).map((i) => [i.codigo, i]))
  const valores = EMBUDO.map((p) => {
    const i = porCodigo.get(p.codigo)
    return i && !i.suprimidoPorPrivacidad && i.valor !== undefined ? i.valor : undefined
  })
  const base = valores[0] !== undefined ? Number(valores[0]) : 0
  return EMBUDO.map((p, k) => {
    const v = valores[k]
    if (v === undefined) return { nombre: p.nombre, texto: 'Sin datos', ancho: 0 }
    // Para el ANCHO de la barra un conteo entero cabe sin pérdida en `number`; el texto sigue saliendo de la cadena.
    const ancho = base > 0 ? Math.min(100, Math.round((Number(v) / base) * 100)) : 0
    return { nombre: p.nombre, texto: decimal(v), ancho }
  })
}

/** El tablero del período, una consulta; el servidor devuelve solo las familias del permiso de quien pregunta. */
export function recursoTablero(periodo: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaTablero>(() => ({
    url: `${gateway}/indicadores`,
    params: { periodo: periodo(), dimension: 'GLOBAL', periodosDeSerie: 12 },
  }))
}
