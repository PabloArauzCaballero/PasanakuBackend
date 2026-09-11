import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { EstadoServicio, serviciosSimulados } from '../dominio/datos-simulados'
import { textosSistemas } from '../textos'

const TONO: Record<EstadoServicio['estado'], 'ok' | 'aviso' | 'error'> = { operativo: 'ok', degradado: 'aviso', caido: 'error' }

/**
 * Estado de servicios y SLO con presupuesto de error. Sin contrato de "indicadores"
 * real (hueco declarado en `dominio/datos-simulados.ts`), se muestra con datos de
 * ejemplo fijos, igual que Prism serviría un contrato que todavía no existe.
 */
@Component({
  selector: 'ap-pantalla-servicios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'estado') {
            <ap-chip-estado [tono]="tonoDe(fila.estado)">{{ fila.estado }}</ap-chip-estado>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaServicios {
  protected readonly t = textosSistemas.servicios
  protected readonly filas = serviciosSimulados
  protected readonly columnas: Columna<EstadoServicio>[] = [
    { clave: 'nombre', titulo: 'Servicio', ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'disponibilidad30d', titulo: 'Disponibilidad 30d', numerica: true },
    { clave: 'presupuestoErrorRestante', titulo: 'Presupuesto de error restante', numerica: true },
    { clave: 'ultimaInterrupcion', titulo: 'Última interrupción' },
  ]
  protected tonoDe(e: EstadoServicio['estado']) {
    return TONO[e]
  }
}
