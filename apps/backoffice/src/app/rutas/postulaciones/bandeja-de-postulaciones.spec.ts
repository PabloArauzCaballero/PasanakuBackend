import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting, type TestRequest } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { erroresInterceptor } from '../../nucleo/errores.interceptor'
import { GATEWAY } from '../../nucleo/gateway'
import { BandejaDePostulaciones } from './bandeja-de-postulaciones'
import type { PaginaDeBandeja } from './dominio/cu90-postulaciones'

/** **Prueba de UI contra HTTP simulado**: la bandeja y su cliente, NO la integración con `organizador`. Datos sintéticos. */
const GW = 'http://gw/api/v1'
const URL = `${GW}/organizadores/postulaciones/bandeja`
const EJEMPLO = ejemploDe('organizador', 'listarBandejaDeHabilitaciones', 'ok').cuerpo as PaginaDeBandeja
const fila = EJEMPLO.items[0]!
const pagina = (items: PaginaDeBandeja['items'], proximo?: { fecha: string; id: string }): PaginaDeBandeja => ({
  items,
  proximoFecha: proximo?.fecha ?? null,
  proximoId: proximo?.id ?? null,
})

describe('BandejaDePostulaciones · [UI contra HTTP simulado] CU-90', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([{ path: 'postulaciones-organizador', component: BandejaDePostulaciones }]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: GW },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  const pedido = (): TestRequest => http.expectOne((r) => r.url === URL)
  async function responder(f: ComponentFixture<unknown>, cuerpo: PaginaDeBandeja) {
    pedido().flush(cuerpo)
    await f.whenStable()
    f.detectChanges()
  }
  const texto = (f: ComponentFixture<unknown>) => (f.nativeElement as HTMLElement).textContent ?? ''
  function montar() {
    const f = TestBed.createComponent(BandejaDePostulaciones)
    f.detectChanges()
    return f
  }

  it('con datos: lista las postulaciones con su estado legible y marca las que esperan una decisión', async () => {
    const f = montar()
    const r = pedido()
    expect(r.request.params.get('limite')).toBe('50')
    expect(r.request.params.has('estados')).toBe(false)
    r.flush(pagina([{ ...fila, estado: 'PENDIENTE' as never, acciones: ['RESOLVER' as never] }]))
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).toContain('Pendiente')
    expect(texto(f)).toContain('Espera tu decisión')
    expect(texto(f)).not.toContain('PENDIENTE')
  })

  it('vacío: dice que no hay postulaciones y qué hace que aparezcan', async () => {
    const f = montar()
    await responder(f, pagina([]))
    expect(texto(f)).toContain('No hay postulaciones con este estado')
  })

  it('filtro: el estado de la URL viaja como `estados`; uno desconocido no se manda', async () => {
    await TestBed.inject(Router).navigateByUrl('/postulaciones-organizador?estado=EN_REVISION')
    const f = montar()
    expect(pedido().request.params.get('estados')).toBe('EN_REVISION')
    f.destroy()
    await TestBed.inject(Router).navigateByUrl('/postulaciones-organizador?estado=INVENTADO')
    montar()
    expect(pedido().request.params.has('estados')).toBe(false)
  })

  it('cursor: «Siguientes» pide la página con el cursor del servidor, y sin cursor no se puede avanzar', async () => {
    const f = montar()
    await responder(f, pagina([fila], { fecha: '2026-10-28T12:00:00Z', id: fila.solicitudId }))
    const c = f.componentInstance as unknown as { avanzar(): void; siguiente(): unknown }
    c.avanzar()
    f.detectChanges()
    TestBed.tick()
    const r = pedido()
    expect(r.request.params.get('despuesDeFecha')).toBe('2026-10-28T12:00:00Z')
    expect(r.request.params.get('despuesDeId')).toBe(fila.solicitudId)
    r.flush(pagina([fila]))
    await f.whenStable()
    f.detectChanges()
    expect(c.siguiente()).toBeNull()
  })

  it('error: mensaje accionable en español, sin el texto crudo del backend', async () => {
    const f = montar()
    pedido().flush({ codigo: 'AP-SEG-01', mensaje: 'crudo del backend', trazaId: 't' }, { status: 403, statusText: 'x' })
    await f.whenStable()
    f.detectChanges()
    expect(texto(f)).not.toContain('crudo del backend')
    expect(texto(f)).toContain('Volver a intentar')
  })
})
