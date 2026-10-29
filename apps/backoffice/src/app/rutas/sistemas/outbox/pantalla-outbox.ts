import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { PUERTO_OUTBOX, type MensajeDescartado, type MensajeOutbox, type OutboxYDescartados } from '../dominio/puertos'
import { textosSistemas } from '../textos'

const ESTADO: Record<MensajeOutbox['estado'], string> = { pendiente: 'Pendiente', entregado: 'Entregado' }

/**
 * GATE: la cola de descartados es visible, con el motivo de cada mensaje a la vista (no en un detalle escondido).
 * Las dos colecciones son registros append-only: ningún listado ofrece editar ni eliminar (regla 91.1.3).
 */
@Component({
  selector: 'ap-pantalla-outbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="outbox.isLoading()" (pulsado)="outbox.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de la bandeja de salida" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado titulo="Bandeja de salida" [columnas]="columnasOutbox" [listado]="mensajes" [carga]="carga()" ayudaDeBusqueda="Busca por tipo de mensaje, estado o fecha de creación." [exportables]="exportablesOutbox" nombreDeRegistros="mensajes" [textoVacio]="t.vacio" explicacionVacio="Cuando un servicio publique un evento, aparece acá hasta que se entregue." (reintentar)="outbox.reload()">
        <ng-template #celda let-f let-c="columna">
          @if (c.clave === 'estado') { <ap-chip-estado [tono]="f.estado === 'entregado' ? 'ok' : 'aviso'">{{ estadoDe(f) }}</ap-chip-estado> } @else { {{ f[c.clave] }} }
        </ng-template>
      </ap-listado>
      <ap-listado [titulo]="t.tituloDescartados" [columnas]="columnasDescartados" [listado]="descartados" [carga]="carga()" ayudaDeBusqueda="Busca por tipo de mensaje, motivo del descarte o fecha." nombreDeRegistros="descartados" [textoVacio]="t.vacioDescartados" explicacionVacio="Un mensaje que no se pudo entregar tras sus reintentos aparece acá con su motivo." (reintentar)="outbox.reload()" />
    </section>
  `,
  styles: `:host { display: block; min-width: 0; } .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; } /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }`,
})
export class PantallaOutbox {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.outbox
  private readonly puerto = inject(PUERTO_OUTBOX)
  protected readonly outbox = resource({ loader: () => this.puerto.obtener() })
  protected readonly carga = cargaDe(this.outbox)
  private readonly filasOutbox = filasDe<MensajeOutbox, OutboxYDescartados>(this.outbox, (d) => d.mensajes)
  private readonly filasDescartados = filasDe<MensajeDescartado, OutboxYDescartados>(this.outbox, (d) => d.descartados)
  protected readonly mensajes = crearListado<MensajeOutbox>({
    filas: this.filasOutbox,
    camposDeBusqueda: ['tipo', (m) => ESTADO[m.estado], 'creadoEl'],
    filtros: [
      { nombre: 'tipo', etiqueta: 'Tipo', valor: (m) => m.tipo },
      { nombre: 'estado', etiqueta: 'Estado', valor: (m) => m.estado, texto: (v) => ESTADO[v as MensajeOutbox['estado']] ?? v },
    ],
    porPagina: 10,
  })
  protected readonly descartados = crearListado<MensajeDescartado>({
    filas: this.filasDescartados,
    camposDeBusqueda: ['tipo', 'motivo', 'descartadoEl'],
    filtros: [{ nombre: 'tipo', etiqueta: 'Tipo', valor: (m) => m.tipo }],
    porPagina: 10,
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const m = this.filasOutbox()
    return [
      { etiqueta: 'Mensajes', valor: contar(m), icono: 'bandeja' },
      { etiqueta: 'Pendientes', valor: contar(m, (x) => x.estado === 'pendiente'), icono: 'reloj', tono: 'aviso' },
      { etiqueta: 'Entregados', valor: contar(m, (x) => x.estado === 'entregado'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'Descartados', valor: contar(this.filasDescartados()), icono: 'prohibido', tono: 'error' },
    ]
  })
  protected readonly columnasOutbox: Columna<MensajeOutbox>[] = [
    { clave: 'tipo', titulo: 'Tipo', ordenable: true, mono: true },
    { clave: 'creadoEl', titulo: 'Creado el', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
  ]
  protected readonly exportablesOutbox: ColumnaExportable<MensajeOutbox>[] = [
    { clave: 'tipo', titulo: 'Tipo' },
    { clave: 'creadoEl', titulo: 'Creado el' },
    { clave: 'estado', titulo: 'Estado', valor: (m) => ESTADO[m.estado] },
  ]
  protected readonly columnasDescartados: Columna<MensajeDescartado>[] = [
    { clave: 'tipo', titulo: 'Tipo', ordenable: true, mono: true },
    { clave: 'motivo', titulo: 'Motivo' },
    { clave: 'descartadoEl', titulo: 'Descartado el', ordenable: true },
  ]
  protected estadoDe(m: MensajeOutbox): string {
    return ESTADO[m.estado]
  }
}
