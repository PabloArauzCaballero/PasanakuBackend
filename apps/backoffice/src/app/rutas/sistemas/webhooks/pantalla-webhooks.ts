import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Webhook, webhooksSimulados } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-webhooks',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'ultimoEstado') {
            <ap-chip-estado [tono]="fila.ultimoEstado === 'ok' ? 'ok' : 'error'">{{ fila.ultimoEstado }}</ap-chip-estado>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaWebhooks {
  protected readonly t = textosSistemas.webhooks
  protected readonly filas = webhooksSimulados
  protected readonly columnas: Columna<Webhook>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'direccion', titulo: 'Dirección' },
    { clave: 'ultimoEstado', titulo: 'Último estado' },
    { clave: 'ultimaEntregaEl', titulo: 'Última entrega' },
  ]
}
