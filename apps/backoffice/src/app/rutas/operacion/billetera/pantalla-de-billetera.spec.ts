import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, Router } from '@angular/router'
import { ejemploDe } from '@aportaya/simulado'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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
        provideRouter([]),
        provideHttpClient(withInterceptors([trazaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })

  it('cargando: lo dice mientras espera, sin pantalla en blanco', async () => {
    const fixture = await montar()
    const estado = fixture.nativeElement.querySelector('ap-estado-de-pantalla [role="status"]') as HTMLElement
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
    const boton = fixture.nativeElement.querySelector('ap-estado-de-pantalla button') as HTMLButtonElement
    expect(boton.textContent?.trim()).toBe('Volver a intentar')
    expect(fixture.nativeElement.textContent).toContain('Código de seguimiento: traza-401')
    expect(fixture.nativeElement.textContent).not.toContain('tecnico')
  })

  it('sin red: no se queda cargando para siempre', async () => {
    const fixture = await montar()
    http.expectOne(URL).error(new ProgressEvent('error'), { status: 0 })
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('No hay conexión')
    expect(fixture.nativeElement.querySelector('ap-estado-de-pantalla button')?.textContent?.trim()).toBe('Volver a intentar')
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
    ;(fixture.nativeElement.querySelector('ap-estado-de-pantalla button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await Promise.resolve()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    await fixture.whenStable()
    expect(fixture.nativeElement.querySelector('ap-monto')).not.toBeNull()
  })
})

describe('PantallaDeBilletera · actualizar y consultar otra cuenta (2026-10-26)', () => {
  let http: HttpTestingController
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })
  const botonDe = (f: { nativeElement: HTMLElement }, nombre: string) => [...f.nativeElement.querySelectorAll('button')].find((b) => b.textContent?.trim() === nombre)!

  it('aceptado: «Actualizar saldo» vuelve a pedir el saldo al servidor', async () => {
    const f = await montar()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    await f.whenStable()
    f.detectChanges()
    botonDe(f, 'Actualizar saldo').click()
    f.detectChanges()
    await Promise.resolve()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
  })

  it('aceptado: «Consultar saldo» con un identificador válido navega a la billetera de esa cuenta (la ruta vuelve a decidir el permiso)', async () => {
    const f = await montar()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    const otra = '22222222-2222-4222-8222-222222222222'
    f.componentInstance['otra'].set(` ${otra.toUpperCase()}`)
    f.detectChanges()
    botonDe(f, 'Consultar saldo').click()
    expect(navegar).toHaveBeenCalledWith(['/operacion/billetera', otra])
  })

  it('error: un identificador mal formado se marca en el campo, conserva lo escrito y no navega ni pide', async () => {
    const f = await montar()
    http.expectOne(URL).flush(ejemploDe('nucleo-financiero', 'consultarSaldo', 'ok').cuerpo as object)
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true)
    f.componentInstance['otra'].set('cuenta-1')
    botonDe(f, 'Consultar saldo').click()
    f.detectChanges()
    expect(f.nativeElement.textContent).toContain('Ese identificador no tiene el formato de una cuenta de billetera.')
    expect(f.componentInstance['otra']()).toBe('cuenta-1')
    expect(navegar).not.toHaveBeenCalled()
    http.expectNone(() => true)
  })
})
