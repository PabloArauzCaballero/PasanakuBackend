import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { PantallaDeBilletera } from './pantalla-de-billetera'

/**
 * H12.S1.M2 · separar saldos y posiciones. El contrato publicado de `nucleo-financiero` trae `disponible` y
 * `retenido`; `titularId` y `comprometidoEnPozos` son el pedido del carril D, SIN productor todavía. Por eso
 * estos casos usan un DOBLE declarado del contrato pedido (tres niveles: correcto, límite, inválido), y el único
 * caso con el cuerpo real del ejemplo del contrato es el de «sin separar». Nada de esto prueba el núcleo real.
 */
const CUENTA = '11111111-1111-4111-8111-111111111111'
const URL = `http://gw/api/v1/billetera/${CUENTA}/saldo`
const TITULAR = '22222222-2222-4222-8222-222222222222'
const bs = (monto: string, moneda = 'BOB') => ({ monto, moneda })
const doble = (disponible: string, retenido: string, comprometido?: string) => ({
  cuentaId: CUENTA,
  titularId: TITULAR,
  disponible: bs(disponible),
  retenido: bs(retenido),
  ...(comprometido ? { comprometidoEnPozos: bs(comprometido) } : {}),
  alCorteDe: '2026-09-09T14:30:00Z',
})

async function mostrar(cuerpo: object) {
  const fixture = TestBed.createComponent(PantallaDeBilletera)
  fixture.componentRef.setInput('cuentaId', CUENTA)
  fixture.detectChanges()
  TestBed.inject(HttpTestingController).expectOne(URL).flush(cuerpo)
  await fixture.whenStable()
  const el = fixture.nativeElement as HTMLElement
  const filas = [...el.querySelectorAll('.fila')].map((f) => f.textContent!.replace(/\s+/g, ' ').trim())
  return { el, filas }
}

describe('PantallaDeBilletera · posiciones separadas (contrato pedido, con doble)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
  })

  it('correcto: lo gastable, lo comprometido en pozos y lo retenido van separados y suman el total', async () => {
    const { filas, el } = await mostrar(doble('1240.00', '150.00', '400.00'))
    expect(filas[0]).toMatch(/Gastable por el titular ahora.*Bs 840,00.*Gastable/)
    expect(filas[1]).toMatch(/Comprometido en pozos de grupos.*Bs 400,00.*No gastable/)
    expect(filas[2]).toMatch(/Retenido por una operación en curso.*Bs 150,00.*No gastable/)
    expect(filas[3]).toMatch(/Total de la cuenta en el libro.*Bs 1\.390,00/)
    expect(el.textContent).toContain('lo invertido no forman parte de lo gastable')
    expect(el.textContent).toContain('Al corte de')
  })

  it('límite: todo el disponible comprometido deja lo gastable en cero (no negativo) y no avisa de nada raro', async () => {
    const { filas, el } = await mostrar(doble('400.00', '0.00', '400.00'))
    expect(filas[0]).toContain('Bs 0,00')
    expect(filas[1]).toContain('Bs 400,00')
    expect(el.querySelector('ap-alerta')).toBeNull()
  })

  it('límite: comprometido por encima del disponible se acota, lo gastable es cero y se avisa para conciliar', async () => {
    const { filas, el } = await mostrar(doble('100.00', '0.00', '250.00'))
    expect(filas[0]).toContain('Bs 0,00')
    expect(filas[1]).toContain('Bs 100,00')
    expect(el.querySelector('ap-alerta')?.textContent).toContain('revisar la conciliación')
  })

  it('el ejemplo real del contrato (sin comprometidoEnPozos) NO afirma ninguna cifra gastable', async () => {
    const { el, filas } = await mostrar(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    expect(el.textContent).toContain('Disponible sin separar')
    expect(el.textContent).not.toContain('Gastable por el titular ahora')
    expect(filas[0]).toContain('Bs 1.240,00')
    expect(filas[0]).toContain('Sin separar')
    expect(filas.at(-1)).toContain('Bs 1.390,00')
  })

  it('inválido: monedas distintas, importe negativo o fuera de contrato → no se muestra ninguna cifra', async () => {
    for (const cuerpo of [
      { ...doble('10.00', '1.00', '1.00'), retenido: bs('1.00', 'USD') },
      doble('-5.00', '0.00', '0.00'),
      doble('12.5', '0.00', '0.00'),
    ]) {
      TestBed.resetTestingModule()
      TestBed.configureTestingModule({
        providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
      })
      const { el } = await mostrar(cuerpo)
      expect(el.querySelector('ap-alerta')?.textContent).toContain('No mostramos este saldo')
      expect(el.querySelector('ap-posiciones-de-saldo')).toBeNull()
      expect(el.querySelector('ap-monto')).toBeNull()
    }
  })

  it('cuenta en cero con todo informado: sigue siendo el estado vacío, no una falla', async () => {
    const { el } = await mostrar(doble('0.00', '0.00', '0.00'))
    expect(el.textContent).toContain('La cuenta está en cero')
  })
})
