import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import {
  CaraDelExpediente,
  ExpedienteEnRevisionEstadoEnum,
  type ExpedienteEnRevision,
} from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { crearResolver, expedientesEnEstado, coincideConBusqueda, esPendiente, POR_DECIDIR, recortar, tonoDelExpediente, vencimientoDe } from '../dominio/cu02-expedientes'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { textosCumplimiento } from '../textos'
import { DecisionDeExpediente, type Decision } from './decision-de-expediente'
import { VencimientoDelDocumento } from './vencimiento-del-documento'
import { TiraDeFotos } from './tira-de-fotos'

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad y la decisión.
 */
@Component({
  selector: 'ap-pantalla-de-expedientes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ErrorDeEnvio, EstadoDePantalla, CabeceraDePagina, ChipEstado, Boton, FormsModule, TiraDeFotos, DecisionDeExpediente, VencimientoDelDocumento],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="verificado" />

      <div class="filtros" role="group" data-tutorial-id="cumplimiento-filtros" [attr.aria-label]="t.filtrar">
        @for (e of estados; track e) {
          <ap-boton [variante]="estado() === e ? 'primario' : 'fantasma'" [presionado]="estado() === e" (pulsado)="estado.set(e)">
            {{ etiquetaDeFiltro(e) }}
          </ap-boton>
        }
      </div>

      <label class="busqueda">
        <span>{{ t.buscarExpediente }}</span>
        <input type="search" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" [attr.aria-label]="t.buscarExpediente" />
      </label>

      <ap-estado-de-pantalla
        data-tutorial-id="cumplimiento-cola"
        [recurso]="cola"
        [vacio]="vacio"
        [mensajeVacio]="mensajeVacio()"
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
                    <p class="telefono">{{ t.telefono }}: {{ e.telefono }}</p>
                    <p class="correo">{{ t.correo }}: {{ e.correo || t.sinCorreo }}</p>
                    <p class="doc">{{ e.documento ?? t.sinDocumento }}</p>
                    <ap-vencimiento-del-documento [expediente]="e" />
                  </div>
                  <ap-chip-estado [tono]="tonoDe(e)">{{ e.estado }}</ap-chip-estado>
                </header>

                <ap-tira-de-fotos [expediente]="e" />
                @if (pendiente(e)) {
                  <ap-decision-de-expediente
                    [verificacionId]="e.verificacionId"
                    [completo]="completo(e)"
                    [vencimiento]="vencimientoDe(e)"
                    [ocupado]="resolviendo() !== null"
                    [enCurso]="resolviendo()?.id === e.verificacionId ? resolviendo()!.decision : null"
                    (decidir)="resolver(e, $event)"
                  />
                  <ap-error-de-envio [error]="errores()[e.verificacionId] ?? null" />
                } @else if (e.motivoRechazo) {
                  <p class="aviso">{{ t.motivo }}: {{ e.motivoRechazo }}</p>
                }
              </li>
            }
          </ul>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 52rem; display: flex; flex-direction: column; gap: var(--s4); }
    .filtros { display: flex; gap: var(--s2); flex-wrap: wrap; }
    .busqueda { display: flex; flex-direction: column; gap: var(--s1); max-width: 32rem; color: var(--text-2); font-size: .875rem; }
    .busqueda input { min-height: 2.75rem; padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: inherit; }
    .cola { list-style: none; padding: 0; display: flex; flex-direction: column; gap: var(--s4); }
    .cola li { border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); padding: var(--s4); display: flex; flex-direction: column; gap: var(--s3); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05rem; }
    .doc { margin: 0; color: var(--text-2); font-size: .875rem; }
    .telefono { margin: 0; color: var(--text-2); font-size: .875rem; }
    .correo { margin: 0; color: var(--text-2); font-size: .875rem; }
    .aviso { margin: 0; color: var(--aviso-texto); font-size: .875rem; }
  `,
})
export class PantallaDeExpedientes {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = textosCumplimiento.verificaciones
  /**
   * La cola de trabajo primero y por omisión; después los del contrato, no una lista
   * paralela — dos listas de lo mismo divergen.
   */
  protected readonly estados: readonly string[] = [POR_DECIDIR, ...Object.values(ExpedienteEnRevisionEstadoEnum)]
  protected readonly caras = Object.values(CaraDelExpediente)

  protected readonly estado = signal<string>(POR_DECIDIR)
  protected readonly busqueda = signal('')
  protected readonly cola = expedientesEnEstado(this.estado)
  /**
   * El error de la última decisión, por expediente: se ve en la tarjeta donde se tomó. Se guarda el error traducido
   * entero (no solo el texto) para que `ap-error-de-envio` distinga «no pasó» de «no sabemos si pasó».
   */
  protected readonly errores = signal<Readonly<Record<string, ErrorTraducido>>>({})
  /** La decisión en vuelo; mientras haya una, no se dispara otra. */
  protected readonly resolviendo = signal<{ id: string; decision: Decision['decision'] } | null>(null)
  /**
   * Lo que se pinta. `POR_DECIDIR` se recorta acá porque el backend filtra por UN
   * estado y los que esperan una persona son dos.
   */
  protected readonly visibles = computed(() => {
    return recortar(this.cola.hasValue() ? (this.cola.value() ?? []) : [], this.estado()).filter((e) =>
      coincideConBusqueda(e, this.busqueda()),
    )
  })

  /**
   * Vacío es lo que queda DESPUÉS del recorte: con `POR_DECIDIR` la respuesta trae
   * también los resueltos, y decir «no hay nada» porque el recorte los sacó sería
   * mentir al revés — mostrar la lista vacía sin explicar por qué.
   */
  protected readonly vacio = (): boolean => this.visibles().length === 0
  protected readonly mensajeVacio = computed(() =>
    this.busqueda().trim() ? this.t.sinCoincidencias : this.t.sinPendientes,
  )

  /** `POR_DECIDIR` no es un estado del contrato: se nombra en castellano, no en mayúsculas. */
  protected etiquetaDeFiltro(estado: string): string {
    return estado === POR_DECIDIR ? this.t.filtroPorDecidir : estado
  }

  private readonly enviarDecision = crearResolver()

  /** Sin las cinco fotos no se aprueba: aprobar a ciegas es no revisar. */
  protected completo(e: ExpedienteEnRevision): boolean {
    return this.caras.every((c) => e.fotos.includes(c))
  }

  /** Lo decide el servidor (`documentoVigente`), no el reloj de quien mira. */
  protected readonly vencimientoDe = vencimientoDe

  /** Un expediente ya resuelto no se vuelve a decidir desde acá (regla pura en el dominio). */
  protected readonly pendiente = esPendiente
  protected readonly tonoDe = tonoDelExpediente

  protected resolver(e: ExpedienteEnRevision, { decision, motivo }: Decision): void {
    const id = e.verificacionId
    this.resolviendo.set({ id, decision })
    this.errores.update((m) => {
      const resto = { ...m }
      delete resto[id]
      return resto
    })
    this.enviarDecision(id, { decision, motivo }).subscribe({
      next: () => {
        this.resolviendo.set(null)
        this.cola.reload()
      },
      // Callarlo dejaba al operador creyendo que decidió: el error se ve en la tarjeta.
      error: (err: ErrorTraducido) => {
        this.resolviendo.set(null)
        this.errores.update((m) => ({ ...m, [id]: { ...err, mensaje: err?.mensaje ?? this.t.decisionFallo } }))
      },
    })
  }
}
