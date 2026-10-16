import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import type { EjercicioFiscal, PaginaDeEjercicios, PeriodoConCierre, PeriodosDelEjercicio, PeriodoVigente } from '../dominio/cu100-periodos'
import { PantallaDePeriodo } from './pantalla-de-periodo'

/**
 * Montaje compartido de las pruebas de la pantalla de períodos (CU-100), contra `HttpTestingController`: **doble
 * declarado** del ERP (regla 65) con la forma exacta de `erp.yaml`. Todos los datos son sintéticos.
 */
export const GW = 'http://gw/api/v1'
export const URL_VIGENTE = `${GW}/erp/periodos/vigente`
export const URL_EJERCICIOS = `${GW}/erp/ejercicios?limite=5`
export const urlPeriodos = (ejercicioId: string) => `${GW}/erp/ejercicios/${ejercicioId}/periodos`

export type Falla = { status: number; cuerpo: unknown }

export function configurar(): HttpTestingController {
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
      provideHttpClientTesting(),
      { provide: GATEWAY, useValue: GW },
    ],
  })
  return TestBed.inject(HttpTestingController)
}

export function vigente(mes: number, ejercicioId = 'e-2026', estado: 'ABIERTO' | 'CERRADO' = 'ABIERTO'): PeriodoVigente {
  const mm = String(mes).padStart(2, '0')
  return { periodoId: `p-${mes}`, ejercicioId, anio: 2026, mes, estado, fechaInicio: `2026-${mm}-01`, fechaFin: `2026-${mm}-28` }
}

export function ejercicio(anio: number, ejercicioId: string, estado: 'ABIERTO' | 'CERRADO' = 'ABIERTO'): EjercicioFiscal {
  return { ejercicioId, anio, estado, fechaInicio: `${anio}-01-01`, fechaFin: `${anio}-12-31`, periodos: [] }
}

/** Un mes del ejercicio. Cerrado lleva totales sellados (iguales salvo que se pidan otros); abierto, `null`. */
export function mes(n: number, estado: 'ABIERTO' | 'CERRADO', debe?: string, haber?: string): PeriodoConCierre {
  const mm = String(n).padStart(2, '0')
  const cerrado = estado === 'CERRADO'
  return {
    periodoId: `p-${n}`,
    mes: n,
    estado,
    fechaInicio: `2026-${mm}-01`,
    fechaFin: `2026-${mm}-28`,
    cerradoEn: cerrado ? '2026-02-01T10:00:00Z' : null,
    totalDebe: cerrado ? (debe ?? '12000.00') : null,
    totalHaber: cerrado ? (haber ?? debe ?? '12000.00') : null,
  }
}

export function periodos(ejercicioId: string, filas: PeriodoConCierre[]): PeriodosDelEjercicio {
  return { ejercicioId, anio: 2026, estado: 'ABIERTO', periodos: filas }
}

function responder(req: TestRequest, respuesta: unknown | Falla): void {
  const r = respuesta as Partial<Falla>
  if (typeof r === 'object' && r !== null && typeof r.status === 'number' && 'cuerpo' in r) {
    req.flush(r.cuerpo as object, { status: r.status, statusText: 'x' })
  } else {
    req.flush(respuesta as object)
  }
}

/** Deja correr la cola de microtareas y un ciclo de detección: lo que necesita un `httpResource` que depende de otro. */
export async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 0))
  fixture.detectChanges()
}

export type Escenario = {
  permisos?: string[]
  ejercicioId?: string
  vigente: PeriodoVigente | Falla
  ejercicios?: PaginaDeEjercicios
  /** Lo que responde `/periodos`. Ausente: no se responde (para mirar el estado de carga o comprobar que no se pidió). */
  periodos?: PeriodosDelEjercicio | Falla
}

export async function montar(http: HttpTestingController, e: Escenario): Promise<ComponentFixture<PantallaDePeriodo>> {
  TestBed.inject(Sesion).abrir('t', e.permisos ?? ['CONTABILIDAD_ERP_CERRAR'], 'contabilidad')
  const fixture = TestBed.createComponent(PantallaDePeriodo)
  if (e.ejercicioId) fixture.componentRef.setInput('ejercicioId', e.ejercicioId)
  fixture.detectChanges()
  http.expectOne(URL_EJERCICIOS).flush(e.ejercicios ?? { items: [], proximoAntesDeAnio: null })
  responder(http.expectOne(URL_VIGENTE), e.vigente)
  await esperar(fixture)
  const objetivo = e.ejercicioId ?? ('ejercicioId' in e.vigente ? e.vigente.ejercicioId : undefined)
  if (objetivo && e.periodos) {
    responder(http.expectOne(urlPeriodos(objetivo)), e.periodos)
    await esperar(fixture)
  }
  return fixture
}

export function textosDeBotones(fixture: { nativeElement: HTMLElement }): string[] {
  return [...fixture.nativeElement.querySelectorAll('ap-boton button')].map((b) => (b.textContent ?? '').trim())
}
