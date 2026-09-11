import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Monto } from '@aportaya/ui/monto/monto'
import { Proveedor, proveedoresSimulados } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-proveedores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, Monto],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'costoRealUltimoPeriodo') {
            <ap-monto [monto]="fila.costoRealUltimoPeriodo" [moneda]="fila.moneda" [etiqueta]="'Costo real de ' + fila.nombre" />
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaProveedores {
  protected readonly t = textosSistemas.proveedores
  protected readonly filas = proveedoresSimulados
  protected readonly columnas: Columna<Proveedor>[] = [
    { clave: 'nombre', titulo: 'Proveedor', ordenable: true },
    { clave: 'categoria', titulo: 'Categoría' },
    { clave: 'costoRealUltimoPeriodo', titulo: 'Costo real (último período)', numerica: true },
  ]
}
