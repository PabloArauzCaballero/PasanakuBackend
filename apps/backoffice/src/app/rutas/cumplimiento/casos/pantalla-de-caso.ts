import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { EscaleraDeEtapas } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { CATALOGO_DE_CAUSALES, etapasDelCaso, puedeConfirmar, type CasoDeCumplimiento } from '../dominio/cu44-caso'
import { textosCumplimiento } from '../textos'

/**
 * CU-44 · De alerta de monitoreo a reporte de operación sospechosa, con el patrón de
 * `debido-proceso` completo. Dos garantías del gate del carril viven acá:
 *
 * 1. Rechazar u observar SIN CAUSAL DEL CATÁLOGO es imposible: `puedeConfirmar()`
 *    deshabilita el botón de confirmar hasta que se elija una causal de
 *    `CATALOGO_DE_CAUSALES` — nunca texto libre.
 * 2. La narrativa larga del ROS se guarda con `ServicioBorrador` y se recupera tras
 *    una sesión caída: se guarda en cada cambio y se lee al abrir la pantalla.
 */
@Component({
  selector: 'ap-pantalla-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, EscaleraDeEtapas, GrupoRadio, Boton],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-escalera-de-etapas [etapas]="progreso().etapas" [actual]="progreso().actual" />

      <ap-grupo-radio [etiqueta]="t.causal" [opciones]="CATALOGO_DE_CAUSALES" [(elegido)]="causal" />
      @if (!puedeConfirmar(causal())) {
        <p class="ayuda">{{ t.sinCausal }}</p>
      }

      <label for="narrativa">Narrativa</label>
      <textarea id="narrativa" [value]="narrativa()" (input)="alEscribir($any($event.target).value)" rows="6"></textarea>
      @if (borradorRecuperado()) { <p class="aviso">{{ t.borradorRecuperado }}</p> }

      <ap-boton variante="primario" [deshabilitado]="!puedeConfirmar(causal())" (pulsado)="confirmar()">{{ t.confirmar }}</ap-boton>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); }
    .ayuda { color: var(--text-3); font-size: .9em; margin: 0; }
    .aviso { color: var(--info); font-size: .9em; margin: 0; }
  `,
})
export class PantallaDeCaso implements OnInit {
  private readonly servicioBorrador = inject(ServicioBorrador)
  protected readonly t = textosCumplimiento.casos
  protected readonly CATALOGO_DE_CAUSALES = CATALOGO_DE_CAUSALES
  protected readonly puedeConfirmar = puedeConfirmar

  readonly casoId = input.required<string>()
  readonly caso = input<CasoDeCumplimiento>({
    id: 'demo',
    causal: null,
    notificadoEn: null,
    plazoVenceEn: null,
    descargo: null,
    decisionMotivada: null,
    apelacionResueltaPor: null,
  })

  protected readonly progreso = () => etapasDelCaso(this.caso())
  protected readonly causal = signal<string | null>(null)
  protected readonly narrativa = signal('')
  protected readonly borradorRecuperado = signal(false)

  private claveBorrador(): string {
    return `cumplimiento:caso:${this.casoId()}:narrativa`
  }

  async ngOnInit(): Promise<void> {
    const guardado = await this.servicioBorrador.leer<string>(this.claveBorrador())
    if (guardado) {
      this.narrativa.set(guardado)
      this.borradorRecuperado.set(true)
    }
  }

  protected alEscribir(valor: string): void {
    this.narrativa.set(valor)
    void this.servicioBorrador.guardar(this.claveBorrador(), valor)
  }

  protected confirmar(): void {
    if (!puedeConfirmar(this.causal())) return
    void this.servicioBorrador.borrar(this.claveBorrador())
  }
}
