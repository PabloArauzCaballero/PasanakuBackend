import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { AlertaEnBandeja, SalidaCasoLft } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeAlertas } from '../dominio/cu44-bandejas'
import { MAX_ALERTAS_POR_CASO } from '../dominio/cu44-validacion'
import { textosLft } from '../textos-lft'
import { AbrirCaso } from './abrir-caso'

const T = textosLft.alertas
const COLUMNAS: Columna<AlertaEnBandeja>[] = [
  { clave: 'reglaCodigo', titulo: T.columnas.reglaCodigo, mono: true },
  { clave: 'tipologia', titulo: T.columnas.tipologia },
  { clave: 'severidad', titulo: T.columnas.severidad },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'detectadaEn', titulo: T.columnas.detectadaEn },
  { clave: 'casoId', titulo: T.columnas.casoId },
]
const TONO: Record<string, Tono> = { CRITICA: 'error', ALTA: 'aviso', MEDIA: 'info', BAJA: 'neutro' }
const severidad = (a: AlertaEnBandeja) => textosLft.severidades[a.severidad] ?? a.severidad
const estado = (a: AlertaEnBandeja) => textosLft.estadosDeAlerta[a.estado] ?? a.estado
const caso = (a: AlertaEnBandeja) => (a.casoId ? T.conCaso : T.sinCaso)
/** El archivo lleva lo que la fila muestra; la fila no trae persona ni monto (deber de reserva). */
const EXPORTABLES: ColumnaExportable<AlertaEnBandeja>[] = [
  { clave: 'reglaCodigo', titulo: T.columnas.reglaCodigo },
  { clave: 'tipologia', titulo: T.columnas.tipologia },
  { clave: 'severidad', titulo: T.columnas.severidad, valor: severidad },
  { clave: 'estado', titulo: T.columnas.estado, valor: estado },
  { clave: 'detectadaEn', titulo: T.columnas.detectadaEn, valor: (a) => formatearFecha(a.detectadaEn) },
  { clave: 'caso', titulo: T.columnas.casoId, valor: caso },
]

/**
 * CU-44 · la bandeja de alertas LFT (`listarBandejaDeAlertas`), paginada por cursor en el servidor, con filtros de
 * estado y severidad que viajan en la consulta. Deber de reserva: la fila no trae persona, monto ni detalle de la
 * regla. Con CUMPLIMIENTO_CASOS se agrupan alertas en un caso (`abrirCasoLft`): marcando varias, o desde la acción
 * de fila «Abrir caso» de una alerta suelta. Una alerta es expediente: no se edita ni se elimina.
 */
@Component({
  selector: 'ap-bandeja-de-alertas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, AccionesDeFila, ChipEstado, Boton, Alerta, Icono, AbrirCaso],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="alerta">
      @if (puedeAbrir()) {
        <ap-boton acciones variante="primario" icono="carpeta" [deshabilitado]="!seleccionValida()" (pulsado)="abrirCon(elegidas())">{{ t.abrirCaso(elegidas().length) }}</ap-boton>
      }
      @if (puedeAbrir()) { <p class="ayuda"><ap-icono nombre="info" tamano="s4" />{{ t.elegirAlertas }}</p> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-listado-de-bandeja recursoDeExportacion="cumplimiento-alertas" [titulo]="t.tabla" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="alertas" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" ayudaDeBusqueda="Busca por regla, tipología, severidad o estado en la página cargada: el servidor no admite búsqueda. Estado y severidad filtran en el servidor." [seleccionable]="puedeAbrir()" [(elegidas)]="elegidas">
        <ng-template #celda let-a let-columna="columna">
          @switch (columna.clave) {
            @case ('severidad') { <ap-chip-estado [tono]="tono(a.severidad)">{{ severidad(a) }}</ap-chip-estado> }
            @case ('estado') { {{ estado(a) }} }
            @case ('detectadaEn') { {{ fecha(a.detectadaEn) }} }
            @case ('casoId') { {{ caso(a) }} }
            @default { {{ a[columna.clave] }} }
          }
        </ng-template>
        @if (puedeAbrir()) {
          <ng-template #acciones let-a>
            <ap-acciones-de-fila [fila]="a.reglaCodigo">
              @if (!a.casoId) { <button type="button" apAccionDeFila [attr.aria-label]="'Abrir caso con la alerta ' + a.reglaCodigo" (click)="abrirCon([a.alertaId])"><ap-icono nombre="carpeta" tamano="s4" /><span>Abrir caso</span></button> } @else { <span class="sin-accion">Ya está en un caso</span> }
            </ap-acciones-de-fila>
          </ng-template>
        }
      </ap-listado-de-bandeja>
    </ap-marco-de-cumplimiento>
    @if (puedeAbrir()) { <ap-abrir-caso [(abierto)]="dialogoAbierto" [alertaIds]="paraAbrir()" (abrio)="alAbrir($event)" /> }
  `,
  styles: `.sin-accion { color: var(--text-3); font: var(--t-cuerpo-chico); white-space: nowrap; } .ayuda { display: flex; align-items: center; gap: var(--s1); margin: 0; color: var(--text-3); font: var(--t-cuerpo-chico); }`,
})
export class BandejaDeAlertas {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly severidad = severidad
  protected readonly estado = estado
  protected readonly caso = caso
  protected readonly bandeja = crearBandeja<AlertaEnBandeja>({
    cargador: cargadorDeAlertas(),
    camposDeBusqueda: ['reglaCodigo', 'tipologia', severidad, estado],
    filtros: [
      { nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosLft.estadosDeAlerta), textoTodos: 'Abiertas y en análisis' },
      { nombre: 'severidades', etiqueta: 'Severidad', opciones: opcionesDe(textosLft.severidades), textoTodos: 'Todas' },
    ],
  })
  protected readonly identidad = (a: AlertaEnBandeja) => a.alertaId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly tono = (s: string): Tono => TONO[s] ?? 'neutro'

  protected readonly elegidas = signal<string[]>([])
  /** Las alertas que va a agrupar el diálogo: las marcadas, o la de la fila desde la que se pidió. */
  protected readonly paraAbrir = signal<string[]>([])
  protected readonly seleccionValida = computed(() => this.elegidas().length >= 1 && this.elegidas().length <= MAX_ALERTAS_POR_CASO)
  protected readonly puedeAbrir = computed(() => tieneAlguno(this.sesion, ['CUMPLIMIENTO_CASOS']))
  protected readonly dialogoAbierto = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected abrirCon(ids: string[]): void {
    this.paraAbrir.set([...ids])
    this.dialogoAbierto.set(true)
  }

  protected alAbrir(salida: SalidaCasoLft): void {
    this.recibo.set(textosLft.apertura.recibo(formatearFecha(salida.plazoLimite)))
    this.elegidas.set([])
    void this.bandeja.recargar()
  }
}
