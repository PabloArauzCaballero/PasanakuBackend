import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio, type ErrorParaVista } from '@aportaya/ui/error-de-envio/error-de-envio'
import { mensajeDeError } from '../compartido/carga'
import { actualizadorElementoCatalogo, type ElementoCatalogoOperativo } from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'

/**
 * Activar o desactivar un elemento de catálogo (PATCH `/auditoria/catalogos/{codigo}/elementos/{id}` con
 * `{activo, motivo}`). Se monta con `@if` y abre el `ap-dialogo` nativo: foco atrapado, `Escape` y vuelta del foco.
 * Antes leía `err.error?.mensaje` y perdía siempre el `ErrorTraducido`; ahora el rechazo deja el motivo escrito y
 * el mensaje traducido con su código de seguimiento.
 */
@Component({
  selector: 'ap-modal-cambiar-estado-elemento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, ChipEstado, Dialogo, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.cambiarEstadoTitulo" [textoDeConfirmar]="enviando() ? 'Guardando…' : (nuevoEstado() ? t.activar : t.desactivar) + ' elemento'" [destructivo]="!nuevoEstado()" [cargando]="enviando()" [confirmarDeshabilitado]="!esValido() || enviando()" [puedeDescartar]="puedeCerrar" [abierto]="true" (confirmar)="guardar()" (cancelar)="cerrar.emit()">
      <dl class="resumen">
        <div><dt>Elemento</dt><dd>{{ elemento().nombre }} <code>{{ elemento().codigo }}</code></dd></div>
        <div><dt>Nuevo estado</dt><dd><ap-chip-estado [tono]="nuevoEstado() ? 'ok' : 'neutro'">{{ nuevoEstado() ? 'Activo' : 'Inactivo' }}</ap-chip-estado></dd></div>
      </dl>
      <ap-area-de-texto [etiqueta]="t.motivoObligatorio" [(valor)]="motivo" [maximo]="500" [deshabilitado]="enviando()" ayuda="Mínimo 5 caracteres. Queda en la bitácora inmutable." />
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
  `,
  styles: `
    .resumen { display: grid; gap: var(--s2); margin: 0 0 var(--s3); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface-2); }
    .resumen div { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    dt { min-width: calc(var(--s7) * 2); font: var(--t-cuerpo-chico); color: var(--text-2); }
    dd { margin: 0; font: var(--t-cuerpo-chico); color: var(--text); overflow-wrap: anywhere; }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface); font-family: var(--f-mono); }
  `,
})
export class ModalCambiarEstadoElemento {
  readonly catalogoCodigo = input.required<string>()
  readonly elemento = input.required<ElementoCatalogoOperativo>()
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  protected readonly t = textosSistemas.catalogosOperativos
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorParaVista | null>(null)
  protected readonly puedeCerrar = () => !this.enviando()
  private readonly actualizar = actualizadorElementoCatalogo()

  protected nuevoEstado(): boolean {
    return !this.elemento().activo
  }

  protected esValido(): boolean {
    return this.motivo().trim().length >= 5
  }

  protected guardar(): void {
    if (!this.esValido() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    this.actualizar(this.catalogoCodigo(), this.elemento().id, { activo: this.nuevoEstado(), motivo: this.motivo().trim() }).subscribe({
      next: () => {
        this.enviando.set(false)
        this.guardado.emit()
      },
      error: (e: unknown) => {
        this.enviando.set(false)
        this.error.set(mensajeDeError(e, 'No se pudo actualizar el elemento. Probá de nuevo.'))
      },
    })
  }
}
