import { ChangeDetectionStrategy, Component, inject, linkedSignal, resource, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Interruptor as InterruptorUI } from '@aportaya/ui/interruptor/interruptor'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { puedeConfirmar } from '../dominio/datos-simulados'
import { PUERTO_DESPLIEGUES, type Despliegue, type DespliguesEInterruptores, type Interruptor } from '../dominio/puertos'
import { textosSistemas } from '../textos'

/**
 * Despliegues recientes + interruptores. El que toca dinero (`tocaDinero`) no se activa
 * con un clic: pide un correo de confirmación distinto al de quien lo solicitó — la
 * interfaz impide confirmarlo solo/a (`puedeConfirmar`), modelando doble persona.
 *
 * `interruptores` es un `linkedSignal`: deriva del recurso pero admite mutación local
 * (activar/pedir/confirmar) sin perder el vínculo — si el recurso se recarga
 * (`reintentar`), vuelve a tomar los valores frescos.
 */
@Component({
  selector: 'ap-pantalla-despliegues',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, TablaDeDatos, ChipEstado, InterruptorUI, Alerta, Dialogo, Campo, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>

      <ap-estado-de-pantalla [recurso]="despliegues" [vacio]="esVacio" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="despliegues.reload()">
        @if (despliegues.hasValue() && despliegues.value(); as datos) {
          <section>
            <ap-tabla-de-datos [titulo]="'Despliegues'" [columnas]="columnasDespliegue" [filas]="datos.despliegues">
              <ng-template #celda let-fila let-columna="columna">
                @if (columna.clave === 'estado') {
                  <ap-chip-estado [tono]="fila.estado === 'exitoso' ? 'ok' : fila.estado === 'revertido' ? 'error' : 'info'">{{ fila.estado }}</ap-chip-estado>
                } @else {
                  {{ fila[columna.clave] }}
                }
              </ng-template>
            </ap-tabla-de-datos>
          </section>

          <section class="interruptores">
            <h2>Interruptores</h2>
            @for (i of interruptores(); track i.id) {
              <div class="fila">
                <ap-interruptor [activo]="i.activo" [deshabilitado]="i.tocaDinero" (activoChange)="cambiar(i, $event)">{{ i.nombre }}</ap-interruptor>
                @if (i.tocaDinero) {
                  <button type="button" class="pedir-cambio" (click)="pedirCambio(i)">{{ i.activo ? 'Pedir apagarlo' : 'Pedir activarlo' }} (toca dinero)</button>
                }
              </div>
              @if (solicitudPendiente()?.id === i.id) {
                <ap-alerta tono="aviso">{{ t.confirmarPrimeraPersona }}</ap-alerta>
              }
            }
          </section>
        }
      </ap-estado-de-pantalla>

      <ap-dialogo [titulo]="t.dobleConfirmacionTitulo" [textoDeConfirmar]="t.confirmarSegundaPersona" [abierto]="dialogoAbierto()" (confirmar)="confirmar()" (cancelar)="cancelar()">
        <p>{{ t.dobleConfirmacionCuerpo }}</p>
        <ap-campo etiqueta="Correo de quien confirma" tipo="email" [(valor)]="correoConfirmante" />
        @if (correoConfirmante() && !puedeConfirmarAhora()) {
          <ap-alerta tono="error">No podés confirmar el mismo cambio que pediste. Necesitamos el correo de otra persona.</ap-alerta>
        }
      </ap-dialogo>
    </main>
  `,
  styles: `main { padding: 0; max-width: 60rem; display: flex; flex-direction: column; gap: var(--s5); } h1 { margin-bottom: 0; } .fila { display: flex; align-items: center; gap: var(--s3); } button { background: transparent; border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); padding: 0 var(--s3); min-height: var(--area-tactil); cursor: pointer; }`,
})
export class PantallaDespliegues {
  protected readonly t = textosSistemas.despliegues
  private readonly puerto = inject(PUERTO_DESPLIEGUES)
  protected readonly despliegues = resource({ loader: () => this.puerto.obtener() })
  protected readonly esVacio = (datos: DespliguesEInterruptores) => datos.despliegues.length === 0 && datos.interruptores.length === 0
  protected readonly columnasDespliegue: Columna<Despliegue>[] = [
    { clave: 'servicio', titulo: 'Servicio', ordenable: true },
    { clave: 'version', titulo: 'Versión' },
    { clave: 'desplegadoEl', titulo: 'Desplegado el' },
    { clave: 'desplegadoPor', titulo: 'Por' },
    { clave: 'estado', titulo: 'Estado' },
  ]

  protected readonly interruptores = linkedSignal<Interruptor[]>(() => this.despliegues.value()?.interruptores ?? [])
  protected readonly solicitudPendiente = signal<{ id: string; solicitante: string } | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly correoConfirmante = signal('')
  private readonly SOLICITANTE_ACTUAL = 'operador.actual@aportaya.bo' // TODO: reemplazar por Sesion cuando exista el correo del operador en el token

  cambiar(i: Interruptor, activo: boolean): void {
    if (i.tocaDinero) return // no se toca directo: pasa por pedirCambio + doble confirmación
    this.interruptores.set(this.interruptores().map((x) => (x.id === i.id ? { ...x, activo } : x)))
  }

  pedirCambio(i: Interruptor): void {
    this.solicitudPendiente.set({ id: i.id, solicitante: this.SOLICITANTE_ACTUAL })
    this.correoConfirmante.set('')
    this.dialogoAbierto.set(true)
  }

  protected puedeConfirmarAhora(): boolean {
    const s = this.solicitudPendiente()
    if (!s) return false
    return puedeConfirmar(s.solicitante, this.correoConfirmante())
  }

  confirmar(): void {
    const s = this.solicitudPendiente()
    if (!s || !this.puedeConfirmarAhora()) return
    this.interruptores.set(this.interruptores().map((x) => (x.id === s.id ? { ...x, activo: !x.activo } : x)))
    this.solicitudPendiente.set(null)
    this.dialogoAbierto.set(false)
  }

  cancelar(): void {
    this.solicitudPendiente.set(null)
    this.dialogoAbierto.set(false)
  }
}
