import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed, type ComponentFixture } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { elegir, estabilizar, pulsar, texto } from '../compartido/pruebas.apoyo'
import type { EventoBitacora, PaginaDeBitacora } from '../dominio/bitacora'
import { PantallaBitacora } from './pantalla-bitacora'

const BASE = 'http://gw/api/v1/auditoria/bitacora'
/** Eventos sintéticos y deterministas (regla 97.6). */
function pagina(desde: number, n: number, siguiente: boolean): PaginaDeBitacora {
  const items: EventoBitacora[] = Array.from({ length: n }, (_, i) => ({
    id: `00000000-0000-4000-8000-${String(desde + i).padStart(12, '0')}`, secuencia: 1000 - desde - i, entidad: i % 2 ? 'USUARIO' : 'GRUPO', entidadId: 'e-1',
    accion: i === 0 ? 'CAMBIO_PERMISOS' : 'ACTUALIZACION', actorRol: 'ADMIN_PLATAFORMA', origen: 'PANEL_ADMIN', correlationId: 'c-1', hashRegistro: `hash-${desde + i}`, fechaHora: '2026-10-25T10:00:00Z',
  }))
  return { items, totalAproximado: 60, ...(siguiente ? { proximoAntesDe: '2026-10-24T00:00:00Z', proximoAntesDeId: `cursor-${desde + n}` } : {}) }
}
const pedido = (http: HttpTestingController): TestRequest => http.expectOne((r) => r.url.startsWith(BASE))
const consulta = (r: TestRequest) => new URL(r.request.urlWithParams).searchParams
const exportables = (el: Element) => texto(el.querySelector('ap-exportar-listado .cuenta'))
/** Deja correr los efectos y la detección de cambios hasta que la vista emita su próxima request (máx. 10 vueltas). */
async function hastaQueSalga(f: ComponentFixture<unknown>, http: HttpTestingController): Promise<TestRequest> {
  for (let i = 0; i < 10; i++) {
    const [r] = http.match((x) => x.url.startsWith(BASE))
    if (r) return r
    f.detectChanges()
    await Promise.resolve()
  }
  throw new Error('La vista no pidió la página siguiente')
}

describe('PantallaBitacora · paginación por cursor del servidor (auditoria.yaml)', () => {
  let http: HttpTestingController
  let f: ComponentFixture<PantallaBitacora>
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }] })
    http = TestBed.inject(HttpTestingController)
    // La descarga de la bitácora exige su permiso de exportación (regla 90.2): esta suite la ejercita.
    TestBed.inject(Sesion).abrir('token', ['exportar:bitacora'], 'PLATAFORMA')
    f = TestBed.createComponent(PantallaBitacora)
    f.detectChanges()
  })
  afterEach(() => http.verify())

  it('aceptado: la primera página va sin cursor con limite=25; Siguiente manda antesDe y antesDeId', async () => {
    const r1 = pedido(http)
    expect(consulta(r1).get('limite')).toBe('25')
    expect(consulta(r1).has('antesDe')).toBe(false)
    r1.flush(pagina(0, 25, true))
    const el = await estabilizar(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(25)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toContain('Página 1 de 3')
    pulsar(el.querySelector('ap-paginacion')!, 'Siguiente')
    f.detectChanges()
    const r2 = pedido(http)
    expect(consulta(r2).get('antesDeId')).toBe('cursor-25')
    expect(consulta(r2).get('antesDe')).toBe('2026-10-24T00:00:00Z')
    r2.flush(pagina(25, 25, true))
    await estabilizar(f)
    expect(texto(el.querySelector('ap-paginacion .resumen'))).toContain('Página 2 de 3')
  })

  it('aceptado: «Ir a 3» recorre la 2 y pide la 3 con su cursor; filtrar por entidad viaja en la consulta y vuelve a la 1', async () => {
    pedido(http).flush(pagina(0, 25, true))
    const el = await estabilizar(f)
    const ir = el.querySelector('ap-paginacion input[type=number]') as HTMLInputElement
    ir.value = '3'
    ir.form!.dispatchEvent(new Event('submit'))
    f.detectChanges()
    pedido(http).flush(pagina(25, 25, true))
    // La respuesta de la 2 le da el cursor de la 3 al paginador, que pide la 3 sola: se espera esa request, no un tiempo.
    const r3 = await hastaQueSalga(f, http)
    expect(consulta(r3).get('antesDeId')).toBe('cursor-50')
    r3.flush(pagina(50, 10, false))
    await estabilizar(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(10)
    elegir(el, 'Entidad', 'USUARIO')
    f.detectChanges()
    const rf = pedido(http)
    expect(consulta(rf).get('entidad')).toBe('USUARIO')
    expect(consulta(rf).has('antesDeId')).toBe(false)
    rf.flush(pagina(0, 3, false))
    await estabilizar(f)
  })

  it('límite: buscar en la página cargada achica la tabla y lo descargable; sin coincidencias ofrece «Quitar filtros»', async () => {
    pedido(http).flush(pagina(0, 4, false))
    const el = await estabilizar(f)
    expect(exportables(el)).toBe('4 filas')
    f.componentInstance['enPagina'].buscar('cambio permisos')
    await estabilizar(f)
    expect(exportables(el)).toBe('1 fila')
    f.componentInstance['enPagina'].buscar('zzz-no-existe')
    await estabilizar(f)
    pulsar(el, 'Quitar filtros')
    // Solo había búsqueda en la página: la consulta al servidor no cambia y no se vuelve a pedir.
    await estabilizar(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(4)
  })

  it('aceptado: la única acción de fila es Ver detalle (append-only, sin Editar ni Eliminar)', async () => {
    pedido(http).flush(pagina(0, 2, false))
    const el = await estabilizar(f)
    expect(Array.from(el.querySelectorAll('tbody button')).map((b) => texto(b))).toEqual(['Ver detalle', 'Ver detalle'])
  })

  it('error: un 500 muestra el error traducido y «Volver a intentar» repite el mismo GET', async () => {
    pedido(http).flush({ mensaje: 'org.postgresql crudo' }, { status: 500, statusText: 'Error' })
    const el = await estabilizar(f)
    expect(el.querySelector('ap-estado-de-error')).not.toBeNull()
    expect(texto(el)).not.toContain('postgresql')
    pulsar(el, 'Volver a intentar')
    f.detectChanges()
    pedido(http).flush(pagina(0, 1, false))
    await estabilizar(f)
    expect(el.querySelectorAll('tbody tr').length).toBe(1)
  })
})
