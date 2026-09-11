import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { motivoDeBloqueo, puedeCerrarActa, type ActaDeComite } from '../dominio/cu94-acta'
import { textosCumplimiento } from '../textos'

/**
 * CU-94 · Elevar una decisión al comité de gobierno. El acta registra voto nominal y
 * abstención con su motivo, y NO SE CIERRA SIN QUÓRUM: el botón de cerrar queda
 * deshabilitado — y muestra por qué — mientras falte quórum, falte un rol de la
 * composición requerida, o haya una abstención sin motivo escrito.
 */
@Component({
  selector: 'ap-pantalla-de-acta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, ChipEstado, Boton],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>

      <p>{{ t.quorum }}: {{ acta().asistentes.length }} / {{ acta().quorumMinimo }}</p>

      <ul class="votos">
        @for (v of acta().asistentes; track v.integranteId) {
          <li>
            <span>{{ v.rol }}</span>
            <ap-chip-estado [tono]="tonoDe(v.voto)">{{ v.voto }}</ap-chip-estado>
            @if (v.voto === 'ABSTENCION') { <span class="motivo">{{ v.motivoAbstencion || t.motivoAbstencion + ' — falta' }}</span> }
          </li>
        }
      </ul>

      @if (bloqueo(); as motivo) {
        <p class="bloqueo" role="alert">{{ mensajeDeBloqueo(motivo) }}</p>
      }

      <ap-boton variante="primario" [deshabilitado]="!puedeCerrar()" (pulsado)="cerrar()">{{ t.cerrar }}</ap-boton>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    .votos { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s2); }
    .votos li { display: flex; align-items: center; gap: var(--s3); }
    .motivo { color: var(--text-3); font-size: .9em; }
    .bloqueo { color: var(--err); }
  `,
})
export class PantallaDeActa {
  protected readonly t = textosCumplimiento.actas
  readonly acta = input.required<ActaDeComite>()
  protected readonly puedeCerrar = computed(() => puedeCerrarActa(this.acta()))
  protected readonly bloqueo = computed(() => motivoDeBloqueo(this.acta()))
  protected cerrada = false

  protected tonoDe(voto: string) {
    return voto === 'A_FAVOR' ? 'ok' : voto === 'EN_CONTRA' ? 'error' : 'neutro'
  }

  protected mensajeDeBloqueo(motivo: string): string {
    if (motivo === 'sinQuorum') return this.t.sinQuorum
    if (motivo === 'composicionIncompleta') return this.t.composicionIncompleta
    return 'Falta el motivo de una abstención registrada.'
  }

  protected cerrar(): void {
    if (!puedeCerrarActa(this.acta())) return
    this.cerrada = true
  }
}
