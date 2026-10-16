import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { DefinicionDeBandeja, LecturaDeBandeja } from './dominio/bandejas'
import { textosTablero } from './textos'

/** Lo que pinta una tarjeta: la lectura (o «cargando» mientras no hay) y cuándo se consultó. */
export type EstadoDeTarjeta = { tipo: 'cargando' } | (LecturaDeBandeja & { consultadaEn: string })

/**
 * Una bandeja del tablero, presentacional: no consulta nada. Dice «hay pendientes» o «al día», nunca un número, porque la
 * bandeja no trae total (ver `dominio/bandejas.ts`). El estado no depende solo del color: el chip lleva el texto.
 */
@Component({
  selector: 'ap-tarjeta-de-bandeja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Fecha, RouterLink],
  template: `
    <article class="tarjeta" [attr.aria-labelledby]="'bandeja-' + bandeja().id" [attr.data-bandeja]="bandeja().id">
      <h3 [id]="'bandeja-' + bandeja().id">{{ bandeja().titulo }}</h3>
      <p class="definicion">{{ bandeja().definicion }}</p>
      @switch (estado().tipo) {
        @case ('cargando') {
          <p role="status" class="cargando">{{ t.cargando }}…</p>
        }
        @case ('pendientes') {
          <ap-chip-estado tono="aviso">{{ t.pendientes }}</ap-chip-estado>
        }
        @case ('al-dia') {
          <ap-chip-estado tono="ok">{{ t.alDia }}</ap-chip-estado>
        }
        @case ('error') {
          <div role="alert" class="error">
            <ap-chip-estado tono="error">{{ t.error }}</ap-chip-estado>
            <p>{{ mensajeDeError() }}</p>
            @if (trazaDeError(); as traza) { <p class="traza">{{ t.traza }}: {{ traza }}</p> }
          </div>
        }
      }
      @if (consultadaEn(); as iso) {
        <p class="hora">{{ t.consultadaEl }} <ap-fecha [iso]="iso" /></p>
      }
      <a class="abrir" [routerLink]="bandeja().ruta">{{ t.abrir }}</a>
    </article>
  `,
  styles: `
    .tarjeta { display: grid; gap: var(--s2); align-content: start; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); height: 100%; }
    h3 { margin: 0; font-size: 1em; color: var(--text); }
    .definicion, .hora, .cargando, .traza { margin: 0; color: var(--text-2); font-size: .9em; }
    .error { display: grid; gap: var(--s1); }
    .error p { margin: 0; color: var(--text); }
    .abrir { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; }
    .abrir:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class TarjetaDeBandeja {
  protected readonly t = textosTablero.accion
  readonly bandeja = input.required<DefinicionDeBandeja>()
  readonly estado = input.required<EstadoDeTarjeta>()

  protected consultadaEn(): string | null {
    const e = this.estado()
    return e.tipo === 'cargando' ? null : e.consultadaEn
  }
  protected mensajeDeError(): string {
    const e = this.estado()
    return e.tipo === 'error' ? e.error.mensaje : ''
  }
  protected trazaDeError(): string | undefined {
    const e = this.estado()
    return e.tipo === 'error' ? e.error.trazaId : undefined
  }
}
