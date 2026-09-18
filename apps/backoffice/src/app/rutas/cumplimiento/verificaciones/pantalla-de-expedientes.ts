import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core'
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
import { colaVacia, crearResolver, expedientesEnEstado, POR_DECIDIR, recortar } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'
import { TiraDeFotos } from './tira-de-fotos'

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad y la decisión.
 *
 * **La decisión es de una persona, y queda con su nombre.** Empieza siendo manual a
 * propósito: un motor que rechaza sin que nadie mire deja a alguien sin cuenta y sin
 * explicación, y el día que se equivoca nadie sabe por qué.
 *
 * Las fotos **no se cargan al listar**: se piden al abrir el expediente, y el enlace
 * vive diez minutos. Cada foto de una cédula es un dato personal sensible y cada
 * lectura queda registrada — una grilla que las precarga todas es una filtración
 * cómoda.
 *
 * Abierto un expediente, las tres se piden juntas y se muestran **una al lado de la
 * otra**: la prueba de vida se decide cotejando la cara contra el documento, y
 * cotejar es mirar las dos a la vez. Antes cada foto pisaba a la anterior y había que
 * decidir de memoria.
 */
@Component({
  selector: 'ap-pantalla-de-expedientes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla, BandaDeProposito, ChipEstado, Boton, Campo, FormsModule, TiraDeFotos],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>

      <div class="filtros" role="group" data-tutorial-id="cumplimiento-filtros" [attr.aria-label]="t.filtrar">
        @for (e of estados; track e) {
          <ap-boton [variante]="estado() === e ? 'primario' : 'fantasma'" (pulsado)="estado.set(e)">
            {{ etiquetaDeFiltro(e) }}
          </ap-boton>
        }
      </div>

      <ap-estado-de-pantalla
        data-tutorial-id="cumplimiento-cola"
        [recurso]="cola"
        [vacio]="vacio"
        [mensajeVacio]="t.sinPendientes"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="cola.reload()"
      >
        @if (cola.hasValue()) {
          <ul class="cola">
            @for (e of visibles(); track e.verificacionId) {
              <li>
                <header>
                  <div>
                    <h2>{{ e.nombreCompleto }}</h2>
                    <p class="doc">{{ e.documento ?? t.sinDocumento }}</p>
                  </div>
                  <ap-chip-estado [tono]="tonoDe(e)">{{ e.estado }}</ap-chip-estado>
                </header>

                <ap-tira-de-fotos [expediente]="e" />

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
    .cola li { border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); padding: var(--s4); display: flex; flex-direction: column; gap: var(--s3); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05rem; }
    .doc { margin: 0; color: var(--text-2); font-size: .875rem; }
    .aviso { margin: 0; color: var(--aviso-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
  `,
})
export class PantallaDeExpedientes {
  protected readonly t = textosCumplimiento.verificaciones
  /**
   * La cola de trabajo primero y por omisión; después los del contrato, no una lista
   * paralela — dos listas de lo mismo divergen.
   */
  protected readonly estados: readonly string[] = [POR_DECIDIR, ...Object.values(ExpedienteEnRevisionEstadoEnum)]
  protected readonly caras = Object.values(ExpedienteEnRevisionFotosEnum)

  protected readonly estado = signal<string>(POR_DECIDIR)
  protected readonly cola = expedientesEnEstado(this.estado)
  protected readonly motivo = signal('')
  protected readonly resolviendo = signal(false)
  /**
   * Lo que se pinta. `POR_DECIDIR` se recorta acá porque el backend filtra por UN
   * estado y los que esperan una persona son dos.
   */
  protected readonly visibles = computed(() => recortar(this.cola.value() ?? [], this.estado()))

  /**
   * Vacío es lo que queda DESPUÉS del recorte: con `POR_DECIDIR` la respuesta trae
   * también los resueltos, y decir «no hay nada» porque el recorte los sacó sería
   * mentir al revés — mostrar la lista vacía sin explicar por qué.
   */
  protected readonly vacio = (c: ExpedienteEnRevision[]): boolean => colaVacia(recortar(c, this.estado()))

  /** `POR_DECIDIR` no es un estado del contrato: se nombra en castellano, no en mayúsculas. */
  protected etiquetaDeFiltro(estado: string): string {
    return estado === POR_DECIDIR ? this.t.filtroPorDecidir : estado
  }

  protected readonly decisiones = DecisionDeVerificacionDecisionEnum

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

  protected tonoDe(e: ExpedienteEnRevision): 'ok' | 'error' | 'aviso' | 'neutro' {
    if (e.estado === ExpedienteEnRevisionEstadoEnum.Aprobada) return 'ok'
    if (e.estado === ExpedienteEnRevisionEstadoEnum.Rechazada) return 'error'
    return e.estado === ExpedienteEnRevisionEstadoEnum.EnRevision ? 'aviso' : 'neutro'
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
