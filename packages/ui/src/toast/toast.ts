import { ChangeDetectionStrategy, Component, computed, inject, Injectable, signal } from '@angular/core'

export type Aviso = { id: number; texto: string; tono: 'ok' | 'error' | 'info'; accion?: { texto: string; alPulsar: () => void } }

/** Cola de avisos efímeros: uno a la vez, el siguiente espera. Un error no se va solo. */
@Injectable({ providedIn: 'root' })
export class Avisos {
  private static secuencia = 0
  private readonly cola = signal<Aviso[]>([])
  readonly actual = computed(() => this.cola()[0] ?? null)

  mostrar(texto: string, tono: Aviso['tono'] = 'info', accion?: Aviso['accion']): void {
    const aviso = { id: ++Avisos.secuencia, texto, tono, accion }
    this.cola.update((c) => [...c, aviso])
    if (tono !== 'error') setTimeout(() => this.cerrar(aviso.id), 5000)
  }

  cerrar(id: number): void {
    this.cola.update((c) => c.filter((a) => a.id !== id))
  }
}

/** Se monta una vez en el shell. Anuncia sin robar el foco (`aria-live`). */
@Component({
  selector: 'ap-toast',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-live': 'polite' },
  template: `
    @if (avisos.actual(); as a) {
      <div class="aviso" [class]="'aviso tono-' + a.tono" [attr.role]="a.tono === 'error' ? 'alert' : 'status'">
        <span class="texto">{{ a.texto }}</span>
        @if (a.accion) { <button type="button" (click)="a.accion.alPulsar(); avisos.cerrar(a.id)">{{ a.accion.texto }}</button> }
        <button type="button" aria-label="Cerrar el aviso" (click)="avisos.cerrar(a.id)">✕</button>
      </div>
    }
  `,
  styles: `
    :host { position: fixed; inset-inline: var(--s4); bottom: var(--s5); display: flex; justify-content: center; pointer-events: none; z-index: 10; }
    .aviso { display: flex; align-items: center; gap: var(--s3); max-width: 60ch; padding: var(--s3) var(--s4); border-radius: var(--r-md); background: var(--ink); color: var(--white); pointer-events: auto; box-shadow: var(--sombra-3); }
    .tono-ok { border-left: var(--s1) solid var(--ok); }
    .tono-error { border-left: var(--s1) solid var(--err); }
    .texto { flex: 1; }
    button { min-height: var(--s6); padding: 0 var(--s2); border: 0; border-radius: var(--r-sm); background: transparent; color: var(--o300); font: inherit; font-weight: 600; cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); }
  `,
})
export class Toast {
  readonly avisos = inject(Avisos)
}
