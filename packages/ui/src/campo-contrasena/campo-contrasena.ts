import { ChangeDetectionStrategy, Component, input, model, signal } from '@angular/core'
import { Campo } from '../campo/campo'
import { Icono } from '../icono/icono'

/** Contraseña con «ver». El botón dice lo que hará, y el campo nunca se autocompleta con la actual. */
@Component({
  selector: 'ap-campo-contrasena',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, Icono],
  template: `
    <ap-campo [etiqueta]="etiqueta()" [(valor)]="valor" [tipo]="visible() ? 'text' : 'password'" [autocompletar]="autocompletar()" [ayuda]="ayuda()" [error]="error()">
      <button sufijo type="button" [attr.aria-label]="visible() ? 'Ocultar la contraseña' : 'Mostrar la contraseña'" [attr.aria-pressed]="visible()" (click)="visible.set(!visible())">
        <ap-icono [nombre]="visible() ? 'ojoCerrado' : 'ojo'" tamano="s5" />
      </button>
    </ap-campo>
  `,
  styles: `
    button { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); margin-right: calc(var(--s3) * -1); border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-2); cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); }
  `,
})
export class CampoContrasena {
  readonly etiqueta = input('Contraseña')
  readonly valor = model('')
  readonly autocompletar = input<'current-password' | 'new-password'>('current-password')
  readonly ayuda = input<string>()
  readonly error = input<string>()
  readonly visible = signal(false)
}
