import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeListado, observarListado } from '../../contabilidad/listado/estado-de-listado'
import { accionesDeModeracion, cargadorDePiezasPendientes, type PiezaCreativa } from '../dominio/cu112-moderacion'
import { textosPublicidad } from '../textos'
import { EntradaRevisionDecisionEnum } from 'clientes/angular/publicidad/model/entradaRevision'

type Decision = 'APROBADA' | 'RECHAZADA'

const COLUMNAS: ColumnaVirtual<PiezaCreativa>[] = [
  { clave: 'titulo', titulo: 'Título', ancho: '2' },
  { clave: 'tipoRecurso', titulo: 'Recurso' },
  { clave: 'creadaEn', titulo: 'Subida' },
  { clave: 'piezaCreativaId', titulo: 'Acciones', ancho: '2' },
]

/**
 * CU-112 · **Cola de moderación — gate de entrada del carril.** Toda pieza nace PENDIENTE y solo pasa a
 * APROBADA/RECHAZADA acá (`moderarPieza`). La cola es `GET /publicidad/piezas-creativas` por cursor (la más antigua
 * primero, orden del servidor).
 *
 * **Identidad separada.** La ruta ya exige `PUBLICIDAD_MODERAR`, y la pantalla vuelve a preguntarlo antes de dibujar
 * los botones: sin el permiso no hay aprobar ni rechazar. Quién subió cada pieza NO viaja en el contrato, así que
 * R-PUB-05 lo decide el servidor (`AP-CU112-03`) y la pantalla muestra ese rechazo dentro del diálogo, sin afirmar
 * nada antes del 2xx. Una pieza resuelta muestra su resultado en la fila (la tabla del shell no se recarga sola).
 */
@Component({
  selector: 'ap-cola-de-moderacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, ChipEstado, ErrorDeEnvio, EstadoDeListado, Fecha, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-listado [error]="listado.error()" [vacio]="listado.vacio()" [mensajeVacio]="t.sinPiezas" />
      <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="listado.cargador" [ordenPermitido]="[]" [identidad]="identidad">
        <ng-template #celda let-p let-columna="columna">
          @switch (columna.clave) {
            @case ('tipoRecurso') {
              <ap-chip-estado tono="info">{{ p.tipoRecurso }}</ap-chip-estado>
              <a class="ver" [href]="p.urlRecurso" target="_blank" rel="noopener noreferrer" [attr.aria-label]="t.verPieza(p.titulo)">Ver</a>
            }
            @case ('creadaEn') {
              <ap-fecha [iso]="p.creadaEn" [conHora]="false" />
            }
            @case ('piezaCreativaId') {
              @if (resultado(p); as r) {
                <ap-chip-estado [tono]="r === 'APROBADA' ? 'ok' : 'error'">{{ t.estados[r] }}</ap-chip-estado>
              } @else if (puedeModerarEsta()) {
                <div class="acciones">
                  <ap-boton (pulsado)="abrir(p, 'APROBADA')">{{ t.aprobar }}</ap-boton>
                  <ap-boton variante="fantasma" (pulsado)="abrir(p, 'RECHAZADA')">{{ t.rechazar }}</ap-boton>
                </div>
              }
            }
            @default {
              {{ p[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </section>

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
        <p class="nota">{{ t.autorLoDecideElServidor }}</p>
        @if (decision() === 'RECHAZADA') {
          <ap-campo [etiqueta]="t.motivoDeRechazo" [(valor)]="motivo" [error]="motivoVacio() ? t.faltaMotivo : undefined" />
        }
        <ap-error-de-envio [error]="errorDeEnvio()" />
      </ap-dialogo>
    }
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 68rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .acciones { display: flex; gap: var(--s2); }
    .ver { margin-left: var(--s2); color: var(--text); }
    .ver:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .nota { color: var(--text-2); font-size: .9em; }
  `,
})
export class ColaDeModeracion {
  private readonly sesion = inject(Sesion)
  private readonly acciones = accionesDeModeracion()
  protected readonly t = textosPublicidad.moderacion
  protected readonly COLUMNAS = COLUMNAS
  protected readonly identidad = (p: PiezaCreativa) => p.piezaCreativaId
  protected readonly listado = observarListado(cargadorDePiezasPendientes(), textosPublicidad.errores)

  protected readonly seleccionada = signal<PiezaCreativa | null>(null)
  protected readonly decision = signal<Decision>('APROBADA')
  protected readonly dialogoAbierto = signal(false)
  protected readonly motivo = signal('')
  protected readonly intentoConfirmar = signal(false)
  protected readonly enviando = signal(false)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  /** Lo que el servidor ya confirmó (2xx), por pieza. */
  protected readonly resueltas = signal<Readonly<Record<string, Decision>>>({})
  protected readonly motivoVacio = () => this.intentoConfirmar() && this.decision() === 'RECHAZADA' && this.motivo().trim().length === 0
  protected readonly resultado = (p: PiezaCreativa): Decision | undefined => this.resueltas()[p.piezaCreativaId]

  /** Sin `PUBLICIDAD_MODERAR` no se dibujan los botones (el servidor igual lo exige). */
  protected puedeModerarEsta(): boolean {
    return this.sesion.puede('PUBLICIDAD_MODERAR')
  }

  abrir(p: PiezaCreativa, decision: Decision): void {
    this.seleccionada.set(p)
    this.decision.set(decision)
    this.motivo.set('')
    this.intentoConfirmar.set(false)
    this.errorDeEnvio.set(null)
    this.dialogoAbierto.set(true)
  }

  cerrar(): void {
    this.dialogoAbierto.set(false)
    this.seleccionada.set(null)
  }

  async confirmar(): Promise<void> {
    const p = this.seleccionada()
    if (!p || this.enviando()) return
    if (this.decision() === 'RECHAZADA') {
      this.intentoConfirmar.set(true)
      if (this.motivo().trim().length === 0) return
    }
    this.enviando.set(true)
    this.errorDeEnvio.set(null)
    const decision = this.decision()
    try {
      const decisionEnum = decision === 'APROBADA' ? EntradaRevisionDecisionEnum.Aprobada : EntradaRevisionDecisionEnum.Rechazada
      await this.acciones.moderar(p.piezaCreativaId, { decision: decisionEnum, ...(decision === 'RECHAZADA' ? { motivo: this.motivo() } : {}) })
      this.resueltas.update((r) => ({ ...r, [p.piezaCreativaId]: decision }))
      this.cerrar()
    } catch (e) {
      const error = e as ErrorTraducido
      const propio = error?.codigo ? textosPublicidad.errores[error.codigo] : undefined
      this.errorDeEnvio.set(propio ? { ...error, mensaje: propio } : error)
    } finally {
      this.enviando.set(false)
    }
  }
}
