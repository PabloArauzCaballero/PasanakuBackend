import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { EstadoDeBandeja } from './bandeja'

/**
 * Lo que la tabla no dice sola: vacío (con su porqué), sin permiso y error accionable.
 * «Cargando» lo anuncia la propia tabla; con datos, esto no dibuja nada.
 */
@Component({
  selector: 'ap-estado-de-bandeja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, EstadoVacio],
  template: `
    @switch (estado()) {
      @case ('vacio') {
        <ap-estado-vacio [titulo]="textoVacio()" [motivo]="conFiltros() ? 'porFiltro' : 'sinDatos'" [explicacion]="conFiltros() ? undefined : explicacionVacio()" (actuar)="quitarFiltros.emit()" />
      }
      @case ('denegado') {
        <ap-estado-vacio titulo="No tenés acceso a esta bandeja." motivo="porPermiso" explicacion="Tu rol no puede ver esta información. Si la necesitás, pedí el permiso a quien administra los accesos." />
      }
      @case ('error') {
        <ap-alerta tono="error" titulo="No pudimos cargar la bandeja">
          {{ error()?.mensaje }}
          @if (error()?.trazaId) { <span class="traza">Código de seguimiento: {{ error()?.trazaId }}</span> }
          <span class="accion"><ap-boton tamano="sm" (pulsado)="reintentar.emit()">Reintentar</ap-boton></span>
        </ap-alerta>
      }
    }
  `,
  styles: `
    :host { display: block; }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
    .accion { display: block; margin-top: var(--s2); }
  `,
})
export class EstadoDeBandejaVista {
  readonly estado = input.required<EstadoDeBandeja>()
  readonly error = input<ErrorTraducido | null>(null)
  readonly textoVacio = input.required<string>()
  readonly explicacionVacio = input<string>()
  readonly conFiltros = input(false)
  readonly reintentar = output<void>()
  readonly quitarFiltros = output<void>()
}
