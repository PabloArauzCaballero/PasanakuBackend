import { ChangeDetectionStrategy, Component } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../../contabilidad/listado/listado-por-cursor'
import { TablaPorCursor } from '../../contabilidad/listado/tabla-por-cursor'
import { cargadorDeEnvios, type EnvioEnLista } from '../dominio/cu80-cu83-mensajeria'
import { NO_DISPONIBLE } from '../dominio/listado'
import { textosConfiguracion } from '../textos'

const t = textosConfiguracion.mensajeria
const ESTADOS: Record<string, string> = t.estadosEnvio
const estado = (r: EnvioEnLista) => ESTADOS[r.estado] ?? r.estado
/**
 * El costo del proveedor viaja con **cuatro** decimales (`0.0125`): `ap-monto` solo acepta el esquema `Dinero` de dos
 * (`^-?\d+\.\d{2}$`) y lanzaría. Se muestra la cadena exacta del contrato con su moneda, sin redondear ni convertir.
 */
const costo = (r: EnvioEnLista) => `${r.costo} ${r.moneda}`

/** Sin destinatario, contenido ni identificador del proveedor: el contrato no los trae y esta tabla no los pide. */
const COLUMNAS: Columna<EnvioEnLista>[] = [
  { clave: 'encoladoEn', titulo: 'Encolado' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'proveedor', titulo: 'Proveedor' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'intentos', titulo: 'Intentos', numerica: true },
  { clave: 'costo', titulo: 'Costo', numerica: true },
  { clave: 'codigoError', titulo: 'Código de error', mono: true },
  { clave: 'entregadoEn', titulo: 'Entregado' },
]

const EXPORTABLES = [
  { clave: 'encoladoEn', titulo: 'Encolado' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'proveedor', titulo: 'Proveedor' },
  { clave: 'estado', titulo: 'Estado', valor: estado },
  { clave: 'intentos', titulo: 'Intentos', valor: (r: EnvioEnLista) => `${r.intentos} / ${r.maxIntentos}` },
  { clave: 'costo', titulo: 'Costo', valor: costo },
  { clave: 'codigoError', titulo: 'Código de error', valor: (r: EnvioEnLista) => r.codigoError ?? NO_DISPONIBLE },
  { clave: 'entregadoEn', titulo: 'Entregado', valor: (r: EnvioEnLista) => r.entregadoEn ?? NO_DISPONIBLE },
]

/**
 * CU-83 · Auditoría de envíos, de solo lectura (el contrato no tiene operación sobre un envío). El estado viaja como
 * `estados` y arranca con el `?filtro=` de la URL; el buscador, el canal y el proveedor filtran la página cargada.
 */
@Component({
  selector: 'ap-tabla-de-envios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Fecha, TablaPorCursor],
  template: `
    <ap-tabla-por-cursor [titulo]="t.envios.titulo" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="envíos" [textoVacio]="t.envios.vacio" ayudaDeBusqueda="Busca por proveedor, canal o código de error en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-r let-columna="columna">
        @switch (columna.clave) {
          @case ('estado') { <ap-chip-estado [tono]="r.estado === 'FALLIDO' || r.estado === 'RECHAZADO' || r.estado === 'EXPIRADO' ? 'error' : r.estado === 'ENTREGADO' || r.estado === 'LEIDO' ? 'ok' : 'neutro'">{{ estado(r) }}</ap-chip-estado> }
          @case ('intentos') { {{ r.intentos }} / {{ r.maxIntentos }} }
          @case ('costo') { <span class="cifra">{{ costo(r) }}</span> }
          @case ('encoladoEn') { <ap-fecha [iso]="r.encoladoEn" /> }
          @case ('entregadoEn') { @if (r.entregadoEn) { <ap-fecha [iso]="r.entregadoEn" /> } @else { {{ noDisponible }} } }
          @default { {{ r[columna.clave] ?? noDisponible }} }
        }
      </ng-template>
    </ap-tabla-por-cursor>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .cifra { font-variant-numeric: tabular-nums; white-space: nowrap; }
  `,
})
export class TablaDeEnvios {
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly estado = estado
  protected readonly costo = costo
  protected readonly identidad = (r: EnvioEnLista) => r.envioId
  protected readonly listado = crearListadoPorCursor<EnvioEnLista>({
    cargador: cargadorDeEnvios(),
    camposDeBusqueda: ['proveedor', 'canal', 'codigoError'],
    servidor: { nombre: 'estado', etiqueta: textosConfiguracion.comun.estado, opciones: opcionesDe(ESTADOS), inicial: filtroInicialDeLaUrl(opcionesDe(ESTADOS)) },
    filtros: [
      { nombre: 'canal', etiqueta: 'Canal', valor: (r) => r.canal },
      { nombre: 'proveedor', etiqueta: 'Proveedor', valor: (r) => r.proveedor },
    ],
  })
}
