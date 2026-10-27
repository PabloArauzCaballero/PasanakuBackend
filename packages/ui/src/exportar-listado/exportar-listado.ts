import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Icono } from '../icono/icono'
import { aCsv, aJson, descargar, nombreDeArchivo, type ColumnaExportable } from './exportar'

/**
 * «Descargar CSV» y «Descargar JSON» de las filas **filtradas** de un listado (todas las páginas, no solo la
 * visible). Con cero filas igual baja el archivo con el encabezado: el vacío también es una respuesta. Cuántas filas
 * van a salir lo dicen el `title` de cada botón y un texto para lector de pantalla: a la vista ya está el resumen del
 * listado, y un tercer contador al lado de los botones era ruido (2026-10-27).
 */
@Component({
  selector: 'ap-exportar-listado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { role: 'group', '[attr.aria-label]': '"Descargar " + nombre()' },
  template: `
    <button type="button" [disabled]="deshabilitado()" [title]="'Descargar ' + cuenta() + ' en CSV'" (click)="bajar('csv')"><ap-icono nombre="descargar" tamano="s4" /><span>Descargar CSV</span></button>
    @if (conJson()) {
      <button type="button" [disabled]="deshabilitado()" [title]="'Descargar ' + cuenta() + ' en JSON'" (click)="bajar('json')"><ap-icono nombre="descargar" tamano="s4" /><span>Descargar JSON</span></button>
    }
    <span class="cuenta" aria-live="polite">{{ cuenta() }}</span>
  `,
  styles: `
    :host { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--s1); }
    button { display: inline-flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: 0; border-radius: var(--r-md); background: transparent; color: var(--text-2); font: var(--t-cuerpo-chico); font-weight: 500; cursor: pointer; transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    button:hover:not(:disabled) { background: var(--surface-2); color: var(--text); }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .cuenta { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
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
  protected readonly cuenta = computed(() => `${this.filas().length} ${this.filas().length === 1 ? 'fila' : 'filas'}`)

  bajar(formato: 'csv' | 'json'): void {
    const contenido = formato === 'csv' ? aCsv(this.columnas(), this.filas()) : aJson(this.columnas(), this.filas())
    const tipo = formato === 'csv' ? 'text/csv;charset=utf-8' : 'application/json;charset=utf-8'
    if (descargar(contenido, nombreDeArchivo(this.base(), formato), tipo)) this.descargado.emit({ formato, filas: this.filas().length })
  }
}
