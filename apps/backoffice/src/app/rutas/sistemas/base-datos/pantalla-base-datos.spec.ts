import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { SaludDeServicioEstadoEnum as E, type EsquemaDelCatalogo, type ResumenDelCatalogo, type SalidaSaludDeServicios } from 'clientes/angular/auditoria'
import { axe } from 'vitest-axe'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeEsquemas } from '../dominio/base-de-datos'
import { PantallaBaseDeDatos } from './pantalla-base-datos'

const URL_RESUMEN = 'http://gw/api/v1/indicadores/catalogo-de-datos/resumen'
const URL_SALUD = 'http://gw/api/v1/indicadores/operacion/servicios'

// Sintético: la forma de `ResumenDelCatalogo` y `SalidaSaludDeServicios` (auditoria.yaml), no la base real.
const esquema = (nombre: string, servicio: string | undefined, tablas: number): EsquemaDelCatalogo => ({
  nombre,
  ...(servicio ? { servicio } : {}),
  descripcion: `Lo que guarda ${nombre}`,
  tablas,
  tablasDeNegocio: tablas,
  columnas: tablas * 10,
  relaciones: tablas,
  appendOnly: 1,
  conDatosPersonales: 2,
})
const RESUMEN = {
  tablas: 39,
  columnas: 390,
  relaciones: 39,
  esquemas: [esquema('identidad', 'identidad', 20), esquema('nucleo_financiero', 'nucleo-financiero', 15), esquema('catalogo', undefined, 4)],
} as unknown as ResumenDelCatalogo
const SALUD: SalidaSaludDeServicios = {
  consultadoEn: '2026-10-28T19:04:05Z',
  servicios: [
    { servicio: 'identidad', estado: E.Arriba, latenciaMs: 9 },
    { servicio: 'nucleo-financiero', estado: E.Caido },
  ],
}

describe('filasDeEsquemas · un esquema por fila con la conexión de su dueño', () => {
  it('correcto: cruza esquema con servicio aunque uno use guion bajo y el otro guion', () => {
    const [identidad, nucleo, catalogo] = filasDeEsquemas(RESUMEN, SALUD)
    expect(identidad).toMatchObject({ esquema: 'identidad', conexion: 'conectado', tono: 'ok', tablas: 20 })
    expect(nucleo).toMatchObject({ esquema: 'nucleo_financiero', conexion: 'el servicio no responde', tono: 'error' })
    expect(catalogo).toMatchObject({ servicio: '—', conexion: 'compartido' })
  })

  it('límite: sin la salud (falló o sin permiso) dice «sin dato», nunca «conectado»', () => {
    const filas = filasDeEsquemas(RESUMEN, undefined)
    expect(filas.filter((f) => f.servicio !== '—').every((f) => f.conexion === 'sin dato')).toBe(true)
  })

  it('límite: un resumen sin esquemas no inventa filas', () => {
    expect(filasDeEsquemas({ ...RESUMEN, esquemas: [] }, SALUD)).toEqual([])
  })
})

describe('PantallaBaseDeDatos · contra obtenerResumenDelCatalogo y consultarSaludDeServicios (HTTP)', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function montar(resumen: (h: HttpTestingController) => void, salud: (h: HttpTestingController) => void) {
    const fixture = TestBed.createComponent(PantallaBaseDeDatos)
    fixture.detectChanges()
    resumen(http)
    salud(http)
    await fixture.whenStable()
    fixture.detectChanges()
    return fixture.nativeElement as HTMLElement
  }

  it('correcto: pinta cada esquema real con su dueño, su conexión y sus cifras, y enlaza al catálogo', async () => {
    const el = await montar((h) => h.expectOne(URL_RESUMEN).flush(RESUMEN), (h) => h.expectOne(URL_SALUD).flush(SALUD))
    const texto = el.textContent ?? ''
    expect(texto).toContain('nucleo_financiero')
    expect(texto).toContain('conectado')
    expect(texto).toContain('el servicio no responde')
    expect(texto).toContain('390')
    expect(texto).not.toContain('todavía no hay datos')
    expect(el.querySelector('a[href="/tablero/catalogo-de-datos?esquema=identidad"]')).toBeTruthy()
  })

  it('límite: si la salud falla, los esquemas se ven igual con «sin dato»', async () => {
    const el = await montar((h) => h.expectOne(URL_RESUMEN).flush(RESUMEN), (h) => h.expectOne(URL_SALUD).flush({}, { status: 500, statusText: 'Server Error' }))
    expect(el.textContent).toContain('identidad')
    expect(el.textContent).toContain('sin dato')
    expect(el.textContent).not.toContain('conectado')
  })

  it('inválido: si el resumen falla, error accionable y ninguna fila inventada', async () => {
    const el = await montar((h) => h.expectOne(URL_RESUMEN).flush({}, { status: 500, statusText: 'Server Error' }), (h) => h.expectOne(URL_SALUD).flush(SALUD))
    expect(el.querySelector('[role="alert"]')).toBeTruthy()
    expect(el.textContent).not.toContain('nucleo_financiero')
  })

  it('accesibilidad: sin violaciones serias con datos', async () => {
    const el = await montar((h) => h.expectOne(URL_RESUMEN).flush(RESUMEN), (h) => h.expectOne(URL_SALUD).flush(SALUD))
    expect(await axe(el)).toHaveNoViolations()
  })
})
