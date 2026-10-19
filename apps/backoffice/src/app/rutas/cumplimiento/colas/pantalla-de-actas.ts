import { ChangeDetectionStrategy, Component } from '@angular/core'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import type { ActaEnLista } from 'clientes/angular/cumplimiento'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeActas } from '../dominio/colas-de-lectura'
import { textosColas } from '../textos-colas'
import { ColaDeLectura, type Celda } from './cola-de-lectura'

const T = textosColas.actas
const COLUMNAS: ColumnaVirtual<ActaEnLista>[] = [
  { clave: 'numero', titulo: T.columnas.numero },
  { clave: 'fecha', titulo: T.columnas.fecha },
  { clave: 'cumpleQuorum', titulo: T.columnas.cumpleQuorum },
  { clave: 'hashDocumento', titulo: T.columnas.hashDocumento, ancho: '3' },
]

/** CU-94 · actas de comité (`listarActasDeComite`), solo lectura. Sesionar no se ofrece (permiso sin sembrar). */
@Component({
  selector: 'ap-pantalla-de-actas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ColaDeLectura],
  template: `
    <ap-cola-de-lectura [titulo]="t.titulo" [proposito]="t.proposito" [tabla]="t.tabla" [vacio]="t.vacio" [columnas]="COLUMNAS" [cargador]="cargador" [identidad]="identidad" [celda]="celda">
      <p class="nota">{{ t.sinSesionar }}</p>
    </ap-cola-de-lectura>
  `,
  styles: `.nota { margin: 0; color: var(--text-3); }`,
})
export class PantallaDeActas {
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDeActas()
  protected readonly identidad = (a: ActaEnLista) => a.actaId
  protected readonly celda = (a: ActaEnLista, clave: string): Celda => {
    if (clave === 'fecha') return formatearDia(a.fecha)
    if (clave === 'cumpleQuorum') return a.cumpleQuorum ? T.conQuorum : T.sinQuorum
    return String(a[clave as keyof ActaEnLista] ?? '')
  }
}
