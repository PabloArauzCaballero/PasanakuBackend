import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import { BandejaDeRetiros } from './bandeja-de-retiros'
import type { RetiroEnRevision } from './dominio/cu11-retiros-en-revision'

/**
 * **UI contra HTTP simulado** (no prueba integración con `nucleo-financiero`). Las filas salen del ejemplo generado
 * del contrato (`packages/simulado`, sintético). Cubre: página con datos y avance por cursor, vacío, `puedeDecidir=false`,
 * 403 y 422 `AP-CU11-14`.
 */
const URL = 'http://gw/api/v1/billetera/retiros'
const OK = ejemploDe('nucleo-financiero', 'listarRetirosEnRevision', 'ok').cuerpo as { items: RetiroEnRevision[]; proximoFecha: string; proximoId: string }
const FILA = OK.items[0]!

describe('BandejaDeRetiros · [UI contra HTTP simulado] CU-11', () => {
  let http: HttpTestingController
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('t', ['RETIRO_APROBAR'], 'TESORERIA', 'tes-1')
  })

  async function montar(): Promise<ComponentFixture<BandejaDeRetiros>> {
    const f = TestBed.createComponent(BandejaDeRetiros)
    f.detectChanges()
    await f.whenStable()
    return f
  }
  const pedido = () => http.expectOne((r) => r.url === URL)
  async function responder(f: ComponentFixture<unknown>, p: TestRequest, cuerpo: object, estado = 200) {
    p.flush(cuerpo, { status: estado, statusText: 'x' })
    await new Promise((r) => setTimeout(r, 0))
    await f.whenStable()
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  const tabla = (f: ComponentFixture<unknown>) => f.debugElement.query((d) => d.componentInstance instanceof TablaDeDatosVirtualizada)

  it('correcto: pide la primera página sin cursor y la siguiente con el cursor (proximoFecha, proximoId) del servidor', async () => {
    const f = await montar()
    const primera = pedido()
    expect(primera.request.method).toBe('GET')
    expect(primera.request.params.get('limite')).toBe('25')
    expect(primera.request.params.has('despuesDeFecha')).toBe(false)
    await responder(f, primera, OK)
    expect(f.componentInstance['b'].estado()).toBe('datos')
    const t = tabla(f).componentInstance as TablaDeDatosVirtualizada<RetiroEnRevision>
    expect(t.filas()).toEqual(OK.items)
    t.pagina.set(2)
    f.detectChanges()
    const segunda = pedido()
    expect(segunda.request.params.get('despuesDeFecha')).toBe(OK.proximoFecha)
    expect(segunda.request.params.get('despuesDeId')).toBe(OK.proximoId)
    await responder(f, segunda, { items: [], proximoFecha: null, proximoId: null })
  })

  it('límite: la bandeja vacía lo explica y no muestra error', async () => {
    const f = await montar()
    await responder(f, pedido(), { items: [], proximoFecha: null, proximoId: null })
    expect(texto(f)).toContain('No hay retiros esperando una segunda firma.')
    expect(texto(f)).not.toContain('No pudimos traer')
  })

  it('límite: una fila propia (puedeDecidir=false) no navega a decidir; una ajena abre retiros/:ordenId con la fila', async () => {
    const f = await montar()
    await responder(f, pedido(), OK)
    const router = TestBed.inject(Router)
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true)
    f.componentInstance['abrir']({ ...FILA, puedeDecidir: false })
    expect(navegar).not.toHaveBeenCalled()
    f.componentInstance['abrir'](FILA)
    expect(navegar).toHaveBeenCalledWith([FILA.ordenRetiroId], expect.objectContaining({ state: { retiro: FILA } }))
  })

  it('inválido: 403 muestra «sin acceso» y no deja la tabla', async () => {
    const f = await montar()
    await responder(f, pedido(), { codigo: 'AP-GW-403', mensaje: 'crudo', trazaId: 't' }, 403)
    expect(texto(f)).toContain('No tenés acceso a esta bandeja.')
    expect(tabla(f)).toBeNull()
    expect(texto(f)).not.toContain('crudo')
  })

  it('inválido: 422 AP-CU11-14 se muestra traducido, con traza, y «Reintentar» vuelve a pedir', async () => {
    const f = await montar()
    await responder(f, pedido(), { codigo: 'AP-CU11-14', mensaje: 'crudo del backend', trazaId: 'traza-1' }, 422)
    expect(texto(f)).toContain('No pudimos traer la bandeja.')
    expect(texto(f)).toContain('traza-1')
    expect(texto(f)).not.toContain('crudo del backend')
    f.componentInstance['b'].reintentar()
    f.detectChanges()
    await f.whenStable()
    await responder(f, pedido(), OK)
    expect(f.componentInstance['b'].estado()).toBe('datos')
  })
})
