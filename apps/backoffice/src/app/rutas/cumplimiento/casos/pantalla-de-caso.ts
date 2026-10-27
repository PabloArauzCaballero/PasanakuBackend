import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Icono } from '@aportaya/ui/icono/icono'
import type { CasoEnBandeja, SalidaDecisionCasoLft } from 'clientes/angular/cumplimiento'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ServicioBorrador } from '../../../nucleo/borrador'
import { CATALOGO_DE_CAUSALES, etapasDelCaso, puedeConfirmar, type CasoDeCumplimiento } from '../dominio/cu44-caso'
import { textosCumplimiento } from '../textos'
import { textosLft } from '../textos-lft'
import { DecidirCaso } from './decidir-caso'
import { FormularioDeCaso } from './formulario-de-caso'

/**
 * CU-44 · De alerta de monitoreo a reporte de operación sospechosa, con el patrón de
 * `debido-proceso` completo.
 *
 * «Confirmar decisión» antes solo borraba el borrador local: era un botón que no hacía nada
 * (2026-10-26). Ahora abre la decisión real del caso (`decidirCasoLft`, `DecidirCaso`) con la
 * causal y la narrativa como punto de partida de la conclusión; el borrador se borra recién
 * cuando el servidor registró la decisión. Quién puede decidir lo resuelve el servidor (403).
 */
@Component({
  selector: 'ap-pantalla-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, FormularioDeCaso, DecidirCaso, Alerta, Icono, RouterLink],
  template: `
    <section class="pantalla">
      <a class="volver" routerLink="/cumplimiento/casos"><ap-icono nombre="flechaIzquierda" tamano="s4" />Volver a los casos</a>
      <ap-cabecera-de-pagina [eyebrow]="'Cumplimiento · caso ' + casoId()" [titulo]="t.titulo" [descripcion]="t.proposito" icono="carpeta" />
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <div class="tarjeta">
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
      </div>
      <ap-decidir-caso [(abierto)]="decidiendo" [caso]="casoParaDecidir()" [conclusionInicial]="conclusionInicial()" (decidio)="alDecidir($event)" />
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5) var(--s4); display: flex; flex-direction: column; gap: var(--s4); min-width: 0; }
    .tarjeta { padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .volver { display: inline-flex; align-items: center; gap: var(--s1); align-self: flex-start; min-height: var(--area-tactil); color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; text-decoration: none; }
    .volver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
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

  protected readonly decidiendo = signal(false)
  protected readonly recibo = signal<string | null>(null)
  /** La bandeja no viaja hasta acá: el diálogo solo necesita el id y cómo nombrar el caso. */
  protected readonly casoParaDecidir = computed(() => ({ casoId: this.casoId(), codigo: this.casoId() }) as CasoEnBandeja)
  protected readonly conclusionInicial = computed(() => {
    const causal = CATALOGO_DE_CAUSALES.find((c) => c.valor === this.causal())?.texto
    return [causal ? `Causal: ${causal}.` : '', this.narrativa().trim()].filter(Boolean).join(' ')
  })

  protected confirmar(): void {
    if (!this.puedeConfirmarComputado()) return
    this.decidiendo.set(true)
  }

  protected alDecidir(salida: SalidaDecisionCasoLft): void {
    void this.servicioBorrador.borrar(this.claveBorrador())
    this.borradorRecuperado.set(false)
    const decisiones = textosLft.decisiones
    this.recibo.set(textosLft.decision.recibo(decisiones[salida.decision] ?? salida.decision, salida.alertasCerradas, salida.fueraDePlazo))
  }
}
