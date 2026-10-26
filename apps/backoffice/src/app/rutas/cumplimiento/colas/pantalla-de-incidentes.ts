import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { IncidenteEnLista } from 'clientes/angular/cumplimiento'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { cargadorDeIncidentes } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { camposDeCelda, ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.incidentes
const COLUMNAS: Columna<IncidenteEnLista>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo, mono: true },
  { clave: 'tipo', titulo: T.columnas.tipo },
  { clave: 'severidad', titulo: T.columnas.severidad },
  { clave: 'detectadoEn', titulo: T.columnas.detectadoEn },
  { clave: 'plazoReporte', titulo: T.columnas.plazoReporte },
  { clave: 'reportadoAlOrganismoEn', titulo: T.columnas.reportadoAlOrganismoEn },
  { clave: 'estado', titulo: T.columnas.estado },
]
const celda = (i: IncidenteEnLista, clave: string): Celda => {
  switch (clave) {
    case 'severidad': return textosColas.severidades[i.severidad] ?? i.severidad
    case 'detectadoEn': return formatearFecha(i.detectadoEn)
    case 'plazoReporte': return formatearFecha(i.plazoReporte)
    case 'reportadoAlOrganismoEn': return i.reportadoAlOrganismoEn ? formatearFecha(i.reportadoAlOrganismoEn) : T.sinReportar
    case 'estado': return textosColas.estadosDeIncidente[i.estado] ?? i.estado
    default: return String(i[clave as keyof IncidenteEnLista] ?? '')
  }
}

/** CU-55 · incidentes de seguridad (`listarIncidentesDeSeguridad`), solo lectura, con sus plazos guardados. */
@Component({
  selector: 'ap-pantalla-de-incidentes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" icono="escudo" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [celda]="celda" nombreDeRegistros="incidentes" ayudaDeBusqueda="Busca por código, tipo, severidad o estado en la página cargada: el servidor no admite búsqueda. El estado filtra en el servidor." />
  `,
})
export class PantallaDeIncidentes {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly celda = celda
  protected readonly bandeja = crearBandeja<IncidenteEnLista>({
    cargador: cargadorDeIncidentes(),
    camposDeBusqueda: camposDeCelda(COLUMNAS, celda),
    filtros: [{ nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosColas.estadosDeIncidente) }],
  })
  protected readonly identidad = (i: IncidenteEnLista) => i.incidenteId
}
