import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import type { ReclamoDelConsumidor } from '../dominio/cu52-reclamo'
import { PantallaDeReclamo } from './pantalla-de-reclamo'

/**
 * Conecta la ruta con `PantallaDeReclamo`. **Supuesto declarado:** sin ruta HTTP para
 * CU-52/CU-53 en `cumplimiento.yaml` (ver `dominio/cu52-reclamo.ts`), arma un reclamo
 * de ejemplo con un plazo guardado fijo (10 días hábiles desde el registro, como
 * exige `R-CON-03`), para que la pantalla se pueda probar de punta a punta. Cuando el
 * contrato exista, solo este archivo cambia.
 */
@Component({
  selector: 'ap-contenedor-de-reclamo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PantallaDeReclamo],
  template: `<ap-pantalla-de-reclamo [reclamo]="reclamo()" />`,
})
export class ContenedorDeReclamo {
  readonly reclamoId = input.required<string>()
  protected readonly reclamo = computed<ReclamoDelConsumidor>(() => ({
    id: this.reclamoId(),
    registradoEn: '2026-09-01T10:00:00-04:00',
    plazoVenceEn: '2026-09-15T10:00:00-04:00',
    norma: 'ASFI · Reglamento de atención al consumidor financiero',
    respuesta: null,
  }))
}
