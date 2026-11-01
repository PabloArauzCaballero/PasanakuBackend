import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { CABECERA_DE_HASH, type EstadoFinancieroGenerado } from '../dominio/cu106-estados-financieros'
import { PantallaDeEstadosFinancieros } from './pantalla-de-estados-financieros'

const PERIODO = '6ba86e52-9a88-4126-8edf-108c0630ef78'
const URL_GENERAR = `http://gw/api/v1/erp/periodos/${PERIODO}/estados-financieros`
const GENERADO = ejemploDe('erp', 'generarEstadoFinanciero', 'ok').cuerpo as EstadoFinancieroGenerado
const URL_DOCUMENTO = `http://gw/api/v1/erp/estados-financieros/${GENERADO.estadoFinancieroId}/documento`

/**
 * **El gate propio del carril B3, punto 2, en la pantalla.** El hash que se muestra y el
 * que viaja con la descarga son el que devolvió el backend en la generación — sale del
 * ejemplo real del contrato, y la pantalla no lo recompone en ningún paso.
 */
const guardados: string[] = []
const URL_VIGENTE = 'http://gw/api/v1/erp/periodos/vigente'

describe('PantallaDeEstadosFinancieros · el hash es el del backend', () => {
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
    URL.createObjectURL = vi.fn(() => 'blob:documento')
    URL.revokeObjectURL = vi.fn()
    guardados.length = 0
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      guardados.push(this.download)
    })
  })
  afterEach(() => vi.restoreAllMocks())

  function montar(periodoCerrado = true) {
    const fixture = TestBed.createComponent(PantallaDeEstadosFinancieros)
    fixture.componentRef.setInput('periodoId', PERIODO)
    fixture.componentRef.setInput('periodoNombre', 'marzo 2026')
    fixture.componentRef.setInput('periodoCerrado', periodoCerrado)
    fixture.detectChanges()
    return fixture
  }

  function generar(fixture: ReturnType<typeof montar>) {
    const boton = [...fixture.nativeElement.querySelectorAll('ap-boton button')].find((b) =>
      (b.textContent ?? '').includes('Generar'),
    ) as HTMLButtonElement
    boton.click()
    http.expectOne(URL_GENERAR).flush(GENERADO)
    fixture.detectChanges()
  }

  it('tras generar, el hash del backend está a la vista, sin recomponerlo', () => {
    const fixture = montar()
    generar(fixture)
    expect(fixture.nativeElement.textContent).toContain(GENERADO.hashContenido)
  })

  it('la descarga entrega el archivo al navegador con el hash del backend en el nombre', () => {
    const fixture = montar()
    generar(fixture)
    const descargar = [...fixture.nativeElement.querySelectorAll('ap-boton button')].find((b) =>
      (b.textContent ?? '').includes('Descargar'),
    ) as HTMLButtonElement
    descargar.click()
    http.expectOne(URL_DOCUMENTO).flush(new Blob(['sellado']), { headers: { [CABECERA_DE_HASH]: GENERADO.hashContenido } })
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain(GENERADO.hashContenido.slice(0, 8))
    expect(fixture.nativeElement.textContent).not.toContain('no corresponde al estado financiero generado')
    expect(guardados).toHaveLength(1)
    expect(guardados[0]).toContain(GENERADO.hashContenido.slice(0, 8))
    expect(guardados[0]).toMatch(/^balance-general-marzo-2026-.+\.pdf$/)
    expect(fixture.nativeElement.textContent).toContain('Documento descargado')
  })

  it('si el documento que llega no es el generado, NO se entrega el archivo y se dice por qué', () => {
    const fixture = montar()
    generar(fixture)
    const descargar = [...fixture.nativeElement.querySelectorAll('ap-boton button')].find((b) =>
      (b.textContent ?? '').includes('Descargar'),
    ) as HTMLButtonElement
    descargar.click()
    http.expectOne(URL_DOCUMENTO).flush(new Blob(['otro documento']), { headers: { [CABECERA_DE_HASH]: 'hash-que-no-es' } })
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('no corresponde al estado financiero generado')
    expect(fixture.nativeElement.textContent).not.toContain('Documento descargado')
    expect(guardados).toHaveLength(0)
  })

  it('un período todavía abierto avisa que el estado es provisorio', () => {
    const fixture = montar(false)
    expect(fixture.nativeElement.textContent).toContain('Provisorio')
  })

  it('un período cerrado avisa que el estado es definitivo', () => {
    const fixture = montar(true)
    expect(fixture.nativeElement.textContent).toContain('Definitivo')
  })

  it('si el período de la URL es el de hoy, su nombre y estado salen del servidor, no de un valor por omisión', async () => {
    const fixture = TestBed.createComponent(PantallaDeEstadosFinancieros)
    fixture.componentRef.setInput('periodoId', PERIODO)
    fixture.detectChanges()
    await new Promise((r) => setTimeout(r, 0))
    http.expectOne(URL_VIGENTE).flush({ periodoId: PERIODO, ejercicioId: 'e', anio: 2026, mes: 3, estado: 'CERRADO', fechaInicio: '2026-03-01', fechaFin: '2026-03-31' })
    await new Promise((r) => setTimeout(r, 0))
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('marzo 2026')
    expect(fixture.nativeElement.textContent).toContain('Definitivo')
  })

  it('error · la generación rechazada se dice y el botón no queda trabado', () => {
    const fixture = montar()
    const boton = [...fixture.nativeElement.querySelectorAll('ap-boton button')].find((b) => (b.textContent ?? '').includes('Generar')) as HTMLButtonElement
    boton.click()
    http.expectOne(URL_GENERAR).flush({ codigo: 'X', mensaje: 'interno', trazaId: 'tz-1' }, { status: 500, statusText: 'x' })
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).not.toContain('interno')
    expect(boton.disabled).toBe(false)
  })
})
