import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Avatar } from '../avatar/avatar'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Monto } from '../monto/monto'
import { Progreso } from '../progreso/progreso'
import type { Tono } from '../tono/tono'

export type EstadoDeGrupo = 'alDia' | 'porVencer' | 'atrasado' | 'cerrado'

const TONO: Record<EstadoDeGrupo, Tono> = { alDia: 'ok', porVencer: 'aviso', atrasado: 'error', cerrado: 'neutro' }

/** Un grupo en una lista: nombre, aporte, avance del ciclo y el estado de mi cuota. */
@Component({
  selector: 'ap-item-pasanaku',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Avatar, ChipEstado, Monto, Progreso],
  host: { role: 'listitem' },
  template: `
    <ap-avatar [nombre]="nombre()" tamano="lg" />
    <div class="cuerpo">
      <p class="nombre">{{ nombre() }}</p>
      <p class="detalle"><ap-monto [monto]="aporte()" [moneda]="moneda()" etiqueta="Aporte" /> · turno {{ turnoActual() }} de {{ turnos() }}</p>
      <ap-progreso [valor]="turnoActual() / turnos()" [etiqueta]="'Avance del grupo ' + nombre()" />
    </div>
    <ap-chip-estado [tono]="tono()">{{ NOMBRE[estado()] }}</ap-chip-estado>
  `,
  styles: `
    :host { display: flex; align-items: center; gap: var(--s3); padding: var(--s3) 0; border-bottom: var(--borde-fino) solid var(--border); }
    .cuerpo { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: var(--s1); }
    p { margin: 0; }
    .nombre { font-weight: 600; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .detalle { color: var(--text-2); font-size: .9em; }
  `,
})
export class ItemPasanaku {
  readonly NOMBRE: Record<EstadoDeGrupo, string> = { alDia: 'Al día', porVencer: 'Por vencer', atrasado: 'Atrasado', cerrado: 'Cerrado' }
  readonly nombre = input.required<string>()
  readonly aporte = input.required<string>()
  readonly moneda = input.required<string>()
  readonly turnoActual = input.required<number>()
  readonly turnos = input.required<number>()
  readonly estado = input.required<EstadoDeGrupo>()
  readonly tono = computed<Tono>(() => TONO[this.estado()])
}
