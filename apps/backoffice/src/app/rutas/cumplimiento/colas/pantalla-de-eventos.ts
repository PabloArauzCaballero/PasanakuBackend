import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { EventoDeRiesgoEnLista } from 'clientes/angular/cumplimiento'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { opcionesDe } from '../compartido/campos'
import { cargadorDeEventosDeRiesgo } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.eventos
const COLUMNAS: ColumnaVirtual<EventoDeRiesgoEnLista>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo },
  { clave: 'categoria', titulo: T.columnas.categoria, ancho: '2' },
  { clave: 'fechaDeteccion', titulo: T.columnas.fechaDeteccion, ancho: '2' },
  { clave: 'perdidaBruta', titulo: T.columnas.perdidaBruta, numerica: true },
  { clave: 'perdidaNeta', titulo: T.columnas.perdidaNeta, numerica: true },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'reportadoCentral', titulo: T.columnas.reportadoCentral },
]

/**
 * CU-54 · eventos de riesgo operativo (`listarEventosDeRiesgo`), solo lectura. Las pérdidas son
 * `Dinero` (cadena + moneda) y se muestran con `ap-monto`; la neta es la que calculó la base.
 */
@Component({
  selector: 'ap-pantalla-de-eventos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [cargador]="cargador" [identidad]="identidad" [celda]="celda" [filtrosDeEstado]="FILTROS" [etiquetaFiltro]="filtro" />
  `,
})
export class PantallaDeEventos {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS = opcionesDe(textosColas.estadosDeEvento)
  protected readonly filtro = textosColas.filtroEstado
  protected readonly cargador = cargadorDeEventosDeRiesgo()
  protected readonly identidad = (e: EventoDeRiesgoEnLista) => e.eventoId
  protected readonly celda = (e: EventoDeRiesgoEnLista, clave: string): Celda => {
    switch (clave) {
      case 'fechaDeteccion': return formatearFecha(e.fechaDeteccion)
      case 'perdidaBruta': return { monto: e.perdidaBruta.monto, moneda: e.perdidaBruta.moneda }
      case 'perdidaNeta': return { monto: e.perdidaNeta.monto, moneda: e.perdidaNeta.moneda }
      case 'estado': return textosColas.estadosDeEvento[e.estado] ?? e.estado
      case 'reportadoCentral': return e.reportadoCentral ? T.reportado : T.sinReportar
      default: return String(e[clave as keyof EventoDeRiesgoEnLista] ?? '')
    }
  }
}
