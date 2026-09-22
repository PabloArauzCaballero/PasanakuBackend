import { ChangeDetectionStrategy, Component, DOCUMENT, computed, effect, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { FocoDeTutorial, type RecuadroResaltado } from '@aportaya/ui/foco-de-tutorial/foco-de-tutorial'
import { GloboDeTutorial } from '@aportaya/ui/globo-de-tutorial/globo-de-tutorial'
import { MotorDeTutoriales } from './motor'
import { LocalizadorDeObjetivo } from './objetivo'
import { vigilarAccion } from './vigilar-accion'
import { textosDelRecorrido } from './textos-del-recorrido'

/**
 * **El anfitrión**: lo único que el shell monta para que los tutoriales existan.
 *
 * Junta las tres piezas que el motor no toca porque son vista: el velo, el globo y el
 * teclado. Sigue el recuadro del elemento mientras la página se desplaza o cambia de
 * tamaño, y engancha la escucha de la acción esperada del paso.
 *
 * Con el tutorial apagado no dibuja nada y no escucha nada.
 */
@Component({
  selector: 'ap-anfitrion-de-tutorial',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocoDeTutorial, GloboDeTutorial, Dialogo],
  host: {
    '(document:keydown)': 'tecla($event)',
    '(window:resize)': 'medir()',
    '(window:scroll)': 'medir()',
  },
  template: `
    <!--
      Todo el tutorial va dentro de un \`@defer\`: el velo, el globo y el diálogo de
      salida son varios kilobytes que nadie necesita hasta que alguien pide ayuda, y el
      shell se monta en TODAS las pantallas. Se traen al encenderse el motor.
    -->
    @defer (when motor.activo()) {
      @if (motor.activo() && motor.paso(); as paso) {
        <ap-foco-de-tutorial [recuadro]="recuadro()" [bloquea]="paso.bloquea ?? false" (fueraDelFoco)="motor.pedirSalida()" />
        <ap-globo-de-tutorial
          [titulo]="paso.titulo"
          [descripcion]="paso.descripcion"
          [indice]="motor.indice()"
          [total]="motor.total()"
          [recuadro]="recuadro()"
          [posicion]="paso.posicion ?? 'abajo'"
          [ventana]="ventana()"
          [textoAvanzar]="motor.esUltimo() ? textos.terminar : textos.siguiente"
          [textoOmitir]="textos.omitir"
          [problema]="aviso()"
          (avanzar)="motor.avanzar()"
          (retroceder)="motor.retroceder()"
          (omitir)="motor.pedirSalida()"
          (cerrar)="motor.pedirSalida()"
          (reintentar)="motor.reintentar()"
        />
      }
      <ap-dialogo
        [abierto]="motor.confirmandoSalida()"
        [titulo]="textos.salirTitulo"
        [textoDeConfirmar]="textos.salirConfirmar"
        [textoDeCancelar]="textos.salirSeguir"
        (confirmar)="motor.omitir()"
        (cancelar)="motor.seguirEnElTutorial()"
      >
        {{ textos.salirCuerpo }}
      </ap-dialogo>
    }
  `,
  styles: `:host { display: contents; }`,
})
export class AnfitrionDeTutorial {
  protected readonly motor = inject(MotorDeTutoriales)
  protected readonly textos = textosDelRecorrido
  private readonly localizador = inject(LocalizadorDeObjetivo)
  private readonly router = inject(Router)
  private readonly doc = inject(DOCUMENT)

  protected readonly recuadro = signal<RecuadroResaltado | null>(null)
  protected readonly ventana = signal(this.medida())
  /** El aviso del globo: primero el problema del paso, después el del guardado. */
  protected readonly aviso = computed(() => this.motor.problema()?.detalle ?? (this.motor.sinGuardar() ? textosDelRecorrido.sinGuardar : null))

  private soltar: (() => void) | null = null
  private foco: HTMLElement | null = null

  constructor() {
    // El elemento resaltado cambió: se mide, se engancha la escucha de la acción y se
    // suelta la anterior. Un `effect` y no una suscripción a mano porque el motor es
    // señales de punta a punta.
    effect(() => {
      const paso = this.motor.paso()
      const elemento = this.motor.elemento()
      this.soltar?.()
      this.soltar = null
      this.recuadro.set(elemento === null ? null : this.localizador.recuadroDe(elemento))
      if (paso === null) return
      this.soltar = vigilarAccion(paso, { elemento, router: this.router, localizador: this.localizador }, () => this.motor.accionCumplida.set(true))
    })
    // Al abrir un tutorial se recuerda dónde estaba el foco; al cerrarlo se devuelve.
    effect(() => {
      if (this.motor.activo()) this.foco ??= this.doc.activeElement as HTMLElement | null
      else this.devolverFoco()
    })
  }

  /** Recalcula el recuadro: la página se movió y el agujero tiene que seguirla. */
  protected medir(): void {
    this.ventana.set(this.medida())
    const elemento = this.motor.elemento()
    this.recuadro.set(elemento === null ? null : this.localizador.recuadroDe(elemento))
  }

  /**
   * El teclado, que es como se recorre un tutorial sin ratón: Escape sale (preguntando
   * si va por la mitad), las flechas y Enter avanzan y retroceden. No se secuestra el
   * teclado cuando alguien está escribiendo en un campo: ahí las flechas son suyas.
   */
  protected tecla(evento: KeyboardEvent): void {
    if (!this.motor.activo() || this.motor.confirmandoSalida()) return
    if (escribiendo(evento.target)) return
    if (evento.key === 'Escape') {
      evento.preventDefault()
      this.motor.pedirSalida()
      return
    }
    if (evento.key === 'ArrowRight') {
      evento.preventDefault()
      void this.motor.avanzar()
    }
    if (evento.key === 'ArrowLeft') {
      evento.preventDefault()
      void this.motor.retroceder()
    }
  }

  /** El tamaño de la ventana, que es lo que decide de qué lado entra el globo. */
  private medida(): { ancho: number; alto: number } {
    const ventana = this.doc.defaultView
    return { ancho: ventana?.innerWidth ?? 0, alto: ventana?.innerHeight ?? 0 }
  }

  private devolverFoco(): void {
    const previo = this.foco
    this.foco = null
    if (previo !== null && typeof previo.focus === 'function' && previo.isConnected) previo.focus()
  }
}

/** ¿El teclado es de un campo? Entonces no es del tutorial. */
function escribiendo(destino: EventTarget | null): boolean {
  if (destino === null || !(typeof Element !== 'undefined' && destino instanceof Element)) return false
  const etiqueta = destino.tagName.toLowerCase()
  return etiqueta === 'input' || etiqueta === 'textarea' || etiqueta === 'select' || destino.hasAttribute('contenteditable')
}
