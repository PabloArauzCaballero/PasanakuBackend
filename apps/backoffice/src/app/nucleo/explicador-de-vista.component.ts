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
  styleUrl: './explicador-de-vista.component.css',
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
