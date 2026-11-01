import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, Injector, runInInjectionContext, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { IndicadorFamiliaEnum, SalidaTableroDimensionEnum } from 'clientes/angular/auditoria'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import {
  ESPERADOS_FINANCIERO,
  conUnidad,
  decimal,
  embudo,
  montoExacto,
  periodoCerrado,
  recursoTablero,
  tarjetas,
  variacionEnPalabras,
  type IndicadorPublicado,
  type SalidaTablero,
} from './indicadores'

const indicador = (codigo: string, extra: Partial<IndicadorPublicado> = {}): IndicadorPublicado => ({
  codigo,
  nombre: codigo,
  valor: '1500.0000',
  unidad: 'BOB',
  familia: IndicadorFamiliaEnum.Finanzas,
  duenoFamilia: 'Tesorería',
  definicionVersion: 'v1',
  suprimidoPorPrivacidad: false,
  casos: 6,
  minimoCasos: 5,
  calculadoEn: '2026-10-16T06:15:00Z',
  ...extra,
})

const salida = (...indicadores: IndicadorPublicado[]): SalidaTablero => ({
  periodo: '2026-09',
  provisorio: false,
  dimension: SalidaTableroDimensionEnum.Global,
  indicadores,
})

describe('periodoCerrado · el tablero abre en el último período cerrado, nunca en el en curso', () => {
  it('correcto: ayer, el mes pasado y el año pasado', () => {
    const hoy = new Date(2026, 9, 16)
    expect(periodoCerrado(hoy, 'dia')).toBe('2026-10-15')
    expect(periodoCerrado(hoy, 'mes')).toBe('2026-09')
    expect(periodoCerrado(hoy, 'anio')).toBe('2025')
  })

  it('límite: el 1 de enero cruza de año en día y en mes', () => {
    const primero = new Date(2027, 0, 1)
    expect(periodoCerrado(primero, 'dia')).toBe('2026-12-31')
    expect(periodoCerrado(primero, 'mes')).toBe('2026-12')
  })
})

describe('tarjetas · null no es cero', () => {
  it('correcto: un importe va con su moneda y la variación en palabras', () => {
    const [t] = tarjetas(
      salida(indicador('SALDO_CARGADO', { variacionPeriodoAnterior: '12.50' })),
      ESPERADOS_FINANCIERO.slice(0, 1),
    )
    // NUMERIC(16,4) → dos decimales exactos para `ap-monto`: solo se recortan ceros.
    expect(t).toMatchObject({ estado: 'valor', valor: '1500.00', moneda: 'BOB', casos: 6 })
    expect(t!.variacion).toBe('12,5 % más que el período anterior')
  })

  it('límite: un código que no llegó se muestra «sin datos», nunca como cero', () => {
    const [t] = tarjetas(salida(), ESPERADOS_FINANCIERO.slice(0, 1))
    expect(t).toMatchObject({ codigo: 'SALDO_CARGADO', estado: 'sinDatos' })
    expect(t!.valor).toBeUndefined()
  })

  it('límite: suprimido por privacidad no trae valor y dice el mínimo de casos', () => {
    const [t] = tarjetas(
      salida(indicador('SALDO_CARGADO', { suprimidoPorPrivacidad: true, valor: undefined, minimoCasos: 5 })),
      ESPERADOS_FINANCIERO.slice(0, 1),
    )
    expect(t).toMatchObject({ estado: 'suprimido', minimoCasos: 5 })
    expect(t!.valor).toBeUndefined()
  })

  it('correcto: lo que no es dinero sale con su unidad', () => {
    const [t] = tarjetas(
      salida(indicador('TASA_MORA', { valor: '23.3300', unidad: 'PORCENTAJE' })),
      [{ codigo: 'TASA_MORA', nombre: 'Tasa de mora', grupo: 'Mora' }],
    )
    expect(t).toMatchObject({ estado: 'valor', texto: '23,33 %' })
    expect(t!.moneda).toBeUndefined()
  })
})

