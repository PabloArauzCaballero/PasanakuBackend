import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeActivos, crearDepreciacion, motivoParaNoDepreciar, type ActivoFijo } from '../dominio/cu105-activos'
import { EstadoDeListado, observarListado } from '../listado/estado-de-listado'
import { textosContabilidad } from '../textos'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ErrorTraducido } from '../../../nucleo/errores'

const COLUMNAS: ColumnaVirtual<ActivoFijo>[] = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'descripcion', titulo: 'Descripción', ancho: '2' },
  { clave: 'categoriaNombre', titulo: 'Categoría' },
  { clave: 'costoAdquisicion', titulo: 'Costo', numerica: true },
  { clave: 'depreciacionAcumulada', titulo: 'Depreciación acumulada', numerica: true },
  { clave: 'valorEnLibros', titulo: 'Valor en libros', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'activoFijoId', titulo: 'Depreciación' },
]

/**
 * CU-105 · Inventario de activos fijos, sobre `TablaDeDatosVirtualizada` (la del shell), paginado por cursor en el orden
 * del servidor (adquisición más reciente primero).
 * `valor_en_libros` es columna `GENERATED` en la base: se muestra tal como vino, **no se
 * recalcula acá**, y como todo importe pasa por el átomo `Monto`.
 *
 * Un activo agotado, dado de baja o ya corrido en el período no ofrece depreciar: en su
 * lugar queda el motivo, que es lo que el operador necesita leer para entender por qué.
 */
@Component({
  selector: 'ap-pantalla-de-activos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ErrorDeEnvio, BarraDeFiltros, Boton, CabeceraDePagina, ChipEstado, EstadoDeListado, Monto, TablaDeDatosVirtualizada],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="casa" />
      <ap-error-de-envio [error]="errorDeEnvio()" />
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-estado-de-listado [error]="listado.error()" [vacio]="listado.vacio()" [mensajeVacio]="t.enCero" [filtrado]="!!filtrosActivos()['estado']" />
      @if (!listado.error() && !listado.vacio()) {
        <ap-tabla-de-datos-virtualizada
          [titulo]="t.titulo"
          [columnas]="COLUMNAS"
          [cargador]="listado.cargador"
          [ordenPermitido]="[]"
          [identidad]="identidad"
          [filtros]="filtrosActivos()"
        >
          <ng-template #celda let-a let-columna="columna">
            @switch (columna.clave) {
              @case ('costoAdquisicion') {
                <ap-monto [monto]="a.costoAdquisicion.monto" [moneda]="a.costoAdquisicion.moneda" [etiqueta]="t.costo" />
              }
              @case ('depreciacionAcumulada') {
                <ap-monto [monto]="a.depreciacionAcumulada.monto" [moneda]="a.depreciacionAcumulada.moneda" [etiqueta]="t.acumulada" />
              }
              @case ('valorEnLibros') {
                <ap-monto [monto]="a.valorEnLibros.monto" [moneda]="a.valorEnLibros.moneda" [etiqueta]="t.valorEnLibros" />
              }
              @case ('estado') {
                <ap-chip-estado [tono]="a.estado === 'ACTIVO' ? 'ok' : 'neutro'">{{ etiqueta(a) }}</ap-chip-estado>
              }
              @case ('activoFijoId') {
                @if (motivo(a); as porQueNo) {
                  <span class="motivo">{{ porQueNo }}</span>
                } @else {
                  <ap-boton [cargando]="enviando() === a.activoFijoId" (pulsado)="depreciar(a)">{{ t.depreciar }}</ap-boton>
                }
              }
              @default {
                {{ a[columna.clave] }}
              }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 84rem; display: grid; gap: var(--s4); }
    .motivo { color: var(--text-2); white-space: normal; }
  `,
})
export class PantallaDeActivos {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosContabilidad.activos
  protected readonly COLUMNAS = COLUMNAS
  readonly periodoVigenteId = input.required<string>()
  protected readonly listado = observarListado(cargadorDeActivos(), textosContabilidad.erroresDeListado)
  protected readonly identidad = (a: ActivoFijo) => a.activoFijoId
  protected readonly etiqueta = (a: ActivoFijo) => this.t.estados[a.estado]
  protected readonly enviando = signal<string | null>(null)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  private readonly sesion = inject(Sesion)
  private readonly enviarDepreciacion = crearDepreciacion()
  protected readonly puedeDepreciar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_ACTIVOS_FIJOS'))
  protected motivo = (a: ActivoFijo): string | null => motivoParaNoDepreciar(a, this.puedeDepreciar())
  protected readonly filtros: Filtro[] = [
    { valor: 'ACTIVO', texto: 'En uso' },
    { valor: 'DADO_DE_BAJA', texto: 'Dado de baja' },
    { valor: 'VENDIDO', texto: 'Vendido' },
  ]
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    const filtros: Record<string, string> = {}
    if (elegido) filtros['estado'] = elegido
    return filtros
  })

  protected depreciar(a: ActivoFijo): void {
    this.enviando.set(a.activoFijoId)
    this.errorDeEnvio.set(null)
    this.enviarDepreciacion(a.activoFijoId, this.periodoVigenteId()).subscribe({
      next: () => this.enviando.set(null),
      error: (e: ErrorTraducido) => {
        this.errorDeEnvio.set(e)
        this.enviando.set(null)
      },
    })
  }
}
