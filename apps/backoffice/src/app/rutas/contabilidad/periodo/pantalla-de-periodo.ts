import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Sesion } from '../../../nucleo/sesion'
import {
  crearCierreDePeriodo,
  cuadra,
  motivoParaNoAsentar,
  periodosDelEjercicio,
  periodoVigente,
  puedeAsentarEn,
  puedeCerrar,
  sinPeriodos,
  type PeriodoContable,
} from '../dominio/cu100-periodos'
import { errorDelRecurso } from '../dominio/contrato-erp'
import { textosContabilidad } from '../textos'
import { PeriodoDeHoy } from './periodo-de-hoy'
import { SelectorDeEjercicios } from './selector-de-ejercicios'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ErrorTraducido } from '../../../nucleo/errores'

const EJERCICIO_INEXISTENTE = 'AP-CU100-05'

/**
 * CU-100 · Abrir y cerrar el período contable. Es donde aterriza la sección Contabilidad.
 *
 * **Sin identificador de demostración.** Sin `:ejercicioId` en la URL, la pantalla pregunta al ERP cuál es el período de
 * HOY (`GET /erp/periodos/vigente`) y muestra su ejercicio. Si el ERP responde `AP-CU100-04` (ningún período contiene hoy)
 * la pantalla lo dice y **no elige otro**: la persona puede consultar un ejercicio de la lista, que es una elección suya
 * y queda en la URL (`periodo/:ejercicioId`), pegable en un expediente.
 *
 * **Gate propio del carril: un período cerrado se ve cerrado.** La acción de asentar no aparece sobre un período
 * `CERRADO` —ausente y con el motivo escrito al lado—. La regla vive en `dominio/cu100-periodos.ts` (`puedeAsentarEn`).
 *
 * Los totales SELLADOS del cierre viajan SIN moneda en el contrato: se muestra el veredicto del cuadre, nunca un importe
 * con moneda supuesta (regla 91.1.2). Pedido a carril A en el reporte de L3.
 *
 * Cerrar es irreversible, así que pasa por `Dialogo` con el nombre del mes delante.
 */
