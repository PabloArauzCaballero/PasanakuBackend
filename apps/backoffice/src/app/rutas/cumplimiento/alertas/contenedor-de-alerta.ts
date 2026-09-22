import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import type { AlertaDeRiesgo } from '../dominio/cu97-alertas'
import { PantallaDeAlertas } from './pantalla-de-alertas'

/**
 * Conecta la ruta con `PantallaDeAlertas`. **Supuesto declarado:** sin ruta HTTP para
 * CU-97 en `cumplimiento.yaml` (ver `dominio/cu97-alertas.ts`), este contenedor arma
 * la alerta con datos de ejemplo a partir del id de la URL, en vez de dejar la
 * pantalla sin poder probarse. Cuando el contrato exista, solo este archivo cambia:
 * pasa a usar `httpResource` como `cu90-habilitacion.ts`.
 */
@Component({
  selector: 'ap-contenedor-de-alerta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PantallaDeAlertas],
  template: `<ap-pantalla-de-alertas [alerta]="alerta()" />`,
})
export class ContenedorDeAlerta {
  readonly alertaId = input.required<string>()
  protected readonly alerta = computed<AlertaDeRiesgo>(() => ({
    id: this.alertaId(),
    grupoId: 'grupo-demo',
    hechos: [
      { nombre: 'Aporte tardío', explicacion: 'El aporte del turno anterior se registró 6 días después de lo acordado.' },
      { nombre: 'Reclamo abierto', explicacion: 'Hay un reclamo del consumidor sin responder, dentro de su plazo.' },
    ],
    estimaciones: [
      { nombre: 'Puntaje de riesgo del modelo', explicacion: 'El modelo calcula un puntaje alto para este grupo, según los factores guardados en su calibración vigente.' },
    ],
    generadaEn: new Date().toISOString(),
  }))
}
