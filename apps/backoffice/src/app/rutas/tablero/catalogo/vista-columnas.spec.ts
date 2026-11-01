import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { beforeEach, describe, expect, it } from 'vitest'
import { DecisionDeRevision, type TablaDelCatalogo } from 'clientes/angular/auditoria'
import { GATEWAY } from '../../../nucleo/gateway'
import { Sesion } from '../../../nucleo/sesion'
import { OBLIGACION } from './catalogo-sintetico'
import { VistaColumnas } from './vista-columnas'

const PUT = 'http://gw/api/v1/indicadores/catalogo-de-datos/aportes/obligacion_aporte/columnas/monto_esperado/revision'

/** CU-131 · la pestaña Columnas: cifras, filtros, revisión en palabras y el cajón que guarda con la versión leída. */
describe('VistaColumnas', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }],
    })
    http = TestBed.inject(HttpTestingController)
  })

  async function montar(permisos: string[], tabla: TablaDelCatalogo = OBLIGACION) {
    TestBed.inject(Sesion).abrir('token', permisos, 'ADMIN_PLATAFORMA')
    const fixture = TestBed.createComponent(VistaColumnas)
    fixture.componentRef.setInput('tabla', tabla)
    let cambios = 0
    fixture.componentInstance.cambio.subscribe(() => cambios++)
    fixture.detectChanges()
    await fixture.whenStable()
    return { fixture, el: fixture.nativeElement as HTMLElement, cambios: () => cambios }
  }

  /** El texto de una celda sin la etiqueta que la tabla repite para el celular. */
  function celdas(el: HTMLElement, clave: string): string[] {
    return Array.from(el.querySelectorAll('tbody tr')).map((tr) => {
      const td = tr.querySelectorAll('td')[clave === 'revision' ? 5 : 0]
      // La celda del nombre lleva también la acción «Revisar»: el nombre es el `code`.
      if (clave === 'nombre') return td?.querySelector('code')?.textContent ?? ''
      return Array.from(td?.childNodes ?? [])
        .filter((n) => n.nodeType === Node.TEXT_NODE || (n instanceof HTMLElement && !n.classList.contains('etiqueta-movil')))
        .map((n) => n.textContent)
        .join('')
        .replace(/\s+/g, ' ')
        .trim()
    })
  }

  it('correcto: cifras de la tabla, la revisión «Sin revisar» en palabras y «Revisar» con permiso', async () => {
    const { el } = await montar(['CATALOGO_DATOS_EDITAR'])
    const cifras = Array.from(el.querySelectorAll('ap-tarjeta-kpi .valor')).map((v) => v.textContent?.trim())
    expect(cifras).toEqual(['3', '0', '1', '2', '0'])
    expect(celdas(el, 'revision')).toEqual(['Sin revisar', 'Sin revisar', 'Sin revisar'])
    expect(el.querySelector('tbody ap-boton')?.textContent?.trim()).toBe('Revisar')
  })

  it('correcto: aprobar desde el cajón envía la versión leída, cierra y avisa que cambió', async () => {
    const { fixture, el, cambios } = await montar(['CATALOGO_DATOS_EDITAR'])
    const botones = Array.from(el.querySelectorAll('tbody ap-boton button')) as HTMLButtonElement[]
    botones[1]!.click()
    fixture.detectChanges()
    await fixture.whenStable()
    const cajon = el.querySelector('ap-cajon-de-revision dialog') as HTMLDialogElement
    expect(cajon.open).toBe(true)
    expect(cajon.querySelector('h2')?.textContent).toBe('Revisar columna monto_esperado')
    const decision = cajon.querySelector('select') as HTMLSelectElement
    decision.value = 'APROBADA'
    decision.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    ;(Array.from(cajon.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes('Guardar revisión')) as HTMLButtonElement).click()
    const pedido = http.expectOne({ method: 'PUT', url: PUT })
    expect(pedido.request.body).toEqual({ decision: 'APROBADA', version: 0 })
    pedido.flush({ decision: 'APROBADA', version: 1 })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(cajon.open).toBe(false)
    expect(cambios()).toBe(1)
  })

  it('inválido: rechazar sin motivo no envía nada y marca el campo; AP-CU131-03 se explica y conserva lo escrito', async () => {
    const { fixture, el } = await montar(['CATALOGO_DATOS_EDITAR'])
    ;(el.querySelector('tbody ap-boton button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await fixture.whenStable()
    const cajon = el.querySelector('ap-cajon-de-revision dialog') as HTMLDialogElement
    const decision = cajon.querySelector('select') as HTMLSelectElement
    decision.value = 'RECHAZADA'
    decision.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    const guardar = Array.from(cajon.querySelectorAll('ap-boton button')).find((b) => b.textContent?.includes('Guardar revisión')) as HTMLButtonElement
    guardar.click()
    fixture.detectChanges()
    http.expectNone({ method: 'PUT' })
    expect(cajon.querySelector('[role="alert"]')?.textContent).toContain('Para rechazar hay que decir por qué.')
    const motivo = cajon.querySelector('textarea') as HTMLTextAreaElement
    motivo.value = 'No es un identificador'
    motivo.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    guardar.click()
    http.expectOne({ method: 'PUT' }).flush({ codigo: 'AP-CU131-03', mensaje: 'x' }, { status: 422, statusText: 'Unprocessable Entity' })
    await fixture.whenStable()
    fixture.detectChanges()
    expect(cajon.open).toBe(true)
    expect(cajon.textContent).toContain('Otra persona revisó esta columna antes')
    expect(motivo.value).toBe('No es un identificador')
  })

  it('límite: filtrar por revisión «Aprobada» deja solo las aprobadas; sin permiso la acción es «Ver revisión» y el cajón es de lectura', async () => {
    const tabla: TablaDelCatalogo = { ...OBLIGACION, columnas: OBLIGACION.columnas.map((c, i) => (i === 0 ? { ...c, revision: { decision: DecisionDeRevision.Aprobada, version: 1 } } : c)) }
    const { fixture, el } = await montar(['TABLERO_SISTEMAS_VER'], tabla)
    const selects = el.querySelectorAll('ap-barra-de-filtros select')
    const revision = selects[1] as HTMLSelectElement
    revision.value = 'APROBADA'
    revision.dispatchEvent(new Event('change'))
    fixture.detectChanges()
    expect(celdas(el, 'nombre')).toEqual(['id'])
    expect(el.querySelector('tbody ap-boton')?.textContent?.trim()).toBe('Ver revisión')
    ;(el.querySelector('tbody ap-boton button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await fixture.whenStable()
    const cajon = el.querySelector('ap-cajon-de-revision dialog') as HTMLDialogElement
    expect(cajon.querySelector('form')).toBeNull()
    expect(cajon.textContent).toContain('hace falta el permiso de edición')
  })
})
