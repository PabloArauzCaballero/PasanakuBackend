import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { IncidenteEnLista } from 'clientes/angular/cumplimiento'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { opcionesDe } from '../compartido/campos'
import { cargadorDeIncidentes } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.incidentes
const COLUMNAS: ColumnaVirtual<IncidenteEnLista>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo },
  { clave: 'tipo', titulo: T.columnas.tipo },
  { clave: 'severidad', titulo: T.columnas.severidad },
  { clave: 'detectadoEn', titulo: T.columnas.detectadoEn, ancho: '2' },
  { clave: 'plazoReporte', titulo: T.columnas.plazoReporte, ancho: '2' },
  { clave: 'reportadoAlOrganismoEn', titulo: T.columnas.reportadoAlOrganismoEn, ancho: '2' },
  { clave: 'estado', titulo: T.columnas.estado },
]

/** CU-55 · incidentes de seguridad (`listarIncidentesDeSeguridad`), solo lectura, con sus plazos guardados. */
@Component({
  selector: 'ap-pantalla-de-incidentes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [cargador]="cargador" [identidad]="identidad" [celda]="celda" [filtrosDeEstado]="FILTROS" [etiquetaFiltro]="filtro" />
  `,
})
export class PantallaDeIncidentes {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS = opcionesDe(textosColas.estadosDeIncidente)
  protected readonly filtro = textosColas.filtroEstado
  protected readonly cargador = cargadorDeIncidentes()
  protected readonly identidad = (i: IncidenteEnLista) => i.incidenteId
  protected readonly celda = (i: IncidenteEnLista, clave: string): Celda => {
    switch (clave) {
      case 'severidad': return textosColas.severidades[i.severidad] ?? i.severidad
      case 'detectadoEn': return formatearFecha(i.detectadoEn)
      case 'plazoReporte': return formatearFecha(i.plazoReporte)
      case 'reportadoAlOrganismoEn': return i.reportadoAlOrganismoEn ? formatearFecha(i.reportadoAlOrganismoEn) : T.sinReportar
      case 'estado': return textosColas.estadosDeIncidente[i.estado] ?? i.estado
      default: return String(i[clave as keyof IncidenteEnLista] ?? '')
    }
  }
}
