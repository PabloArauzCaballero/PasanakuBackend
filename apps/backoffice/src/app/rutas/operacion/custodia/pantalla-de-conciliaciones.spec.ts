import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ConciliacionEnLista } from './dominio/cu50-conciliaciones'
import { PantallaDeConciliaciones } from './pantalla-de-conciliaciones'

/** **UI contra HTTP simulado** de `listarConciliaciones` (CU-50). Página del ejemplo generado del contrato (sintética). */
const URL = 'http://gw/api/v1/custodia/conciliaciones'
const OK = ejemploDe('nucleo-financiero', 'listarConciliaciones', 'ok').cuerpo as { items: ConciliacionEnLista[]; proximoAntesDe: string; proximoAntesDeId: string }

describe('PantallaDeConciliaciones · [UI contra HTTP simulado] CU-50', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function montar(permisos: string[]): Promise<ComponentFixture<PantallaDeConciliaciones>> {
    TestBed.inject(Sesion).abrir('t', permisos, 'r')
    const f = TestBed.createComponent(PantallaDeConciliaciones)
    f.detectChanges()
    await f.whenStable()
    return f
  }
  const listado = () => http.expectOne((r) => r.method === 'GET' && r.url === URL)
  async function responder(f: ComponentFixture<unknown>, p: TestRequest, cuerpo: object, estado = 200) {
    p.flush(cuerpo, { status: estado, statusText: 'x' })
    await new Promise((r) => setTimeout(r, 0))
    await f.whenStable()
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''

  it('correcto: primera página sin cursor y la segunda con antesDe/antesDeId del servidor; Tesorería ve el formulario', async () => {
    const f = await montar(['AUDITORIA_LEER', 'TESORERIA'])
    const primera = listado()
    expect(primera.request.params.keys()).toEqual(['limite'])
    await responder(f, primera, OK)
    expect(f.nativeElement.querySelector('ap-formulario-de-conciliacion')).not.toBeNull()
    const tabla = f.debugElement.query((d) => d.componentInstance instanceof TablaDeDatosVirtualizada).componentInstance as TablaDeDatosVirtualizada<ConciliacionEnLista>
    expect(tabla.filas()).toEqual(OK.items)
    tabla.pagina.set(2)
    f.detectChanges()
    const segunda = listado()
    expect(segunda.request.params.get('antesDe')).toBe(OK.proximoAntesDe)
    expect(segunda.request.params.get('antesDeId')).toBe(OK.proximoAntesDeId)
    await responder(f, segunda, { items: [], proximoAntesDe: null, proximoAntesDeId: null })
  })

  it('límite: sin el rol TESORERIA no hay formulario (lo explica) y la bandeja vacía se explica', async () => {
    const f = await montar(['AUDITORIA_LEER'])
    await responder(f, listado(), { items: [], proximoAntesDe: null, proximoAntesDeId: null })
    expect(f.nativeElement.querySelector('ap-formulario-de-conciliacion')).toBeNull()
    expect(texto(f)).toContain('Registrar una conciliación es de Tesorería')
    expect(texto(f)).toContain('Todavía no hay conciliaciones registradas.')
  })

  it('inválido: 403 ⇒ sin acceso y sin tabla', async () => {
    const f = await montar(['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-GW-403', mensaje: 'x', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(f.nativeElement.querySelector('ap-tabla-de-datos-virtualizada')).toBeNull()
  })

  it('inválido: 422 AP-CU50-04 ⇒ error traducido, sin el mensaje crudo, y reintento', async () => {
    const f = await montar(['AUDITORIA_LEER'])
    await responder(f, listado(), { codigo: 'AP-CU50-04', mensaje: 'crudo', trazaId: 'traza-50' }, 422)
    expect(texto(f)).toContain('No pudimos traer la bandeja.')
    expect(texto(f)).not.toContain('crudo')
    f.componentInstance['b'].reintentar()
    f.detectChanges()
    await f.whenStable()
    await responder(f, listado(), OK)
    expect(f.componentInstance['b'].estado()).toBe('datos')
  })
})
