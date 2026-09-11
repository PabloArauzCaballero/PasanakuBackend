import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import type { ActaDeComite } from '../dominio/cu94-acta'
import { PantallaDeActa } from './pantalla-de-acta'

/**
 * Conecta la ruta con `PantallaDeActa`. **Supuesto declarado:** sin ruta HTTP para
 * CU-94 en `cumplimiento.yaml` (ver `dominio/cu94-acta.ts`), arma un acta de ejemplo
 * EN CURSO (sin quórum todavía) para que el gate del carril —cerrar sin quórum queda
 * bloqueado— se pueda probar en la pantalla real. Cuando el contrato exista, solo
 * este archivo cambia.
 */
@Component({
  selector: 'ap-contenedor-de-acta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PantallaDeActa],
  template: `<ap-pantalla-de-acta [acta]="acta()" />`,
})
export class ContenedorDeActa {
  readonly actaId = input.required<string>()
  protected readonly acta = computed<ActaDeComite>(() => ({
    id: this.actaId(),
    comiteId: 'comite-cumplimiento',
    quorumMinimo: 3,
    composicionRequerida: ['OFICIAL_CUMPLIMIENTO', 'GERENCIA', 'DIRECTORIO'],
    asistentes: [
      { integranteId: 'u1', rol: 'OFICIAL_CUMPLIMIENTO', voto: 'A_FAVOR' },
      { integranteId: 'u2', rol: 'GERENCIA', voto: 'A_FAVOR' },
    ],
  }))
}
