import { provideHttpClient } from '@angular/common/http'
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'
import { provideZonelessChangeDetection } from '@angular/core'
import { provideRouter } from '@angular/router'
import { GATEWAY } from '../../../nucleo/gateway'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { PantallaDeCaso } from './pantalla-de-caso'

/**
 * Doble en memoria de `ServicioBorrador`: el entorno de pruebas del backoffice no
 * expone `indexedDB` (jsdom sin polyfill, y `fake-indexeddb` no está en el catálogo
 * de dependencias que este carril puede tocar). Comparte el mismo contrato público
 * (`guardar`/`leer`/`borrar`), así que la pantalla se prueba igual, sin depender del
 * navegador real — eso ya lo cubre el uso real de `ServicioBorrador` en producción.
 */
class ServicioBorradorEnMemoria {
  private readonly almacen = new Map<string, unknown>()
  async guardar(clave: string, valor: unknown): Promise<void> {
    this.almacen.set(clave, valor)
  }
  async leer<T>(clave: string): Promise<T | undefined> {
    return this.almacen.get(clave) as T | undefined
  }
  async borrar(clave: string): Promise<void> {
    this.almacen.delete(clave)
  }
}

const borradorCompartido = new ServicioBorradorEnMemoria()

/** Ficha sintética de `obtenerCasoLft`: el titular ya viene enmascarado del servidor. */
const FICHA = {
  casoId: 'c1000000-0000-4000-8000-0000000000a2',
  codigo: 'LFT-2026-0042',
  origen: 'ALERTA',
  estado: 'EN_ANALISIS',
  prioridad: 'ALTA',
  abiertoEn: '2026-01-15T10:30:00Z',
  plazoLimite: '2026-02-14T10:30:00Z',
  vencido: false,
  alertaDeOrigen: { alertaId: 'c1000000-0000-4000-8000-0000000000a1', reglaCodigo: 'RM-FRACCIONAMIENTO', tipologia: 'Fraccionamiento de operaciones', severidad: 'ALTA', detectadaEn: '2026-01-14T18:05:00Z' },
  alertasEnlazadas: 3,
  titularEnmascarado: '****7c2e',
}

async function montar(casoId = 'caso-1') {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: GATEWAY, useValue: 'http://gw/api/v1' }, { provide: ServicioBorrador, useValue: borradorCompartido }] })
  const fixture = TestBed.createComponent(PantallaDeCaso)
  fixture.componentRef.setInput('casoId', casoId)
  fixture.detectChanges()
  await fixture.whenStable()
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
  return fixture
}

