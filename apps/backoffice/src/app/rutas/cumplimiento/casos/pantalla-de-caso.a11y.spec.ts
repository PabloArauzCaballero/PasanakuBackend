import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { PantallaDeCaso } from './pantalla-de-caso'

class ServicioBorradorEnMemoria {
  async guardar(): Promise<void> {}
  async leer(): Promise<undefined> {
    return undefined
  }
  async borrar(): Promise<void> {}
}

describe('PantallaDeCaso · accesibilidad', () => {
  it('sin violaciones serias, con la causal sin elegir y el botón deshabilitado', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), { provide: ServicioBorrador, useValue: new ServicioBorradorEnMemoria() }] })
    const fixture = TestBed.createComponent(PantallaDeCaso)
    fixture.componentRef.setInput('casoId', 'caso-a11y')
    fixture.detectChanges()
    await fixture.whenStable()
    expect(await axe(fixture.nativeElement)).toHaveNoViolations()
  })
})
