import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { By } from '@angular/platform-browser'
import { provideRouter } from '@angular/router'
import { aCsv } from '@aportaya/ui/exportar-listado/exportar'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import type { Listado } from '@aportaya/ui/listado/crear-listado'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  ExpedienteEnRevisionEstadoEnum,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { PantallaDeExpedientes } from './pantalla-de-expedientes'

const URL = 'http://gw/api/v1/identidad/verificaciones'

function expediente(correo?: string): ExpedienteEnRevision {
  return {
    verificacionId: 'v-1',
    usuarioId: 'u-1',
    nombreCompleto: 'Ana Torres',
    telefono: '+59170000001',
    correo,
    documento: 'CI SC',
    estado: ExpedienteEnRevisionEstadoEnum.EnRevision,
    iniciadaEn: '2026-10-10T10:00:00Z',
    documentoVigente: true,
    fotos: [],
  }
}

describe('PantallaDeExpedientes · correo de contacto', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  async function renderizar(correo?: string): Promise<string> {
    const fixture = TestBed.createComponent(PantallaDeExpedientes)
    fixture.detectChanges()
    http.expectOne(URL).flush([expediente(correo)])
    await fixture.whenStable()
    fixture.detectChanges()
    return (fixture.nativeElement as HTMLElement).textContent ?? ''
  }

  it('muestra el correo recibido del expediente y lo nombra en el buscador', async () => {
    const texto = await renderizar('ana@example.test')

    expect(texto).toContain('Correo: ana@example.test')
    expect(texto).toContain('Buscar por nombre, teléfono, correo o documento')
  })

  it('explica cuando el expediente antiguo no trae correo', async () => {
    const texto = await renderizar()

    expect(texto).toContain('Correo: No registrado')
  })
})

/** Los tres niveles del listado de expedientes (kit `crearListado`), contra el doble HTTP del contrato de identidad. */
describe('PantallaDeExpedientes · listado (buscar, filtrar, paginar, descargar)', () => {
  let http: HttpTestingController
  const muchos = (n: number): ExpedienteEnRevision[] =>
    Array.from({ length: n }, (_, i) => ({ ...expedienteBase(), verificacionId: `v-${i + 1}`, nombreCompleto: `Persona ${i + 1}`, telefono: `+5917000${String(i).padStart(4, '0')}` }))
  function expedienteBase(): ExpedienteEnRevision {
    return { verificacionId: 'v-1', usuarioId: 'u-1', nombreCompleto: 'Ana Torres', telefono: '+59170000001', correo: 'ana@example.test', documento: 'CI 1234567 SC', estado: ExpedienteEnRevisionEstadoEnum.EnRevision, iniciadaEn: '2026-10-10T10:00:00Z', documentoVigente: true, fechaExpiracionDocumento: '2030-01-01', fotos: [] }
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar(respuesta: ExpedienteEnRevision[]) {
    const fixture = TestBed.createComponent(PantallaDeExpedientes)
    fixture.detectChanges()
    http.expectOne(URL).flush(respuesta)
    await fixture.whenStable()
    fixture.detectChanges()
    const p = fixture.componentInstance as unknown as { listado: Listado<ExpedienteEnRevision>; alFiltrar: (e: { nombre: string; valor: string }) => void }
    return { fixture, p, texto: () => (fixture.nativeElement as HTMLElement).textContent ?? '' }
  }

  it('correcto: busca por nombre, teléfono, correo y documento sin acentos; filtra por vencimiento', async () => {
    const otra = { ...expedienteBase(), verificacionId: 'v-2', nombreCompleto: 'José Quispe', telefono: '+59171111111', correo: 'jose@example.test', documento: 'CI 7654321 LP', documentoVigente: false }
    const { fixture, p } = await montar([expedienteBase(), otra])
    for (const termino of ['jose', '71111111', 'jose@example', '7654321']) {
      p.listado.buscar(termino)
      expect(p.listado.filtradas().map((e) => e.verificacionId), termino).toEqual(['v-2'])
    }
    p.listado.buscar('')
    p.alFiltrar({ nombre: 'vencimiento', valor: 'Documento vencido' })
    fixture.detectChanges()
    expect(p.listado.filtradas().map((e) => e.verificacionId)).toEqual(['v-2'])
  })

  it('correcto: el estado se pide al servidor; «Por decidir» es la opción sin filtro', async () => {
    const { fixture, p } = await montar([expedienteBase()])
    p.alFiltrar({ nombre: 'estado', valor: 'APROBADA' })
    fixture.detectChanges()
    http.expectOne(`${URL}?estado=APROBADA`).flush([])
    await fixture.whenStable()
  })

  it('correcto: la descarga lleva lo filtrado con los valores que se ven, sin ids internos', async () => {
    const { fixture } = await montar([expedienteBase()])
    const exportar = fixture.debugElement.query(By.directive(ExportarListado)).componentInstance as ExportarListado<ExpedienteEnRevision>
    const csv = aCsv(exportar.columnas(), exportar.filas())
    expect(csv).toContain('Nombre,Teléfono,Correo,Documento,Vencimiento del documento,Fotos,Estado')
    // El «+» inicial del teléfono sale con «'» delante: el kit neutraliza lo que una planilla leería como fórmula.
    expect(csv).toContain("Ana Torres,'+59170000001,ana@example.test,CI 1234567 SC,Vigente,Incompletas,EN_REVISION")
    expect(csv).not.toContain('u-1')
  })

  it('límite: 25 expedientes con 10 por página → página 1 de 3; ir a la 3 muestra 5', async () => {
    const { fixture, p, texto } = await montar(muchos(25))
    expect(texto()).toContain('Página 1 de 3')
    expect(fixture.nativeElement.querySelectorAll('ap-tarjeta-de-expediente').length).toBe(10)
    p.listado.irAPagina(3)
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelectorAll('ap-tarjeta-de-expediente').length).toBe(5)
  })

  it('límite: una búsqueda sin coincidencias explica por qué y ofrece «Quitar filtros», que vuelve a mostrar todo', async () => {
    const { fixture, p, texto } = await montar([expedienteBase()])
    p.listado.buscar('zzz')
    fixture.detectChanges()
    expect(texto()).toContain('No hay expedientes que coincidan con la búsqueda.')
    const quitar = [...fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.includes('Quitar filtros')) as HTMLButtonElement
    quitar.click()
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelectorAll('ap-tarjeta-de-expediente').length).toBe(1)
  })

  it('inválido: un 500 muestra un error accionable y «Volver a intentar» repite el pedido', async () => {
    const fixture = TestBed.createComponent(PantallaDeExpedientes)
    fixture.detectChanges()
    http.expectOne(URL).flush({ codigo: 'X', mensaje: 'crudo', trazaId: 'traza-9' }, { status: 500, statusText: 'x' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('traza-9')
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('crudo')
    const reintentar = [...fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.includes('Volver a intentar')) as HTMLButtonElement
    reintentar.click()
    fixture.detectChanges()
    http.expectOne(URL).flush([])
    await fixture.whenStable()
  })
})
