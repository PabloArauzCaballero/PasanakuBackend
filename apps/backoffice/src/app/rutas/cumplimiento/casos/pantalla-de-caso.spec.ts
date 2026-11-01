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
    await borradorCompartido.guardar('cumplimiento:caso:caso-recibo:narrativa', 'texto')
    ;(fixture.componentInstance as unknown as { alDecidir: (s: unknown) => void }).alDecidir({ decision: 'DESCARTAR', alertasCerradas: 2, fueraDePlazo: false })
    fixture.detectChanges()
    await new Promise((r) => setTimeout(r, 0))
    expect(await borradorCompartido.leer('cumplimiento:caso:caso-recibo:narrativa')).toBeUndefined()
    expect(fixture.nativeElement.textContent).toContain('Decisión registrada: Descartar. Se cerraron 2 alertas.')
    http.verify()
  })
})
