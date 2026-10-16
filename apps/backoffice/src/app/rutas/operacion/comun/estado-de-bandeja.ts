import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { EstadoDeBandeja } from '../dominio/bandeja-por-cursor'

/**
 * Los estados de una bandeja paginada por cursor que la tabla no dibuja por su cuenta: vacío explicado, error
 * traducido con reintento y sin acceso (403). La carga la anuncia la propia tabla. Los textos los pone cada pantalla.
 */
@Component({
  selector: 'ap-estado-de-bandeja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, EstadoVacio],
  template: `
    @switch (estado()) {
      @case ('vacio') {
        <ap-estado-vacio [motivo]="conFiltro() ? 'porFiltro' : 'sinDatos'" [titulo]="vacio()" [explicacion]="explicacionVacio()" [accionPropia]="conFiltro() ? undefined : ''" (actuar)="quitarFiltros.emit()" />
      }
      @case ('denegado') {
        <ap-estado-vacio motivo="porPermiso" titulo="No tenés acceso a esta bandeja." [explicacion]="denegado()" />
      }
      @case ('error') {
        <ap-alerta tono="error" titulo="No pudimos traer la bandeja.">
          {{ error()?.mensaje }}
          @if (error()?.trazaId; as traza) { <span class="traza">Código de seguimiento: {{ traza }}</span> }
        </ap-alerta>
        <div><ap-boton (pulsado)="reintentar.emit()">Reintentar</ap-boton></div>
      }
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    :host:empty { display: none; }
    .traza { display: block; margin-top: var(--s1); font-size: .875rem; overflow-wrap: anywhere; }
  `,
})
export class EstadoDeBandejaVista {
  readonly estado = input.required<EstadoDeBandeja>()
  readonly error = input<ErrorTraducido | null>(null)
  readonly vacio = input.required<string>()
  readonly explicacionVacio = input<string>()
  readonly denegado = input.required<string>()
  readonly conFiltro = input(false)
  readonly reintentar = output<void>()
  readonly quitarFiltros = output<void>()
}
