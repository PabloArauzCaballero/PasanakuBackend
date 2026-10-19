import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import type { ErrorDeLectura } from './dominio/listado'
import { textosConfiguracion } from './textos'

/**
 * Error y vacío de un listado de configuración, presentacional. La carga la pinta la propia tabla
 * (`TablaDeDatosVirtualizada`); el error 403 dice «no tenés acceso» y nada más (`nucleo/errores.ts`).
 */
@Component({
  selector: 'ap-aviso-de-listado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, EstadoVacio],
  template: `
    @if (error(); as e) {
      <ap-alerta tono="error" [titulo]="t.noSePudo">
        <p class="mensaje">{{ e.mensaje }}</p>
        @if (e.trazaId) { <p class="traza">{{ t.traza }}: {{ e.trazaId }}</p> }
      </ap-alerta>
      <ap-boton variante="primario" (pulsado)="reintentar.emit()">{{ t.reintentar }}</ap-boton>
    } @else if (vacio()) {
      <ap-estado-vacio [motivo]="conFiltro() ? 'porFiltro' : 'sinDatos'" [titulo]="conFiltro() ? t.vacioPorFiltro : mensajeVacio()" [explicacion]="explicacionVacio()" />
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); justify-items: start; }
    :host:empty { display: none; }
    .mensaje, .traza { margin: 0; }
    .traza { color: var(--text-2); font-size: .9em; }
  `,
})
export class AvisoDeListado {
  protected readonly t = textosConfiguracion.comun
  readonly error = input<ErrorDeLectura | null>(null)
  readonly vacio = input(false)
  readonly conFiltro = input(false)
  readonly mensajeVacio = input.required<string>()
  readonly explicacionVacio = input('')
  readonly reintentar = output<void>()
}
