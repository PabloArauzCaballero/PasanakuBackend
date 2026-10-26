import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { ColumnaDelCatalogo, TablaDelCatalogo } from 'clientes/angular/auditoria'
import { Sesion } from '../../../nucleo/sesion'
import { CajonDeRevision } from './cajon-de-revision'
import { DECISIONES, filasDeColumnas, filtrarColumnas, metricasDeColumnas, type FilaDeColumna } from './revision'
import { textosCatalogo } from './textos'

const SENSIBILIDADES: Record<string, string> = { NINGUNA: 'Ninguna', PERSONAL: 'Dato personal', FINANCIERO: 'Dato financiero', SECRETO: 'Secreto', DERIVADO: 'Derivado de un secreto' }

/**
 * Pestaña «Columnas» de la ficha, como en Atlas: cifras, barra de filtros, tabla con la sensibilidad
 * y la revisión de cada columna en palabras, y la acción «Revisar», que abre el cajón lateral (CU-131).
 */
@Component({
  selector: 'ap-vista-columnas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Boton, CajonDeRevision, ChipEstado, EstadoVacio, TablaDeDatos, TarjetaKPI],
  template: `
    <div class="cifras">
      <ap-tarjeta-kpi [etiqueta]="t.metricas.columnas" [valor]="'' + cifras().columnas" />
      <ap-tarjeta-kpi [etiqueta]="t.metricas.personales" [valor]="'' + cifras().personales" icono="persona" [tono]="cifras().personales ? 'pii' : 'neutro'" />
      <ap-tarjeta-kpi [etiqueta]="t.metricas.financieras" [valor]="'' + cifras().financieras" icono="aporte" [tono]="cifras().financieras ? 'info' : 'neutro'" />
      <ap-tarjeta-kpi [etiqueta]="t.metricas.descritas" [valor]="'' + cifras().descritas" icono="libro" [pista]="textos.metricas.pista(cifras().descritas, cifras().columnas)" />
      <ap-tarjeta-kpi [etiqueta]="t.metricas.aprobadas" [valor]="'' + cifras().aprobadas" icono="verificado" [tono]="cifras().aprobadas === cifras().columnas ? 'ok' : 'aviso'" [pista]="textos.metricas.pista(cifras().aprobadas, cifras().columnas)" />
    </div>
    <ap-barra-de-filtros [etiquetaDeBusqueda]="t.busqueda" [ayudaDeBusqueda]="t.ayudaDeBusqueda" [busqueda]="q()" [selectores]="selectores()" (busquedaChange)="q.set($event)" (selectorChange)="elegir($event.nombre, $event.valor)" (limpiar)="limpiar()" />
    @if (encontradas().length === 0) {
      <ap-estado-vacio motivo="porFiltro" [titulo]="t.vacio" (actuar)="limpiar()" />
    } @else {
      <ap-tabla-de-datos [titulo]="t.registros + ' de ' + tabla().tabla" [columnas]="columnas" [filas]="filas()" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.registros" [altoMaximo]="true">
        <ng-template #celda let-c let-columna="columna">
          @switch (columna.clave) {
            @case ('tipo') { <ap-chip-estado tono="info">{{ c.tipo }}</ap-chip-estado> }
            @case ('sensibilidad') { <ap-chip-estado [tono]="c.tonoSensibilidad">{{ c.sensibilidad }}</ap-chip-estado> }
            @case ('revision') { <ap-chip-estado [tono]="c.tonoRevision">{{ c.revision }}</ap-chip-estado> }
            @case ('negocio') { <span class="largo">{{ c.negocio }}</span> }
            @case ('sistema') { <span class="largo">{{ c.sistema }}</span> }
            @case ('acciones') { <ap-boton variante="secundario" tamano="sm" [icono]="puedeEditar() ? 'aprobar' : 'ojo'" (pulsado)="revisar(c.columna)">{{ puedeEditar() ? t.revisar : t.ver }}</ap-boton> }
            @default { {{ c[columna.clave] }} }
          }
        </ng-template>
      </ap-tabla-de-datos>
    }
    <ap-cajon-de-revision [tabla]="tabla()" [columna]="elegida()" [(abierto)]="cajonAbierto" (guardado)="cambio.emit()" />
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr)); }
    .largo { display: block; min-width: 14rem; white-space: normal; overflow-wrap: anywhere; }
  `,
})
export class VistaColumnas {
  readonly tabla = input.required<TablaDelCatalogo>()
  /** Se guardó una revisión: la ficha recarga el catálogo. */
  readonly cambio = output<void>()
  protected readonly textos = textosCatalogo
  protected readonly t = textosCatalogo.ficha.columnas
  private readonly sesion = inject(Sesion)
  protected readonly puedeEditar = computed(() => this.sesion.puede('CATALOGO_DATOS_EDITAR'))
  protected readonly q = signal('')
  protected readonly sensibilidad = signal('')
  protected readonly revision = signal('')
  protected readonly elegida = signal<ColumnaDelCatalogo | null>(null)
  protected readonly cajonAbierto = signal(false)
  protected readonly cifras = computed(() => metricasDeColumnas(this.tabla()))
  protected readonly encontradas = computed(() => filtrarColumnas(this.tabla(), { q: this.q(), sensibilidad: this.sensibilidad(), revision: this.revision() }))
  protected readonly filas = computed(() => filasDeColumnas({ ...this.tabla(), columnas: this.encontradas() }))
  protected readonly selectores = computed<Selector[]>(() => [
    { nombre: 'sensibilidad', etiqueta: this.t.filtros.sensibilidad, valor: this.sensibilidad(), opciones: Object.entries(SENSIBILIDADES).map(([valor, texto]) => ({ valor, texto })) },
    { nombre: 'revision', etiqueta: this.t.filtros.revision, valor: this.revision(), opciones: [{ valor: 'sinRevisar', texto: this.t.filtros.sinRevisar }, ...Object.entries(DECISIONES).map(([valor, texto]) => ({ valor, texto }))] },
  ])
  protected readonly porId = (c: FilaDeColumna) => c.id
  protected readonly columnas: Columna<FilaDeColumna>[] = [
    { clave: 'nombre', titulo: this.t.tabla.nombre, mono: true },
    { clave: 'tipo', titulo: this.t.tabla.tipo },
    { clave: 'negocio', titulo: this.t.tabla.negocio },
    { clave: 'sensibilidad', titulo: this.t.tabla.sensibilidad },
    { clave: 'nulos', titulo: this.t.tabla.nulos },
    { clave: 'sistema', titulo: this.t.tabla.sistema },
    { clave: 'revision', titulo: this.t.tabla.revision },
    { clave: 'confianza', titulo: this.t.tabla.confianza },
    { clave: 'acciones', titulo: this.t.tabla.acciones, fijaDerecha: true },
  ]

  protected elegir(nombre: string, valor: string): void {
    if (nombre === 'sensibilidad') this.sensibilidad.set(valor)
    else this.revision.set(valor)
  }
  protected limpiar(): void {
    this.q.set('')
    this.sensibilidad.set('')
    this.revision.set('')
  }
  protected revisar(c: ColumnaDelCatalogo): void {
    this.elegida.set(c)
    this.cajonAbierto.set(true)
  }
}
