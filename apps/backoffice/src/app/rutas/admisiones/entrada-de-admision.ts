import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { textosAdmisiones } from './textos'

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * Puerta de entrada a las solicitudes de ingreso. El contrato de `grupos` no tiene (todavía) un
 * listado de solicitudes pendientes, así que cada una se abre por su identificador, que llega en
 * el aviso de la solicitud. La pantalla lo dice en vez de simular una bandeja que no existe.
 */
@Component({
  selector: 'ap-entrada-de-admision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Campo],
  template: `
    <ap-banda-de-proposito [texto]="t.buscarProposito" />
    <main>
      <h1>{{ t.buscarTitulo }}</h1>
      <p class="aviso">{{ t.buscarSinListado }}</p>
      <form (submit)="$event.preventDefault(); abrir()" novalidate>
        <ap-campo [etiqueta]="t.buscarEtiqueta" [ayuda]="t.buscarAyuda" [error]="error()" [(valor)]="id" />
        <div class="acciones">
          <ap-boton variante="primario" tipo="submit">{{ t.buscarAccion }}</ap-boton>
        </div>
      </form>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    h1 { margin: 0; }
    .aviso { margin: 0; color: var(--text-2); }
    form { display: flex; flex-direction: column; gap: var(--s4); }
  `,
})
export class EntradaDeAdmision {
  protected readonly t = textosAdmisiones
  private readonly router = inject(Router)
  protected readonly id = signal('')
  protected readonly error = signal('')

  protected abrir(): void {
    const valor = this.id().trim()
    if (!UUID.test(valor)) {
      this.error.set(this.t.buscarError)
      return
    }
    this.error.set('')
    void this.router.navigate(['/admisiones', valor.toLowerCase()])
  }
}
