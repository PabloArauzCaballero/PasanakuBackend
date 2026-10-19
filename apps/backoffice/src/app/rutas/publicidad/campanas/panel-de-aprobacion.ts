import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { map } from 'rxjs'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { confirmarDescarteSiSucio } from '@aportaya/ui/dialogo/politica-de-descarte'
import { Campo } from '@aportaya/ui/campo/campo'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { accionesDeCampana } from '../dominio/cu111-campanas'
import { textosPublicidad } from '../textos'

type Decision = 'APROBAR' | 'RECHAZAR'

/**
 * CU-111 · Lado **aprobación**. Solo la monta el `canMatch` de
 * `PUBLICIDAD_APROBAR_CAMPANA` en `publicidad.routes.ts`.
 */
@Component({
  selector: 'ap-panel-de-aprobacion-de-campana',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, Dialogo, Campo],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina
        [titulo]="t.tituloAprobacion"
        [descripcion]="t.propositoAprobacion"
        icono="estrella"
      />
      <p>Campaña <strong>{{ campanaId() }}</strong></p>
      <div class="acciones">
        <ap-boton (pulsado)="abrir('APROBAR')">{{ t.aprobar }}</ap-boton>
        <ap-boton variante="fantasma" (pulsado)="abrir('RECHAZAR')">{{ t.rechazar }}</ap-boton>
      </div>
    </section>

    @if (dialogoAbierto()) {
      <ap-dialogo
        [titulo]="decision() === 'APROBAR' ? t.aprobar : t.rechazar"
        [textoDeConfirmar]="decision() === 'APROBAR' ? t.aprobar : t.rechazar"
        [destructivo]="decision() === 'RECHAZAR'"
        [abierto]="dialogoAbierto()"
        [cargando]="enviando()"
        [puedeDescartar]="puedeDescartar"
        (abiertoChange)="dialogoAbierto.set($event)"
        (confirmar)="confirmar()"
        (cancelar)="cerrar()"
      >
        <p>{{ decision() === 'APROBAR' ? t.confirmarAprobar(campanaId()) : t.confirmarRechazar(campanaId()) }}</p>
        @if (decision() === 'RECHAZAR') {
          <ap-campo [etiqueta]="t.motivoDeRechazo" [(valor)]="motivo" [error]="motivoVacio() ? t.faltaMotivo : undefined" />
        }
      </ap-dialogo>
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    .acciones { display: flex; gap: var(--s2); }
  `,
})
export class PanelDeAprobacionDeCampana {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.propositoAprobacion)
  }
  private readonly acciones = accionesDeCampana()
  private readonly ruta = inject(ActivatedRoute)
  protected readonly t = textosPublicidad.campanas
  protected readonly campanaId = toSignal(this.ruta.paramMap.pipe(map((p) => p.get('campanaId') ?? '')), { initialValue: '' })

  protected readonly dialogoAbierto = signal(false)
  protected readonly decision = signal<Decision>('APROBAR')
  protected readonly motivo = signal('')
  protected readonly intentoConfirmar = signal(false)
  protected readonly enviando = signal(false)
  protected readonly motivoVacio = () => this.intentoConfirmar() && this.decision() === 'RECHAZAR' && this.motivo().trim().length === 0
  /** Borrador sucio: solo el rechazo tiene campo, y solo cuenta si ya se escribió algo. */
  protected readonly sucio = () => this.decision() === 'RECHAZAR' && this.motivo().trim().length > 0
  protected readonly puedeDescartar = confirmarDescarteSiSucio(this.sucio)

  abrir(decision: Decision): void {
    this.decision.set(decision)
    this.motivo.set('')
    this.intentoConfirmar.set(false)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
  }

  async confirmar(): Promise<void> {
    if (this.decision() === 'RECHAZAR') {
      this.intentoConfirmar.set(true)
      if (this.motivo().trim().length === 0) return
    }
    this.enviando.set(true)
    try {
      if (this.decision() === 'APROBAR') {
        await this.acciones.aprobar(this.campanaId())
      } else {
        await this.acciones.rechazar(this.campanaId(), { motivo: this.motivo() })
      }
      this.cerrar()
    } finally {
      this.enviando.set(false)
    }
  }
}
