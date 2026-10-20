import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core'
import { NavigationEnd, Router } from '@angular/router'
import { filter } from 'rxjs'
import { Icono } from '@aportaya/ui/icono/icono'
import { ExplicadorDeVista } from './explicador-de-vista'

/**
 * Componente que presenta el explicador de la vista y del módulo en dos planos:
 * Negocio y Sistemas (patrón Atlas Admin Portal).
 *
 * Plegado por omisión: se muestra como una barra sobria y elegante con ícono de libro,
 * el nombre del módulo y el texto de ayuda. Al desplegarse, revela dos bloques:
 * - Explicación de negocio (tono cálido / balanza): valor al usuario, decisiones y riesgo prevenido.
 * - Cómo funciona (Sistemas) (tono tecnológico / servidor): endpoints, tablas SQL, eventos y seguridad.
 *
 * Al navegar a otra ruta se colapsa automáticamente para no entorpecer el trabajo del operador.
 */
@Component({
  selector: 'ap-explicador-de-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: {
    '[class.abierto]': 'abierto()',
    '[class.oculto]': '!explicacion()',
  },
  template: `
    @if (explicacion(); as exp) {
      <div class="contenedor" [class.abierto]="abierto()">
        <button
          type="button"
          class="barra-gatillo"
          [attr.aria-expanded]="abierto()"
          (click)="alternar()"
        >
          <span class="info-resumen">
            <ap-icono nombre="libro" tamano="s4" class="icono-libro" />
            <span class="texto-resumen">
              <strong class="modulo-nombre">{{ exp.modulo.modulo }}</strong>
              <span class="separador">—</span>
              <span class="guia-texto">qué es este módulo y esta vista (para qué sirve y cómo funciona)</span>
            </span>
          </span>
          <ap-icono
            nombre="chevronAbajo"
            tamano="s4"
            class="icono-chevron"
            [class.rotado]="abierto()"
          />
        </button>

        @if (abierto()) {
          <div class="panel-detallado animate-fade-in">
            <div class="bloques-grilla">
              <!-- Bloque Negocio -->
              <section class="bloque bloque-negocio" aria-labelledby="titulo-negocio">
                <header class="bloque-cabecera">
                  <ap-icono nombre="verificado" tamano="s4" class="icono-negocio" />
                  <h3 id="titulo-negocio" class="bloque-titulo titulo-negocio">
                    Explicación de negocio
                  </h3>
                </header>
                <div class="bloque-cuerpo">
                  <p class="parrafo">
                    <strong class="etiqueta-plano">Módulo:</strong>
                    <span class="texto-plano">{{ exp.modulo.negocio }}</span>
                  </p>
                  @if (exp.vista?.negocio; as textoVista) {
                    <p class="parrafo parrafo-vista">
                      <strong class="etiqueta-plano">Esta vista:</strong>
                      <span class="texto-plano">{{ textoVista }}</span>
                    </p>
                  }
                </div>
              </section>

              <!-- Bloque Sistemas -->
              <section class="bloque bloque-sistemas" aria-labelledby="titulo-sistemas">
                <header class="bloque-cabecera">
                  <ap-icono nombre="baseDeDatos" tamano="s4" class="icono-sistemas" />
                  <h3 id="titulo-sistemas" class="bloque-titulo titulo-sistemas">
                    Cómo funciona (Sistemas)
                  </h3>
                </header>
                <div class="bloque-cuerpo">
                  <p class="parrafo">
                    <strong class="etiqueta-plano">Módulo:</strong>
                    <span class="texto-plano">{{ exp.modulo.sistemas }}</span>
                  </p>
                  @if (exp.vista?.sistemas; as textoSistemas) {
                    <p class="parrafo parrafo-vista">
                      <strong class="etiqueta-plano">Esta vista:</strong>
                      <span class="texto-plano">{{ textoSistemas }}</span>
                    </p>
                  }
                </div>
              </section>
            </div>

            @if (servicio.texto(); as notaManual) {
              <div class="nota-adicional">
                <ap-icono nombre="bombilla" tamano="s4" class="icono-nota" />
                <span class="texto-nota"><strong>Nota operativa:</strong> {{ notaManual }}</span>
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      margin-bottom: var(--s3);
    }
    :host(.oculto) {
      display: none;
    }
    .contenedor {
      border: var(--borde-fino) solid color-mix(in srgb, var(--brand) 18%, var(--border));
      border-radius: var(--r-lg);
      background: color-mix(in srgb, var(--brand-bg) 30%, var(--surface));
      transition: all var(--mov-micro) var(--curva-llega);
    }
    .contenedor.abierto {
      border-color: color-mix(in srgb, var(--brand) 32%, var(--border));
      background: var(--surface);
      box-shadow: var(--sombra-2);
    }
    .barra-gatillo {
      display: flex;
      width: 100%;
      align-items: center;
      justify-content: space-between;
      gap: var(--s3);
      min-height: calc(var(--area-tactil) - var(--s1));
      padding: 0 var(--s3);
      border: 0;
      border-radius: var(--r-lg);
      background: transparent;
      color: var(--text-2);
      font: var(--t-campo-etiqueta);
      text-align: left;
      cursor: pointer;
      transition: background-color var(--mov-micro) ease, color var(--mov-micro) ease;
    }
    .barra-gatillo:hover {
      background: color-mix(in srgb, var(--brand-bg) 60%, transparent);
      color: var(--text);
    }
    .barra-gatillo:focus-visible {
      outline: var(--borde-foco) solid var(--g300);
      outline-offset: var(--borde-desfase);
    }
    .info-resumen {
      display: flex;
      align-items: center;
      gap: var(--s2);
      min-width: 0;
    }
    .icono-libro {
      color: var(--brand-texto);
      flex: none;
    }
    .texto-resumen {
      font-size: 0.8125rem;
      line-height: 1.4;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .modulo-nombre {
      color: var(--brand-texto);
      font-weight: 600;
    }
    .separador {
      margin: 0 var(--s1);
      color: var(--text-3);
    }
    .guia-texto {
      color: var(--text-2);
    }
    .icono-chevron {
      color: var(--brand-texto);
      flex: none;
      margin-left: auto;
      transition: transform var(--mov-entrada) var(--curva-llega);
    }
    .icono-chevron.rotado {
      transform: rotate(180deg);
    }
    .panel-detallado {
      border-top: var(--borde-fino) solid var(--border);
      padding: var(--s4);
      display: grid;
      gap: var(--s3);
    }
    .bloques-grilla {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--s4);
    }
    @media (min-width: 48rem) {
      .bloques-grilla {
        grid-template-columns: 1fr 1fr;
      }
    }
    .bloque {
      border-radius: var(--r-md);
      padding: var(--s3) var(--s4);
      display: flex;
      flex-direction: column;
      gap: var(--s2);
    }
    .bloque-negocio {
      border: var(--borde-fino) solid color-mix(in srgb, var(--warn) 35%, var(--border));
      background: color-mix(in srgb, var(--warn-bg) 45%, var(--surface));
    }
    .bloque-sistemas {
      border: var(--borde-fino) solid color-mix(in srgb, var(--brand) 30%, var(--border));
      background: color-mix(in srgb, var(--brand-bg) 35%, var(--surface));
    }
    .bloque-cabecera {
      display: flex;
      align-items: center;
      gap: var(--s2);
    }
    .bloque-titulo {
      margin: 0;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }
    .titulo-negocio {
      color: color-mix(in srgb, var(--warn) 85%, var(--text));
    }
    .icono-negocio {
      color: var(--warn);
    }
    .titulo-sistemas {
      color: var(--brand-texto);
    }
    .icono-sistemas {
      color: var(--brand-texto);
    }
    .bloque-cuerpo {
      display: flex;
      flex-direction: column;
      gap: var(--s2);
    }
    .parrafo {
      margin: 0;
      font-size: 0.8125rem;
      line-height: 1.55;
      color: var(--text);
    }
    .parrafo-vista {
      padding-top: var(--s1);
      border-top: var(--borde-fino) solid color-mix(in srgb, currentColor 12%, transparent);
    }
    .etiqueta-plano {
      color: var(--text-2);
      margin-right: var(--s1);
      font-weight: 600;
    }
    .texto-plano {
      color: var(--text);
    }
    .nota-adicional {
      display: flex;
      align-items: flex-start;
      gap: var(--s2);
      padding: var(--s2) var(--s3);
      border-radius: var(--r-md);
      background: color-mix(in srgb, var(--info-bg) 50%, var(--surface));
      border: var(--borde-fino) solid color-mix(in srgb, var(--info) 25%, var(--border));
      font-size: 0.8125rem;
      color: var(--text);
      line-height: 1.5;
    }
    .icono-nota {
      color: var(--info-texto);
      margin-top: 2px;
      flex: none;
    }
  `,
})
export class ExplicadorDeVistaComponent {
  protected readonly servicio = inject(ExplicadorDeVista)
  private readonly router = inject(Router)
  protected readonly abierto = signal(false)

  protected readonly explicacion = computed(() => this.servicio.explicacion())

  constructor() {
    const sub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        this.abierto.set(false)
      })
    inject(DestroyRef).onDestroy(() => sub.unsubscribe())
  }

  protected alternar(): void {
    this.abierto.set(!this.abierto())
  }
}
