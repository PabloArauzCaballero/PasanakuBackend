import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { CasoEnBandeja, SalidaDecisionCasoLft } from 'clientes/angular/cumplimiento'
import { Sesion } from '../../../nucleo/sesion'
import { tieneAlguno } from '../compartido/acceso'
import { crearBandeja } from '../compartido/bandeja-por-cursor'
import { opcionesDe } from '../compartido/campos'
import { ListadoDeBandeja } from '../compartido/listado-de-bandeja'
import { MarcoDeCumplimiento } from '../compartido/marco'
import { cargadorDeCasos } from '../dominio/cu44-bandejas'
import { textosLft } from '../textos-lft'
import { DecidirCaso } from './decidir-caso'

const T = textosLft.casos
const COLUMNAS: Columna<CasoEnBandeja>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo, mono: true },
  { clave: 'prioridad', titulo: T.columnas.prioridad },
  { clave: 'estado', titulo: T.columnas.estado },
  { clave: 'plazoLimite', titulo: T.columnas.plazoLimite },
  { clave: 'vencido', titulo: T.columnas.vencido },
  { clave: 'decision', titulo: T.columnas.decision },
]
const prioridad = (c: CasoEnBandeja) => textosLft.prioridades[c.prioridad] ?? c.prioridad
const estado = (c: CasoEnBandeja) => textosLft.estadosDeCaso[c.estado] ?? c.estado
const plazo = (c: CasoEnBandeja) => (c.vencido ? T.vencido : T.enPlazo)
const decision = (c: CasoEnBandeja) => (c.decision ? (textosLft.decisiones[c.decision] ?? c.decision) : 'Sin decidir')
const EXPORTABLES: ColumnaExportable<CasoEnBandeja>[] = [
  { clave: 'codigo', titulo: T.columnas.codigo },
  { clave: 'prioridad', titulo: T.columnas.prioridad, valor: prioridad },
  { clave: 'estado', titulo: T.columnas.estado, valor: estado },
  { clave: 'plazoLimite', titulo: T.columnas.plazoLimite, valor: (c) => formatearFecha(c.plazoLimite) },
  { clave: 'vencido', titulo: T.columnas.vencido, valor: plazo },
  { clave: 'decision', titulo: T.columnas.decision, valor: decision },
]

/**
 * CU-44 · la bandeja de casos LFT (`listarBandejaDeCasos`): el que vence antes primero, con el plazo GUARDADO al
 * abrir (`vencido` lo calcula el servidor con su hora). No viaja la persona, el resumen ni los hallazgos. Acciones de
 * fila: «Ver» abre la ficha del caso y «Decidir» (`decidirCasoLft`) sobre un caso no cerrado y sin decisión. Un caso
 * es expediente: no se edita ni se elimina, y el contrato no tiene esas operaciones.
 */
@Component({
  selector: 'ap-bandeja-de-casos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MarcoDeCumplimiento, ListadoDeBandeja, AccionesDeFila, ChipEstado, Alerta, Icono, DecidirCaso],
  template: `
    <ap-marco-de-cumplimiento [titulo]="t.titulo" [proposito]="t.proposito" icono="carpeta">
      @if (recibo()) { <ap-alerta tono="ok">{{ recibo() }}</ap-alerta> }
      <ap-listado-de-bandeja recursoDeExportacion="cumplimiento-casos" [titulo]="t.tabla" [columnas]="COLUMNAS" [bandeja]="bandeja" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="casos" [textoVacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" ayudaDeBusqueda="Busca por código, prioridad, estado o decisión en la página cargada: el servidor no admite búsqueda. El estado filtra en el servidor.">
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
            @case ('prioridad') { {{ prioridad(c) }} }
            @case ('estado') { {{ estado(c) }} }
            @case ('plazoLimite') { {{ fecha(c.plazoLimite) }} }
            @case ('vencido') { <ap-chip-estado [tono]="c.vencido ? 'error' : 'neutro'">{{ plazo(c) }}</ap-chip-estado> }
            @case ('decision') { {{ decision(c) }} }
            @default { {{ c[columna.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-c>
          <ap-acciones-de-fila [fila]="'caso ' + c.codigo" [ver]="true" (verPulsado)="ver(c)">
            @if (puedeDecidir() && sePuedeDecidir(c)) {
              <button type="button" apAccionDeFila [attr.aria-label]="t.decidir + ' el caso ' + c.codigo" (click)="decidir(c)"><ap-icono nombre="balanza" tamano="s4" /><span>{{ t.decidir }}</span></button>
            }
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado-de-bandeja>
    </ap-marco-de-cumplimiento>
    @if (puedeDecidir()) { <ap-decidir-caso [(abierto)]="dialogoAbierto" [caso]="elegido()" (decidio)="alDecidir($event)" /> }
  `,
})
export class BandejaDeCasos {
  private readonly sesion = inject(Sesion)
  private readonly router = inject(Router)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly prioridad = prioridad
  protected readonly estado = estado
  protected readonly plazo = plazo
  protected readonly decision = decision
  protected readonly bandeja = crearBandeja<CasoEnBandeja>({
    cargador: cargadorDeCasos(),
    camposDeBusqueda: ['codigo', prioridad, estado, decision],
    filtros: [{ nombre: 'estados', etiqueta: 'Estado', opciones: opcionesDe(textosLft.estadosDeCaso), textoTodos: 'Los no cerrados' }],
  })
  protected readonly identidad = (c: CasoEnBandeja) => c.casoId
  protected readonly fecha = (iso: string) => formatearFecha(iso)
  protected readonly sePuedeDecidir = (c: CasoEnBandeja) => c.estado !== 'CERRADO' && !c.decision

  protected readonly puedeDecidir = computed(() => tieneAlguno(this.sesion, ['CUMPLIMIENTO_CASOS']))
  protected readonly elegido = signal<CasoEnBandeja | null>(null)
  protected readonly dialogoAbierto = signal(false)
  protected readonly recibo = signal<string | null>(null)

  protected ver(caso: CasoEnBandeja): void {
    void this.router.navigate(['/cumplimiento/casos', caso.casoId])
  }

  protected decidir(caso: CasoEnBandeja): void {
    this.elegido.set(caso)
    this.dialogoAbierto.set(true)
  }

  protected alDecidir(salida: SalidaDecisionCasoLft): void {
    this.recibo.set(textosLft.decision.recibo(this.decisiones[salida.decision] ?? salida.decision, salida.alertasCerradas, salida.fueraDePlazo))
    void this.bandeja.recargar()
  }

  private readonly decisiones = textosLft.decisiones
}
