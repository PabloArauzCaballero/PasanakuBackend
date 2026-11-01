import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ActaEnLista } from 'clientes/angular/cumplimiento'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { cargadorDeActas } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { camposDeCelda, ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.actas
const COLUMNAS: Columna<ActaEnLista>[] = [
  { clave: 'numero', titulo: T.columnas.numero, mono: true },
  { clave: 'fecha', titulo: T.columnas.fecha },
  { clave: 'cumpleQuorum', titulo: T.columnas.cumpleQuorum },
  { clave: 'hashDocumento', titulo: T.columnas.hashDocumento, mono: true },
]
const celda = (a: ActaEnLista, clave: string): Celda => {
  if (clave === 'fecha') return formatearDia(a.fecha)
  if (clave === 'cumpleQuorum') return a.cumpleQuorum ? T.conQuorum : T.sinQuorum
  return String(a[clave as keyof ActaEnLista] ?? '')
}

/**
 * CU-94 · actas de comité (`listarActasDeComite`), solo lectura. El contrato no admite filtros: solo buscador local.
 * Un acta firmada no se edita ni se elimina; sesionar no se ofrece (permiso sin sembrar).
 */
@Component({
  selector: 'ap-pantalla-de-actas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura, Icono],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" icono="libro" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [celda]="celda" nombreDeRegistros="actas" ayudaDeBusqueda="Busca por número, fecha, quórum o huella en la página cargada: el servidor no admite búsqueda ni filtros.">
      <p class="nota"><ap-icono nombre="candado" tamano="s4" />{{ t.sinSesionar }}</p>
    </ap-cola-de-lectura>
  `,
  styles: `.nota { display: flex; align-items: center; gap: var(--s1); margin: 0; color: var(--text-3); font: var(--t-cuerpo-chico); }`,
})
export class PantallaDeActas {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly celda = celda
  protected readonly bandeja = crearBandeja<ActaEnLista>({ cargador: cargadorDeActas(), camposDeBusqueda: camposDeCelda(COLUMNAS, celda) })
  protected readonly identidad = (a: ActaEnLista) => a.actaId
}
