import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CodigoCopiable } from '@aportaya/ui/codigo-copiable/codigo-copiable'
import { CodigoQR } from '@aportaya/ui/codigo-qr/codigo-qr'
import type { SalidaInicioEnrolamientoTotp } from '../dominio/cuenta'

let secuencia = 0

/**
 * El paso «activá tu autenticador»: genera el factor (`iniciarFactorDeInvitacion`), muestra el QR de `uriOtpauth` y la semilla en
 * texto para quien no puede escanear. La semilla se muestra **una sola vez**: el servidor no la vuelve a dar, y la pantalla lo dice.
 * Presentacional: quien lo aloja decide cuándo pedir el factor y qué hacer con el error.
 */
@Component({
  selector: 'ap-enrolamiento-totp',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CodigoCopiable, CodigoQR],
  template: `
    <fieldset [attr.aria-describedby]="error() ? idError : null">
      <legend>Activá tu aplicación de autenticación</legend>
      @if (!factor()) {
        <p class="texto">Vas a necesitar una aplicación de autenticación (como Google Authenticator, Microsoft Authenticator o Authy) en tu teléfono.</p>
        <ap-boton variante="secundario" icono="candado" [cargando]="cargando()" [deshabilitado]="cargando()" (pulsado)="iniciar.emit()">Generar mi código QR</ap-boton>
      } @else {
        <ol class="pasos">
          <li>Abrí tu aplicación de autenticación y elegí «Escanear código QR».</li>
          <li>Escaneá este código. Si no podés, ingresá la clave de abajo a mano.</li>
        </ol>
        <div class="qr"><ap-codigo-qr [contenido]="factor()!.uriOtpauth" etiqueta="Código QR para registrar tu aplicación de autenticación" /></div>
        <div class="semilla">
          <span class="rotulo" id="rotulo-semilla">Clave para ingresar a mano</span>
          <ap-codigo-copiable [valor]="factor()!.semilla" nombre="la clave del autenticador" />
        </div>
        <ap-alerta tono="aviso">Esta clave se muestra una sola vez. Si la perdés antes de terminar, generá un código nuevo.</ap-alerta>
        <ap-boton variante="enlace" [cargando]="cargando()" [deshabilitado]="cargando()" (pulsado)="iniciar.emit()">Generar un código nuevo</ap-boton>
      }
      @if (error()) { <p class="error" [id]="idError" role="alert">{{ error() }}</p> }
    </fieldset>
  `,
  styles: `
    fieldset { display: grid; gap: var(--s3); min-width: 0; margin: 0; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface-2); }
    legend { padding: 0 var(--s2); font-weight: 600; color: var(--text-2); }
    .texto, .pasos { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-2); }
    .pasos { padding-left: var(--s5); display: grid; gap: var(--s1); }
    .qr { display: flex; justify-content: center; }
    .semilla { display: grid; gap: var(--s1); min-width: 0; overflow-wrap: anywhere; }
    .rotulo { font: var(--t-etiqueta); color: var(--text-2); }
    .error { margin: 0; font: var(--t-cuerpo-chico); color: var(--err-texto); }
  `,
})
export class EnrolamientoTotp {
  readonly idError = `ap-enrolamiento-error-${++secuencia}`
  readonly factor = input<SalidaInicioEnrolamientoTotp | null>(null)
  readonly cargando = input(false)
  readonly error = input<string>()
  readonly iniciar = output<void>()
}
