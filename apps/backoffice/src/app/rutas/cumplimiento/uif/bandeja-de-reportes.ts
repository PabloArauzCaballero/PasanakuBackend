import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipsDeFiltro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { ReporteRegulatorioEnLista, SalidaReporteRegulatorio } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { tieneAlguno } from '../compartido/acceso'
import { filtrosDe, observarBandeja } from '../compartido/bandeja'
import { opcionesDe } from '../compartido/campos'
import { EstadoDeBandejaVista } from '../compartido/estado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeReportes, sePuedeEnviar, type Recibo } from '../dominio/cu43-reportes'
import { textosUif } from '../textos-uif'
import { AprobarYEnviar } from './aprobar-y-enviar'
import { PrepararReporte } from './preparar-reporte'

const T = textosUif.reportes
const COLUMNAS: ColumnaVirtual<ReporteRegulatorioEnLista>[] = [
  { clave: 'periodo', titulo: T.columnas.periodo },
  { clave: 'fechaCorte', titulo: T.columnas.fechaCorte },
  { clave: 'fechaLimite', titulo: T.columnas.fechaLimite },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'cantidadRegistros', titulo: T.columnas.cantidadRegistros, numerica: true },
  { clave: 'montoTotal', titulo: T.columnas.montoTotal, numerica: true },
  { clave: 'reporteId', titulo: T.columnas.reporteId, ancho: '2' },
]

/**
 * CU-43 · la cola de reportes regulatorios (`listarReportesRegulatorios`). Lectura con
 * CUMPLIMIENTO_REPORTAR o AUDITORIA_LEER; preparar y aprobar solo con CUMPLIMIENTO_REPORTAR.
 * El monto total viaja SIN moneda (reporte_regulatorio no la modela): se muestra la cadena tal
 * cual y la cabecera lo declara —no se le inventa una moneda—.
 */
@Component({
  selector: 'ap-bandeja-de-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, EstadoDeBandejaVista, TablaDeDatosVirtualizada, ChipsDeFiltro, Boton, Alerta, PrepararReporte, AprobarYEnviar],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito">
      @if (puedeOperar()) { <ap-boton acciones variante="primario" (pulsado)="preparando.set(true)">{{ t.preparar }}</ap-boton> }
      <ap-chips-de-filtro [etiqueta]="t.filtroEstado" [filtros]="FILTROS" [(elegidos)]="estados" />
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-estado-de-bandeja [estado]="bandeja.estado()" [error]="bandeja.error()" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [conFiltros]="estados().length > 0" (reintentar)="bandeja.recargar()" (quitarFiltros)="estados.set([])" />
      <p class="nota">{{ t.sinMoneda }}</p>
      @for (m of [bandeja.montaje()]; track m) {
        <div [hidden]="bandeja.estado() === 'denegado'">
          <ap-tabla-de-datos-virtualizada [titulo]="t.tabla" [columnas]="COLUMNAS" [cargador]="bandeja.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtros()" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('fechaCorte') { {{ dia(r.fechaCorte) }} }
                @case ('fechaLimite') { {{ dia(r.fechaLimite) }} }
                @case ('estado') { {{ estados_[r.estado] ?? r.estado }} }
                @case ('cantidadRegistros') { {{ r.reporteEnCero ? t.enCero : r.cantidadRegistros }} }
                @case ('reporteId') {
                  @if (puedeOperar() && sePuedeEnviar(r)) { <ap-boton tamano="sm" (pulsado)="aprobar(r)">{{ t.aprobar }} {{ r.periodo }}</ap-boton> }
                }
                @default { {{ r[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        </div>
      }
    </ap-marco-de-cumplimiento>
    @if (puedeOperar()) {
      <ap-preparar-reporte [(abierto)]="preparando" (preparo)="alPreparar($event)" />
      <ap-aprobar-y-enviar [(abierto)]="aprobando" [reporte]="elegido()" (envio)="alEnviar($event)" />
    }
  `,
  styles: `.nota { margin: 0; color: var(--text-3); font-size: .9em; }`,
})
export class BandejaDeReportes {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS = opcionesDe(textosUif.estadosDeReporte)
  protected readonly estados_ = textosUif.estadosDeReporte
  protected readonly bandeja = observarBandeja(cargadorDeReportes())
  protected readonly identidad = (r: ReporteRegulatorioEnLista) => r.reporteId
  protected readonly dia = formatearDia
  protected readonly sePuedeEnviar = sePuedeEnviar

  protected readonly estados = signal<string[]>([])
  protected readonly filtros = computed(() => filtrosDe({ estados: this.estados() }))
  protected readonly puedeOperar = computed(() => tieneAlguno(this.sesion, ['CUMPLIMIENTO_REPORTAR']))
  protected readonly preparando = signal(false)
  protected readonly aprobando = signal(false)
  protected readonly elegido = signal<ReporteRegulatorioEnLista | null>(null)
  protected readonly recibo = signal<string | null>(null)

  protected aprobar(reporte: ReporteRegulatorioEnLista): void {
    this.elegido.set(reporte)
    this.aprobando.set(true)
  }

  protected alPreparar({ salida, yaExistia }: Recibo<SalidaReporteRegulatorio>): void {
    this.recibo.set(textosUif.preparacion.recibo(salida.cantidadRegistros, salida.reporteEnCero, salida.fueraDePlazo, yaExistia))
    this.bandeja.recargar()
  }

  protected alEnviar({ salida, yaExistia }: Recibo<SalidaReporteRegulatorio>): void {
    this.recibo.set(textosUif.envio.recibo(salida.fueraDePlazo, yaExistia, salida.hashArchivo))
    this.bandeja.recargar()
  }
}
