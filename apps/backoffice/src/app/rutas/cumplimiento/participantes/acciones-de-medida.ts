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
import type { ExpedienteDeMedida } from 'clientes/angular/identidad/model/expedienteDeMedida';
import type { Observable } from 'rxjs';
import { Sesion } from '../../../nucleo/sesion';
import { DialogoDeActo } from '../../sistemas/compartido/dialogo-de-acto';
import { tieneAlguno } from '../compartido/acceso';
import {
  accionesDeParticipantes,
  puedeDecidir,
  puedeLiberar,
  puedeResolver,
} from '../dominio/cu134-acciones';
import { textosAcciones } from '../textos-acciones-participantes';

const T = textosAcciones;

/** Un acto listo para el diálogo: lo propio de cada botón. */
type Acto = {
  titulo: string;
  confirmar: string;
  efecto: string;
  motivo: string;
  minimo: number;
  maximo: number;
  destructivo: boolean;
  accion: (evidencia: string, motivo: string) => Observable<unknown>;
};

/**
 * CU-134 · los actos sobre una medida, con segundo factor: confirmar o desestimar (en descargo), aceptar o rechazar la
 * apelación (en apelación) y liberar la entrega retenida (firme o cumplida, A-1). Cada botón aparece solo con su
 * permiso y en su etapa; el servidor lo vuelve a validar. Reusa `DialogoDeActo`: motivo, código y efecto dicho antes.
 */
@Component({
  selector: 'ap-acciones-de-medida',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, DialogoDeActo],
  template: `
    @if (botones().length) {
      <div class="acciones">
        @for (b of botones(); track b.texto) {
          <ap-boton tamano="sm" (pulsado)="acto.set(b.acto)">{{ b.texto }}</ap-boton>
        }
      </div>
    }
    @if (acto(); as a) {
      <ap-dialogo-de-acto
        [titulo]="a.titulo"
        [textoDeConfirmar]="a.confirmar"
        [destructivo]="a.destructivo"
        [efecto]="a.efecto"
        [etiquetaDeMotivo]="a.motivo"
        [minimoDeMotivo]="a.minimo"
        [maximoDeMotivo]="a.maximo"
        [accion]="a.accion"
        [abierto]="true"
        (abiertoChange)="!$event && acto.set(null)"
        (completado)="cambio.emit()"
      />
    }
  `,
  styles: `
    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: var(--s2);
    }
  `,
})
export class AccionesDeMedida {
  readonly medida = input.required<ExpedienteDeMedida>();
  /** Algo cambió: la ficha se vuelve a pedir. */
  readonly cambio = output<void>();

  private readonly sesion = inject(Sesion);
  private readonly api = accionesDeParticipantes();
  protected readonly acto = signal<Acto | null>(null);

  protected readonly botones = computed(() => {
    const m = this.medida();
    const id = m.expedienteId;
    const lista: { texto: string; acto: Acto }[] = [];
    const permite = (codigo: string) => tieneAlguno(this.sesion, [codigo]);
    if (puedeDecidir(m.estado) && permite('PARTICIPANTES_MEDIDA_DECIDIR')) {
      lista.push(
        { texto: T.decidir.confirmar, acto: this.decision(id, 'CONFIRMADA') },
        { texto: T.decidir.desestimar, acto: this.decision(id, 'DESESTIMADA') },
      );
    }
    if (puedeResolver(m.estado) && permite('PARTICIPANTES_APELACION_RESOLVER')) {
      lista.push(
        { texto: T.resolver.aceptar, acto: this.resolucion(id, 'ACEPTADA') },
        { texto: T.resolver.rechazar, acto: this.resolucion(id, 'RECHAZADA') },
      );
    }
    if (puedeLiberar(m.estado) && permite('PARTICIPANTES_MEDIDA_DECIDIR')) {
      lista.push({
        texto: T.liberar.boton,
        acto: {
          titulo: T.liberar.titulo,
          confirmar: T.liberar.boton,
          efecto: T.liberar.efecto,
          motivo: T.liberar.motivo,
          minimo: 20,
          maximo: 2000,
          destructivo: false,
          accion: (evidencia, motivo) => this.api.liberar(id, motivo, evidencia),
        },
      });
    }
    return lista;
  });

  private decision(id: string, resultado: 'CONFIRMADA' | 'DESESTIMADA'): Acto {
    const confirma = resultado === 'CONFIRMADA';
    return {
      titulo: confirma ? T.decidir.tituloConfirmar : T.decidir.tituloDesestimar,
      confirmar: confirma ? T.decidir.confirmar : T.decidir.desestimar,
      efecto: confirma ? T.decidir.efectoConfirmar : T.decidir.efectoDesestimar,
      motivo: T.decidir.motivo,
      minimo: 50,
      maximo: 4000,
      destructivo: confirma,
      accion: (evidencia, motivo) => this.api.decidir(id, resultado, motivo, evidencia),
    };
  }

  private resolucion(id: string, resultado: 'ACEPTADA' | 'RECHAZADA'): Acto {
    const acepta = resultado === 'ACEPTADA';
    return {
      titulo: acepta ? T.resolver.tituloAceptar : T.resolver.tituloRechazar,
      confirmar: acepta ? T.resolver.aceptar : T.resolver.rechazar,
      efecto: acepta ? T.resolver.efectoAceptar : T.resolver.efectoRechazar,
      motivo: T.resolver.motivo,
      minimo: 50,
      maximo: 4000,
      destructivo: !acepta,
      accion: (evidencia, motivo) => this.api.resolver(id, resultado, motivo, evidencia),
    };
  }
}
