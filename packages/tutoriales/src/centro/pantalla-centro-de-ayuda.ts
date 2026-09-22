import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal } from '@angular/core'
import { Router } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { MotorDeTutoriales } from '../motor'
import { ProgresoDeTutoriales } from '../progreso'
import { RegistroDeTutoriales } from '../registro'
import { componerLista, recomendado, type FiltroDeEstado, type TutorialEnLista } from './vista-de-tutoriales'
import { FiltrosDeAyuda } from './filtros-de-ayuda'
import { ResumenDeAvance } from './resumen-de-avance'
import { TarjetaDeTutorial } from './tarjeta-de-tutorial'
import { PRESENTACION_DEL_CENTRO, presentacionPorOmision, textosAyuda } from './textos'

/**
 * **El centro de tutoriales.** Lista lo que esta persona puede aprender, con su avance,
 * y lanza el recorrido sobre la interfaz de verdad: al pulsar «Comenzar» el motor
 * navega a la pantalla del tutorial y el globo aparece allá, no acá.
 *
 * El filtro vive en la dirección (`?q=`, `?estado=`, `?categoria=`) como el de
 * cualquier tabla del backoffice: así un enlace a «lo que me falta» se puede pegar en
 * un mensaje.
 */
@Component({
  selector: 'ap-pantalla-centro-de-ayuda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, EstadoVacio, Alerta, FiltrosDeAyuda, ResumenDeAvance, TarjetaDeTutorial],
  template: `
    <ap-banda-de-proposito [texto]="presentacion.proposito" />
    <main>
      <h1>{{ presentacion.titulo }}</h1>

      @if (registro.problemas().length > 0) {
        <ap-alerta tono="error" [titulo]="t.problemasTitulo">{{ t.problemasAyuda }}</ap-alerta>
        <ul class="problemas">
          @for (p of registro.problemas(); track p.codigo + p.tutorialId + p.detalle) {
            <li>{{ p.tutorialId }}: {{ p.detalle }}</li>
          }
        </ul>
      }

      <ap-resumen-de-avance data-tutorial-id="ayuda-avance" [hechos]="hechos()" [total]="registro.disponibles().length" />

      <ap-filtros-de-ayuda [categorias]="registro.categorias()" [(texto)]="texto" [(estado)]="estadoElegido" [(categoria)]="categoriaElegida" />

      @if (sugerido(); as s) {
        <section class="sugerido" aria-live="polite">
          <p class="etiqueta">{{ t.recomendado }}</p>
          <ap-tarjeta-de-tutorial [fila]="s" (comenzar)="comenzar(s)" (continuar)="continuar(s)" (reiniciar)="reiniciar(s)" />
        </section>
      }

      @if (resto().length === 0 && sugerido() === undefined) {
        <ap-estado-vacio
          [motivo]="registro.disponibles().length === 0 ? 'sinDatos' : 'porFiltro'"
          [titulo]="registro.disponibles().length === 0 ? t.sinTutoriales : t.sinResultados"
        />
      } @else {
        <ul class="lista" data-tutorial-id="ayuda-lista" role="list">
          @for (fila of resto(); track fila.tutorial.id) {
            <li>
              <ap-tarjeta-de-tutorial [fila]="fila" (comenzar)="comenzar(fila)" (continuar)="continuar(fila)" (reiniciar)="reiniciar(fila)" />
            </li>
          }
        </ul>
      }
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 64rem; display: flex; flex-direction: column; gap: var(--s4); }
    h1 { margin: 0; }
    .sugerido { display: flex; flex-direction: column; gap: var(--s2); }
    .etiqueta { margin: 0; color: var(--text-3); font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr)); gap: var(--s4); }
    .lista > li { display: flex; }
    .lista > li > * { flex: 1; }
    .problemas { margin: 0; padding-left: var(--s5); color: var(--err-texto); font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaCentroDeAyuda {
  protected readonly t = textosAyuda
  /** Título y propósito los pone cada producto; el resto de los textos son comunes. */
  protected readonly presentacion = inject(PRESENTACION_DEL_CENTRO, { optional: true }) ?? presentacionPorOmision
  protected readonly registro = inject(RegistroDeTutoriales)
  private readonly progresos = inject(ProgresoDeTutoriales)
  private readonly motor = inject(MotorDeTutoriales)
  private readonly router = inject(Router)

  /** Entradas desde la dirección (`withComponentInputBinding`). */
  readonly q = input('')
  readonly estado = input<FiltroDeEstado>('todos')
  readonly categoria = input('')

  /**
   * Lo elegido en pantalla arranca de la dirección y después manda. `linkedSignal` es
   * justo esto: un estado propio que se reinicia cuando cambia la entrada.
   *
   * El `??` no sobra: con `withComponentInputBinding()` una entrada que viene de un
   * parámetro **ausente** llega `undefined`, no con su valor por omisión. Sin esto, la
   * página reventaba al prerrenderizarse, que es justo cuando no hay ningún parámetro.
   */
  protected readonly texto = linkedSignal(() => this.q() ?? '')
  protected readonly estadoElegido = linkedSignal<FiltroDeEstado>(() => this.estado() ?? 'todos')
  protected readonly categoriaElegida = linkedSignal(() => this.categoria() ?? '')

  protected readonly lista = computed<readonly TutorialEnLista[]>(() =>
    componerLista(this.registro.disponibles(), this.progresos.mapa(), {
      texto: this.texto(),
      estado: this.estadoElegido(),
      categoria: this.categoriaElegida() === '' ? null : this.categoriaElegida(),
    }),
  )

  /** ¿Se está buscando o filtrando algo? */
  protected readonly hayFiltro = computed(
    () => this.texto().trim() !== '' || this.estadoElegido() !== 'todos' || this.categoriaElegida() !== '',
  )

  /**
   * Por dónde conviene empezar. **Se muestra solo sin filtro**: quien busca «zzzz» y no
   * encuentra nada tiene que ver que no encontró nada, no una tarjeta que no pidió.
   */
  protected readonly sugerido = computed(() =>
    this.hayFiltro()
      ? undefined
      : recomendado(componerLista(this.registro.disponibles(), this.progresos.mapa(), { texto: '', estado: 'todos', categoria: null })),
  )

  /**
   * La lista sin el sugerido: ya está arriba, con su propia tarjeta. Mostrarlo dos veces
   * hacía dudar de si eran dos tutoriales distintos con el mismo nombre.
   */
  protected readonly resto = computed(() => this.lista().filter((f) => f.tutorial.id !== this.sugerido()?.tutorial.id))

  protected readonly hechos = computed(
    () => componerLista(this.registro.disponibles(), this.progresos.mapa(), { texto: '', estado: 'completado', categoria: null }).length,
  )

  constructor() {
    void this.registro.cargar()
    this.progresos.cargar()
    // El filtro se refleja en la dirección con `replaceUrl`: no ensucia el historial,
    // pero el enlace que uno copia trae puesto lo que está viendo.
    effect(() => {
      const queryParams = { q: vacioANulo(this.texto()), estado: this.estadoElegido() === 'todos' ? null : this.estadoElegido(), categoria: vacioANulo(this.categoriaElegida()) }
      void this.router.navigate([], { queryParams, replaceUrl: true })
    })
  }

  protected comenzar(fila: TutorialEnLista): void {
    void this.motor.iniciar(fila.tutorial.id, fila.progreso?.repeticiones ?? 0)
  }

  protected continuar(fila: TutorialEnLista): void {
    if (fila.progreso === undefined) return this.comenzar(fila)
    void this.motor.continuar(fila.tutorial.id, fila.progreso)
  }

  protected reiniciar(fila: TutorialEnLista): void {
    this.progresos.reiniciar(fila.tutorial.id).subscribe()
  }
}

const vacioANulo = (valor: string | undefined): string | null => (valor === undefined || valor.trim() === '' ? null : valor)
