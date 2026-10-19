import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal } from '@angular/core'
import { Campo } from '../campo/campo'
import { Dialogo } from '../dialogo/dialogo'
import { Icono } from '../icono/icono'

/**
 * La confirmación antes de una escritura, como en el portal de Atlas: ícono de advertencia,
 * título, qué va a pasar («Se actualizará la ficha de X (3 campos)») y, para lo irreversible, una
 * frase que hay que escribir igual antes de poder confirmar. Hereda del diálogo de la casa el
 * foco atrapado, `Escape`, el fondo inerte y la vuelta del foco.
 */
@Component({
  selector: 'ap-dialogo-de-confirmacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, Dialogo, Icono],
  template: `
    <ap-dialogo [titulo]="titulo()" [textoDeConfirmar]="textoDeConfirmar()" [textoDeCancelar]="textoDeCancelar()" [destructivo]="destructivo()" [cargando]="cargando()" [confirmarDeshabilitado]="!puedeConfirmar()" [(abierto)]="abierto" (confirmar)="confirmar.emit()" (cancelar)="cancelar.emit()">
      <div class="cuerpo">
        <span class="loseta" aria-hidden="true"><ap-icono nombre="advertencia" tamano="s5" /></span>
        <div class="textos">
          <p>{{ descripcion() }}</p>
          @if (fraseDeConfirmacion()) {
            <ap-campo [etiqueta]="'Escribí «' + fraseDeConfirmacion() + '» para confirmar'" [valor]="escrito()" (valorChange)="escrito.set($event)" autocompletar="off" />
          }
        </div>
      </div>
    </ap-dialogo>
  `,
  styles: `
    .cuerpo { display: flex; gap: var(--s3); align-items: flex-start; }
    .loseta { display: inline-flex; flex: none; align-items: center; justify-content: center; width: calc(var(--s6) + var(--s2)); height: calc(var(--s6) + var(--s2)); border-radius: var(--r-md); background: var(--warn-bg); color: var(--aviso-texto); }
    .textos { display: grid; gap: var(--s3); min-width: 0; flex: 1; }
    p { margin: 0; overflow-wrap: anywhere; }
  `,
})
export class DialogoDeConfirmacion {
  readonly titulo = input.required<string>()
  readonly descripcion = input.required<string>()
  readonly textoDeConfirmar = input('Confirmar')
  readonly textoDeCancelar = input('Cancelar')
  readonly destructivo = input(false)
  readonly cargando = input(false)
  /** Para lo irreversible: solo se puede confirmar escribiendo exactamente esta frase. */
  readonly fraseDeConfirmacion = input<string>()
  readonly abierto = model(false)
  readonly confirmar = output<void>()
  readonly cancelar = output<void>()
  readonly escrito = signal('')
  readonly puedeConfirmar = computed(() => !this.fraseDeConfirmacion() || this.escrito().trim() === this.fraseDeConfirmacion())

  constructor() {
    // Cada apertura arranca con la frase sin escribir: la confirmación anterior no vale para esta.
    effect(() => {
      if (this.abierto()) this.escrito.set('')
    })
  }
}
