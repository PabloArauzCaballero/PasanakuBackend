import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
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
 * **Por qué pestañas y no las dos tablas una debajo de la otra.** `TablaDeDatosVirtualizada`
 * trae su `Paginacion`, que es un landmark `navigation` con nombre fijo («Paginación»):
 * dos tablas a la vez en el DOM dan dos landmarks con el mismo nombre accesible, y
 * `axe` lo marca como violación `landmark-unique` — un lector de pantalla no sabría cuál
 * de las dos paginaciones está recorriendo. Con pestañas hay una sola tabla montada por
 * vez y el problema desaparece sin tocar el shell. Queda anotado como hueco de
 * `packages/ui`/`nucleo` en `planes/informes/carril-B3.md`.
 */
@Component({
  selector: 'ap-pantalla-de-compras',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, BandaDeProposito, Pestanas, TablaDeOrdenes, TablaDeFacturas],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-alerta tono="info" titulo="Registrar no es pagar">{{ t.segregacion }}</ap-alerta>
      <ap-pestanas etiqueta="Compras y cuentas por pagar" [pestanas]="PESTANAS" [(elegida)]="pestana" />
      <div role="tabpanel" [id]="'panel-' + pestana()" [attr.aria-labelledby]="'pestana-' + pestana()">
        @if (pestana() === 'ordenes') {
          <ap-tabla-de-ordenes />
        } @else {
          <ap-tabla-de-facturas />
        }
      </div>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 80rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
  `,
})
export class PantallaDeCompras {
  protected readonly t = textosContabilidad.compras
  protected readonly PESTANAS = PESTANAS
  protected readonly pestana = signal('ordenes')
}
