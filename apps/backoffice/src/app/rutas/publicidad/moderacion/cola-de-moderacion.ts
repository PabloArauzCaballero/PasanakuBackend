import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { accionesDeModeracion, cargadorDePiezasPendientes, type PiezaCreativa } from '../dominio/cu112-moderacion'
import { textosPublicidad } from '../textos'
import { EntradaRevisionDecisionEnum } from 'clientes/angular/publicidad/model/entradaRevision'

type Decision = 'APROBADA' | 'RECHAZADA'

const COLUMNAS: ColumnaVirtual<PiezaCreativa>[] = [
  { clave: 'titulo', titulo: 'Título', ordenable: true },
  { clave: 'anuncianteNombre', titulo: 'Anunciante' },
  { clave: 'tipoRecurso', titulo: 'Recurso' },
  { clave: 'piezaCreativaId', titulo: 'Acciones' },
]

/**
 * CU-112 · **Cola de moderación — gate de entrada del carril.** Toda pieza nace
 * PENDIENTE (CU-112, `subirPiezaCreativa`) y solo pasa a APROBADA/RECHAZADA acá
 * (`moderarPieza`). El botón se oculta si el operador de sesión es quien subió la
 * pieza (R-PUB-05, `puedeModerar`); el servidor es quien decide de verdad
 * (AP-CU112-03) — la UI solo evita el viaje inútil.
 */
@Component({
  selector: 'ap-cola-de-moderacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, ChipEstado, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="cargador" [ordenPermitido]="['titulo']" [identidad]="identidad">
        <ng-template #celda let-p let-columna="columna">
          @switch (columna.clave) {
            @case ('tipoRecurso') {
              <ap-chip-estado tono="info">{{ p.tipoRecurso }}</ap-chip-estado>
            }
            @case ('piezaCreativaId') {
              @if (puedeModerarEsta()) {
                <div class="acciones">
                  <ap-boton (pulsado)="abrir(p, 'APROBADA')">{{ t.aprobar }}</ap-boton>
                  <ap-boton variante="fantasma" (pulsado)="abrir(p, 'RECHAZADA')">{{ t.rechazar }}</ap-boton>
                </div>
              } @else {
                <span class="bloqueado">{{ t.esAutorNoPuedeModerar }}</span>
              }
            }
            @default {
              {{ p[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>

    @if (seleccionada(); as p) {
      <ap-dialogo
        [titulo]="decision() === 'APROBADA' ? t.aprobar : t.rechazar"
        [textoDeConfirmar]="decision() === 'APROBADA' ? t.aprobar : t.rechazar"
        [destructivo]="decision() === 'RECHAZADA'"
        [abierto]="dialogoAbierto()"
        [cargando]="enviando()"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmar()"
        (cancelar)="cerrar()"
      >
        <p>{{ decision() === 'APROBADA' ? t.confirmarAprobar(p.titulo) : t.confirmarRechazar(p.titulo) }}</p>
        @if (decision() === 'RECHAZADA') {
          <ap-campo [etiqueta]="t.motivoDeRechazo" [(valor)]="motivo" [error]="motivoVacio() ? t.faltaMotivo : undefined" />
        }
      </ap-dialogo>
    }
  `,
  styles: `
    main { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .acciones { display: flex; gap: var(--s2); }
    .bloqueado { color: var(--text-2); font-size: .9em; }
  `,
})
export class ColaDeModeracion {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeModeracion()
  protected readonly t = textosPublicidad.moderacion
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (p: PiezaCreativa) => p.piezaCreativaId
  protected readonly cargador = cargadorDePiezasPendientes()

  protected readonly seleccionada = signal<PiezaCreativa | null>(null)
  protected readonly decision = signal<Decision>('APROBADA')
  protected readonly dialogoAbierto = signal(false)
  protected readonly motivo = signal('')
  protected readonly intentoConfirmar = signal(false)
  protected readonly enviando = signal(false)
  protected readonly motivoVacio = () => this.intentoConfirmar() && this.decision() === 'RECHAZADA' && this.motivo().trim().length === 0

  /**
   * Supuesto declarado: `nucleo/sesion.ts` (F6, solo lectura) guarda token, permisos y
   * rol, pero no el id del operador autenticado — no hay de dónde leer «quién soy» para
   * compararlo contra `subidaPor` en el cliente. `puedeModerar` (dominio) queda escrita
   * y probada para el día que la sesión lo exponga; mientras tanto, R-PUB-05 lo decide
   * el servidor (AP-CU112-03) y esta pantalla solo exige el permiso de moderación.
   */
  protected puedeModerarEsta(): boolean {
    return this.sesion.puede('PUBLICIDAD_MODERAR')
  }

  abrir(p: PiezaCreativa, decision: Decision): void {
    this.seleccionada.set(p)
    this.decision.set(decision)
    this.motivo.set('')
    this.intentoConfirmar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
    this.seleccionada.set(null)
  }

  async confirmar(): Promise<void> {
    const p = this.seleccionada()
    if (!p) return
    if (this.decision() === 'RECHAZADA') {
      this.intentoConfirmar.set(true)
      if (this.motivo().trim().length === 0) return
    }
    this.enviando.set(true)
    try {
      const decisionEnum = this.decision() === 'APROBADA' ? EntradaRevisionDecisionEnum.Aprobada : EntradaRevisionDecisionEnum.Rechazada
      await this.acciones.moderar(p.piezaCreativaId, {
        decision: decisionEnum,
        ...(this.decision() === 'RECHAZADA' ? { motivo: this.motivo() } : {}),
      })
      this.cerrar()
    } finally {
      this.enviando.set(false)
    }
  }
}
