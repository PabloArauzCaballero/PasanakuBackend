import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { textoDeImporte } from '../dominio/contrato-erp'
import { admiteDepreciacion, cargadorDeActivos, crearDepreciacion, motivoParaNoDepreciar, type ActivoFijo } from '../dominio/cu105-activos'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../listado/listado-por-cursor'
import { TablaPorCursor } from '../listado/tabla-por-cursor'
import { textosContabilidad } from '../textos'

const t = textosContabilidad.activos

const COLUMNAS: Columna<ActivoFijo>[] = [
  { clave: 'codigo', titulo: 'Código', mono: true },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'valorEnLibros', titulo: 'Valor en libros', numerica: true },
  { clave: 'costoAdquisicion', titulo: 'Costo', numerica: true },
  { clave: 'depreciacionAcumulada', titulo: 'Depreciación acumulada', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'categoriaNombre', titulo: 'Categoría' },
]

const EXPORTABLES = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'descripcion', titulo: 'Descripción' },
  { clave: 'categoria', titulo: 'Categoría', valor: (a: ActivoFijo) => a.categoriaNombre },
  { clave: 'fechaAdquisicion', titulo: 'Adquirido' },
  { clave: 'vidaUtilMeses', titulo: 'Vida útil (meses)' },
  { clave: 'costo', titulo: 'Costo', valor: (a: ActivoFijo) => textoDeImporte(a.costoAdquisicion) },
  { clave: 'acumulada', titulo: 'Depreciación acumulada', valor: (a: ActivoFijo) => textoDeImporte(a.depreciacionAcumulada) },
  { clave: 'valorEnLibros', titulo: 'Valor en libros', valor: (a: ActivoFijo) => textoDeImporte(a.valorEnLibros) },
  { clave: 'estado', titulo: 'Estado', valor: (a: ActivoFijo) => t.estados[a.estado] },
]

/**
 * CU-105 · Inventario de activos fijos, listado por cursor del servidor (adquisición más reciente primero). El estado
 * viaja como `estados`; el buscador y la categoría filtran la página cargada. `valor_en_libros` es `GENERATED` en la
 * base: se muestra como vino, no se recalcula, y como todo importe pasa por `ap-monto`.
 *
 * **Acción de fila: «Depreciar»** (`depreciarActivo` con el período de la URL e Idempotency-Key), solo sobre un activo en
 * uso, no agotado y sin cuota en el período; si no, queda el motivo. No hay Editar ni Eliminar en `erp.yaml`. Tras
 * depreciar, la lista se recarga (la cuota cambia el valor en libros) y se dice qué pasó.
 */
