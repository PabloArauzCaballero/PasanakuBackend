import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { PUERTO_WEBHOOKS, type Webhook } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-webhooks',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="qr" />
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
    </section>
  `,
  styles: `.pantalla { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s4); }`,
})
export class PantallaWebhooks {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

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
