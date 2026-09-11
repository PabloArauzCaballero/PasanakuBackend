import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual, PedidoDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'
import { Exportador } from '../../../nucleo/exportador'
import { NIVELES, SERVICIOS, type ServicioDeArquitectura } from './dominio/estado-del-sistema'

const COLUMNAS: ColumnaVirtual<ServicioDeArquitectura>[] = [
  { clave: 'servicio', titulo: 'Servicio', ordenable: true },
  { clave: 'nivel', titulo: 'Nivel', ordenable: true },
  { clave: 'replicas', titulo: 'Réplicas', ordenable: true, numerica: true },
  { clave: 'porQue', titulo: 'Por qué ese nivel', ancho: '2' },
  { clave: 'salud', titulo: 'Salud' },
]
const ORDEN_PERMITIDO = ['servicio', 'nivel', 'replicas'] as const

/**
 * `operacion/estado` de la maqueta («Arquitectura y estado del proyecto»): el nivel de
 * criticidad de cada servicio (ADR-037) y cuántas réplicas exige. Es la pantalla de F6
 * que ejercita `TablaDeDatosVirtualizada`, `BarraDeFiltros` (filtro por nivel, en la
 * URL) y `Exportador` sobre un dato real de arquitectura, sin esperar a ningún backend.
 */
@Component({
  selector: 'ap-pantalla-de-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, TablaDeDatosVirtualizada, ChipEstado],
  template: `
    <ap-banda-de-proposito
      texto="Qué hay debajo y en qué estado está de verdad: el nivel de criticidad de cada servicio y cuántas réplicas exige."
    />
    <main>
      <div class="cabecera">
        <h1>Arquitectura y estado del proyecto</h1>
        @if (exportador.puedeExportar('estado-plataforma')) {
          <button type="button" (click)="exportar()">Exportar</button>
        }
      </div>
      <ap-barra-de-filtros etiqueta="Nivel" [incluirBusqueda]="false" [definiciones]="filtrosDeNivel" />
      <ap-tabla-de-datos-virtualizada
        titulo="Servicios por nivel de criticidad"
        [columnas]="columnas"
        [cargador]="cargador"
        [ordenPermitido]="ordenPermitido"
        [tamanoDePagina]="20"
      >
        <ng-template #celda let-fila let-columna="columna">
          @if (columna.clave === 'salud') {
            <ap-chip-estado tono="ok">{{ fila.salud }}</ap-chip-estado>
          } @else if (columna.clave === 'nivel') {
            <ap-chip-estado [tono]="fila.nivel === 'N1' ? 'error' : fila.nivel === 'N2' ? 'aviso' : 'info'">{{ fila.nivel }}</ap-chip-estado>
          } @else {
            {{ fila[columna.clave] }}
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </main>
  `,
  styles: `
    main { padding: var(--s5); display: flex; flex-direction: column; gap: var(--s4); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); }
    h1 { margin: 0; }
    button { min-height: var(--area-tactil); padding: 0 var(--s5); border: 0; border-radius: var(--r-md); background: var(--verde-solido); color: var(--sobre-verde-solido); font: inherit; font-weight: 600; cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaDeEstado {
  protected readonly exportador = inject(Exportador)
  protected readonly columnas = COLUMNAS
  protected readonly ordenPermitido = ORDEN_PERMITIDO
  protected readonly filtrosDeNivel = NIVELES.map((n) => ({ valor: n.valor, texto: n.texto }))

  protected readonly cargador = async (pedido: PedidoDePagina): Promise<PaginaServidor<ServicioDeArquitectura>> => {
    const nivelesElegidos = (pedido.filtros['filtro'] ?? '').split(',').filter(Boolean)
    let filas = nivelesElegidos.length ? SERVICIOS.filter((s) => nivelesElegidos.includes(s.nivel)) : [...SERVICIOS]
    if (pedido.orden) {
      const { clave, sentido } = pedido.orden
      filas = [...filas].sort((a, b) => {
        const va = a[clave as keyof ServicioDeArquitectura]
        const vb = b[clave as keyof ServicioDeArquitectura]
        const cmp = va < vb ? -1 : va > vb ? 1 : 0
        return sentido === 'asc' ? cmp : -cmp
      })
    }
    const total = filas.length
    const desde = (pedido.pagina - 1) * pedido.tamano
    return { filas: filas.slice(desde, desde + pedido.tamano), total }
  }

  exportar(): void {
    this.exportador.solicitar({ recurso: 'estado-plataforma', formato: 'csv', filtros: {} }).subscribe()
  }
}
