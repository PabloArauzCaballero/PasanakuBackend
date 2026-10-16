import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { afterEach, describe, expect, it } from 'vitest'
import { GATEWAY } from '../../nucleo/gateway'
import { Sesion } from '../../nucleo/sesion'
import { Tablero } from './tablero'

const GW = 'http://gw/api/v1'

function montar(permisos: string[]) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: GATEWAY, useValue: GW }],
  })
  TestBed.inject(Sesion).abrir('t', permisos, 'BACKOFFICE', 'op-1')
  const fixture = TestBed.createComponent(Tablero)
  fixture.detectChanges()
  return { fixture, http: TestBed.inject(HttpTestingController), raiz: fixture.nativeElement as HTMLElement }
}

/** Deja correr las promesas de las lecturas (`firstValueFrom` → `then`) antes de pintar. */
async function estabilizar(fixture: { whenStable: () => Promise<unknown>; detectChanges: () => void }) {
  await new Promise<void>((resolver) => queueMicrotask(resolver))
  await Promise.resolve()
  await fixture.whenStable()
  fixture.detectChanges()
}

const tarjeta = (raiz: HTMLElement, id: string) => raiz.querySelector<HTMLElement>(`[data-bandeja="${id}"]`)
/** El texto de la tarjeta sin la hora de consulta: lo que no puede llevar ningún número. */
const sinHora = (el: HTMLElement) => Array.from(el.children).filter((c) => !c.classList.contains('hora')).map((c) => c.textContent).join(' ')

describe('Tablero · «Requiere tu acción» desde las bandejas reales', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify())

  it('correcto: solo las bandejas que el permiso habilita; «hay pendientes» / «al día» con hora, sin número', async () => {
    const { fixture, http, raiz } = montar(['RETIRO_APROBAR', 'CUMPLIMIENTO_ALERTAS'])
    expect(tarjeta(raiz, 'retiros')?.querySelector('[role="status"]')).toBeTruthy()
    http.expectOne(`${GW}/billetera/retiros?limite=1`).flush({ items: [{ ordenRetiroId: 'o-1' }], proximoFecha: 'f', proximoId: 'i' })
    http.expectOne(`${GW}/cumplimiento/alertas?limite=1`).flush({ items: [], proximoFecha: null, proximoId: null })
    await estabilizar(fixture)

    expect(raiz.querySelectorAll('[data-bandeja]')).toHaveLength(2)
    expect(tarjeta(raiz, 'reclamos')).toBeNull()
    const retiros = tarjeta(raiz, 'retiros')!
    expect(retiros.textContent).toContain('Hay pendientes')
    expect(retiros.querySelector('time[datetime]')).toBeTruthy()
    expect(retiros.querySelector('a')?.getAttribute('href')).toBe('/operacion/retiros')
    expect(sinHora(retiros)).not.toMatch(/\d/)
    expect(tarjeta(raiz, 'alertas')!.textContent).toContain('Al día')
  })

  it('límite: sin ningún permiso de bandeja no se pide nada y el vacío orienta', async () => {
    const { fixture, raiz } = montar(['CONTABILIDAD_ERP_CERRAR'])
    await estabilizar(fixture)
    expect(raiz.querySelectorAll('[data-bandeja]')).toHaveLength(0)
    expect(raiz.textContent).toContain('Tu rol no atiende ninguna bandeja con pendientes.')
  })

  it('límite: desembolsos se abre con AUDITORIA_LEER aunque no tenga ENTREGA_EJECUTAR', async () => {
    const { fixture, http, raiz } = montar(['AUDITORIA_LEER'])
    http.expectOne(`${GW}/desembolsos?limite=1`).flush({ items: [] })
    await estabilizar(fixture)
    expect(tarjeta(raiz, 'desembolsos')!.textContent).toContain('Al día')
  })

  it('inválido: un 403 deja la tarjeta en error accionable y no afecta a las demás', async () => {
    const { fixture, http, raiz } = montar(['RECLAMO_VER', 'CUMPLIMIENTO_CASOS'])
    http.expectOne(`${GW}/reclamos?limite=1`).flush({ codigo: 'AP-SEG-01', trazaId: 'tr-9' }, { status: 403, statusText: 'Forbidden' })
    http.expectOne(`${GW}/cumplimiento/casos?limite=1`).flush({ items: [{ casoId: 'c' }] })
    await estabilizar(fixture)
    const reclamos = tarjeta(raiz, 'reclamos')!
    expect(reclamos.querySelector('[role="alert"]')?.textContent).toContain('No tenés acceso a esto.')
    expect(reclamos.textContent).toContain('tr-9')
    expect(reclamos.textContent).not.toContain('Al día')
    expect(reclamos.textContent).not.toContain('Hay pendientes')
    expect(tarjeta(raiz, 'casos')!.textContent).toContain('Hay pendientes')
  })

  it('inválido: un 500 se dice como error, sin cifra ni mensaje crudo', async () => {
    const { fixture, http, raiz } = montar(['CUMPLIMIENTO_CASOS'])
    http.expectOne(`${GW}/cumplimiento/casos?limite=1`).flush({ mensaje: 'NullPointerException' }, { status: 500, statusText: 'Error' })
    await estabilizar(fixture)
    const casos = tarjeta(raiz, 'casos')!
    expect(casos.querySelector('[role="alert"]')).toBeTruthy()
    expect(casos.textContent).not.toContain('NullPointerException')
    expect(sinHora(casos)).not.toMatch(/\d/)
  })

  it('«Actualizar» vuelve a consultar cada bandeja visible', async () => {
    const { fixture, http, raiz } = montar(['RETIRO_APROBAR'])
    http.expectOne(`${GW}/billetera/retiros?limite=1`).flush({ items: [] })
    await estabilizar(fixture)
    raiz.querySelector<HTMLButtonElement>('ap-boton button')!.click()
    fixture.detectChanges()
    http.expectOne(`${GW}/billetera/retiros?limite=1`).flush({ items: [{ ordenRetiroId: 'o' }] })
    await estabilizar(fixture)
    expect(tarjeta(raiz, 'retiros')!.textContent).toContain('Hay pendientes')
  })
})

describe('Tablero · monitoreo y accesos', () => {
  it('los indicadores sin proyección se declaran «no disponible», con motivo y sin cifra', () => {
    const { raiz } = montar([])
    const indicadores = raiz.querySelectorAll<HTMLElement>('[data-indicador]')
    expect(indicadores).toHaveLength(2)
    for (const i of Array.from(indicadores)) {
      expect(i.textContent).toContain('Indicador no disponible')
      expect(i.textContent).not.toMatch(/\d/)
    }
  })

  it('los accesos siguen filtrados por permiso y con el ancla de los tutoriales', () => {
    const { http, raiz } = montar(['RETIRO_APROBAR'])
    http.expectOne(`${GW}/billetera/retiros?limite=1`).flush({ items: [] })
    const accesos = raiz.querySelector('[data-tutorial-id="tablero-accesos"]')!
    expect(accesos.querySelector('[data-tutorial-id="tablero-operacion/retiros"]')).toBeTruthy()
    expect(accesos.querySelector('[data-tutorial-id="tablero-contabilidad"]')).toBeNull()
    expect(accesos.querySelector('[data-tutorial-id="tablero-configuracion"]')).toBeNull()
  })
})
