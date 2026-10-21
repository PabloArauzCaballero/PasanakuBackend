import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
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
            <p class="error-mensaje">{{ mensajeDeError() }}</p>
            @if (trazaDeError(); as traza) {
              <p class="traza">{{ t.traza }}: <code class="traza-codigo">{{ traza }}</code></p>
            }
            <button type="button" class="boton-reintentar" (click)="reintentar.emit()">
              {{ t.reintentar }}
            </button>
          </div>
        }
      }
      @if (consultadaEn(); as iso) {
        <p class="hora">{{ estado().tipo === 'error' ? t.intentoEl : t.consultadaEl }} <ap-fecha [iso]="iso" /></p>
      }
      <a class="abrir" [routerLink]="bandeja().ruta">{{ t.abrir }}</a>
    </article>
  `,
  styles: `
    :host { display: block; height: 100%; }
    .tarjeta { display: flex; flex-direction: column; gap: var(--s2); align-content: start; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); height: 100%; box-sizing: border-box; }
    h3 { margin: 0; font-size: 1.05em; font-weight: 600; color: var(--text); }
    .definicion { margin: 0; color: var(--text-2); font-size: .9em; line-height: 1.4; }
    .cargando { margin: 0; color: var(--text-2); font-size: .9em; }
    ap-chip-estado { justify-self: start; width: fit-content; }
    .error { display: flex; flex-direction: column; align-items: flex-start; gap: var(--s2); }
    .error p { margin: 0; color: var(--text); font-size: .9em; line-height: 1.4; }
    .traza { margin: 0; color: var(--text-2) !important; font-size: .85em !important; word-break: break-all; overflow-wrap: anywhere; }
    .traza-codigo { font-family: var(--f-mono); font-size: .9em; background: color-mix(in srgb, var(--surface) 60%, var(--border)); padding: 0.15em 0.4em; border-radius: var(--r-xs); border: var(--borde-fino) solid var(--border); word-break: break-all; overflow-wrap: anywhere; display: inline-block; margin-top: var(--s1); color: var(--text); }
    .boton-reintentar { display: inline-flex; align-items: center; justify-content: center; min-height: calc(var(--s6) + var(--s1)); padding: 0 var(--s3); border-radius: var(--r-md); border: var(--borde-fino) solid var(--border); background: color-mix(in srgb, var(--surface) 80%, var(--border)); color: var(--text); font-size: .85em; font-weight: 600; cursor: pointer; transition: background var(--t-rapida), border-color var(--t-rapida); margin-top: var(--s1); }
    .boton-reintentar:hover { background: color-mix(in srgb, var(--surface) 60%, var(--border)); border-color: var(--text-2); }
    .boton-reintentar:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .hora { margin-top: auto; margin-bottom: 0; color: var(--text-2); font-size: .85em; padding-top: var(--s2); border-top: var(--borde-fino) solid color-mix(in srgb, var(--border) 40%, transparent); }
    .abrir { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; text-decoration: none; }
    .abrir:hover { text-decoration: underline; }
    .abrir:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class TarjetaDeBandeja {
  protected readonly t = textosTablero.accion
  readonly bandeja = input.required<DefinicionDeBandeja>()
  readonly estado = input.required<EstadoDeTarjeta>()
  readonly reintentar = output<void>()

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
