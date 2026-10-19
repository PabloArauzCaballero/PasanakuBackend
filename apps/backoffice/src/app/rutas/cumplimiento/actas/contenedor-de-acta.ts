import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Indisponible } from '../compartido/indisponible'
import { textosCumplimiento } from '../textos'

/**
 * CU-94 · el detalle de un acta (asistentes, votos, quórum). `cumplimiento.yaml` publica solo la
 * LISTA de actas (`listarActasDeComite`: número, fecha, quórum y hash), no el detalle ni el
 * cierre. Antes este contenedor armaba un acta de ejemplo; ahora dice que no está disponible y
 * lleva a la lista real. `PantallaDeActa` queda lista para cuando exista la operación.
 */
@Component({
  selector: 'ap-contenedor-de-acta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Indisponible],
  template: `<ap-indisponible [titulo]="t.titulo" [proposito]="t.proposito" [motivo]="t.indisponible" destino="/cumplimiento/actas" [textoDestino]="t.irALaLista" />`,
})
export class ContenedorDeActa {
  readonly actaId = input<string>()
  protected readonly t = textosCumplimiento.actas
}
