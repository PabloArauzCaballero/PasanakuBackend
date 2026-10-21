import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, Component, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { Router, provideRouter, withComponentInputBinding } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { OBLIGACION, resumen } from './catalogo-sintetico'
import { PantallaDeFichaDeGobierno } from './pantalla-de-ficha-de-gobierno'

const BASE = 'http://gw/api/v1/indicadores/catalogo-de-datos'
const FICHA = `${BASE}/tablas/aportes/obligacion_aporte`

/** La ficha a la que se vuelve tras guardar, sin pedir nada. */
@Component({ template: '' })
class Vacia {}
const PUT = 'http://gw/api/v1/indicadores/catalogo-de-datos/aportes/obligacion_aporte/gobierno'

/** CU-130 · «Configurar tabla»: confirma antes de guardar, guarda con la versión leída y conserva el borrador ante un rechazo. */
describe('PantallaDeFichaDeGobierno', () => {
  let http: HttpTestingController
  let destinos: unknown[]

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'tablero/catalogo-de-datos/:esquema/:tabla/ficha', component: PantallaDeFichaDeGobierno }, { path: '**', component: Vacia }], withComponentInputBinding()),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
    TestBed.inject(Sesion).abrir('token', ['CATALOGO_DATOS_EDITAR'], 'ADMIN_PLATAFORMA')
    destinos = []
    const router = TestBed.inject(Router)
    const original = router.navigate.bind(router)
    router.navigate = ((comandos: unknown[], extras?: unknown) => {
      destinos.push(comandos)
      return original(comandos as string[], extras as never)
    }) as Router['navigate']
  })

  async function abrir() {
    const arnes = await RouterTestingHarness.create('/tablero/catalogo-de-datos/aportes/obligacion_aporte/ficha')
    TestBed.inject(ApplicationRef).tick()
    http.expectOne(FICHA).flush(OBLIGACION)
    http.expectOne(`${BASE}/resumen`).flush(resumen())
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    const el = arnes.routeNativeElement as HTMLElement
    return { arnes, el, boton: (nombre: string) => Array.from(el.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes(nombre)) as HTMLButtonElement }
  }

  function elegir(el: HTMLElement, etiqueta: string, valor: string): void {
    const label = Array.from(el.querySelectorAll('label')).find((l) => l.textContent?.trim() === etiqueta)!
    const select = el.querySelector<HTMLSelectElement>(`#${label.htmlFor}`)!
    select.value = valor
    select.dispatchEvent(new Event('change'))
  }

  it('correcto: sin cambios no se puede guardar; con un cambio pide confirmación diciendo cuántos campos, y guarda con la versión leída', async () => {
    const { arnes, el, boton } = await abrir()
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Configurar tabla')
    expect(boton('Guardar ficha').disabled).toBe(true)
    elegir(el, 'Estado', 'CANDIDATA_A_RETIRO')
    arnes.fixture.detectChanges()
    expect(boton('Guardar ficha').disabled).toBe(false)
    boton('Guardar ficha').click()
    arnes.fixture.detectChanges()
    const dialogo = el.querySelector('ap-dialogo-de-confirmacion dialog') as HTMLDialogElement
    expect(dialogo.open).toBe(true)
    expect(dialogo.textContent).toContain('aportes.obligacion_aporte en el catálogo de datos (1 campo)')
    ;(Array.from(dialogo.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes('Guardar ficha')) as HTMLButtonElement).click()
    const pedido = http.expectOne({ method: 'PUT', url: PUT })
    expect(pedido.request.body).toEqual({ estado: 'CANDIDATA_A_RETIRO', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 0 })
    pedido.flush({ estado: 'CANDIDATA_A_RETIRO', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 1 })
    await new Promise((r) => setTimeout(r))
    // Tras guardar se releen las cifras del catálogo, que cambiaron con la ficha (la ficha la relee la pantalla a la que se vuelve).
    http.match(FICHA).forEach((r) => r.flush(OBLIGACION))
    http.expectOne(`${BASE}/resumen`).flush(resumen())
    await arnes.fixture.whenStable()
    expect(destinos.at(-1)).toEqual(['..'])
  })

  it('inválido: AP-CU130-01 cierra la confirmación, explica que otra persona guardó antes y conserva el borrador', async () => {
    const { arnes, el, boton } = await abrir()
    elegir(el, 'Estado', 'RETIRADA')
    arnes.fixture.detectChanges()
    boton('Guardar ficha').click()
    arnes.fixture.detectChanges()
    const dialogo = el.querySelector('ap-dialogo-de-confirmacion dialog') as HTMLDialogElement
    ;(Array.from(dialogo.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes('Guardar ficha')) as HTMLButtonElement).click()
    http.expectOne({ method: 'PUT', url: PUT }).flush({ codigo: 'AP-CU130-01', mensaje: 'x' }, { status: 422, statusText: 'Unprocessable Entity' })
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    expect(dialogo.open).toBe(false)
    expect(el.textContent).toContain('Otra persona guardó esta ficha antes que vos')
    const label = Array.from(el.querySelectorAll('label')).find((l) => l.textContent?.trim() === 'Estado')!
    expect(el.querySelector<HTMLSelectElement>(`#${label.htmlFor}`)!.value).toBe('RETIRADA')
  })

  it('límite: la clasificación se muestra derivada de las columnas, sin controles para editarla', async () => {
    const { el } = await abrir()
    expect(el.textContent).toContain('Datos financieros: Sí')
    expect(el.textContent).toContain('Datos personales: No')
    expect(el.querySelectorAll('ap-interruptor')).toHaveLength(1)
  })
})
