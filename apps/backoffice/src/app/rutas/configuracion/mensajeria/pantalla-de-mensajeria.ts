import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { filtroInicialDeLaUrl, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { textosConfiguracion } from '../textos'
import { TablaDeEnvios } from './tabla-de-envios'
import { TablaDePlantillas } from './tabla-de-plantillas'

const PESTANAS: Pestana[] = [
  { valor: 'plantillas', texto: 'Plantillas' },
  { valor: 'envios', texto: 'Envíos' },
]

/**
 * CU-80 / CU-83 · Mensajería de solo lectura: qué plantilla rige cada mensaje y la auditoría de envíos, en **una
 * tarjeta con pestañas** (un solo listado montado por vez: una barra de filtros y una paginación en el DOM). Un enlace
 * con `?filtro=` (lo que se audita: fallidos, expirados) abre directo en Envíos, ya filtrado.
 */
@Component({
  selector: 'ap-pantalla-de-mensajeria',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, Pestanas, TablaDeEnvios, TablaDePlantillas],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Configuración · CU-80 y CU-83" [titulo]="t.titulo" [descripcion]="t.proposito" icono="mensaje" />
      <ap-alerta tono="info">{{ comun.soloLectura }} No se muestran destinatarios ni contenido.</ap-alerta>
      <ap-pestanas etiqueta="Mensajería" [pestanas]="PESTANAS" [(elegida)]="pestana" />
      <div role="tabpanel" [id]="'panel-' + pestana()" [attr.aria-labelledby]="'pestana-' + pestana()">
        @if (pestana() === 'plantillas') { <ap-tabla-de-plantillas /> } @else { <ap-tabla-de-envios /> }
      </div>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    [role='tabpanel'] { min-width: 0; }
  `,
})
export class PantallaDeMensajeria {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosConfiguracion.mensajeria
  protected readonly comun = textosConfiguracion.comun
  protected readonly PESTANAS = PESTANAS
  protected readonly pestana = signal(filtroInicialDeLaUrl(opcionesDe(textosConfiguracion.mensajeria.estadosEnvio)) ? 'envios' : 'plantillas')
}
