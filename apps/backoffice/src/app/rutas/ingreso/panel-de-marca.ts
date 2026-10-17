import { ChangeDetectionStrategy, Component } from '@angular/core'
import { Icono } from '@aportaya/ui/icono/icono'

/** Lo que se hace adentro, en la voz del producto: un titular solo no alcanza a decirlo. */
const CAPACIDADES = [
  'Operación de grupos, aportes y entregas',
  'Cumplimiento, KYC y casos con debido proceso',
  'Contabilidad de doble partida y cierres',
  'Auditoría de cada acción de cada operador',
]

/**
 * El panel institucional del ingreso, con la anatomía del portal interno de Atlas
 * (`AtlasAdminPortal/src/features/auth/login-page.tsx`): identidad arriba, mensaje y
 * capacidades al medio, aviso de uso interno abajo. Es decorativo para el lector de
 * pantalla salvo el texto, y desaparece por debajo de 1024 px: en un teléfono lo
 * único que importa es el formulario.
 */
@Component({
  selector: 'ap-panel-de-marca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <div class="identidad">
      <span class="monograma" aria-hidden="true">A</span>
      <div>
        <p class="nombre">AportaYa</p>
        <p class="sub">Backoffice</p>
      </div>
    </div>

    <div class="mensaje">
      <p class="ceja">Operación interna</p>
      <h2>Todo lo que mueve un pasanaku, en un solo lugar.</h2>
      <p class="bajada">
        Grupos, aportes, cumplimiento y contabilidad conectados en tiempo real a los servicios de AportaYa.
      </p>
      <ul>
        @for (item of capacidades; track item; let i = $index) {
          <li [style.--i]="i">
            <span class="tilde"><ap-icono nombre="verificado" tamano="s4" /></span>
            {{ item }}
          </li>
        }
      </ul>
    </div>

    <p class="pie">© {{ anio }} AportaYa · Uso interno · Todas las acciones quedan auditadas</p>
  `,
  styles: `
    :host {
      position: relative; overflow: hidden; display: none; flex-direction: column; justify-content: space-between;
      padding: var(--s7) var(--s6); color: var(--white);
      border-right: var(--borde-fino) solid color-mix(in srgb, var(--white) 10%, transparent);
      background:
        radial-gradient(circle at 15% 10%, color-mix(in srgb, var(--g400) 35%, transparent), transparent 55%),
        radial-gradient(circle at 90% 95%, color-mix(in srgb, var(--g500) 45%, transparent), transparent 50%),
        linear-gradient(160deg, var(--g800), var(--g900));
    }
    @media (min-width: 64rem) { :host { display: flex; width: 46%; } }
    p, h2, ul { margin: 0; }
    .identidad { display: flex; align-items: center; gap: var(--s3); }
    .monograma {
      display: grid; place-items: center; width: 2.75rem; height: 2.75rem; border-radius: var(--r-md);
      font: var(--t-titulo-2); background: color-mix(in srgb, var(--white) 12%, transparent);
      border: var(--borde-fino) solid color-mix(in srgb, var(--white) 18%, transparent);
    }
    .nombre { font: var(--t-cuerpo-fuerte); letter-spacing: .08em; text-transform: uppercase; }
    .sub, .pie { font: var(--t-cuerpo-chico); color: color-mix(in srgb, var(--white) 68%, transparent); }
    .mensaje { max-width: 28rem; }
    .ceja { font: var(--t-etiqueta); letter-spacing: .14em; text-transform: uppercase; color: var(--g200); }
    h2 { margin-top: var(--s3); font: var(--t-cifra); font-size: 2.1rem; line-height: 1.12; letter-spacing: -.02em; text-wrap: balance; }
    .bajada { margin-top: var(--s4); font: var(--t-cuerpo); color: color-mix(in srgb, var(--white) 78%, transparent); }
    ul { list-style: none; padding: 0; margin-top: var(--s6); display: grid; gap: var(--s3); }
    li {
      display: flex; align-items: center; gap: var(--s3); font: var(--t-cuerpo);
      color: color-mix(in srgb, var(--white) 90%, transparent);
      animation: entrar .5s cubic-bezier(.2, .8, .2, 1) both; animation-delay: calc(120ms + var(--i) * 70ms);
    }
    .tilde {
      display: grid; place-items: center; width: 1.75rem; height: 1.75rem; flex: none; border-radius: var(--r-sm);
      color: var(--g200); background: color-mix(in srgb, var(--white) 10%, transparent);
    }
    @keyframes entrar { from { opacity: 0; transform: translateY(.4rem); } }
    @media (prefers-reduced-motion: reduce) { li { animation: none; } }
  `,
})
export class PanelDeMarca {
  protected readonly capacidades = CAPACIDADES
  protected readonly anio = new Date().getFullYear()
}
