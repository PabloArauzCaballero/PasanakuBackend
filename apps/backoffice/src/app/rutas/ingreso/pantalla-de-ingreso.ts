import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { CampoContrasena } from '@aportaya/ui/campo-contrasena/campo-contrasena'
import { CampoOTP } from '@aportaya/ui/campo-otp/campo-otp'
import { Icono } from '@aportaya/ui/icono/icono'
import { EntradaAutenticacionPlataformaEnum, FactorPresentadoTipoEnum } from 'clientes/angular/identidad'
import type { ErrorTraducido } from '../../nucleo/errores'
import { Sesion } from '../../nucleo/sesion'
import { crearAutenticar, huellaDeEstaPestana } from './dominio/cu04-autenticar'
import { leerIdentificador } from './dominio/identificador'
import { SimboloDeMarca } from '../../layout/simbolo-de-marca'
import { PanelDeMarca } from './panel-de-marca'

type Paso = 'credencial' | 'codigo'
const MENSAJE_CREDENCIAL = 'El correo, el teléfono o la contraseña no coinciden.'

/**
 * CU-04 · El ingreso de un operador al backoffice, con la anatomía del portal interno
 * de Atlas: panel de marca a la izquierda y tarjeta a la derecha.
 *
 * Dos pasos, porque así responde `identidad`: con la credencial correcta contesta
 * `AP-CU04-03` («falta el factor»), y R-SEG-10 exige segundo factor a todo operador
 * sin excepción. El código se manda en un segundo `POST /sesiones` junto con la
 * credencial, que **vive solo en esta pantalla** y se borra al abrir la sesión.
 *
 * Se entra con el correo **o** el teléfono (un solo campo). Nunca se distingue «esa
 * cuenta no existe» de «la contraseña no coincide»: el servidor no lo dice y esta
 * pantalla tampoco lo adivina.
 */
@Component({
  selector: 'ap-pantalla-de-ingreso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, CampoContrasena, CampoOTP, Icono, PanelDeMarca, SimboloDeMarca],
  template: `
    <main>
      <ap-panel-de-marca />

      <section class="lado" aria-labelledby="titulo-ingreso">
        <div class="tarjeta">
          <div class="cabecera">
            <ap-simbolo-de-marca class="simbolo" />
            <p class="ceja">{{ paso() === 'credencial' ? 'Acceso de operadores' : 'Segundo factor' }}</p>
            <h1 id="titulo-ingreso">{{ paso() === 'credencial' ? 'Bienvenido de vuelta' : 'Confirmá que sos vos' }}</h1>
            <p class="bajada">
              {{ paso() === 'credencial' ? 'Ingresá con tu correo o tu teléfono para entrar al backoffice.' : 'Ingresá el código de 6 dígitos de tu aplicación de autenticación.' }}
            </p>
          </div>

          @if (error(); as e) {
            <ap-alerta tono="error">{{ e }}</ap-alerta>
          }

          <form (submit)="$event.preventDefault(); enviar()" novalidate>
            @if (paso() === 'credencial') {
              <ap-campo
                etiqueta="Correo o teléfono"
                tipo="text"
                marcador="nombre@aportaya.bo o 71234567"
                autocompletar="username"
                ayuda="El correo o el teléfono con el que te dieron de alta."
                [(valor)]="identificador"
                [error]="errorIdentificador()"
              />
              <ap-campo-contrasena [(valor)]="credencial" [error]="errorCredencial()" />
            } @else {
              <ap-campo-otp [(valor)]="codigo" [error]="errorCodigo()" (completado)="enviar()" />
            }

            <ap-boton variante="primario" tipo="submit" [icono]="paso() === 'credencial' ? 'flechaDerecha' : 'candado'" [ancho]="true" [cargando]="enviando()" [deshabilitado]="enviando()">
              {{ paso() === 'credencial' ? 'Continuar' : 'Entrar' }}
            </ap-boton>

            @if (paso() === 'codigo') {
              <ap-boton variante="enlace" icono="flechaIzquierda" [ancho]="true" (pulsado)="volver()">Usar otra cuenta</ap-boton>
            }
          </form>

          <p class="nota">
            <ap-icono nombre="candado" tamano="s4" />
            La sesión se renueva con una cookie segura y el token nunca se guarda en el navegador. Todas las acciones quedan auditadas.
          </p>
        </div>
        <p class="aviso">Acceso únicamente para personal autorizado de AportaYa.</p>
      </section>
    </main>
  `,
  styles: `
    main { min-height: 100dvh; display: flex; background: var(--bg); }
    .lado {
      flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: var(--s4);
      padding: var(--s5) var(--s4);
      background: radial-gradient(circle at 85% 0%, color-mix(in srgb, var(--brand) 9%, transparent), transparent 45%);
    }
    .tarjeta {
      width: 100%; max-width: 27rem; display: grid; gap: var(--s4); padding: var(--s6) var(--s5);
      border: var(--borde-fino) solid var(--border); border-radius: var(--r-xl); background: var(--surface);
      box-shadow: var(--sombra-2); animation: aparecer .45s cubic-bezier(.2, .8, .2, 1) both;
    }
    .simbolo { --tamano-simbolo: 3rem; margin-bottom: var(--s3); }
    .ceja { margin: 0; font: var(--t-etiqueta); letter-spacing: .06em; text-transform: uppercase; color: var(--brand-texto); }
    h1 { margin: 0; font: var(--t-titulo-1); letter-spacing: -.01em; color: var(--text); }
    .bajada { margin: 0; font: var(--t-cuerpo); color: var(--text-2); }
    .cabecera { display: grid; gap: var(--s1); } form { display: grid; gap: var(--s4); }
    .nota {
      display: flex; gap: var(--s2); align-items: flex-start; margin: 0; padding-top: var(--s4);
      border-top: var(--borde-fino) solid var(--border); font: var(--t-cuerpo-chico); color: var(--text-3);
    }
    .nota ap-icono { margin-top: .1rem; color: var(--text-2); } .aviso { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-3); text-align: center; }
    @media (min-width: 64rem) { .simbolo { display: none; } .tarjeta { padding: var(--s7) var(--s6); } }
    @keyframes aparecer { from { opacity: 0; transform: translateY(.5rem); } } @media (prefers-reduced-motion: reduce) { .tarjeta { animation: none; } }
  `,
})
export class PantallaDeIngreso {
  private readonly autenticar = crearAutenticar()
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  private readonly route = inject(ActivatedRoute)
  private readonly huella = huellaDeEstaPestana()

