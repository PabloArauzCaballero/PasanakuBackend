import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY, GW } from '../../../nucleo/gateway'
import { idempotenciaInterceptor, olvidarIntentos } from '../../../nucleo/idempotencia.interceptor'
import { FormularioDeConciliacion } from './formulario-de-conciliacion'

/** **UI contra HTTP simulado** de `conciliarCustodia` (CU-50). Identificadores e importes sintéticos. */
const URL = `${GW}/custodia/conciliaciones`
const CUENTA = '66d81a5c-e974-4f02-8705-39aa2504c995'
const OK = ejemploDe('nucleo-financiero', 'conciliarCustodia', 'ok').cuerpo as { conciliacionId: string }
const DESCUADRE = { ...OK, estado: 'DESCUADRADA', cumpleEncaje: false, ratioCobertura: '0.982000', diferencia: { monto: '-1250.00', moneda: 'BOB' } }

describe('FormularioDeConciliacion · [UI contra HTTP simulado] CU-50', () => {
  let http: HttpTestingController
  beforeEach(() => {
    olvidarIntentos()
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: GW }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  function montar(): ComponentFixture<FormularioDeConciliacion> {
    const f = TestBed.createComponent(FormularioDeConciliacion)
    f.detectChanges()
    return f
  }
  function llenar(f: ComponentFixture<FormularioDeConciliacion>, cuenta = CUENTA, fecha = '2026-10-15', moneda: string | null = 'BOB', custodia = '1.250.000,50', transito = '0') {
    const c = f.componentInstance
    c['cuenta'].set(cuenta)
    c['fecha'].set(fecha)
    c['moneda'].set(moneda)
    c['custodia'].set(custodia)
    c['transito'].set(transito)
    c['revisar']()
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const confirmar = (f: ComponentFixture<FormularioDeConciliacion>) => f.componentInstance['enviar'](f.componentInstance['pendiente']()!)

  it('correcto: revisar abre la confirmación con la consecuencia exacta y SIN pedir nada; confirmar manda el cuerpo del contrato con Idempotency-Key y muestra el recibo con su id', () => {
    const f = montar()
    llenar(f)
    http.expectNone(URL)
    expect(texto(f)).toContain(`la cuenta ${CUENTA} del 15 oct 2026`)
    expect(texto(f)).toContain('El saldo emitido lo calcula el servidor')
    confirmar(f)
    f.detectChanges()
    confirmar(f)
    const req = http.expectOne(URL)
    expect(req.request.body).toEqual({ cuentaCustodiaId: CUENTA, fecha: '2026-10-15', saldoCustodia: { monto: '1250000.50', moneda: 'BOB' }, saldoEnTransito: { monto: '0.00', moneda: 'BOB' } })
    expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(texto(f)).not.toContain('Conciliación registrada')
    req.flush(OK, { status: 201, statusText: 'Created' })
    f.detectChanges()
    expect(texto(f)).toContain('Conciliación registrada')
    expect(texto(f)).toContain(OK.conciliacionId)
  })

  it('límite: un recibo descuadrado lo dice en texto («Descuadre» y «No cumple el encaje»), no solo con el tono', () => {
    const f = montar()
    llenar(f)
    confirmar(f)
    http.expectOne(URL).flush(DESCUADRE, { status: 201, statusText: 'Created' })
    f.detectChanges()
    expect(texto(f)).toContain('Descuadre:')
    expect(texto(f)).toContain('No cumple el encaje')
  })

  it('límite: datos incompletos o mal formados marcan cada campo y no abren la confirmación', () => {
    const f = montar()
    llenar(f, 'no-es-uuid', '', null, 'abc', '1,234')
    expect(f.componentInstance['pendiente']()).toBeNull()
    const e = f.componentInstance['errores']()
    expect(Object.keys(e).sort()).toEqual(['cuenta', 'custodia', 'fecha', 'moneda', 'transito'])
    http.expectNone(URL)
  })

  it('límite: corregir un campo marcado le saca el error a ese campo y deja el resto marcado', () => {
    const f = montar()
    llenar(f, 'no-es-uuid', '', null, 'abc', '1,234')
    f.componentInstance['cuenta'].set(CUENTA)
    f.detectChanges()
    expect(Object.keys(f.componentInstance['errores']()).sort()).toEqual(['custodia', 'fecha', 'moneda', 'transito'])
  })

  it('inválido: 422 AP-CU50-03 no muestra recibo y conserva lo escrito; 403 dice «sin acceso»', () => {
    const f = montar()
    llenar(f)
    confirmar(f)
    http.expectOne(URL).flush({ codigo: 'AP-CU50-03', mensaje: 'crudo', trazaId: 't' }, { status: 422, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).not.toContain('Conciliación registrada')
    expect(texto(f)).not.toContain('crudo')
    expect(texto(f)).toContain('Ese día ya tiene una conciliación de esta cuenta con otros saldos')
    expect(f.componentInstance['custodia']()).toBe('1.250.000,50')
    llenar(f)
    confirmar(f)
    http.expectOne(URL).flush({ codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, { status: 403, statusText: 'x' })
    f.detectChanges()
    expect(texto(f)).toContain('No tenés acceso a esto.')
  })

  it('inválido: sin respuesta queda «sin confirmar» y el reintento sale con la misma clave', () => {
    const f = montar()
    llenar(f)
    confirmar(f)
    const primera = http.expectOne(URL)
    const clave = primera.request.headers.get('Idempotency-Key')
    primera.error(new ProgressEvent('error'), { status: 0 })
    f.detectChanges()
    expect(texto(f)).toContain('No pudimos confirmar si la conciliación quedó registrada')
    llenar(f)
    confirmar(f)
    expect(http.expectOne(URL).request.headers.get('Idempotency-Key')).toBe(clave)
  })
})
