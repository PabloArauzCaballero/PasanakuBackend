import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Icono } from '@aportaya/ui/icono/icono'
import { SimboloDeMarca } from '../../../layout/simbolo-de-marca'
import { PanelDeMarca } from '../../ingreso/panel-de-marca'

let secuencia = 0

/**
 * El marco de las pantallas **públicas** de acceso (invitación y «No puedo entrar»): la misma anatomía que el ingreso
 * (panel de marca a la izquierda, tarjeta a la derecha), con la cabecera y el pie como datos y el contenido proyectado.
 * El ingreso conserva la suya a propósito: tocarla está fuera del alcance de este trabajo.
 */
@Component({
  selector: 'ap-marco-de-acceso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, PanelDeMarca, RouterLink, SimboloDeMarca],
  template: `
    <main>
      <ap-panel-de-marca />
      <section class="lado" [attr.aria-labelledby]="idTitulo">
        <div class="tarjeta">
          <div class="cabecera">
            <ap-simbolo-de-marca class="simbolo" />
            <p class="ceja">{{ ceja() }}</p>
            <h1 [id]="idTitulo" tabindex="-1">{{ titulo() }}</h1>
            @if (bajada()) { <p class="bajada">{{ bajada() }}</p> }
          </div>
          <ng-content />
          <p class="nota"><ap-icono nombre="candado" tamano="s4" />Acceso únicamente para personal autorizado de AportaYa. Nunca te pediremos tu clave ni tus códigos por mensaje.</p>
        </div>
        @if (conVolver()) { <a class="volver" routerLink="/ingreso">Volver al ingreso</a> }
      </section>
    </main>
  `,
  styles: `
    main { min-height: 100dvh; display: flex; background: var(--bg); }
    .lado {
      flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: var(--s4);
      padding: var(--s5) var(--s4);
      background: radial-gradient(circle at 85% 0%, color-mix(in srgb, var(--brand) 9%, transparent), transparent 45%);
    }
    .tarjeta { width: 100%; max-width: 30rem; display: grid; gap: var(--s4); padding: var(--s6) var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-xl); background: var(--surface); box-shadow: var(--sombra-2); }
    .simbolo { --tamano-simbolo: 3rem; margin-bottom: var(--s3); }
    .cabecera { display: grid; gap: var(--s1); }
    .ceja { margin: 0; font: var(--t-etiqueta); letter-spacing: .06em; text-transform: uppercase; color: var(--brand-texto); }
    h1 { margin: 0; font: var(--t-titulo-1); letter-spacing: -.01em; color: var(--text); }
    h1:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .bajada { margin: 0; font: var(--t-cuerpo); color: var(--text-2); }
    .nota { display: flex; gap: var(--s2); align-items: flex-start; margin: 0; padding-top: var(--s4); border-top: var(--borde-fino) solid var(--border); font: var(--t-cuerpo-chico); color: var(--text-3); }
    .nota ap-icono { margin-top: var(--borde-desfase); color: var(--text-2); }
    .volver { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--brand-texto); font: var(--t-cuerpo-chico); }
    .volver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    @media (min-width: 64rem) { .simbolo { display: none; } .tarjeta { padding: var(--s7) var(--s6); } }
  `,
})
export class MarcoDeAcceso {
  readonly idTitulo = `ap-marco-titulo-${++secuencia}`
  readonly ceja = input.required<string>()
  readonly titulo = input.required<string>()
  readonly bajada = input<string>()
  readonly conVolver = input(true)
}
