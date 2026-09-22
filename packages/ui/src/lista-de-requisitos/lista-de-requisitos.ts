import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { ChipEstado } from '../chip-estado/chip-estado'
import type { Tono } from '../tono/tono'

export type Requisito = { id: string; nombre: string; estado: 'cumplido' | 'pendiente' | 'observado'; nota?: string; accion?: string }

const TONO: Record<Requisito['estado'], Tono> = { cumplido: 'ok', pendiente: 'neutro', observado: 'aviso' }
const NOMBRE: Record<Requisito['estado'], string> = { cumplido: 'Cumplido', pendiente: 'Pendiente', observado: 'Observado' }

/** Qué falta para habilitarse, con el avance arriba y una acción por requisito pendiente. */
@Component({
  selector: 'ap-lista-de-requisitos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado],
  template: `
    <p class="avance">{{ cumplidos() }} de {{ requisitos().length }} requisitos cumplidos</p>
    <ul>
      @for (r of requisitos(); track r.id) {
        <li>
          <div class="cuerpo"><p class="nombre">{{ r.nombre }}</p>@if (r.nota) { <p class="nota">{{ r.nota }}</p> }</div>
          <ap-chip-estado [tono]="TONO[r.estado]">{{ NOMBRE[r.estado] }}</ap-chip-estado>
          @if (r.accion && r.estado !== 'cumplido') { <ap-boton variante="fantasma" tamano="sm" (pulsado)="actuar.emit(r.id)">{{ r.accion }}</ap-boton> }
        </li>
      }
    </ul>
  `,
  styles: `
    .avance { margin: 0 0 var(--s2); font-weight: 600; color: var(--text-2); }
    ul { margin: 0; padding: 0; list-style: none; }
    li { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s3); padding: var(--s3) 0; border-bottom: var(--borde-fino) solid var(--border); }
    .cuerpo { flex: 1; min-width: calc(var(--s7) * 3); }
    p { margin: 0; }
    .nombre { color: var(--text); }
    .nota { color: var(--text-3); font-size: .85em; }
  `,
})
export class ListaDeRequisitos {
  readonly TONO = TONO
  readonly NOMBRE = NOMBRE
  readonly requisitos = input.required<Requisito[]>()
  readonly actuar = output<string>()
  readonly cumplidos = computed(() => this.requisitos().filter((r) => r.estado === 'cumplido').length)
}
