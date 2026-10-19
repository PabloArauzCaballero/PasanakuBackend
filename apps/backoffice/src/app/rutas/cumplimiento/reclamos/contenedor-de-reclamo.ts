import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Indisponible } from '../compartido/indisponible'
import { textosCumplimiento } from '../textos'

/**
 * CU-52 · un reclamo del consumidor. La atención de reclamos (bandeja, ficha y respuesta,
 * `GET /reclamos`, `GET /reclamos/{id}`, `POST /reclamos/{id}/respuesta`) vive en
 * `operacion/reclamos`, con el permiso RECLAMO_VER / RECLAMO_ATENDER. Antes este contenedor
 * armaba un reclamo de ejemplo y su botón no enviaba nada: ahora dice adónde ir, para no tener
 * dos lugares que respondan el mismo reclamo.
 */
@Component({
  selector: 'ap-contenedor-de-reclamo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Indisponible],
  template: `<ap-indisponible [titulo]="t.titulo" [proposito]="t.proposito" [motivo]="t.indisponible" destino="/operacion/reclamos" [textoDestino]="t.irAOperacion" />`,
})
export class ContenedorDeReclamo {
  readonly reclamoId = input<string>()
  protected readonly t = textosCumplimiento.reclamos
}
