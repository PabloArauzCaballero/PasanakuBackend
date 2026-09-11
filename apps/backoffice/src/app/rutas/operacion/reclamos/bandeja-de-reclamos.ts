import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { RelojDePlazo } from '@aportaya/ui/reloj-de-plazo/reloj-de-plazo'
import { Monto } from '@aportaya/ui/monto/monto'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Sesion } from '../../../nucleo/sesion'
import { bandejaDeReclamos, ordenadosPorVencimiento, vencido, type EstadoReclamo, type ReclamoDeBandeja } from '../dominio/cu52-reclamos'
import { textosOperacion } from '../textos'

const TONO_POR_ESTADO: Record<EstadoReclamo, 'ok' | 'aviso' | 'error' | 'info' | 'neutro'> = {
  INGRESADO: 'info',
  EN_ANALISIS: 'aviso',
  RESPONDIDO: 'ok',
  CERRADO: 'neutro',
  ELEVADO: 'error',
}

/**
 * D-18 · Bandeja de reclamos (CU-52), ordenada por plazo: el que vence antes va primero.
 * Segregación de funciones: **cualquier operador la ve**; solo quien tiene
 * `RECLAMO_ATENDER` puede abrir el diálogo de respuesta — el mismo permiso que CU-52
 * exige del lado del backend para `reclamo.respondido`, así el 403 real nunca es la
 * primera línea de defensa, solo la confirma.
 */
@Component({
  selector: 'ap-bandeja-de-reclamos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, BandaDeProposito, ChipEstado, RelojDePlazo, Monto, Boton, Dialogo],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla
        [recurso]="reclamos"
        [vacio]="esVacio"
        [mensajeVacio]="t.sinReclamos"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="reclamos.reload()"
      >
        @if (reclamos.hasValue()) {
          <ul class="lista">
            @for (r of ordenados(); track r.reclamoId) {
              <li [class.vencido]="vencido(r, ahoraIso)">
                <div class="cabecera">
                  <strong>{{ r.codigo }}</strong>
                  <ap-chip-estado [tono]="TONO[r.estado]">{{ ETIQUETA[r.estado] }}</ap-chip-estado>
                </div>
                <p class="categoria">{{ r.categoria }} · {{ r.canalIngreso }}</p>
                @if (r.montoReclamado) { <ap-monto [monto]="r.montoReclamado.monto" [moneda]="r.montoReclamado.moneda" [etiqueta]="t.montoReclamado" /> }
                <ap-reloj-de-plazo [etiqueta]="t.plazo" [venceIso]="r.plazoRespuesta" [ahoraIso]="ahoraIso" [norma]="t.norma" />
                @if (puedeAtender()) {
                  <ap-boton [deshabilitado]="r.estado === 'CERRADO'" (pulsado)="abrirConfirmacion(r)">{{ t.responder }}</ap-boton>
                }
              </li>
            }
          </ul>
        }
      </ap-estado-de-pantalla>
    </main>

    @if (seleccionado(); as r) {
      <ap-dialogo
        [titulo]="'Responder ' + r.codigo"
        [textoDeConfirmar]="'Confirmar respuesta de ' + r.codigo"
        [abierto]="dialogoAbierto()"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmarRespuesta()"
        (cancelar)="cerrarConfirmacion()"
      >
        <p>{{ t.confirmarResponder(r.codigo) }}</p>
      </ap-dialogo>
    }
  `,
  styles: `
    main { padding: var(--s5); max-width: 48rem; }
    h1 { margin-bottom: var(--s4); }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); }
    li { padding: var(--s4); background: var(--surface); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); display: grid; gap: var(--s2); }
    li.vencido { border-color: var(--err); }
    .cabecera { display: flex; justify-content: space-between; align-items: center; gap: var(--s2); }
    .categoria { margin: 0; color: var(--text-2); }
  `,
})
export class BandejaDeReclamos {
  private readonly sesion = inject(Sesion)
  protected readonly t = textosOperacion.reclamos
  protected readonly reclamos = bandejaDeReclamos()
  protected readonly TONO = TONO_POR_ESTADO
  protected readonly ETIQUETA: Record<EstadoReclamo, string> = textosOperacion.reclamos.estados
  protected readonly ahoraIso = new Date().toISOString()
  protected readonly seleccionado = signal<ReclamoDeBandeja | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly puedeAtender = computed(() => this.sesion.puede('RECLAMO_ATENDER'))

  protected readonly ordenados = computed(() => (this.reclamos.hasValue() ? ordenadosPorVencimiento(this.reclamos.value()!) : []))
  protected readonly esVacio = (lista: ReclamoDeBandeja[]) => lista.length === 0
  protected readonly vencido = vencido

  abrirConfirmacion(r: ReclamoDeBandeja): void {
    this.seleccionado.set(r)
    this.dialogoAbierto.set(true)
  }

  cerrarConfirmacion(): void {
    this.dialogoAbierto.set(false)
    this.seleccionado.set(null)
  }

  confirmarRespuesta(): void {
    // El envío real queda pendiente del contrato `POST /reclamos/{id}/respuesta`
    // (ver el supuesto declarado en dominio/cu52-reclamos.ts). El diálogo ya exige
    // la confirmación con el código concreto delante, que es lo que pide el gate.
    this.cerrarConfirmacion()
    this.reclamos.reload()
  }
}
