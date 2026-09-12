import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { map } from 'rxjs'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { accionesDeCampana } from '../dominio/cu111-campanas'
import { textosPublicidad } from '../textos'

type Decision = 'APROBAR' | 'RECHAZAR'

/**
 * CU-111 · Lado **aprobación**. Solo la monta el `canMatch` de
 * `PUBLICIDAD_APROBAR_CAMPANA` en `publicidad.routes.ts` — quien no tiene ese permiso
 * ni siquiera llega a esta ruta (segregación de funciones al nivel de ruta, no de UI
 * condicional). Un rechazo exige motivo escrito.
 */
@Component({
  selector: 'ap-panel-de-aprobacion-de-campana',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo],
  template: `
    <ap-banda-de-proposito [texto]="t.propositoAprobacion" />
    <main>
      <h1>{{ t.tituloAprobacion }}</h1>
      <p>Campaña <strong>{{ campanaId() }}</strong></p>
      <div class="acciones">
        <ap-boton (pulsado)="abrir('APROBAR')">{{ t.aprobar }}</ap-boton>
        <ap-boton variante="fantasma" (pulsado)="abrir('RECHAZAR')">{{ t.rechazar }}</ap-boton>
      </div>
    </main>

    @if (dialogoAbierto()) {
      <ap-dialogo
        [titulo]="decision() === 'APROBAR' ? t.aprobar : t.rechazar"
        [textoDeConfirmar]="decision() === 'APROBAR' ? t.aprobar : t.rechazar"
        [destructivo]="decision() === 'RECHAZAR'"
        [abierto]="dialogoAbierto()"
        [cargando]="enviando()"
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
    main { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .acciones { display: flex; gap: var(--s2); }
  `,
})
export class PanelDeAprobacionDeCampana {
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
