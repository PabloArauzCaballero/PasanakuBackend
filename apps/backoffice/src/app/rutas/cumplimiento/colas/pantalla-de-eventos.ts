import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { EventoDeRiesgoEnLista } from 'clientes/angular/cumplimiento'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { cargadorDeEventosDeRiesgo } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { camposDeCelda, ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.eventos
const COLUMNAS: Columna<EventoDeRiesgoEnLista>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo, mono: true },
  { clave: 'categoria', titulo: T.columnas.categoria },
  { clave: 'fechaDeteccion', titulo: T.columnas.fechaDeteccion },
  { clave: 'perdidaBruta', titulo: T.columnas.perdidaBruta, numerica: true },
  { clave: 'perdidaNeta', titulo: T.columnas.perdidaNeta, numerica: true },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'reportadoCentral', titulo: T.columnas.reportadoCentral },
]
const celda = (e: EventoDeRiesgoEnLista, clave: string): Celda => {
  switch (clave) {
    case 'fechaDeteccion': return formatearFecha(e.fechaDeteccion)
    case 'perdidaBruta': return { monto: e.perdidaBruta.monto, moneda: e.perdidaBruta.moneda }
    case 'perdidaNeta': return { monto: e.perdidaNeta.monto, moneda: e.perdidaNeta.moneda }
    case 'estado': return textosColas.estadosDeEvento[e.estado] ?? e.estado
    case 'reportadoCentral': return e.reportadoCentral ? T.reportado : T.sinReportar
    default: return String(e[clave as keyof EventoDeRiesgoEnLista] ?? '')
  }
}

/**
 * CU-54 · eventos de riesgo operativo (`listarEventosDeRiesgo`), solo lectura. Las pérdidas son `Dinero` (cadena +
 * moneda), se muestran con `ap-monto` y salen al archivo con su moneda; la neta es la que calculó la base.
 */
@Component({
  selector: 'ap-pantalla-de-eventos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" icono="advertencia" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [celda]="celda" nombreDeRegistros="eventos" ayudaDeBusqueda="Busca por código, categoría, estado o importe en la página cargada: el servidor no admite búsqueda. El estado filtra en el servidor." />
  `,
})
export class PantallaDeEventos {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly celda = celda
  protected readonly bandeja = crearBandeja<EventoDeRiesgoEnLista>({
    cargador: cargadorDeEventosDeRiesgo(),
    camposDeBusqueda: camposDeCelda(COLUMNAS, celda),
    filtros: [{ nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosColas.estadosDeEvento) }],
  })
  protected readonly identidad = (e: EventoDeRiesgoEnLista) => e.eventoId
}
