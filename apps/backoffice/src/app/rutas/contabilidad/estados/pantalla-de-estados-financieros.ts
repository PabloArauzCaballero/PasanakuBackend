import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { SeccionDeExpediente } from '@aportaya/ui/seccion-de-expediente/seccion-de-expediente'
import {
  crearDescarga,
  crearGeneracion,
  DocumentoDiscrepante,
  esProvisorio,
  type DocumentoDescargado,
  type EstadoFinancieroGenerado,
  type TipoDeEstadoFinanciero,
} from '../dominio/cu106-estados-financieros'
import { textosContabilidad } from '../textos'

/**
 * CU-106 · `VisorEstadoFinanciero`. **Gate propio del carril B3: el estado financiero se
 * descarga con su hash.** El hash es el que devolvió el backend al generar el estado
 * (`hashContenido` de `SalidaEstadoFinanciero`), y esta pantalla **no lo recalcula ni lo
 * recompone**: lo muestra, lo pone en el nombre del archivo y lo compara con el que el
 * backend repite junto al documento. Si difieren, no se entrega el archivo.
 *
 * Un período abierto solo da un estado **provisorio** (CU-106, 2a): la pantalla lo dice
 * con esas palabras, en vez de dejar que alguien lo confunda con el definitivo.
 */
@Component({
  selector: 'ap-pantalla-de-estados-financieros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, BandaDeProposito, Boton, Fecha, GrupoRadio, SeccionDeExpediente],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-alerta [tono]="provisorio() ? 'aviso' : 'info'">{{ provisorio() ? t.provisorio : t.definitivo }}</ap-alerta>

      <ap-grupo-radio nombre="tipo-de-estado" etiqueta="Tipo de estado financiero" [opciones]="tipos" [(elegido)]="tipo" />
      <ap-boton variante="primario" [cargando]="generando()" (pulsado)="generar()">{{ t.generar }}</ap-boton>

      @if (generado(); as g) {
        <ap-alerta [tono]="g.cuadra ? 'ok' : 'error'">{{ g.cuadra ? t.cuadra : t.noCuadra }}</ap-alerta>
        <h2>Estado financiero generado</h2>
        <ap-seccion-de-expediente [titulo]="etiquetaDeTipo(g.tipo)" [datos]="datosDe(g)" [actualizadoIso]="g.generadoEn">
          <p class="hash-ayuda">{{ t.hashExplicacion }}</p>
          <ap-fecha [iso]="g.generadoEn" />
          <ap-boton [cargando]="descargando()" (pulsado)="descargar(g)">{{ t.descargar }}</ap-boton>
        </ap-seccion-de-expediente>
      }

      @if (descarga(); as d) {
        <p class="listo">
          Documento listo: <strong>{{ d.nombreDeArchivo }}</strong>
          @if (!d.verificado) { <span class="sinVerificar">El backend no repitió el hash junto al documento: no se pudo comparar.</span> }
        </p>
      }
      @if (errorDeDescarga(); as e) { <ap-alerta tono="error">{{ e }}</ap-alerta> }
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 48rem; display: grid; gap: var(--s4); justify-items: start; }
    h1, h2 { margin: 0; }
    h2 { font-size: 1.05em; }
    .hash-ayuda { color: var(--text-2); margin: 0; }
    .listo { margin: 0; }
    .sinVerificar { display: block; color: var(--aviso-texto); }
  `,
})
export class PantallaDeEstadosFinancieros {
  protected readonly t = textosContabilidad.estados
  readonly periodoId = input.required<string>()
  readonly periodoNombre = input('Período')
  readonly periodoCerrado = input(false)
  protected readonly tipo = signal<string>('BALANCE_GENERAL')
  protected readonly tipos = [
    { valor: 'BALANCE_GENERAL', texto: 'Balance general' },
    { valor: 'ESTADO_RESULTADOS', texto: 'Estado de resultados' },
  ]
  protected readonly generando = signal(false)
  protected readonly descargando = signal(false)
  protected readonly generado = signal<EstadoFinancieroGenerado | null>(null)
  protected readonly descarga = signal<DocumentoDescargado | null>(null)
  protected readonly errorDeDescarga = signal<string | null>(null)
  protected readonly provisorio = computed(() => esProvisorio(this.periodoCerrado()))
  private readonly enviarGeneracion = crearGeneracion()
  private readonly pedirDocumento = crearDescarga()

  // Aserción sobre el ÍNDICE, no sobre el contrato: `t.tipos` es un diccionario local de
  // traducción con claves literales; esto no cambia el comportamiento si `tipo` no está.
  protected etiquetaDeTipo = (tipo: TipoDeEstadoFinanciero) => this.t.tipos[tipo as keyof typeof this.t.tipos]

  /** El hash viaja tal cual llegó del backend: acá solo se lo nombra y se lo muestra. */
  protected datosDe(g: EstadoFinancieroGenerado) {
    return [
      { nombre: this.t.hash, valor: g.hashContenido },
      { nombre: 'Identificador', valor: g.estadoFinancieroId },
      { nombre: 'Cuadre', valor: g.cuadra ? this.t.cuadra : this.t.noCuadra },
    ]
  }

  protected generar(): void {
    this.generando.set(true)
    this.descarga.set(null)
    this.errorDeDescarga.set(null)
    this.enviarGeneracion(this.periodoId(), this.tipo() as TipoDeEstadoFinanciero).subscribe({
      next: (g) => {
        this.generado.set(g)
        this.generando.set(false)
      },
      error: () => this.generando.set(false),
    })
  }

  protected descargar(g: EstadoFinancieroGenerado): void {
    this.descargando.set(true)
    this.errorDeDescarga.set(null)
    this.pedirDocumento(g, this.periodoNombre()).subscribe({
      next: (d) => {
        this.descarga.set(d)
        this.descargando.set(false)
      },
      error: (e: unknown) => {
        this.descargando.set(false)
        this.descarga.set(null)
        this.errorDeDescarga.set(e instanceof DocumentoDiscrepante ? this.t.hashDiscrepante : 'No se pudo traer el documento del estado financiero.')
      },
    })
  }
}
