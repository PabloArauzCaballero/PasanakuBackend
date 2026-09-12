import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { Sesion } from '../../../nucleo/sesion'
import {
  crearCierreDePeriodo,
  motivoParaNoAsentar,
  periodosDelEjercicio,
  puedeAsentarEn,
  puedeCerrar,
  sinPeriodos,
  type PeriodoContable,
} from '../dominio/cu100-periodos'
import { textosContabilidad } from '../textos'

/**
 * CU-100 · Abrir y cerrar el período contable. **Gate propio del carril B3: un período
 * cerrado se ve cerrado.** La acción de asentar no aparece sobre un período `CERRADO`
 * —no deshabilitada y muda, sino ausente y con el motivo escrito al lado—, porque la
 * corrección de un mes cerrado va en el mes abierto siguiente, con glosa, y ofrecer el
 * botón enseñaría lo contrario. La regla vive en `dominio/cu100-periodos.ts`
 * (`puedeAsentarEn`), no acá: la comparte con el alta de factura de CU-103.
 *
 * Cerrar es irreversible, así que pasa por `Dialogo` con el nombre del mes delante.
 */
@Component({
  selector: 'ap-pantalla-de-periodo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, ChipEstado, Dialogo, EstadoDePantalla, Fecha, Monto],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <p class="aviso">{{ t.cerrarIrreversible }}</p>
      <ap-estado-de-pantalla
        [recurso]="periodos"
        [vacio]="vacio"
        [mensajeVacio]="t.enCero"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="periodos.reload()"
      >
        <ul class="meses">
          @for (p of lista(); track p.periodoId) {
            <li class="mes" [class.cerrado]="!puedeAsentar(p)">
              <div class="cabecera">
                <h2>{{ p.nombre }}</h2>
                <ap-chip-estado [tono]="puedeAsentar(p) ? 'ok' : 'neutro'">{{ etiqueta(p) }}</ap-chip-estado>
              </div>
              @if (p.totalDebe && p.totalHaber) {
                <dl class="cuadre">
                  <dt>{{ t.debe }}</dt>
                  <dd><ap-monto [monto]="p.totalDebe.monto" [moneda]="p.totalDebe.moneda" [etiqueta]="t.debe" /></dd>
                  <dt>{{ t.haber }}</dt>
                  <dd><ap-monto [monto]="p.totalHaber.monto" [moneda]="p.totalHaber.moneda" [etiqueta]="t.haber" /></dd>
                </dl>
              }
              @if (p.cerradoEn) { <ap-fecha [iso]="p.cerradoEn" /> }
              @if (puedeAsentar(p)) {
                <ap-boton (pulsado)="asentar(p)">{{ t.asentar }}</ap-boton>
              } @else {
                <p class="motivo">{{ motivo(p) }}</p>
              }
              @if (puedeCerrarlo(p) && puedeOperarElCierre()) {
                <ap-boton variante="primario" (pulsado)="abrirConfirmacion(p)">{{ t.cerrar }}</ap-boton>
              }
            </li>
          }
        </ul>
      </ap-estado-de-pantalla>
    </main>

    @if (elegido(); as p) {
      <ap-dialogo
        [titulo]="t.cerrar + ' · ' + p.nombre"
        [textoDeConfirmar]="t.cerrar + ' ' + p.nombre"
        [destructivo]="true"
        [cargando]="cerrando()"
        [abierto]="dialogoAbierto()"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmarCierre(p)"
        (cancelar)="cerrarConfirmacion()"
      >
        <p>{{ t.confirmarCierre(p.nombre) }}</p>
      </ap-dialogo>
    }
  `,
  styles: `
    main { padding: var(--s5); max-width: 56rem; display: grid; gap: var(--s4); }
    h1, h2 { margin: 0; }
    h2 { font-size: 1em; }
    .aviso { color: var(--text-2); margin: 0; }
    .meses { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); }
    .mes { border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); padding: var(--s4); display: grid; gap: var(--s3); }
    .mes.cerrado { background: var(--surface-2); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    .cuadre { display: grid; grid-template-columns: auto auto; gap: var(--s1) var(--s4); margin: 0; justify-content: start; }
    dt { color: var(--text-3); } dd { margin: 0; }
    .motivo { color: var(--text-2); margin: 0; }
  `,
})
export class PantallaDePeriodo {
  protected readonly t = textosContabilidad.periodo
  readonly ejercicioId = input.required<string>()
  protected readonly periodos = periodosDelEjercicio(() => this.ejercicioId())
  protected readonly vacio = sinPeriodos
  /** `hasValue()` antes de `value()`: el contenido proyectado se evalúa aunque el recurso falle. */
  protected readonly lista = computed<PeriodoContable[]>(() => (this.periodos.hasValue() ? (this.periodos.value() ?? []) : []))
  private readonly sesion = inject(Sesion)
  protected readonly puedeOperarElCierre = computed(() => this.sesion.puede('CONTABILIDAD_ERP_CERRAR'))
  protected readonly elegido = signal<PeriodoContable | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly cerrando = signal(false)
  protected readonly asentadoEn = signal<string | null>(null)
  private readonly enviarCierre = crearCierreDePeriodo()

  protected puedeAsentar = puedeAsentarEn
  protected motivo = motivoParaNoAsentar
  protected puedeCerrarlo = (p: PeriodoContable) => puedeCerrar(p, this.lista())
  protected etiqueta = (p: PeriodoContable) => this.t.estados[p.estado]

  /** Deja el período elegido para el alta de asiento; el formulario es de CU-103. */
  protected asentar(p: PeriodoContable): void {
    this.asentadoEn.set(p.periodoId)
  }

  protected abrirConfirmacion(p: PeriodoContable): void {
    this.elegido.set(p)
    this.dialogoAbierto.set(true)
  }

  protected cerrarConfirmacion(): void {
    this.dialogoAbierto.set(false)
    this.elegido.set(null)
  }

  protected confirmarCierre(p: PeriodoContable): void {
    this.cerrando.set(true)
    this.enviarCierre(p.periodoId, `Cierre de ${p.nombre}`).subscribe({
      next: () => {
        this.cerrando.set(false)
        this.cerrarConfirmacion()
        this.periodos.reload()
      },
      error: () => this.cerrando.set(false),
    })
  }
}
