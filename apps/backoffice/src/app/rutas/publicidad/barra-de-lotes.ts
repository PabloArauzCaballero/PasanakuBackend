import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { TAMANO_DE_LOTE } from './dominio/lotes-por-cursor'

/**
 * Lo que dice un listado de publicidad sobre sus lotes: cuántas filas trajo y, si el servidor avisó que hay más,
 * «Cargar 100 más». Presentacional: quien la aloja tiene los lotes. Un lote que falla no borra lo ya cargado: se
 * dice acá y el mismo botón reintenta.
 */
@Component({
  selector: 'ap-barra-de-lotes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    @if (hayMas()) {
      <ap-boton icono="descargar" variante="fantasma" tamano="sm" [cargando]="cargando()" (pulsado)="cargarMas.emit()">Cargar {{ tamano }} más</ap-boton>
    }
    @if (error(); as e) { <span class="error" role="alert">{{ e }}</span> }
  `,
  styles: `
    :host { display: contents; }
    .error { color: var(--err-texto); font: var(--t-cuerpo-chico); }
  `,
})
export class BarraDeLotes {
  protected readonly tamano = TAMANO_DE_LOTE
  readonly hayMas = input(false)
  readonly cargando = input(false)
  readonly error = input<string | null>(null)
  readonly cargarMas = output<void>()
}
