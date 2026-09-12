import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { PantallaDeCompras } from './pantalla-de-compras'

/**
 * Accesibilidad de la pantalla de compras, en **las dos pestañas**: es la prueba que
 * defiende la decisión de montar una tabla por vez (ver el comentario de
 * `pantalla-de-compras.ts`). El `cdk-virtual-scroll-viewport` no mide ni renderiza filas
 * en jsdom (mismo criterio que `nucleo/tabla/…spec.ts` y la bandeja de B1): lo que se
 * verifica acá es la estructura —encabezados, roles de tabla, pestañas, paginación—, que
 * es donde viven las violaciones serias.
 */
describe('PantallaDeCompras · accesibilidad', () => {
  beforeEach(() => {
    if (!Element.prototype.scrollTo) Element.prototype.scrollTo = () => {}
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
  })

  async function montar() {
    TestBed.inject(Sesion).abrir('t', ['CONTABILIDAD_ERP_COMPRAS', 'CONTABILIDAD_ERP_PAGAR'], 'contabilidad')
    const fixture = TestBed.createComponent(PantallaDeCompras)
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .match(() => true)
      .forEach((p) => p.flush([]))
    await fixture.whenStable()
    return fixture
  }

  it('sin violaciones serias en la pestaña de órdenes, con el aviso de segregación a la vista', async () => {
    const fixture = await montar()
    expect(fixture.nativeElement.textContent).toContain('Quien aprobó la factura no puede autorizar su pago')
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })

  it('sin violaciones serias en la pestaña de facturas de proveedor', async () => {
    const fixture = await montar()
    fixture.componentInstance['pestana'].set('facturas')
    fixture.detectChanges()
    TestBed.inject(HttpTestingController)
      .match(() => true)
      .forEach((p) => p.flush([]))
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
