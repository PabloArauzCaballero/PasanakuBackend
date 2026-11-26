import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { Boton } from '@aportaya/ui/boton/boton';
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado';
import { formatearFecha } from '@aportaya/ui/fecha/fecha';
import { Icono } from '@aportaya/ui/icono/icono';
import type { ExpedienteDeMedida } from 'clientes/angular/identidad/model/expedienteDeMedida';
import { Sesion } from '../../../nucleo/sesion';
import { tieneAlguno } from '../compartido/acceso';
import { textosAcciones } from '../textos-acciones-participantes';
import { textosParticipantes } from '../textos-participantes';
import { AccionesDeMedida } from './acciones-de-medida';
import { ProponerMedida } from './proponer-medida';

const T = textosParticipantes;

/**
 * CU-134 · las medidas de un participante: causal, etapa, línea de tiempo y plazos de cada una, con sus actos
 * ({@link AccionesDeMedida}) y, con permiso, el botón para proponer una nueva. Avisa `cambio` para recargar la ficha.
 */
@Component({
  selector: 'ap-medidas-del-participante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeMedida, Boton, ChipEstado, Icono, ProponerMedida],
  template: `
    <section aria-labelledby="medidas" class="bloque">
      <h2 id="medidas"><ap-icono nombre="escudo" tamano="s4" />{{ t.ficha.medidas }}</h2>
      @if (puedeProponer()) {
        <ap-boton tamano="sm" icono="escudo" (pulsado)="proponiendo.set(true)">{{
          ta.proponer.boton
        }}</ap-boton>
        <ap-proponer-medida
          [usuarioId]="usuarioId()"
          [(abierto)]="proponiendo"
          (propuesta)="cambio.emit()"
        />
      }
      @for (m of medidas(); track m.expedienteId) {
        <article class="medida">
          <header>
            <strong>{{ texto(t.causales, m.causal) }}</strong> · {{ texto(t.medidas, m.medida) }}
            <ap-chip-estado tono="aviso">{{ texto(t.etapas, m.estado) }}</ap-chip-estado>
          </header>
          <h3><ap-icono nombre="historial" tamano="s4" />{{ t.ficha.lineaDeTiempo }}</h3>
          <ol>
            @for (h of m.lineaDeTiempo; track $index) {
              <li>
                <time [attr.datetime]="h.ocurridoEn">{{ fecha(h.ocurridoEn) }}</time> ·
                {{ texto(t.etapas, h.estado) }}
              </li>
            }
          </ol>
          <p class="plazos">{{ plazos(m) }}</p>
          <ap-acciones-de-medida [medida]="m" (cambio)="cambio.emit()" />
        </article>
      } @empty {
        <p class="indicacion">{{ t.ficha.sinMedidas }}</p>
      }
    </section>
  `,
  styles: `
    :host {
      display: block;
      margin-top: var(--s5);
    }
    .bloque {
      display: grid;
      gap: var(--s3);
    }
    .indicacion,
    .plazos {
      margin: 0;
      color: var(--text-2);
    }
    h2,
    h3 {
      display: flex;
      align-items: center;
      gap: var(--s2);
      margin: 0;
      font: var(--t-titulo-3);
    }
    .medida {
      display: grid;
      gap: var(--s2);
      padding: var(--s4);
      border: var(--borde-fino) solid var(--border);
      border-radius: var(--r-lg);
      background: var(--surface);
    }
    .medida header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--s2);
    }
    ol {
      margin: 0;
      padding-left: var(--s5);
    }
  `,
})
export class MedidasDelParticipante {
  readonly usuarioId = input.required<string>();
  readonly medidas = input.required<ExpedienteDeMedida[]>();
  readonly cambio = output<void>();

  protected readonly t = T;
  protected readonly ta = textosAcciones;
  protected readonly proponiendo = signal(false);
  private readonly sesion = inject(Sesion);
  protected readonly puedeProponer = computed(() =>
    tieneAlguno(this.sesion, ['PARTICIPANTES_MEDIDA_PROPONER']),
  );
  protected readonly fecha = (iso: string) => formatearFecha(iso);
  protected readonly texto = (mapa: Record<string, string>, codigo: string) =>
    mapa[codigo] ?? codigo;

  protected plazos(m: ExpedienteDeMedida): string {
    const partes = [
      m.fechaLimiteDescargo ? `Descargo hasta ${formatearFecha(m.fechaLimiteDescargo)}` : null,
      m.fechaLimiteApelacion ? `Apelación hasta ${formatearFecha(m.fechaLimiteApelacion)}` : null,
      `Prescribe el ${formatearFecha(m.prescribeEn)}`,
    ];
    return partes.filter(Boolean).join(' · ');
  }
}
