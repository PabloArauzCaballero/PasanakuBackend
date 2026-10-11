import { Component, provideZonelessChangeDetection, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'
import { describe, expect, it } from 'vitest'
import { Dialogo } from './dialogo'

/**
 * H12.S1.M4 · el diálogo que se muestra con `@if` (así lo usan reclamos, solicitudes escaladas y cierre de período) se DESTRUYE al cerrar.
 * Antes el foco se perdía en <body>; ahora vuelve a quien lo abrió. En el navegador real lo confirma
 * `apps/backoffice/e2e/f2-red-y-teclado.e2e.ts`; acá se prueba la rama que sirve en jsdom (sin `showModal`).
 */
@Component({
  imports: [Dialogo],
  template: `
    <button id="abre" (click)="abierto.set(true)">Abrir</button>
    @if (abierto()) {
      <ap-dialogo titulo="T" textoDeConfirmar="Sí" [abierto]="true" (cancelar)="abierto.set(false)">cuerpo</ap-dialogo>
    }
  `,
})
class Anfitrion {
  readonly abierto = signal(false)
}

describe('Dialogo · devuelve el foco a quien lo abrió', () => {
  it('al destruirse abierto, el foco que quedó en <body> vuelve al botón que lo abrió', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] })
    const f = TestBed.createComponent(Anfitrion)
    document.body.appendChild(f.nativeElement)
    f.detectChanges()
    const abre = f.nativeElement.querySelector('#abre') as HTMLButtonElement
    abre.focus()
    abre.click()
    f.detectChanges()
    await f.whenStable()
    expect(f.nativeElement.querySelector('ap-dialogo')).not.toBeNull()
    abre.blur() // el navegador real deja el foco fuera del botón mientras el modal está abierto
    expect(document.activeElement).toBe(document.body)
    f.componentInstance.abierto.set(false)
    f.detectChanges()
    await f.whenStable()
    expect(f.nativeElement.querySelector('ap-dialogo')).toBeNull()
    expect(document.activeElement).toBe(abre)
    f.nativeElement.remove()
  })
})
