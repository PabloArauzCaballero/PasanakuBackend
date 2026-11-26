import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  input,
  model,
  output,
  signal,
  untracked,
} from '@angular/core';
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto';
import { Campo } from '@aportaya/ui/campo/campo';
import { Casilla } from '@aportaya/ui/casilla/casilla';
import { Dialogo } from '@aportaya/ui/dialogo/dialogo';
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio';
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio';
import type { EntradaMedidaDeCuenta } from 'clientes/angular/identidad/model/entradaMedidaDeCuenta';
import {
  traducirErrorDeGobierno,
  type ErrorDeGobierno,
} from '../../sistemas/dominio/errores-de-gobierno';
import { accionesDeParticipantes } from '../dominio/cu134-acciones';
import { textosAcciones } from '../textos-acciones-participantes';
import { textosParticipantes } from '../textos-participantes';

const T = textosAcciones.proponer;
const opciones = (mapa: Record<string, string>): OpcionDeRadio[] =>
  Object.entries(mapa).map(([valor, texto]) => ({ valor, texto }));
/** El error del servidor va al campo que corresponde (regla 95.3.2). */
const CAMPO_DEL_CODIGO: Record<string, 'causal' | 'cautelar' | 'motivo'> = {
  'AP-CU134-04': 'causal',
  'AP-CU134-05': 'cautelar',
};

/**
 * CU-134 · proponer una medida sobre la cuenta (sin segundo factor: la decide otra persona, R-SEG-13). Causal y medida
 * de catálogo cerrado, motivo y al menos una evidencia. Sin doble envío; lo escrito se conserva si falla.
 */
@Component({
  selector: 'ap-proponer-medida',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AreaDeTexto, Campo, Casilla, Dialogo, ErrorDeEnvio, GrupoRadio],
  template: `
    <ap-dialogo
      [titulo]="T.titulo"
      [textoDeConfirmar]="enviando() ? 'Proponiendo…' : T.confirmar"
      [cargando]="enviando()"
      [confirmarDeshabilitado]="enviando()"
      [puedeDescartar]="puedeCerrar"
      [(abierto)]="abierto"
      (confirmar)="enviar()"
      (cancelar)="abierto.set(false)"
    >
      <div class="campos">
        <ap-grupo-radio [etiqueta]="T.causal" [opciones]="causales" [(elegido)]="causal" />
        @if (errorDe('causal'); as e) {
          <p class="error" role="alert">{{ e }}</p>
        }
        <ap-grupo-radio [etiqueta]="T.medida" [opciones]="medidas" [(elegido)]="medida" />
        <ap-casilla [(marcada)]="cautelar">{{ T.cautelar }}</ap-casilla>
        @if (errorDe('cautelar'); as e) {
          <p class="error" role="alert">{{ e }}</p>
        }
        <ap-area-de-texto
          [etiqueta]="T.motivo"
          [ayuda]="T.ayudaMotivo"
          [maximo]="500"
          [(valor)]="motivo"
          [error]="errorDe('motivo')"
        />
        <ap-campo
          [etiqueta]="T.evidencia"
          [ayuda]="T.ayudaEvidencia"
          [(valor)]="evidencia"
          [error]="errorDe('evidencia')"
        />
      </div>
      <ap-error-de-envio [error]="errorGeneral()" />
    </ap-dialogo>
  `,
  styles: `
    .campos {
      display: grid;
      gap: var(--s4);
    }
    .error {
      margin: 0;
      color: var(--err-texto);
      font: var(--t-ayuda);
    }
  `,
})
export class ProponerMedida {
  readonly usuarioId = input.required<string>();
  readonly abierto = model(false);
  readonly propuesta = output<void>();

  protected readonly T = T;
  protected readonly causales = opciones(textosParticipantes.causales);
  protected readonly medidas = opciones(textosParticipantes.medidas);
  private readonly api = accionesDeParticipantes();
  protected readonly causal = signal<string | null>(null);
  protected readonly medida = signal<string | null>('SUSPENSION_TEMPORAL');
  protected readonly cautelar = signal(false);
  protected readonly motivo = signal('');
  protected readonly evidencia = signal('');
  protected readonly enviando = signal(false);
  private readonly errores = signal<
    Partial<Record<'causal' | 'cautelar' | 'motivo' | 'evidencia', string>>
  >({});
  private readonly error = signal<ErrorDeGobierno | null>(null);
  protected readonly puedeCerrar = () => !this.enviando();
  protected readonly errorGeneral = computed(() => this.error());
  protected readonly errorDe = (campo: 'causal' | 'cautelar' | 'motivo' | 'evidencia') =>
    this.errores()[campo];

  constructor() {
    effect(() => {
      if (!this.abierto()) return;
      untracked(() => {
        this.errores.set({});
        this.error.set(null);
      });
    });
  }

  protected enviar(): void {
    if (this.enviando()) return;
    const errores: Partial<Record<'causal' | 'motivo' | 'evidencia', string>> = {};
    if (!this.causal()) errores.causal = 'Elegí la causal.';
    if (this.motivo().trim().length < 30) errores.motivo = T.errorMotivo;
    if (!this.evidencia().trim()) errores.evidencia = T.errorEvidencia;
    this.errores.set(errores);
    this.error.set(null);
    if (Object.keys(errores).length) return;
    const entrada: EntradaMedidaDeCuenta = {
      causal: this.causal() as EntradaMedidaDeCuenta['causal'],
      medida: this.medida() as EntradaMedidaDeCuenta['medida'],
      cautelar: this.cautelar(),
      motivo: this.motivo().trim(),
      evidencias: [{ descripcion: this.evidencia().trim() }],
    };
    this.enviando.set(true);
    this.api.proponer(this.usuarioId(), entrada).subscribe({
      next: () => {
        this.enviando.set(false);
        this.abierto.set(false);
        this.propuesta.emit();
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        const traducido = traducirErrorDeGobierno(
          e,
          'No se pudo proponer la medida. Probá de nuevo.',
        );
        const campo = traducido.codigo ? CAMPO_DEL_CODIGO[traducido.codigo] : undefined;
        if (campo) this.errores.set({ [campo]: traducido.mensaje });
        else this.error.set(traducido);
      },
    });
  }
}
