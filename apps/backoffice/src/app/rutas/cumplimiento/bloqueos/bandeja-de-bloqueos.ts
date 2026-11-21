import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { BloqueoEnLista } from 'clientes/angular/nucleo-financiero/model/bloqueoEnLista'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeBloqueos } from '../dominio/cu17-bloqueos'
import { textosBloqueos } from '../textos-bloqueos'
import { LevantarBloqueo } from './levantar-bloqueo'

const T = textosBloqueos
const COLUMNAS: Columna<BloqueoEnLista>[] = [
  { clave: 'autoridad', titulo: T.columnas.autoridad },
  { clave: 'tipoOrden', titulo: T.columnas.tipoOrden },
  { clave: 'alcance', titulo: T.columnas.alcance },
  { clave: 'montoBloqueado', titulo: T.columnas.montoBloqueado },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'recibidoEn', titulo: T.columnas.recibidoEn },
  { clave: 'venceEn', titulo: T.columnas.venceEn },
]
const autoridad = (b: BloqueoEnLista) => T.autoridades[b.autoridad] ?? b.autoridad
const orden = (b: BloqueoEnLista) => T.ordenes[b.tipoOrden] ?? b.tipoOrden
const alcance = (b: BloqueoEnLista) => T.alcances[b.alcance] ?? b.alcance
const estado = (b: BloqueoEnLista) => T.estados[b.estado] ?? b.estado
const vence = (b: BloqueoEnLista) => (b.venceEn ? formatearFecha(b.venceEn) : T.sinVencimiento)
// El importe viaja como cadena exacta del contrato; se exporta tal cual, con su moneda, sin pasar por número.
const monto = (b: BloqueoEnLista) => (b.montoBloqueado ? `${b.montoBloqueado.monto} ${b.montoBloqueado.moneda}` : T.saldoCompleto)
const TONOS: Record<string, Tono> = { VIGENTE: 'aviso', LEVANTADO: 'ok', VENCIDO: 'neutro' }
const EXPORTABLES: ColumnaExportable<BloqueoEnLista>[] = [
  { clave: 'autoridad', titulo: T.columnas.autoridad, valor: autoridad },
  { clave: 'tipoOrden', titulo: T.columnas.tipoOrden, valor: orden },
  { clave: 'alcance', titulo: T.columnas.alcance, valor: alcance },
  { clave: 'montoBloqueado', titulo: T.columnas.montoBloqueado, valor: monto },
  { clave: 'estado', titulo: T.columnas.estado, valor: estado },
  { clave: 'recibidoEn', titulo: T.columnas.recibidoEn, valor: (b) => formatearFecha(b.recibidoEn) },
  { clave: 'venceEn', titulo: T.columnas.venceEn, valor: vence },
]

/**
 * CU-17 · la bandeja de bloqueos por orden de autoridad (`listarBloqueosPorAutoridad`), el más reciente primero.
 * No viaja la persona: solo la cuenta, la orden y su estado. «Levantar» aparece solo si el servidor dice
 * `puedeLevantar` (vigente y no registrado por quien consulta) y la sesión tiene `BLOQUEO_AUTORIDAD_LEVANTAR`;
 * ocultarlo no protege nada: el servidor vuelve a decidir (AP-CU17-04/05).
 */
@Component({
  selector: 'ap-bandeja-de-bloqueos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, AccionesDeFila, ChipEstado, Alerta, Icono, Monto, LevantarBloqueo],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="candado">
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-listado-de-bandeja recursoDeExportacion="cumplimiento-bloqueos" [titulo]="t.tabla" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="bloqueos" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [ayudaDeBusqueda]="t.ayudaDeBusqueda">
        <ng-template #celda let-b let-columna="columna">
          @switch (columna.clave) {
            @case ('autoridad') { {{ autoridad(b) }} }
            @case ('tipoOrden') { {{ orden(b) }} }
            @case ('alcance') { {{ alcance(b) }} }
            @case ('montoBloqueado') {
              @if (b.montoBloqueado) { <ap-monto [monto]="b.montoBloqueado.monto" [moneda]="b.montoBloqueado.moneda" /> } @else { {{ t.saldoCompleto }} }
            }
            @case ('estado') { <ap-chip-estado [tono]="tono(b)">{{ estado(b) }}</ap-chip-estado> }
            @case ('recibidoEn') { {{ fecha(b.recibidoEn) }} }
            @case ('venceEn') { {{ vence(b) }} }
            @default { {{ b[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-b>
          <ap-acciones-de-fila [fila]="'bloqueo ' + orden(b) + ' de ' + autoridad(b)">
            @if (puedeLevantar() && b.puedeLevantar) {
              <button type="button" apAccionDeFila [attr.aria-label]="t.levantar + ' el bloqueo ' + orden(b) + ' de ' + autoridad(b) + ' recibido el ' + fecha(b.recibidoEn)" (click)="levantar(b)"><ap-icono nombre="candado" tamano="s4" /><span>{{ t.levantar }}</span></button>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado-de-bandeja>
    </ap-marco-de-cumplimiento>
    @if (puedeLevantar()) { <ap-levantar-bloqueo [(abierto)]="dialogoAbierto" [bloqueo]="elegido()" (levanto)="alLevantar()" /> }
  `,
})
export class BandejaDeBloqueos {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly autoridad = autoridad
  protected readonly orden = orden
  protected readonly alcance = alcance
  protected readonly estado = estado
  protected readonly vence = vence
  protected readonly bandeja = crearBandeja<BloqueoEnLista>({
    cargador: cargadorDeBloqueos(),
    camposDeBusqueda: [autoridad, orden, alcance, estado],
    filtros: [{ nombre: 'estado', etiqueta: 'Estado', opciones: opcionesDe(T.estados), textoTodos: T.todos }],
  })
  protected readonly identidad = (b: BloqueoEnLista) => b.bloqueoId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly tono = (b: BloqueoEnLista): Tono => TONOS[b.estado] ?? 'neutro'

  protected readonly puedeLevantar = computed(() => tieneAlguno(this.sesion, ['BLOQUEO_AUTORIDAD_LEVANTAR']))
  protected readonly elegido = signal<BloqueoEnLista | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected levantar(bloqueo: BloqueoEnLista): void {
    this.recibo.set(null)
    this.elegido.set(bloqueo)
    this.dialogoAbierto.set(true)
  }

  protected alLevantar(): void {
    this.recibo.set(T.levantamiento.recibo)
    void this.bandeja.recargar()
  }
}
