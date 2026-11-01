import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { CatalogoOperativoResumenEstadoEnum } from 'clientes/angular/auditoria'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CatalogoOperativoResumen } from '../dominio/catalogos-operativos'
import { GW_DE_PRUEBA, probarListadoHttp } from '../compartido/pruebas-http.apoyo'
import { listadoDe } from '../compartido/pruebas.apoyo'
import { PantallaCatalogosOperativos } from './pantalla-catalogos-operativos'

const CATALOGOS_EJEMPLO: CatalogoOperativoResumen[] = [
  {
    codigo: 'HECHOS_GENERADORES',
    nombre: 'Hechos Generadores',
    dueno: 'Contabilidad y Reglas',
    version: '1.2.0',
    totalElementos: 15,
    elementosActivos: 15,
    vigenteDesde: '2026-01-01',
    estado: CatalogoOperativoResumenEstadoEnum.Vigente,
    descripcion: 'Eventos transaccionales que disparan asientos contables.',
  },
  {
    codigo: 'UMBRALES_UIF',
    nombre: 'Umbrales UIF',
    dueno: 'Cumplimiento Normativo',
    version: '2.0.0',
    totalElementos: 4,
    elementosActivos: 4,
    vigenteDesde: '2026-03-01',
    estado: CatalogoOperativoResumenEstadoEnum.Vigente,
    descripcion: 'Límites normativos de debida diligencia.',
  },
]

describe('PantallaCatalogosOperativos', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  afterEach(() => http.verify())

  it('renderiza la lista de catálogos operativos con sus metadatos', async () => {
    const fixture = TestBed.createComponent(PantallaCatalogosOperativos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/auditoria/catalogos').flush(CATALOGOS_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('Catálogos operativos')
    expect(texto).toContain('HECHOS_GENERADORES')
    expect(texto).toContain('Hechos Generadores')
    expect(texto).toContain('UMBRALES_UIF')
    expect(texto).toContain('Cumplimiento Normativo')
    expect(texto).toContain('15 / 15')
  })

  it('filtra los catálogos según el término de búsqueda', async () => {
    const fixture = TestBed.createComponent(PantallaCatalogosOperativos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/auditoria/catalogos').flush(CATALOGOS_EJEMPLO)
    await fixture.whenStable()
    fixture.detectChanges()

    listadoDe(fixture).buscar('UIF')
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('UMBRALES_UIF')
    expect(texto).not.toContain('HECHOS_GENERADORES')
  })

  it('muestra estado vacío cuando no hay catálogos', async () => {
    const fixture = TestBed.createComponent(PantallaCatalogosOperativos)
    fixture.detectChanges()

    http.expectOne('http://gw/api/v1/auditoria/catalogos').flush([])
    await fixture.whenStable()
    fixture.detectChanges()

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('No se encontraron catálogos operativos')
  })
})

describe('PantallaCatalogosOperativos · listado del kit en los tres niveles', () => {
  probarListadoHttp<CatalogoOperativoResumen>({
    componente: PantallaCatalogosOperativos,
    url: `${GW_DE_PRUEBA}/auditoria/catalogos`,
    filas: (n) => Array.from({ length: n }, (_, i) => ({ codigo: i === 6 ? 'FERIADOS_BANCARIOS' : `CATALOGO_${i + 1}`, nombre: `Catálogo ${i + 1}`, dueno: i % 5 === 0 ? 'Tesorería' : 'Cumplimiento', version: '1.0.0', totalElementos: 4, elementosActivos: 3, vigenteDesde: '2026-01-01', estado: i % 2 ? CatalogoOperativoResumenEstadoEnum.EnRevision : CatalogoOperativoResumenEstadoEnum.Vigente, descripcion: 'Catálogo sintético de prueba' })),
    textoVacio: 'No se encontraron catálogos operativos.',
    busqueda: { termino: 'feriados', esperadas: 1 },
    filtro: { etiqueta: 'Dueño', valor: 'Tesorería', esperadas: 5 },
  })
})
