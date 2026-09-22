import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { PantallaDespliegues } from './pantalla-despliegues'

/** GATE: un interruptor que toca dinero exige dos personas; la interfaz impide confirmarlo solo/a. */
describe('PantallaDespliegues · doble confirmación del interruptor que toca dinero', () => {
  function montar() {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(PantallaDespliegues)
    fixture.detectChanges()
    return fixture
  }

  it('el interruptor que toca dinero está deshabilitado: no se activa con un clic directo', () => {
    const fixture = montar()
    const interruptorDinero = fixture.nativeElement.querySelectorAll('ap-interruptor button')[0] as HTMLButtonElement
    expect(interruptorDinero.disabled).toBe(true)
  })

  it('pedir el cambio abre el diálogo y avisa que falta otra persona', async () => {
    const fixture = montar()
    const botonPedir = fixture.nativeElement.querySelector('.pedir-cambio') as HTMLButtonElement
    botonPedir.click()
    fixture.detectChanges()
    await fixture.whenStable()
    expect(fixture.nativeElement.textContent).toContain('Falta que otra persona lo confirme')
  })

  it('confirmar con el mismo correo de quien lo pidió no cierra el cambio', async () => {
    const fixture = montar()
    const instancia = fixture.componentInstance as unknown as { pedirCambio: (i: { id: string; nombre: string; tocaDinero: boolean; activo: boolean }) => void; correoConfirmante: { set(v: string): void }; confirmar: () => void; interruptores: () => { id: string; activo: boolean }[] }
    instancia.pedirCambio({ id: 'i-1', nombre: 'x', tocaDinero: true, activo: false })
    instancia.correoConfirmante.set('operador.actual@aportaya.bo')
    instancia.confirmar()
    fixture.detectChanges()
    expect(instancia.interruptores().find((i) => i.id === 'i-1')?.activo).toBe(false)
  })

  it('confirmar con un correo distinto sí cierra el cambio', async () => {
    const fixture = montar()
    const instancia = fixture.componentInstance as unknown as { pedirCambio: (i: { id: string; nombre: string; tocaDinero: boolean; activo: boolean }) => void; correoConfirmante: { set(v: string): void }; confirmar: () => void; interruptores: () => { id: string; activo: boolean }[] }
    instancia.pedirCambio({ id: 'i-1', nombre: 'x', tocaDinero: true, activo: false })
    instancia.correoConfirmante.set('otra.persona@aportaya.bo')
    instancia.confirmar()
    fixture.detectChanges()
    expect(instancia.interruptores().find((i) => i.id === 'i-1')?.activo).toBe(true)
  })
})
