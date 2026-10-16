import { HttpClient, HttpParams, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { SalidaCierre } from 'clientes/angular/erp'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoConClave } from './contrato-erp'
import { aCentavos } from './cu101-presupuestos'

/**
 * CU-100 · Abrir y cerrar el período contable.
 *
 * Contrato real (`servicios/erp/src/main/resources/openapi/erp.yaml`):
 * - `GET /erp/periodos/vigente` (`consultarPeriodoVigente`): el período que contiene HOY en America/La_Paz. Si ninguno lo
 *   contiene responde 422 `AP-CU100-04 SIN_PERIODO_VIGENTE`: la pantalla lo dice y **no elige otro** por su cuenta.
 * - `GET /erp/ejercicios` (`listarEjerciciosFiscales`): ejercicios del más reciente al más antiguo, cursor `antesDeAnio`.
 * - `GET /erp/ejercicios/{id}/periodos` (`listarPeriodosDelEjercicio`): a lo sumo doce, sin paginar. Los totales son los
 *   SELLADOS en el cierre y viajan `null` mientras el período está abierto. 422 `AP-CU100-05` si el ejercicio no existe.
 * - `POST /erp/periodos/{periodoId}/cierre` (`cerrarPeriodoContable`).
 *
 * Las tres lecturas las ve cualquiera que abra Contabilidad (cualquiera de los permisos ERP): `ver:contabilidad` del shell.
 *
 * **La regla del gate de este carril vive acá, no en la pantalla**: un período cerrado NO admite asentar. El modelo lo
 * rechaza (`AP-CU103-01 PERIODO_CERRADO`), y la interfaz tiene que decirlo antes, con el motivo a la vista.
 *
 * Los tipos se declaran acá como uniones de literales con la forma EXACTA del YAML (el cliente generado los expone como
 * `enum` de TypeScript, que no se comparan contra el literal que viaja en el JSON sin un cast).
 */
export type EstadoPeriodo = 'ABIERTO' | 'CERRADO'

/** `PeriodoContable` de `erp.yaml`: la forma del período vigente y de los períodos dentro de `EjercicioFiscal`. */
export type PeriodoVigente = {
  periodoId: string
  ejercicioId: string
  anio: number
  mes: number
  estado: EstadoPeriodo
  fechaInicio: string
  fechaFin: string
}

/** `EjercicioFiscal` de `erp.yaml`. */
export type EjercicioFiscal = {
  ejercicioId: string
  anio: number
  estado: EstadoPeriodo
  fechaInicio: string
  fechaFin: string
  periodos: PeriodoVigente[]
}

/** `PaginaDeEjercicios`: `proximoAntesDeAnio = null` (o ausente) es «no hay más». */
export type PaginaDeEjercicios = { items: EjercicioFiscal[]; proximoAntesDeAnio?: number | null }

/** `PeriodoConCierre`: los totales son `Monto` (cadena) SIN moneda en el contrato; `null` mientras está abierto. */
export type PeriodoConCierre = {
  periodoId: string
  mes: number
  estado: EstadoPeriodo
  fechaInicio: string
  fechaFin: string
  cerradoEn?: string | null
  totalDebe?: string | null
  totalHaber?: string | null
}

/** `PeriodosDelEjercicio`. */
export type PeriodosDelEjercicio = { ejercicioId: string; anio: number; estado: EstadoPeriodo; periodos: PeriodoConCierre[] }

/** Lo que dibuja la pantalla: el período del contrato más el año del ejercicio y su nombre en español (lo arma la UI). */
export type PeriodoContable = PeriodoConCierre & { ejercicioId: string; anio: number; nombre: string }

/** La salida del cierre es el tipo GENERADO de `clientes/angular/erp`: no se redeclara. */
export type { SalidaCierre }

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'] as const

/** «marzo 2026». El contrato trae solo el número de mes: el nombre lo arma la interfaz, en español. */
export function nombreDelPeriodo(anio: number, mes: number): string {
  const nombre = MESES[mes - 1]
  return nombre ? `${nombre} ${anio}` : `${mes}/${anio}`
}

/** De la respuesta del contrato a las filas de la pantalla, en orden de mes (el contrato no promete orden). */
export function aPeriodosContables(respuesta: PeriodosDelEjercicio): PeriodoContable[] {
  return [...respuesta.periodos]
    .sort((a, b) => a.mes - b.mes)
    .map((p) => ({ ...p, ejercicioId: respuesta.ejercicioId, anio: respuesta.anio, nombre: nombreDelPeriodo(respuesta.anio, p.mes) }))
}

