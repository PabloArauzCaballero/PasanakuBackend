import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { RequerimientoEnLista, SalidaRequerimientoDeAutoridad } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import type { Recibo } from '../dominio/cu43-reportes'
import { cargadorDeRequerimientos, sePuedeResponder } from '../dominio/cu45-requerimientos'
import { textosUif } from '../textos-uif'
import { RegistrarRequerimiento } from './registrar-requerimiento'
import { ResponderRequerimiento } from './responder-requerimiento'

const T = textosUif.requerimientos
const COLUMNAS: Columna<RequerimientoEnLista>[] = [
  { clave: 'numeroOficio', titulo: T.columnas.numeroOficio, mono: true },
  { clave: 'autoridad', titulo: T.columnas.autoridad },
  { clave: 'fechaRecepcion', titulo: T.columnas.fechaRecepcion },
  { clave: 'plazoRespuesta', titulo: T.columnas.plazoRespuesta },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'conBloqueoDeSaldo', titulo: T.columnas.conBloqueoDeSaldo },
]
const autoridad = (r: RequerimientoEnLista) => textosUif.autoridades[r.autoridad] ?? r.autoridad
const estado = (r: RequerimientoEnLista) => textosUif.estadosDeRequerimiento[r.estado] ?? r.estado
const bloqueo = (r: RequerimientoEnLista) => (r.conBloqueoDeSaldo ? T.conBloqueo : T.sinBloqueo)
const EXPORTABLES: ColumnaExportable<RequerimientoEnLista>[] = [
  { clave: 'numeroOficio', titulo: T.columnas.numeroOficio },
  { clave: 'autoridad', titulo: T.columnas.autoridad, valor: autoridad },
  { clave: 'fechaRecepcion', titulo: T.columnas.fechaRecepcion, valor: (r) => formatearFecha(r.fechaRecepcion) },
  { clave: 'plazoRespuesta', titulo: T.columnas.plazoRespuesta, valor: (r) => formatearFecha(r.plazoRespuesta) },
  { clave: 'estado', titulo: T.columnas.estado, valor: estado },
  { clave: 'conBloqueoDeSaldo', titulo: T.columnas.conBloqueoDeSaldo, valor: bloqueo },
]

/**
 * CU-45 · la cola de requerimientos de autoridad (`listarRequerimientosDeAutoridad`), paginada por cursor en el
 * servidor. Lectura con OFICIAL_CUMPLIMIENTO o AUDITORIA_LEER; registrar y responder solo el rol OFICIAL_CUMPLIMIENTO.
 * La fila no muestra a la persona alcanzada (respuesta mínima). Acción de fila: «Responder» mientras no esté respondido
 * ni archivado. Un oficio registrado es expediente: no se edita ni se elimina.
 */
@Component({
  selector: 'ap-bandeja-de-requerimientos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, AccionesDeFila, Boton, ChipEstado, Alerta, Icono, RegistrarRequerimiento, ResponderRequerimiento],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="balanza">
      @if (puedeOperar()) { <ap-boton acciones variante="primario" icono="mas" (pulsado)="registrando.set(true)">{{ t.registrar }}</ap-boton> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-listado-de-bandeja [titulo]="t.tabla" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="requerimientos" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" ayudaDeBusqueda="Busca por oficio, autoridad o estado en la página cargada: el servidor no admite búsqueda. El estado filtra en el servidor.">
        <ng-template #celda let-r let-columna="columna">
          @switch (columna.clave) {
            @case ('autoridad') { {{ autoridad(r) }} }
            @case ('fechaRecepcion') { {{ fecha(r.fechaRecepcion) }} }
            @case ('plazoRespuesta') { {{ fecha(r.plazoRespuesta) }} }
            @case ('estado') { {{ estado(r) }} }
            @case ('conBloqueoDeSaldo') { <ap-chip-estado [tono]="r.conBloqueoDeSaldo ? 'aviso' : 'neutro'">{{ bloqueo(r) }}</ap-chip-estado> }
            @default { {{ r[columna.clave] }} }
          }
        </ng-template>
        @if (puedeOperar()) {
          <ng-template #acciones let-r>
            <ap-acciones-de-fila [fila]="'oficio ' + r.numeroOficio">
              @if (sePuedeResponder(r)) {
                <button type="button" apAccionDeFila [attr.aria-label]="t.responder + ' el oficio ' + r.numeroOficio" (click)="responder(r)"><ap-icono nombre="mensaje" tamano="s4" /><span>{{ t.responder }}</span></button>
              }
            </ap-acciones-de-fila>
          </ng-template>
        }
      </ap-listado-de-bandeja>
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
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly autoridad = autoridad
  protected readonly estado = estado
  protected readonly bloqueo = bloqueo
  protected readonly bandeja = crearBandeja<RequerimientoEnLista>({
    cargador: cargadorDeRequerimientos(),
    camposDeBusqueda: ['numeroOficio', autoridad, estado],
    filtros: [{ nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosUif.estadosDeRequerimiento) }],
  })
  protected readonly identidad = (r: RequerimientoEnLista) => r.requerimientoId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly sePuedeResponder = sePuedeResponder

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
    void this.bandeja.recargar()
  }
}
