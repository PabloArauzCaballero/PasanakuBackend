import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { EstadoDeRevision, EstadoDeTabla } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { OBLIGACION } from './catalogo-sintetico'
import { SeccionDeGobierno } from './seccion-de-gobierno'

const PUT = 'http://gw/api/v1/indicadores/catalogo-de-datos/aportes/obligacion_aporte/gobierno'

/** CU-130 · la ficha de gobierno: guarda con la versión leída, conserva el borrador ante un rechazo y se lee sin permiso. */
describe('SeccionDeGobierno', () => {
  let http: HttpTestingController
  let sesion: Sesion

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
    sesion = TestBed.inject(Sesion)
  })

  async function montar(permisos: string[], version = 0) {
    sesion.abrir('token', permisos, 'ADMIN_PLATAFORMA')
    const fixture = TestBed.createComponent(SeccionDeGobierno)
    const tabla = { ...OBLIGACION, gobierno: { estado: EstadoDeTabla.Activa, estadoRevision: EstadoDeRevision.Detectada, criticaParaAuditoria: false, version } }
    fixture.componentRef.setInput('tabla', tabla)
    fixture.detectChanges()
    await fixture.whenStable()
    return { fixture, el: fixture.nativeElement as HTMLElement }
  }

  function elegir(el: HTMLElement, etiqueta: string, valor: string): void {
    const label = Array.from(el.querySelectorAll('label')).find((l) => l.textContent?.trim() === etiqueta)!
    const select = el.querySelector<HTMLSelectElement>(`#${label.htmlFor}`)!
    select.value = valor
    select.dispatchEvent(new Event('change'))
  }

  const enviar = (el: HTMLElement) => el.querySelector('form')!.dispatchEvent(new Event('submit'))

  it('correcto: guarda el estado elegido con la versión leída y avisa que quedó en la bitácora', async () => {
    const { fixture, el } = await montar(['CATALOGO_DATOS_EDITAR'], 2)
    elegir(el, 'Estado', 'CANDIDATA_A_RETIRO')
    enviar(el)
    const pedido = http.expectOne({ method: 'PUT', url: PUT })
    expect(pedido.request.body).toEqual({ estado: 'CANDIDATA_A_RETIRO', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 2 })
    pedido.flush({ estado: 'CANDIDATA_A_RETIRO', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 3 })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(el.textContent).toContain('Ficha guardada')
  })

  it('límite: mientras guarda, el botón queda bloqueado y un segundo envío no sale (sin doble envío)', async () => {
    const { fixture, el } = await montar(['CATALOGO_DATOS_EDITAR'])
    enviar(el)
    fixture.detectChanges()
    enviar(el)
    const pedidos = http.match({ method: 'PUT', url: PUT })
    expect(pedidos).toHaveLength(1)
    expect(el.querySelector('ap-boton button')?.hasAttribute('disabled')).toBe(true)
    pedidos[0]!.flush({ estado: 'ACTIVA', estadoRevision: 'DETECTADA', criticaParaAuditoria: false, version: 1 })
  })

  it('inválido: AP-CU130-01 explica que otra persona guardó antes y el borrador se conserva', async () => {
    const { fixture, el } = await montar(['CATALOGO_DATOS_EDITAR'])
    elegir(el, 'Estado', 'RETIRADA')
    enviar(el)
    http.expectOne({ method: 'PUT', url: PUT }).flush({ codigo: 'AP-CU130-01', mensaje: 'x' }, { status: 422, statusText: 'Unprocessable Entity' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(el.textContent).toContain('Otra persona guardó esta ficha antes que vos')
    const label = Array.from(el.querySelectorAll('label')).find((l) => l.textContent?.trim() === 'Estado')!
    expect(el.querySelector<HTMLSelectElement>(`#${label.htmlFor}`)!.value).toBe('RETIRADA')
  })

  it('sin permiso de edición: muestra la ficha en lectura, sin formulario', async () => {
    const { el } = await montar(['TABLERO_SISTEMAS_VER'])
    expect(el.querySelector('form')).toBeNull()
    expect(el.textContent).toContain('Activa')
    expect(el.textContent).toContain('hace falta el permiso de edición')
    http.expectNone({ method: 'PUT' })
  })
})
