import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Alerta } from '@aportaya/ui/alerta/alerta';
import { Boton } from '@aportaya/ui/boton/boton';
import { map } from 'rxjs';
import { Sesion } from '../../../nucleo/sesion';
import { DialogoDeActo } from '../../sistemas/compartido/dialogo-de-acto';
import { tieneAlguno } from '../compartido/acceso';
import { accionesDeParticipantes, type CampoRevelable } from '../dominio/cu134-acciones';
import { textosAcciones } from '../textos-acciones-participantes';

const T = textosAcciones.revelar;
const NOMBRE: Record<CampoRevelable, string> = { TELEFONO: 'teléfono', CORREO: 'correo' };

/**
 * CU-134 · revelar el teléfono o el correo completo de un participante, con segundo factor y justificación (R-SEG-12).
 * El valor vive solo en memoria de esta pantalla: no se guarda, no se registra y se pierde al salir.
 */
@Component({
  selector: 'ap-revelar-dato',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, DialogoDeActo],
  template: `
    @if (habilitado()) {
      <div class="acciones">
        <ap-boton tamano="sm" icono="candado" (pulsado)="campo.set('TELEFONO')">{{
          T.telefono
        }}</ap-boton>
        @if (tieneCorreo()) {
          <ap-boton tamano="sm" icono="candado" (pulsado)="campo.set('CORREO')">{{
            T.correo
          }}</ap-boton>
        }
      </div>
    }
    @if (revelado(); as r) {
      <ap-alerta tono="aviso" [titulo]="T.revelado(nombre(r.campo))">{{ r.valor }}</ap-alerta>
    }
    @if (campo(); as c) {
      <ap-dialogo-de-acto
        [titulo]="T.titulo(nombre(c))"
        [textoDeConfirmar]="T.confirmar"
        [efecto]="T.efecto"
        [etiquetaDeMotivo]="T.motivo"
        [minimoDeMotivo]="10"
        [maximoDeMotivo]="300"
        [accion]="accion(c)"
        [abierto]="true"
        (abiertoChange)="!$event && campo.set(null)"
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
export class RevelarDato {
  readonly usuarioId = input.required<string>();
  readonly tieneCorreo = input(false);

  protected readonly T = T;
  private readonly sesion = inject(Sesion);
  private readonly api = accionesDeParticipantes();
  protected readonly campo = signal<CampoRevelable | null>(null);
  protected readonly revelado = signal<{ campo: CampoRevelable; valor: string } | null>(null);
  protected readonly habilitado = computed(() =>
    tieneAlguno(this.sesion, ['DATOS_SENSIBLES_LEER']),
  );
  protected readonly nombre = (c: CampoRevelable) => NOMBRE[c];

  protected accion(campo: CampoRevelable) {
    return (evidencia: string, motivo: string) =>
      this.api.revelar(this.usuarioId(), campo, motivo, evidencia).pipe(
        map((dato) => {
          this.revelado.set({ campo, valor: dato.valor });
          return dato;
        }),
      );
  }
}
