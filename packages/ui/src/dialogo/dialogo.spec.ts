import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { provideZonelessChangeDetection } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Dialogo } from './dialogo'

/**
 * H3.S2.M2 (kill-test del encargo) + H3.S1.M1/M4.
 *
 * Antes de este archivo, ninguna de las tres rutas de cierre (botón/Escape/fondo) protegía
 * un borrador sucio, y el clic en el fondo no cerraba nada (ver entregables/contrato-dialogo.md
 * §3). Este spec prueba que HOY las tres pasan por `intentarCerrar()` y ninguna la saltea.
 */
@Component({
  selector: 'ap-anfitrion-de-prueba',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo],
  template: `
    <button type="button" id="abridor" (click)="abierto.set(true)">Abrir</button>
    <ap-dialogo titulo="Confirmar" textoDeConfirmar="Confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [hayCambiosSinGuardar]="sucio()" (confirmar)="confirmaciones.set(confirmaciones() + 1)" (cancelar)="cancelaciones.set(cancelaciones() + 1)">
      <input id="campo" type="text" />
    </ap-dialogo>
  `,
})
class AnfitrionDePrueba {
  readonly abierto = signal(true)
  readonly sucio = signal(false)
  readonly confirmaciones = signal(0)
  readonly cancelaciones = signal(0)
}

function montar() {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
  const fixture = TestBed.createComponent(AnfitrionDePrueba)
  fixture.detectChanges()
  const caja = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement
  return { fixture, caja }
}

