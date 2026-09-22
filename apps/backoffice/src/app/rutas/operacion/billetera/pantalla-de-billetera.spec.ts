import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { PantallaDeBilletera } from './pantalla-de-billetera'

/**
 * Los cuatro estados de la pantalla real de F0, contra los ejemplos del contrato —los
 * mismos JSON que Prism sirve y que Flutter prueba—. En un backoffice el estado vacío
 * importa el doble: «la cuenta está en cero» y «la consulta falló» llevan a acciones
 * distintas.
 */
const CUENTA = '11111111-1111-4111-8111-111111111111'
const URL = `http://gw/api/v1/billetera/${CUENTA}/saldo`

async function montar() {
  const fixture = TestBed.createComponent(PantallaDeBilletera)
  fixture.componentRef.setInput('cuentaId', CUENTA)
  // whenStable() esperaría la petición pendiente: se fuerza el primer render y se responde.
  fixture.detectChanges()
  return fixture
}

describe('PantallaDeBilletera · los cuatro estados', () => {
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
  })

  it('cargando: lo dice mientras espera, sin pantalla en blanco', async () => {
    const fixture = await montar()
    const estado = fixture.nativeElement.querySelector('[role="status"]') as HTMLElement
    expect(estado?.getAttribute('aria-label')).toBe('Cargando el saldo de la cuenta')
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
  })

  it('éxito: muestra el saldo que respondió el contrato, formateado por Monto y sin recalcular', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    await fixture.whenStable()
    const disponible = fixture.nativeElement.querySelector('ap-monto[aria-label^="Saldo disponible: "]') as HTMLElement
    expect(disponible.getAttribute('aria-label')).toMatch(/^Saldo disponible: (Bs|USD) -?[\d.]+,\d{2}$/)
    expect(disponible.textContent).toBe('Bs 1.240,00')
  })

  it('vacío: una cuenta en cero lo dice, y aclara que no es una falla de consulta', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'vacio').cuerpo as object)
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('La cuenta está en cero')
    expect(fixture.nativeElement.textContent).toContain('No es un error de consulta')
  })

  it('error: mensaje humano, reintento a la vista y traza para soporte; el texto del backend no se muestra', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush({ codigo: 'AP-CU04-01', mensaje: 'tecnico', trazaId: 'traza-401' }, { status: 401, statusText: 'Unauthorized' })
    await fixture.whenStable()
    const boton = fixture.nativeElement.querySelector('button') as HTMLButtonElement
    expect(boton.textContent?.trim()).toBe('Volver a intentar')
    expect(fixture.nativeElement.textContent).toContain('Código de seguimiento: traza-401')
    expect(fixture.nativeElement.textContent).not.toContain('tecnico')
  })

  it('sin red: no se queda cargando para siempre', async () => {
    const fixture = await montar()
    http.expectOne(URL).error(new ProgressEvent('error'), { status: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('No hay conexión')
    expect(fixture.nativeElement.querySelector('button')?.textContent?.trim()).toBe('Volver a intentar')
  })

  it('un 403 no dice más que «no tenés acceso»: detallarlo confirma que el recurso existe', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush({ codigo: 'AP-SEG-03', mensaje: 'detalle', trazaId: 't' }, { status: 403, statusText: 'Forbidden' })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('No tenés acceso a esto.')
    expect(fixture.nativeElement.textContent).not.toContain('detalle')
  })

  it('reintentar vuelve a pedir el saldo al servidor: nunca se ajusta en memoria', async () => {
    const fixture = await montar()
    http.expectOne(URL).flush({ codigo: 'AP-GW-503', mensaje: '', trazaId: 't' }, { status: 503, statusText: 'Service Unavailable' })
    await fixture.whenStable()
    ;(fixture.nativeElement.querySelector('button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await Promise.resolve()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('ap-monto')).not.toBeNull()
  })
})
