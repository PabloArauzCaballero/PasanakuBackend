import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Icono } from '@aportaya/ui/icono/icono'
import { SeccionDeExpediente } from '@aportaya/ui/seccion-de-expediente/seccion-de-expediente'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { nombreDelPeriodo, periodoVigente } from '../dominio/cu100-periodos'
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
import { guardarArchivo } from './guardar-archivo'

/**
 * CU-106 · `VisorEstadoFinanciero`, en una tarjeta centrada. **El estado financiero se descarga con su hash.** El hash
 * es el que devolvió el backend al generar (`hashContenido`), y esta pantalla **no lo recalcula**: lo muestra, lo pone
 * en el nombre del archivo y lo compara con el que el backend repite junto al documento. Si difieren, no se entrega.
 *
 * «Descargar el documento» **entrega el archivo** al navegador (antes lo traía y no lo guardaba: el botón no hacía
 * nada visible). Un período abierto solo da un estado **provisorio** (CU-106, 2a): si el período de la URL es el de hoy,
 * su nombre y estado salen de `GET /erp/periodos/vigente`; si no, de las entradas de la ruta.
 */
@Component({
  selector: 'ap-pantalla-de-estados-financieros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ErrorDeEnvio, Alerta, Boton, CabeceraDePagina, GrupoRadio, Icono, SeccionDeExpediente],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="balanza" />
      <div class="tarjeta">
        <p class="periodo"><ap-icono nombre="calendario" tamano="s4" />Período: <strong>{{ nombre() }}</strong></p>
        <ap-alerta [tono]="provisorio() ? 'aviso' : 'info'">{{ provisorio() ? t.provisorio : t.definitivo }}</ap-alerta>
        <ap-grupo-radio nombre="tipo-de-estado" etiqueta="Tipo de estado financiero" [opciones]="tipos" [(elegido)]="tipo" />
        <ap-boton variante="primario" icono="grafico" [cargando]="generando()" (pulsado)="generar()">{{ t.generar }}</ap-boton>
        <ap-error-de-envio [error]="errorDeEnvio()" />

        @if (generado(); as g) {
          <ap-alerta [tono]="g.cuadra ? 'ok' : 'error'">{{ g.cuadra ? t.cuadra : t.noCuadra }}</ap-alerta>
          <h2><ap-icono nombre="documento" tamano="s4" />Estado financiero generado</h2>
          <ap-seccion-de-expediente [titulo]="etiquetaDeTipo(g.tipo)" [datos]="datosDe(g)" [actualizadoIso]="g.generadoEn">
            <p class="hash-ayuda">{{ t.hashExplicacion }}</p>
            <ap-boton icono="descargar" [cargando]="descargando()" (pulsado)="descargar(g)">{{ t.descargar }}</ap-boton>
          </ap-seccion-de-expediente>
        }

        @if (descarga(); as d) {
          <p class="listo" role="status">
            <ap-icono nombre="verificado" tamano="s4" />Documento descargado: <strong>{{ d.nombreDeArchivo }}</strong>
            @if (!d.verificado) { <span class="sinVerificar">El backend no repitió el hash junto al documento: no se pudo comparar.</span> }
          </p>
        }
        @if (errorDeDescarga(); as e) { <ap-alerta tono="error">{{ e }}</ap-alerta> }
      </div>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .tarjeta { display: grid; gap: var(--s4); justify-items: start; width: 100%; max-width: 56rem; margin-inline: auto; padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .tarjeta > ap-alerta, .tarjeta > ap-seccion-de-expediente, .tarjeta > ap-grupo-radio { justify-self: stretch; }
    h2, .periodo, .listo { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); margin: 0; }
    h2 { font-size: 1.05em; }
    .hash-ayuda { color: var(--text-2); margin: 0; }
    .listo strong { overflow-wrap: anywhere; }
    .sinVerificar { display: block; flex-basis: 100%; color: var(--aviso-texto); }
  `,
})
export class PantallaDeEstadosFinancieros {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosContabilidad.estados
  readonly periodoId = input.required<string>()
  readonly periodoNombre = input('Período')
  readonly periodoCerrado = input(false)
  private readonly vigente = periodoVigente()
  /** El período de hoy, solo si es el de la URL: entonces su nombre y estado son los del servidor. */
  private readonly delVigente = computed(() => {
    const v = this.vigente.hasValue() ? this.vigente.value() : null
    return v && v.periodoId === this.periodoId() ? v : null
  })
  protected readonly nombre = computed(() => {
    const v = this.delVigente()
    return v ? nombreDelPeriodo(v.anio, v.mes) : this.periodoNombre()
  })
  protected readonly provisorio = computed(() => esProvisorio(this.delVigente() ? this.delVigente()!.estado === 'CERRADO' : this.periodoCerrado()))
  protected readonly tipo = signal<string>('BALANCE_GENERAL')
  protected readonly tipos = [
    { valor: 'BALANCE_GENERAL', texto: 'Balance general' },
    { valor: 'ESTADO_RESULTADOS', texto: 'Estado de resultados' },
  ]
  protected readonly generando = signal(false)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  protected readonly descargando = signal(false)
  protected readonly generado = signal<EstadoFinancieroGenerado | null>(null)
  protected readonly descarga = signal<DocumentoDescargado | null>(null)
  protected readonly errorDeDescarga = signal<string | null>(null)
  private readonly enviarGeneracion = crearGeneracion()
  private readonly pedirDocumento = crearDescarga()

  // Aserción sobre el ÍNDICE, no sobre el contrato: `t.tipos` es un diccionario local de traducción.
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
    if (this.generando()) return
    this.generando.set(true)
    this.errorDeEnvio.set(null)
    this.descarga.set(null)
    this.errorDeDescarga.set(null)
    this.enviarGeneracion(this.periodoId(), this.tipo() as TipoDeEstadoFinanciero).subscribe({
      next: (g) => {
        this.generado.set(g)
        this.generando.set(false)
      },
      error: (e: ErrorTraducido) => {
        this.errorDeEnvio.set(e)
        this.generando.set(false)
      },
    })
  }

  protected descargar(g: EstadoFinancieroGenerado): void {
    if (this.descargando()) return
    this.descargando.set(true)
    this.errorDeDescarga.set(null)
    this.pedirDocumento(g, this.nombre()).subscribe({
      next: (d) => {
        this.descargando.set(false)
        if (guardarArchivo(d.archivo, d.nombreDeArchivo)) this.descarga.set(d)
        else this.errorDeDescarga.set('Este navegador no permitió guardar el archivo. Volvé a intentar desde otro navegador.')
      },
      error: (e: unknown) => {
        this.descargando.set(false)
        this.descarga.set(null)
        this.errorDeDescarga.set(e instanceof DocumentoDiscrepante ? this.t.hashDiscrepante : 'No se pudo traer el documento del estado financiero. Volvé a intentar.')
      },
    })
  }
}
