import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core'
import { DialogoDeConfirmacion } from '../dialogo-de-confirmacion/dialogo-de-confirmacion'
import { Icono } from '../icono/icono'

/**
 * Las acciones de una fila de tabla, siempre **ícono + nombre** (nunca un ícono solo): Ver, Editar y
 * Eliminar, cada una solo si quien aloja la tabla la pide — y solo debe pedirla si el contrato tiene la
 * operación detrás (un botón sin endpoint es un botón muerto). Eliminar nunca se ofrece sobre registros
 * append-only (asientos, bitácoras, movimientos: regla 91.1.3). Eliminar pregunta antes y recién después
 * emite. Las acciones de dominio (Revisar, Aprobar…) se proyectan entre Ver y Editar con el mismo aspecto
 * (`<button apAccionDeFila>` o cualquier contenido).
 *
 * El nombre accesible de cada botón dice **qué fila** («Editar Juana Pérez»): en una tabla hay N «Editar».
 */
@Component({
  selector: 'ap-acciones-de-fila',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DialogoDeConfirmacion, Icono],
  host: { role: 'group', '[attr.aria-label]': '"Acciones de " + fila()' },
  template: `
    @if (ver()) {
      <button type="button" class="accion" [attr.aria-label]="textoVer() + ' ' + fila()" (click)="verPulsado.emit()"><ap-icono nombre="ojo" tamano="s4" /><span>{{ textoVer() }}</span></button>
    }
    <ng-content />
    @if (editar()) {
      <button type="button" class="accion" [disabled]="ocupada()" [attr.aria-label]="textoEditar() + ' ' + fila()" (click)="editarPulsado.emit()"><ap-icono nombre="editar" tamano="s4" /><span>{{ textoEditar() }}</span></button>
    }
    @if (eliminar()) {
      <button type="button" class="accion peligro" [disabled]="ocupada()" [attr.aria-busy]="ocupada() || null" [attr.aria-label]="textoEliminar() + ' ' + fila()" (click)="preguntando.set(true)"><ap-icono nombre="eliminar" tamano="s4" /><span>{{ ocupada() ? textoEliminando() : textoEliminar() }}</span></button>
      <ap-dialogo-de-confirmacion [titulo]="textoEliminar() + ' ' + fila()" [descripcion]="descripcionEliminar()" [textoDeConfirmar]="textoEliminar()" [destructivo]="true" [(abierto)]="preguntando" (confirmar)="confirmado()" />
    }
  `,
  styles: `
    :host { display: inline-flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: var(--s1); }
    .accion, ::ng-deep [apAccionDeFila] { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); padding: 0 var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; white-space: nowrap; cursor: pointer; transition: background-color var(--mov-micro) ease-out, border-color var(--mov-micro) ease-out; }
    .accion:hover:not(:disabled) { background: var(--brand-bg); border-color: var(--brand); }
    .accion.peligro { color: var(--err-texto); }
    .accion.peligro:hover:not(:disabled) { background: var(--err-bg); border-color: var(--err); }
    .accion:disabled { opacity: .55; cursor: not-allowed; }
    .accion:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class AccionesDeFila {
  /** Cómo se llama la fila para un lector de pantalla: «Juana Pérez», «Retiro R-104». */
  readonly fila = input.required<string>()
  readonly ver = input(false)
  readonly editar = input(false)
  readonly eliminar = input(false)
  /** Mientras la eliminación viaja: los botones quedan quietos y Eliminar dice «Eliminando…». */
  readonly ocupada = input(false)
  readonly textoVer = input('Ver')
  readonly textoEditar = input('Editar')
  readonly textoEliminar = input('Eliminar')
  readonly textoEliminando = input('Eliminando…')
  readonly descripcionEliminar = input('Esta acción no se puede deshacer.')
  readonly verPulsado = output<void>()
  readonly editarPulsado = output<void>()
  /** Solo después de que la persona confirmó en el diálogo. */
  readonly eliminarConfirmado = output<void>()
  readonly preguntando = signal(false)

  confirmado(): void {
    this.preguntando.set(false)
    this.eliminarConfirmado.emit()
  }
}
