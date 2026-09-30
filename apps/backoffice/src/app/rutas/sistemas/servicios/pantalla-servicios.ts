import { ChangeDetectionStrategy, Component, inject, resource } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { PUERTO_SERVICIOS, type EstadoServicio } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const TONO: Record<EstadoServicio['estado'], 'ok' | 'aviso' | 'error'> = { operativo: 'ok', degradado: 'aviso', caido: 'error' }

/**
 * Estado de servicios y SLO con presupuesto de error. Ya NO importa datos de ejemplo
 * directo (el hallazgo de H2): inyecta `PUERTO_SERVICIOS` y pinta los cuatro estados
 * con `EstadoDePantalla` — quién resuelve el puerto (simulado o "no disponible") lo
 * decide `sistemas.routes.ts` según el modo, nunca esta pantalla.
 */
@Component({
  selector: 'ap-pantalla-servicios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1 data-tutorial-id="sistemas-servicios">{{ t.titulo }}</h1>
      <ap-estado-de-pantalla
        data-tutorial-id="sistemas-servicios-estado"
        [recurso]="servicios"
        [vacio]="esVacio"
        [mensajeVacio]="t.vacio"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="servicios.reload()"
      >
        @if (servicios.hasValue() && servicios.value(); as filas) {
          <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [filas]="filas">
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'estado') {
                <ap-chip-estado [tono]="tonoDe(fila.estado)">{{ fila.estado }}</ap-chip-estado>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; } h1 { margin-bottom: var(--s4); }`,
})
export class PantallaServicios {
  protected readonly t = textosSistemas.servicios
  private readonly puerto = inject(PUERTO_SERVICIOS)
  protected readonly servicios = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (filas: EstadoServicio[]) => filas.length === 0
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
