import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { TablaDeOrdenes } from './tabla-de-ordenes'
import { TablaDeFacturas } from './tabla-de-facturas'
import { textosContabilidad } from '../textos'

const PESTANAS: Pestana[] = [
  { valor: 'ordenes', texto: 'Órdenes de compra' },
  { valor: 'facturas', texto: 'Facturas de proveedor' },
]

/**
 * CU-102 y CU-103 en una sola ruta (`contabilidad/compras`, como la fija
 * `docs/Flujo de pantallas · backoffice administrador` §5): primero la orden autorizada,
 * después la factura que de ella nace. Cada una es un organismo con su propio archivo.
 *
 * **Por qué pestañas y no las dos tablas una debajo de la otra.** Es una sola tarjeta de trabajo (regla del cliente:
 * pestañas donde cabe una) y hay un solo listado montado por vez: una sola barra de filtros y una sola paginación
 * en el DOM, sin landmarks repetidos.
 */
@Component({
  selector: 'ap-pantalla-de-compras',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, Pestanas, TablaDeOrdenes, TablaDeFacturas],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="recibo" />
      <ap-alerta tono="info" titulo="Registrar no es pagar">{{ t.segregacionGeneral }}</ap-alerta>
      <ap-pestanas etiqueta="Compras y cuentas por pagar" [pestanas]="PESTANAS" [(elegida)]="pestana" />
      <div role="tabpanel" [id]="'panel-' + pestana()" [attr.aria-labelledby]="'pestana-' + pestana()">
        @if (pestana() === 'ordenes') {
          <ap-tabla-de-ordenes />
        } @else {
          <ap-tabla-de-facturas />
        }
      </div>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    [role='tabpanel'] { min-width: 0; }
  `,
})
export class PantallaDeCompras {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosContabilidad.compras
  protected readonly PESTANAS = PESTANAS
  protected readonly pestana = signal('ordenes')
}