/** Un período cerrado no admite ningún asiento nuevo: es irreversible por diseño (CU-100). */
export function puedeAsentarEn(periodo: Pick<PeriodoContable, 'estado'>): boolean {
  return periodo.estado === 'ABIERTO'
}

/**
 * Por qué no se puede asentar. Devuelve `null` cuando sí se puede: la interfaz nunca
 * muestra un botón deshabilitado sin motivo visible al lado.
 */
export function motivoParaNoAsentar(periodo: Pick<PeriodoContable, 'estado'>): string | null {
  return puedeAsentarEn(periodo)
    ? null
    : 'Período cerrado: no se puede asentar en él. La corrección va en el período abierto siguiente, con glosa que lo referencie.'
}

/** El más antiguo sin cerrar del ejercicio: el único que el backend acepta cerrar (AP-CU100-02). */
export function periodoMasAntiguoAbierto(periodos: readonly PeriodoContable[]): PeriodoContable | null {
  const abiertos = periodos.filter((p) => p.estado === 'ABIERTO')
  if (abiertos.length === 0) return null
  return [...abiertos].sort((a, b) => a.anio - b.anio || a.mes - b.mes)[0] ?? null
}

/** Solo el más antiguo abierto se puede cerrar; el resto, con su motivo a la vista. */
export function puedeCerrar(periodo: PeriodoContable, periodos: readonly PeriodoContable[]): boolean {
  return periodoMasAntiguoAbierto(periodos)?.periodoId === periodo.periodoId
}

/**
 * El cuadre SELLADO del mes: `total_debe` distinto de `total_haber` es un incidente, no un cierre (3a). Se compara en
 * centavos enteros, nunca como `number` decimal. Sin totales (período abierto) no se puede afirmar que cuadre.
 */
export function cuadra(periodo: Pick<PeriodoConCierre, 'totalDebe' | 'totalHaber'>): boolean {
  if (!periodo.totalDebe || !periodo.totalHaber) return false
  return aCentavos(periodo.totalDebe) === aCentavos(periodo.totalHaber)
}

/** El período que contiene hoy. 422 `AP-CU100-04` si no hay ninguno: el error llega a la pantalla, no se reemplaza. */
export function periodoVigente() {
  const gateway = inject(GATEWAY)
  return httpResource<PeriodoVigente>(() => `${gateway}/erp/periodos/vigente`)
}

/** Los períodos del ejercicio elegido; sin ejercicio (`undefined`) no se pide nada. */
export function periodosDelEjercicio(ejercicioId: () => string | undefined) {
  const gateway = inject(GATEWAY)
  return httpResource<PeriodoContable[]>(
    () => {
      const id = ejercicioId()
      return id ? `${gateway}/erp/ejercicios/${id}/periodos` : undefined
    },
    { parse: (crudo) => aPeriodosContables(crudo as PeriodosDelEjercicio) },
  )
}

export const sinPeriodos = (periodos: PeriodoContable[] | undefined): boolean => (periodos?.length ?? 0) === 0

/** Tamaño de página del selector de ejercicios: el contrato admite 1 a 20 (por omisión 5). */
export const EJERCICIOS_POR_PAGINA = 5

/** `GET /erp/ejercicios`: una página del selector; `antesDeAnio` es el cursor que devolvió la página anterior. */
export function crearListadoDeEjercicios(): (antesDeAnio: number | null) => Observable<PaginaDeEjercicios> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (antesDeAnio) => {
    let params = new HttpParams().set('limite', EJERCICIOS_POR_PAGINA)
    if (antesDeAnio !== null) params = params.set('antesDeAnio', antesDeAnio)
    return http.get<PaginaDeEjercicios>(`${gateway}/erp/ejercicios`, { params })
  }
}

/** `POST /erp/periodos/{periodoId}/cierre`, con clave de idempotencia nueva por intento. */
export function crearCierreDePeriodo(): (periodoId: string, glosa: string) => Observable<SalidaCierre> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (periodoId, glosa) =>
    http.post<SalidaCierre>(`${gateway}/erp/periodos/${periodoId}/cierre`, { glosa }, { context: contextoConClave() })
}
