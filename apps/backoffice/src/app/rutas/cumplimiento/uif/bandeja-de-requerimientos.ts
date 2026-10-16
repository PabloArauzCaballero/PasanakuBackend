import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipsDeFiltro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { RequerimientoEnLista, SalidaRequerimientoDeAutoridad } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { tieneAlguno } from '../compartido/acceso'
import { filtrosDe, observarBandeja } from '../compartido/bandeja'
import { opcionesDe } from '../compartido/campos'
import { EstadoDeBandejaVista } from '../compartido/estado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import type { Recibo } from '../dominio/cu43-reportes'
import { cargadorDeRequerimientos, sePuedeResponder } from '../dominio/cu45-requerimientos'
import { textosUif } from '../textos-uif'
import { RegistrarRequerimiento } from './registrar-requerimiento'
import { ResponderRequerimiento } from './responder-requerimiento'

const T = textosUif.requerimientos
const COLUMNAS: ColumnaVirtual<RequerimientoEnLista>[] = [
  { clave: 'numeroOficio', titulo: T.columnas.numeroOficio },
  { clave: 'autoridad', titulo: T.columnas.autoridad },
  { clave: 'fechaRecepcion', titulo: T.columnas.fechaRecepcion, ancho: '2' },
  { clave: 'plazoRespuesta', titulo: T.columnas.plazoRespuesta, ancho: '2' },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'conBloqueoDeSaldo', titulo: T.columnas.conBloqueoDeSaldo },
  { clave: 'requerimientoId', titulo: T.columnas.requerimientoId, ancho: '2' },
]

/**
 * CU-45 · la cola de requerimientos de autoridad (`listarRequerimientosDeAutoridad`). Lectura
 * con OFICIAL_CUMPLIMIENTO o AUDITORIA_LEER; registrar y responder solo el rol
 * OFICIAL_CUMPLIMIENTO. La fila no muestra a la persona alcanzada (respuesta mínima).
 */
@Component({
  selector: 'ap-bandeja-de-requerimientos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, EstadoDeBandejaVista, TablaDeDatosVirtualizada, ChipsDeFiltro, Boton, Alerta, RegistrarRequerimiento, ResponderRequerimiento],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito">
      @if (puedeOperar()) { <ap-boton acciones variante="primario" (pulsado)="registrando.set(true)">{{ t.registrar }}</ap-boton> }
      <ap-chips-de-filtro [etiqueta]="t.filtroEstado" [filtros]="FILTROS" [(elegidos)]="estados" />
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-estado-de-bandeja [estado]="bandeja.estado()" [error]="bandeja.error()" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [conFiltros]="estados().length > 0" (reintentar)="bandeja.recargar()" (quitarFiltros)="estados.set([])" />
      @for (m of [bandeja.montaje()]; track m) {
        <div [hidden]="bandeja.estado() === 'denegado'">
          <ap-tabla-de-datos-virtualizada [titulo]="t.tabla" [columnas]="COLUMNAS" [cargador]="bandeja.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtros()" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('autoridad') { {{ autoridades[r.autoridad] ?? r.autoridad }} }
                @case ('fechaRecepcion') { {{ fecha(r.fechaRecepcion) }} }
                @case ('plazoRespuesta') { {{ fecha(r.plazoRespuesta) }} }
                @case ('estado') { {{ estados_[r.estado] ?? r.estado }} }
                @case ('conBloqueoDeSaldo') { {{ r.conBloqueoDeSaldo ? t.conBloqueo : t.sinBloqueo }} }
                @case ('requerimientoId') {
                  @if (puedeOperar() && sePuedeResponder(r)) { <ap-boton tamano="sm" (pulsado)="responder(r)">{{ t.responder }} {{ r.numeroOficio }}</ap-boton> }
                }
                @default { {{ r[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        </div>
      }
    </ap-marco-de-cumplimiento>
    @if (puedeOperar()) {
      <ap-registrar-requerimiento [(abierto)]="registrando" (registro)="alTerminar($event, true)" />
      <ap-responder-requerimiento [(abierto)]="respondiendo" [requerimiento]="elegido()" (respondio)="alTerminar($event, false)" />
    }
  `,
})
export class BandejaDeRequerimientos {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS = opcionesDe(textosUif.estadosDeRequerimiento)
  protected readonly estados_ = textosUif.estadosDeRequerimiento
  protected readonly autoridades = textosUif.autoridades
  protected readonly bandeja = observarBandeja(cargadorDeRequerimientos())
  protected readonly identidad = (r: RequerimientoEnLista) => r.requerimientoId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly sePuedeResponder = sePuedeResponder

  protected readonly estados = signal<string[]>([])
  protected readonly filtros = computed(() => filtrosDe({ estados: this.estados() }))
  /** `registrar` y `responder` exigen el ROL OFICIAL_CUMPLIMIENTO (cumplimiento.yaml), no un permiso. */
  protected readonly puedeOperar = computed(() => tieneAlguno(this.sesion, ['OFICIAL_CUMPLIMIENTO']))
  protected readonly registrando = signal(false)
  protected readonly respondiendo = signal(false)
  protected readonly elegido = signal<RequerimientoEnLista | null>(null)
  protected readonly recibo = signal<string | null>(null)

  protected responder(r: RequerimientoEnLista): void {
    this.elegido.set(r)
    this.respondiendo.set(true)
  }

  protected alTerminar({ yaExistia }: Recibo<SalidaRequerimientoDeAutoridad>, esRegistro: boolean): void {
    this.recibo.set(esRegistro ? textosUif.registro.recibo(yaExistia) : textosUif.respuesta.recibo(yaExistia))
    this.bandeja.recargar()
  }
}