describe('PantallaDeCaso · gate del carril', () => {
  it('rechazar u observar SIN CAUSAL DEL CATÁLOGO es imposible: el botón de confirmar queda deshabilitado sin selección', async () => {
    const fixture = await montar()
    const boton = fixture.nativeElement.querySelector('ap-boton button') as HTMLButtonElement
    expect(boton.disabled).toBe(true)

    const radio = fixture.nativeElement.querySelector('input[type="radio"]') as HTMLInputElement
    radio.click()
    fixture.detectChanges()
    await fixture.whenStable()

    expect(boton.disabled).toBe(false)
  })

  it('el resumen del caso dice qué caso es, el paso en curso y la causal, y declara que la ficha está cargando', async () => {
    const fixture = await montar('caso-resumen')
    const resumen = () => (fixture.nativeElement.querySelector('ap-resumen-de-caso') as HTMLElement).textContent ?? ''
    expect(resumen()).toContain('caso-resumen')
    expect(fixture.nativeElement.querySelector('ap-barra-de-pasos')?.getAttribute('aria-label')).toBe('Paso 1 de 6: Causal')
    expect(resumen()).toContain('Sin elegir todavía')
    expect(resumen()).toContain('Cargando la ficha del caso')
    expect(resumen()).not.toContain('todavía no los publica')
    ;(fixture.nativeElement.querySelector('input[type="radio"]') as HTMLInputElement).click()
    fixture.detectChanges()
    expect(resumen()).toContain('Fraccionamiento de operaciones')
  })

  it('con la ficha del servidor muestra la alerta de origen y el titular ENMASCARADO (nunca otro dato de la persona)', async () => {
    const fixture = await montar('c1000000-0000-4000-8000-0000000000a2')
    const http = TestBed.inject(HttpTestingController)
    http.expectOne('http://gw/api/v1/cumplimiento/casos/c1000000-0000-4000-8000-0000000000a2').flush(FICHA)
    fixture.detectChanges()
    const resumen = (fixture.nativeElement.querySelector('ap-resumen-de-caso') as HTMLElement).textContent ?? ''
    expect(resumen).toContain('LFT-2026-0042')
    expect(resumen).toContain('RM-FRACCIONAMIENTO')
    expect(resumen).toContain('Fraccionamiento de operaciones')
    expect(resumen).toContain('Alta')
    expect(resumen).toContain('3 alertas enlazadas')
    expect(resumen).toContain('****7c2e')
    expect(resumen).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/)
    http.verify()
  })

  it('un caso sin alerta de origen lo dice, y un 403 de la ficha se explica sin el error crudo', async () => {
    const sinAlerta = await montar('caso-denuncia')
    const http = TestBed.inject(HttpTestingController)
    http.expectOne('http://gw/api/v1/cumplimiento/casos/caso-denuncia').flush({ ...FICHA, origen: 'DENUNCIA', alertaDeOrigen: null, alertasEnlazadas: 0 })
    sinAlerta.detectChanges()
    expect(sinAlerta.nativeElement.textContent).toContain('El caso no nació de una alerta de monitoreo')

    const prohibido = await montar('caso-ajeno')
    const http2 = TestBed.inject(HttpTestingController)
    http2.expectOne('http://gw/api/v1/cumplimiento/casos/caso-ajeno').flush({ codigo: 'X', mensaje: 'detalle interno', trazaId: 't' }, { status: 403, statusText: 'Forbidden' })
    prohibido.detectChanges()
    await prohibido.whenStable()
    prohibido.detectChanges()
    const texto = prohibido.nativeElement.textContent as string
    expect(texto).not.toContain('detalle interno')
    expect(texto).toMatch(/Tu rol no puede ver la ficha de este caso|No se pudo cargar la ficha del caso/)
  })

  it('el formulario largo guarda borrador con ServicioBorrador y lo recupera tras una sesión caída', async () => {
    const primera = await montar('caso-borrador')
    const area = primera.nativeElement.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'La contraparte fraccionó cinco depósitos el mismo día.'
    area.dispatchEvent(new Event('input'))
    primera.detectChanges()
    await primera.whenStable()
    // deja tiempo a que la escritura asíncrona a IndexedDB termine antes de "recargar"
    await new Promise((r) => setTimeout(r, 50))
    primera.destroy()

    const segunda = await montar('caso-borrador')
    expect(segunda.nativeElement.querySelector('textarea').value).toBe('La contraparte fraccionó cinco depósitos el mismo día.')
    expect(segunda.nativeElement.textContent).toContain('Se recuperó un borrador')
  })

  it('«Confirmar decisión» ya no es un botón muerto: abre la decisión real del caso con la causal y la narrativa como borrador de la conclusión', async () => {
    const fixture = await montar('caso-decidir')
    ;(fixture.nativeElement.querySelector('input[type="radio"]') as HTMLInputElement).click()
    const area = fixture.nativeElement.querySelector('textarea#narrativa') as HTMLTextAreaElement
    area.value = 'Cinco depósitos bajo el umbral el mismo día.'
    area.dispatchEvent(new Event('input'))
    fixture.detectChanges()
    ;(fixture.nativeElement.querySelector('ap-formulario-de-caso ap-boton button') as HTMLButtonElement).click()
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain('Decidir el caso caso-decidir')
    const conclusion = [...fixture.nativeElement.querySelectorAll('dialog textarea')].at(-1) as HTMLTextAreaElement
    expect(conclusion.value).toBe('Causal: Fraccionamiento de operaciones. Cinco depósitos bajo el umbral el mismo día.')
  })

  it('el borrador se borra recién cuando el servidor registró la decisión, y se ve el recibo', async () => {
    const fixture = await montar('caso-recibo')
    const http = TestBed.inject(HttpTestingController)
    http.expectOne('http://gw/api/v1/cumplimiento/casos/caso-recibo').flush({ ...FICHA, casoId: 'caso-recibo' })
    await borradorCompartido.guardar('cumplimiento:caso:caso-recibo:narrativa', 'texto')
    ;(fixture.componentInstance as unknown as { alDecidir: (s: unknown) => void }).alDecidir({ decision: 'DESCARTAR', alertasCerradas: 2, fueraDePlazo: false })
    fixture.detectChanges()
    await new Promise((r) => setTimeout(r, 0))
    expect(await borradorCompartido.leer('cumplimiento:caso:caso-recibo:narrativa')).toBeUndefined()
    expect(fixture.nativeElement.textContent).toContain('Decisión registrada: Descartar. Se cerraron 2 alertas.')
    http.verify()
  })
})
