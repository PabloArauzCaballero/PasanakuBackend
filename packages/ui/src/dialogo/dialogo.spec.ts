import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { Dialogo } from './dialogo'

@Component({
  selector: 'ap-anfitrion-de-prueba',
  imports: [Dialogo],
  template: `
    <button id="abridor" type="button" (click)="abierto.set(true)">Abrir</button>
    <ap-dialogo
      titulo="Confirmás el envío"
      textoDeConfirmar="Confirmar"
      [abierto]="abierto()"
      [puedeDescartar]="puedeDescartar()"
      (abiertoChange)="abierto.set($event)"
      (confirmar)="confirmaciones.set(confirmaciones() + 1)"
      (cancelar)="cancelaciones.set(cancelaciones() + 1)"
    >
      cuerpo
    </ap-dialogo>
  `,
})
class Anfitrion {
  readonly abierto = signal(false)
  readonly puedeDescartar = signal<() => boolean>(() => true)
  readonly confirmaciones = signal(0)
  readonly cancelaciones = signal(0)
}

function montar() {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(Anfitrion)
  fixture.detectChanges()
  return fixture
}

/**
 * jsdom no implementa `showModal` (ver el comentario en `dialogo.ts`): acá el diálogo se
 * abre por el atributo `open`, no por el navegador. La trampa y la restauración de foco
 * reales —que da gratis `showModal`/`close` del HTML Standard— se verifican en un
 * navegador de verdad (Playwright, `apps/backoffice/e2e/`), no acá. Lo que sí se puede
 * probar en jsdom, y con precisión, es la lógica de las tres rutas de cierre: que las tres
 * llaman al mismo predicado y que ninguna lo saltea.
 */
describe('Dialogo · política de descarte única', () => {
  it('botón cancelar: cierra y emite cancelar cuando puedeDescartar da true', () => {
    const fixture = montar()
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    expect(dialog.hasAttribute('open')).toBe(true)
    const botonCancelar = fixture.nativeElement.querySelectorAll('.acciones button')[0] as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(false)
    expect(fixture.componentInstance.cancelaciones()).toBe(1)
  })

  it('botón cancelar: NO cierra ni emite cancelar cuando puedeDescartar da false (borrador sucio)', () => {
    const fixture = montar()
    fixture.componentInstance.puedeDescartar.set(() => false)
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const botonCancelar = fixture.nativeElement.querySelectorAll('.acciones button')[0] as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(true)
    expect(fixture.componentInstance.cancelaciones()).toBe(0)
  })

  it('Escape (evento nativo `cancel`, cancelable): la misma política lo bloquea con preventDefault', () => {
    const fixture = montar()
    fixture.componentInstance.puedeDescartar.set(() => false)
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const evento = new Event('cancel', { cancelable: true })
    dialog.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(true)
    expect(fixture.componentInstance.cancelaciones()).toBe(0)
  })

  it('Escape: cuando puedeDescartar da true, no cancela el evento y emite cancelar', () => {
    const fixture = montar()
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const evento = new Event('cancel', { cancelable: true })
    dialog.dispatchEvent(evento)
    expect(evento.defaultPrevented).toBe(false)
    expect(fixture.componentInstance.cancelaciones()).toBe(1)
  })

  it('clic en el fondo: hoy cierra (antes de este cambio no hacía nada — el backdrop nativo no cierra `<dialog>` solo)', () => {
    const fixture = montar()
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    // El handler compara `evento.target === caja().nativeElement`; se dispara directo sobre `dialog`.
    const clickEnFondo = new MouseEvent('click')
    Object.defineProperty(clickEnFondo, 'target', { value: dialog })
    dialog.dispatchEvent(clickEnFondo)
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(false)
  })

  it('clic en el fondo: la misma política lo bloquea también', () => {
    const fixture = montar()
    fixture.componentInstance.puedeDescartar.set(() => false)
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const clickEnFondo = new MouseEvent('click')
    Object.defineProperty(clickEnFondo, 'target', { value: dialog })
    dialog.dispatchEvent(clickEnFondo)
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(true)
  })

  it('clic dentro del cuerpo no cuenta como clic en el fondo (el target no es el <dialog>)', () => {
    const fixture = montar()
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
    const cuerpo = fixture.nativeElement.querySelector('.cuerpo') as HTMLElement
    const clickInterno = new MouseEvent('click', { bubbles: true })
    Object.defineProperty(clickInterno, 'target', { value: cuerpo })
    dialog.dispatchEvent(clickInterno)
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(true)
  })

  it('abrir y cerrar cien veces no acumula instancias de <dialog> en el DOM', () => {
    const fixture = montar()
    for (let i = 0; i < 100; i++) {
      fixture.componentInstance.abierto.set(true)
      fixture.detectChanges()
      fixture.componentInstance.abierto.set(false)
      fixture.detectChanges()
    }
    expect(fixture.nativeElement.querySelectorAll('dialog').length).toBe(1)
  })

  it('confirmar no depende de puedeDescartar: guardar es una intención distinta de cerrar', () => {
    const fixture = montar()
    fixture.componentInstance.puedeDescartar.set(() => false)
    fixture.componentInstance.abierto.set(true)
    fixture.detectChanges()
    const botonConfirmar = fixture.nativeElement.querySelectorAll('.acciones button')[1] as HTMLButtonElement
    botonConfirmar.click()
    expect(fixture.componentInstance.confirmaciones()).toBe(1)
  })
})

describe('Dialogo · política de descarte por omisión', () => {
  it('sin [puedeDescartar], un diálogo sin formulario cierra siempre (compatibilidad hacia atrás)', () => {
    @Component({
      selector: 'ap-anfitrion-simple',
      imports: [Dialogo],
      template: `<ap-dialogo titulo="t" textoDeConfirmar="c" [abierto]="abierto()" (abiertoChange)="abierto.set($event)">cuerpo</ap-dialogo>`,
    })
    class AnfitrionSimple {
      readonly abierto = signal(true)
    }
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const fixture = TestBed.createComponent(AnfitrionSimple)
    fixture.detectChanges()
    const botonCancelar = fixture.nativeElement.querySelectorAll('.acciones button')[0] as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(fixture.componentInstance.abierto()).toBe(false)
  })
})