  protected readonly paso = signal<Paso>('credencial')
  protected readonly identificador = signal('')
  protected readonly credencial = signal('')
  protected readonly codigo = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<string | null>(null)
  protected readonly errorIdentificador = signal<string | undefined>(undefined)
  // Cada error de forma va con su campo (95.3.1); el del servidor, que no dice qué dato falló, va en la alerta.
  protected readonly errorCredencial = signal<string | undefined>(undefined)
  protected readonly errorCodigo = signal<string | undefined>(undefined)

  protected enviar(): void {
    if (this.enviando()) return
    this.error.set(null)

    const lectura = leerIdentificador(this.identificador())
    this.errorIdentificador.set(lectura.ok ? undefined : lectura.error)
    this.errorCredencial.set(this.credencial().length < 8 ? 'La contraseña tiene al menos 8 caracteres.' : undefined)
    this.errorCodigo.set(this.paso() === 'codigo' && this.codigo().length !== 6 ? 'El código tiene 6 dígitos: completalo para entrar.' : undefined)
    if (!lectura.ok || this.errorCredencial() || this.errorCodigo()) return

    this.enviando.set(true)
    this.autenticar({
      ...lectura.valor,
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
        void this.router.navigateByUrl(this.destinoTrasIngreso())
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(false)
        if (e.codigo === 'AP-CU04-03') {
          this.pedirCodigo()
          return
        }
        if (this.paso() === 'codigo') this.codigo.set('')
        // Un 401 sin código en el ingreso no es «tu sesión venció»: es la credencial (sin decir cuál dato falló).
        this.error.set(e.estado === 401 && !e.codigo ? MENSAJE_CREDENCIAL : e.mensaje)
      },
    })
  }

  protected volver(): void {
    this.paso.set('credencial')
    this.credencial.set('')
    this.codigo.set('')
    this.error.set(null)
    this.errorCodigo.set(undefined)
  }

  private pedirCodigo(): void {
    this.codigo.set('')
    this.paso.set('codigo')
  }

  /** `volverA` solo si es una ruta interna real (regla `frontend-security`: nunca una URL externa). */
  private destinoTrasIngreso(): string {
    const volverA = this.route.snapshot.queryParamMap.get('volverA')
    return volverA && volverA.startsWith('/') && !volverA.startsWith('//') ? volverA : '/tablero'
  }
}
