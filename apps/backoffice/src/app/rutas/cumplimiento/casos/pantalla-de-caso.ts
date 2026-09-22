import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { CATALOGO_DE_CAUSALES, etapasDelCaso, puedeConfirmar, type CasoDeCumplimiento } from '../dominio/cu44-caso'
import { textosCumplimiento } from '../textos'
import { FormularioDeCaso } from './formulario-de-caso'

/**
 * CU-44 · De alerta de monitoreo a reporte de operación sospechosa, con el patrón de
 * `debido-proceso` completo.
 *
 * **Contenedor**: conecta ruta, `ServicioBorrador` (persistencia del borrador) y las dos
 * garantías del gate del carril — que `FormularioDeCaso` (presentación pura) no puede
 * romper porque no tiene cómo llegar a ellas:
 *
 * 1. Rechazar u observar SIN CAUSAL DEL CATÁLOGO es imposible: `puedeConfirmarComputado`
 *    deshabilita el botón hasta que se elija una causal de `CATALOGO_DE_CAUSALES` — nunca
 *    texto libre — derivada UNA vez (H2 del carril, antes se evaluaba dos veces en el
 *    propio template).
 * 2. La narrativa larga del ROS se guarda con `ServicioBorrador` y se recupera tras una
 *    sesión caída: se guarda en cada cambio y se lee al abrir la pantalla.
 */
@Component({
  selector: 'ap-pantalla-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, FormularioDeCaso],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-formulario-de-caso
        [etapas]="progreso().etapas"
        [etapaActual]="progreso().actual"
        [causales]="CATALOGO_DE_CAUSALES"
        [causalElegida]="causal()"
        [narrativa]="narrativa()"
        [borradorRecuperado]="borradorRecuperado()"
        [puedeConfirmar]="puedeConfirmarComputado()"
        [textoCausal]="t.causal"
        [textoSinCausal]="t.sinCausal"
        [textoBorradorRecuperado]="t.borradorRecuperado"
        [textoConfirmar]="t.confirmar"
        (seEligioCausal)="alEscribirCausal($event)"
        (seEscribioNarrativa)="alEscribirNarrativa($event)"
        (seQuiereConfirmar)="confirmar()"
      />
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
  `,
})
export class PantallaDeCaso implements OnInit {
  private readonly servicioBorrador = inject(ServicioBorrador)
  protected readonly t = textosCumplimiento.casos
  protected readonly CATALOGO_DE_CAUSALES = CATALOGO_DE_CAUSALES

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

  protected readonly progreso = computed(() => etapasDelCaso(this.caso()))
  protected readonly causal = signal<string | null>(null)
  protected readonly narrativa = signal('')
  protected readonly borradorRecuperado = signal(false)
  /** Derivada una sola vez (H2): antes `puedeConfirmar(causal())` se evaluaba dos veces
   * en el propio template (deshabilitar el botón y mostrar la ayuda). */
  protected readonly puedeConfirmarComputado = computed(() => puedeConfirmar(this.causal()))

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

  protected alEscribirCausal(valor: string | null): void {
    this.causal.set(valor)
  }

  protected alEscribirNarrativa(valor: string): void {
    this.narrativa.set(valor)
    void this.servicioBorrador.guardar(this.claveBorrador(), valor)
  }

  protected confirmar(): void {
    if (!this.puedeConfirmarComputado()) return
    void this.servicioBorrador.borrar(this.claveBorrador())
  }
}
