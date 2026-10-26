import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Bandeja } from '../compartido/bandeja-por-cursor'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'

/** Un importe del contrato (`Dinero`): cadena decimal + moneda. Se dibuja con `ap-monto`, nunca formateado a mano. */
export type Importe = { monto: string; moneda: string }
export type Celda = string | Importe

const esImporte = (c: Celda): c is Importe => typeof c === 'object' && c !== null && 'moneda' in c

/** El texto de una celda tal como se lee: el importe va con su moneda al lado, nunca solo el número. */
export const textoDeCelda = (c: Celda): string => (esImporte(c) ? `${c.monto} ${c.moneda}` : c)

/** El buscador y el archivo leen lo mismo que la tabla muestra: una sola función de celda para los tres. */
export function camposDeCelda<T>(columnas: readonly Columna<T>[], celda: (f: T, clave: string) => Celda): ((f: T) => string)[] {
  return columnas.map((c) => (f: T) => textoDeCelda(celda(f, c.clave)))
}

export function exportablesDeCelda<T>(columnas: readonly Columna<T>[], celda: (f: T, clave: string) => Celda): ColumnaExportable<T>[] {
  return columnas.map((c) => ({ clave: c.clave, titulo: c.titulo, valor: (f: T) => textoDeCelda(celda(f, c.clave)) }))
}

/**
 * Cola de solo lectura por cursor con la anatomía del kit (buscador, filtros, CSV/JSON, paginación y estados). Cada
 * pantalla trae su bandeja (`crearBandeja`), columnas y cómo se lee cada celda; no hay acciones de fila —son
 * registros de expediente y el contrato no tiene editar ni eliminar—. Quien necesita una acción de pantalla (registrar
 * una prueba) la proyecta con `acciones` y llama `bandeja.recargar()` al terminar.
 */
@Component({
  selector: 'ap-cola-de-lectura',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, Monto],
  template: `
    <ap-marco-de-cumplimiento [titulo]="titulo()" [proposito]="proposito()" [icono]="icono()">
      <div acciones class="acciones"><ng-content select="[acciones]" /></div>
      <ng-content />
      <ap-listado-de-bandeja [titulo]="tabla()" [columnas]="columnas()" [bandeja]="bandeja()" [identidad]="identidad()" [exportables]="exportables()" [nombreDeRegistros]="nombreDeRegistros()" [textoVacio]="vacio()" [ayudaDeBusqueda]="ayudaDeBusqueda()">
        <ng-template #celda let-f let-columna="columna">
          @if (importe(f, columna.clave); as i) { <ap-monto [monto]="i.monto" [moneda]="i.moneda" /> }
          @else { {{ texto(f, columna.clave) }} }
        </ng-template>
      </ap-listado-de-bandeja>
    </ap-marco-de-cumplimiento>
  `,
  styles: `.acciones { display: flex; flex-wrap: wrap; gap: var(--s2); }`,
})
export class ColaDeLectura<T extends object> {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
  readonly icono = input<NombreDeIcono>('lista')
  readonly tabla = input.required<string>()
  readonly vacio = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  readonly bandeja = input.required<Bandeja<T>>()
  readonly identidad = input.required<(fila: T) => string>()
  readonly celda = input.required<(fila: T, clave: string) => Celda>()
  readonly nombreDeRegistros = input('registros')
  readonly ayudaDeBusqueda = input<string>()
  protected readonly exportables = computed(() => exportablesDeCelda(this.columnas(), this.celda()))

  recargar(): void {
    void this.bandeja().recargar()
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
