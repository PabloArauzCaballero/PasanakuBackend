import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Progreso } from '@aportaya/ui/progreso/progreso'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { TutorialEnLista } from './vista-de-tutoriales'
import { textosAyuda } from './textos'

/** Un tutorial en la lista: qué enseña, cuánto lleva y qué botón corresponde. */
@Component({
  selector: 'ap-tarjeta-de-tutorial',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado, Progreso],
  template: `
    <header>
      <div class="titulo">
        <h3>{{ fila().tutorial.titulo }}</h3>
        <p class="meta">
          {{ fila().tutorial.categoria }} · {{ t.dificultad[fila().tutorial.dificultad] }} · {{ t.pasos(fila().tutorial.pasos.length) }}
          @if (fila().tutorial.minutos; as m) { · {{ t.minutos(m) }} }
        </p>
      </div>
      <div class="chips">
        @if (fila().tutorial.obligatorio) {
          <ap-chip-estado tono="aviso">{{ t.obligatorio }}</ap-chip-estado>
        }
        <ap-chip-estado [tono]="tono()">{{ t.estado[fila().estado] }}</ap-chip-estado>
      </div>
    </header>
    <p class="descripcion">{{ fila().tutorial.descripcion }}</p>

    @if (fila().requisitosPendientes.length > 0) {
      <p class="requisitos">{{ t.requisitos }}: {{ nombresDeRequisitos() }}</p>
    }
    @if (fila().fraccion > 0 && fila().estado !== 'completado') {
      <ap-progreso [valor]="fila().fraccion" [etiqueta]="'Avance de ' + fila().tutorial.titulo" />
    }

    <div class="acciones">
      @if (fila().continuable) {
        <ap-boton variante="primario" tamano="sm" (pulsado)="continuar.emit()">{{ t.continuar }}</ap-boton>
      } @else {
        <ap-boton variante="primario" tamano="sm" (pulsado)="comenzar.emit()">{{ fila().estado === 'completado' ? t.repetir : t.comenzar }}</ap-boton>
      }
      @if (fila().progreso !== undefined) {
        <ap-boton variante="fantasma" tamano="sm" (pulsado)="reiniciar.emit()">{{ t.reiniciar }}</ap-boton>
      }
    </div>
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s3); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); flex-wrap: wrap; }
    h3 { margin: 0; font: var(--t-titulo-3); letter-spacing: var(--t-titulo-3-track); }
    .meta { margin: var(--s1) 0 0; color: var(--text-3); font: var(--t-cuerpo-chico); }
    .chips { display: flex; gap: var(--s2); flex-wrap: wrap; }
    .descripcion { margin: 0; color: var(--text-2); font: var(--t-cuerpo); }
    .requisitos { margin: 0; color: var(--aviso-texto); font: var(--t-cuerpo-chico); }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; margin-top: auto; }
  `,
})
export class TarjetaDeTutorial {
  protected readonly t = textosAyuda
  readonly fila = input.required<TutorialEnLista>()
  readonly comenzar = output<void>()
  readonly continuar = output<void>()
  readonly reiniciar = output<void>()

  /** El estado también se dice con palabra, no solo con color (regla de accesibilidad). */
  protected readonly tono = computed<Tono>(() => {
    switch (this.fila().estado) {
      case 'completado':
        return 'ok'
      case 'en-progreso':
        return 'info'
      case 'omitido':
        return 'aviso'
      default:
        return 'neutro'
    }
  })

  protected readonly nombresDeRequisitos = computed(() => this.fila().requisitosPendientes.map((r) => r.titulo).join(', '))
}
