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
import type { ParametroFinanciero, VersionDeParametro } from '../dominio/cu132-parametros'
import { PantallaDeParametrosFinancieros } from './pantalla-de-parametros-financieros'

/** La pantalla usa un doble HTTP con la forma del OpenAPI; las pruebas de persistencia del historial corren en CU132Test. */
const GW = 'http://gw/api/v1'
const CATALOGO = `${GW}/tarifas/parametros`
const PARAMETRO = {
  codigo: 'DIAS_DE_GRACIA',
  descripcion: 'Días de gracia',
  unidad: 'DIAS',
  porNivel: false,
  vigentes: [{ id: 'v-aprobada', codigo: 'DIAS_DE_GRACIA', valor: '5.000000', moneda: null, nivel: null, vigenteDesde: '2026-10-01T00:00:00Z', estado: 'APROBADA' }],
  propuestas: [{ id: 'v-propuesta', codigo: 'DIAS_DE_GRACIA', valor: '6', moneda: null, nivel: null, vigenteDesde: '2026-12-01T00:00:00Z', estado: 'PROPUESTA' }],
} as unknown as ParametroFinanciero
const VERSION_RECHAZADA = { id: 'v-rechazada', codigo: 'DIAS_DE_GRACIA', valor: '7', moneda: null, nivel: null, vigenteDesde: '2026-09-01T00:00:00Z', estado: 'RECHAZADA' } as unknown as VersionDeParametro

