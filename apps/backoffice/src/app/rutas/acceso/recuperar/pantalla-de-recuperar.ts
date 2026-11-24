import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Icono } from '@aportaya/ui/icono/icono'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { MarcoDeAcceso } from '../compartido/marco-de-acceso'
import { crearPedirRecuperacion, type AlcanceDeRecuperacion } from '../dominio/cuenta'
import { errorDeCuenta } from '../dominio/errores-de-cuenta'

const CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const OPCIONES: OpcionDeRadio[] = [
  { valor: 'CLAVE', texto: 'Olvidé mi clave' },
  { valor: 'FACTOR', texto: 'Perdí mi teléfono o mi autenticador' },
]
const PASOS = [
  'Otra persona de seguridad revisa tu pedido: nadie puede aprobar el suyo propio.',
  'Si lo aprueba, hay un plazo de espera antes de que salga nada. Es a propósito: protege tu cuenta si el pedido no fuera tuyo.',
  'Cumplido el plazo, te llega un correo con un enlace de un solo uso para elegir tu clave nueva o registrar tu autenticador.',
]

/**
 * GU-C · «No puedo entrar» (`/acceso/recuperar`, pública). Pide la recuperación de la clave o del factor (`pedirRecuperacion`).
 * El servidor responde **siempre** 202 con el mismo cuerpo —exista o no el correo, esté o no activa la cuenta, haya o no un pedido
 * vivo— y esta pantalla también: el texto de confirmación es fijo y no depende de la respuesta. Solo abre un expediente; no entrega nada.
 */
@Component({
  selector: 'ap-pantalla-de-recuperar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, Boton, Campo, GrupoRadio, Icono, MarcoDeAcceso],
  template: `
    <ap-marco-de-acceso ceja="Recuperar el acceso" [titulo]="enviado() ? 'Recibimos tu pedido' : 'No puedo entrar'" [bajada]="enviado() ? undefined : 'Contanos qué te pasó y abrimos un pedido de recuperación. No cambia nada hasta que otra persona lo apruebe.'">
      @if (enviado()) {
        <ap-alerta tono="ok" titulo="Pedido recibido">Si el correo corresponde a un operador de AportaYa, abrimos el pedido. Por seguridad respondemos lo mismo en todos los casos.</ap-alerta>
        <ol class="pasos">@for (p of pasos; track p) { <li>{{ p }}</li> }</ol>
        <ap-boton variante="secundario" icono="flechaIzquierda" [ancho]="true" (pulsado)="otro()">Hacer otro pedido</ap-boton>
      } @else {
        @if (error(); as e) { <ap-alerta tono="error">{{ e }}</ap-alerta> }
        <form #formulario (submit)="$event.preventDefault(); enviar()" novalidate>
          <ap-campo etiqueta="Correo institucional" tipo="email" autocompletar="username" marcador="nombre@ejemplo.test" [(valor)]="correo" [error]="errorCorreo()" [deshabilitado]="enviando()" />
          <ap-grupo-radio etiqueta="¿Qué pasó?" [opciones]="opciones" [(elegido)]="alcance" />
          <ap-area-de-texto etiqueta="Motivo (opcional)" ayuda="Ayuda a quien revisa el pedido. Entre 5 y 200 caracteres." [maximo]="200" [(valor)]="motivo" [error]="errorMotivo()" [deshabilitado]="enviando()" />
          <ap-boton variante="primario" tipo="submit" icono="enviar" [ancho]="true" [cargando]="enviando()" [deshabilitado]="enviando()">Pedir recuperación</ap-boton>
        </form>
        <p class="pie"><ap-icono nombre="info" tamano="s4" />No te pedimos tu clave ni tus códigos en este formulario.</p>
      }
    </ap-marco-de-acceso>
  `,
  styles: `
    form { display: grid; gap: var(--s4); }
    .pasos { margin: 0; padding-left: var(--s5); display: grid; gap: var(--s2); font: var(--t-cuerpo); color: var(--text-2); }
    .pie { display: flex; gap: var(--s2); align-items: center; margin: 0; font: var(--t-cuerpo-chico); color: var(--text-3); }
  `,
})
export class PantallaDeRecuperar {
  private readonly pedir = crearPedirRecuperacion()
  private readonly injector = inject(Injector)
  private readonly formulario = viewChild<ElementRef<HTMLElement>>('formulario')
  protected readonly opciones = OPCIONES
  protected readonly pasos = PASOS
  protected readonly correo = signal('')
  protected readonly alcance = signal<string | null>('CLAVE')
  protected readonly motivo = signal('')
  protected readonly enviando = signal(false)
  protected readonly enviado = signal(false)
  protected readonly error = signal<string | null>(null)
  protected readonly errorCorreo = signal<string | undefined>(undefined)
  protected readonly errorMotivo = signal<string | undefined>(undefined)

  protected readonly motivoLimpio = computed(() => this.motivo().trim())

  protected enviar(): void {
    if (this.enviando()) return
    this.error.set(null)
    const correo = this.correo().trim()
    this.errorCorreo.set(!correo ? 'Escribí tu correo institucional.' : !CORREO.test(correo) || correo.length > 150 ? 'Ese correo no está completo. Ejemplo: nombre@ejemplo.test' : undefined)
    const largo = this.motivoLimpio().length
    this.errorMotivo.set(largo > 0 && (largo < 5 || largo > 200) ? 'El motivo tiene entre 5 y 200 caracteres, o dejalo vacío.' : undefined)
    if (this.errorCorreo() || this.errorMotivo()) {
      afterNextRender(() => this.formulario()?.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(), { injector: this.injector })
      return
    }
    this.enviando.set(true)
    this.pedir({ correo, alcance: (this.alcance() ?? 'CLAVE') as AlcanceDeRecuperacion, ...(largo ? { motivo: this.motivoLimpio() } : {}) }).subscribe({
      next: () => { this.enviando.set(false); this.enviado.set(true) },
      // Un fallo no revela nada de la cuenta: solo «no pudimos» o el límite de intentos. Lo escrito se conserva.
      error: (e: ErrorTraducido) => { this.enviando.set(false); this.error.set(errorDeCuenta(e).mensaje) },
    })
  }

  protected otro(): void {
    this.correo.set('')
    this.motivo.set('')
    this.alcance.set('CLAVE')
    this.enviado.set(false)
  }
}
