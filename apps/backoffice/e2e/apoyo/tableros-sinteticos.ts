import { readFileSync } from 'node:fs'
import path from 'node:path'
import type { Page, Route } from '@playwright/test'

/**
 * **DOBLE DE PRUEBA declarado (regla 65).** Las respuestas de `auditoria` para la prueba visual de los tableros: la
 * pila real no se puede levantar en esta máquina sin pisar la de otra sesión (mismos nombres de contenedor). Las cifras
 * son SINTÉTICAS y redondas a propósito; nada acá es un dato real ni un catálogo oficial. El catálogo de datos sí es el
 * real: el JSON que genera `scripts/catalogo_de_datos.py` (estructura, no datos de personas).
 */

const AHORA = '2026-10-17T06:15:00Z'

type Ind = { codigo: string; valor?: string; unidad: string; familia: string; suprimido?: boolean; variacion?: string }

function indicador(i: Ind) {
  return {
    codigo: i.codigo,
    nombre: i.codigo,
    unidad: i.unidad,
    familia: i.familia,
    duenoFamilia: 'Responsable sintético',
    definicionVersion: 'v1',
    suprimidoPorPrivacidad: i.suprimido ?? false,
    ...(i.suprimido ? { minimoCasos: 5 } : { valor: i.valor, casos: 120 }),
    ...(i.variacion ? { variacionPeriodoAnterior: i.variacion } : {}),
    calculadoEn: AHORA,
  }
}

const salida = (indicadores: Ind[]) => ({ periodo: '2026-09', provisorio: false, dimension: 'GLOBAL', indicadores: indicadores.map(indicador) })

export const FINANCIERO = salida([
  { codigo: 'SALDO_CARGADO', valor: '184500.0000', unidad: 'BOB', familia: 'FINANZAS', variacion: '12.50' },
  { codigo: 'PENDIENTES_COBRADOS_PROPIO', valor: '7350.0000', unidad: 'BOB', familia: 'FINANZAS', variacion: '-4.00' },
  { codigo: 'SALDO_RETIRADO', valor: '96200.0000', unidad: 'BOB', familia: 'FINANZAS' },
  { codigo: 'SALDO_INVERTIDO', valor: '15000.0000', unidad: 'BOB', familia: 'FINANZAS', variacion: '0.00' },
  { codigo: 'DINERO_EN_MORA', valor: '4200.0000', unidad: 'BOB', familia: 'RIESGO' },
  { codigo: 'TASA_MORA', valor: '3.4500', unidad: 'PORCENTAJE', familia: 'RIESGO' },
  { codigo: 'IMPAGOS_CUBIERTOS', unidad: 'BOB', familia: 'RIESGO', suprimido: true },
  { codigo: 'INTERESES_MORA_COBRADOS', valor: '310.5000', unidad: 'BOB', familia: 'FINANZAS' },
  { codigo: 'COMISIONES_COBRADAS', valor: '2750.0000', unidad: 'BOB', familia: 'FINANZAS' },
  { codigo: 'TURNOS_NORMALES', valor: '48', unidad: 'CONTEO', familia: 'FINANZAS' },
  { codigo: 'TURNOS_CIERRE_CICLO', valor: '6', unidad: 'CONTEO', familia: 'FINANZAS' },
  { codigo: 'TIEMPO_TURNO_DESEMBOLSO', valor: '1.5000', unidad: 'DIAS', familia: 'FINANZAS' },
])

export const COMERCIAL = salida([
  { codigo: 'USUARIOS_NUEVOS', valor: '420', unidad: 'CONTEO', familia: 'NEGOCIO', variacion: '8.00' },
  { codigo: 'USUARIOS_ACTIVOS', valor: '3180', unidad: 'CONTEO', familia: 'NEGOCIO' },
  { codigo: 'EMBUDO_KYC_APROBADO', valor: '310', unidad: 'CONTEO', familia: 'NEGOCIO' },
  { codigo: 'EMBUDO_EN_GRUPO', valor: '190', unidad: 'CONTEO', familia: 'NEGOCIO' },
  { codigo: 'EMBUDO_PRIMER_APORTE', valor: '150', unidad: 'CONTEO', familia: 'NEGOCIO' },
  { codigo: 'GRUPOS_NUEVOS', valor: '36', unidad: 'CONTEO', familia: 'NEGOCIO' },
  { codigo: 'DIAS_LLENADO_GRUPO', valor: '4.2000', unidad: 'DIAS', familia: 'NEGOCIO' },
  { codigo: 'VOLUMEN_COMPROMETIDO', valor: '612000.0000', unidad: 'BOB', familia: 'NEGOCIO' },
])

export const SISTEMAS = salida([
  { codigo: 'SOLICITUDES_TOTALES', valor: '1284300', unidad: 'CONTEO', familia: 'OPERACION' },
  { codigo: 'SOLICITUDES_EXITOSAS', valor: '1270011', unidad: 'CONTEO', familia: 'OPERACION' },
  { codigo: 'SOLICITUDES_FALLIDAS_4XX', valor: '13900', unidad: 'CONTEO', familia: 'OPERACION' },
  { codigo: 'SOLICITUDES_FALLIDAS_5XX', valor: '389', unidad: 'CONTEO', familia: 'OPERACION' },
  { codigo: 'TASA_ERROR', valor: '0.0300', unidad: 'PORCENTAJE', familia: 'OPERACION' },
  { codigo: 'LATENCIA_P50', valor: '42.0000', unidad: 'MILISEGUNDOS', familia: 'OPERACION' },
  { codigo: 'LATENCIA_P95', valor: '187.5000', unidad: 'MILISEGUNDOS', familia: 'OPERACION' },
  { codigo: 'USUARIOS_ACTIVOS_HOY', valor: '912', unidad: 'CONTEO', familia: 'OPERACION' },
])

export const ENDPOINTS = {
  desde: '2026-10-17T04:00:00Z',
  hasta: '2026-10-17T18:00:00Z',
  fuenteDisponible: true,
  endpoints: [
    { servicio: 'nucleo-financiero', metodo: 'GET', ruta: '/billetera/{cuentaId}/saldo', solicitudes: 48210, errores4xx: 31, errores5xx: 0, p95Ms: '38.20' },
    { servicio: 'aportes', metodo: 'POST', ruta: '/aportes/obligaciones/{obligacionId}/pagos', solicitudes: 9120, errores4xx: 120, errores5xx: 3, p95Ms: '210.40' },
    { servicio: 'grupos', metodo: 'GET', ruta: '/grupos/{grupoId}', solicitudes: 7340, errores4xx: 12, errores5xx: 0 },
  ],
}

export const CATALOGO = JSON.parse(
  readFileSync(path.resolve(__dirname, '../../../../servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json'), 'utf8'),
)

export const VACIO = { periodo: '2026-09', provisorio: false, dimension: 'GLOBAL', indicadores: [] }

export type Respuesta = { cuerpo?: unknown; estado?: number; colgar?: boolean }

/** Sirve `cuerpo` (o un error) a la ruta de auditoria pedida; `colgar` la deja sin contestar para capturar «cargando». */
export async function servir(page: Page, ruta: '/indicadores' | '/indicadores/operacion/endpoints' | '/indicadores/catalogo-de-datos', r: Respuesta) {
  await page.route(
    (url) => url.pathname.endsWith(`/api/v1${ruta}`),
    async (route: Route) => {
      if (r.colgar) return
      await route.fulfill({ status: r.estado ?? 200, contentType: 'application/json', body: JSON.stringify(r.cuerpo ?? { codigo: 'AP-ERROR', mensaje: 'detalle interno' }) })
    },
  )
}
