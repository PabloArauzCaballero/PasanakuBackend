import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { CATALOGO_DE_CAUSALES, etapasDelCaso, puedeConfirmar, type CasoDeCumplimiento } from '../dominio/cu44-caso'
import { textosCumplimiento } from '../textos'
import { FormularioDeCaso } from './formulario-de-caso'

/**
 * CU-44 · De alerta de monitoreo a reporte de operación sospechosa, con el patrón de
 * `debido-proceso` completo.
 */
@Component({
  selector: 'ap-pantalla-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, FormularioDeCaso],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="candado" />
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
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s4); }
  `,
})
export class PantallaDeCaso implements OnInit {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly servicioBorrador = inject(ServicioBorrador)
  protected readonly t = textosCumplimiento.casos
  protected readonly CATALOGO_DE_CAUSALES = CATALOGO_DE_CAUSALES

  readonly casoId = input.required<string>()
  /**
   * HALLAZGO (E2E real, `caso-de-cumplimiento.e2e.ts`): `withComponentInputBinding()`
   * deja este input en `undefined` cuando se llega por una navegación real (no por
   * `TestBed.createComponent` sin setear el input, que sí respeta el valor por defecto
   * de `input()`) — el router no tiene de dónde sacar un `caso` real todavía (`Q-R1` del
   * carril madre) y el binding automático pisa el default en vez de dejarlo. Por eso el
   * "valor por omisión" se resuelve con un `computed()` (`casoResuelto`), no con el
   * argumento de `input()`, que demostró no ser confiable contra el router real.
   */
  readonly caso = input<CasoDeCumplimiento | undefined>(undefined)
  private readonly casoResuelto = computed<CasoDeCumplimiento>(
    () =>
      this.caso() ?? {
        id: 'demo',
        causal: null,
        notificadoEn: null,
        plazoVenceEn: null,
        descargo: null,
        decisionMotivada: null,
        apelacionResueltaPor: null,
      },
  )

  protected readonly progreso = computed(() => etapasDelCaso(this.casoResuelto()))
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
