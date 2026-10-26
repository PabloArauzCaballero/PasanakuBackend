import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ReporteRegulatorioEnLista, SalidaReporteRegulatorio } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeReportes, sePuedeEnviar, type Recibo } from '../dominio/cu43-reportes'
import { textosUif } from '../textos-uif'
import { AprobarYEnviar } from './aprobar-y-enviar'
import { PrepararReporte } from './preparar-reporte'

const T = textosUif.reportes
const COLUMNAS: Columna<ReporteRegulatorioEnLista>[] = [
  { clave: 'periodo', titulo: T.columnas.periodo, mono: true },
  { clave: 'fechaCorte', titulo: T.columnas.fechaCorte },
  { clave: 'fechaLimite', titulo: T.columnas.fechaLimite },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'cantidadRegistros', titulo: T.columnas.cantidadRegistros, numerica: true },
  { clave: 'montoTotal', titulo: T.columnas.montoTotal, numerica: true },
]
const estado = (r: ReporteRegulatorioEnLista) => textosUif.estadosDeReporte[r.estado] ?? r.estado
const registros = (r: ReporteRegulatorioEnLista) => (r.reporteEnCero ? T.enCero : String(r.cantidadRegistros))
const EXPORTABLES: ColumnaExportable<ReporteRegulatorioEnLista>[] = [
  { clave: 'periodo', titulo: T.columnas.periodo },
  { clave: 'fechaCorte', titulo: T.columnas.fechaCorte, valor: (r) => formatearDia(r.fechaCorte) },
  { clave: 'fechaLimite', titulo: T.columnas.fechaLimite, valor: (r) => formatearDia(r.fechaLimite) },
  { clave: 'estado', titulo: T.columnas.estado, valor: estado },
  { clave: 'cantidadRegistros', titulo: T.columnas.cantidadRegistros, valor: registros },
  // Sin moneda: el contrato no la modela y no se le inventa una (regla 91.1.2). El encabezado lo dice.
  { clave: 'montoTotal', titulo: T.sinMoneda },
]

/**
 * CU-43 · la cola de reportes regulatorios (`listarReportesRegulatorios`), paginada por cursor en el servidor.
 * Lectura con CUMPLIMIENTO_REPORTAR o AUDITORIA_LEER; preparar y aprobar solo con CUMPLIMIENTO_REPORTAR. La acción de
 * fila «Aprobar y enviar» aparece solo en un reporte GENERADO; un reporte enviado es constancia ante el organismo y
 * no se edita ni se elimina (el contrato tampoco lo permite). El monto total viaja SIN moneda y se muestra tal cual.
 */
@Component({
  selector: 'ap-bandeja-de-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, AccionesDeFila, Boton, Alerta, Icono, PrepararReporte, AprobarYEnviar],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="documento">
      @if (puedeOperar()) { <ap-boton acciones variante="primario" icono="mas" (pulsado)="preparando.set(true)">{{ t.preparar }}</ap-boton> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <p class="nota"><ap-icono nombre="info" tamano="s4" />{{ t.sinMoneda }}</p>
      <ap-listado-de-bandeja recursoDeExportacion="uif-reportes" [titulo]="t.tabla" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="reportes" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" ayudaDeBusqueda="Busca por período o estado en la página cargada: el servidor no admite búsqueda. El estado filtra en el servidor.">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('fechaCorte') { {{ dia(r.fechaCorte) }} }
            @case ('fechaLimite') { {{ dia(r.fechaLimite) }} }
            @case ('estado') { {{ estado(r) }} }
            @case ('cantidadRegistros') { {{ registros(r) }} }
            @default { {{ r[columna.clave] }} }
          }
        </ng-template>
        @if (puedeOperar()) {
          <ng-template #acciones let-r>
            <ap-acciones-de-fila [fila]="'reporte ' + r.periodo">
              @if (sePuedeEnviar(r)) {
                <button type="button" apAccionDeFila [attr.aria-label]="t.aprobar + ' del período ' + r.periodo" (click)="aprobar(r)"><ap-icono nombre="enviar" tamano="s4" /><span>{{ t.aprobar }}</span></button>
              }
            </ap-acciones-de-fila>
          </ng-template>
        }
      </ap-listado-de-bandeja>
    </ap-marco-de-cumplimiento>
    @if (puedeOperar()) {
      <ap-preparar-reporte [(abierto)]="preparando" (preparo)="alPreparar($event)" />
      <ap-aprobar-y-enviar [(abierto)]="aprobando" [reporte]="elegido()" (envio)="alEnviar($event)" />
    }
  `,
  styles: `.nota { display: flex; align-items: center; gap: var(--s1); margin: 0; color: var(--text-3); font: var(--t-cuerpo-chico); }`,
})
export class BandejaDeReportes {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly estado = estado
  protected readonly registros = registros
  protected readonly bandeja = crearBandeja<ReporteRegulatorioEnLista>({
    cargador: cargadorDeReportes(),
    camposDeBusqueda: ['periodo', estado],
    filtros: [{ nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosUif.estadosDeReporte) }],
  })
  protected readonly identidad = (r: ReporteRegulatorioEnLista) => r.reporteId
  protected readonly dia = formatearDia
  protected readonly sePuedeEnviar = sePuedeEnviar

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
    void this.bandeja.recargar()
  }

  protected alEnviar({ salida, yaExistia }: Recibo<SalidaReporteRegulatorio>): void {
    this.recibo.set(textosUif.envio.recibo(salida.fueraDePlazo, yaExistia, salida.hashArchivo))
    void this.bandeja.recargar()
  }
}
