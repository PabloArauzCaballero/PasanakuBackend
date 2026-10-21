import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ChipsDeFiltro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { Tono } from '@aportaya/ui/tono/tono'
import type { AlertaEnBandeja, SalidaCasoLft } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { tieneAlguno } from '../compartido/acceso'
import { filtrosDe, observarBandeja } from '../compartido/bandeja'
import { EstadoDeBandejaVista } from '../compartido/estado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeAlertas } from '../dominio/cu44-bandejas'
import { MAX_ALERTAS_POR_CASO } from '../dominio/cu44-validacion'
import { textosLft } from '../textos-lft'
import { AbrirCaso } from './abrir-caso'

const T = textosLft.alertas
const COLUMNAS: ColumnaVirtual<AlertaEnBandeja>[] = [
  { clave: 'reglaCodigo', titulo: T.columnas.reglaCodigo },
  { clave: 'tipologia', titulo: T.columnas.tipologia, ancho: '2' },
  { clave: 'severidad', titulo: T.columnas.severidad },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'detectadaEn', titulo: T.columnas.detectadaEn, ancho: '2' },
  { clave: 'casoId', titulo: T.columnas.casoId },
]
const TONO: Record<string, Tono> = { CRITICA: 'error', ALTA: 'aviso', MEDIA: 'info', BAJA: 'neutro' }
const aFiltros = (d: Record<string, string>) => Object.entries(d).map(([valor, texto]) => ({ valor, texto }))

/**
 * CU-44 · la bandeja de alertas LFT (`listarBandejaDeAlertas`), por cursor, con filtros de
 * estado y severidad. Deber de reserva: la fila no trae persona, monto ni detalle de la regla.
 * Desde acá se agrupan alertas en un caso (`abrirCasoLft`), solo con CUMPLIMIENTO_CASOS.
 */
@Component({
  selector: 'ap-bandeja-de-alertas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, EstadoDeBandejaVista, TablaDeDatosVirtualizada, ChipsDeFiltro, ChipEstado, Boton, Alerta, AbrirCaso],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito">
      @if (puedeAbrir()) {
        <ap-boton acciones variante="primario" [deshabilitado]="!seleccionValida()" (pulsado)="dialogoAbierto.set(true)">{{ t.abrirCaso(elegidas().length) }}</ap-boton>
      }
      <div class="filtros">
        <ap-chips-de-filtro [etiqueta]="t.filtroEstado" [filtros]="FILTROS_ESTADO" [(elegidos)]="estados" />
        <ap-chips-de-filtro [etiqueta]="t.filtroSeveridad" [filtros]="FILTROS_SEVERIDAD" [(elegidos)]="severidades" />
      </div>
      @if (puedeAbrir()) { <p class="ayuda">{{ t.elegirAlertas }}</p> }
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-estado-de-bandeja [estado]="bandeja.estado()" [error]="bandeja.error()" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [conFiltros]="conFiltros()" (reintentar)="bandeja.recargar()" (quitarFiltros)="quitarFiltros()" />
      @for (m of [bandeja.montaje()]; track m) {
        <div [hidden]="bandeja.estado() === 'denegado' || bandeja.estado() === 'vacio'">
          <ap-tabla-de-datos-virtualizada [titulo]="t.tabla" [columnas]="COLUMNAS" [cargador]="bandeja.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtros()" [seleccionable]="puedeAbrir()" [tamanoDePagina]="25" [(elegidas)]="elegidas">
            <ng-template #celda let-a let-columna="columna">
              @switch (columna.clave) {
                @case ('severidad') { <ap-chip-estado [tono]="tono(a.severidad)">{{ severidades_[a.severidad] ?? a.severidad }}</ap-chip-estado> }
                @case ('estado') { {{ estados_[a.estado] ?? a.estado }} }
                @case ('detectadaEn') { {{ fecha(a.detectadaEn) }} }
                @case ('casoId') { {{ a.casoId ? t.conCaso : t.sinCaso }} }
                @default { {{ a[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        </div>
      }
    </ap-marco-de-cumplimiento>
    @if (puedeAbrir()) { <ap-abrir-caso [(abierto)]="dialogoAbierto" [alertaIds]="elegidas()" (abrio)="alAbrir($event)" /> }
  `,
  styles: `
    .filtros { display: grid; gap: var(--s2); }
    .ayuda { margin: 0; color: var(--text-3); }
  `,
})
export class BandejaDeAlertas {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS_ESTADO = aFiltros(textosLft.estadosDeAlerta)
  protected readonly FILTROS_SEVERIDAD = aFiltros(textosLft.severidades)
  protected readonly estados_ = textosLft.estadosDeAlerta
  protected readonly severidades_ = textosLft.severidades
  protected readonly bandeja = observarBandeja(cargadorDeAlertas())
  protected readonly identidad = (a: AlertaEnBandeja) => a.alertaId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly tono = (s: string): Tono => TONO[s] ?? 'neutro'

  protected readonly estados = signal<string[]>([])
  protected readonly severidades = signal<string[]>([])
  protected readonly filtros = computed(() => filtrosDe({ estados: this.estados(), severidades: this.severidades() }))
  protected readonly conFiltros = computed(() => Object.keys(this.filtros()).length > 0)
  protected readonly elegidas = signal<string[]>([])
  protected readonly seleccionValida = computed(() => this.elegidas().length >= 1 && this.elegidas().length <= MAX_ALERTAS_POR_CASO)
  protected readonly puedeAbrir = computed(() => tieneAlguno(this.sesion, ['CUMPLIMIENTO_CASOS']))
  protected readonly dialogoAbierto = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected quitarFiltros(): void {
    this.estados.set([])
    this.severidades.set([])
  }

  protected alAbrir(salida: SalidaCasoLft): void {
    this.recibo.set(textosLft.apertura.recibo(formatearFecha(salida.plazoLimite)))
    this.elegidas.set([])
    this.bandeja.recargar()
  }
}