describe('Dialogo · una sola política de descarte (H3.S2.M2)', () => {
  let confirmSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    confirmSpy = vi.spyOn(window, 'confirm')
  })
  afterEach(() => confirmSpy.mockRestore())

  it('sin borrador sucio: el botón cierra directo, sin preguntar nada', () => {
    const { fixture, caja } = montar()
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(caja.hasAttribute('open')).toBe(false)
    expect((fixture.componentInstance as AnfitrionDePrueba).cancelaciones()).toBe(1)
  })

  it('borrador sucio + botón "Cancelar": pregunta, y si se acepta descartar, cierra', () => {
    confirmSpy.mockReturnValue(true)
    const { fixture, caja } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    expect(caja.hasAttribute('open')).toBe(false)
  })

  it('borrador sucio + botón "Cancelar": si NO se acepta descartar, el diálogo sigue abierto y el borrador no se pierde', () => {
    confirmSpy.mockReturnValue(false)
    const { fixture, caja } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    const botonCancelar = fixture.nativeElement.querySelectorAll('ap-boton')[0].querySelector('button') as HTMLButtonElement
    botonCancelar.click()
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    expect(caja.hasAttribute('open')).toBe(true)
    expect((fixture.componentInstance as AnfitrionDePrueba).cancelaciones()).toBe(0)
  })

  it('Escape (evento nativo `cancel`) con borrador sucio pasa por la MISMA guardia que el botón — antes de este cambio no preguntaba nada', () => {
    confirmSpy.mockReturnValue(false)
    const { fixture, caja } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    const evento = new Event('cancel', { cancelable: true })
    caja.dispatchEvent(evento)
    fixture.detectChanges()
    expect(evento.defaultPrevented).toBe(true)
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    expect(caja.hasAttribute('open')).toBe(true)
  })

  it('Escape sin borrador sucio no pregunta y cierra', () => {
    const { fixture, caja } = montar()
    caja.dispatchEvent(new Event('cancel', { cancelable: true }))
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(caja.hasAttribute('open')).toBe(false)
  })

  it('clic en el fondo (target === el propio <dialog>) con borrador sucio pasa por la MISMA guardia — antes de este cambio no cerraba nada, protegido o no', () => {
    confirmSpy.mockReturnValue(true)
    const { fixture, caja } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    caja.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    fixture.detectChanges()
    expect(confirmSpy).toHaveBeenCalledTimes(1)
    expect(caja.hasAttribute('open')).toBe(false)
  })

  it('clic en el CONTENIDO del diálogo (target !== el propio <dialog>) no cierra ni pregunta', () => {
    const { fixture, caja } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    const campo = fixture.nativeElement.querySelector('#campo') as HTMLInputElement
    campo.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(caja.hasAttribute('open')).toBe(true)
  })

  it('el botón "Confirmar" nunca pasa por la guardia de descarte: guardar es una intención explícita, no un cierre', () => {
    const { fixture } = montar()
    ;(fixture.componentInstance as AnfitrionDePrueba).sucio.set(true)
    fixture.detectChanges()
    const botonConfirmar = fixture.nativeElement.querySelectorAll('ap-boton')[1].querySelector('button') as HTMLButtonElement
    botonConfirmar.click()
    fixture.detectChanges()
    expect(confirmSpy).not.toHaveBeenCalled()
    expect((fixture.componentInstance as AnfitrionDePrueba).confirmaciones()).toBe(1)
  })

  it('conteo de escuchas antes/después: se agrega UNA vez al montar la vista y se saca UNA vez al destruir (H3.S1.M4)', () => {
    // `addEventListener` se espía en el prototipo (el elemento todavía no existe antes de montar);
    // como `isolate: false` corre varios archivos de spec en el mismo realm de jsdom, se filtran
    // las llamadas a SOLO esta instancia de `<dialog>` por `this` (`mock.instances`), no por conteo
    // global — otros dialogos de otros specs del mismo proceso no deben contaminar el número.
    const agregar = vi.spyOn(HTMLDialogElement.prototype, 'addEventListener')
    const { fixture, caja } = montar()
    const llamadasDeEstaCaja = agregar.mock.calls.filter((_, i) => agregar.mock.instances[i] === caja)
    // Antes de destruir: una escucha agregada, ninguna sacada todavía.
    expect(llamadasDeEstaCaja).toHaveLength(1)
    expect(llamadasDeEstaCaja[0][0]).toBe('click')
    const quitar = vi.spyOn(caja, 'removeEventListener')
    expect(quitar).not.toHaveBeenCalled()
    fixture.destroy()
    // Después de destruir: la misma (y única) escucha, sacada.
    expect(quitar).toHaveBeenCalledTimes(1)
    expect(quitar).toHaveBeenCalledWith('click', expect.any(Function))
    agregar.mockRestore()
  })

  it('100 aperturas/cierres seguidas no acumulan preguntas de más ni dejan el diálogo en un estado inconsistente (H3.S1.M4, sin fugas)', () => {
    confirmSpy.mockReturnValue(true)
    const { fixture, caja } = montar()
    const host = fixture.componentInstance as AnfitrionDePrueba
    for (let i = 0; i < 100; i++) {
      host.sucio.set(i % 2 === 0)
      host.abierto.set(true)
      fixture.detectChanges()
      caja.dispatchEvent(new MouseEvent('click', { bubbles: true }))
      fixture.detectChanges()
    }
    expect(caja.hasAttribute('open')).toBe(false)
    // Solo las 50 aperturas "sucias" preguntaron; las 50 limpias cerraron directo.
    expect(confirmSpy).toHaveBeenCalledTimes(50)
  })
})

describe('Dialogo · foco (H3.S1.M2) — lo que se puede afirmar sin `showModal` real en jsdom', () => {
  it('al abrir, el `<dialog>` existe en el DOM listo para que el navegador real le dé foco con showModal()', () => {
    const { caja } = montar()
    expect(caja).not.toBeNull()
    expect(caja.getAttribute('aria-labelledby')).toContain('-titulo')
  })

  it('NO CUBIERTO en este spec: jsdom no implementa showModal() (ver comentario en dialogo.ts) — foco inicial real, trampa de Tab y restauración de foco al cerrar requieren un navegador real (Playwright). Este test documenta la limitación, no la esconde.', () => {
    expect(typeof HTMLDialogElement.prototype.showModal).not.toBe('function')
  })
})
