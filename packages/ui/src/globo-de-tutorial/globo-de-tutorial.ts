import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, computed, inject, input, output, signal } from '@angular/core'
import { Boton } from '../boton/boton'
import { Progreso } from '../progreso/progreso'
import type { RecuadroResaltado } from '../foco-de-tutorial/foco-de-tutorial'
import { ubicar, type LadoDelGlobo, type Medida } from './ubicacion'

/**
 * Lo que se supone que mide el globo **antes de haberlo pintado una vez**. Se usa para
 * el primer cálculo y nada más: apenas existe en el documento, el globo se mide solo.
 *
 * Suponerlo y no medirlo era un error caro: con un texto de tres renglones el globo
 * mide bastante más que esto, no entraba donde se lo había calculado y terminaba
 * tapando justo el elemento que estaba señalando.
 */
const TAMANO_SUPUESTO: Medida = { ancho: 340, alto: 240 }

/**
 * **El globo del tutorial**: una idea, un elemento, una acción. Es presentación pura —
 * no sabe qué tutorial corre ni cómo se guarda el avance; recibe textos y emite
 * intenciones.
 *
 * Es un `dialog` con `aria-modal="false"`: se anuncia como diálogo y el lector lo lee
 * entero al cambiar de paso, pero **no aísla la aplicación**, porque el paso siguiente
 * suele ser «pulsá ese botón de ahí».
 */
@Component({
  selector: 'ap-globo-de-tutorial',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Progreso],
  host: {
    role: 'dialog',
    'aria-modal': 'false',
    '[attr.aria-labelledby]': 'id + "-titulo"',
    '[attr.aria-describedby]': 'id + "-texto"',
    '[style.left.px]': 'ubicacion().izquierda',
    '[style.top.px]': 'ubicacion().arriba',
    '[class]': '"lado-" + ubicacion().lado',
  },
  template: `
    <span class="flecha" aria-hidden="true"></span>
    <p class="cuenta">Paso {{ indice() + 1 }} de {{ total() }}</p>
    <h2 [id]="id + '-titulo'">{{ titulo() }}</h2>
    <p class="texto" [id]="id + '-texto'">{{ descripcion() }}</p>
    @if (problema(); as p) {
      <p class="problema" role="alert">{{ p }}</p>
    }
    <ap-progreso class="barra" [valor]="fraccion()" [etiqueta]="'Avance del tutorial'" />
    <div class="acciones">
      <ap-boton variante="enlace" tamano="sm" (pulsado)="omitir.emit()">{{ textoOmitir() }}</ap-boton>
      <span class="separador"></span>
      @if (problema() !== null) {
        <ap-boton variante="secundario" tamano="sm" (pulsado)="reintentar.emit()">Reintentar</ap-boton>
      }
      <ap-boton variante="secundario" tamano="sm" [deshabilitado]="indice() === 0" (pulsado)="retroceder.emit()">Atrás</ap-boton>
      <ap-boton variante="primario" tamano="sm" (pulsado)="avanzar.emit()">{{ textoAvanzar() }}</ap-boton>
    </div>
    <button type="button" class="cerrar" (click)="cerrar.emit()" aria-label="Cerrar el tutorial">×</button>
  `,
  styles: `
    :host { position: fixed; z-index: 50; display: block; width: min(100vw - var(--s5), calc(var(--s7) * 7)); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); color: var(--text); box-shadow: var(--sombra-3); }
    .cuenta { margin: 0; color: var(--text-3); font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; }
    h2 { margin: var(--s1) 0 var(--s2); font: var(--t-titulo-3); letter-spacing: var(--t-titulo-3-track); }
    .texto { margin: 0; color: var(--text-2); font: var(--t-cuerpo); }
    .problema { margin: var(--s3) 0 0; padding: var(--s2) var(--s3); border-radius: var(--r-md); background: var(--warn-bg); color: var(--aviso-texto); font: var(--t-cuerpo-chico); }
    .barra { margin: var(--s4) 0 var(--s3); }
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    .separador { flex: 1; }
    .cerrar { position: absolute; top: var(--s2); right: var(--s2); min-width: var(--area-tactil); min-height: var(--area-tactil); border: 0; background: transparent; color: var(--text-3); font-size: 1.4em; line-height: 1; cursor: pointer; }
    .cerrar:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); border-radius: var(--r-sm); }
    .flecha { position: absolute; width: var(--s3); height: var(--s3); background: var(--surface); border: var(--borde-fino) solid var(--border); transform: rotate(45deg); }
    :host(.lado-centro) .flecha { display: none; }
    :host(.lado-abajo) .flecha { top: calc(var(--s2) * -1 + 1 * var(--borde-fino)); left: 50%; margin-left: calc(var(--s3) / -2); clip-path: polygon(0 0, 100% 0, 0 100%); }
    :host(.lado-arriba) .flecha { bottom: calc(var(--s2) * -1 + 1 * var(--borde-fino)); left: 50%; margin-left: calc(var(--s3) / -2); clip-path: polygon(100% 0, 100% 100%, 0 100%); }
    :host(.lado-derecha) .flecha { left: calc(var(--s2) * -1 + 1 * var(--borde-fino)); top: 50%; margin-top: calc(var(--s3) / -2); clip-path: polygon(0 0, 0 100%, 100% 100%); }
    :host(.lado-izquierda) .flecha { right: calc(var(--s2) * -1 + 1 * var(--borde-fino)); top: 50%; margin-top: calc(var(--s3) / -2); clip-path: polygon(0 0, 100% 0, 100% 100%); }
    @media (max-width: 30rem) { :host { left: var(--s3) !important; right: var(--s3); top: auto !important; bottom: var(--s3); width: auto; } .flecha { display: none; } }
  `,
})
export class GloboDeTutorial {
  private static secuencia = 0
  readonly id = `ap-globo-tutorial-${++GloboDeTutorial.secuencia}`

