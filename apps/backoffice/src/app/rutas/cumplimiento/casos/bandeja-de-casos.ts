import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ChipsDeFiltro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import type { CasoEnBandeja, SalidaDecisionCasoLft } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { tieneAlguno } from '../compartido/acceso'
import { filtrosDe, observarBandeja } from '../compartido/bandeja'
import { EstadoDeBandejaVista } from '../compartido/estado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeCasos } from '../dominio/cu44-bandejas'
import { textosLft } from '../textos-lft'
import { DecidirCaso } from './decidir-caso'

const T = textosLft.casos
const COLUMNAS: ColumnaVirtual<CasoEnBandeja>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo },
  { clave: 'prioridad', titulo: T.columnas.prioridad },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'plazoLimite', titulo: T.columnas.plazoLimite, ancho: '2' },
  { clave: 'vencido', titulo: T.columnas.vencido },
  { clave: 'decision', titulo: T.columnas.decision, ancho: '2' },
]

/**
 * CU-44 · la bandeja de casos LFT (`listarBandejaDeCasos`): el que vence antes primero, con el
 * plazo GUARDADO al abrir (`vencido` lo calcula el servidor con su hora). No viaja la persona,
 * el resumen ni los hallazgos. «Decidir» abre `DecidirCaso` sobre un caso no cerrado.
 */
@Component({
  selector: 'ap-bandeja-de-casos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, EstadoDeBandejaVista, TablaDeDatosVirtualizada, ChipsDeFiltro, ChipEstado, Boton, Alerta, DecidirCaso],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito">
      <ap-chips-de-filtro [etiqueta]="t.filtroEstado" [filtros]="FILTROS_ESTADO" [(elegidos)]="estados" />
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-estado-de-bandeja [estado]="bandeja.estado()" [error]="bandeja.error()" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [conFiltros]="estados().length > 0" (reintentar)="bandeja.recargar()" (quitarFiltros)="estados.set([])" />
      @for (m of [bandeja.montaje()]; track m) {
        <div [hidden]="bandeja.estado() === 'denegado'">
          <ap-tabla-de-datos-virtualizada [titulo]="t.tabla" [columnas]="COLUMNAS" [cargador]="bandeja.cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtros()" [tamanoDePagina]="25">
            <ng-template #celda let-c let-columna="columna">
              @switch (columna.clave) {
                @case ('prioridad') { {{ prioridades[c.prioridad] ?? c.prioridad }} }
                @case ('estado') { {{ estados_[c.estado] ?? c.estado }} }
                @case ('plazoLimite') { {{ fecha(c.plazoLimite) }} }
                @case ('vencido') { <ap-chip-estado [tono]="c.vencido ? 'error' : 'neutro'">{{ c.vencido ? t.vencido : t.enPlazo }}</ap-chip-estado> }
                @case ('decision') {
                  @if (c.decision) { {{ decisiones[c.decision] ?? c.decision }} }
                  @else if (c.estado !== 'CERRADO' && puedeDecidir()) { <ap-boton tamano="sm" (pulsado)="decidir(c)">{{ t.decidir }} {{ c.codigo }}</ap-boton> }
                }
                @default { {{ c[columna.clave] }} }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        </div>
      }
    </ap-marco-de-cumplimiento>
    @if (puedeDecidir()) { <ap-decidir-caso [(abierto)]="dialogoAbierto" [caso]="elegido()" (decidio)="alDecidir($event)" /> }
  `,
})
export class BandejaDeCasos {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly FILTROS_ESTADO = Object.entries(textosLft.estadosDeCaso).map(([valor, texto]) => ({ valor, texto }))
  protected readonly estados_ = textosLft.estadosDeCaso
  protected readonly prioridades = textosLft.prioridades
  protected readonly decisiones = textosLft.decisiones
  protected readonly bandeja = observarBandeja(cargadorDeCasos())
  protected readonly identidad = (c: CasoEnBandeja) => c.casoId
  protected readonly fecha = (iso: string) => formatearFecha(iso)

  protected readonly estados = signal<string[]>([])
  protected readonly filtros = computed(() => filtrosDe({ estados: this.estados() }))
  protected readonly puedeDecidir = computed(() => tieneAlguno(this.sesion, ['CUMPLIMIENTO_CASOS']))
  protected readonly elegido = signal<CasoEnBandeja | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected decidir(caso: CasoEnBandeja): void {
    this.elegido.set(caso)
    this.dialogoAbierto.set(true)
  }

  protected alDecidir(salida: SalidaDecisionCasoLft): void {
    this.recibo.set(textosLft.decision.recibo(this.decisiones[salida.decision] ?? salida.decision, salida.alertasCerradas, salida.fueraDePlazo))
    this.bandeja.recargar()
  }
}