@Component({
  selector: 'ap-pantalla-de-activos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, CabeceraDePagina, ChipEstado, ErrorDeEnvio, Icono, Monto, TablaPorCursor, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Contabilidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="paquete" />
      <!-- Las cifras cuentan lo cargado: sin carga (error o cargando) no se muestran, un «0» afirmaría algo que no se midió. -->
      @if (listado.carga().tipo === 'lista') {
        <div class="kpis">
          <ap-tarjeta-kpi etiqueta="Activos en la página" [valor]="'' + listado.filas().length" icono="paquete" />
          <ap-tarjeta-kpi etiqueta="Por depreciar en la página" [valor]="'' + porDepreciar()" icono="calendario" [tono]="porDepreciar() > 0 ? 'aviso' : 'ok'" />
          <ap-tarjeta-kpi etiqueta="Fuera de uso en la página" [valor]="'' + fueraDeUso()" icono="prohibido" />
        </div>
      }
      @if (depreciado(); as codigo) { <ap-alerta tono="ok" titulo="Cuota calculada">El activo {{ codigo }} ya tiene su cuota del período. La lista muestra el valor en libros nuevo.</ap-alerta> }
      <ap-error-de-envio [error]="errorDeEnvio()" />
      <ap-tabla-por-cursor [titulo]="t.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="activos" [textoVacio]="t.enCero" ayudaDeBusqueda="Busca por código, descripción o categoría en la página cargada: el servidor no busca por texto.">
        <ng-template #celda let-a let-columna="columna">
          @switch (columna.clave) {
            @case ('costoAdquisicion') { <ap-monto [monto]="a.costoAdquisicion.monto" [moneda]="a.costoAdquisicion.moneda" [etiqueta]="t.costo" /> }
            @case ('depreciacionAcumulada') { <ap-monto [monto]="a.depreciacionAcumulada.monto" [moneda]="a.depreciacionAcumulada.moneda" [etiqueta]="t.acumulada" /> }
            @case ('valorEnLibros') { <ap-monto [monto]="a.valorEnLibros.monto" [moneda]="a.valorEnLibros.moneda" [etiqueta]="t.valorEnLibros" /> }
            @case ('estado') { <ap-chip-estado [tono]="a.estado === 'ACTIVO' ? 'ok' : 'neutro'">{{ etiqueta(a) }}</ap-chip-estado> }
            @default { <span class="texto">{{ a[columna.clave] }}</span> }
          }
        </ng-template>
        <ng-template #acciones let-a>
          @if (motivo(a); as porQueNo) {
            <span class="motivo">{{ porQueNo }}</span>
          } @else {
            <ap-acciones-de-fila [fila]="'Activo ' + a.codigo">
              <button type="button" apAccionDeFila [disabled]="enviando() !== null" [attr.aria-busy]="enviando() === a.activoFijoId || null" [attr.aria-label]="t.depreciar + ' el activo ' + a.codigo" (click)="depreciar(a)"><ap-icono nombre="tendencia" tamano="s4" /><span>{{ enviando() === a.activoFijoId ? 'Depreciando…' : t.depreciar }}</span></button>
            </ap-acciones-de-fila>
          }
        </ng-template>
      </ap-tabla-por-cursor>
    </section>
  `,
  styles: `
    /* Los textos largos se parten en la celda: así la tabla entra en su marco y la columna fija de acciones no tapa un importe. */
    .texto { display: inline-block; min-width: 8rem; white-space: normal; overflow-wrap: anywhere; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr)); gap: var(--s3); }
    .motivo { display: block; max-width: 13rem; color: var(--text-2); white-space: normal; font: var(--t-cuerpo-chico); }
  `,
})
export class PantallaDeActivos {
  constructor() {
    inject(ExplicadorDeVista).fijar(t.proposito)
  }

  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  readonly periodoVigenteId = input.required<string>()
  protected readonly identidad = (a: ActivoFijo) => a.activoFijoId
  protected readonly etiqueta = (a: ActivoFijo) => t.estados[a.estado]
  protected readonly enviando = signal<string | null>(null)
  protected readonly depreciado = signal<string | null>(null)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  private readonly sesion = inject(Sesion)
  private readonly enviarDepreciacion = crearDepreciacion()
  protected readonly puedeDepreciar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_ACTIVOS_FIJOS'))
  protected readonly motivo = (a: ActivoFijo): string | null => motivoParaNoDepreciar(a, this.puedeDepreciar())
  protected readonly listado = crearListadoPorCursor<ActivoFijo>({
    cargador: cargadorDeActivos(),
    camposDeBusqueda: ['codigo', 'descripcion', 'categoriaNombre'],
    servidor: { nombre: 'estado', etiqueta: 'Estado del activo', opciones: opcionesDe(t.estados), inicial: filtroInicialDeLaUrl(opcionesDe(t.estados)) },
    filtros: [{ nombre: 'categoria', etiqueta: 'Categoría', valor: (a) => a.categoriaNombre }],
    textosDeError: textosContabilidad.erroresDeListado,
  })
  protected readonly porDepreciar = computed(() => this.listado.filas().filter(admiteDepreciacion).length)
  protected readonly fueraDeUso = computed(() => this.listado.filas().filter((a) => a.estado !== 'ACTIVO').length)

  protected depreciar(a: ActivoFijo): void {
    if (this.enviando()) return
    this.enviando.set(a.activoFijoId)
    this.errorDeEnvio.set(null)
    this.depreciado.set(null)
    this.enviarDepreciacion(a.activoFijoId, this.periodoVigenteId()).subscribe({
      next: () => {
        this.enviando.set(null)
        this.depreciado.set(a.codigo)
        this.listado.recargar()
      },
      error: (e: ErrorTraducido) => {
        this.errorDeEnvio.set(e)
        this.enviando.set(null)
      },
    })
  }
}
