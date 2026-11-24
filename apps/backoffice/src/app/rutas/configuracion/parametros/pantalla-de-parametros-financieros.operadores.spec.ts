import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { Sesion } from '../../../nucleo/sesion'
import type { ParametroFinanciero } from '../dominio/cu132-parametros'
import { PantallaDeParametrosFinancieros } from './pantalla-de-parametros-financieros'

/**
 * CU-132 · «Propuso» y «Aprobó» con el nombre del operador. Doble HTTP con la forma de los contratos de tarifas e
 * identidad (`POST /usuarios-internos/nombres`) en sus tres niveles: correcto (nombres), límite (un id que no
 * vuelve) e inválido (identidad responde 503). La consulta real la cubren las pruebas de identidad.
 */
const GW = 'http://gw/api/v1'
const CATALOGO = `${GW}/tarifas/parametros`
const NOMBRES = `${GW}/usuarios-internos/nombres`
const [PROPONENTE, APROBADOR] = ['c1320000-0000-4000-8000-00000000a001', 'c1320000-0000-4000-8000-00000000b002'] // operadores sintéticos
const OPERADORES = [{ usuarioId: PROPONENTE, nombre: 'Carla Gutiérrez Rojas' }, { usuarioId: APROBADOR, nombre: 'Luis Fernando Mamani Choque' }]
const version = (id: string, valor: string, estado: string, aprobadoPor: string | null) =>
  ({ id, codigo: 'DIAS_DE_GRACIA', valor, moneda: null, nivel: null, vigenteDesde: '2026-10-01T00:00:00Z', estado, propuestoPor: PROPONENTE, aprobadoPor })
const PARAMETRO = {
  codigo: 'DIAS_DE_GRACIA', descripcion: 'Días de gracia', unidad: 'DIAS', porNivel: false,
  vigentes: [version('v-aprobada', '5.000000', 'APROBADA', APROBADOR)],
  propuestas: [version('v-propuesta', '6', 'PROPUESTA', null)],
} as unknown as ParametroFinanciero

describe('PantallaDeParametrosFinancieros · nombre del operador [UI con API simulada]', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    TestBed.inject(Sesion).abrir('token', ['TARIFARIO_ADMINISTRAR'], 'ADMIN_PLATAFORMA', 'op-1')
    http = TestBed.inject(HttpTestingController)
  })

  async function cargar(nombres: object | null = OPERADORES): Promise<ComponentFixture<PantallaDeParametrosFinancieros>> {
    const fixture = TestBed.createComponent(PantallaDeParametrosFinancieros)
    fixture.detectChanges()
    http.expectOne(CATALOGO).flush([PARAMETRO])
    const pedido = http.expectOne(NOMBRES)
    if (nombres === null) pedido.flush({ codigo: 'X', mensaje: 'detalle interno' }, { status: 503, statusText: 'Service Unavailable' })
    else pedido.flush(nombres)
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture
  }

  const raiz = (fixture: ComponentFixture<unknown>) => fixture.nativeElement as HTMLElement
  const historial = async (fixture: ComponentFixture<PantallaDeParametrosFinancieros>) => {
    ;(fixture.componentInstance as unknown as { alternarHistorial(c: string): void }).alternarHistorial('DIAS_DE_GRACIA')
    fixture.detectChanges()
    http.expectOne(`${CATALOGO}/DIAS_DE_GRACIA/versiones`).flush([version('v-rechazada', '7', 'RECHAZADA', null), ...PARAMETRO.vigentes])
    await fixture.whenStable()
    fixture.detectChanges()
  }

  it('correcto: cada versión dice quién la propuso y quién la aprobó; sin aprobador, dice por qué', async () => {
    const fixture = await cargar()
    const en = (sel: string) => raiz(fixture).querySelector(sel)?.textContent ?? ''
    expect([en('.carril.rige'), en('.carril.espera')].join('|')).toMatch(/Luis Fernando Mamani Choque.*\|.*Carla Gutiérrez Rojas.*Falta la segunda firma/s)
    await historial(fixture)
    const [rechazada = '', aprobada = ''] = [...raiz(fixture).querySelectorAll('.tabla-historial tbody tr')].map((f) => f.textContent ?? '')
    expect([rechazada.includes('Carla Gutiérrez Rojas'), rechazada.includes('Nadie: se rechazó'), aprobada.includes('Luis Fernando Mamani Choque')]).toEqual([true, true, true])
    http.expectNone(NOMBRES) // los ids del historial ya tenían nombre: no se vuelve a preguntar
  })

  it('correcto: resuelve los nombres con UNA llamada por carga, sin repetir ids ni mandar datos de más', async () => {
    const fixture = TestBed.createComponent(PantallaDeParametrosFinancieros)
    fixture.detectChanges()
    http.expectOne(CATALOGO).flush([PARAMETRO])
    const pedido = http.expectOne(NOMBRES)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.body).toEqual({ usuarioIds: [PROPONENTE, APROBADOR] })
    expect(pedido.request.headers.has('Idempotency-Key')).toBe(false)
    pedido.flush(OPERADORES)
    await fixture.whenStable()
    fixture.detectChanges()
    const operadores = [...raiz(fixture).querySelectorAll('.operador')]
    expect(operadores.map((o) => o.firstChild?.textContent?.trim())).toEqual(['Carla Gutiérrez Rojas', 'Luis Fernando Mamani Choque', 'Carla Gutiérrez Rojas'])
    expect(operadores[0]?.getAttribute('title')).toBe(`Identificador interno del operador: ${PROPONENTE}`)
  })

  it('límite: un id que identidad no devuelve se muestra como «Operador no disponible», con su identificador en la ayuda', async () => {
    const fixture = await cargar([OPERADORES[0]])
    const aprobo = raiz(fixture).querySelector('.carril.rige .operador.no-disponible')
    expect(aprobo?.textContent).toContain('Operador no disponible')
    expect(aprobo?.getAttribute('title')).toContain(APROBADOR)
    expect(raiz(fixture).textContent).toContain('Carla Gutiérrez Rojas')
  })

  it('inválido: si identidad falla, la pantalla sigue entera y dice «Operador no disponible», sin el error crudo', async () => {
    const fixture = await cargar(null)
    http.expectNone(NOMBRES)
    expect(raiz(fixture).querySelectorAll('.operador.no-disponible').length).toBe(3)
    expect(raiz(fixture).textContent).toContain('5.000000')
    expect(raiz(fixture).textContent).not.toContain('Buscando el nombre')
    expect(raiz(fixture).textContent).not.toContain('detalle interno')
  })
})