describe('montoExacto · dos decimales sin redondear', () => {
  it('correcto: recorta solo ceros y completa los que faltan', () => {
    expect(montoExacto('1500.0000')).toBe('1500.00')
    expect(montoExacto('25.5000')).toBe('25.50')
    expect(montoExacto('7')).toBe('7.00')
    expect(montoExacto('-3.1')).toBe('-3.10')
  })

  it('inválido: un decimal significativo de más no se redondea: no hay importe exacto', () => {
    expect(montoExacto('10.1234')).toBeUndefined()
    expect(montoExacto('abc')).toBeUndefined()
  })

  it('límite: un importe sin exactitud de dos decimales se muestra como texto con su moneda, nunca redondeado', () => {
    const [t] = tarjetas(salida(indicador('SALDO_CARGADO', { valor: '10.1234' })), ESPERADOS_FINANCIERO.slice(0, 1))
    expect(t).toMatchObject({ estado: 'valor', texto: '10,1234 BOB' })
    expect(t!.moneda).toBeUndefined()
  })
})

describe('formato · exacto, sin pasar por number', () => {
  it('correcto: miles con punto, decimales con coma, sin ceros de relleno', () => {
    expect(decimal('1234567.5000')).toBe('1.234.567,5')
    expect(decimal('12.0000')).toBe('12')
    expect(conUnidad('4.50', 'DIAS')).toBe('4,5 días')
    expect(conUnidad('2.1000', 'VECES')).toBe('2,1 veces')
    expect(conUnidad('7', 'CONTEO')).toBe('7')
    expect(conUnidad('182.5000', 'MILISEGUNDOS')).toBe('182,5 ms')
  })

  it('límite: sin variación, cero y negativa se dicen distinto', () => {
    expect(variacionEnPalabras(undefined)).toBe('Sin comparación con el período anterior')
    // El backend real manda `null`, no «ausente»: antes lanzaba TypeError al leer `.startsWith`.
    expect(variacionEnPalabras(null)).toBe('Sin comparación con el período anterior')
    expect(variacionEnPalabras('0.00')).toBe('Igual que el período anterior')
    expect(variacionEnPalabras('-8.25')).toBe('8,25 % menos que el período anterior')
  })
})

describe('embudo · cada paso con su número; el ancho es relativo al primero', () => {
  it('correcto: los cuatro pasos en orden, el primero al 100 %', () => {
    const pasos = embudo(
      salida(
        indicador('USUARIOS_NUEVOS', { valor: '40', unidad: 'CONTEO' }),
        indicador('EMBUDO_KYC_APROBADO', { valor: '30', unidad: 'CONTEO' }),
        indicador('EMBUDO_EN_GRUPO', { valor: '10', unidad: 'CONTEO' }),
      ),
    )
    expect(pasos.map((p) => p.ancho)).toEqual([100, 75, 25, 0])
    expect(pasos[3]).toMatchObject({ texto: 'Sin datos' })
  })

  it('inválido: sin el primer paso no hay base y ninguna barra se dibuja', () => {
    const pasos = embudo(salida(indicador('EMBUDO_EN_GRUPO', { valor: '10', unidad: 'CONTEO' })))
    expect(pasos.every((p) => p.ancho === 0)).toBe(true)
    expect(pasos[2]).toMatchObject({ texto: '10' })
  })
})

describe('recursoTablero · una consulta a GET /indicadores con el período elegido', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('correcto: pide periodo, dimensión GLOBAL y doce períodos de serie', () => {
    const periodo = signal('2026-09')
    runInInjectionContext(TestBed.inject(Injector), () => recursoTablero(periodo))
    TestBed.inject(ApplicationRef).tick()
    const req = http.expectOne((r) => r.url === 'http://gw/api/v1/indicadores')
    expect(req.request.params.get('periodo')).toBe('2026-09')
    expect(req.request.params.get('dimension')).toBe('GLOBAL')
    expect(req.request.params.get('periodosDeSerie')).toBe('12')
    req.flush(salida())
    http.verify()
  })
})
