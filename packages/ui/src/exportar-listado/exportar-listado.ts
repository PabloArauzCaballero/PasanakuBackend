import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Icono } from '../icono/icono'
import { aCsv, aJson, descargar, nombreDeArchivo, type ColumnaExportable } from './exportar'

/**
 * «Descargar CSV» y «Descargar JSON» de las filas **filtradas** de un listado (todas las páginas, no solo la
 * visible). Con cero filas igual baja el archivo con el encabezado: el vacío también es una respuesta. El
 * botón dice cuántas filas van a salir, para que nadie descargue «lo que ve» creyendo que es todo.
 */
@Component({
  selector: 'ap-exportar-listado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { role: 'group', '[attr.aria-label]': '"Descargar " + nombre()' },
  template: `
    <button type="button" [disabled]="deshabilitado()" (click)="bajar('csv')"><ap-icono nombre="archivoCsv" tamano="s4" /><span>Descargar CSV</span></button>
    @if (conJson()) {
      <button type="button" [disabled]="deshabilitado()" (click)="bajar('json')"><ap-icono nombre="archivoJson" tamano="s4" /><span>Descargar JSON</span></button>
    }
    <span class="cuenta" aria-live="polite">{{ filas().length }} {{ filas().length === 1 ? 'fila' : 'filas' }}</span>
  `,
  styles: `
    :host { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    button { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: var(--t-cuerpo-chico); font-weight: 600; cursor: pointer; transition: background-color var(--mov-micro) ease-out; }
    button:hover:not(:disabled) { background: var(--surface-2); }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .cuenta { color: var(--text-3); font: var(--t-cuerpo-chico); }
  `,
})
export class ExportarListado<T> {
  /** Qué se descarga, en palabras: «retiros en revisión». Da el nombre del archivo. */
  readonly nombre = input.required<string>()
  readonly columnas = input.required<readonly ColumnaExportable<T>[]>()
  readonly filas = input.required<readonly T[]>()
  readonly conJson = input(true)
  /** Mientras carga o con error no hay «lo filtrado» que bajar. */
  readonly deshabilitado = input(false)
  readonly descargado = output<{ formato: 'csv' | 'json'; filas: number }>()
  private readonly base = computed(() => this.nombre())

  bajar(formato: 'csv' | 'json'): void {
    const contenido = formato === 'csv' ? aCsv(this.columnas(), this.filas()) : aJson(this.columnas(), this.filas())
    const tipo = formato === 'csv' ? 'text/csv;charset=utf-8' : 'application/json;charset=utf-8'
    if (descargar(contenido, nombreDeArchivo(this.base(), formato), tipo)) this.descargado.emit({ formato, filas: this.filas().length })
  }
}
