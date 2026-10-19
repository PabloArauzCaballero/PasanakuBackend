import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Monto } from '@aportaya/ui/monto/monto'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { PUERTO_PROVEEDORES, type Proveedor } from '../dominio/puertos'
import { textosSistemas } from '../textos'

@Component({
  selector: 'ap-pantalla-proveedores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, TablaDeDatos, Monto, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="grupo" />
      <ap-estado-de-pantalla [recurso]="proveedores" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="proveedores.reload()">
        @if (proveedores.hasValue() && proveedores.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'costoRealUltimoPeriodo') {
                <ap-monto [monto]="fila.costoRealUltimoPeriodo" [moneda]="fila.moneda" [etiqueta]="'Costo real de ' + fila.nombre" />
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
export class PantallaProveedores {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.proveedores
  private readonly puerto = inject(PUERTO_PROVEEDORES)
  protected readonly proveedores = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: Proveedor[]) => filas.length === 0
  protected readonly columnas: Columna<Proveedor>[] = [
    { clave: 'nombre', titulo: 'Proveedor', ordenable: true },
    { clave: 'categoria', titulo: 'Categoría' },
    { clave: 'costoRealUltimoPeriodo', titulo: 'Costo real (último período)', numerica: true },
  ]
}
