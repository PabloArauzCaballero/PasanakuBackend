import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import type { PeriodoContable } from '../dominio/cu100-periodos'
import { PantallaDePeriodo } from './pantalla-de-periodo'

const EJERCICIO = '11111111-1111-4111-8111-111111111111'
const URL = `http://gw/api/v1/erp/ejercicios/${EJERCICIO}/periodos`

function periodo(mes: number, estado: 'ABIERTO' | 'CERRADO'): PeriodoContable {
  return {
    periodoId: `p-${mes}`,
    ejercicioFiscalId: EJERCICIO,
    anio: 2026,
    mes,
    nombre: `${mes}/2026`,
    estado,
    cerradoEn: estado === 'CERRADO' ? '2026-02-01T10:00:00Z' : null,
    totalDebe: { monto: '12000.00', moneda: 'BOB' },
    totalHaber: { monto: '12000.00', moneda: 'BOB' },
  }
}

/**
 * **El gate propio del carril B3, punto 1.** Un período cerrado se ve cerrado: la
 * pantalla NO ofrece la acción de asentar en él, y el motivo queda escrito a la vista.
 * Es la prueba que exige la ficha `F13` de `planes/18`.
 */
describe('PantallaDePeriodo · un período CERRADO no ofrece asentar', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function montar(periodos: PeriodoContable[], permisos: string[] = ['CONTABILIDAD_ERP_CERRAR']) {
    TestBed.inject(Sesion).abrir('t', permisos, 'contabilidad')
    const fixture = TestBed.createComponent(PantallaDePeriodo)
    fixture.componentRef.setInput('ejercicioId', EJERCICIO)
    fixture.detectChanges()
    http.expectOne(URL).flush(periodos)
    await fixture.whenStable()
    return fixture
  }

  function textosDeBotones(fixture: { nativeElement: HTMLElement }): string[] {
    return [...fixture.nativeElement.querySelectorAll('ap-boton button')].map((b) => (b.textContent ?? '').trim())
  }

  it('sobre un período cerrado no hay botón de asentar, y el motivo está a la vista', async () => {
    const fixture = await montar([periodo(1, 'CERRADO')])
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Asentar'))).toBe(false)
    expect(fixture.nativeElement.textContent).toContain('Período cerrado: no se puede asentar en él')
  })

  it('sobre un período abierto sí aparece el botón de asentar', async () => {
    const fixture = await montar([periodo(1, 'ABIERTO')])
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Asentar'))).toBe(true)
  })

  it('con un mes cerrado y el siguiente abierto, solo el abierto ofrece asentar', async () => {
    const fixture = await montar([periodo(1, 'CERRADO'), periodo(2, 'ABIERTO')])
    expect(textosDeBotones(fixture).filter((texto) => texto.includes('Asentar'))).toHaveLength(1)
  })

  it('solo el período más antiguo abierto ofrece cerrarse: los períodos se cierran en orden', async () => {
    const fixture = await montar([periodo(2, 'ABIERTO'), periodo(3, 'ABIERTO')])
    expect(textosDeBotones(fixture).filter((texto) => texto.includes('Cerrar el período'))).toHaveLength(1)
  })

  it('sin CONTABILIDAD_ERP_CERRAR no aparece el botón de cerrar, aunque el mes sea el más antiguo', async () => {
    const fixture = await montar([periodo(1, 'ABIERTO')], [])
    expect(textosDeBotones(fixture).some((texto) => texto.includes('Cerrar el período'))).toBe(false)
  })

  it('el cuadre del mes lo dibuja el átomo Monto, con su etiqueta para el lector', async () => {
    const fixture = await montar([periodo(1, 'ABIERTO')])
    const debe = fixture.nativeElement.querySelector('ap-monto[aria-label^="Total debe: "]') as HTMLElement
    expect(debe.textContent).toBe('Bs 12.000,00')
  })
})

describe('PantallaDePeriodo · los cuatro estados', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function montar() {
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_CERRAR'], 'contabilidad')
    const fixture = TestBed.createComponent(PantallaDePeriodo)
    fixture.componentRef.setInput('ejercicioId', EJERCICIO)
    fixture.detectChanges()
    return fixture
  }

  it('cargando: lo dice mientras espera, sin pantalla en blanco', () => {
    const fixture = montar()
    const estado = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement
    expect(estado?.getAttribute('aria-label')).toBe('Cargando los períodos del ejercicio')
    http.expectOne(URL).flush([])
  })

  it('vacío: un ejercicio sin períodos lo dice', async () => {
    const fixture = montar()
    http.expectOne(URL).flush([])
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('todavía no tiene períodos')
  })

  it('error: mensaje humano, traza para soporte y reintento a la vista', async () => {
    const fixture = montar()
    http.expectOne(URL).flush({ codigo: 'AP-CU100-01', mensaje: 'tecnico', trazaId: 'traza-100' }, { status: 401, statusText: 'Unauthorized' })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Código de seguimiento: traza-100')
    expect(fixture.nativeElement.textContent).not.toContain('tecnico')
  })

  it('con datos: los meses del ejercicio, cada uno con su estado', async () => {
    const fixture = montar()
    http.expectOne(URL).flush([periodo(1, 'CERRADO'), periodo(2, 'ABIERTO')])
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelectorAll('.mes')).toHaveLength(2)
  })
})
