import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { ApplicationRef, provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { RouterTestingHarness } from '@angular/router/testing'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { erroresInterceptor } from '../../../nucleo/errores.interceptor'
import { GATEWAY } from '../../../nucleo/gateway'
import { idempotenciaInterceptor } from '../../../nucleo/idempotencia.interceptor'
import { trazaInterceptor } from '../../../nucleo/traza.interceptor'
import { FichaDeExpediente } from './ficha-de-expediente'

const URL = 'http://gw/api/v1/identidad/verificaciones'
const CINCO = ['ANVERSO', 'REVERSO', 'SELFIE', 'PERFIL_IZQUIERDO', 'PERFIL_DERECHO']

/** Datos sintéticos: ninguna persona real. */
const expediente = (cambios: Record<string, unknown> = {}): ExpedienteEnRevision =>
  ({
    verificacionId: 'v-7',
    usuarioId: 'u-7',
    nombreCompleto: 'Rosa Sintética Mamani',
    telefono: '+59170000007',
    correo: 'rosa@ejemplo.invalid',
    documento: 'CI CB',
    estado: 'EN_REVISION',
    iniciadaEn: '2026-10-20T14:00:00Z',
    fechaExpiracionDocumento: '2031-03-02',
    documentoVigente: true,
    fotos: CINCO,
    ...cambios,
  }) as ExpedienteEnRevision

describe('FichaDeExpediente', () => {
  let http: HttpTestingController

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptors([trazaInterceptor, idempotenciaInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'cumplimiento/verificaciones/:verificacionId', component: FichaDeExpediente }], withComponentInputBinding()),
        { provide: GATEWAY, useValue: 'http://gw/api/v1' },
      ],
    })
    http = TestBed.inject(HttpTestingController)
  })
  afterEach(() => http.verify())

  async function abrir(url: string, cola: ExpedienteEnRevision[]) {
    const arnes = await RouterTestingHarness.create(url)
    TestBed.inject(ApplicationRef).tick()
    http.expectOne(URL).flush(cola)
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    return { arnes, el: arnes.routeNativeElement as HTMLElement }
  }
  const filas = (el: HTMLElement, region: string) =>
    Object.fromEntries(Array.from(el.querySelectorAll(`[aria-label="${region}"] dl > div`)).map((f) => [f.querySelector('dt')?.textContent?.trim(), f.querySelector('dd')?.textContent?.trim()]))

  it('correcto: una tarjeta con cuatro pestañas; cabecera con nombre, estado en palabras e insignias; el resumen dice todo en palabras', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/v-7', [expediente({ verificacionId: 'v-otro' }), expediente()])
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Rosa Sintética Mamani')
    expect(Array.from(el.querySelectorAll('ap-cabecera-de-pagina ap-chip-estado')).map((c) => c.textContent?.trim())).toEqual(['En revisión', 'Documento: Vigente', 'Fotos: 5 de 5'])
    expect(Array.from(el.querySelectorAll('[role="tab"]')).map((t) => t.textContent?.trim().replace(/\s+/g, ' '))).toEqual(['Resumen', 'Documento y fotos 5', 'Decisión pendiente', 'Cómo funciona'])
    expect(el.querySelectorAll('article.tarjeta')).toHaveLength(1)
    const doc = filas(el, 'El documento')
    expect(doc['Tipo de documento']).toBe('Cédula de identidad')
    expect(doc['Lugar de expedición']).toBe('Cochabamba')
    expect(doc['Fecha de vencimiento']).toBe('2 mar 2031')
    expect(doc['Número del documento']).toContain('No viaja al backoffice')
    const exp = filas(el, 'El expediente')
    expect(exp['Estado']).toBe('En revisión')
    expect(exp['Resuelto']).toBe('Todavía no se decidió.')
    expect(exp['Carpeta en el archivo']).toContain('identidad/u-7/')
  })

  it('límite: un dato ausente se explica en vez de quedar vacío (sin correo, sin documento, sin fecha)', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/v-7', [expediente({ correo: undefined, documento: undefined, fechaExpiracionDocumento: undefined, documentoVigente: false, fotos: ['ANVERSO'] })])
    const persona = filas(el, 'La persona')
    expect(persona['Correo']).toContain('No registrado')
    const doc = filas(el, 'El documento')
    expect(doc['Tipo de documento']).toBe('La persona todavía no cargó su documento.')
    expect(doc['Fecha de vencimiento']).toContain('no se aprueba')
    expect(doc['Vigencia según el servidor']).toBe('Sin fecha de vencimiento')
    expect(filas(el, 'El expediente')['Fotos cargadas']).toContain('1 de 5 · faltan: Reverso del documento')
    expect(el.textContent).not.toMatch(/\bundefined\b|\bnull\b/)
  })

  it('límite: ?vista=fotos abre las cinco caras con su estado y lo que hay que mirar', async () => {
    const fotos = await abrir('/cumplimiento/verificaciones/v-7?vista=fotos', [expediente({ fotos: ['ANVERSO', 'SELFIE'] })])
    const caras = Array.from(fotos.el.querySelectorAll('.caras li')).map((l) => l.textContent?.replace(/\s+/g, ' ').trim())
    expect(caras).toHaveLength(5)
    expect(caras[0]).toContain('Anverso del documento · Cargada')
    expect(caras[1]).toContain('Reverso del documento · Falta')
    expect(fotos.el.textContent).toContain('Qué mirar antes de decidir')
  })

  it('límite: una vista desconocida en la URL cae en Resumen (un enlace viejo nunca deja la ficha en blanco)', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/v-7?vista=inventada', [expediente()])
    expect(el.querySelector('[role="tab"][aria-selected="true"]')?.textContent?.trim()).toBe('Resumen')
    expect(el.querySelector('[aria-label="La persona"]')).not.toBeNull()
  })

  it('correcto: ?vista=como-funciona explica negocio, recorrido, reglas, privacidad y sistema, con su fuente', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/v-7?vista=como-funciona', [expediente()])
    const titulos = Array.from(el.querySelectorAll('section h2')).map((h) => h.textContent?.trim())
    expect(titulos).toEqual(['Para qué existe esta revisión (negocio)', 'El recorrido de un expediente', 'Las reglas que hace cumplir el servidor', 'Por qué decide una persona y no un motor', 'Datos personales: qué se ve y qué no', 'Qué pasa por detrás (sistema)'])
    expect(el.textContent).toContain('AP-CU01-09')
    expect(el.querySelectorAll('.fuente').length).toBe(6)
  })

  it('correcto: aprobar desde la ficha envía con clave de idempotencia y recarga el expediente', async () => {
    const { arnes, el } = await abrir('/cumplimiento/verificaciones/v-7?vista=decision', [expediente()])
    expect(el.textContent).toContain('Nada impide aprobar')
    const aprobar = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Aprobar') as HTMLButtonElement
    expect(aprobar.disabled).toBe(false)
    aprobar.click()
    const envio = http.expectOne(`${URL}/v-7/decision`)
    expect(envio.request.body).toEqual({ decision: 'APROBAR', motivo: undefined })
    expect(envio.request.headers.get('Idempotency-Key')).toMatch(/.{8,}/)
    envio.flush(expediente({ estado: 'APROBADA', resueltaEn: '2026-10-21T10:00:00Z' }))
    // `reload()` del recurso se programa para el próximo ciclo: se deja correr antes de esperar el pedido.
    TestBed.inject(ApplicationRef).tick()
    http.expectOne(URL).flush([expediente({ estado: 'APROBADA', resueltaEn: '2026-10-21T10:00:00Z' })])
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    expect(el.textContent).toContain('Resultado: Aprobada')
    expect(el.textContent).toContain('AP-CU01-08')
  })

  it('inválido: con fotos faltantes y documento vencido, aprobar está apagado y la ficha dice por qué', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/v-7?vista=decision', [expediente({ fotos: ['ANVERSO'], documentoVigente: false, fechaExpiracionDocumento: '2020-01-01' })])
    expect(el.textContent).toContain('Por qué no se puede aprobar todavía')
    expect(el.textContent).toContain('Faltan 4 fotos')
    expect(el.textContent).toContain('El documento está vencido según el servidor.')
    const aprobar = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Aprobar') as HTMLButtonElement
    expect(aprobar.disabled).toBe(true)
  })

  it('inválido: un id que no está en la cola dice que no existe y ofrece volver', async () => {
    const { el } = await abrir('/cumplimiento/verificaciones/no-existe', [expediente()])
    expect(el.textContent).toContain('Este expediente no está en la cola de verificación.')
    expect(Array.from(el.querySelectorAll('button')).some((b) => b.textContent?.includes('Volver a la cola'))).toBe(true)
    expect(el.querySelector('[role="tab"]')).toBeNull()
  })

  it('inválido: un 500 al pedir la cola muestra un error accionable con su traza, sin el mensaje crudo', async () => {
    const arnes = await RouterTestingHarness.create('/cumplimiento/verificaciones/v-7')
    TestBed.inject(ApplicationRef).tick()
    http.expectOne(URL).flush({ codigo: 'X', mensaje: 'crudo', trazaId: 'traza-7' }, { status: 500, statusText: 'x' })
    await arnes.fixture.whenStable()
    arnes.fixture.detectChanges()
    const texto = (arnes.routeNativeElement as HTMLElement).textContent ?? ''
    expect(texto).toContain('traza-7')
    expect(texto).not.toContain('crudo')
  })
})
