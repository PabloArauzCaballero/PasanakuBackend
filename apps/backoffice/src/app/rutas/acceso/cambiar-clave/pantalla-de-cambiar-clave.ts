import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, effect, inject, signal, untracked, viewChild } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CampoContrasena } from '@aportaya/ui/campo-contrasena/campo-contrasena'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { IndicadorDeClave } from '../compartido/indicador-de-clave'
import { LARGO_MINIMO_DE_CLAVE, requisitosDeClave } from '../dominio/clave'
import { crearCambiarClave, type SalidaCambioDeClave } from '../dominio/cuenta'
import { errorDeCuenta, type ErrorDeCuenta } from '../dominio/errores-de-cuenta'

/**
 * GU-C · «Cambiar mi clave» (`/acceso/cambiar-clave`, con sesión). El titular sale del token. Para un operador el servidor cierra
 * **todas** las sesiones —también esta— (R-SEG-11): se avisa y se lleva al ingreso con la sesión ya cerrada en el navegador. Si el
 * servidor conservara la actual, la pantalla lo dice y deja el formulario vacío. Los errores van a su campo; lo escrito se conserva.
 */
@Component({
  selector: 'ap-pantalla-de-cambiar-clave',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, CampoContrasena, IndicadorDeClave],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Mi cuenta" titulo="Cambiar mi clave" descripcion="Por seguridad pedimos tu clave actual. Si trabajás en Operación o Cumplimiento, al cambiarla cerramos tus sesiones y volvés a entrar con la nueva." />
      @if (hecho(); as h) {
        <ap-alerta tono="ok" titulo="Clave cambiada">
          {{ h.sesionesCerradas > 0 ? 'Cerramos ' + h.sesionesCerradas + (h.sesionesCerradas === 1 ? ' sesión' : ' sesiones') + ' de tu cuenta.' : 'No había otras sesiones abiertas.' }} Tu sesión actual sigue abierta.
        </ap-alerta>
      }
      @if (alerta(); as a) { <ap-alerta tono="error">{{ a }}</ap-alerta> }
      <form #formulario (submit)="$event.preventDefault(); enviar()" novalidate>
        <ap-campo-contrasena etiqueta="Clave actual" autocompletar="current-password" [(valor)]="actual" [error]="errorActual()" />
        <ap-campo-contrasena etiqueta="Clave nueva" autocompletar="new-password" [ayuda]="'Mínimo ' + largoMinimo + ' caracteres.'" [(valor)]="nueva" [error]="errorNueva()" />
        <ap-campo-contrasena etiqueta="Repetí la clave nueva" autocompletar="new-password" [(valor)]="repeticion" [error]="errorRepeticion()" />
        <ap-indicador-de-clave [requisitos]="requisitos()" />
        <ap-boton variante="primario" tipo="submit" icono="candado" [cargando]="enviando()" [deshabilitado]="enviando()">Cambiar mi clave</ap-boton>
      </form>
    </section>
  `,
  styles: `
    .pantalla { display: grid; gap: var(--s5); max-width: 32rem; min-width: 0; }
    form { display: grid; gap: var(--s4); }
  `,
})
export class PantallaDeCambiarClave {
  private readonly cambiar = crearCambiarClave()
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  private readonly injector = inject(Injector)
  private readonly formulario = viewChild<ElementRef<HTMLElement>>('formulario')
  protected readonly largoMinimo = LARGO_MINIMO_DE_CLAVE
  protected readonly actual = signal('')
  protected readonly nueva = signal('')
  protected readonly repeticion = signal('')
  protected readonly enviando = signal(false)
  protected readonly hecho = signal<SalidaCambioDeClave | null>(null)
  protected readonly local = signal<Record<string, string>>({})
  protected readonly servidor = signal<ErrorDeCuenta | null>(null)
  protected readonly requisitos = computed(() => requisitosDeClave(this.nueva(), this.repeticion(), this.actual()))
  private readonly de = (campo: string) => (this.servidor()?.campo === campo ? this.servidor()!.mensaje : undefined)
  protected readonly errorActual = computed(() => this.local()['actual'] ?? this.de('claveActual'))
  protected readonly errorNueva = computed(() => this.local()['nueva'] ?? this.de('claveNueva'))
  protected readonly errorRepeticion = computed(() => this.local()['repeticion'])
  protected readonly alerta = computed(() => (this.servidor() && !this.servidor()!.campo ? this.servidor()!.mensaje : null))

  constructor() {
    // Tocar un campo descarta el error que ya no es actual.
    effect(() => {
      this.actual(); this.nueva(); this.repeticion()
      untracked(() => { this.local.set({}); this.servidor.set(null) })
    })
  }

  protected enviar(): void {
    if (this.enviando()) return
    const e: Record<string, string> = {}
    if (!this.actual()) e['actual'] = 'Escribí tu clave actual.'
    if (this.nueva().length < LARGO_MINIMO_DE_CLAVE) e['nueva'] = `La clave nueva tiene al menos ${LARGO_MINIMO_DE_CLAVE} caracteres.`
    else if (this.nueva() === this.actual()) e['nueva'] = 'La clave nueva tiene que ser distinta de la actual.'
    if (this.repeticion() !== this.nueva()) e['repeticion'] = 'Las dos claves no coinciden. Volvé a escribir la repetición.'
    this.local.set(e)
    if (Object.keys(e).length) return this.enfocarPrimerError()
    this.enviando.set(true)
    this.hecho.set(null)
    this.cambiar({ claveActual: this.actual(), claveNueva: this.nueva() }).subscribe({
      next: (r) => this.alExito(r),
      error: (err: ErrorTraducido) => {
        this.enviando.set(false)
        this.servidor.set(errorDeCuenta(err))
        this.enfocarPrimerError()
      },
    })
  }

  private alExito(r: SalidaCambioDeClave): void {
    this.enviando.set(false)
    this.actual.set('')
    this.nueva.set('')
    this.repeticion.set('')
    if (!r.cierraLaSesionActual) return this.hecho.set(r)
    // El servidor ya revocó esta sesión: se cierra acá sin llamar a `DELETE /sesion` (daría 401) y se vuelve a entrar.
    this.sesion.cerrar()
    void this.router.navigate(['/ingreso'], { queryParams: { motivo: 'clave' } })
  }

  private enfocarPrimerError(): void {
    afterNextRender(() => this.formulario()?.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(), { injector: this.injector })
  }
}
