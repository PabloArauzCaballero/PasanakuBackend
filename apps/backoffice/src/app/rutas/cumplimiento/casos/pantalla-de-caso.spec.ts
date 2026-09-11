import { provideZonelessChangeDetection } from '@angular/core'
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
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ServicioBorrador, useValue: borradorCompartido }] })
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
})
