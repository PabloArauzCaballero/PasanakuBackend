import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { ServicioBorrador } from '../../../nucleo/borrador'
import type { ReclamoDelConsumidor } from '../dominio/cu52-reclamo'
import { PantallaDeReclamo } from './pantalla-de-reclamo'

const RECLAMO: ReclamoDelConsumidor = {
  id: 'reclamo-1',
  registradoEn: '2026-09-01T10:00:00-04:00',
  plazoVenceEn: '2026-09-15T10:00:00-04:00',
  norma: 'ASFI · Reglamento de atención al consumidor financiero',
  respuesta: null,
}

/** Ver el mismo doble en `casos/pantalla-de-caso.spec.ts`: sin `indexedDB` en el entorno de pruebas. */
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

async function montar() {
  TestBed.resetTestingModule()
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ServicioBorrador, useValue: borradorCompartido }] })
  const fixture = TestBed.createComponent(PantallaDeReclamo)
  fixture.componentRef.setInput('reclamo', RECLAMO)
  fixture.componentRef.setInput('ahoraIso', '2026-09-11T10:00:00-04:00')
  fixture.detectChanges()
  await fixture.whenStable()
  await new Promise((r) => setTimeout(r, 0))
  fixture.detectChanges()
  return fixture
}

describe('PantallaDeReclamo', () => {
  it('el plazo mostrado es el guardado en el reclamo, con su fecha visible — nunca uno recalculado en el cliente', async () => {
    const fixture = await montar()
    // El componente recibe literalmente `plazoVenceEn` del reclamo, sin transformarlo.
    expect(fixture.componentInstance.reclamo().plazoVenceEn).toBe(RECLAMO.plazoVenceEn)
    expect(fixture.nativeElement.textContent).toContain('Faltan 4 días')
  })

  it('la respuesta larga guarda borrador y lo recupera tras una sesión caída', async () => {
    const primera = await montar()
    const area = primera.nativeElement.querySelector('textarea') as HTMLTextAreaElement
    area.value = 'Se revisó el cargo y corresponde una devolución parcial.'
    area.dispatchEvent(new Event('input'))
    primera.detectChanges()
    await primera.whenStable()
    primera.destroy()

    const segunda = await montar()
    expect(segunda.nativeElement.querySelector('textarea').value).toBe('Se revisó el cargo y corresponde una devolución parcial.')
  })
})
