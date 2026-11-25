import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { Boton } from '@aportaya/ui/boton/boton';
import { Campo } from '@aportaya/ui/campo/campo';
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado';
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla';
import { formatearFecha } from '@aportaya/ui/fecha/fecha';
import { Icono } from '@aportaya/ui/icono/icono';
import { SeccionDeExpediente } from '@aportaya/ui/seccion-de-expediente/seccion-de-expediente';
import type { ExpedienteDeMedida } from 'clientes/angular/identidad/model/expedienteDeMedida';
import type { FichaDeParticipante as Ficha } from 'clientes/angular/identidad/model/fichaDeParticipante';
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista';
import { MarcoDeCumplimiento } from '../compartido/marco';
import { fichaDe, justificacionValida } from '../dominio/cu134-participantes';
import { textosParticipantes } from '../textos-participantes';

const T = textosParticipantes;

/**
 * CU-134 · ficha del participante. Se abre recién con la justificación escrita: es la que queda en el registro de
 * accesos. Muestra el contacto enmascarado y las medidas con su línea de tiempo; no muestra saldo, movimientos ni
 * casos de cumplimiento (el contrato no los devuelve). Revelar un dato, decidir y resolver exigen segundo factor y
 * todavía no tienen pantalla (H3.S6.M3).
 */
@Component({
  selector: 'ap-ficha-de-participante',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MarcoDeCumplimiento,
    Campo,
    Boton,
    EstadoDePantalla,
    SeccionDeExpediente,
    ChipEstado,
    Icono,
  ],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.ficha.titulo" [proposito]="t.proposito" icono="persona">
      @if (!motivo()) {
        <form class="justificar" (submit)="$event.preventDefault(); abrir()">
          <p>{{ t.ficha.pedirJustificacion }}</p>
          <ap-campo
            [etiqueta]="t.justificacion"
            [ayuda]="t.ayudaJustificacion"
            [(valor)]="justificacion"
            [error]="error()"
          />
          <ap-boton variante="primario" tipo="submit" icono="persona">{{ t.ficha.abrir }}</ap-boton>
        </form>
      } @else {
        <ap-estado-de-pantalla
          [recurso]="ficha"
          [mensajeVacio]="t.ficha.noEncontrada"
          [etiquetaDeCarga]="t.ficha.cargando"
          (reintentar)="ficha.reload()"
        >
          @if (ficha.hasValue() && ficha.value(); as f) {
            <section aria-labelledby="cuenta" class="bloque">
              <h2 id="cuenta"><ap-icono nombre="persona" tamano="s4" />{{ t.ficha.cuenta }}</h2>
              <ap-seccion-de-expediente [titulo]="t.ficha.datos" [datos]="datosDe(f)">
                <ap-chip-estado [tono]="f.estado === 'ACTIVO' ? 'ok' : 'aviso'">{{
                  estado(f.estado)
                }}</ap-chip-estado>
              </ap-seccion-de-expediente>
              <ap-seccion-de-expediente [titulo]="t.ficha.contacto" [datos]="contactoDe(f)" />
            </section>
            <section aria-labelledby="medidas" class="bloque">
              <h2 id="medidas"><ap-icono nombre="escudo" tamano="s4" />{{ t.ficha.medidas }}</h2>
              @for (m of f.medidas; track m.expedienteId) {
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
                </article>
              } @empty {
                <p class="indicacion">{{ t.ficha.sinMedidas }}</p>
              }
            </section>
          }
        </ap-estado-de-pantalla>
      }
    </ap-marco-de-cumplimiento>
  `,
  styles: `
    .bloque {
      display: grid;
      gap: var(--s3);
    }
    .justificar {
      display: grid;
      gap: var(--s3);
      max-width: 40rem;
    }
    .justificar p,
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
export class FichaDeParticipante {
  constructor() {
    inject(ExplicadorDeVista).fijar(T.proposito);
  }
  protected readonly t = T;
  readonly usuarioId = input.required<string>();
  protected readonly justificacion = signal('');
  protected readonly error = signal<string | undefined>(undefined);
  protected readonly motivo = signal<string | null>(null);
  protected readonly ficha = fichaDe(this.usuarioId, this.motivo);
  protected readonly fecha = (iso: string) => formatearFecha(iso);
  protected readonly texto = (mapa: Record<string, string>, codigo: string) => mapa[codigo] ?? codigo
  protected readonly estado = (codigo: string) => T.estados[codigo] ?? codigo;

  protected abrir(): void {
    if (!justificacionValida(this.justificacion())) {
      this.error.set(T.errorJustificacion);
      return;
    }
    this.error.set(undefined);
    this.motivo.set(this.justificacion().trim());
  }

  protected datosDe(f: Ficha) {
    return [
      { nombre: T.columnas.codigoPublico, valor: f.codigoPublico },
      { nombre: T.columnas.nombreAbreviado, valor: f.nombreCompleto },
      { nombre: T.columnas.nivelKyc, valor: f.nivelKyc },
      { nombre: 'Registro', valor: formatearFecha(f.fechaRegistro) },
    ];
  }

  protected contactoDe(f: Ficha) {
    return [
      { nombre: T.ficha.telefono, valor: f.telefonoEnmascarado },
      { nombre: T.ficha.correo, valor: f.correoEnmascarado ?? '—' },
    ];
  }

  protected plazos(m: ExpedienteDeMedida): string {
    const partes = [
      m.fechaLimiteDescargo ? `Descargo hasta ${formatearFecha(m.fechaLimiteDescargo)}` : null,
      m.fechaLimiteApelacion ? `Apelación hasta ${formatearFecha(m.fechaLimiteApelacion)}` : null,
      `Prescribe el ${formatearFecha(m.prescribeEn)}`,
    ];
    return partes.filter(Boolean).join(' · ');
  }
}
