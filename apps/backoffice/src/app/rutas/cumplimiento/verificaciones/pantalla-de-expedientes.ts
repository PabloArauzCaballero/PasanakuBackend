import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import {
  DecisionDeVerificacionDecisionEnum,
  ExpedienteEnRevisionEstadoEnum,
  ExpedienteEnRevisionFotosEnum,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
import { colaVacia, crearPedirFoto, crearResolver, expedientesEnEstado } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad y la decisión.
 *
 * **La decisión es de una persona, y queda con su nombre.** Empieza siendo manual a
 * propósito: un motor que rechaza sin que nadie mire deja a alguien sin cuenta y sin
 * explicación, y el día que se equivoca nadie sabe por qué.
 *
 * Las fotos **no se cargan al listar**: se piden de a una, al mirarlas, y el enlace
 * vive diez minutos. Cada foto de una cédula es un dato personal sensible y cada
 * lectura queda registrada — una grilla que las precarga todas es una filtración
 * cómoda.
 */
@Component({
  selector: 'ap-pantalla-de-expedientes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, BandaDeProposito, ChipEstado, Boton, Campo, FormsModule],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>

      <div class="filtros" role="group" [attr.aria-label]="t.filtrar">
        @for (e of estados; track e) {
          <ap-boton [variante]="estado() === e ? 'primario' : 'fantasma'" (pulsado)="estado.set(e)">{{ e }}</ap-boton>
        }
      </div>

      <ap-estado-de-pantalla
        [recurso]="cola"
        [vacio]="vacio"
        [mensajeVacio]="t.sinPendientes"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="cola.reload()"
      >
        @if (cola.hasValue()) {
          <ul class="cola">
            @for (e of cola.value(); track e.verificacionId) {
              <li>
                <header>
                  <div>
                    <h2>{{ e.nombreCompleto }}</h2>
                    <p class="doc">{{ e.documento ?? t.sinDocumento }}</p>
                  </div>
                  <ap-chip-estado [tono]="tonoDe(e)">{{ e.estado }}</ap-chip-estado>
                </header>

                <p class="fotos">
                  {{ t.fotos }}:
                  @for (cara of caras; track cara) {
                    @if (e.fotos.includes(cara)) {
                      <ap-boton variante="fantasma" (pulsado)="mirar(e.verificacionId, cara)">{{ cara }}</ap-boton>
                    } @else {
                      <span class="falta">{{ cara }} {{ t.faltante }}</span>
                    }
                  }
                </p>

                @if (verUrl()[e.verificacionId]; as url) {
                  <img [src]="url" [alt]="t.fotoDe + ' ' + e.nombreCompleto" />
                }

                @if (pendiente(e)) {
                  @if (!completo(e)) {
                    <p class="aviso">{{ t.expedienteIncompleto }}</p>
                  }
                  <ap-campo
                    [etiqueta]="t.motivo"
                    [ayuda]="t.motivoAyuda"
                    [(valor)]="motivo"
                    [id]="'motivo-' + e.verificacionId"
                  />
                  <div class="acciones">
                    <ap-boton variante="primario" [deshabilitado]="!completo(e) || resolviendo()" (pulsado)="resolver(e, decisiones.Aprobar)">
                      {{ t.aprobar }}
                    </ap-boton>
                    <ap-boton variante="peligro" [deshabilitado]="!motivo() || resolviendo()" (pulsado)="resolver(e, decisiones.Rechazar)">
                      {{ t.rechazar }}
                    </ap-boton>
                  </div>
                } @else if (e.motivoRechazo) {
                  <p class="aviso">{{ t.motivo }}: {{ e.motivoRechazo }}</p>
                }
              </li>
            }
          </ul>
        }
      </ap-estado-de-pantalla>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 52rem; display: flex; flex-direction: column; gap: var(--s4); }
    .filtros { display: flex; gap: var(--s2); flex-wrap: wrap; }
    .cola { list-style: none; padding: 0; display: flex; flex-direction: column; gap: var(--s4); }
    .cola li { border: 1px solid var(--border); border-radius: var(--r-lg); padding: var(--s4); display: flex; flex-direction: column; gap: var(--s3); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05rem; }
    .doc { margin: 0; color: var(--text-2); font-size: .875rem; }
    .fotos { display: flex; align-items: center; gap: var(--s2); flex-wrap: wrap; margin: 0; color: var(--text-2); font-size: .875rem; }
    .falta { color: var(--text-3); }
    img { max-width: 100%; border-radius: var(--r-md); border: 1px solid var(--border); }
    .aviso { margin: 0; color: var(--aviso-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
  `,
})
export class PantallaDeExpedientes {
  protected readonly t = textosCumplimiento.verificaciones
  /** Los del contrato, no una lista paralela: dos listas de lo mismo divergen. */
  protected readonly estados = Object.values(ExpedienteEnRevisionEstadoEnum)
  protected readonly caras = Object.values(ExpedienteEnRevisionFotosEnum)

  protected readonly estado = signal<string>(ExpedienteEnRevisionEstadoEnum.Pendiente)
  protected readonly cola = expedientesEnEstado(this.estado)
  protected readonly vacio = colaVacia
  protected readonly motivo = signal('')
  protected readonly resolviendo = signal(false)
  protected readonly verUrl = signal<Record<string, string>>({})

  protected readonly decisiones = DecisionDeVerificacionDecisionEnum

  private readonly pedirFoto = crearPedirFoto()
  private readonly enviarDecision = crearResolver()

  /** Sin las tres fotos no se aprueba: aprobar a ciegas es no revisar. */
  protected completo(e: ExpedienteEnRevision): boolean {
    return this.caras.every((c) => e.fotos.includes(c))
  }

  /** Un expediente ya resuelto no se vuelve a decidir desde acá. */
  protected pendiente(e: ExpedienteEnRevision): boolean {
    return (
      e.estado === ExpedienteEnRevisionEstadoEnum.Pendiente ||
      e.estado === ExpedienteEnRevisionEstadoEnum.EnRevision
    )
  }

  protected tonoDe(e: ExpedienteEnRevision): 'ok' | 'err' | 'aviso' | 'neutro' {
    if (e.estado === ExpedienteEnRevisionEstadoEnum.Aprobada) return 'ok'
    if (e.estado === ExpedienteEnRevisionEstadoEnum.Rechazada) return 'err'
    return e.estado === ExpedienteEnRevisionEstadoEnum.EnRevision ? 'aviso' : 'neutro'
  }

  protected mirar(verificacionId: string, cara: ExpedienteEnRevisionFotosEnum): void {
    this.pedirFoto(verificacionId, cara).subscribe((enlace) => {
      this.verUrl.update((actual) => ({ ...actual, [verificacionId]: enlace.url }))
    })
  }

  protected resolver(e: ExpedienteEnRevision, decision: DecisionDeVerificacionDecisionEnum): void {
    this.resolviendo.set(true)
    this.enviarDecision(e.verificacionId, { decision, motivo: this.motivo() || undefined }).subscribe({
      next: () => {
        this.resolviendo.set(false)
        this.motivo.set('')
        this.cola.reload()
      },
      error: () => this.resolviendo.set(false),
    })
  }
}