@Component({
  selector: 'ap-pantalla-de-periodo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ErrorDeEnvio, BandaDeProposito, Boton, ChipEstado, Dialogo, EstadoDePantalla, Fecha, PeriodoDeHoy, SelectorDeEjercicios],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1 data-tutorial-id="contabilidad-periodo">{{ t.titulo }}</h1>
      <p class="aviso">{{ t.cerrarIrreversible }}</p>

      <ap-periodo-de-hoy [periodo]="valorVigente()" [error]="errorVigente()" />
      <ap-selector-de-ejercicios [elegido]="ejercicioElegido()" />

      @if (ejercicioElegido()) {
        <ap-estado-de-pantalla
          [recurso]="periodos"
          [vacio]="vacio"
          [mensajeVacio]="t.enCero"
          [etiquetaDeCarga]="t.cargando"
          (reintentar)="periodos.reload()"
        >
          <ng-template #plantillaError let-error>
            <p class="mensaje">{{ error.codigo === EJERCICIO_INEXISTENTE ? t.ejercicioInexistente : error.mensaje }}</p>
            @if (error.trazaId) { <p class="traza">Código de seguimiento: {{ error.trazaId }}</p> }
          </ng-template>
          <ul class="meses">
            @for (p of lista(); track p.periodoId) {
              <li class="mes" [class.cerrado]="!puedeAsentar(p)">
                <div class="cabecera">
                  <h3>{{ p.nombre }}</h3>
                  <span class="chips">
                    @if (p.periodoId === periodoDeHoy()) { <ap-chip-estado tono="info">{{ t.marcaVigente }}</ap-chip-estado> }
                    <ap-chip-estado [tono]="puedeAsentar(p) ? 'ok' : 'neutro'">{{ etiqueta(p) }}</ap-chip-estado>
                  </span>
                </div>
                @if (p.totalDebe && p.totalHaber) {
                  <p class="cuadre" [class.descuadre]="!cuadraElCierre(p)">{{ cuadraElCierre(p) ? t.cuadroAlCierre : t.descuadreSellado }}</p>
                } @else {
                  <p class="nota">{{ t.sinCierre }}</p>
                }
                @if (p.cerradoEn) { <p class="nota">{{ t.cerradoEl }} <ap-fecha [iso]="p.cerradoEn" /></p> }
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
      } @else if (errorVigente()) {
        <p class="nota">{{ t.elegirEjercicio }}</p>
      }
    </section>

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
        <ap-error-de-envio [error]="errorDeEnvio()" />
      </ap-dialogo>
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 56rem; display: grid; gap: var(--s4); }
    h1, h2, h3 { margin: 0; }
    h2 { font-size: 1.05em; }
    h3 { font-size: 1em; }
    .aviso, .nota, .motivo { color: var(--text-2); margin: 0; }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
    .meses { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); }
    .mes { border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); padding: var(--s4); display: grid; gap: var(--s3); }
    .mes.cerrado { background: var(--surface-2); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    .chips { display: inline-flex; gap: var(--s2); }
    .cuadre { margin: 0; }
    .cuadre.descuadre { color: var(--err-texto); }
    .mensaje { color: var(--text); margin: 0 0 var(--s2); }
  `,
})
export class PantallaDePeriodo {
  protected readonly t = textosContabilidad.periodo
  protected readonly EJERCICIO_INEXISTENTE = EJERCICIO_INEXISTENTE
  /** De la URL (`periodo/:ejercicioId`). Ausente en la ruta raíz de Contabilidad: ahí manda el período de hoy. */
  readonly ejercicioId = input<string | undefined>(undefined)
  private readonly sesion = inject(Sesion)

  protected readonly vigente = periodoVigente()
  protected readonly errorVigente = computed(() => errorDelRecurso(this.vigente))
  protected readonly valorVigente = computed(() => (this.vigente.hasValue() ? (this.vigente.value() ?? null) : null))
  protected readonly periodoDeHoy = computed(() => this.valorVigente()?.periodoId ?? null)

  /** La URL manda; sin ella, el ejercicio del período de hoy. Si no hay período de hoy, ninguno: no se elige por la persona. */
  protected readonly ejercicioElegido = computed(() => this.ejercicioId() || this.valorVigente()?.ejercicioId || undefined)
  protected readonly periodos = periodosDelEjercicio(() => this.ejercicioElegido())
  protected readonly vacio = sinPeriodos
  /** `hasValue()` antes de `value()`: el contenido proyectado se evalúa aunque el recurso falle. */
  protected readonly lista = computed<PeriodoContable[]>(() => (this.periodos.hasValue() ? (this.periodos.value() ?? []) : []))

  protected readonly puedeOperarElCierre = computed(() => this.sesion.puede('CONTABILIDAD_ERP_CERRAR'))
  protected readonly elegido = signal<PeriodoContable | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly cerrando = signal(false)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  protected readonly asentadoEn = signal<string | null>(null)
  private readonly enviarCierre = crearCierreDePeriodo()

  protected puedeAsentar = puedeAsentarEn
  protected motivo = motivoParaNoAsentar
  protected cuadraElCierre = cuadra
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
    this.errorDeEnvio.set(null)
    this.dialogoAbierto.set(false)
    this.elegido.set(null)
  }

  protected confirmarCierre(p: PeriodoContable): void {
    this.cerrando.set(true)
    this.errorDeEnvio.set(null)
    this.enviarCierre(p.periodoId, `Cierre de ${p.nombre}`).subscribe({
      next: () => {
        this.cerrando.set(false)
        this.cerrarConfirmacion()
        this.periodos.reload()
      },
      error: (e: ErrorTraducido) => {
        this.errorDeEnvio.set(e)
        this.cerrando.set(false)
      },
    })
  }
}
