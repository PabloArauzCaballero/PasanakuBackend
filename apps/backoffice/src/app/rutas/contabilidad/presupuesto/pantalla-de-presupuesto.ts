import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { calcularDesviacion, cargadorDePartidas, sobreEjecutada, type PartidaPresupuestaria } from '../dominio/cu101-presupuestos'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<PartidaPresupuestaria>[] = [
  { clave: 'centroCostoNombre', titulo: 'Centro de costo', ordenable: true },
  { clave: 'cuentaContableNombre', titulo: 'Cuenta contable' },
  { clave: 'periodoNombre', titulo: 'Período', ordenable: true },
  { clave: 'montoPresupuestado', titulo: 'Presupuestado', numerica: true, ordenable: true },
  { clave: 'montoEjecutado', titulo: 'Ejecutado', numerica: true, ordenable: true },
  { clave: 'partidaId', titulo: 'Desviación', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
]

/**
 * CU-101 · Presupuestar por centro de costo, sobre `TablaDeDatosVirtualizada` (la del
 * shell: no se duplica ninguna grilla). Las partidas sobre-ejecutadas van primero
 * —las ordena el `cargador` del dominio, no la tabla— y la desviación se resalta.
 *
 * El presupuesto **mide, no bloquea** (CU-101, 4a): pasarse no deshabilita nada acá,
 * registra una desviación que alguien tiene que explicar. Todos los importes, incluida
 * la desviación, pasan por el átomo `Monto`.
 */
@Component({
  selector: 'ap-pantalla-de-presupuesto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, ChipEstado, Monto, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <p class="aviso">{{ t.noBloquea }}</p>
      <ap-barra-de-filtros etiqueta="Estado" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.titulo"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['centroCostoNombre', 'periodoNombre', 'montoPresupuestado', 'montoEjecutado']"
        [identidad]="identidad"
        [filtros]="filtrosActivos()"
      >
        <ng-template #celda let-p let-columna="columna">
          @switch (columna.clave) {
            @case ('montoPresupuestado') {
              <ap-monto [monto]="p.montoPresupuestado.monto" [moneda]="p.montoPresupuestado.moneda" [etiqueta]="t.presupuestado" />
            }
            @case ('montoEjecutado') {
              <ap-monto [monto]="p.montoEjecutado.monto" [moneda]="p.montoEjecutado.moneda" [etiqueta]="t.ejecutado" />
            }
            @case ('partidaId') {
              <span class="desviacion" [class.sobre]="sobre(p)">
                <ap-monto [monto]="desviacion(p).monto" [moneda]="desviacion(p).moneda" [etiqueta]="t.desviacion" />
                @if (sobre(p)) { <span class="marca">{{ t.sobreEjecutado }}</span> }
              </span>
            }
            @case ('estado') {
              <ap-chip-estado [tono]="p.estado === 'APROBADO' ? 'ok' : 'neutro'">{{ etiqueta(p) }}</ap-chip-estado>
            }
            @default {
              {{ p[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .aviso { color: var(--text-2); margin: 0; }
    .desviacion { display: inline-flex; align-items: center; gap: var(--s2); justify-content: flex-end; }
    .desviacion.sobre { color: var(--err-texto); }
    .marca { font-size: .8em; }
  `,
})
export class PantallaDePresupuesto {
  protected readonly t = textosContabilidad.presupuesto
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDePartidas()
  protected readonly identidad = (p: PartidaPresupuestaria) => p.partidaId
  protected readonly desviacion = calcularDesviacion
  protected readonly sobre = sobreEjecutada
  protected readonly etiqueta = (p: PartidaPresupuestaria) => this.t.estados[p.estado]
  protected readonly filtros: Filtro[] = [
    { valor: 'BORRADOR', texto: 'Borrador' },
    { valor: 'APROBADO', texto: 'Aprobado' },
    { valor: 'CERRADO', texto: 'Cerrado' },
  ]
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    const filtros: Record<string, string> = {}
    if (elegido) filtros['estado'] = elegido
    return filtros
  })
}
