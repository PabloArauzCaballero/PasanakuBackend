import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { firstValueFrom } from 'rxjs'
import { describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import type { ErrorTraducido } from '../../../nucleo/errores'
import {
  aPeriodosContables,
  crearListadoDeEjercicios,
  cuadra,
  motivoParaNoAsentar,
  nombreDelPeriodo,
  periodoMasAntiguoAbierto,
  puedeAsentarEn,
  puedeCerrar,
  type PeriodoContable,
} from './cu100-periodos'

/** Datos sintéticos con la forma de `PeriodoConCierre` + lo que agrega la UI. */
function periodo(mes: number, estado: 'ABIERTO' | 'CERRADO', debe: string | null = '10.00', haber: string | null = '10.00'): PeriodoContable {
  return {
    periodoId: `p-${mes}`,
    ejercicioId: 'e-1',
    anio: 2026,
    mes,
    nombre: nombreDelPeriodo(2026, mes),
    estado,
    fechaInicio: `2026-${String(mes).padStart(2, '0')}-01`,
    fechaFin: `2026-${String(mes).padStart(2, '0')}-28`,
    cerradoEn: estado === 'CERRADO' ? '2026-02-01T10:00:00Z' : null,
    totalDebe: debe,
    totalHaber: haber,
  }
}

describe('CU-100 · la regla de un período cerrado', () => {
  it('un período ABIERTO admite asentar y no tiene motivo que mostrar', () => {
    expect(puedeAsentarEn(periodo(1, 'ABIERTO'))).toBe(true)
    expect(motivoParaNoAsentar(periodo(1, 'ABIERTO'))).toBeNull()
  })

  it('un período CERRADO no admite asentar, y el motivo dice dónde va la corrección', () => {
    expect(puedeAsentarEn(periodo(1, 'CERRADO'))).toBe(false)
    expect(motivoParaNoAsentar(periodo(1, 'CERRADO'))).toContain('período abierto siguiente')
  })
})

describe('CU-100 · el orden estricto de cierre (AP-CU100-02)', () => {
  const meses = [periodo(3, 'ABIERTO'), periodo(1, 'CERRADO'), periodo(2, 'ABIERTO')]

  it('el más antiguo abierto es febrero, no marzo, aunque marzo venga primero en la lista', () => {
    expect(periodoMasAntiguoAbierto(meses)?.mes).toBe(2)
  })

  it('solo el más antiguo abierto se puede cerrar', () => {
    expect(puedeCerrar(periodo(2, 'ABIERTO'), meses)).toBe(true)
    expect(puedeCerrar(periodo(3, 'ABIERTO'), meses)).toBe(false)
  })

  it('sin ningún período abierto no hay nada que cerrar', () => {
    expect(periodoMasAntiguoAbierto([periodo(1, 'CERRADO')])).toBeNull()
  })
})

describe('CU-100 · el cuadre sellado del mes (AP-CU100-03)', () => {
  it('cuadra cuando debe y haber coinciden', () => {
    expect(cuadra(periodo(1, 'CERRADO', '1200.50', '1200.50'))).toBe(true)
  })

  it('no cuadra con un centavo de diferencia: es un incidente, no un cierre', () => {
    expect(cuadra(periodo(1, 'CERRADO', '1200.50', '1200.51'))).toBe(false)
  })

  it('totales null (período abierto, el contrato no los calcula en vivo): no se puede afirmar que cuadre', () => {
    expect(cuadra(periodo(1, 'ABIERTO', null, null))).toBe(false)
  })

  it('compara en centavos enteros: un importe grande no pierde el centavo como lo haría un doble', () => {
    expect(cuadra(periodo(1, 'CERRADO', '90071992547409.91', '90071992547409.92'))).toBe(false)
  })
})

describe('CU-100 · de la respuesta del contrato a la pantalla', () => {
  it('el nombre del mes lo arma la UI en español', () => {
    expect(nombreDelPeriodo(2026, 3)).toBe('marzo 2026')
    expect(nombreDelPeriodo(2025, 12)).toBe('diciembre 2025')
  })

  it('aPeriodosContables ordena por mes y agrega año, ejercicio y nombre; los totales null siguen null', () => {
    const filas = aPeriodosContables({
      ejercicioId: 'e-9',
      anio: 2025,
      estado: 'ABIERTO',
      periodos: [
        { periodoId: 'b', mes: 2, estado: 'ABIERTO', fechaInicio: '2025-02-01', fechaFin: '2025-02-28', cerradoEn: null, totalDebe: null, totalHaber: null },
        { periodoId: 'a', mes: 1, estado: 'CERRADO', fechaInicio: '2025-01-01', fechaFin: '2025-01-31', cerradoEn: '2025-02-03T12:00:00Z', totalDebe: '5.00', totalHaber: '5.00' },
      ],
    })
    expect(filas.map((f) => [f.periodoId, f.nombre, f.ejercicioId, f.anio])).toEqual([
      ['a', 'enero 2025', 'e-9', 2025],
      ['b', 'febrero 2025', 'e-9', 2025],
    ])
    expect(filas[1]?.totalDebe).toBeNull()
  })
})

describe('CU-100 · GET /erp/ejercicios por cursor antesDeAnio', () => {
  function preparar() {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    const pedir = TestBed.runInInjectionContext(() => crearListadoDeEjercicios())
    return { pedir, http: TestBed.inject(HttpTestingController) }
  }

  it('la primera página va sin cursor y con el límite del contrato', async () => {
    const { pedir, http } = preparar()
    const respuesta = firstValueFrom(pedir(null))
    http.expectOne('http://gw/api/v1/erp/ejercicios?limite=5').flush({ items: [], proximoAntesDeAnio: 2024 })
    expect(await respuesta).toEqual({ items: [], proximoAntesDeAnio: 2024 })
    http.verify()
  })

  it('la siguiente usa el cursor que devolvió el servidor, sin calcularlo', async () => {
    const { pedir, http } = preparar()
    const respuesta = firstValueFrom(pedir(2024))
    http.expectOne('http://gw/api/v1/erp/ejercicios?limite=5&antesDeAnio=2024').flush({ items: [], proximoAntesDeAnio: null })
    expect((await respuesta).proximoAntesDeAnio).toBeNull()
  })

  it('403: llega traducido, sin el cuerpo del backend', async () => {
    const { pedir, http } = preparar()
    const respuesta = firstValueFrom(pedir(null)).catch((e: ErrorTraducido) => e)
    http.expectOne('http://gw/api/v1/erp/ejercicios?limite=5').flush({ mensaje: 'detalle interno' }, { status: 403, statusText: 'Forbidden' })
    const error = (await respuesta) as ErrorTraducido
    expect(error.estado).toBe(403)
    expect(error.mensaje).toBe('No tenés acceso a esto.')
  })
})
