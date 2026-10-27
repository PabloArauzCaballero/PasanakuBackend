import { Location } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { toSignal } from '@angular/core/rxjs-interop'
import { map } from 'rxjs'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { confirmarDescarteSiSucio } from '@aportaya/ui/dialogo/politica-de-descarte'
import { Campo } from '@aportaya/ui/campo/campo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Icono } from '@aportaya/ui/icono/icono'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { accionesDeCampana, type Campana } from '../dominio/cu111-campanas'
import { traducirError } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'

type Decision = 'APROBAR' | 'RECHAZAR'

/**
 * CU-111 · Lado **aprobación**. Solo la monta el `canMatch` de `PUBLICIDAD_APROBAR_CAMPANA` en `publicidad.routes.ts`.
 * La campaña llega en el estado de la navegación desde la cola (el contrato no tiene `GET /campanas/{id}`); abierta por
 * enlace, se muestra solo el identificador y se dice por qué. Antes, aprobar o rechazar cerraba el diálogo sin decir
 * nada y un error se perdía: ahora el resultado del servidor se ve, el error se queda en el diálogo con lo escrito, y
 * una campaña ya decidida no ofrece decidirla de nuevo.
 */
@Component({
  selector: 'ap-panel-de-aprobacion-de-campana',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, Campo, Dialogo, ErrorDeEnvio, Icono, RouterLink, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Publicidad · campañas" [titulo]="t.tituloAprobacion" [descripcion]="t.propositoAprobacion" icono="aprobar" />
      <div class="tarjeta">
        @if (campana(); as c) {
          <ap-seccion-clave-valor [titulo]="c.nombre" [items]="detalle()" />
        } @else {
          <p>{{ t.campana }} <strong class="id">{{ campanaId() }}</strong></p>
          <ap-alerta tono="info">{{ t.sinDetalle }}</ap-alerta>
        }
        @if (decidida(); as d) {
          <ap-alerta [tono]="d === 'APROBAR' ? 'ok' : 'info'">{{ d === 'APROBAR' ? t.aprobada : t.rechazada }}</ap-alerta>
        } @else {
          <div class="acciones">
            <ap-boton variante="primario" icono="aprobar" (pulsado)="abrir('APROBAR')">{{ t.aprobar }}</ap-boton>
            <ap-boton variante="peligro" icono="rechazar" (pulsado)="abrir('RECHAZAR')">{{ t.rechazar }}</ap-boton>
          </div>
        }
        <a class="volver" routerLink="/publicidad/campanas"><ap-icono nombre="flechaIzquierda" tamano="s4" />{{ t.volver }}</a>
      </div>
    </section>

    <ap-dialogo [titulo]="decision() === 'APROBAR' ? t.aprobar : t.rechazar" [textoDeConfirmar]="decision() === 'APROBAR' ? t.aprobar : t.rechazar" [destructivo]="decision() === 'RECHAZAR'" [(abierto)]="dialogoAbierto" [cargando]="enviando()" [puedeDescartar]="puedeDescartar" (confirmar)="confirmar()" (cancelar)="dialogoAbierto.set(false)">
      <p>{{ decision() === 'APROBAR' ? t.confirmarAprobar(nombre()) : t.confirmarRechazar(nombre()) }}</p>
      @if (decision() === 'RECHAZAR') {
        <ap-campo [etiqueta]="t.motivoDeRechazo" [(valor)]="motivo" [error]="motivoVacio() ? t.faltaMotivo : undefined" />
      }
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); justify-items: center; }
    .pantalla > * { width: 100%; max-width: 56rem; }
    .tarjeta { display: grid; gap: var(--s4); padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); min-width: 0; }
    .id { font-family: var(--mono); overflow-wrap: anywhere; }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--s2); }
    .volver { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; justify-self: start; }
    .volver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PanelDeAprobacionDeCampana {
  private readonly acciones = accionesDeCampana()
  protected readonly t = textosPublicidad.campanas
  protected readonly campanaId = toSignal(inject(ActivatedRoute).paramMap.pipe(map((p) => p.get('campanaId') ?? '')), { initialValue: '' })
  /** Solo si la que vino en el estado es la de la URL: un estado viejo no describe otra campaña. */
  protected readonly campana = signal<Campana | null>(null)
  protected readonly detalle = computed<ClaveValor[]>(() => {
    const c = this.campana()
    if (!c) return []
    return [
      { etiqueta: 'Objetivo', valor: this.t.objetivos[c.objetivo] },
      { etiqueta: 'Estado', valor: this.t.estados[c.estado] },
      { etiqueta: 'Presupuesto', valor: `${c.presupuestoTotal.monto} ${c.presupuestoTotal.moneda}`, mono: true },
      { etiqueta: 'Consumido', valor: `${c.presupuestoConsumido.monto} ${c.presupuestoConsumido.moneda}`, mono: true },
      { etiqueta: 'Inicio', valor: c.fechaInicio.slice(0, 10) },
      { etiqueta: 'Identificador', valor: c.campanaId, mono: true },
    ]
  })
  protected readonly nombre = () => this.campana()?.nombre ?? this.campanaId()

  protected readonly dialogoAbierto = signal(false)
  protected readonly decision = signal<Decision>('APROBAR')
  protected readonly decidida = signal<Decision | null>(null)
  protected readonly motivo = signal('')
  protected readonly intentoConfirmar = signal(false)
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly motivoVacio = () => this.intentoConfirmar() && this.decision() === 'RECHAZAR' && this.motivo().trim().length === 0
  /** Borrador sucio: solo el rechazo tiene campo, y solo cuenta si ya se escribió algo. */
  protected readonly sucio = () => this.decision() === 'RECHAZAR' && this.motivo().trim().length > 0
  protected readonly puedeDescartar = confirmarDescarteSiSucio(this.sucio)

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.propositoAprobacion)
    const enviada = (inject(Location).getState() as { campana?: Campana } | null)?.campana
    if (enviada && enviada.campanaId === this.campanaId()) this.campana.set(enviada)
  }

  abrir(decision: Decision): void {
    this.decision.set(decision)
    this.motivo.set('')
    this.intentoConfirmar.set(false)
    this.error.set(null)
    this.dialogoAbierto.set(true)
  }

  async confirmar(): Promise<void> {
    if (this.enviando()) return
    const decision = this.decision()
    if (decision === 'RECHAZAR') {
      this.intentoConfirmar.set(true)
      if (this.motivo().trim().length === 0) return
    }
    this.enviando.set(true)
    this.error.set(null)
    try {
      if (decision === 'APROBAR') await this.acciones.aprobar(this.campanaId())
      else await this.acciones.rechazar(this.campanaId(), { motivo: this.motivo().trim() })
      this.decidida.set(decision)
      this.dialogoAbierto.set(false)
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
