import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoContrasena } from '@aportaya/ui/campo-contrasena/campo-contrasena'
import { CampoOTP } from '@aportaya/ui/campo-otp/campo-otp'
import { EntradaAutenticacionPlataformaEnum, FactorPresentadoTipoEnum } from 'clientes/angular/identidad'
import type { ErrorTraducido } from '../../nucleo/errores'
import { Sesion } from '../../nucleo/sesion'
import { crearAutenticar, huellaDeEstaPestana } from './dominio/cu04-autenticar'

type Paso = 'credencial' | 'codigo'

/** Lo que el contrato exige al teléfono: `^\+591\d{8}$`. */
const OCHO_DIGITOS = /^\d{8}$/

/**
 * CU-04 · El ingreso de un operador al backoffice.
 *
 * Dos pasos, porque así responde `identidad`: con teléfono y contraseña correctos
 * contesta `AP-CU04-03` («falta el factor»), y R-SEG-10 exige segundo factor a todo
 * operador sin excepción. El código se manda en un segundo `POST /sesiones` junto con
 * la credencial, que **vive solo en esta pantalla** y se borra al abrir la sesión.
 *
 * Nunca se distingue «el teléfono no existe» de «la contraseña no coincide»: el servidor
 * no lo dice y esta pantalla tampoco lo adivina.
 */
@Component({
  selector: 'ap-pantalla-de-ingreso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, CampoContrasena, CampoOTP],
  template: `
    <main>
      <section class="tarjeta" aria-labelledby="titulo-ingreso">
        <p class="marca">AportaYa · Backoffice</p>
        <h1 id="titulo-ingreso">{{ paso() === 'credencial' ? 'Ingresá con tu cuenta de operador' : 'Confirmá que sos vos' }}</h1>

        @if (error(); as e) {
          <ap-alerta tono="error">{{ e }}</ap-alerta>
        }

        <form (submit)="$event.preventDefault(); enviar()" novalidate>
          @if (paso() === 'credencial') {
            <ap-campo
              etiqueta="Teléfono"
              tipo="tel"
              prefijo="+591"
              marcador="71234567"
              [(valor)]="telefono"
              [error]="errorTelefono()"
            />
            <ap-campo-contrasena [(valor)]="credencial" />
          } @else {
            <p class="ayuda">Ingresá el código de 6 dígitos de tu segundo factor.</p>
            <ap-campo-otp [(valor)]="codigo" (completado)="enviar()" />
          }

          <ap-boton variante="primario" tipo="submit" [ancho]="true" [cargando]="enviando()" [deshabilitado]="enviando()">
            {{ paso() === 'credencial' ? 'Continuar' : 'Entrar' }}
          </ap-boton>

          @if (paso() === 'codigo') {
            <ap-boton variante="enlace" [ancho]="true" (pulsado)="volver()">Usar otra cuenta</ap-boton>
          }
        </form>
      </section>
    </main>
  `,
  styles: `
    main { min-height: 100dvh; display: grid; place-items: center; padding: var(--s5); background: var(--g100); }
    .tarjeta { width: 100%; max-width: 26rem; padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .marca { margin: 0 0 var(--s2); color: var(--brand-texto); font-weight: 600; }
    h1 { margin: 0 0 var(--s4); font-size: 1.35rem; color: var(--text); }
    form { display: grid; gap: var(--s4); margin-top: var(--s4); }
    .ayuda { margin: 0; color: var(--text-2); }
  `,
})
export class PantallaDeIngreso {
  private readonly autenticar = crearAutenticar()
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  private readonly huella = huellaDeEstaPestana()

  protected readonly paso = signal<Paso>('credencial')
  protected readonly telefono = signal('')
  protected readonly credencial = signal('')
  protected readonly codigo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<string | null>(null)
  protected readonly errorTelefono = signal<string | undefined>(undefined)

  protected enviar(): void {
    if (this.enviando()) return
    this.error.set(null)

    const digitos = this.telefono().replace(/\D/g, '').replace(/^591(?=\d{8}$)/, '')
    if (!OCHO_DIGITOS.test(digitos)) {
      this.errorTelefono.set('Son 8 dígitos, sin el +591.')
      return
    }
    this.errorTelefono.set(undefined)
    if (this.credencial().length < 8) {
      this.error.set('La contraseña tiene al menos 8 caracteres.')
      return
    }
    if (this.paso() === 'codigo' && this.codigo().length !== 6) {
      this.error.set('El código tiene 6 dígitos.')
      return
    }

    this.enviando.set(true)
    this.autenticar({
      telefonoE164: `+591${digitos}`,
      credencial: this.credencial(),
      huellaDispositivo: this.huella,
      plataforma: EntradaAutenticacionPlataformaEnum.Web,
      ...(this.paso() === 'codigo' ? { factor: { tipo: FactorPresentadoTipoEnum.Totp, valor: this.codigo() } } : {}),
    }).subscribe({
      next: (salida) => {
        this.enviando.set(false)
        if (salida.requiereFactorAdicional || !salida.tokenAcceso) {
          this.pedirCodigo()
          return
        }
        this.sesion.abrirConToken(salida.tokenAcceso)
        this.credencial.set('')
        this.codigo.set('')
        void this.router.navigateByUrl('/tablero')
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        if (e.codigo === 'AP-CU04-03') {
          this.pedirCodigo()
          return
        }
        if (this.paso() === 'codigo') this.codigo.set('')
        this.error.set(e.mensaje)
      },
    })
  }

  protected volver(): void {
    this.paso.set('credencial')
    this.credencial.set('')
    this.codigo.set('')
    this.error.set(null)
  }

  private pedirCodigo(): void {
    this.codigo.set('')
    this.paso.set('codigo')
  }
}
