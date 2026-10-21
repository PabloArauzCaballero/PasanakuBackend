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
export async function servir(page: Page, ruta: '/indicadores' | '/indicadores/operacion/endpoints', r: Respuesta) {
  await page.route(
    (url) => url.pathname.endsWith(`/api/v1${ruta}`),
    async (route: Route) => {
      if (r.colgar) return
      await route.fulfill({ status: r.estado ?? 200, contentType: 'application/json', body: JSON.stringify(r.cuerpo ?? { codigo: 'AP-ERROR', mensaje: 'detalle interno' }) })
    },
  )
}

type TablaDoble = {
  tabla: string
  esquema: string
  tipo?: string
  modulo: string
  nombreModulo: string
  servicio?: string
  appendOnly: boolean
  negocio: { queEs?: string }
  columnas: { nombre: string }[]
  relaciones: { salientes: unknown[]; entrantes: unknown[] }
  clasificacion?: { personales: number; financieros: number; secretos: number; derivados: number }
  gobierno?: { estado: string; estadoRevision: string; responsable?: string; criticaParaAuditoria: boolean; version: number }
}
type CatalogoDoble = { tablas: TablaDoble[]; detalleDeEsquemas?: unknown[]; politicasDeRetencion?: unknown[] }

const OMISION = { estado: 'ACTIVA', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 0 }
const normalizar = (x: string) => x.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replaceAll('_', ' ').trim()
const SENSIBLE: Record<string, 'personales' | 'financieros' | 'secretos'> = { PERSONAL: 'personales', FINANCIERO: 'financieros', SECRETO: 'secretos' }

/**
 * **DOBLE declarado de auditoria** para el catálogo por partes (`/resumen`, `/tablas`, `/tablas/{esquema}/{tabla}`):
 * filtra y pagina igual que `ConsultarCatalogoDeDatos` (ConsultarCatalogoDeDatosTest lo prueba del lado real). Con
 * `estado`, todas las partes responden ese error.
 */
export async function servirCatalogo(page: Page, catalogo: CatalogoDoble, r: { estado?: number } = {}) {
  await page.route(
    (url) => url.pathname.includes('/api/v1/indicadores/catalogo-de-datos'),
    async (route: Route) => {
      const responder = (cuerpo: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(cuerpo) })
      if (r.estado) return responder({ codigo: 'AP-ERROR', mensaje: 'detalle interno' }, r.estado)
      const url = new URL(route.request().url())
      const ruta = url.pathname.replace(/.*catalogo-de-datos/, '')
      const tablas = catalogo.tablas
      const g = (t: TablaDoble) => t.gobierno ?? OMISION
      if (ruta === '/resumen') {
        const cuenta = (f: (t: TablaDoble) => boolean) => tablas.filter(f).length
        const porEstado = Object.fromEntries(['ACTIVA', 'PLANIFICADA', 'CANDIDATA_A_RETIRO', 'RETIRADA', 'DESHABILITADA'].map((e) => [e, cuenta((t) => g(t).estado === e)]))
        const porRevision = Object.fromEntries(['DETECTADA', 'NECESITA_REVISION', 'APROBADA', 'RECHAZADA'].map((e) => [e, cuenta((t) => g(t).estadoRevision === e)]))
        return responder({
          tablas: tablas.length,
          tablasDeNegocio: cuenta((t) => t.tipo !== 'INFRAESTRUCTURA'),
          tablasDeInfraestructura: cuenta((t) => t.tipo === 'INFRAESTRUCTURA'),
          columnas: tablas.reduce((a, t) => a + t.columnas.length, 0),
          columnasRevisadas: 0,
          relaciones: tablas.reduce((a, t) => a + t.relaciones.salientes.length, 0),
          conDatosPersonales: cuenta((t) => (t.clasificacion?.personales ?? 0) > 0),
          conDatosFinancieros: cuenta((t) => (t.clasificacion?.financieros ?? 0) > 0),
          criticasParaAuditoria: cuenta((t) => g(t).criticaParaAuditoria),
          sinResponsable: cuenta((t) => !g(t).responsable),
          porEstado,
          porRevision,
          esquemas: catalogo.detalleDeEsquemas ?? [],
          modulos: [],
          politicasDeRetencion: catalogo.politicasDeRetencion ?? [],
        })
      }
      if (ruta === '/tablas') {
        const p = url.searchParams
        const q = normalizar(p.get('q') ?? '')
        const sensible = SENSIBLE[p.get('sensibilidad') ?? '']
        const elegidas = tablas.filter(
          (t) =>
            (!p.get('esquema') || t.esquema === p.get('esquema')) &&
            (!p.get('tipo') || (t.tipo ?? 'NEGOCIO') === p.get('tipo')) &&
            (!p.get('estado') || g(t).estado === p.get('estado')) &&
            (!p.get('revision') || g(t).estadoRevision === p.get('revision')) &&
            (!sensible || (t.clasificacion?.[sensible] ?? 0) > 0) &&
            (!q || normalizar([t.esquema, t.tabla, ...t.columnas.map((c) => c.nombre), t.negocio.queEs ?? ''].join(' ')).includes(q)),
        )
        const limite = Number(p.get('limite') ?? 20)
        const pagina = Number(p.get('pagina') ?? 1)
        const items = elegidas.slice((pagina - 1) * limite, pagina * limite).map((t) => ({
          tabla: t.tabla,
          esquema: t.esquema,
          tipo: t.tipo ?? 'NEGOCIO',
          modulo: t.modulo,
          nombreModulo: t.nombreModulo,
          ...(t.servicio ? { servicio: t.servicio } : {}),
          ...(t.negocio.queEs ? { queEs: t.negocio.queEs } : {}),
          appendOnly: t.appendOnly,
          columnas: t.columnas.length,
          columnasRevisadas: 0,
          relacionesSalientes: t.relaciones.salientes.length,
          relacionesEntrantes: t.relaciones.entrantes.length,
          clasificacion: t.clasificacion ?? { personales: 0, financieros: 0, secretos: 0, derivados: 0 },
          gobierno: g(t),
        }))
        return responder({ items, pagina, limite, total: elegidas.length, totalPaginas: Math.ceil(elegidas.length / limite) })
      }
      const ficha = ruta.match(/^\/tablas\/(\w+)\/(\w+)$/)
      if (ficha && route.request().method() === 'GET') {
        const t = tablas.find((x) => x.esquema === ficha[1] && x.tabla === ficha[2])
        return t ? responder({ ...t, gobierno: g(t) }) : responder({ codigo: 'AP-VAL-05', mensaje: 'Esa ruta no existe.', trazaId: 'e2e' }, 404)
      }
      return route.fallback()
    },
  )
}
