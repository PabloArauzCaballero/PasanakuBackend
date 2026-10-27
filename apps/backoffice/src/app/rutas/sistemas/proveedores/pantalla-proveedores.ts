import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { PUERTO_PROVEEDORES, type Proveedor } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/**
 * Proveedores externos con su costo real del último período. El costo es texto decimal con su moneda al lado
 * (regla 91.1.1/2): nunca se suma entre monedas, por eso las cifras de arriba cuentan proveedores y no importes.
 */
@Component({
  selector: 'ap-pantalla-proveedores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, CifrasDeSistemas, ListadoDeDatos, Monto],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito" icono="enlace">
        <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="proveedores.isLoading()" (pulsado)="proveedores.reload()">Actualizar</ap-boton>
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de proveedores" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por nombre del proveedor, categoría o moneda." [exportables]="exportables" nombreDeRegistros="proveedores" [textoVacio]="t.vacio" explicacionVacio="Cuando se registre un proveedor externo, aparece acá con su costo real." (reintentar)="proveedores.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'costoRealUltimoPeriodo') {
            <ap-monto [monto]="f.costoRealUltimoPeriodo" [moneda]="f.moneda" [etiqueta]="'Costo real de ' + f.nombre" />
          } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaProveedores {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.proveedores
  private readonly puerto = inject(PUERTO_PROVEEDORES)
  protected readonly proveedores = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.proveedores)
  private readonly filas = filasDe<Proveedor>(this.proveedores)
  protected readonly listado = crearListado<Proveedor>({
    filas: this.filas,
    camposDeBusqueda: ['nombre', 'categoria', 'moneda'],
    filtros: [
      { nombre: 'categoria', etiqueta: 'Categoría', valor: (p) => p.categoria },
      { nombre: 'moneda', etiqueta: 'Moneda', valor: (p) => p.moneda },
    ],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Proveedores', valor: contar(f), icono: 'enlace' },
      { etiqueta: 'Categorías', valor: String(new Set(f.map((p) => p.categoria)).size), icono: 'etiqueta' },
      { etiqueta: 'Monedas', valor: String(new Set(f.map((p) => p.moneda)).size), icono: 'moneda', pista: 'Los costos no se suman entre monedas.' },
    ]
  })
  protected readonly columnas: Columna<Proveedor>[] = [
    { clave: 'nombre', titulo: 'Proveedor', ordenable: true },
    { clave: 'categoria', titulo: 'Categoría', ordenable: true },
    { clave: 'costoRealUltimoPeriodo', titulo: 'Costo real (último período)', numerica: true, ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<Proveedor>[] = [
    { clave: 'nombre', titulo: 'Proveedor' },
    { clave: 'categoria', titulo: 'Categoría' },
    { clave: 'costoRealUltimoPeriodo', titulo: 'Costo real (último período)' },
    { clave: 'moneda', titulo: 'Moneda' },
  ]
}