describe('PantallaDeParametrosFinancieros · CU-132 [UI con API simulada]', () => {
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

  function montar(): ComponentFixture<PantallaDeParametrosFinancieros> {
    const fixture = TestBed.createComponent(PantallaDeParametrosFinancieros)
    fixture.detectChanges()
    return fixture
  }

  async function cargar(fixture: ComponentFixture<PantallaDeParametrosFinancieros>, lista: ParametroFinanciero[]): Promise<void> {
    http.expectOne(CATALOGO).flush(lista)
    await fixture.whenStable()
    fixture.detectChanges()
  }

  const texto = (fixture: ComponentFixture<unknown>) => (fixture.nativeElement as HTMLElement).textContent ?? ''
  const api = (fixture: ComponentFixture<PantallaDeParametrosFinancieros>) => fixture.componentInstance as unknown as {
    abrirPropuesta(p: ParametroFinanciero): void
    editar(campo: string, valor: string): void
    enviarPropuesta(p: ParametroFinanciero): void
    aprobar(v: VersionDeParametro): void
    alternarHistorial(codigo: string): void
    erroresDeCampo(): Record<string, string | undefined>
  }

  it('correcto: muestra versiones vigentes y propuestas, sin inventar valores; avisa de cuatro ojos', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    expect(texto(fixture)).toContain('5.000000')
    expect(texto(fixture)).toContain('6')
    expect(texto(fixture)).toContain('dos sesiones')
    expect((fixture.nativeElement as HTMLElement).querySelector('input')?.getAttribute('value')).not.toBe('5')
  })

  it('correcto: resume de un vistazo cuántos parámetros hay y cuántas propuestas esperan aprobación', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    expect(texto(fixture)).toContain('1 parámetro en el catálogo · 1 propuesta espera aprobación')
    expect(texto(fixture)).toContain('Rige hoy')
    expect(texto(fixture)).toContain('Espera aprobación')
    expect(texto(fixture)).toContain('distinta de quien la propuso')
  })

  it('vacío: orienta sobre el catálogo cerrado, no deja una pantalla muda', async () => {
    const fixture = montar()
    await cargar(fixture, [])
    expect(texto(fixture)).toContain('No hay parámetros disponibles')
    expect(texto(fixture)).toContain('catálogo está cerrado')
  })

  it('error de carga: no expone el mensaje crudo y permite reintentar', async () => {
    const fixture = montar()
    http.expectOne(CATALOGO).flush({ codigo: 'X', mensaje: 'detalle interno', trazaId: 'traza-1' }, { status: 503, statusText: 'Service Unavailable' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).not.toContain('detalle interno')
    expect(texto(fixture)).toContain('Volver a intentar')
    expect(texto(fixture)).toContain('traza-1')
  })

  it('inválido: asocia errores de campos requeridos y valor decimal al input correspondiente', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    api(fixture).abrirPropuesta(PARAMETRO)
    api(fixture).enviarPropuesta(PARAMETRO)
    fixture.detectChanges()
    expect(api(fixture).erroresDeCampo()['valor']).toContain('decimal')
    expect(api(fixture).erroresDeCampo()['fuente']).toContain('Completá')
    expect((fixture.nativeElement as HTMLElement).querySelector('input[aria-invalid="true"]')).not.toBeNull()
    http.expectNone((req) => req.method === 'POST')
  })

  it('correcto: propone con procedencia e Idempotency-Key, conserva cuatro ojos y actualiza el catálogo', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    const c = api(fixture)
    c.abrirPropuesta(PARAMETRO)
    c.editar('valor', '8.5')
    c.editar('vigenteDesde', '2026-12-01T08:00')
    c.editar('fuente', 'Resolución sintética')
    c.editar('referencia', 'REF-TEST-1')
    c.editar('fecha', '2026-11-20')
    c.enviarPropuesta(PARAMETRO)
    const pedido = http.expectOne(`${CATALOGO}/DIAS_DE_GRACIA/versiones`)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    expect(pedido.request.body).toMatchObject({ valor: '8.5', nivel: null, moneda: null, procedencia: { fuente: 'Resolución sintética', referencia: 'REF-TEST-1', fecha: '2026-11-20' } })
    pedido.flush({ ...PARAMETRO.propuestas[0]!, id: 'v-nueva', valor: '8.5' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).toContain('La propuesta quedó registrada')
    http.expectOne(CATALOGO).flush([PARAMETRO])
    await fixture.whenStable()
    fixture.detectChanges()
  })

  it('límite: AP-CU132-02 señala nivel y moneda cuando corresponden al parámetro', async () => {
    const porNivel = { ...PARAMETRO, codigo: 'TASA', porNivel: true, unidad: 'MONTO' } as unknown as ParametroFinanciero
    const fixture = montar()
    await cargar(fixture, [porNivel])
    const c = api(fixture)
    c.abrirPropuesta(porNivel)
    c.editar('valor', '8')
    c.editar('nivel', 'PUNTUAL')
    c.editar('moneda', 'BOB')
    c.editar('vigenteDesde', '2026-12-01T08:00')
    c.editar('fuente', 'Resolución sintética')
    c.editar('referencia', 'REF-TEST-2')
    c.editar('fecha', '2026-11-20')
    c.enviarPropuesta(porNivel)
    http.expectOne(`${CATALOGO}/TASA/versiones`).flush({ codigo: 'AP-CU132-02' }, { status: 422, statusText: 'Unprocessable Entity' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(c.erroresDeCampo()['nivel']).toContain('no coincide')
    expect(c.erroresDeCampo()['moneda']).toContain('no coincide')
  })

  it('correcto: muestra el historial con estados resueltos, incluidos los rechazados', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    api(fixture).alternarHistorial(PARAMETRO.codigo)
    fixture.detectChanges()
    http.expectOne(`${CATALOGO}/DIAS_DE_GRACIA/versiones`).flush([VERSION_RECHAZADA, ...PARAMETRO.vigentes, ...PARAMETRO.propuestas])
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).toContain('Rechazada')
    expect(texto(fixture)).toContain('7')
  })

  it('correcto: aprobación usa una clave idempotente y el permiso de quien inicia esta sesión', async () => {
    const fixture = montar()
    await cargar(fixture, [PARAMETRO])
    api(fixture).aprobar(PARAMETRO.propuestas[0]!)
    const pedido = http.expectOne(`${CATALOGO}/versiones/v-propuesta/aprobacion`)
    expect(pedido.request.method).toBe('POST')
    expect(pedido.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/)
    pedido.flush({ ...PARAMETRO.propuestas[0]!, estado: 'APROBADA' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(texto(fixture)).toContain('La versión quedó aprobada')
    http.expectOne(CATALOGO).flush([PARAMETRO])
    await fixture.whenStable()
    fixture.detectChanges()
  })
})
