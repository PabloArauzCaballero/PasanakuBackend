import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { IndicadorFamiliaEnum, SalidaTableroDimensionEnum, type Indicador, type SalidaTablero } from 'clientes/angular/auditoria'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../nucleo/traza.interceptor'
import { TableroPorRol, type TipoDeTablero } from './tablero-por-rol'

const URL = 'http://gw/api/v1/indicadores'

const indicador = (codigo: string, extra: Partial<Indicador> = {}): Indicador => ({
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

const salida = (...indicadores: Indicador[]): SalidaTablero => ({
  periodo: '2026-09',
  provisorio: false,
  dimension: SalidaTableroDimensionEnum.Global,
  indicadores,
})

describe('TableroPorRol · los cuatro estados y null no es cero', () => {
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

  async function montar(tipo: TipoDeTablero) {
    const fixture = TestBed.createComponent(TableroPorRol)
    fixture.componentRef.setInput('tipo', tipo)
    fixture.detectChanges()
    // `whenStable` esperaria a la peticion pendiente, que este test es quien responde: se dispara el efecto a mano.
    TestBed.inject(ApplicationRef).tick()
    return fixture
  }

  async function responder(fixture: Awaited<ReturnType<typeof montar>>, cuerpo: SalidaTablero) {
    const req = http.expectOne((r) => r.url === URL)
    req.flush(cuerpo)
    await fixture.whenStable()
    fixture.detectChanges()
    return req
  }

  it('correcto: pide el mes cerrado anterior, muestra el importe con su moneda y los ausentes como «sin datos»', async () => {
    const fixture = await montar('financiero')
    const req = await responder(fixture, salida(indicador('SALDO_CARGADO', { variacionPeriodoAnterior: '10.00' })))

    expect(req.request.params.get('periodo')).toMatch(/^\d{4}-\d{2}$/)
    const html = fixture.nativeElement as HTMLElement
    expect(html.querySelector('[data-indicador="SALDO_CARGADO"] ap-tarjeta-kpi')).not.toBeNull()
    expect(html.querySelector('[data-indicador="SALDO_CARGADO"]')!.textContent).toContain('10 % más que el período anterior')
    expect(html.querySelector('[data-indicador="SALDO_RETIRADO"]')!.textContent).toContain('Sin datos en el período')
    // Nunca un cero inventado para lo que no llegó.
    expect(html.querySelector('[data-indicador="SALDO_RETIRADO"] ap-tarjeta-kpi')).toBeNull()
  })

  it('límite: un indicador suprimido por privacidad no muestra valor y dice el mínimo de casos', async () => {
    const fixture = await montar('financiero')
    await responder(fixture, salida(indicador('TASA_MORA', { valor: undefined, suprimidoPorPrivacidad: true, minimoCasos: 20 })))

    const tarjeta = (fixture.nativeElement as HTMLElement).querySelector('[data-indicador="TASA_MORA"]')!
    expect(tarjeta.textContent).toContain('Oculto por privacidad')
    expect(tarjeta.textContent).toContain('menos de 20 casos')
  })

  it('límite: el comercial dibuja el embudo con el número de cada paso al lado de su barra', async () => {
    const fixture = await montar('comercial')
    await responder(
      fixture,
      salida(
        indicador('USUARIOS_NUEVOS', { valor: '40', unidad: 'CONTEO', familia: IndicadorFamiliaEnum.Negocio }),
        indicador('EMBUDO_KYC_APROBADO', { valor: '30', unidad: 'CONTEO', familia: IndicadorFamiliaEnum.Negocio }),
      ),
    )

    const embudo = (fixture.nativeElement as HTMLElement).querySelector('.embudo')!
    expect(embudo.textContent).toContain('Se registraron')
    expect(embudo.textContent).toContain('40')
    expect(embudo.textContent).toContain('Sin datos')
  })

  it('límite: sin ningún indicador en el período, el vacío orienta a elegir un período cerrado', async () => {
    const fixture = await montar('financiero')
    await responder(fixture, salida())

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('elegí un período cerrado')
  })

  it('inválido: un 403 del servidor se muestra como error accionable, no como tablero vacío', async () => {
    const fixture = await montar('comercial')
    http.expectOne((r) => r.url === URL).flush({ codigo: 'AP-SEG-403', mensaje: 'x' }, { status: 403, statusText: 'Forbidden' })
    await fixture.whenStable()
    fixture.detectChanges()

    const html = fixture.nativeElement as HTMLElement
    expect(html.textContent).not.toContain('elegí un período cerrado')
    expect(html.querySelector('[data-indicador]')).toBeNull()
  })
})
