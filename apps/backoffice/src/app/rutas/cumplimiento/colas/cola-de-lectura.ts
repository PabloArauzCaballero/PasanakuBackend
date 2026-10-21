import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core'
import { ChipsDeFiltro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { Monto } from '@aportaya/ui/monto/monto'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { CargadorDePagina, ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { filtrosDe, observarBandeja } from '../compartido/bandeja'
import type { Opcion } from '../compartido/campos'
import { EstadoDeBandejaVista } from '../compartido/estado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'

/** Un importe del contrato (`Dinero`): cadena decimal + moneda. Se dibuja con `ap-monto`, nunca formateado a mano. */
export type Importe = { monto: string; moneda: string }
export type Celda = string | Importe

const esImporte = (c: Celda): c is Importe => typeof c === 'object' && c !== null && 'moneda' in c

/**
 * Cola de solo lectura por cursor con sus cinco estados. Cada pantalla trae columnas, cargador
 * y cómo se lee cada celda (`celda`); no hay acciones acá —quien necesita una (registrar una
 * prueba) la proyecta en `[acciones]` y llama `recargar()` al terminar—.
 */
@Component({
  selector: 'ap-cola-de-lectura',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, EstadoDeBandejaVista, TablaDeDatosVirtualizada, ChipsDeFiltro, Monto],
  template: `
    <ap-marco-de-cumplimiento [titulo]="titulo()" [proposito]="proposito()">
      <div acciones><ng-content select="[acciones]" /></div>
      @if (filtrosDeEstado().length) {
        <ap-chips-de-filtro [etiqueta]="etiquetaFiltro()" [filtros]="filtrosDeEstado()" [(elegidos)]="estados" />
      }
      <ng-content />
      <ap-estado-de-bandeja [estado]="bandeja().estado()" [error]="bandeja().error()" [textoVacio]="vacio()" [conFiltros]="estados().length > 0" (reintentar)="recargar()" (quitarFiltros)="estados.set([])" />
      @for (m of [bandeja().montaje()]; track m) {
        <div [hidden]="bandeja().estado() === 'denegado' || bandeja().estado() === 'vacio'">
          <ap-tabla-de-datos-virtualizada [titulo]="tabla()" [columnas]="columnas()" [cargador]="bandeja().cargador" [ordenPermitido]="[]" [identidad]="identidad()" [filtros]="filtros()" [tamanoDePagina]="25">
            <ng-template #celda let-f let-columna="columna">
              @if (importe(f, columna.clave); as i) { <ap-monto [monto]="i.monto" [moneda]="i.moneda" /> }
              @else { {{ texto(f, columna.clave) }} }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        </div>
      }
    </ap-marco-de-cumplimiento>
  `,
})
export class ColaDeLectura<T extends object> {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
  readonly tabla = input.required<string>()
  readonly vacio = input.required<string>()
  readonly columnas = input.required<ColumnaVirtual<T>[]>()
  readonly cargador = input.required<CargadorDePagina<T>>()
  readonly identidad = input.required<(fila: T) => string>()
  readonly celda = input.required<(fila: T, clave: string) => Celda>()
  readonly filtrosDeEstado = input<Opcion[]>([])
  readonly etiquetaFiltro = input('Filtrar por estado')

  protected readonly estados = signal<string[]>([])
  protected readonly filtros = computed(() => filtrosDe({ estados: this.estados() }))
  protected readonly bandeja = computed(() => observarBandeja(this.cargador()))

  recargar(): void {
    this.bandeja().recargar()
  }

  protected importe(fila: T, clave: string): Importe | null {
    const c = this.celda()(fila, clave)
    return esImporte(c) ? c : null
  }

  protected texto(fila: T, clave: string): string {
    const c = this.celda()(fila, clave)
    return esImporte(c) ? '' : c
  }
}