  readonly titulo = input.required<string>()
  readonly descripcion = input.required<string>()
  readonly indice = input.required<number>()
  readonly total = input.required<number>()
  readonly recuadro = input<RecuadroResaltado | null>(null)
  readonly posicion = input<LadoDelGlobo>('abajo')
  readonly ventana = input.required<Medida>()
  /** El texto del botón principal cambia en el último paso («Terminar»). */
  readonly textoAvanzar = input('Siguiente')
  readonly textoOmitir = input('Omitir')
  /** Qué salió mal, si algo salió mal. Aparece como aviso, sin tapar el paso. */
  readonly problema = input<string | null>(null)

  readonly avanzar = output<void>()
  readonly retroceder = output<void>()
  readonly omitir = output<void>()
  readonly cerrar = output<void>()
  readonly reintentar = output<void>()

  private readonly host = inject(ElementRef<HTMLElement>)
  /** Lo que el globo mide de verdad, una vez que existe. */
  private readonly medida = signal<Medida>(TAMANO_SUPUESTO)

  protected readonly ubicacion = computed(() => ubicar(this.recuadro(), this.posicion(), this.ventana(), this.medida()))
  protected readonly fraccion = computed(() => (this.total() === 0 ? 0 : (this.indice() + 1) / this.total()))

  constructor() {
    const destruccion = inject(DestroyRef)
    afterNextRender(() => {
      this.medirse()
      // El texto de cada paso cambia el alto. `ResizeObserver` avisa de todos los
      // cambios de tamaño, incluido el que trae cambiar de paso; mover el globo no
      // dispara otro, así que no hay bucle.
      if (typeof ResizeObserver === 'undefined') return
      const observador = new ResizeObserver(() => this.medirse())
      observador.observe(this.host.nativeElement)
      destruccion.onDestroy(() => observador.disconnect())
    })
  }

  private medirse(): void {
    const elemento = this.host.nativeElement
    const ancho = elemento.offsetWidth
    const alto = elemento.offsetHeight
    if (ancho > 0 && alto > 0) this.medida.set({ ancho, alto })
  }
}
