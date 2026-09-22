import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Fecha } from '../fecha/fecha'

export type PasoDeSorteo = { nombre: string; hash: string; cuandoIso: string }

/**
 * El sorteo se puede reproducir: semilla comprometida, semilla revelada, orden
 * resultante. Con `coincide` dice si lo reproducido es lo guardado. En el sitio público
 * es la verificación abierta; en la app, el panel del grupo.
 */
@Component({
  selector: 'ap-verificador-de-sorteo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado, Fecha],
  host: { role: 'region', 'aria-label': 'Verificación del sorteo' },
  template: `
    <ol>
      @for (p of pasos(); track p.nombre; let i = $index) {
        <li>
          <span class="numero" aria-hidden="true">{{ i + 1 }}</span>
          <div><p class="nombre">{{ p.nombre }}</p><ap-fecha [iso]="p.cuandoIso" /><p class="hash">{{ p.hash }}</p></div>
        </li>
      }
    </ol>
    @if (coincide() !== undefined) {
      <ap-chip-estado [tono]="coincide() ? 'ok' : 'error'" [icono]="coincide() ? 'verificado' : 'alerta'">{{ coincide() ? 'El orden reproducido coincide con el guardado' : 'El orden NO coincide' }}</ap-chip-estado>
    }
    <div class="acciones">
      @if (conReproducir()) { <ap-boton variante="secundario" (pulsado)="reproducir.emit()">Reproducir con la semilla</ap-boton> }
      @if (enlacePublico()) { <a [href]="enlacePublico()">Verificación pública</a> }
    </div>
  `,
  styles: `
    ol { margin: 0 0 var(--s3); padding: 0; list-style: none; display: flex; flex-direction: column; gap: var(--s3); }
    li { display: flex; gap: var(--s3); }
    .numero { display: inline-flex; align-items: center; justify-content: center; width: var(--s5); height: var(--s5); flex: none; border-radius: var(--r-pill); background: var(--verde-solido); color: var(--sobre-verde-solido); font-size: .8em; font-weight: 600; }
    p { margin: 0; }
    .nombre { color: var(--text); }
    .hash { font-family: var(--mono); font-size: .8em; color: var(--text-2); overflow-wrap: anywhere; }
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s3); margin-top: var(--s3); }
    a { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class VerificadorDeSorteo {
  readonly pasos = input.required<PasoDeSorteo[]>()
  readonly coincide = input<boolean>()
  readonly conReproducir = input(false)
  readonly enlacePublico = input<string>()
  readonly reproducir = output<void>()
}
