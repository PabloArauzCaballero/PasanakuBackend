import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { PUERTO_WEBHOOKS, type Webhook } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-webhooks',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="webhooks" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="webhooks.reload()">
        @if (webhooks.hasValue() && webhooks.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'ultimoEstado') {
                <ap-chip-estado [tono]="fila.ultimoEstado === 'ok' ? 'ok' : 'error'">{{ fila.ultimoEstado }}</ap-chip-estado>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaWebhooks {
  protected readonly t = textosSistemas.webhooks
  private readonly puerto = inject(PUERTO_WEBHOOKS)
  protected readonly webhooks = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Webhook[]) => filas.length === 0
  protected readonly columnas: Columna<Webhook>[] = [
    { clave: 'origen', titulo: 'Origen', ordenable: true },
    { clave: 'direccion', titulo: 'Dirección' },
    { clave: 'ultimoEstado', titulo: 'Último estado' },
    { clave: 'ultimaEntregaEl', titulo: 'Última entrega' },
  ]
}
