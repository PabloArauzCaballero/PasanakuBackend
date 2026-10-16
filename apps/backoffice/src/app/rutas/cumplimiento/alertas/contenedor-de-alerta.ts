import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Indisponible } from '../compartido/indisponible'
import { textosCumplimiento } from '../textos'

/**
 * CU-97 · alertas de riesgo temprano de un grupo. `cumplimiento.yaml` no publica ninguna
 * operación para consultarlas (ver `dominio/cu97-alertas.ts`). Antes este contenedor armaba
 * una alerta de ejemplo a partir del id de la URL; el plan B4 exige no mostrar datos de
 * ejemplo como reales, así que dice que la consulta no está disponible. `PantallaDeAlertas`
 * queda lista para el día que el contrato exista: solo este archivo cambia.
 */
@Component({
  selector: 'ap-contenedor-de-alerta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Indisponible],
  template: `<ap-indisponible [titulo]="t.titulo" [proposito]="t.proposito" [motivo]="t.indisponible" destino="/cumplimiento/alertas" [textoDestino]="t.irALaBandeja" />`,
})
export class ContenedorDeAlerta {
  readonly alertaId = input<string>()
  protected readonly t = textosCumplimiento.alertas
}
