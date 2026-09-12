import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeActivos, crearDepreciacion, motivoParaNoDepreciar, type ActivoFijo } from '../dominio/cu105-activos'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<ActivoFijo>[] = [
  { clave: 'codigo', titulo: 'Código', ordenable: true },
  { clave: 'descripcion', titulo: 'Descripción', ancho: '2' },
  { clave: 'categoriaNombre', titulo: 'Categoría' },
  { clave: 'costoAdquisicion', titulo: 'Costo', numerica: true, ordenable: true },
  { clave: 'depreciacionAcumulada', titulo: 'Depreciación acumulada', numerica: true },
  { clave: 'valorEnLibros', titulo: 'Valor en libros', numerica: true, ordenable: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'activoFijoId', titulo: 'Depreciación' },
]

/**
 * CU-105 · Inventario de activos fijos, sobre `TablaDeDatosVirtualizada` (la del shell).
 * `valor_en_libros` es columna `GENERATED` en la base: se muestra tal como vino, **no se
 * recalcula acá**, y como todo importe pasa por el átomo `Monto`.
 *
 * Un activo agotado, dado de baja o ya corrido en el período no ofrece depreciar: en su
 * lugar queda el motivo, que es lo que el operador necesita leer para entender por qué.
 */
@Component({
  selector: 'ap-pantalla-de-activos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, Boton, ChipEstado, Monto, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['codigo', 'costoAdquisicion', 'valorEnLibros']"
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
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 84rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .motivo { color: var(--text-2); white-space: normal; }
  `,
})
export class PantallaDeActivos {
  protected readonly t = textosContabilidad.activos
  protected readonly COLUMNAS = COLUMNAS
  readonly periodoVigenteId = input.required<string>()
  protected readonly cargador = cargadorDeActivos()
  protected readonly identidad = (a: ActivoFijo) => a.activoFijoId
  protected readonly etiqueta = (a: ActivoFijo) => this.t.estados[a.estado]
  protected readonly enviando = signal<string | null>(null)
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
    this.enviarDepreciacion(a.activoFijoId, this.periodoVigenteId()).subscribe({
      next: () => this.enviando.set(null),
      error: () => this.enviando.set(null),
    })
  }
}
